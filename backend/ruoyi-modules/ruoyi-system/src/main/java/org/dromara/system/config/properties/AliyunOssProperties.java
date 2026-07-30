package org.dromara.system.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Aliyun OSS bootstrap configuration.
 */
@Data
@Component
@ConfigurationProperties(prefix = "crehn.oss.aliyun")
public class AliyunOssProperties {

    /**
     * Whether to sync Aliyun OSS settings into sys_oss_config at startup.
     */
    private boolean enabled = false;

    /**
     * Whether the synced Aliyun OSS config should become the default upload target.
     */
    private boolean defaultConfig = true;

    /**
     * sys_oss_config.config_key.
     */
    private String configKey = "aliyun";

    private String accessKey;

    private String secretKey;

    private String bucketName;

    /**
     * Object key prefix.
     */
    private String prefix = "crehn";

    /**
     * Aliyun OSS endpoint host, for example oss-cn-hangzhou.aliyuncs.com.
     */
    private String endpoint;

    /**
     * Optional custom domain host without protocol.
     */
    private String domain;

    /**
     * Optional region for the S3-compatible client.
     */
    private String region;

    /**
     * Y for HTTPS, N for HTTP.
     */
    private String isHttps = "Y";

    /**
     * 0 private, 1 public, 2 custom.
     */
    private String accessPolicy = "0";

    private String remark = "Aliyun OSS object storage";
}
