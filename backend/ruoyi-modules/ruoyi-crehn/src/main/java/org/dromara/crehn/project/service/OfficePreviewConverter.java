package org.dromara.crehn.project.service;

import org.dromara.common.core.utils.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class OfficePreviewConverter {

    private static final long CONVERT_TIMEOUT_SECONDS = 90L;

    @Value("${crehn.office.soffice-path:}")
    private String configuredSofficePath;

    @Value("${crehn.office.timeout-seconds:90}")
    private long configuredTimeoutSeconds;

    public OfficePreviewResult convertToPdf(MultipartFile file, String ext) {
        if (!isOfficeExt(ext)) {
            return OfficePreviewResult.notSupported();
        }

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("crehn-office-preview-");
            String sourceName = safeBaseName(file.getOriginalFilename()) + "." + normalizeExt(ext);
            Path source = tempDir.resolve(sourceName);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, source, StandardCopyOption.REPLACE_EXISTING);
            }

            return convertTempSource(tempDir, sourceName, source);
        } catch (Exception e) {
            return OfficePreviewResult.failed(tempDir, "文件已上传，但 PDF 预览转换失败：" + e.getMessage());
        }
    }

    public OfficePreviewResult convertToPdf(Path sourceFile, String ext, String originalName) {
        if (!isOfficeExt(ext)) {
            return OfficePreviewResult.notSupported();
        }

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("crehn-office-preview-");
            String sourceName = safeBaseName(originalName) + "." + normalizeExt(ext);
            Path source = tempDir.resolve(sourceName);
            Files.copy(sourceFile, source, StandardCopyOption.REPLACE_EXISTING);
            return convertTempSource(tempDir, sourceName, source);
        } catch (Exception e) {
            return OfficePreviewResult.failed(tempDir, "文件已上传，但 PDF 预览转换失败：" + e.getMessage());
        }
    }

    public boolean isOfficeExt(String ext) {
        String value = normalizeExt(ext);
        return "doc".equals(value) || "docx".equals(value)
            || "ppt".equals(value) || "pptx".equals(value)
            || "xls".equals(value) || "xlsx".equals(value);
    }

    private OfficePreviewResult convertTempSource(Path tempDir, String sourceName, Path source) throws IOException {
        ConvertAttempt attempt = runLibreOffice(source, tempDir);
        if (!attempt.isSuccess()) {
            return OfficePreviewResult.failed(tempDir, attempt.message());
        }
        Path pdf = tempDir.resolve(stripExt(sourceName) + ".pdf");
        if (!Files.isRegularFile(pdf) || Files.size(pdf) <= 0) {
            return OfficePreviewResult.failed(tempDir, "文件已上传，但未生成 PDF 预览文件");
        }
        if (!isPdf(pdf)) {
            return OfficePreviewResult.failed(tempDir, "文件已上传，但生成的预览文件不是有效 PDF");
        }
        return OfficePreviewResult.success(tempDir, pdf);
    }

    private boolean isPdf(Path pdf) throws IOException {
        byte[] header = new byte[4];
        try (InputStream input = Files.newInputStream(pdf)) {
            int read = input.read(header);
            return read == 4 && header[0] == '%' && header[1] == 'P' && header[2] == 'D' && header[3] == 'F';
        }
    }

    private ConvertAttempt runLibreOffice(Path source, Path outDir) {
        List<String> candidates = sofficeCandidates();
        List<String> messages = new ArrayList<>();
        for (String command : candidates) {
            try {
                Process process = new ProcessBuilder(command, "--headless", "--convert-to", "pdf", "--outdir", outDir.toString(), source.toString())
                    .redirectErrorStream(true)
                    .start();
                boolean finished = process.waitFor(convertTimeoutSeconds(), TimeUnit.SECONDS);
                String output = new String(process.getInputStream().readAllBytes());
                if (!finished) {
                    process.destroyForcibly();
                    messages.add(command + " timed out");
                    continue;
                }
                if (process.exitValue() == 0) {
                    return ConvertAttempt.success();
                }
                messages.add(command + " exit code " + process.exitValue() + ": " + output);
            } catch (IOException e) {
                messages.add(command + " unavailable: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return ConvertAttempt.failed("文件已上传，但 PDF 预览转换被中断");
            }
        }
        return ConvertAttempt.failed("文件已上传，但 PDF 预览转换失败。请安装 LibreOffice/soffice，或配置 crehn.office.soffice-path。"
            + (messages.isEmpty() ? "" : " 详细信息：" + String.join("; ", messages)));
    }

    private List<String> sofficeCandidates() {
        List<String> candidates = new ArrayList<>();
        String configured = configuredSofficePath;
        if (StringUtils.isBlank(configured)) {
            configured = System.getProperty("crehn.office.soffice-path");
        }
        if (StringUtils.isBlank(configured)) {
            configured = System.getenv("CREHN_SOFFICE_PATH");
        }
        if (StringUtils.isNotBlank(configured)) {
            candidates.add(configured);
        }
        candidates.add("soffice");
        candidates.add("soffice.exe");
        candidates.add("libreoffice");
        return candidates.stream().distinct().collect(Collectors.toList());
    }

    private long convertTimeoutSeconds() {
        return configuredTimeoutSeconds > 0 ? configuredTimeoutSeconds : CONVERT_TIMEOUT_SECONDS;
    }

    private String normalizeExt(String ext) {
        return StringUtils.blankToDefault(ext, "").replace(".", "").toLowerCase(Locale.ROOT);
    }

    private String safeBaseName(String originalName) {
        String value = StringUtils.blankToDefault(originalName, "document");
        value = stripExt(value).replaceAll("[^a-zA-Z0-9._-]", "_");
        if (StringUtils.isBlank(value)) {
            return "document";
        }
        return value.length() > 80 ? value.substring(0, 80) : value;
    }

    private String stripExt(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static class ConvertAttempt {
        private final boolean success;
        private final String message;

        private ConvertAttempt(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        static ConvertAttempt success() {
            return new ConvertAttempt(true, "PDF 预览已生成");
        }

        static ConvertAttempt failed(String message) {
            return new ConvertAttempt(false, message);
        }

        boolean isSuccess() {
            return success;
        }

        String message() {
            return message;
        }
    }
}
