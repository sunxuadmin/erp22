package org.dromara.crehn.project.service;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SubmitValidationResult {
    private String status = "passed";
    private List<String> warnings = new ArrayList<>();
    private List<String> manualCheckTips = new ArrayList<>();
    private List<String> rejectReasonTemplates = new ArrayList<>();

    public void warn(String message) {
        if (message != null && !message.isBlank()) {
            warnings.add(message);
        }
    }

    public void manualTip(String message) {
        if (message != null && !message.isBlank()) {
            manualCheckTips.add(message);
        }
    }

    public void rejectReasonTemplate(String message) {
        if (message != null && !message.isBlank()) {
            rejectReasonTemplates.add(message);
        }
    }
}
