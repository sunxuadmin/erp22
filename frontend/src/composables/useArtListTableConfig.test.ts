import { describe, expect, it } from 'vitest';
import type { ArtListTableAppearanceConfig, ArtListTablePageConfig, ArtReviewListColumnConfig } from '@/api/crehn/detailDisplay';
import {
  ART_LIST_TABLE_LAYOUT_VERSION,
  artReviewGroupDisplayText,
  migrateLegacyActionFixedRight,
  migrateLegacyReviewDefaultColumns,
  reviewListBuiltinColumnOptions
} from '@/views/crehn/components/artListTableDefaults';
import {
  artListActionColumnMinimumWidth,
  artListSerialColumn,
  artListStatusColumnMinimumWidth,
  toArtListRuntimeColumns
} from './artListTableRuntime';
import { resolveArtTableFitWidths, type ArtResizableColumn } from './useArtTableColumnWidths';

const pageConfig = (): ArtListTablePageConfig =>
  ({
    columns: [
      { key: 'projectName', source: 'builtin', label: '项目名称', visible: true, width: 180, minWidth: 120, fixed: 'none' },
      { key: 'actions', source: 'builtin', label: '操作', visible: true, width: 110, minWidth: 110, fixed: 'right' }
    ],
    selectionFixed: 'none',
    serialVisible: true,
    serialWidth: 66,
    serialFixed: 'none',
    statusLabels: {
      draft: '草稿',
      pending: '待审核',
      approved: '已通过',
      returned: '已退回',
      unscored: '未评分',
      scored: '已评分',
      scoreDraft: '完成评审',
      locked: '他人评分'
    },
    actionLabels: {
      view: '查看评分',
      score: '开始评分',
      edit: '修改评分',
      return: '退回',
      withdraw: '撤回',
      submit: '提交',
      delete: '删除'
    }
  }) as ArtListTablePageConfig;

const appearanceConfig = (): ArtListTableAppearanceConfig =>
  ({
    bodyFontSize: 14,
    buttonFontSize: 14,
    statusAppearance: {
      iconVisible: false
    },
    rowActionAppearance: {
      iconVisible: true,
      uniformWidth: true
    }
  }) as ArtListTableAppearanceConfig;

