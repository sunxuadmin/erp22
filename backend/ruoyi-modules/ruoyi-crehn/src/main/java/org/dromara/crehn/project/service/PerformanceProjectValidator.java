package org.dromara.crehn.project.service;

import org.dromara.crehn.domain.ProjectFile;
import org.dromara.crehn.mapper.ProjectMapper;
import org.dromara.crehn.mapper.ProjectMemberMapper;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PerformanceProjectValidator extends ProjectSubmitValidationSupport {

    public PerformanceProjectValidator(ProjectMapper projectMapper, ProjectMemberMapper memberMapper) {
        super(projectMapper, memberMapper);
    }

    @Override
    public boolean supports(ProjectSubmitContext context) {
        return hasValidator(context, "performance", "performance", "show", "program", "biaoyan", "节目", "表演");
    }

    @Override
    public void validate(ProjectSubmitContext context) {
        if (boolRule(context, "requireSingleProjectPerSchool", true)) {
            requireSingleProjectPerSchool(context, "同一学校在当前活动类别下只能提交一个艺术表演项目");
        }
        int maxTeacherCount = intRule(context, "maxTeacherCount", 3);
        if (boolRule(context, "validateTeacherCount", true)) {
            require(memberCount(context, "teacher") <= maxTeacherCount, "艺术表演项目指导教师人数不能超过 " + maxTeacherCount + " 人");
        }
        if (boolRule(context, "requireOriginalCommitment", true)) {
            require(truthyAnyRule(context, "originalCommitmentFields", "originalCommitment", "original_commitment", "isOriginal", "is_original",
                    "authorizationCommitment", "authorization_commitment", "authorizedCommitment", "authorized_commitment"),
                "请确认原创或授权承诺");
        }
        if (mediaTechnicalTipsOnly(context, "video")) {
            if (boolRule(context, "requireNoIdentityInVideo", true)
                && !truthyAnyRule(context, "noIdentityInVideoFields", "noIdentityInVideo", "no_identity_in_video", "noSchoolNameInVideo", "no_school_name_in_video")) {
                context.getResult().manualTip("人工审核：未勾选视频不含省份、学校、姓名、指导教师等身份信息确认项。");
            }
        } else {
            if (boolRule(context, "requireNoIdentityInVideo", true)) {
                require(truthyAnyRule(context, "noIdentityInVideoFields", "noIdentityInVideo", "no_identity_in_video", "noSchoolNameInVideo", "no_school_name_in_video"),
                    "请确认视频中不出现省份、学校、姓名、指导教师等身份信息");
            }
        }

        if (boolRule(context, "requireUniqueStudentProject", true)) {
            requirePersonLimit(context, "student", intRule(context, "studentMaxProjectCount", 1), "同一学生只能提交一个艺术表演项目");
        }
        validateVideos(context);
        context.getResult().manualTip("人工审核：表演视频不得出现省份、学校、学生姓名或指导教师姓名。");
        context.getResult().manualTip("人工审核：请核验原创或授权承诺材料。");
    }

    private void validateVideos(ProjectSubmitContext context) {
        if (mediaTechnicalTipsOnly(context, "video")) {
            if (!activeFiles(context, this::isVideoFile).isEmpty()) {
                context.getResult().manualTip("人工审核：请核验表演视频格式、分辨率、帧率、码率、时长、文件大小、固定机位及音画同步情况。");
            }
            return;
        }
        double maxDuration = resolveMaxDuration(context);
        double minBitrate = numberRule(context, "minBitrateMbps", 10D);
        double targetFps = numberRule(context, "fps", 25D);
        double fpsTolerance = numberRule(context, "fpsTolerance", 0.5D);
        int minWidth = intRule(context, "minVideoWidth", 1920);
        int minHeight = intRule(context, "minVideoHeight", 1080);
        long maxVideoMb = Math.round(numberRule(context, "maxVideoMb", 1024D));
        for (ProjectFile file : activeFiles(context, this::isVideoFile)) {
            if (boolRule(context, "validateVideoExt", true)) {
                require(isExt(file, stringListRule(context, "videoAllowedExt", "mp4", "mov").toArray(String[]::new)), "表演视频格式不符合要求：" + file.getOriginalName());
            }
            if (boolRule(context, "validateVideoSize", true)) {
                require(file.getFileSize() == null || file.getFileSize() <= mb(maxVideoMb), "表演视频大小不能超过 " + maxVideoMb + "MB：" + file.getOriginalName());
            }
            if (boolRule(context, "validateVideoResolution", true)) {
                require(file.getWidth() != null && file.getHeight() != null && file.getWidth() >= minWidth && file.getHeight() >= minHeight,
                    "视频分辨率不能低于 " + minWidth + "x" + minHeight + "：" + file.getOriginalName());
            }
            if (boolRule(context, "validateVideoFps", true)) {
                require(file.getFps() != null && Math.abs(file.getFps() - targetFps) <= fpsTolerance,
                    "视频帧率需约为 " + targetFps + "fps：" + file.getOriginalName());
            }
            if (boolRule(context, "validateVideoBitrate", true)) {
                require(file.getBitrate() != null && file.getBitrate() >= Math.round(minBitrate * 1024 * 1024),
                    "视频码率不能低于 " + minBitrate + "Mbps：" + file.getOriginalName());
            }
            if (boolRule(context, "validateVideoDuration", true) && maxDuration > 0) {
                require(file.getDurationSeconds() != null && file.getDurationSeconds() <= maxDuration,
                    "表演视频时长超过配置限制：" + file.getOriginalName());
            }
        }
    }

    private boolean isVideoFile(ProjectFile file) {
        return "video".equalsIgnoreCase(StringUtils.blankToDefault(file.getMediaType(), ""))
            || "video".equalsIgnoreCase(StringUtils.blankToDefault(file.getFileTypeCode(), ""));
    }

    @SuppressWarnings("unchecked")
    private double resolveMaxDuration(ProjectSubmitContext context) {
        Object byNature = context.getCategoryRule().get("maxDurationSecondsByProjectNature");
        String nature = firstField(context, "projectNature", "project_nature", "programNature", "program_nature");
        if (byNature instanceof Map<?, ?> raw) {
            Map<String, Object> rules = (Map<String, Object>) raw;
            for (Map.Entry<String, Object> entry : rules.entrySet()) {
                if (containsAny(nature, entry.getKey())) {
                    Double value = parseDouble(entry.getValue());
                    if (value != null) {
                        return value;
                    }
                }
            }
        }
        return numberRule(context, "maxDurationSeconds", 0D);
    }

    private Double parseDouble(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
