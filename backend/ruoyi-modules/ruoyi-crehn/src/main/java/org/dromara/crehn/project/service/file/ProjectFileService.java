package org.dromara.crehn.project.service.file;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.crehn.activity.service.ArtCategoryHierarchyService;
import org.dromara.crehn.common.ArtReviewConstants;
import org.dromara.crehn.common.ArtReviewSecurity;
import org.dromara.crehn.config.service.IArtActivityScopeService;
import org.dromara.crehn.domain.Activity;
import org.dromara.crehn.domain.ActivityCategory;
import org.dromara.crehn.domain.CategoryFileRequirement;
import org.dromara.crehn.domain.Project;
import org.dromara.crehn.domain.ProjectFile;
import org.dromara.crehn.domain.bo.ProjectFileDirectUploadCompleteBo;
import org.dromara.crehn.domain.bo.ProjectFileDirectUploadInitBo;
import org.dromara.crehn.domain.vo.ProjectFileDirectUploadInitVo;
import org.dromara.crehn.domain.vo.ProjectFileVo;
import org.dromara.crehn.mapper.ActivityCategoryMapper;
import org.dromara.crehn.mapper.ActivityMapper;
import org.dromara.crehn.mapper.CategoryFileRequirementMapper;
import org.dromara.crehn.mapper.ProjectFileMapper;
import org.dromara.crehn.mapper.ProjectMapper;
import org.dromara.crehn.project.service.FileRequirementRuleChecker;
import org.dromara.crehn.project.service.FileRuleCheckResult;
import org.dromara.crehn.project.service.MediaMetadata;
import org.dromara.crehn.project.service.MediaMetadataParser;
import org.dromara.crehn.project.service.OfficePreviewQueueService;
import org.dromara.crehn.project.service.assembler.ProjectDetailAssembler;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.vo.SysOssDirectUploadVo;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.service.ISysOssService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ART-OWNER: BE.PROJECT.FILE_UPLOAD
 * 项目材料上传服务，只承接上传、直传、校验、预览请求和文件删除，接口返回结构仍由原门面方法维持。
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class ProjectFileService {

    private static final Duration DIRECT_UPLOAD_EXPIRES = Duration.ofMinutes(30);
    private static final String DIRECT_UPLOAD_CACHE_PREFIX = "crehn:project:file:direct-upload:";

    private final ProjectMapper projectMapper;
    private final ProjectFileMapper projectFileMapper;
    private final ActivityMapper activityMapper;
    private final ActivityCategoryMapper categoryMapper;
    private final ArtCategoryHierarchyService categoryHierarchyService;
    private final CategoryFileRequirementMapper fileRequirementMapper;
    private final ISysOssService ossService;
    private final ArtReviewSecurity artReviewSecurity;
    private final IArtActivityScopeService activityScopeService;
    private final MediaMetadataParser mediaMetadataParser;
    private final OfficePreviewQueueService officePreviewQueueService;
    private final FileRequirementRuleChecker fileRequirementRuleChecker;
    private final ProjectDetailAssembler projectDetailAssembler;

    public ProjectFileVo uploadFile(Long projectId, Long requirementId, MultipartFile file) {
        UploadPrepareContext context = prepareUpload(projectId, requirementId, file == null ? null : file.getOriginalFilename(),
            file == null ? null : file.getSize(), true);
        String uploadExt = normalizeExt(file.getOriginalFilename());
        String contentType = resolveContentType(file.getOriginalFilename(), file.getContentType());
        MediaMetadata metadata = mediaMetadataParser.parse(file, uploadExt, contentType);
        FileRuleCheckResult checkResult = checkUploadedFile(context.requirement(), context.categoryRule(), metadata,
            file.getOriginalFilename(), uploadExt, file.getSize());

        SysOssVo oss = ossService.upload(file);
        return saveProjectFile(projectId, context.requirement(), context.activeCount(), oss, file.getSize(), contentType, metadata, checkResult);
    }

    public ProjectFileDirectUploadInitVo initDirectUpload(Long projectId, Long requirementId, ProjectFileDirectUploadInitBo bo) {
        String originalName = bo == null ? null : bo.getOriginalName();
        Long fileSize = bo == null ? null : bo.getFileSize();
        UploadPrepareContext context = prepareUpload(projectId, requirementId, originalName, fileSize, false);
        String contentType = resolveContentType(originalName, bo == null ? null : bo.getContentType());
        SysOssDirectUploadVo directUpload = ossService.createDirectUpload(originalName, DIRECT_UPLOAD_EXPIRES);
        String uploadToken = UUID.randomUUID().toString().replace("-", "");
        DirectUploadSession session = new DirectUploadSession(LoginHelper.getUserId(), projectId, requirementId,
            directUpload.getObjectKey(), originalName, fileSize, contentType);
        // 直传会话继续使用原 Redis key 和 30 分钟有效期，避免前端 token 字段和补传流程变化。
        RedisUtils.setCacheObject(directUploadCacheKey(uploadToken), JsonUtils.toJsonString(session), DIRECT_UPLOAD_EXPIRES);

        ProjectFileDirectUploadInitVo vo = new ProjectFileDirectUploadInitVo();
        vo.setUploadUrl(directUpload.getUploadUrl());
        vo.setUploadToken(uploadToken);
        vo.setObjectKey(directUpload.getObjectKey());
        vo.setFileName(directUpload.getFileName());
        vo.setOriginalName(context.originalName());
        vo.setContentType(contentType);
        vo.setFileSize(context.fileSize());
        vo.setExpiresInSeconds(directUpload.getExpiresInSeconds());
        return vo;
    }

    public ProjectFileVo completeDirectUpload(Long projectId, Long requirementId, ProjectFileDirectUploadCompleteBo bo) {
        DirectUploadSession session = requireDirectUploadSession(projectId, requirementId, bo);
        UploadPrepareContext context = prepareUpload(projectId, requirementId, session.getOriginalName(), session.getFileSize(), true);
        Long objectSize = ossService.getObjectSize(session.getObjectKey());
        if (objectSize == null || objectSize <= 0) {
            throw new ServiceException("直传文件不存在或尚未上传完成，请重新上传");
        }
        if (!Objects.equals(objectSize, session.getFileSize())) {
            throw new ServiceException("直传文件大小与登记信息不一致，请重新上传");
        }

        String uploadExt = normalizeExt(session.getOriginalName());
        String contentType = resolveContentType(session.getOriginalName(), session.getContentType());
        Path tempFile = null;
        try {
            tempFile = ossService.downloadToTemp(session.getObjectKey());
            MediaMetadata metadata = mediaMetadataParser.parse(tempFile, uploadExt, contentType);
            FileRuleCheckResult checkResult = checkUploadedFile(context.requirement(), context.categoryRule(), metadata,
                session.getOriginalName(), uploadExt, objectSize);
            SysOssVo oss = ossService.registerDirectUpload(session.getOriginalName(), session.getObjectKey(), contentType, objectSize);
            ProjectFileVo result = saveProjectFile(projectId, context.requirement(), context.activeCount(), oss, objectSize,
                contentType, metadata, checkResult);
            RedisUtils.deleteObject(directUploadCacheKey(bo.getUploadToken()));
            return result;
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.warn("直传临时文件清理失败: {}", tempFile, e);
                }
            }
        }
    }

    public ProjectFileVo requestFilePreview(Long projectId, Long fileId) {
        Project project = requireProject(projectId);
        artReviewSecurity.checkProjectAccess(project);
        ProjectFile projectFile = projectFileMapper.selectById(fileId);
        if (projectFile == null || !Objects.equals(projectFile.getProjectId(), projectId)
            || !ArtReviewConstants.FILE_ACTIVE.equals(projectFile.getStatus())) {
            throw new ServiceException("文件不存在");
        }
        if (!officePreviewQueueService.shouldConvert(projectFile.getFileExt())) {
            return projectDetailAssembler.refreshProjectFileAccessUrl(projectFileMapper.selectVoById(fileId));
        }
        if (OfficePreviewQueueService.PREVIEW_CONVERTED.equals(projectFile.getPreviewStatus())
            && StringUtils.isNotBlank(projectFile.getPreviewPath())) {
            return projectDetailAssembler.refreshProjectFileAccessUrl(projectFileMapper.selectVoById(fileId));
        }
        if (!OfficePreviewQueueService.PREVIEW_QUEUED.equals(projectFile.getPreviewStatus())
            && !OfficePreviewQueueService.PREVIEW_CONVERTING.equals(projectFile.getPreviewStatus())) {
            projectFileMapper.update(null, Wrappers.lambdaUpdate(ProjectFile.class)
                .set(ProjectFile::getPreviewStatus, OfficePreviewQueueService.PREVIEW_QUEUED)
                .set(ProjectFile::getPreviewMessage, "PDF 预览任务已重新排队，请稍后刷新")
                .set(ProjectFile::getPreviewRetryCount, 0)
                .set(ProjectFile::getPreviewStartedAt, null)
                .eq(ProjectFile::getId, fileId)
                .eq(ProjectFile::getStatus, ArtReviewConstants.FILE_ACTIVE));
        }
        // 与原实现一致：事务提交后再入队，避免转换器读到未提交的文件记录。
        officePreviewQueueService.enqueueAfterCommit(fileId);
        return projectDetailAssembler.refreshProjectFileAccessUrl(projectFileMapper.selectVoById(fileId));
    }

    public void deleteFile(Long projectId, Long fileId) {
        Project project = requireProjectForUpdate(projectId);
        artReviewSecurity.checkProjectAccess(project);
        requireEditable(project);
        requireCategoryAvailable(project.getActivityId(), project.getCategoryId());
        ProjectFile projectFile = projectFileMapper.selectById(fileId);
        if (projectFile == null || !Objects.equals(projectFile.getProjectId(), projectId)) {
            throw new ServiceException("文件不存在");
        }
        projectFile.setStatus(ArtReviewConstants.FILE_DELETED);
        projectFile.setUpdateBy(LoginHelper.getUserId());
        projectFile.setUpdateTime(new Date());
        projectFileMapper.updateById(projectFile);
    }

    private ProjectFileVo saveProjectFile(Long projectId, CategoryFileRequirement requirement, long activeCount, SysOssVo oss,
                                          Long fileSize, String contentType, MediaMetadata metadata, FileRuleCheckResult checkResult) {
        ProjectFile projectFile = new ProjectFile();
        projectFile.setProjectId(projectId);
        projectFile.setRequirementId(requirement.getId());
        projectFile.setOssId(oss.getOssId());
        projectFile.setFileTypeCode(requirement.getFileTypeCode());
        projectFile.setOriginalName(oss.getOriginalName());
        projectFile.setStoragePath(oss.getUrl());
        String fileExt = normalizeExt(oss.getFileSuffix());
        projectFile.setFileExt(fileExt);
        projectFile.setFileSize(fileSize);
        projectFile.setMimeType(contentType);
        projectFile.setMediaType(metadata.getMediaType());
        projectFile.setDurationSeconds(metadata.getDurationSeconds());
        projectFile.setWidth(metadata.getWidth());
        projectFile.setHeight(metadata.getHeight());
        projectFile.setFps(metadata.getFps());
        projectFile.setBitrate(metadata.getBitrate());
        projectFile.setDpi(metadata.getDpi());
        projectFile.setMetadataJson(metadata.getMetadataJson());
        projectFile.setCheckStatus(checkResult.getStatus());
        projectFile.setCheckMessage(checkResult.getMessage());
        if (officePreviewQueueService.shouldConvert(fileExt)) {
            projectFile.setPreviewStatus(OfficePreviewQueueService.PREVIEW_QUEUED);
            projectFile.setPreviewMessage("PDF 预览任务已排队，请稍后刷新");
            projectFile.setPreviewRetryCount(0);
        }
        projectFile.setVersionNo((int) activeCount + 1);
        projectFile.setUploadedBy(LoginHelper.getUserId());
        projectFile.setUploadedAt(new Date());
        projectFile.setStatus(ArtReviewConstants.FILE_ACTIVE);
        projectFileMapper.insert(projectFile);
        officePreviewQueueService.enqueueAfterCommit(projectFile.getId());
        return projectDetailAssembler.refreshProjectFileAccessUrl(projectFileMapper.selectVoById(projectFile.getId()));
    }

    /**
     * 附件技术校验失败仍只落库为 failed 状态，不在上传阶段阻断，保持后续提交前统一拦截的旧行为。
     */
    private FileRuleCheckResult checkUploadedFile(CategoryFileRequirement requirement, Map<String, Object> categoryRule, MediaMetadata metadata,
                                                  String originalName, String uploadExt, long fileSize) {
        try {
            return fileRequirementRuleChecker.check(requirement, categoryRule, metadata, originalName, uploadExt, fileSize);
        } catch (ServiceException e) {
            FileRuleCheckResult result = new FileRuleCheckResult();
            result.fail(e.getMessage());
            return result;
        }
    }

    private UploadPrepareContext prepareUpload(Long projectId, Long requirementId, String originalName, Long fileSize, boolean forUpdate) {
        Project project = forUpdate ? requireProjectForUpdate(projectId) : requireProject(projectId);
        artReviewSecurity.checkProjectAccess(project);
        requireEditable(project);
        requireCategoryAvailable(project.getActivityId(), project.getCategoryId());
        CategoryFileRequirement requirement = requireFileRequirement(requirementId);
        if (!Objects.equals(requirement.getCategoryId(), project.getCategoryId())) {
            throw new ServiceException("材料要求不属于当前项目类别");
        }
        validateUploadFile(requirement, originalName, fileSize == null ? 0L : fileSize);
        ActivityCategory category = categoryMapper.selectById(project.getCategoryId());
        Map<String, Object> categoryRule = parseRuleObject(category == null ? null : category.getRuleJson());
        long activeCount = countActiveFiles(projectId, requirementId);
        if (requirement.getMaxCount() != null && activeCount >= requirement.getMaxCount()) {
            throw new ServiceException("该材料最多上传 " + requirement.getMaxCount() + " 个，请先删除已有文件后再上传");
        }
        return new UploadPrepareContext(project, requirement, categoryRule, activeCount, originalName, fileSize);
    }

    private Project requireProject(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        return project;
    }

    private Project requireProjectForUpdate(Long id) {
        Project project = projectMapper.selectOne(Wrappers.lambdaQuery(Project.class)
            .eq(Project::getId, id)
            .last("for update"));
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        return project;
    }

    private void requireEditable(Project project) {
        if (!ArtReviewConstants.PROJECT_DRAFT.equals(project.getStatus())
            && !ArtReviewConstants.PROJECT_RETURNED.equals(project.getStatus())
            && !ArtReviewConstants.PROJECT_PARTICIPANT_RETURNED.equals(project.getStatus())) {
            throw new ServiceException("项目当前状态不可编辑");
        }
    }

    private void requireCategoryAvailable(Long activityId, Long categoryId) {
        Date now = new Date();
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || !ArtReviewConstants.ACTIVITY_ENABLED.equals(activity.getStatus())) {
            throw new ServiceException("活动不存在或未启用");
        }
        if (activity.getSignupStartAt() != null && activity.getSignupStartAt().after(now)) {
            throw new ServiceException("活动报名尚未开始");
        }
        if (activity.getSignupEndAt() != null && activity.getSignupEndAt().before(now)) {
            throw new ServiceException("活动报名已结束");
        }
        Long schoolId = artReviewSecurity.requireEnabledSchoolId();
        String scopeType = StringUtils.blankToDefault(activity.getScopeType(), ArtReviewConstants.ACTIVITY_SCOPE_ALL);
        if (ArtReviewConstants.ACTIVITY_SCOPE_ASSIGNED.equals(scopeType) && !activityScopeService.isSchoolAllowed(activityId, schoolId)) {
            throw new ServiceException("当前学校未获得该活动授权");
        }
        ActivityCategory category = categoryMapper.selectById(categoryId);
        if (category == null || !Objects.equals(category.getActivityId(), activityId)) {
            throw new ServiceException("类别不存在或不属于当前活动");
        }
        categoryHierarchyService.requireBusinessAvailableLeaf(category);
    }

    private CategoryFileRequirement requireFileRequirement(Long requirementId) {
        CategoryFileRequirement requirement = fileRequirementMapper.selectById(requirementId);
        if (requirement == null) {
            throw new ServiceException("上传材料要求不存在或已被删除");
        }
        return requirement;
    }

    private DirectUploadSession requireDirectUploadSession(Long projectId, Long requirementId, ProjectFileDirectUploadCompleteBo bo) {
        if (bo == null || StringUtils.isBlank(bo.getUploadToken())) {
            throw new ServiceException("直传凭证不能为空，请重新上传");
        }
        String cacheKey = directUploadCacheKey(bo.getUploadToken());
        String cached = RedisUtils.getCacheObject(cacheKey);
        DirectUploadSession session = JsonUtils.parseObject(cached, DirectUploadSession.class);
        if (session == null) {
            throw new ServiceException("直传凭证不存在或已过期，请重新上传");
        }
        if (!Objects.equals(session.getUserId(), LoginHelper.getUserId())
            || !Objects.equals(session.getProjectId(), projectId)
            || !Objects.equals(session.getRequirementId(), requirementId)
            || !Objects.equals(session.getObjectKey(), bo.getObjectKey())) {
            throw new ServiceException("直传凭证与文件不匹配，请重新上传");
        }
        if (StringUtils.isNotBlank(bo.getOriginalName()) && !Objects.equals(session.getOriginalName(), bo.getOriginalName())) {
            throw new ServiceException("直传文件名与登记信息不一致，请重新上传");
        }
        if (bo.getFileSize() != null && !Objects.equals(session.getFileSize(), bo.getFileSize())) {
            throw new ServiceException("直传文件大小与登记信息不一致，请重新上传");
        }
        return session;
    }

    private String directUploadCacheKey(String token) {
        return DIRECT_UPLOAD_CACHE_PREFIX + token;
    }

    private void validateUploadFile(CategoryFileRequirement requirement, String originalName, long fileSize) {
        if (StringUtils.isBlank(originalName) || fileSize <= 0) {
            throw new ServiceException("上传文件不能为空");
        }
        String ext = normalizeExt(originalName);
        if (!isAllAllowedExt(requirement.getAllowedExt())) {
            List<String> allowed = new ArrayList<>();
            Arrays.stream(requirement.getAllowedExt().split("[,，、;\\s]+"))
                .map(this::normalizeExt)
                .filter(StringUtils::isNotBlank)
                .forEach(allowed::add);
            if (!allowed.contains(ext)) {
                throw new ServiceException("文件格式不正确，请上传 " + formatAllowedExt(allowed) + " 格式文件");
            }
        }
        Map<String, Object> rule = parseRuleObject(requirement.getRuleJson());
        Double minMb = positiveNumber(rule.get("minMb"));
        Double ruleMaxMb = positiveNumber(rule.get("maxMb"));
        Double maxMb = requirement.getMaxSizeMb() == null ? ruleMaxMb : requirement.getMaxSizeMb().doubleValue();
        if (requirement.getMaxSizeMb() != null && ruleMaxMb != null) {
            maxMb = Math.min(maxMb, ruleMaxMb);
        }
        double sizeMb = fileSize / 1024D / 1024D;
        if (minMb != null && sizeMb < minMb) {
            throw new ServiceException("文件大小不能小于 " + formatMb(minMb));
        }
        if (maxMb != null && sizeMb > maxMb) {
            throw new ServiceException("文件大小不能超过 " + formatMb(maxMb));
        }
    }

    private Double positiveNumber(Object value) {
        if (value == null) {
            return null;
        }
        try {
            double number = Double.parseDouble(String.valueOf(value).trim());
            return Double.isFinite(number) && number > 0 ? number : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String formatMb(double value) {
        return value % 1D == 0D ? (long) value + "MB" : String.format(Locale.ROOT, "%.2fMB", value);
    }

    private Map<String, Object> parseRuleObject(String text) {
        if (StringUtils.isBlank(text) || !JsonUtils.isJsonObject(text)) {
            return Map.of();
        }
        Map<String, Object> parsed = JsonUtils.parseObject(text, new TypeReference<Map<String, Object>>() {
        });
        return parsed == null ? Map.of() : parsed;
    }

    private String formatAllowedExt(List<String> allowedExts) {
        String text = allowedExts.stream()
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining("/"));
        return StringUtils.isBlank(text) ? "指定" : text;
    }

    private boolean isAllAllowedExt(String allowedExt) {
        if (StringUtils.isBlank(allowedExt)) {
            return true;
        }
        String normalized = allowedExt.trim().toLowerCase(Locale.ROOT);
        return "*".equals(normalized) || "all".equals(normalized) || "所有格式".equals(normalized);
    }

    private long countActiveFiles(Long projectId, Long requirementId) {
        return projectFileMapper.selectCount(Wrappers.lambdaQuery(ProjectFile.class)
            .eq(ProjectFile::getProjectId, projectId)
            .eq(ProjectFile::getRequirementId, requirementId)
            .eq(ProjectFile::getStatus, ArtReviewConstants.FILE_ACTIVE));
    }

    private String normalizeExt(String nameOrExt) {
        if (StringUtils.isBlank(nameOrExt)) {
            return "";
        }
        String value = nameOrExt.trim().toLowerCase();
        int index = value.lastIndexOf('.');
        if (index >= 0) {
            value = value.substring(index + 1);
        }
        return value;
    }

    private String resolveContentType(String fileName, String contentType) {
        if (StringUtils.isNotBlank(contentType) && !MediaType.APPLICATION_OCTET_STREAM_VALUE.equalsIgnoreCase(contentType)) {
            return contentType;
        }
        String guessed = URLConnection.guessContentTypeFromName(fileName);
        return StringUtils.isNotBlank(guessed) ? guessed : MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private record UploadPrepareContext(Project project, CategoryFileRequirement requirement, Map<String, Object> categoryRule,
                                        long activeCount, String originalName, Long fileSize) {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class DirectUploadSession {
        private Long userId;
        private Long projectId;
        private Long requirementId;
        private String objectKey;
        private String originalName;
        private Long fileSize;
        private String contentType;
    }
}