describe('全局业务表格运行列规则', () => {
  it('按后台配置固定操作列且固定列不参与宽度拖动', () => {
    const page = pageConfig();
    const columns = toArtListRuntimeColumns('projectView', page.columns, page, appearanceConfig());
    const actions = columns.find((column) => column.key === 'actions');
    const projectName = columns.find((column) => column.key === 'projectName');

    expect(actions).toMatchObject({ fixed: 'right', resizable: false });
    expect(projectName).toMatchObject({ fixed: false, resizable: true });
  });

  it('操作列取消固定后进入中间区域并保持不可拖动宽度', () => {
    const page = pageConfig();
    page.columns[1].fixed = 'none';
    const actions = toArtListRuntimeColumns('projectView', page.columns, page, appearanceConfig()).find((column) => column.key === 'actions');

    expect(actions).toMatchObject({ fixed: false, resizable: false });
  });

  it('固定列按左侧、中间、右侧连续分区，操作列在所在分区保持最后', () => {
    const page = pageConfig();
    page.columns = [
      { key: 'actions', source: 'builtin', label: '操作', visible: true, width: 110, minWidth: 110, fixed: 'right' },
      { key: 'status', source: 'builtin', label: '状态', visible: true, width: 90, minWidth: 70, fixed: 'right' },
      { key: 'projectName', source: 'builtin', label: '项目名称', visible: true, width: 180, minWidth: 120, fixed: 'none' },
      { key: 'projectNo', source: 'builtin', label: '项目编号', visible: true, width: 130, minWidth: 90, fixed: 'left' }
    ];

    expect(toArtListRuntimeColumns('projectView', page.columns, page, appearanceConfig()).map((column) => column.key)).toEqual([
      'projectNo',
      'projectName',
      'status',
      'actions'
    ]);
  });

  it('操作文字变长时同步增加安全宽度，避免按钮换行挤压', () => {
    const page = pageConfig();
    const appearance = appearanceConfig();
    const defaultWidth = artListActionColumnMinimumWidth('review', page, appearance);
    page.actionLabels.view = '查看专家评分详情';
    const longerWidth = artListActionColumnMinimumWidth('review', page, appearance);

    expect(longerWidth).toBeGreaterThan(defaultWidth);
    const actions = toArtListRuntimeColumns('review', page.columns, page, appearance).find((column) => column.key === 'actions');
    expect(actions?.width).toBeGreaterThanOrEqual(longerWidth);
  });

  it('四类多按钮页面完整计入图标、间距和单元格余量，避免末项被右边界裁切', () => {
    const page = pageConfig();
    const appearance = appearanceConfig();
    Object.assign(page.actionLabels, {
      view: '查看',
      edit: '编辑',
      return: '退回',
      withdraw: '撤回',
      submit: '提交',
      delete: '删除'
    });

    expect(artListActionColumnMinimumWidth('project', page, appearance)).toBe(226);
    expect(artListActionColumnMinimumWidth('schoolSubmit', page, appearance)).toBe(226);
    expect(artListActionColumnMinimumWidth('audit', page, appearance)).toBe(226);
    expect(artListActionColumnMinimumWidth('projectView', page, appearance)).toBe(156);
  });

  it('操作图标随公共按钮字号增大时同步增加安全宽度', () => {
    const page = pageConfig();
    const appearance = appearanceConfig();
    const withIcon = artListActionColumnMinimumWidth('project', page, appearance);
    appearance.rowActionAppearance.iconVisible = false;
    const withoutIcon = artListActionColumnMinimumWidth('project', page, appearance);

    expect(withIcon - withoutIcon).toBe(3 * (appearance.buttonFontSize + 6));
  });

  it('状态文字和公共按钮字号共同决定状态列安全最小宽度', () => {
    const page = pageConfig();
    const appearance = appearanceConfig();
    const minimumWidth = artListStatusColumnMinimumWidth(page, appearance);
    page.columns.unshift({ key: 'scoreStatus', source: 'builtin', label: '状态', visible: true, width: 70, minWidth: 60, fixed: 'none' });

    const status = toArtListRuntimeColumns('review', page.columns, page, appearance).find((column) => column.key === 'scoreStatus');
    expect(minimumWidth).toBeGreaterThan(70);
    expect(status?.width).toBeGreaterThanOrEqual(minimumWidth);
    expect(status?.minWidth).toBeGreaterThanOrEqual(minimumWidth);
  });

  it('五类页面共用的序号结构列支持显隐、宽度和左固定', () => {
    const page = pageConfig();
    page.serialWidth = 96;
    page.serialFixed = 'left';
    expect(artListSerialColumn(page)).toMatchObject({ key: 'serial', width: 96, fixed: 'left', resizable: false });
    page.serialVisible = false;
    expect(artListSerialColumn(page)).toBeUndefined();
  });

  it('旧版布局升级时把操作列恢复为右固定，新版仍尊重管理员设置', () => {
    const legacy = pageConfig().columns.map((column) => ({ ...column, fixed: 'none' as const }));
    expect(migrateLegacyActionFixedRight(legacy, 3).find((column) => column.key === 'actions')?.fixed).toBe('right');
    expect(migrateLegacyActionFixedRight(legacy, ART_LIST_TABLE_LAYOUT_VERSION).find((column) => column.key === 'actions')?.fixed).toBe('none');
  });

  it('专家评分系统默认只显示约定业务列，并将旧版默认配置升级到新基线', () => {
    const legacy = reviewListBuiltinColumnOptions.map(
      (column) =>
        ({
          ...column,
          source: 'builtin',
          deleted: false,
          visible: ['activityName', 'scoreMode', 'scoreStatus'].includes(column.key) ? true : column.visible
        }) as ArtReviewListColumnConfig
    );
    const migrated = migrateLegacyReviewDefaultColumns(legacy, legacy, 1);
    const visibleKeys = migrated.filter((column) => column.visible).map((column) => column.key);

    expect(ART_LIST_TABLE_LAYOUT_VERSION).toBe(4);
    expect(visibleKeys).toEqual(['projectNo', 'projectName', 'categoryName', 'groupOrNature', 'scoreResult', 'scoreSubmittedAt', 'actions']);
  });

  it('新版专家评分配置继续尊重管理员后续显隐设置', () => {
    const customized = reviewListBuiltinColumnOptions.map(
      (column) => ({ ...column, source: 'builtin', deleted: false }) as ArtReviewListColumnConfig
    );
    const activity = customized.find((column) => column.key === 'activityName');
    if (activity) activity.visible = true;

    const normalized = migrateLegacyReviewDefaultColumns(customized, customized, ART_LIST_TABLE_LAYOUT_VERSION);

    expect(normalized.find((column) => column.key === 'activityName')?.visible).toBe(true);
  });

  it('旧版同顺序但已自定义显隐或删除时不覆盖管理员设置', () => {
    const customized = reviewListBuiltinColumnOptions.map(
      (column) =>
        ({
          ...column,
          source: 'builtin',
          deleted: column.key === 'projectName',
          visible: column.key === 'activityName' ? false : ['scoreMode', 'scoreStatus'].includes(column.key) ? true : column.visible
        }) as ArtReviewListColumnConfig
    );

    const normalized = migrateLegacyReviewDefaultColumns(customized, customized, 1);

    expect(normalized.find((column) => column.key === 'activityName')?.visible).toBe(false);
    expect(normalized.find((column) => column.key === 'projectName')?.deleted).toBe(true);
    expect(normalized.find((column) => column.key === 'scoreMode')?.visible).toBe(true);
  });

  it('未分组只在专家评分表格显示为空白，真实组别保持原值', () => {
    expect(artReviewGroupDisplayText('未分组')).toBe('');
    expect(artReviewGroupDisplayText(undefined, '甲组')).toBe('甲组');
    expect(artReviewGroupDisplayText('个人')).toBe('个人');
  });
});

