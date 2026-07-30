package org.dromara.crehn.project.service;

import org.dromara.crehn.domain.ProjectFile;
import org.dromara.crehn.mapper.ProjectMapper;
import org.dromara.crehn.mapper.ProjectMemberMapper;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AestheticAchievementValidator extends ProjectSubmitValidationSupport {

    public AestheticAchievementValidator(ProjectMapper projectMapper, ProjectMemberMapper memberMapper) {
        super(projectMapper, memberMapper);
    }

    @Override
    public boolean supports(ProjectSubmitContext context) {
        return hasValidator(context, "aesthetic_achievement", "aesthetic", "achievement", "reform", "meiyu", "美育", "改革", "创新成果");
    }

    @Override
    public void validate(ProjectSubmitContext context) {
        if (isTeachingCase(context)) {
            validateTeachingCase(context);
        } else {
            validatePaper(context);
        }
    }

    private void validatePaper(ProjectSubmitContext context) {
        int maxPaperAuthors = intRule(context, "paperMaxAuthorCount", 2);
        int abstractMinLength = intRule(context, "paperAbstractMinLength", 250);
        int abstractMaxLength = intRule(context, "paperAbstractMaxLength", 350);
        int minBodyLength = intRule(context, "paperMinBodyLength", 5000);
        if (boolRule(context, "validatePaperAuthorCount", true)) {
            require(memberCount(context, "author") <= maxPaperAuthors, "学术论文作者人数不能超过 " + maxPaperAuthors + " 人");
        }
        if (boolRule(context, "validatePaperAbstractLength", true)) {
            int abstractLength = firstFieldRule(context, "paperAbstractFields", "abstract", "summary", "paperAbstract", "paper_abstract").length();
            require(abstractLength >= abstractMinLength && abstractLength <= abstractMaxLength, "学术论文摘要应为 " + abstractMinLength + " 至 " + abstractMaxLength + " 个字符");
        }
        if (boolRule(context, "validatePaperBodyLength", true)) {
            require(firstFieldRule(context, "paperBodyFields", "bodyText", "body_text", "mainText", "main_text", "paperText", "paper_text").length() >= minBodyLength,
                "学术论文正文不能少于 " + minBodyLength + " 个字符");
        }
        if (boolRule(context, "requirePaperReferences", true)) {
            require(StringUtils.isNotBlank(firstFieldRule(context, "paperReferenceFields", "references", "referenceList", "reference_list")),
                "请填写学术论文参考文献");
        }
        if (boolRule(context, "requireUnpublishedCommitment", true)) {
            require(truthyAnyRule(context, "unpublishedCommitmentFields", "unpublishedCommitment", "unpublished_commitment"),
                "请确认学术论文未公开发表承诺");
        }
        context.getResult().manualTip("人工审核：请核验学术论文未公开发表承诺。");
    }

    private void validateTeachingCase(ProjectSubmitContext context) {
        int maxCompleters = intRule(context, "caseMaxCompleterCount", 3);
        int maxCaseTextLength = intRule(context, "caseMaxTextLength", 5000);
        if (boolRule(context, "requireCaseUnitSubmitter", true)) {
            require(containsAny(firstFieldRule(context, "caseSubmitterTypeFields", "submitterType", "submitter_type"), "unit", "organization", "school", "单位")
                    || StringUtils.isNotBlank(firstFieldRule(context, "caseUnitNameFields", "unitName", "unit_name")),
                "教学改革案例须以组织或单位名义提交");
        }
        if (boolRule(context, "validateCaseCompleterCount", true)) {
            require(memberCount(context, "completer") <= maxCompleters, "教学改革案例完成人不能超过 " + maxCompleters + " 人");
        }
        String text = firstFieldRule(context, "caseTextFields", "caseText", "case_text", "bodyText", "body_text", "mainText", "main_text");
        if (boolRule(context, "validateCaseTextLength", true)) {
            require(text.length() <= maxCaseTextLength, "教学改革案例正文不能超过 " + maxCaseTextLength + " 个字符");
        }
        if (boolRule(context, "requireCaseSections", true)) {
            requireCaseSections(context, text);
        }
        validateTeachingCaseFiles(context);
    }

    private void validateTeachingCaseFiles(ProjectSubmitContext context) {
        int maxVideoCount = intRule(context, "caseMaxVideoCount", 1);
        long maxVideoMb = Math.round(numberRule(context, "caseVideoMaxMb", 1024D));
        double maxVideoDuration = numberRule(context, "caseVideoMaxDurationSeconds", 300D);
        int maxImageCount = intRule(context, "caseMaxImageCount", 5);
        long minImageMb = Math.round(numberRule(context, "caseImageMinMb", 10D));
        int minImageDpi = intRule(context, "caseImageMinDpi", 300);
        boolean mediaTipsOnly = mediaTechnicalTipsOnly(context);
        List<ProjectFile> videos = activeFiles(context, file -> "video".equalsIgnoreCase(StringUtils.blankToDefault(file.getMediaType(), "")));
        if (boolRule(context, "validateCaseVideoCount", true)) {
            require(videos.size() <= maxVideoCount, "教学改革案例视频不能超过 " + maxVideoCount + " 个");
        }
        for (ProjectFile file : videos) {
            if (mediaTipsOnly) {
                context.getResult().manualTip("人工审核：请核验教学改革案例视频格式、大小和时长：" + file.getOriginalName());
            } else {
                if (boolRule(context, "validateCaseVideoExt", true)) {
                    require(isExt(file, stringListRule(context, "caseVideoAllowedExt", "mp4", "mov").toArray(String[]::new)), "教学改革案例视频格式不符合要求：" + file.getOriginalName());
                }
                if (boolRule(context, "validateCaseVideoSize", true)) {
                    require(file.getFileSize() == null || file.getFileSize() <= mb(maxVideoMb), "教学改革案例视频大小不能超过 " + maxVideoMb + "MB：" + file.getOriginalName());
                }
                if (boolRule(context, "validateCaseVideoDuration", true)) {
                    require(file.getDurationSeconds() == null || file.getDurationSeconds() <= maxVideoDuration, "教学改革案例视频时长超过配置限制：" + file.getOriginalName());
                }
            }
        }
        List<ProjectFile> images = activeFiles(context, file -> "image".equalsIgnoreCase(StringUtils.blankToDefault(file.getMediaType(), "")));
        if (boolRule(context, "validateCaseImageCount", true)) {
            require(images.size() <= maxImageCount, "教学改革案例图片不能超过 " + maxImageCount + " 张");
        }
        for (ProjectFile file : images) {
            if (mediaTipsOnly) {
                context.getResult().manualTip("人工审核：请核验教学改革案例图片格式、大小和 DPI：" + file.getOriginalName());
            } else {
                if (boolRule(context, "validateCaseImageExt", true)) {
                    require(isExt(file, stringListRule(context, "caseImageAllowedExt", "jpg", "jpeg").toArray(String[]::new)), "教学改革案例图片格式不符合要求：" + file.getOriginalName());
                }
                if (boolRule(context, "validateCaseImageMinSize", true)) {
                    require(file.getFileSize() != null && file.getFileSize() >= mb(minImageMb), "教学改革案例图片大小不能小于 " + minImageMb + "MB：" + file.getOriginalName());
                }
                if (boolRule(context, "validateCaseImageDpi", true)) {
                    if (file.getDpi() == null) {
                        context.getResult().warn("图片 DPI 无法识别，需人工复核：" + file.getOriginalName());
                    } else {
                        require(file.getDpi() >= minImageDpi, "教学改革案例图片 DPI 不能低于 " + minImageDpi + "：" + file.getOriginalName());
                    }
                }
            }
        }
    }

    private void requireCaseSections(ProjectSubmitContext context, String text) {
        List<String> sections = stringListRule(context, "caseRequiredSections", "background|背景", "practice|method|做法", "effect|result|成效", "suggestion|建议");
        for (String section : sections) {
            List<String> aliases = parseStringList(section.replace("|", ","));
            if (!aliases.isEmpty()) {
                require(containsAny(text, aliases.toArray(String[]::new)), "教学改革案例需包含" + aliases.get(aliases.size() - 1));
            }
        }
    }

    private boolean isTeachingCase(ProjectSubmitContext context) {
        if (containsAny(context.categoryText(), "achievement_case", "teaching-case", "teaching_case", "case", "教学改革案例", "教学改革")) {
            return true;
        }
        String text = firstFieldRule(context, "achievementTypeFields", "achievementType", "achievement_type", "resultType", "result_type");
        return containsAny(text, "case", "teaching", "案例", "教学改革");
    }

}
