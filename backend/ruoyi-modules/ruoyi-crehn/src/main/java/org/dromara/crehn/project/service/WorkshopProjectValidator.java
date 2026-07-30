package org.dromara.crehn.project.service;

import org.dromara.crehn.mapper.ProjectMapper;
import org.dromara.crehn.mapper.ProjectMemberMapper;
import org.springframework.stereotype.Component;

@Component
public class WorkshopProjectValidator extends ProjectSubmitValidationSupport {

    public WorkshopProjectValidator(ProjectMapper projectMapper, ProjectMemberMapper memberMapper) {
        super(projectMapper, memberMapper);
    }

    @Override
    public boolean supports(ProjectSubmitContext context) {
        return hasValidator(context, "workshop", "workshop", "fang", "实践", "工作坊");
    }

    @Override
    public void validate(ProjectSubmitContext context) {
        long students = roleMemberCountRule(context, "student", "studentCountFields", "学生");
        long teachers = roleMemberCountRule(context, "teacher", "teacherCountFields", "指导教师", "指导老师", "指导");
        int minStudents = intRule(context, "minStudentCount", 7);
        int maxStudents = intRule(context, "maxStudentCount", 9);
        int minTeachers = intRule(context, "minTeacherCount", 1);
        int maxTeachers = intRule(context, "maxTeacherCount", 3);
        int maxTotalMembers = intRule(context, "maxTotalMemberCount", 12);
        double maxVideoDuration = numberRule(context, "maxVideoDurationSeconds", 480D);
        if (boolRule(context, "validateStudentCount", true)) {
            require(students >= minStudents && students <= maxStudents, "工作坊学生人数应为 " + minStudents + " 至 " + maxStudents + " 人");
        }
        if (boolRule(context, "validateTeacherCount", true)) {
            require(teachers >= minTeachers && teachers <= maxTeachers, "工作坊指导教师人数应为 " + minTeachers + " 至 " + maxTeachers + " 人");
        }
        if (boolRule(context, "validateTotalMemberCount", true)) {
            require(students + teachers <= maxTotalMembers, "工作坊总人数不能超过 " + maxTotalMembers + " 人");
        }
        if (boolRule(context, "requireSingleProjectPerSchool", true)) {
            requireSingleProjectPerSchool(context, "同一学校在当前活动类别下只能提交一个工作坊");
        }
        if (mediaTechnicalTipsOnly(context)) {
            context.getResult().manualTip("人工审核：请核验工作坊视频格式和时长，建议时长不超过 " + Math.round(maxVideoDuration) + " 秒。");
        } else {
            if (boolRule(context, "validateVideoExt", true)) {
                requireFileExt(context, file -> "video".equalsIgnoreCase(file.getMediaType()), "工作坊视频格式不符合要求", stringListRule(context, "videoAllowedExt", "mp4", "mpg", "mpeg").toArray(String[]::new));
            }
            if (boolRule(context, "validateVideoDuration", true)) {
                requireMaxDuration(context, file -> "video".equalsIgnoreCase(file.getMediaType()), maxVideoDuration, "工作坊视频时长超过配置限制");
            }
        }
        if (boolRule(context, "requireNoPreviousAwardCommitment", true)) {
            require(truthyAnyRule(context, "noPreviousAwardCommitmentFields", "noPreviousAwardCommitment", "no_previous_award_commitment"),
                "请确认工作坊未曾获奖承诺");
        }
        if (boolRule(context, "requireWorkshopTextFields", true)) {
            requireRequiredText(context, "projectIntroFields", "projectIntro", "project_intro");
            requireRequiredText(context, "designIdeaFields", "designIdea", "design_idea");
            requireRequiredText(context, "featureDescriptionFields", "featureDescription", "feature_description", "featureDesc", "feature_desc");
            requireRequiredText(context, "exhibitionDesignPlanFields", "exhibitionDesignPlan", "exhibition_design_plan");
        }
        context.getResult().manualTip("人工审核：请核验该工作坊是否未曾获奖。");
    }

    private void requireRequiredText(ProjectSubmitContext context, String ruleKey, String... keys) {
        require(!firstFieldRule(context, ruleKey, keys).isBlank(), "工作坊必填字段缺失：" + keys[0]);
    }
}
