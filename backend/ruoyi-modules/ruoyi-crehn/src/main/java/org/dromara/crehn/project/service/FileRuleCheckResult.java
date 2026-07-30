package org.dromara.crehn.project.service;

import lombok.Data;

@Data
public class FileRuleCheckResult {
    private String status = "passed";
    private String message = "passed";

    public void warn(String message) {
        if (!"failed".equals(status)) {
            this.status = "warning";
            this.message = message;
        }
    }

    public void fail(String message) {
        this.status = "failed";
        this.message = message == null || message.isBlank() ? "failed" : message;
    }
}
