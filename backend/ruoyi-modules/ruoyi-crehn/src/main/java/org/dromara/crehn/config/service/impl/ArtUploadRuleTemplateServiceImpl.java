package org.dromara.crehn.config.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.config.service.IArtUploadRuleTemplateService;
import org.dromara.crehn.domain.ActivityCategory;
import org.dromara.crehn.domain.CategoryFieldSchema;
import org.dromara.crehn.domain.CategoryFileRequirement;
import org.dromara.crehn.domain.UploadRuleTemplate;
import org.dromara.crehn.domain.bo.UploadRuleTemplateApplyBo;
import org.dromara.crehn.domain.vo.UploadRuleTemplateVo;
import org.dromara.crehn.mapper.ActivityCategoryMapper;
import org.dromara.crehn.mapper.CategoryFieldSchemaMapper;
import org.dromara.crehn.mapper.CategoryFileRequirementMapper;
import org.dromara.crehn.mapper.UploadRuleTemplateMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class ArtUploadRuleTemplateServiceImpl implements IArtUploadRuleTemplateService {

    private final UploadRuleTemplateMapper templateMapper;
    private final CategoryFieldSchemaMapper fieldMapper;
    private final CategoryFileRequirementMapper fileRequirementMapper;
    private final ActivityCategoryMapper categoryMapper;

    @Override
    public TableDataInfo<UploadRuleTemplateVo> queryPage(UploadRuleTemplate query, PageQuery pageQuery) {
        Page<UploadRuleTemplateVo> page = templateMapper.selectVoPage(pageQuery.build(), buildQuery(query));
        return TableDataInfo.build(page);
    }

    @Override
    public List<UploadRuleTemplateVo> queryList(UploadRuleTemplate query) {
        return templateMapper.selectVoList(buildQuery(query));
    }

    @Override
    public UploadRuleTemplateVo getInfo(Long id) {
        return templateMapper.selectVoById(id);
    }

    @Override
    public int save(UploadRuleTemplate template) {
        normalize(template);
        if (template.getId() == null) {
            return templateMapper.insert(template);
        }
        return templateMapper.updateById(template);
    }

    @Override
    public UploadRuleTemplateVo copy(Long id) {
        UploadRuleTemplate source = requireTemplate(id);
        UploadRuleTemplate copy = BeanUtil.copyProperties(source, UploadRuleTemplate.class);
        copy.setId(null);
        copy.setTemplateCode(copy.getTemplateCode() + "_copy_" + System.currentTimeMillis());
        copy.setTemplateName(copy.getTemplateName() + " 副本");
        templateMapper.insert(copy);
        return templateMapper.selectVoById(copy.getId());
    }

    @Override
    public UploadRuleTemplateVo saveFromCategory(Long categoryId, String templateName) {
        ActivityCategory category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new ServiceException("类别不存在");
        }
        List<CategoryFieldSchema> fields = fieldMapper.selectList(Wrappers.lambdaQuery(CategoryFieldSchema.class)
            .eq(CategoryFieldSchema::getCategoryId, categoryId)
            .orderByAsc(CategoryFieldSchema::getSortOrder)
            .orderByAsc(CategoryFieldSchema::getId));
        List<CategoryFileRequirement> requirements = fileRequirementMapper.selectList(Wrappers.lambdaQuery(CategoryFileRequirement.class)
            .eq(CategoryFileRequirement::getCategoryId, categoryId)
            .orderByAsc(CategoryFileRequirement::getSortOrder)
            .orderByAsc(CategoryFileRequirement::getId));
        fields.forEach(field -> {
            field.setId(null);
            field.setCategoryId(null);
        });
        requirements.forEach(requirement -> {
            requirement.setId(null);
            requirement.setCategoryId(null);
        });
        String templateCode = categoryTemplateCode(category);
        UploadRuleTemplate template = templateMapper.selectOne(Wrappers.lambdaQuery(UploadRuleTemplate.class)
            .eq(UploadRuleTemplate::getTemplateCode, templateCode)
            .last("limit 1"));
        if (template == null) {
            template = new UploadRuleTemplate();
            template.setTemplateCode(templateCode);
        }
        template.setTemplateName(StringUtils.blankToDefault(templateName, category.getCategoryName() + "模板"));
        template.setCategoryGroup(category.getCategoryGroup());
        template.setFieldSchemaJson(JsonUtils.toJsonString(fields));
        template.setFileRequirementJson(JsonUtils.toJsonString(requirements));
        template.setRuleJson(ruleJsonWithTemplateCode(category.getRuleJson(), templateCode));
        template.setTipText(category.getTipText());
        template.setCheckMode(category.getCheckMode());
        template.setEnabled(true);
        template.setSortOrder(category.getSortOrder() == null ? 0 : category.getSortOrder());
        if (template.getId() == null) {
            templateMapper.insert(template);
        } else {
            templateMapper.updateById(template);
        }
        category.setRuleJson(ruleJsonWithTemplateCode(category.getRuleJson(), templateCode));
        categoryMapper.updateById(category);
        return templateMapper.selectVoById(template.getId());
    }

    @Override
    public List<UploadRuleTemplateVo> syncFromActivityCategories(Long activityId) {
        if (activityId == null) {
            throw new ServiceException("活动ID不能为空");
        }
        List<ActivityCategory> categories = categoryMapper.selectList(Wrappers.lambdaQuery(ActivityCategory.class)
            .eq(ActivityCategory::getActivityId, activityId)
            .eq(ActivityCategory::getEnabled, true)
            .isNotNull(ActivityCategory::getCategoryCode)
            .orderByAsc(ActivityCategory::getSortOrder)
            .orderByAsc(ActivityCategory::getId));
        if (categories.isEmpty()) {
            throw new ServiceException("未找到已启用的活动类别");
        }
        return categories.stream()
            .map(category -> saveFromCategory(category.getId(), category.getCategoryName() + "上传规则模板"))
            .toList();
    }

    @Override
    public void applyToCategory(UploadRuleTemplateApplyBo bo) {
        if (bo.getTemplateId() == null || bo.getCategoryId() == null) {
            throw new ServiceException("模板ID和类别ID不能为空");
        }
        UploadRuleTemplate template = requireTemplate(bo.getTemplateId());
        ActivityCategory category = categoryMapper.selectById(bo.getCategoryId());
        if (category == null) {
            throw new ServiceException("类别不存在");
        }
        String mode = StringUtils.blankToDefault(bo.getApplyMode(), "append");
        if ("overwrite".equals(mode)) {
            fieldMapper.delete(Wrappers.lambdaQuery(CategoryFieldSchema.class).eq(CategoryFieldSchema::getCategoryId, bo.getCategoryId()));
            fileRequirementMapper.delete(Wrappers.lambdaQuery(CategoryFileRequirement.class).eq(CategoryFileRequirement::getCategoryId, bo.getCategoryId()));
        }
        if ("append".equals(mode) || "overwrite".equals(mode) || "fields_only".equals(mode)) {
            insertFields(bo.getCategoryId(), template.getFieldSchemaJson());
        }
        if ("append".equals(mode) || "overwrite".equals(mode) || "files_only".equals(mode)) {
            insertRequirements(bo.getCategoryId(), template.getFileRequirementJson());
        }
        if ("append".equals(mode) || "overwrite".equals(mode) || "rules_only".equals(mode)) {
            category.setCategoryGroup(template.getCategoryGroup());
            category.setRuleJson(template.getRuleJson());
            category.setTipText(template.getTipText());
            category.setCheckMode(template.getCheckMode());
            categoryMapper.updateById(category);
        }
    }

    @Override
    public int delete(Long[] ids) {
        return templateMapper.deleteByIds(Arrays.asList(ids));
    }

    private void insertFields(Long categoryId, String json) {
        if (StringUtils.isBlank(json)) {
            return;
        }
        List<CategoryFieldSchema> fields = JsonUtils.parseObject(json, new TypeReference<List<CategoryFieldSchema>>() {
        });
        if (fields == null) {
            return;
        }
        for (CategoryFieldSchema field : fields) {
            field.setId(null);
            field.setCategoryId(categoryId);
            fieldMapper.insert(field);
        }
    }

    private void insertRequirements(Long categoryId, String json) {
        if (StringUtils.isBlank(json)) {
            return;
        }
        List<CategoryFileRequirement> requirements = JsonUtils.parseObject(json, new TypeReference<List<CategoryFileRequirement>>() {
        });
        if (requirements == null) {
            return;
        }
        for (CategoryFileRequirement requirement : requirements) {
            requirement.setId(null);
            requirement.setCategoryId(categoryId);
            fileRequirementMapper.insert(requirement);
        }
    }

    private UploadRuleTemplate requireTemplate(Long id) {
        UploadRuleTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new ServiceException("模板不存在");
        }
        return template;
    }

    private String categoryTemplateCode(ActivityCategory category) {
        String ruleTemplateCode = templateCodeFromRuleJson(category.getRuleJson());
        if (StringUtils.isNotBlank(ruleTemplateCode)) {
            return ruleTemplateCode;
        }
        String code = StringUtils.blankToDefault(category.getCategoryCode(), String.valueOf(category.getId()));
        return "tpl_" + code.replaceAll("[^A-Za-z0-9_\\-]", "_");
    }

    private String templateCodeFromRuleJson(String ruleJson) {
        if (StringUtils.isBlank(ruleJson)) {
            return null;
        }
        try {
            Map<String, Object> parsed = JsonUtils.parseObject(ruleJson, new TypeReference<Map<String, Object>>() {
            });
            Object templateCode = parsed == null ? null : parsed.get("templateCode");
            return templateCode == null ? null : String.valueOf(templateCode);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String ruleJsonWithTemplateCode(String ruleJson, String templateCode) {
        Map<String, Object> rule = new LinkedHashMap<>();
        if (StringUtils.isNotBlank(ruleJson)) {
            try {
                Map<String, Object> parsed = JsonUtils.parseObject(ruleJson, new TypeReference<Map<String, Object>>() {
                });
                if (parsed != null) {
                    rule.putAll(parsed);
                }
            } catch (Exception ignored) {
                // Keep the template binding usable even when old category rule JSON is malformed.
            }
        }
        rule.put("templateCode", templateCode);
        return JsonUtils.toJsonString(rule);
    }

    private void normalize(UploadRuleTemplate template) {
        if (StringUtils.isBlank(template.getTemplateName())) {
            throw new ServiceException("模板名称不能为空");
        }
        if (StringUtils.isBlank(template.getTemplateCode())) {
            template.setTemplateCode("tpl_" + System.currentTimeMillis());
        }
        if (template.getEnabled() == null) {
            template.setEnabled(true);
        }
        if (template.getSortOrder() == null) {
            template.setSortOrder(0);
        }
    }

    private LambdaQueryWrapper<UploadRuleTemplate> buildQuery(UploadRuleTemplate query) {
        return Wrappers.lambdaQuery(UploadRuleTemplate.class)
            .like(StringUtils.isNotBlank(query.getTemplateName()), UploadRuleTemplate::getTemplateName, query.getTemplateName())
            .eq(StringUtils.isNotBlank(query.getTemplateCode()), UploadRuleTemplate::getTemplateCode, query.getTemplateCode())
            .eq(StringUtils.isNotBlank(query.getCategoryGroup()), UploadRuleTemplate::getCategoryGroup, query.getCategoryGroup())
            .eq(query.getEnabled() != null, UploadRuleTemplate::getEnabled, query.getEnabled())
            .orderByAsc(UploadRuleTemplate::getSortOrder)
            .orderByDesc(UploadRuleTemplate::getCreateTime);
    }
}