describe('真实业务表格页面内自适应', () => {
  const columns: ArtResizableColumn[] = [
    { key: 'selection', width: 50, minWidth: 50, fixed: 'left', resizable: false },
    { key: 'serial', width: 66, minWidth: 66, fixed: false, resizable: false },
    { key: 'projectNo', width: 135, minWidth: 96, fixed: false, resizable: true },
    { key: 'projectName', width: 180, minWidth: 120, fixed: false, resizable: true },
    { key: 'actions', width: 110, minWidth: 110, fixed: 'right', resizable: false }
  ];
  const preferredWidths = Object.fromEntries(columns.map((column) => [column.key, column.width]));

  it('把选择列和序号列纳入总宽度后，中间列按比例填满且不超出容器', () => {
    const resolved = resolveArtTableFitWidths(columns, preferredWidths, 900);
    const wider = resolveArtTableFitWidths(columns, preferredWidths, 1200);

    expect(resolved.selection).toBe(50);
    expect(resolved.actions).toBe(110);
    expect(resolved.projectName / resolved.projectNo).toBeCloseTo(180 / 135, 1);
    expect(Object.values(resolved).reduce((sum, width) => sum + width, 0)).toBe(898);
    expect(wider.projectName).toBeGreaterThan(resolved.projectName);
    expect(Object.values(wider).reduce((sum, width) => sum + width, 0)).toBe(1198);
  });

  it('操作列隐藏后把释放的宽度重新分配给中间区域', () => {
    const withActions = resolveArtTableFitWidths(columns, preferredWidths, 900);
    const withoutActions = resolveArtTableFitWidths(
      columns.map((column) => (column.key === 'actions' ? { ...column, visible: false } : column)),
      preferredWidths,
      900
    );

    expect(withoutActions.projectName).toBeGreaterThan(withActions.projectName);
    expect(Object.values(withoutActions).reduce((sum, width) => sum + width, 0)).toBe(898);
  });

  it('序号列隐藏后把释放的宽度重新分配给中间区域', () => {
    const withSerial = resolveArtTableFitWidths(columns, preferredWidths, 900);
    const withoutSerial = resolveArtTableFitWidths(
      columns.map((column) => (column.key === 'serial' ? { ...column, visible: false } : column)),
      preferredWidths,
      900
    );

    expect(withoutSerial.projectName).toBeGreaterThan(withSerial.projectName);
    expect(Object.values(withoutSerial).reduce((sum, width) => sum + width, 0)).toBe(898);
  });

  it('窄容器优先守住状态和操作列最小宽度，避免文字裁切', () => {
    const protectedColumns: ArtResizableColumn[] = [
      { key: 'projectName', width: 360, minWidth: 120, fixed: false, resizable: true },
      { key: 'status', width: 110, minWidth: 96, fixed: false, resizable: true },
      { key: 'actions', width: 180, minWidth: 160, fixed: 'right', resizable: false }
    ];
    const widths = resolveArtTableFitWidths(protectedColumns, Object.fromEntries(protectedColumns.map((column) => [column.key, column.width])), 480);

    expect(widths.status).toBeGreaterThanOrEqual(96);
    expect(widths.actions).toBeGreaterThanOrEqual(160);
    expect(Object.values(widths).reduce((sum, width) => sum + width, 0)).toBe(478);
  });

  it('历史本地状态列宽低于安全值时自动抬高，不再逐字竖排或覆盖相邻列', () => {
    const protectedColumns: ArtResizableColumn[] = [
      { key: 'projectNo', width: 180, minWidth: 90, fixed: false, resizable: true },
      { key: 'submittedAt', width: 146, minWidth: 100, fixed: false, resizable: true },
      { key: 'status', width: 106, minWidth: 106, fixed: false, resizable: true },
      { key: 'actions', width: 226, minWidth: 226, fixed: 'right', resizable: false }
    ];
    const widths = resolveArtTableFitWidths(protectedColumns, { projectNo: 180, submittedAt: 146, status: 24, actions: 226 }, 900);

    expect(widths.status).toBeGreaterThanOrEqual(106);
    expect(widths.actions).toBeGreaterThanOrEqual(226);
    expect(Object.values(widths).reduce((sum, width) => sum + width, 0)).toBe(898);
  });
});
