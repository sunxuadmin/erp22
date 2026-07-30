package org.dromara.system.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class SysWorkbenchConfigImportPreviewVo {

    private String packageVersion;

    private String exportedAt;

    private int changedCount;

    private int unchangedCount;

    private int skippedCount;

    private List<DiffItem> differences = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiffItem {

        private String scope;

        private String roleKey;

        private String roleName;

        private String path;

        private String changeType;

        private String currentValue;

        private String importedValue;

        private boolean importable;
    }
}
