package org.dromara.crehn.project.service;

import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

@Data
public class OfficePreviewResult {
    private boolean supported;
    private boolean success;
    private Path pdfPath;
    private Path tempDir;
    private String status;
    private String message;

    public static OfficePreviewResult notSupported() {
        OfficePreviewResult result = new OfficePreviewResult();
        result.setSupported(false);
        result.setStatus("not_required");
        return result;
    }

    public static OfficePreviewResult failed(Path tempDir, String message) {
        OfficePreviewResult result = new OfficePreviewResult();
        result.setSupported(true);
        result.setSuccess(false);
        result.setTempDir(tempDir);
        result.setStatus("failed");
        result.setMessage(message);
        return result;
    }

    public static OfficePreviewResult success(Path tempDir, Path pdfPath) {
        OfficePreviewResult result = new OfficePreviewResult();
        result.setSupported(true);
        result.setSuccess(true);
        result.setTempDir(tempDir);
        result.setPdfPath(pdfPath);
        result.setStatus("converted");
        result.setMessage("PDF 预览已生成");
        return result;
    }

    public void cleanup() {
        if (tempDir == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(tempDir)) {
            paths
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        // Best effort temp cleanup.
                    }
                });
        } catch (IOException ignored) {
            // Best effort temp cleanup.
        }
    }
}
