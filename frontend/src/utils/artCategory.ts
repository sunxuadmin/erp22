import type { ActivityCategoryVO } from '@/api/crehn/types';

export const CATEGORY_NODE_TYPE_KEY = 'categoryNodeType';
export const CATEGORY_NODE_TYPE_GROUP = 'group';
export const CATEGORY_NODE_TYPE_CATEGORY = 'category';
export const CATEGORY_MENU_VISIBLE_KEY = 'menuVisible';

export type ActivityCategoryTreeNode = ActivityCategoryVO & {
  children?: ActivityCategoryTreeNode[];
  virtualGroup?: boolean;
};

export const parseCategoryRule = (ruleJson?: string): Record<string, any> => {
  if (!ruleJson) return {};
  try {
    const parsed = JSON.parse(ruleJson);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {};
  } catch {
    return {};
  }
};

export const isCategoryGroup = (category?: ActivityCategoryVO) =>
  String(parseCategoryRule(category?.ruleJson)[CATEGORY_NODE_TYPE_KEY] || '') === CATEGORY_NODE_TYPE_GROUP;

export const isCategoryMenuVisible = (category?: ActivityCategoryVO) => parseCategoryRule(category?.ruleJson)[CATEGORY_MENU_VISIBLE_KEY] !== false;

export const isRootCategory = (category?: ActivityCategoryVO) => {
  const parentId = category?.parentId;
  return parentId === undefined || parentId === null || parentId === '' || String(parentId) === '0';
};

export const isLegacyGroupedCategory = (category?: ActivityCategoryVO) => {
  const groupName = String(category?.categoryGroup || '').trim();
  const categoryName = String(category?.categoryName || '').trim();
  return !isCategoryGroup(category) && isRootCategory(category) && !!groupName && groupName !== categoryName;
};

export const categorySortValue = (category?: ActivityCategoryVO) => {
  const value = Number(category?.sortOrder);
  return Number.isFinite(value) ? value : 9999;
};

export const sortCategoryNodes = <T extends ActivityCategoryVO>(categories: T[]): T[] =>
  [...categories].sort((a, b) => {
    const orderDiff = categorySortValue(a) - categorySortValue(b);
    if (orderDiff !== 0) return orderDiff;
    const aId = Number(a.id);
    const bId = Number(b.id);
    if (Number.isFinite(aId) && Number.isFinite(bId)) {
      return aId - bId;
    }
    return String(a.id || a.categoryName || '').localeCompare(String(b.id || b.categoryName || ''), 'zh-CN');
  });

export const buildActivityCategoryTree = (categories: ActivityCategoryVO[], includeVirtualGroups = true): ActivityCategoryTreeNode[] => {
  const actualGroups = categories.filter(isCategoryGroup).map((item) => ({ ...item, children: [] }) as ActivityCategoryTreeNode);
  const groupById = new Map(actualGroups.filter((item) => item.id !== undefined && item.id !== null).map((item) => [String(item.id), item]));
  const groupByName = new Map(actualGroups.map((item) => [String(item.categoryName || '').trim(), item]));
  const roots: ActivityCategoryTreeNode[] = [...actualGroups];
  const virtualGroups = new Map<string, ActivityCategoryTreeNode>();

  categories
    .filter((item) => !isCategoryGroup(item))
    .forEach((category) => {
      const node: ActivityCategoryTreeNode = { ...category };
      const parent = !isRootCategory(category) ? groupById.get(String(category.parentId)) : undefined;
      const legacyParent = !parent && isLegacyGroupedCategory(category) ? groupByName.get(String(category.categoryGroup || '').trim()) : undefined;
      if (parent || legacyParent) {
        const resolvedParent = parent || legacyParent!;
        resolvedParent.children = resolvedParent.children || [];
        resolvedParent.children.push(node);
        return;
      }
      if (includeVirtualGroups && isLegacyGroupedCategory(category)) {
        const groupName = String(category.categoryGroup || '').trim();
        let virtualGroup = virtualGroups.get(groupName);
        if (!virtualGroup) {
          virtualGroup = {
            id: `legacy-group:${groupName}`,
            activityId: category.activityId,
            parentId: 0,
            categoryCode: `legacy_group_${virtualGroups.size + 1}`,
            categoryName: groupName,
            categoryGroup: groupName,
            sortOrder: categorySortValue(category),
            enabled: true,
            ruleJson: JSON.stringify({ [CATEGORY_NODE_TYPE_KEY]: CATEGORY_NODE_TYPE_GROUP, [CATEGORY_MENU_VISIBLE_KEY]: true }),
            virtualGroup: true,
            children: []
          };
          virtualGroups.set(groupName, virtualGroup);
          roots.push(virtualGroup);
        }
        virtualGroup.sortOrder = Math.min(categorySortValue(virtualGroup), categorySortValue(category));
        virtualGroup.children!.push(node);
        return;
      }
      roots.push(node);
    });

  return sortCategoryNodes(roots).map((node) => ({
    ...node,
    children: node.children?.length ? sortCategoryNodes(node.children) : undefined
  }));
};

export const findCategoryParentGroup = (category: ActivityCategoryVO, catalog: ActivityCategoryVO[]) => {
  const groups = catalog.filter(isCategoryGroup);
  if (!isRootCategory(category)) {
    return groups.find((item) => String(item.id) === String(category.parentId));
  }
  if (!isLegacyGroupedCategory(category)) return undefined;
  return groups.find((item) => String(item.categoryName || '').trim() === String(category.categoryGroup || '').trim());
};

export const isCategoryBusinessAvailable = (category: ActivityCategoryVO, catalog: ActivityCategoryVO[]) => {
  if (isCategoryGroup(category) || category.enabled === false) return false;
  const parent = findCategoryParentGroup(category, catalog);
  if (!isRootCategory(category) && !parent) return false;
  return parent?.enabled !== false;
};

export const isCategoryMenuAvailable = (category: ActivityCategoryVO, catalog: ActivityCategoryVO[]) => {
  if (!isCategoryBusinessAvailable(category, catalog) || !isCategoryMenuVisible(category)) return false;
  const parent = findCategoryParentGroup(category, catalog);
  return !parent || (parent.enabled !== false && isCategoryMenuVisible(parent));
};

export const buildSchoolCategoryMenuTree = (categories: ActivityCategoryVO[]): ActivityCategoryTreeNode[] =>
  buildActivityCategoryTree(categories, true)
    .map((node) => {
      if (!isCategoryGroup(node)) {
        return isCategoryMenuAvailable(node, categories) ? node : undefined;
      }
      if (node.enabled === false || !isCategoryMenuVisible(node)) {
        return undefined;
      }
      const children = sortCategoryNodes((node.children || []).filter((category) => isCategoryMenuAvailable(category, categories)));
      return children.length ? { ...node, children } : undefined;
    })
    .filter((node): node is ActivityCategoryTreeNode => Boolean(node));

export const updateCategoryRuleMeta = (
  ruleJson: string | undefined,
  nodeType: typeof CATEGORY_NODE_TYPE_GROUP | typeof CATEGORY_NODE_TYPE_CATEGORY,
  menuVisible: boolean
) => {
  const rule = parseCategoryRule(ruleJson);
  rule[CATEGORY_NODE_TYPE_KEY] = nodeType;
  rule[CATEGORY_MENU_VISIBLE_KEY] = menuVisible;
  return JSON.stringify(rule);
};
