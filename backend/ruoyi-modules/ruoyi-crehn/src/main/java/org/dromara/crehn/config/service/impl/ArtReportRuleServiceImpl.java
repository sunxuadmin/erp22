package org.dromara.crehn.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.config.service.IArtReportRuleService;
import org.dromara.crehn.config.service.rule.ActivityReportRuleRuntimeService;
import org.dromara.crehn.domain.ActivityCategory;
import org.dromara.crehn.domain.ActivityReportRule;
import org.dromara.crehn.domain.ReportRuleItem;
import org.dromara.crehn.domain.ReportRulePackage;
import org.dromara.crehn.domain.bo.ReportRuleApplyBo;
import org.dromara.crehn.domain.vo.ActivityReportRuleVo;
import org.dromara.crehn.domain.vo.ActivityRuleGroupOptionVo;
import org.dromara.crehn.domain.vo.ReportRuleItemVo;
import org.dromara.crehn.domain.vo.ReportRulePackageVo;
import org.dromara.crehn.mapper.ActivityCategoryMapper;
import org.dromara.crehn.mapper.ActivityReportRuleMapper;
import org.dromara.crehn.mapper.ReportRuleItemMapper;
import org.dromara.crehn.mapper.ReportRulePackageMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class ArtReportRuleServiceImpl implements IArtReportRuleService {

    private final ReportRulePackageMapper packageMapper;
    private final ReportRuleItemMapper itemMapper;
    private final ActivityReportRuleMapper activityRuleMapper;
    private final ActivityCategoryMapper categoryMapper;
    private final ActivityReportRuleRuntimeService reportRuleRuntimeService;

    private static final String DEFAULT_PACKAGE_CODE = "henan_art_show_default_2026";
    private static final String DEFAULT_PACKAGE_NAME = "河南高校艺术展演报送规则默认包";
    private static final String DEFAULT_PACKAGE_VERSION = "2026.06";

    @Override
    public TableDataInfo<ReportRulePackageVo> queryPackagePage(ReportRulePackage query, PageQuery pageQuery) {
        Page<ReportRulePackageVo> page = packageMapper.selectVoPage(pageQuery.build(), buildPackageQuery(query));
        return TableDataInfo.build(page);
    }

    @Override
    public int savePackage(ReportRulePackage rulePackage) {
        if (StringUtils.isBlank(rulePackage.getPackageName())) {
            throw new ServiceException("规则包名称不能为空");
        }
        if (StringUtils.isBlank(rulePackage.getPackageCode())) {
            rulePackage.setPackageCode("pkg_" + System.currentTimeMillis());
        }
        if (StringUtils.isBlank(rulePackage.getStatus())) {
            rulePackage.setStatus("draft");
        }
        if (rulePackage.getEnabled() == null) {
            rulePackage.setEnabled(true);
        }
        if (rulePackage.getId() == null) {
            return packageMapper.insert(rulePackage);
        }
        return packageMapper.updateById(rulePackage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long restoreDefaultPackage() {
        ReportRulePackage rulePackage = packageMapper.selectOne(Wrappers.lambdaQuery(ReportRulePackage.class)
            .eq(ReportRulePackage::getPackageCode, DEFAULT_PACKAGE_CODE)
            .last("limit 1"), false);
        if (rulePackage == null) {
            rulePackage = new ReportRulePackage();
            rulePackage.setPackageCode(DEFAULT_PACKAGE_CODE);
            fillDefaultPackage(rulePackage);
            packageMapper.insert(rulePackage);
            if (rulePackage.getId() == null) {
                rulePackage = packageMapper.selectOne(Wrappers.lambdaQuery(ReportRulePackage.class)
                    .eq(ReportRulePackage::getPackageCode, DEFAULT_PACKAGE_CODE)
                    .last("limit 1"), false);
            }
        } else {
            fillDefaultPackage(rulePackage);
            packageMapper.updateById(rulePackage);
        }
        if (rulePackage == null || rulePackage.getId() == null) {
            throw new ServiceException("默认规则包恢复失败");
        }
        for (ReportRuleItem item : defaultRuleItems(rulePackage.getId())) {
            upsertDefaultItem(item);
        }
        return rulePackage.getId();
    }

    @Override
    public int deletePackage(Long[] ids) {
        Long count = packageMapper.selectCount(Wrappers.lambdaQuery(ReportRulePackage.class)
            .in(ReportRulePackage::getId, Arrays.asList(ids))
            .eq(ReportRulePackage::getPackageCode, DEFAULT_PACKAGE_CODE));
        if (count != null && count > 0) {
            throw new ServiceException("系统默认规则包不能删除，可使用恢复默认重新校准");
        }
        return packageMapper.deleteByIds(Arrays.asList(ids));
    }

    @Override
    public TableDataInfo<ReportRuleItemVo> queryItemPage(ReportRuleItem query, PageQuery pageQuery) {
        Page<ReportRuleItemVo> page = itemMapper.selectVoPage(pageQuery.build(), buildItemQuery(query));
        return TableDataInfo.build(page);
    }

    @Override
    public List<ReportRuleItemVo> queryItemList(ReportRuleItem query) {
        return itemMapper.selectVoList(buildItemQuery(query));
    }

    @Override
    public int saveItem(ReportRuleItem item) {
        if (item.getPackageId() == null) {
            throw new ServiceException("规则包ID不能为空");
        }
        normalizeRule(item);
        if (item.getId() == null) {
            return itemMapper.insert(item);
        }
        return itemMapper.updateById(item);
    }

    @Override
    public int deleteItem(Long[] ids) {
        return itemMapper.deleteByIds(Arrays.asList(ids));
    }

    @Override
    public TableDataInfo<ActivityReportRuleVo> queryActivityRulePage(ActivityReportRule query, PageQuery pageQuery) {
        Page<ActivityReportRuleVo> page = activityRuleMapper.selectVoPage(pageQuery.build(), buildActivityRuleQuery(query));
        return TableDataInfo.build(page);
    }

    @Override
    public List<ActivityReportRuleVo> queryActivityRuleList(ActivityReportRule query) {
        return activityRuleMapper.selectVoList(buildActivityRuleQuery(query));
    }

    @Override
    public List<ActivityRuleGroupOptionVo> queryActivityRuleSchoolOptions(ActivityReportRule query) {
        return reportRuleRuntimeService.listSchoolGroupOptions(query);
    }

    @Override
    public int saveActivityRule(ActivityReportRule rule) {
        if (rule.getActivityId() == null) {
            throw new ServiceException("活动ID不能为空");
        }
        normalizeRule(rule);
        if (rule.getId() == null) {
            return activityRuleMapper.insert(rule);
        }
        return activityRuleMapper.updateById(rule);
    }

    @Override
    public int deleteActivityRule(Long[] ids) {
        return activityRuleMapper.deleteByIds(Arrays.asList(ids));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int applyPackageToActivity(ReportRuleApplyBo bo) {
        if (bo.getPackageId() == null || bo.getActivityId() == null) {
            throw new ServiceException("规则包和活动不能为空");
        }
        List<ReportRuleItem> items = itemMapper.selectList(Wrappers.lambdaQuery(ReportRuleItem.class)
            .eq(ReportRuleItem::getPackageId, bo.getPackageId())
            .eq(ReportRuleItem::getEnabled, true)
            .orderByAsc(ReportRuleItem::getSortOrder)
            .orderByAsc(ReportRuleItem::getId));
        if (items.isEmpty()) {
            throw new ServiceException("规则包没有可应用的规则");
        }
        if (Boolean.TRUE.equals(bo.getOverwrite())) {
            activityRuleMapper.delete(Wrappers.lambdaQuery(ActivityReportRule.class)
                .eq(ActivityReportRule::getActivityId, bo.getActivityId()));
        }
        int inserted = 0;
        for (ReportRuleItem item : items) {
            if (!Boolean.TRUE.equals(bo.getOverwrite()) && activityRuleExists(bo.getActivityId(), item)) {
                continue;
            }
            ActivityReportRule rule = copyItemToActivityRule(item, bo.getActivityId());
            normalizeRule(rule);
            activityRuleMapper.insert(rule);
            inserted++;
        }
        return inserted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int restoreDefaultActivityRules(ReportRuleApplyBo bo) {
        if (bo == null || bo.getActivityId() == null) {
            throw new ServiceException("活动不能为空");
        }
        Long packageId = restoreDefaultPackage();
        ReportRuleApplyBo applyBo = new ReportRuleApplyBo();
        applyBo.setActivityId(bo.getActivityId());
        applyBo.setPackageId(packageId);
        applyBo.setOverwrite(Boolean.TRUE.equals(bo.getOverwrite()));
        return applyPackageToActivity(applyBo);
    }

    private void fillDefaultPackage(ReportRulePackage rulePackage) {
        rulePackage.setPackageCode(DEFAULT_PACKAGE_CODE);
        rulePackage.setPackageName(DEFAULT_PACKAGE_NAME);
        rulePackage.setActivityType("college_art_show");
        rulePackage.setVersionNo(DEFAULT_PACKAGE_VERSION);
        rulePackage.setStatus("published");
        rulePackage.setEnabled(true);
        rulePackage.setRemark("系统默认规则包：按河南高校艺术展演报送要求预置，可作为其他活动的规则模板复用。");
    }

    private void upsertDefaultItem(ReportRuleItem item) {
        normalizeRule(item);
        ReportRuleItem exists = itemMapper.selectOne(Wrappers.lambdaQuery(ReportRuleItem.class)
            .eq(ReportRuleItem::getPackageId, item.getPackageId())
            .eq(ReportRuleItem::getRuleCode, item.getRuleCode())
            .last("limit 1"), false);
        if (exists == null) {
            itemMapper.insert(item);
            return;
        }
        item.setId(exists.getId());
        itemMapper.updateById(item);
    }

    private boolean activityRuleExists(Long activityId, ReportRuleItem item) {
        LambdaQueryWrapper<ActivityReportRule> wrapper = Wrappers.lambdaQuery(ActivityReportRule.class)
            .eq(ActivityReportRule::getActivityId, activityId);
        if (StringUtils.isNotBlank(item.getRuleCode())) {
            wrapper.eq(ActivityReportRule::getRuleCode, item.getRuleCode());
        } else {
            wrapper.eq(ActivityReportRule::getPackageItemId, item.getId());
        }
        Long count = activityRuleMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    private List<ReportRuleItem> defaultRuleItems(Long packageId) {
        return List.of(
            defaultItem(packageId, "performance_total_undergraduate", "艺术表演类本科院校报送总数", "quota", "count", "school_type", null, null, "本科", "categoryGroup", "艺术表演类", "max", 10, null, null, null, "本科院校艺术表演类项目总数建议不超过 10 个。", "{\"countScope\":\"school_category_group\"}", 10, "本科账号数量默认值，可按活动实际文件调整。"),
            defaultItem(packageId, "performance_total_vocational", "艺术表演类高职高专报送总数", "quota", "count", "school_type", null, null, "高职高专", "categoryGroup", "艺术表演类", "max", 6, null, null, null, "高职高专艺术表演类项目总数建议不超过 6 个。", "{\"countScope\":\"school_category_group\"}", 11, "高职高专账号数量默认值，可按活动实际文件调整。"),
            defaultItem(packageId, "artwork_total_undergraduate", "艺术作品类本科院校报送总数", "quota", "count", "school_type", null, null, "本科", "categoryGroup", "艺术作品类", "max", 20, null, null, null, "本科院校艺术作品类项目总数建议不超过 20 件。", "{\"countScope\":\"school_category_group\"}", 12, "本科账号数量默认值，可按活动实际文件调整。"),
            defaultItem(packageId, "artwork_total_vocational", "艺术作品类高职高专报送总数", "quota", "count", "school_type", null, null, "高职高专", "categoryGroup", "艺术作品类", "max", 20, null, null, null, "高职高专艺术作品类项目总数建议不超过 20 件。", "{\"countScope\":\"school_category_group\"}", 13, "高职高专账号数量默认值，可按活动实际文件调整。"),
            defaultItem(packageId, "workshop_total_undergraduate", "艺术实践工作坊本科院校报送总数", "quota", "count", "school_type", null, null, "本科", "categoryGroup", "艺术实践工作坊", "max", 2, null, null, null, "本科院校艺术实践工作坊建议不超过 2 个。", "{\"countScope\":\"school_category_group\"}", 14, null),
            defaultItem(packageId, "workshop_total_vocational", "艺术实践工作坊高职高专报送总数", "quota", "count", "school_type", null, null, "高职高专", "categoryGroup", "艺术实践工作坊", "max", 1, null, null, null, "高职高专艺术实践工作坊建议不超过 1 个。", "{\"countScope\":\"school_category_group\"}", 15, null),
            defaultItem(packageId, "achievement_total_undergraduate", "美育改革创新成果本科院校报送总数", "quota", "count", "school_type", null, null, "本科", "categoryGroup", "高校美育改革创新优秀成果", "max", 4, null, null, null, "本科院校美育改革创新优秀成果建议不超过 4 项。", "{\"countScope\":\"school_category_group\"}", 16, null),
            defaultItem(packageId, "achievement_total_vocational", "美育改革创新成果高职高专报送总数", "quota", "count", "school_type", null, null, "高职高专", "categoryGroup", "高校美育改革创新优秀成果", "max", 2, null, null, null, "高职高专美育改革创新优秀成果建议不超过 2 项。", "{\"countScope\":\"school_category_group\"}", 17, null),

            defaultItem(packageId, "performance_group_a_min_ratio", "甲组比例不低于 60%", "ratio", "ratio", "activity", "艺术表演类", null, null, "displayGroup", "甲组", "min", null, bd(60), null, null, "艺术表演类甲组项目比例应不低于 60%。", "{\"ratioScope\":\"category_group\"}", 30, null),
            defaultItem(packageId, "performance_group_b_max_ratio", "乙组比例不超过 40%", "ratio", "ratio", "activity", "艺术表演类", null, null, "displayGroup", "乙组", "max", null, bd(40), null, null, "艺术表演类乙组项目比例应不超过 40%。", "{\"ratioScope\":\"category_group\"}", 31, null),
            defaultItem(packageId, "performance_collective_min_ratio", "集体项目比例不低于 80%", "ratio", "ratio", "activity", "艺术表演类", null, null, "categoryCode", null, "min", null, bd(80), null, null, "艺术表演类集体项目比例应不低于 80%。", "{\"ratioScope\":\"category_group\",\"excludeTargetValue\":\"performance_personal\"}", 32, "除个人项目外按集体项目统计。"),
            defaultItem(packageId, "performance_personal_max_ratio", "个人项目比例不超过 20%", "ratio", "ratio", "activity", "艺术表演类", "performance_personal", null, "categoryCode", "performance_personal", "max", null, bd(20), null, null, "艺术表演类个人项目比例应不超过 20%。", "{\"ratioScope\":\"category_group\"}", 33, null),
            defaultItem(packageId, "performance_single_category_max_ratio", "艺术表演单类别比例不超过 30%", "ratio", "ratio", "activity", "艺术表演类", null, null, "categoryCode", null, "max", null, bd(30), null, null, "声乐、器乐、舞蹈、戏剧/戏曲、朗诵每类均不应超过节目总数 30%。", "{\"ratioScope\":\"category_group\",\"targetValues\":[\"performance_vocal\",\"performance_instrumental\",\"performance_dance\",\"performance_drama\",\"performance_recitation\"]}", 34, null),
            defaultItem(packageId, "artwork_photography_max_ratio", "摄影作品比例不超过 20%", "ratio", "ratio", "activity", "艺术作品类", "artwork_photography", null, "workType", "摄影", "max", null, bd(20), null, null, "摄影作品比例应不超过艺术作品总数 20%。", "{\"ratioScope\":\"category_group\"}", 35, null),

            defaultItem(packageId, "performance_same_school_same_category_max_2", "同校同类艺术表演不超过 2 个", "quota", "count", "school", "艺术表演类", null, null, "categoryCode", null, "max", 2, null, null, null, "同一学校同一类别艺术表演节目报送不超过 2 个。", "{\"countScope\":\"same_school_same_category\"}", 50, null),
            defaultItem(packageId, "performance_cover_five_categories", "艺术表演覆盖五类", "special", "custom", "activity", "艺术表演类", null, null, "categoryCode", null, "include", null, null, null, null, "艺术表演类应覆盖声乐、器乐、舞蹈、戏剧/戏曲、朗诵 5 类。", "{\"requiredCategoryCodes\":[\"performance_vocal\",\"performance_instrumental\",\"performance_dance\",\"performance_drama\",\"performance_recitation\"]}", 51, null),
            defaultItem(packageId, "performance_personal_student_exclusive", "个人项目学生不得兼报", "special", "custom", "student", "艺术表演类", "performance_personal", null, "studentId", null, "unique", 1, null, null, null, "个人项目每名学生只能报 1 个节目，且不得与集体项目兼报。", "{\"exclusiveWithCategoryGroup\":\"艺术表演类\",\"exclusiveWithCollective\":true}", 52, null),

            defaultItem(packageId, "artwork_author_max_3", "普通艺术作品作者不超过 3 人", "member", "member", "project", "艺术作品类", null, null, "memberRole", "author", "max", 3, null, null, null, "普通艺术作品作者不超过 3 人。", "{\"excludeCategoryCodes\":[\"artwork_film\"]}", 70, null),
            defaultItem(packageId, "artwork_film_author_max_6", "影视作品作者不超过 6 人", "member", "member", "project", "艺术作品类", "artwork_film", null, "memberRole", "author", "max", 6, null, null, null, "影视作品作者不超过 6 人。", "{\"categoryCode\":\"artwork_film\"}", 71, null),
            defaultItem(packageId, "artwork_one_per_person", "每人作品数量限报 1 件", "special", "custom", "student", "艺术作品类", null, null, "studentId", null, "unique", 1, null, null, null, "艺术作品类每人限报 1 件作品。", "{\"uniqueScope\":\"artwork\"}", 72, null),
            defaultItem(packageId, "artwork_same_school_authors", "多人创作须同校", "special", "custom", "project", "艺术作品类", null, null, "schoolId", null, "same", null, null, null, null, "多人创作时创作者必须为同一学校学生。", "{\"sameSchoolForAuthors\":true}", 73, null),
            defaultItem(packageId, "artwork_teacher_max_1", "普通艺术作品指导教师 1 人", "member", "member", "project", "艺术作品类", null, null, "memberRole", "teacher", "max", 1, null, null, null, "普通艺术作品指导教师 1 人。", "{\"excludeCategoryCodes\":[\"artwork_film\"]}", 74, null),
            defaultItem(packageId, "artwork_film_teacher_max_3", "影视作品指导教师不超过 3 人", "member", "member", "project", "艺术作品类", "artwork_film", null, "memberRole", "teacher", "max", 3, null, null, null, "影视作品指导教师不超过 3 人。", "{\"categoryCode\":\"artwork_film\"}", 75, null),
            defaultItem(packageId, "artwork_description_max_400", "艺术作品创作说明 400 字以内", "special", "custom", "project", "艺术作品类", null, null, "creationDescription", null, "max_length", null, null, null, 400, "艺术作品须附 400 字以内创作说明。", "{\"maxLength\":400}", 76, null),

            defaultItem(packageId, "workshop_total_members_max_12", "工作坊每队不超过 12 人", "member", "member", "project", "艺术实践工作坊", null, null, "memberTotal", null, "max", 12, null, null, null, "艺术实践工作坊每队人数不超过 12 人。", null, 90, null),
            defaultItem(packageId, "workshop_student_members_7_9", "工作坊学生 7 至 9 人", "member", "member", "project", "艺术实践工作坊", null, null, "memberRole", "student", "between", null, null, 7, 9, "艺术实践工作坊学生人数应为 7 至 9 人。", "{\"role\":\"student\"}", 91, null),
            defaultItem(packageId, "workshop_teacher_members_1_3", "工作坊指导教师 1 至 3 人", "member", "member", "project", "艺术实践工作坊", null, null, "memberRole", "teacher", "between", null, null, 1, 3, "艺术实践工作坊指导教师应为 1 至 3 人。", "{\"role\":\"teacher\"}", 92, null),
            defaultItem(packageId, "workshop_previous_award_not_repeat", "已获奖工作坊不得重复申报", "special", "custom", "project", "艺术实践工作坊", null, null, "previousAward", null, "deny", null, null, null, null, "历届已获奖工作坊不得重复申报。", "{\"denyPreviousAward\":true}", 93, null),

            defaultItem(packageId, "achievement_paper_author_max_2", "学术论文作者不超过 2 人", "member", "member", "project", "高校美育改革创新优秀成果", "achievement_paper", null, "memberRole", "author", "max", 2, null, null, null, "学术论文作者不超过 2 人。", "{\"achievementType\":\"学术论文\"}", 110, null),
            defaultItem(packageId, "achievement_case_completer_max_3", "教学改革案例完成人不超过 3 人", "member", "member", "project", "高校美育改革创新优秀成果", "achievement_case", null, "memberRole", "participant", "max", 3, null, null, null, "教学改革案例以单位名义提交，完成人不超过 3 人。", "{\"achievementType\":\"教学改革案例\"}", 111, null),
            defaultItem(packageId, "achievement_paper_body_min_5000", "学术论文正文不少于 5000 字", "special", "custom", "project", "高校美育改革创新优秀成果", "achievement_paper", null, "bodyText", null, "min_length", null, null, 5000, null, "学术论文正文不少于 5000 字，含摘要、关键词、参考文献。", "{\"achievementType\":\"学术论文\",\"requiredFields\":[\"abstract\",\"keywords\",\"references\"]}", 112, null),
            defaultItem(packageId, "achievement_case_text_max_5000", "教学改革案例文字 5000 字以内", "special", "custom", "project", "高校美育改革创新优秀成果", "achievement_case", null, "bodyText", null, "max_length", null, null, null, 5000, "教学改革案例文字材料 5000 字以内。", "{\"achievementType\":\"教学改革案例\"}", 113, null),
            defaultItem(packageId, "achievement_case_video_limit", "教学改革案例视频限制", "file", "file", "project", "高校美育改革创新优秀成果", "achievement_case", null, "video", null, "max", 1, null, null, null, "教学改革案例可选 1 个 MP4 或 MOV 视频，5 分钟以内，不超过 1GB。", "{\"allowedExt\":[\"mp4\",\"mov\"],\"maxCount\":1,\"maxDurationSeconds\":300,\"maxSizeMb\":1024}", 114, null),
            defaultItem(packageId, "achievement_case_image_limit", "教学改革案例图片限制", "file", "file", "project", "高校美育改革创新优秀成果", "achievement_case", null, "image", null, "max", 5, null, null, null, "教学改革案例可选 5 张以内 JPG 图片，单张不低于 10MB，300dpi。", "{\"allowedExt\":[\"jpg\",\"jpeg\"],\"maxCount\":5,\"minSizeMb\":10,\"dpi\":300}", 115, null)
        );
    }

    private ReportRuleItem defaultItem(Long packageId, String ruleCode, String ruleName, String ruleGroup, String ruleType,
                                       String scopeType, String scopeCategoryGroup, String categoryCode, String schoolType,
                                       String targetFieldKey, String targetValue, String operator, Integer limitCount,
                                       BigDecimal ratioValue, Integer minValue, Integer maxValue, String message,
                                       String ruleJson, Integer sortOrder, String remark) {
        ReportRuleItem item = new ReportRuleItem();
        item.setPackageId(packageId);
        item.setRuleCode(ruleCode);
        item.setRuleName(ruleName);
        item.setRuleGroup(ruleGroup);
        item.setRuleType(ruleType);
        item.setScopeType(scopeType);
        item.setScopeCategoryGroup(scopeCategoryGroup);
        item.setCategoryCode(categoryCode);
        item.setSchoolType(schoolType);
        item.setTargetFieldKey(targetFieldKey);
        item.setTargetValue(targetValue);
        item.setOperator(operator);
        item.setLimitCount(limitCount);
        item.setRatioValue(ratioValue);
        item.setMinValue(minValue);
        item.setMaxValue(maxValue);
        item.setEnforceMode("warn");
        item.setMessage(message);
        item.setRuleJson(ruleJson);
        item.setEnabled(true);
        item.setSortOrder(sortOrder);
        item.setRemark(remark);
        return item;
    }

    private BigDecimal bd(int value) {
        return BigDecimal.valueOf(value);
    }

    private LambdaQueryWrapper<ReportRulePackage> buildPackageQuery(ReportRulePackage query) {
        return Wrappers.lambdaQuery(ReportRulePackage.class)
            .like(StringUtils.isNotBlank(query.getPackageName()), ReportRulePackage::getPackageName, query.getPackageName())
            .eq(StringUtils.isNotBlank(query.getPackageCode()), ReportRulePackage::getPackageCode, query.getPackageCode())
            .eq(StringUtils.isNotBlank(query.getActivityType()), ReportRulePackage::getActivityType, query.getActivityType())
            .eq(StringUtils.isNotBlank(query.getStatus()), ReportRulePackage::getStatus, query.getStatus())
            .eq(query.getEnabled() != null, ReportRulePackage::getEnabled, query.getEnabled())
            .orderByDesc(ReportRulePackage::getCreateTime);
    }

    private LambdaQueryWrapper<ReportRuleItem> buildItemQuery(ReportRuleItem query) {
        return Wrappers.lambdaQuery(ReportRuleItem.class)
            .eq(query.getPackageId() != null, ReportRuleItem::getPackageId, query.getPackageId())
            .eq(StringUtils.isNotBlank(query.getRuleGroup()), ReportRuleItem::getRuleGroup, query.getRuleGroup())
            .eq(StringUtils.isNotBlank(query.getRuleType()), ReportRuleItem::getRuleType, query.getRuleType())
            .eq(StringUtils.isNotBlank(query.getScopeCategoryGroup()), ReportRuleItem::getScopeCategoryGroup, query.getScopeCategoryGroup())
            .eq(StringUtils.isNotBlank(query.getSchoolType()), ReportRuleItem::getSchoolType, query.getSchoolType())
            .eq(query.getEnabled() != null, ReportRuleItem::getEnabled, query.getEnabled())
            .orderByAsc(ReportRuleItem::getSortOrder)
            .orderByAsc(ReportRuleItem::getId);
    }

    private LambdaQueryWrapper<ActivityReportRule> buildActivityRuleQuery(ActivityReportRule query) {
        return Wrappers.lambdaQuery(ActivityReportRule.class)
            .eq(query.getActivityId() != null, ActivityReportRule::getActivityId, query.getActivityId())
            .eq(query.getPackageId() != null, ActivityReportRule::getPackageId, query.getPackageId())
            .eq(query.getCategoryId() != null, ActivityReportRule::getCategoryId, query.getCategoryId())
            .eq(StringUtils.isNotBlank(query.getRuleGroup()), ActivityReportRule::getRuleGroup, query.getRuleGroup())
            .eq(StringUtils.isNotBlank(query.getRuleType()), ActivityReportRule::getRuleType, query.getRuleType())
            .eq(StringUtils.isNotBlank(query.getScopeCategoryGroup()), ActivityReportRule::getScopeCategoryGroup, query.getScopeCategoryGroup())
            .eq(StringUtils.isNotBlank(query.getSchoolType()), ActivityReportRule::getSchoolType, query.getSchoolType())
            .eq(query.getEnabled() != null, ActivityReportRule::getEnabled, query.getEnabled())
            .orderByAsc(ActivityReportRule::getSortOrder)
            .orderByAsc(ActivityReportRule::getId);
    }

    private ActivityReportRule copyItemToActivityRule(ReportRuleItem item, Long activityId) {
        ActivityReportRule rule = new ActivityReportRule();
        rule.setActivityId(activityId);
        rule.setPackageId(item.getPackageId());
        rule.setPackageItemId(item.getId());
        rule.setRuleCode(item.getRuleCode());
        rule.setRuleName(item.getRuleName());
        rule.setRuleGroup(item.getRuleGroup());
        rule.setRuleType(item.getRuleType());
        rule.setScopeType(item.getScopeType());
        rule.setScopeCategoryGroup(item.getScopeCategoryGroup());
        rule.setCategoryCode(item.getCategoryCode());
        rule.setCategoryId(resolveRuleCategoryId(activityId, item));
        rule.setSchoolType(item.getSchoolType());
        rule.setSchoolId(item.getSchoolId());
        rule.setTargetFieldKey(item.getTargetFieldKey());
        rule.setTargetValue(item.getTargetValue());
        rule.setOperator(item.getOperator());
        rule.setLimitCount(item.getLimitCount());
        rule.setRatioValue(item.getRatioValue());
        rule.setMinValue(item.getMinValue());
        rule.setMaxValue(item.getMaxValue());
        rule.setEnforceMode(item.getEnforceMode());
        rule.setMessage(item.getMessage());
        rule.setRuleJson(item.getRuleJson());
        rule.setEnabled(item.getEnabled());
        rule.setSortOrder(item.getSortOrder());
        rule.setRemark(item.getRemark());
        return rule;
    }

    private Long resolveRuleCategoryId(Long activityId, ReportRuleItem item) {
        if ("ratio".equals(item.getRuleType())
            && StringUtils.isNotBlank(item.getScopeCategoryGroup())
            && "categoryCode".equals(item.getTargetFieldKey())) {
            return null;
        }
        return resolveCategoryId(activityId, item.getCategoryId(), item.getCategoryCode());
    }

    private Long resolveCategoryId(Long activityId, Long categoryId, String categoryCode) {
        if (categoryId != null) {
            ActivityCategory category = categoryMapper.selectById(categoryId);
            if (category != null && activityId.equals(category.getActivityId())) {
                return categoryId;
            }
        }
        if (StringUtils.isBlank(categoryCode)) {
            return null;
        }
        ActivityCategory category = categoryMapper.selectOne(Wrappers.lambdaQuery(ActivityCategory.class)
            .eq(ActivityCategory::getActivityId, activityId)
            .eq(ActivityCategory::getCategoryCode, categoryCode)
            .last("limit 1"), false);
        return category == null ? null : category.getId();
    }

    private void normalizeRule(ReportRuleItem item) {
        if (StringUtils.isBlank(item.getRuleGroup())) {
            item.setRuleGroup("quota");
        }
        if (StringUtils.isBlank(item.getRuleType())) {
            item.setRuleType("count");
        }
        if (StringUtils.isBlank(item.getEnforceMode())) {
            item.setEnforceMode("warn");
        }
        if (item.getEnabled() == null) {
            item.setEnabled(true);
        }
        item.setRuleJson(normalizedRuleJson(item.getRuleJson(), item.getTargetFieldKey(), item.getScopeCategoryGroup(), item.getOperator(), item.getEnforceMode(), item.getMessage()));
    }

    private void normalizeRule(ActivityReportRule rule) {
        if (StringUtils.isBlank(rule.getRuleGroup())) {
            rule.setRuleGroup("quota");
        }
        if (StringUtils.isBlank(rule.getRuleType())) {
            rule.setRuleType("count");
        }
        if (StringUtils.isBlank(rule.getEnforceMode())) {
            rule.setEnforceMode("warn");
        }
        if (rule.getEnabled() == null) {
            rule.setEnabled(true);
        }
        rule.setRuleJson(normalizedRuleJson(rule.getRuleJson(), rule.getTargetFieldKey(), rule.getScopeCategoryGroup(), rule.getOperator(), rule.getEnforceMode(), rule.getMessage()));
    }

    @SuppressWarnings("unchecked")
    private String normalizedRuleJson(String ruleJson, String targetFieldKey, String scopeCategoryGroup, String operator, String enforceMode, String message) {
        Map<String, Object> rule = new LinkedHashMap<>();
        if (StringUtils.isNotBlank(ruleJson) && JsonUtils.isJsonObject(ruleJson)) {
            Map<String, Object> parsed = JsonUtils.parseObject(ruleJson, Map.class);
            if (parsed != null) {
                rule.putAll(parsed);
            }
        }
        putIfNotBlank(rule, "targetFieldKey", targetFieldKey);
        putIfNotBlank(rule, "scopeCategoryGroup", scopeCategoryGroup);
        putIfNotBlank(rule, "ratioOperator", operator);
        putIfNotBlank(rule, "enforceMode", enforceMode);
        putIfNotBlank(rule, "message", message);
        return rule.isEmpty() ? null : JsonUtils.toJsonString(rule);
    }

    private void putIfNotBlank(Map<String, Object> rule, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            rule.put(key, value);
        }
    }
}
