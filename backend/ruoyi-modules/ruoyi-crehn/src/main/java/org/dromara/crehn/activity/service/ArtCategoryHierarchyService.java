package org.dromara.crehn.activity.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.domain.ActivityCategory;
import org.dromara.crehn.domain.vo.ActivityCategoryVo;
import org.dromara.crehn.mapper.ActivityCategoryMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 活动类别层级与学校端业务可用性判定。
 *
 * <p>新数据使用 parent_id 表达真实大类；旧数据继续兼容 category_group 文本分组。</p>
 */
@RequiredArgsConstructor
@Service
public class ArtCategoryHierarchyService {

    public static final String NODE_TYPE_KEY = "categoryNodeType";
    public static final String NODE_TYPE_GROUP = "group";
    public static final String MENU_VISIBLE_KEY = "menuVisible";

    private final ActivityCategoryMapper categoryMapper;

    public boolean isGroup(ActivityCategory category) {
        return category != null && isGroupRule(category.getRuleJson());
    }

    public boolean isGroup(ActivityCategoryVo category) {
        return category != null && isGroupRule(category.getRuleJson());
    }

    public boolean isMenuVisible(ActivityCategoryVo category) {
        return category != null && !Boolean.FALSE.equals(ruleOptions(category.getRuleJson()).get(MENU_VISIBLE_KEY));
    }

    public void normalizeForSave(ActivityCategory category) {
        if (category == null) {
            throw new ServiceException("类别信息不能为空");
        }
        if (category.getActivityId() == null) {
            throw new ServiceException("活动ID不能为空");
        }
        if (StringUtils.isBlank(category.getCategoryCode())) {
            throw new ServiceException("类别编码不能为空");
        }
        if (StringUtils.isBlank(category.getCategoryName())) {
            throw new ServiceException("类别名称不能为空");
        }
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        if (category.getEnabled() == null) {
            category.setEnabled(true);
        }
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        if (isGroup(category)) {
            category.setParentId(0L);
            category.setCategoryGroup(category.getCategoryName());
            return;
        }
        if (category.getParentId() == 0L) {
            return;
        }
        ActivityCategory parent = categoryMapper.selectById(category.getParentId());
        if (parent == null || !Objects.equals(parent.getActivityId(), category.getActivityId()) || !isGroup(parent)) {
            throw new ServiceException("所属大类不存在或不是有效大类");
        }
        if (Objects.equals(parent.getId(), category.getId())) {
            throw new ServiceException("类别不能归属到自身");
        }
        category.setCategoryGroup(parent.getCategoryName());
    }

    public void requireBusinessAvailableLeaf(ActivityCategory category) {
        if (category == null || !Boolean.TRUE.equals(category.getEnabled()) || isGroup(category)) {
            throw new ServiceException("类别不存在、已停用或不是可报送类别");
        }
        ActivityCategory parent = resolveParentGroup(category);
        if (parent != null && !Boolean.TRUE.equals(parent.getEnabled())) {
            throw new ServiceException("所属大类已停用");
        }
        if (category.getParentId() != null && category.getParentId() > 0L && parent == null) {
            throw new ServiceException("所属大类不存在或已删除");
        }
    }

    public boolean isBusinessAvailableLeaf(ActivityCategoryVo category, List<ActivityCategoryVo> catalog) {
        if (category == null || !Boolean.TRUE.equals(category.getEnabled()) || isGroup(category)) {
            return false;
        }
        ActivityCategoryVo parent = resolveParentGroup(category, catalog);
        if (category.getParentId() != null && category.getParentId() > 0L && parent == null) {
            return false;
        }
        return parent == null || Boolean.TRUE.equals(parent.getEnabled());
    }

    public boolean hasChildren(ActivityCategory group) {
        if (!isGroup(group) || group.getId() == null || group.getActivityId() == null) {
            return false;
        }
        List<ActivityCategory> categories = categoryMapper.selectList(Wrappers.lambdaQuery(ActivityCategory.class)
            .eq(ActivityCategory::getActivityId, group.getActivityId()));
        return categories.stream().anyMatch(category ->
            !Objects.equals(category.getId(), group.getId())
                && !isGroup(category)
                && (Objects.equals(category.getParentId(), group.getId())
                || isLegacyChildOf(category.getCategoryGroup(), category.getCategoryName(), group.getCategoryName()))
        );
    }

    private ActivityCategory resolveParentGroup(ActivityCategory category) {
        if (category.getParentId() != null && category.getParentId() > 0L) {
            ActivityCategory parent = categoryMapper.selectById(category.getParentId());
            return parent != null && isGroup(parent) && Objects.equals(parent.getActivityId(), category.getActivityId()) ? parent : null;
        }
        if (!isLegacyGroupedLeaf(category.getCategoryGroup(), category.getCategoryName())) {
            return null;
        }
        return categoryMapper.selectList(Wrappers.lambdaQuery(ActivityCategory.class)
            .eq(ActivityCategory::getActivityId, category.getActivityId())
            .eq(ActivityCategory::getCategoryName, category.getCategoryGroup())
            .orderByAsc(ActivityCategory::getId))
            .stream()
            .filter(this::isGroup)
            .findFirst()
            .orElse(null);
    }

    private ActivityCategoryVo resolveParentGroup(ActivityCategoryVo category, List<ActivityCategoryVo> catalog) {
        if (catalog == null || catalog.isEmpty()) {
            return null;
        }
        if (category.getParentId() != null && category.getParentId() > 0L) {
            return catalog.stream()
                .filter(this::isGroup)
                .filter(item -> Objects.equals(item.getId(), category.getParentId()))
                .findFirst()
                .orElse(null);
        }
        if (!isLegacyGroupedLeaf(category.getCategoryGroup(), category.getCategoryName())) {
            return null;
        }
        return catalog.stream()
            .filter(this::isGroup)
            .filter(item -> Objects.equals(item.getCategoryName(), category.getCategoryGroup()))
            .findFirst()
            .orElse(null);
    }

    private boolean isGroupRule(String ruleJson) {
        return NODE_TYPE_GROUP.equals(String.valueOf(ruleOptions(ruleJson).get(NODE_TYPE_KEY)));
    }

    private boolean isLegacyGroupedLeaf(String categoryGroup, String categoryName) {
        return StringUtils.isNotBlank(categoryGroup) && !Objects.equals(categoryGroup, categoryName);
    }

    private boolean isLegacyChildOf(String categoryGroup, String categoryName, String groupName) {
        return isLegacyGroupedLeaf(categoryGroup, categoryName) && Objects.equals(categoryGroup, groupName);
    }

    private Map<String, Object> ruleOptions(String ruleJson) {
        if (StringUtils.isBlank(ruleJson) || !JsonUtils.isJsonObject(ruleJson)) {
            return Map.of();
        }
        Map<String, Object> parsed = JsonUtils.parseObject(ruleJson, new TypeReference<Map<String, Object>>() {
        });
        return parsed == null ? Map.of() : parsed;
    }
}
