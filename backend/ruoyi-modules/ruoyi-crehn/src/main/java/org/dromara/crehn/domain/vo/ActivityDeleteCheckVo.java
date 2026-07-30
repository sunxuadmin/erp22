package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 活动删除或永久删除前的依赖检查结果。
 */
@Data
public class ActivityDeleteCheckVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long activityId;
    private String activityName;
    private String delFlag;
    private Boolean canDelete;
    private Long categoryCount;
    private Long fieldSchemaCount;
    private Long fileRequirementCount;
    private Long schoolScopeCount;
    private Long projectCount;
    private Long fileCount;
    private Long referenceCount;
    private Boolean categoryDetailsTruncated;
    private Boolean projectDetailsTruncated;
    private Boolean fileDetailsTruncated;
    private String blockMessage;
    private List<CategoryItem> categories = new ArrayList<>();
    private List<ProjectItem> projects = new ArrayList<>();
    private List<FileItem> files = new ArrayList<>();
    private List<ReferenceItem> references = new ArrayList<>();

    @Data
    public static class CategoryItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long id;
        private Long parentId;
        private String categoryCode;
        private String categoryName;
        private String categoryGroup;
        private String delFlag;
        private Long fieldSchemaCount;
        private Long fileRequirementCount;
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
