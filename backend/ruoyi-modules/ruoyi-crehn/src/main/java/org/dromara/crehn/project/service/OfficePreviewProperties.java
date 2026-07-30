package org.dromara.crehn.project.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "crehn.office")
public class OfficePreviewProperties {

    private boolean enabled = true;
    private String sofficePath;
    private long timeoutSeconds = 90L;
    private int workerCount = 1;
    private int queueCapacity = 200;
    private int maxRetry = 2;
    private int scanBatchSize = 20;
    private long scanDelaySeconds = 10L;
    private long staleConvertingMinutes = 30L;

    public int workerCount() {
        return Math.max(1, workerCount);
    }

    public int queueCapacity() {
        return Math.max(1, queueCapacity);
    }

    public int maxRetry() {
        return Math.max(0, maxRetry);
    }

    public int scanBatchSize() {
        return Math.max(1, scanBatchSize);
    }

    public long scanDelaySeconds() {
        return Math.max(3L, scanDelaySeconds);
    }

    public long staleConvertingMinutes() {
        return Math.max(5L, staleConvertingMinutes);
    }
}
