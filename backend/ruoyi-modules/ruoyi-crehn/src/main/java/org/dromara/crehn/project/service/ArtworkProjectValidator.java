package org.dromara.crehn.project.service;

import org.dromara.crehn.domain.ProjectFile;
import org.dromara.crehn.mapper.ProjectMapper;
import org.dromara.crehn.mapper.ProjectMemberMapper;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ArtworkProjectValidator extends ProjectSubmitValidationSupport {

    public ArtworkProjectValidator(ProjectMapper projectMapper, ProjectMemberMapper memberMapper) {
        super(projectMapper, memberMapper);
    }

    @Override
    public boolean supports(ProjectSubmitContext context) {
        return hasValidator(context, "artwork", "artwork", "work", "zuopin", "作品", "美术", "书法", "摄影", "影视");
    }

    @Override
    public void validate(ProjectSubmitContext context) {
        boolean film = isFilm(context);
        long authorCount = roleMemberCount(context, "author", "作者");
        if (authorCount == 0) {
            authorCount = memberCount(context, "student");
        }
        int maxAuthors = film ? intRule(context, "filmMaxAuthorCount", 6) : intRule(context, "maxAuthorCount", 3);
        if (boolRule(context, "validateAuthorCount", true)) {
            require(authorCount <= maxAuthors, (film ? "影视类作品" : "艺术作品") + "作者人数不能超过 " + maxAuthors + " 人");
        }
        long teacherCount = roleMemberCount(context, "teacher", "指导教师", "指导老师", "指导");
        if (boolRule(context, "validateTeacherCount", true)) {
            if (film) {
                int maxFilmTeachers = intRule(context, "filmMaxTeacherCount", 3);
                require(teacherCount <= maxFilmTeachers, "影视类作品指导教师人数不能超过 " + maxFilmTeachers + " 人");
            } else {
                int regularTeacherCount = intRule(context, "teacherCount", 1);
                require(teacherCount == regularTeacherCount, "普通作品指导教师人数必须为 " + regularTeacherCount + " 人");
            }
        }
        int maxDescriptionLength = intRule(context, "creationDescriptionMaxLength", 400);
        if (boolRule(context, "validateCreationDescriptionLength", true)) {
            require(firstFieldRule(context, "creationDescriptionFields", "creationDescription", "creation_description", "workDescription", "work_description").length() <= maxDescriptionLength,
                "作品创作说明不能超过 " + maxDescriptionLength + " 个字符");
        }
        if (boolRule(context, "requireUniqueAuthorProject", true)) {
            requirePersonLimit(context, "author", intRule(context, "authorMaxProjectCount", 1), "同一作者只能提交一件艺术作品");
        }
        if (boolRule(context, "requireUniqueStudentProject", true)) {
            requirePersonLimit(context, "student", intRule(context, "studentMaxProjectCount", 1), "同一学生只能提交一件艺术作品");
        }
        if (boolRule(context, "requireAigcFields", true) && isAigc(context)) {
            require(StringUtils.isNotBlank(firstFieldRule(context, "aigcUsageRatioFields", "aiUsageRatio", "ai_usage_ratio")),
                "AIGC 动画短片请填写 AI 工具使用比例");
            require(StringUtils.isNotBlank(firstFieldRule(context, "aigcProcessDescriptionFields", "aiProcessDescription", "ai_process_description", "aiCreationProcess", "ai_creation_process")),
                "AIGC 动画短片请填写创作过程说明");
        }
        if (mediaTechnicalTipsOnly(context)) {
            context.getResult().manualTip("人工审核：请按活动通知核验作品图片/视频格式、大小、DPI、分辨率、码率、时长、字幕和来源说明。");
        } else {
            validateFiles(context, film);
        }
    }

    private void validateFiles(ProjectSubmitContext context, boolean film) {
        long minImageMb = Math.round(numberRule(context, "imageMinMb", 5D));
        int minImageDpi = intRule(context, "imageMinDpi", 300);
        long maxVideoMb = Math.round(numberRule(context, "videoMaxMb", 1024D));
        for (ProjectFile file : activeFiles(context, file -> "image".equalsIgnoreCase(StringUtils.blankToDefault(file.getMediaType(), "")))) {
            if (boolRule(context, "validateImageExt", true)) {
                require(isExt(file, stringListRule(context, "imageAllowedExt", "jpg", "jpeg").toArray(String[]::new)), "作品图片格式不符合要求：" + file.getOriginalName());
            }
            if (boolRule(context, "validateImageMinSize", true)) {
                require(file.getFileSize() != null && file.getFileSize() >= mb(minImageMb), "作品图片大小不能小于 " + minImageMb + "MB：" + file.getOriginalName());
            }
            if (boolRule(context, "validateImageDpi", true)) {
                if (file.getDpi() == null) {
                    context.getResult().warn("图片 DPI 无法识别，需人工复核：" + file.getOriginalName());
                } else {
                    require(file.getDpi() >= minImageDpi, "作品图片 DPI 不能低于 " + minImageDpi + "：" + file.getOriginalName());
                }
            }
        }
        if (film) {
            for (ProjectFile file : activeFiles(context, file -> "video".equalsIgnoreCase(StringUtils.blankToDefault(file.getMediaType(), "")))) {
                if (boolRule(context, "validateVideoExt", true)) {
                    require(isExt(file, stringListRule(context, "videoAllowedExt", "mp4", "mov").toArray(String[]::new)), "影视类作品视频格式不符合要求：" + file.getOriginalName());
                }
                if (boolRule(context, "validateVideoSize", true)) {
                    require(file.getFileSize() == null || file.getFileSize() <= mb(maxVideoMb), "影视类作品视频大小不能超过 " + maxVideoMb + "MB：" + file.getOriginalName());
                }
                double maxDuration = resolveFilmRuleNumber(context, "maxDurationSeconds", numberRule(context, "filmMaxDurationSeconds", 0D));
                double minBitrate = resolveFilmRuleNumber(context, "minBitrateMbps", numberRule(context, "filmMinBitrateMbps", 0D));
                if (boolRule(context, "validateVideoDuration", true) && maxDuration > 0) {
                    require(file.getDurationSeconds() != null && file.getDurationSeconds() <= maxDuration,
                        "影视类作品视频时长超过配置限制：" + file.getOriginalName());
                }
                if (boolRule(context, "validateVideoBitrate", true) && minBitrate > 0) {
                    require(file.getBitrate() != null && file.getBitrate() >= Math.round(minBitrate * 1024 * 1024),
                        "影视类作品视频码率低于配置要求：" + file.getOriginalName());
                }
            }
        }
    }

    private boolean isFilm(ProjectSubmitContext context) {
        String text = firstFieldRule(context, "workTypeFields", "workType", "work_type", "artworkType", "artwork_type", "workSubtype", "work_subtype");
        return containsAny(text, "film", "video", "movie", "animation", "aigc", "影视", "短片", "动画");
    }

    private boolean isAigc(ProjectSubmitContext context) {
        String text = firstFieldRule(context, "workTypeFields", "workType", "work_type", "artworkType", "artwork_type", "workSubtype", "work_subtype");
        return containsAny(text, "aigc", "ai", "人工智能", "动画");
    }

    private int textLengthAny(ProjectSubmitContext context, String... keys) {
        return firstField(context, keys).length();
    }

    @SuppressWarnings("unchecked")
    private double resolveFilmRuleNumber(ProjectSubmitContext context, String key, double defaultValue) {
        Object rulesObj = context.getCategoryRule().get("filmVideoRules");
        String subtype = firstField(context, "workType", "work_type", "artworkType", "artwork_type", "workSubtype", "work_subtype");
        if (rulesObj instanceof Map<?, ?> rawRules) {
            Map<String, Object> rules = (Map<String, Object>) rawRules;
            for (Map.Entry<String, Object> entry : rules.entrySet()) {
                if (!containsAny(subtype, entry.getKey()) || !(entry.getValue() instanceof Map<?, ?> rawRule)) {
                    continue;
                }
                Object value = rawRule.get(key);
                Double parsed = parseDouble(value);
                if (parsed != null) {
                    return parsed;
                }
            }
        }
        return defaultValue;
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
