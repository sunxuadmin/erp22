import { describe, expect, it, vi } from 'vitest';

vi.mock('@/router', () => ({
  default: { addRoute: vi.fn() },
  constantRoutes: [],
  dynamicRoutes: []
}));
vi.mock('@/store', () => ({ default: {} }));
vi.mock('@/api/menu', () => ({ getRouters: vi.fn() }));
vi.mock('@/api/crehn/activity', () => ({
  listAvailableActivity: vi.fn(),
  listSchoolCategoryCatalog: vi.fn()
}));
vi.mock('@/plugins/auth', () => ({ default: { hasPermi: vi.fn(), hasPermiOr: vi.fn(), hasRoleOr: vi.fn() } }));
vi.mock('@/store/modules/user', () => ({ useUserStore: vi.fn() }));
vi.mock('element-plus', () => ({ ElNotification: vi.fn() }));

import {
  buildParticipantReportingMenuForCatalogs,
  buildParticipantReportingMenusForCatalogs,
  canUseParticipantProjectMenus,
  isParticipantProjectUser,
  stripStaticActivityCategoryMenus
} from './permission';

const visibleGroup = { id: 'group-performance', categoryCode: 'performance', categoryName: '表演类', enabled: true, sortOrder: 1, ruleJson: '{"categoryNodeType":"group","menuVisible":true}' };
const visibleCategory = {
  id: 'category-vocal',
  parentId: 'group-performance',
  categoryCode: 'vocal',
  categoryName: '声乐',
  enabled: true,
  sortOrder: 1,
  ruleJson: '{"menuVisible":true}'
};

describe('CREHN role-aware activity menus', () => {
  it('injects activity categories only for participant users with their own project-add permission', () => {
    expect(isParticipantProjectUser(['crehn_participant'], true)).toBe(true);
    expect(isParticipantProjectUser(['crehn_participant'], false)).toBe(false);
    expect(isParticipantProjectUser(['crehn_school'], true)).toBe(false);
    expect(isParticipantProjectUser(['crehn_reviewer'], true)).toBe(false);
    expect(isParticipantProjectUser(['crehn_admin', 'crehn_participant'], true)).toBe(false);
    expect(isParticipantProjectUser(['admin', 'crehn_participant'], true)).toBe(false);
    expect(isParticipantProjectUser(['crehn_participant', 'crehn_reviewer'], true)).toBe(false);
    expect(isParticipantProjectUser(['crehn_participant', 'crehn_cms_editor'], true)).toBe(false);
    expect(canUseParticipantProjectMenus(['crehn_participant'], true, 'school-1')).toBe(true);
    expect(canUseParticipantProjectMenus(['crehn_participant'], true, '')).toBe(false);
  });

  it('removes only exact M70 category routes and preserves common or same-name future menus', () => {
    const menus = stripStaticActivityCategoryMenus([
      { path: 'all', component: 'crehn/project/index', meta: { title: '全部项目' } },
      { path: 'project', component: 'crehn/project/index', meta: { title: '我的项目' } },
      { path: 'future-vocal', component: 'crehn/project/index', query: '{"categoryCode":"performance_vocal"}', meta: { title: '声乐' } },
      {
        path: 'artwork',
        component: 'ParentView',
        meta: { title: '艺术作品类' },
        children: [{ path: 'design-exhibition', component: 'crehn/project/index', meta: { title: '大艺展设计类' } }]
      }
    ] as any, 'crehn');

    expect(menus.map((item) => item.path)).toEqual(['all', 'project', 'future-vocal']);

    const performanceChildren = stripStaticActivityCategoryMenus(
      [
        { path: 'vocal', component: 'crehn/project/index', query: '{"categoryCode":"performance_vocal"}', meta: { title: '声乐' } },
        { path: 'vocal', component: 'crehn/project/index', query: '{"categoryCode":"future_vocal"}', meta: { title: '声乐' } }
      ] as any,
      'performance'
    );
    expect(performanceChildren).toHaveLength(1);
    expect(performanceChildren[0].query).toBe('{"categoryCode":"future_vocal"}');
  });

  it('places one or many activity trees under the stable project-submission parent', () => {
    const singleCatalogs = [
      { activity: { id: '2026', activityName: '2026 创意河南' }, categories: [visibleGroup, visibleCategory], index: 0 }
    ];
    const multipleCatalogs = [
      { activity: { id: '2026', activityName: '2026 创意河南' }, categories: [visibleGroup, visibleCategory], index: 0 },
      { activity: { id: '2027', activityName: '2027 创意河南' }, categories: [visibleGroup, visibleCategory], index: 1 }
    ];
    const single = buildParticipantReportingMenuForCatalogs(singleCatalogs);
    const multiple = buildParticipantReportingMenuForCatalogs(multipleCatalogs);

    expect(single?.path).toBe('project-submission');
    expect(single?.children?.[0].path).toContain('group-performance');
    expect(single?.children?.[0].children?.[0].path).toContain('category-vocal');
    expect(multiple?.path).toBe('project-submission');
    expect(multiple?.children?.map((item) => item.path)).toEqual(['activity-2026', 'activity-2027']);
    expect(multiple?.children?.[0].children?.[0].path).toContain('group-performance');
    expect(buildParticipantReportingMenusForCatalogs(singleCatalogs)[0].path).toContain('group-performance');
  });

  it('does not generate menus for hidden, disabled, or empty category groups', () => {
    const hiddenGroup = { ...visibleGroup, id: 'hidden', ruleJson: '{"categoryNodeType":"group","menuVisible":false}' };
    const disabledGroup = { ...visibleGroup, id: 'disabled', enabled: false };
    const disabledCategory = { ...visibleCategory, id: 'disabled-category', parentId: 'disabled', enabled: false };
    const emptyGroup = { ...visibleGroup, id: 'empty', categoryCode: 'empty-group' };

    const menus = buildParticipantReportingMenusForCatalogs([
      {
        activity: { id: '2026', activityName: '2026 创意河南' },
        categories: [hiddenGroup, { ...visibleCategory, parentId: 'hidden' }, disabledGroup, disabledCategory, emptyGroup],
        index: 0
      }
    ]);

    expect(menus).toEqual([]);
  });
});
