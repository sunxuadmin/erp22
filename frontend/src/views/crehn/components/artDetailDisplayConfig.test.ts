import { describe, expect, it, vi } from 'vitest';
import type { ArtDetailDisplayConfigVO } from '@/api/crehn/detailDisplay';

vi.mock('@/api/crehn/detailDisplay', () => ({
  getArtDetailDisplayConfig: vi.fn(),
  getArtListTableConfig: vi.fn(),
  updateArtDetailDisplayConfig: vi.fn(),
  updateArtListTableConfig: vi.fn()
}));

import {
  WORKSPACE_HEADER_LAYOUT_VERSION,
  defaultSchoolSubmitHomeHeader,
  defaultSchoolSubmitMaximizedHeader,
  defaultWorkspaceHeaderPage,
  normalizeArtDetailDisplayConfig,
  resolveWorkspaceHeaderGridSpans,
  resolveWorkspaceHeaderRuntimeGridSpans
} from './artDetailDisplayConfig';

describe('顶部组件 12 栅格配置', () => {
  it('旧版本工作区表头直接恢复当前默认配置，不再迁移线上旧布局', () => {
    const normalized = normalizeArtDetailDisplayConfig({
      workspaceHeader: {
        layoutVersion: 3,
        pages: {
          audit: {
            ...defaultWorkspaceHeaderPage('audit'),
            titleTemplate: '不应保留的旧标题',
            cards: [
              {
                key: 'current-server-layout',
                rows: [['navigatorToggle', 'search', 'filters', 'columnWidthReset'], []],
                backgroundColor: '#ffffff',
                backgroundOpacity: 100,
                borderRadius: 8,
                borderVisible: true
              }
            ]
          }
        } as ArtDetailDisplayConfigVO['workspaceHeader']['pages']
      }
    });

    expect(normalized.workspaceHeader.layoutVersion).toBe(WORKSPACE_HEADER_LAYOUT_VERSION);
    expect(normalized.workspaceHeader.pages.audit).toEqual(defaultWorkspaceHeaderPage('audit'));
    expect(normalized.workspaceHeader.pages.audit.titleTemplate).not.toBe('不应保留的旧标题');
  });

  it('当前五个页面分别满足单个配置分片长度限制', () => {
    const pages = Object.values(normalizeArtDetailDisplayConfig().workspaceHeader.pages);

    expect(pages).toHaveLength(5);
    pages.forEach((page) => expect(JSON.stringify(page).length).toBeLessThanOrEqual(20_000));
  });

  it('专家评分默认把评审范围放在独立行，并默认关闭范围进度条', () => {
    const normalized = normalizeArtDetailDisplayConfig();
    const review = normalized.workspaceHeader.pages.review;
    const scopeItems = review.cards.flatMap((card) => card.rows.flat()).filter((item) => item === 'reviewScope');

    expect(scopeItems).toEqual(['reviewScope']);
    expect(review.itemConfigs.reviewScope.texts.scopeLabel).toBe('评审范围');
    expect(normalized.reviewWorkbench.scopeNavigator).toEqual({
      visible: true,
      displayMode: 'buttons',
      showCount: true,
      progressVisible: false,
      progressPlacement: 'inside'
    });
  });

  it('当前版本旧评分表头缺少评审范围字段时只迁移一次', () => {
    const legacy = defaultWorkspaceHeaderPage('review');
    delete (legacy.itemConfigs as Partial<typeof legacy.itemConfigs>).reviewScope;
    legacy.cards.forEach((card) => {
      card.rows = card.rows.map((row) => row.filter((item) => item !== 'reviewScope'));
    });
    const migrated = normalizeArtDetailDisplayConfig({
      workspaceHeader: {
        layoutVersion: WORKSPACE_HEADER_LAYOUT_VERSION,
        pages: { review: legacy } as ArtDetailDisplayConfigVO['workspaceHeader']['pages']
      }
    }).workspaceHeader.pages.review;

    expect(migrated.cards.flatMap((card) => card.rows.flat()).filter((item) => item === 'reviewScope')).toEqual(['reviewScope']);

    const removed = defaultWorkspaceHeaderPage('review');
    removed.cards.forEach((card) => {
      card.rows = card.rows.map((row) => row.filter((item) => item !== 'reviewScope'));
    });
    const preservedRemoval = normalizeArtDetailDisplayConfig({
      workspaceHeader: {
        layoutVersion: WORKSPACE_HEADER_LAYOUT_VERSION,
        pages: { review: removed } as ArtDetailDisplayConfigVO['workspaceHeader']['pages']
      }
    }).workspaceHeader.pages.review;

    expect(preservedRemoval.cards.flatMap((card) => card.rows.flat())).not.toContain('reviewScope');
  });

  it('评审范围显示配置归一化按钮下拉和进度条显隐位置', () => {
    const normalized = normalizeArtDetailDisplayConfig({
      reviewWorkbench: {
        scopeNavigator: {
          visible: false,
          displayMode: 'select',
          showCount: false,
          progressVisible: true,
          progressPlacement: 'below'
        }
      } as ArtDetailDisplayConfigVO['reviewWorkbench']
    });

    expect(normalized.reviewWorkbench.scopeNavigator).toEqual({
      visible: false,
      displayMode: 'select',
      showCount: false,
      progressVisible: true,
      progressPlacement: 'below'
    });
  });

  it('自动伸缩组件平分固定组件之外的剩余栅格', () => {
    const page = defaultWorkspaceHeaderPage('audit');
    const row = ['navigatorToggle', 'search', 'categoryFilter', 'groupFilter', 'schoolFilter'];
    const spans = resolveWorkspaceHeaderGridSpans(row, page);

    expect(Object.values(spans).reduce((total, span) => total + span, 0)).toBe(12);
    expect(spans.navigatorToggle).toBe(1);
    expect(spans.search).toBe(4);
    expect([spans.categoryFilter, spans.groupFilter, spans.schoolFilter].sort()).toEqual([2, 2, 3]);
  });

  it('固定空白占指定格数，自动空白吸收剩余格数', () => {
    const page = defaultWorkspaceHeaderPage('audit');
    page.spacerInstances['fixedSpacer-1'] = { type: 'fixedSpacer', span: 2, allowStatusBorrow: false };
    page.spacerInstances['autoSpacer-1'] = { type: 'autoSpacer', span: 1, allowStatusBorrow: false };
    page.itemConfigs.categoryFilter.widthMode = 'fixed';
    page.itemConfigs.categoryFilter.span = 3;
    const spans = resolveWorkspaceHeaderGridSpans(['fixedSpacer-1', 'categoryFilter', 'autoSpacer-1'], page);

    expect(spans['fixedSpacer-1']).toBe(2);
    expect(spans.categoryFilter).toBe(3);
    expect(spans['autoSpacer-1']).toBe(7);
  });

  it('同类空白实例可重复使用，并保留各自固定格数', () => {
    const page = defaultWorkspaceHeaderPage('audit');
    page.spacerInstances = {
      'fixedSpacer-1': { type: 'fixedSpacer', span: 2, allowStatusBorrow: false },
      'fixedSpacer-2': { type: 'fixedSpacer', span: 4, allowStatusBorrow: false },
      'autoSpacer-1': { type: 'autoSpacer', span: 1, allowStatusBorrow: false },
      'autoSpacer-2': { type: 'autoSpacer', span: 1, allowStatusBorrow: false }
    };
    const row = ['fixedSpacer-1', 'fixedSpacer-2', 'autoSpacer-1', 'autoSpacer-2'];
    const spans = resolveWorkspaceHeaderGridSpans(row, page);

    expect(spans['fixedSpacer-1']).toBe(2);
    expect(spans['fixedSpacer-2']).toBe(4);
    expect(spans['autoSpacer-1']).toBe(3);
    expect(spans['autoSpacer-2']).toBe(3);
  });

  it('当前版本重复实例编号自动克隆，并清理未放置的空白配置', () => {
    const page = defaultWorkspaceHeaderPage('audit');
    page.spacerInstances = {
      'fixedSpacer-1': { type: 'fixedSpacer', span: 3, allowStatusBorrow: false },
      'autoSpacer-orphan': { type: 'autoSpacer', span: 1, allowStatusBorrow: false }
    };
    page.cards = [{ ...page.cards[0], rows: [['fixedSpacer-1', 'fixedSpacer-1', 'categoryFilter'], []] }];
    const normalized = normalizeArtDetailDisplayConfig({
      workspaceHeader: {
        layoutVersion: WORKSPACE_HEADER_LAYOUT_VERSION,
        pages: { audit: page } as ArtDetailDisplayConfigVO['workspaceHeader']['pages']
      }
    });
    const audit = normalized.workspaceHeader.pages.audit;
    const fixedIds = audit.cards[0].rows[0].filter((itemId) => audit.spacerInstances[itemId]?.type === 'fixedSpacer');

    expect(fixedIds).toHaveLength(2);
    expect(new Set(fixedIds).size).toBe(2);
    expect(fixedIds.map((itemId) => audit.spacerInstances[itemId].span)).toEqual([3, 3]);
    expect(audit.spacerInstances['autoSpacer-orphan']).toBeUndefined();
  });

  it('学校首页组件使用独立的紧凑 12 栅格表头', () => {
    const page = defaultSchoolSubmitHomeHeader();
    const row = page.cards[0].rows[0];
    const groupId = row.find((itemId) => Boolean(page.compactGroupInstances[itemId]));
    const spans = resolveWorkspaceHeaderGridSpans(row, page);

    expect(page.sidebarEnabled).toBe(false);
    expect(row).toEqual(['categoryFilter', 'status', 'autoSpacer-1', groupId]);
    expect(page.itemConfigs.categoryFilter.widthMode).toBe('fixed');
    expect(page.itemConfigs.categoryFilter.span).toBe(2);
    expect(page.compactGroupInstances[groupId!].items).toEqual(['columnWidthReset', 'navigationButton', 'home']);
    expect(page.compactGroupInstances[groupId!].gap).toBe(10);
    expect(page.compactGroupInstances[groupId!].collapseMode).toBe('none');
    expect(page.compactGroupInstances[groupId!].pinnedItems).toEqual(['navigationButton']);
    expect(page.compactGroupInstances[groupId!].collapseText).toBe('更多');
    expect(Object.values(spans).reduce((total, span) => total + span, 0)).toBe(12);
    expect(page.statusCollapseOnOverflow).toBe(true);
    expect(page.navigationButton.text).toBe('进入统一提交');
    expect(page.navigationButton.presetKey).toBe('schoolSubmit');
    expect(page.homeButton.text).toBe('最大化');
  });

  it('自动空白的状态借用开关默认关闭，不改变既有十二栅格', () => {
    const page = defaultSchoolSubmitHomeHeader();
    const row = page.cards[0].rows[0];
    const regularSpans = resolveWorkspaceHeaderGridSpans(row, page);
    const runtimeSpans = resolveWorkspaceHeaderRuntimeGridSpans(row, page);

    expect(page.spacerInstances['autoSpacer-1'].allowStatusBorrow).toBe(false);
    expect(runtimeSpans).toEqual(regularSpans);
    expect(runtimeSpans.status).toBe(3);
    expect(runtimeSpans['autoSpacer-1']).toBe(3);
  });

  it('开启后只把自动空白自身格数借给紧邻左侧状态组件', () => {
    const page = defaultSchoolSubmitHomeHeader();
    page.spacerInstances['autoSpacer-1'].allowStatusBorrow = true;
    const row = page.cards[0].rows[0];
    const groupId = row.find((itemId) => Boolean(page.compactGroupInstances[itemId]));
    const regularSpans = resolveWorkspaceHeaderGridSpans(row, page);
    const runtimeSpans = resolveWorkspaceHeaderRuntimeGridSpans(row, page);

    expect(regularSpans.status).toBe(3);
    expect(regularSpans['autoSpacer-1']).toBe(3);
    expect(runtimeSpans.status).toBe(6);
    expect(runtimeSpans['autoSpacer-1']).toBe(0);
    expect(runtimeSpans[groupId!]).toBe(4);
    expect(Object.values(runtimeSpans).reduce((total, span) => total + span, 0)).toBe(12);
  });

  it('当前版本保留管理员显式设置的学校类别自动伸缩', () => {
    const currentPage = defaultSchoolSubmitHomeHeader();
    currentPage.itemConfigs.categoryFilter.widthMode = 'auto';
    currentPage.itemConfigs.categoryFilter.span = 3;
    const preserved = normalizeArtDetailDisplayConfig({
      workspaceHeader: {
        layoutVersion: WORKSPACE_HEADER_LAYOUT_VERSION,
        pages: { schoolSubmit: currentPage } as ArtDetailDisplayConfigVO['workspaceHeader']['pages']
      }
    });

    expect(preserved.workspaceHeader.pages.schoolSubmit.itemConfigs.categoryFilter.widthMode).toBe('auto');
    expect(preserved.workspaceHeader.pages.schoolSubmit.itemConfigs.categoryFilter.span).toBe(3);
  });

  it('自动空白不紧邻状态或状态不可见时不借用', () => {
    const page = defaultSchoolSubmitHomeHeader();
    page.spacerInstances['autoSpacer-1'].allowStatusBorrow = true;
    const row = page.cards[0].rows[0];
    const groupId = row.find((itemId) => Boolean(page.compactGroupInstances[itemId]))!;
    const nonAdjacentRow = ['categoryFilter', 'autoSpacer-1', 'status', groupId];

    expect(resolveWorkspaceHeaderRuntimeGridSpans(nonAdjacentRow, page)).toEqual(resolveWorkspaceHeaderGridSpans(nonAdjacentRow, page));

    const statusHidden = (_itemId: string, itemKey: string) => itemKey !== 'status';
    expect(resolveWorkspaceHeaderRuntimeGridSpans(row, page, statusHidden)).toEqual(resolveWorkspaceHeaderGridSpans(row, page, statusHidden));
  });

  it('当前配置缺省时关闭借用，开启值在重复实例归一化后保留', () => {
    const defaultedPage = defaultSchoolSubmitHomeHeader();
    delete (defaultedPage.spacerInstances['autoSpacer-1'] as Partial<{ allowStatusBorrow: boolean }>).allowStatusBorrow;
    const defaultedNormalized = normalizeArtDetailDisplayConfig({
      workspaceHeader: {
        layoutVersion: WORKSPACE_HEADER_LAYOUT_VERSION,
        pages: { schoolSubmit: defaultedPage } as ArtDetailDisplayConfigVO['workspaceHeader']['pages']
      }
    });

    expect(defaultedNormalized.workspaceHeader.pages.schoolSubmit.spacerInstances['autoSpacer-1'].allowStatusBorrow).toBe(false);

    const enabledPage = defaultSchoolSubmitHomeHeader();
    enabledPage.spacerInstances['autoSpacer-1'].allowStatusBorrow = true;
    const row = enabledPage.cards[0].rows[0];
    enabledPage.cards[0].rows[0] = [row[0], row[1], 'autoSpacer-1', 'autoSpacer-1', row[3]];
    const enabledNormalized = normalizeArtDetailDisplayConfig({
      workspaceHeader: {
        layoutVersion: WORKSPACE_HEADER_LAYOUT_VERSION,
        pages: { schoolSubmit: enabledPage } as ArtDetailDisplayConfigVO['workspaceHeader']['pages']
      }
    });
    const autoSpacerIds = enabledNormalized.workspaceHeader.pages.schoolSubmit.cards[0].rows[0].filter(
      (itemId) => enabledNormalized.workspaceHeader.pages.schoolSubmit.spacerInstances[itemId]?.type === 'autoSpacer'
    );

    expect(autoSpacerIds).toHaveLength(2);
    expect(new Set(autoSpacerIds).size).toBe(2);
    expect(autoSpacerIds.map((itemId) => enabledNormalized.workspaceHeader.pages.schoolSubmit.spacerInstances[itemId].allowStatusBorrow)).toEqual([
      true,
      true
    ]);
  });

  it('当前页面缺省时启用状态溢出下拉，也允许显式关闭', () => {
    const currentPage = defaultWorkspaceHeaderPage('audit');
    delete (currentPage as Partial<{ statusCollapseOnOverflow: boolean }>).statusCollapseOnOverflow;
    const defaultedNormalized = normalizeArtDetailDisplayConfig({
      workspaceHeader: {
        layoutVersion: WORKSPACE_HEADER_LAYOUT_VERSION,
        pages: { audit: currentPage } as ArtDetailDisplayConfigVO['workspaceHeader']['pages']
      }
    });
    expect(defaultedNormalized.workspaceHeader.pages.audit.statusCollapseOnOverflow).toBe(true);

    currentPage.statusCollapseOnOverflow = false;
    const disabledNormalized = normalizeArtDetailDisplayConfig({
      workspaceHeader: {
        layoutVersion: WORKSPACE_HEADER_LAYOUT_VERSION,
        pages: { audit: currentPage } as ArtDetailDisplayConfigVO['workspaceHeader']['pages']
      }
    });
    expect(disabledNormalized.workspaceHeader.pages.audit.statusCollapseOnOverflow).toBe(false);
  });

  it('紧凑组折叠配置保留合法常驻按钮并清理非按钮项', () => {
    const page = defaultSchoolSubmitHomeHeader();
    const groupId = page.cards[0].rows[0].find((itemId) => Boolean(page.compactGroupInstances[itemId]))!;
    page.compactGroupInstances[groupId].collapseMode = 'overflow';
    page.compactGroupInstances[groupId].pinnedItems = ['navigationButton', 'status'];
    page.compactGroupInstances[groupId].collapseText = '其他操作';
    const normalized = normalizeArtDetailDisplayConfig({
      workspaceHeader: {
        layoutVersion: WORKSPACE_HEADER_LAYOUT_VERSION,
        pages: { schoolSubmit: page } as ArtDetailDisplayConfigVO['workspaceHeader']['pages']
      }
    });
    const normalizedGroup = normalized.workspaceHeader.pages.schoolSubmit.compactGroupInstances[groupId];

    expect(normalizedGroup.collapseMode).toBe('overflow');
    expect(normalizedGroup.pinnedItems).toEqual(['navigationButton']);
    expect(normalizedGroup.collapseText).toBe('其他操作');
  });

  it('学校最大化表头与独立页默认表头互不共享返回按钮文字', () => {
    const standalone = defaultWorkspaceHeaderPage('schoolSubmit');
    const maximized = defaultSchoolSubmitMaximizedHeader();

    expect(standalone.homeButton.text).toBe('首页');
    expect(maximized.homeButton.text).toBe('退出最大化');
    maximized.homeButton.text = '自定义退出';
    expect(standalone.homeButton.text).toBe('首页');
  });
});
