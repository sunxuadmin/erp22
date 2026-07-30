package org.dromara.system.domain.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class SysWorkbenchConfigPackageVo {

    private String packageVersion;

    private String exportedAt;

    private Map<String, Object> style = new LinkedHashMap<>();

    private List<RoleConfig> roles = new ArrayList<>();

    private List<String> assetKeys = new ArrayList<>();

    @Data
    @NoArgsConstructor
    public static class RoleConfig {

        private String roleKey;

        private String roleName;

        private boolean shellConfigured;

        private Map<String, Object> shell = new LinkedHashMap<>();

        private List<LayoutConfig> layouts = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    public static class LayoutConfig {

        private String componentKey;

        private String title;

        private String width;

        private Integer sortOrder;

        private String visible;

        private String configJson;
    }
}
