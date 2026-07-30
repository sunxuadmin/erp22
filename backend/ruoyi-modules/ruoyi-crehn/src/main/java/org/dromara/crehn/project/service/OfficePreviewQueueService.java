package org.dromara.crehn.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.crehn.common.ArtReviewConstants;
import org.dromara.crehn.domain.ProjectFile;
import org.dromara.crehn.mapper.ProjectFileMapper;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.oss.core.OssClient;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.service.ISysOssService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OfficePreviewQueueService implements InitializingBean, DisposableBean {

    public static final String PREVIEW_QUEUED = "queued";
    public static final String PREVIEW_CONVERTING = "converting";
    public static final String PREVIEW_CONVERTED = "converted";
    public static final String PREVIEW_FAILED = "failed";

    private final ProjectFileMapper projectFileMapper;
    private final ISysOssService ossService;
    private final OfficePreviewConverter officePreviewConverter;
    private final OfficePreviewProperties properties;

    private final Set<Long> submittedFileIds = ConcurrentHashMap.newKeySet();
    private ThreadPoolExecutor workerExecutor;
    private ScheduledExecutorService scanExecutor;

    @Override
    public void afterPropertiesSet() {
        if (!properties.isEnabled()) {
            return;
        }
        workerExecutor = new ThreadPoolExecutor(
            properties.workerCount(),
            properties.workerCount(),
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(properties.queueCapacity()),
            namedThreadFactory("art-office-preview-worker-")
        );
        scanExecutor = Executors.newSingleThreadScheduledExecutor(namedThreadFactory("art-office-preview-scan-"));
        scanExecutor.scheduleWithFixedDelay(this::safeScanAndSubmit, 5L, properties.scanDelaySeconds(), TimeUnit.SECONDS);
    }

    @Override
    public void destroy() {
        if (scanExecutor != null) {
            scanExecutor.shutdownNow();
        }
        if (workerExecutor != null) {
            workerExecutor.shutdownNow();
        }
    }

    public boolean shouldConvert(String ext) {
        return properties.isEnabled() && officePreviewConverter.isOfficeExt(ext);
    }

    public void enqueueAfterCommit(Long projectFileId) {
        if (!properties.isEnabled() || projectFileId == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enqueue(projectFileId);
                }
            });
            return;
        }
        enqueue(projectFileId);
    }

    public void enqueue(Long projectFileId) {
        if (!properties.isEnabled() || projectFileId == null || workerExecutor == null) {
            return;
        }
        if (!submittedFileIds.add(projectFileId)) {
            return;
        }
        try {
            workerExecutor.execute(() -> {
                try {
                    convert(projectFileId);
                } finally {
                    submittedFileIds.remove(projectFileId);
                }
            });
        } catch (RejectedExecutionException e) {
            submittedFileIds.remove(projectFileId);
            log.warn("office preview queue is full, projectFileId={}", projectFileId);
        }
    }

    private void safeScanAndSubmit() {
        try {
            recoverStaleConverting();
            List<ProjectFile> tasks = TenantHelper.ignore(() -> projectFileMapper.selectList(Wrappers.lambdaQuery(ProjectFile.class)
                .select(ProjectFile::getId)
                .eq(ProjectFile::getStatus, ArtReviewConstants.FILE_ACTIVE)
                .in(ProjectFile::getPreviewStatus, PREVIEW_QUEUED, PREVIEW_FAILED)
                .and(w -> w.isNull(ProjectFile::getPreviewRetryCount)
                    .or()
                    .lt(ProjectFile::getPreviewRetryCount, properties.maxRetry()))
                .orderByAsc(ProjectFile::getUploadedAt)
                .last("limit " + properties.scanBatchSize())));
            for (ProjectFile task : tasks) {
                enqueue(task.getId());
            }
        } catch (Exception e) {
            log.warn("office preview scan failed", e);
        }
    }

    private void recoverStaleConverting() {
        Date staleBefore = Date.from(Instant.now().minusSeconds(properties.staleConvertingMinutes() * 60L));
        TenantHelper.ignore(() -> {
            projectFileMapper.update(null, Wrappers.lambdaUpdate(ProjectFile.class)
                .set(ProjectFile::getPreviewStatus, PREVIEW_QUEUED)
                .set(ProjectFile::getPreviewMessage, "上一次 PDF 预览转换被中断，已重新排队")
                .eq(ProjectFile::getStatus, ArtReviewConstants.FILE_ACTIVE)
                .eq(ProjectFile::getPreviewStatus, PREVIEW_CONVERTING)
                .lt(ProjectFile::getPreviewStartedAt, staleBefore));
        });
    }

    private void convert(Long projectFileId) {
        if (!claim(projectFileId)) {
            return;
        }
        ProjectFile file = TenantHelper.ignore(() -> projectFileMapper.selectById(projectFileId));
        if (file == null) {
            return;
        }
        TenantHelper.dynamic(file.getTenantId(), () -> convertInTenant(file.getId()));
    }

    private boolean claim(Long projectFileId) {
        Integer updated = TenantHelper.ignore(() -> projectFileMapper.update(null, Wrappers.lambdaUpdate(ProjectFile.class)
            .set(ProjectFile::getPreviewStatus, PREVIEW_CONVERTING)
            .set(ProjectFile::getPreviewMessage, "正在生成 PDF 预览")
            .set(ProjectFile::getPreviewStartedAt, new Date())
            .setSql("preview_retry_count = ifnull(preview_retry_count, 0) + 1")
            .eq(ProjectFile::getId, projectFileId)
            .eq(ProjectFile::getStatus, ArtReviewConstants.FILE_ACTIVE)
            .in(ProjectFile::getPreviewStatus, PREVIEW_QUEUED, PREVIEW_FAILED)
            .and(w -> w.isNull(ProjectFile::getPreviewRetryCount)
                .or()
                .lt(ProjectFile::getPreviewRetryCount, properties.maxRetry()))));
        return updated != null && updated == 1;
    }

    private void convertInTenant(Long projectFileId) {
        ProjectFile file = projectFileMapper.selectById(projectFileId);
        if (file == null || !ArtReviewConstants.FILE_ACTIVE.equals(file.getStatus())) {
            return;
        }

        Path source = null;
        OfficePreviewResult preview = null;
        try {
            source = downloadSource(file);
            preview = officePreviewConverter.convertToPdf(source, file.getFileExt(), file.getOriginalName());
            if (!preview.isSuccess() || preview.getPdfPath() == null) {
                markFailed(file, preview == null ? "PDF 预览转换失败" : preview.getMessage());
                return;
            }

            SysOssVo previewOss = ossService.upload(preview.getPdfPath().toFile());
            projectFileMapper.update(null, Wrappers.lambdaUpdate(ProjectFile.class)
                .set(ProjectFile::getPreviewOssId, previewOss.getOssId())
                .set(ProjectFile::getPreviewPath, previewOss.getUrl())
                .set(ProjectFile::getPreviewExt, "pdf")
                .set(ProjectFile::getPreviewStatus, PREVIEW_CONVERTED)
                .set(ProjectFile::getPreviewMessage, "PDF 预览已生成")
                .set(ProjectFile::getPreviewGeneratedAt, new Date())
                .set(ProjectFile::getPreviewStartedAt, null)
                .eq(ProjectFile::getId, file.getId()));
        } catch (Exception e) {
            markFailed(file, "PDF 预览转换失败：" + e.getMessage());
        } finally {
            if (preview != null) {
                preview.cleanup();
            }
            deleteQuietly(source);
        }
    }

    private Path downloadSource(ProjectFile file) throws IOException {
        Path source = Files.createTempFile("crehn-office-source-", "." + normalizeExt(file.getFileExt()));
        if (file.getOssId() != null) {
            SysOssVo oss = ossService.getById(file.getOssId());
            if (oss != null && StringUtils.isNotBlank(oss.getFileName())) {
                OssClient storage = OssFactory.instance(oss.getService());
                try (OutputStream output = Files.newOutputStream(source)) {
                    storage.download(oss.getFileName(), output, ignored -> {
                    });
                }
                return source;
            }
        }

        String path = file.getStoragePath();
        if (StringUtils.isBlank(path)) {
            throw new IOException("源文件路径为空");
        }
        URI uri = URI.create(path);
        if ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) {
            try (InputStream input = uri.toURL().openStream()) {
                Files.copy(input, source, StandardCopyOption.REPLACE_EXISTING);
            }
            return source;
        }
        Path local = "file".equalsIgnoreCase(uri.getScheme()) ? Path.of(uri) : Path.of(path);
        Files.copy(local, source, StandardCopyOption.REPLACE_EXISTING);
        return source;
    }

    private void markFailed(ProjectFile file, String message) {
        ProjectFile latest = projectFileMapper.selectById(file.getId());
        int retryCount = latest == null || latest.getPreviewRetryCount() == null ? 0 : latest.getPreviewRetryCount();
        String finalMessage = message;
        if (retryCount >= properties.maxRetry()) {
            finalMessage = message + "；已达到最大重试次数";
        }
        projectFileMapper.update(null, Wrappers.lambdaUpdate(ProjectFile.class)
            .set(ProjectFile::getPreviewStatus, PREVIEW_FAILED)
            .set(ProjectFile::getPreviewMessage, trim(finalMessage, 1000))
            .set(ProjectFile::getPreviewStartedAt, null)
            .eq(ProjectFile::getId, file.getId()));
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best effort temp cleanup.
        }
    }

    private ThreadFactory namedThreadFactory(String prefix) {
        return new ThreadFactory() {
            private int index = 1;

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, prefix + index++);
                thread.setDaemon(true);
                return thread;
            }
        };
    }

    private String normalizeExt(String ext) {
        return StringUtils.blankToDefault(ext, "").replace(".", "").toLowerCase(Locale.ROOT);
    }

    private String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
