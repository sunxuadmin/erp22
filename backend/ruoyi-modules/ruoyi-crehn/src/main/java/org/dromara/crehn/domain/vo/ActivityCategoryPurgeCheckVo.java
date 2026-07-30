package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 已删除类别永久删除前的依赖检查结果。
 */
@Data
public class ActivityCategoryPurgeCheckVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private Boolean canPurge;
    private Long childCount;
    private Long projectCount;
    private Long fileCount;
    private Long referenceCount;
    private Long fieldSchemaCount;
    private Long fileRequirementCount;
    private Boolean childDetailsTruncated;
    private Boolean projectDetailsTruncated;
    private Boolean fileDetailsTruncated;
    private String blockMessage;
    private List<ChildItem> children = new ArrayList<>();
    private List<ProjectItem> projects = new ArrayList<>();
    private List<FileItem> files = new ArrayList<>();
    private List<ReferenceItem> references = new ArrayList<>();

    @Data
    public static class ChildItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long id;
        private String categoryCode;
        private String categoryName;
        private String delFlag;
    }

    @Data
    public static class ProjectItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long id;
        private String projectNo;
        private String projectName;
        private Long schoolId;
        private String schoolName;
        private String status;
        private String delFlag;
        private Long fileCount;
        private Date createTime;
        private Date updateTime;
    }

    @Data
    public static class FileItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long id;
        private Long projectId;
        private String projectNo;
        private String projectName;
        private String projectStatus;
        private String projectDelFlag;
        private Long ossId;
        private String originalName;
        private String status;
        private String delFlag;
        private Date uploadedAt;
    }

    @Data
    public static class ReferenceItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private String referenceType;
        private String referenceLabel;
        private Long referenceCount;
        private String recordIds;
    }
}
