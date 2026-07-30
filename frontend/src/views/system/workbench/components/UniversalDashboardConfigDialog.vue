<template>
  <el-dialog
    :model-value="modelValue"
    title="通用数据看板配置"
    width="min(1480px, 96vw)"
    top="3vh"
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-if="form" class="dashboard-config">
      <section class="dashboard-config__editor">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="内容与布局" name="content">
            <div class="dashboard-config__toolbar">
              <span>拖拽调整区块顺序，选择区块后编辑详细配置</span>
              <div>
                <el-dropdown trigger="click" @command="addBlock">
                  <el-button type="primary" plain icon="Plus">添加区块</el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item v-for="item in universalDashboardBlockTypeOptions" :key="item.value" :command="item.value">
                        {{ item.label }}
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
                <el-button plain icon="RefreshLeft" @click="restoreDashboardDefault">恢复角色默认</el-button>
              </div>
            </div>

            <div class="dashboard-config__blocks">
              <button
                v-for="(item, index) in form.blocks"
                :key="item.id"
                type="button"
                class="dashboard-config__block-row"
                :class="{ 'is-active': selectedBlockId === item.id, 'is-hidden': item.visible === false }"
                draggable="true"
                @click="selectedBlockId = item.id"
                @dragstart="dragIndex = index"
                @dragover.prevent
                @drop="moveBlock(index)"
              >
                <el-icon><Rank /></el-icon>
                <el-tag size="small" effect="plain">{{ blockTypeLabel(item.type) }}</el-tag>
                <strong>{{ item.title }}</strong>
                <span>{{ item.span }}/{{ form.columns }} · {{ item.height }}px</span>
                <el-switch v-model="item.visible" size="small" @click.stop />
                <el-button text type="primary" icon="CopyDocument" aria-label="复制区块" @click.stop="duplicateBlock(item)" />
                <el-button text type="danger" icon="Delete" aria-label="删除区块" @click.stop="removeBlock(item.id)" />
              </button>
            </div>

            <el-empty v-if="!form.blocks.length" :image-size="70" description="尚未添加看板区块" />

            <el-divider v-if="selectedBlock" content-position="left">当前区块</el-divider>
            <el-form v-if="selectedBlock" :model="selectedBlock" label-width="94px" class="dashboard-config__form">
              <el-row :gutter="12">
                <el-col :span="12">
                  <el-form-item label="区块类型">
                    <el-select v-model="selectedBlock.type" class="w-full" @change="changeBlockType">
                      <el-option v-for="item in universalDashboardBlockTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="显示区块">
                    <el-switch v-model="selectedBlock.visible" />
                    <span class="dashboard-config__hint">可单独关闭此数据模块</span>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item :label="selectedBlock.type === 'action' ? '入口标题' : '区块标题'">
                    <el-input v-model="selectedBlock.title" maxlength="60" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item :label="selectedBlock.type === 'action' ? '入口说明' : '辅助说明'">
                    <el-input
                      v-model="selectedBlock.subtitle"
                      maxlength="100"
                      clearable
                      :placeholder="selectedBlock.type === 'action' ? '例如：进入本人评分任务列表' : '可选的辅助说明'"
                    />
                  </el-form-item>
                </el-col>
                <el-col v-if="selectedBlock.type !== 'action'" :span="24">
                  <el-form-item label="数据源">
                    <el-select v-model="selectedBlock.sourceKey" filterable class="w-full">
                      <el-option v-for="item in currentSourceOptions" :key="item.key" :label="item.label" :value="item.key">
                        <div class="dashboard-config__source-option">
                          <span>{{ item.label }}</span>
                          <small>{{ item.description }}</small>
                        </div>
                      </el-option>
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="占用宽度">
                    <el-slider v-model="selectedBlock.span" :min="2" :max="form.columns" :step="1" show-input />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="区块高度">
                    <el-input-number v-model="selectedBlock.height" :min="110" :max="520" :step="10" controls-position="right" />
                    <span class="dashboard-config__unit">px</span>
                  </el-form-item>
                </el-col>
                <el-col v-if="['metric', 'ranking'].includes(selectedBlock.type)" :span="12">
                  <el-form-item label="数值单位"
                    ><el-input v-model="selectedBlock.unit" maxlength="12" clearable placeholder="个、项、%"
                  /></el-form-item>
                </el-col>
                <el-col v-if="!['action', 'list'].includes(selectedBlock.type)" :span="12">
                  <el-form-item label="小数位">
                    <el-input-number v-model="selectedBlock.decimals" :min="0" :max="2" controls-position="right" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="强调颜色">
                    <div class="dashboard-config__color">
                      <el-color-picker v-model="selectedBlock.color" /><el-input v-model="selectedBlock.color" />
                    </div>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="区块底色">
                    <div class="dashboard-config__color">
                      <el-color-picker v-model="selectedBlock.backgroundColor" show-alpha />
                      <el-input v-model="selectedBlock.backgroundColor" />
                    </div>
                  </el-form-item>
                </el-col>
                <template v-if="['donut', 'bar', 'line'].includes(selectedBlock.type)">
                  <el-col :span="8">
                    <el-form-item label="显示图例"><el-switch v-model="selectedBlock.showLegend" /></el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="显示数值"><el-switch v-model="selectedBlock.showLabels" /></el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="最多项目">
                      <el-input-number v-model="selectedBlock.limit" :min="3" :max="20" controls-position="right" />
                    </el-form-item>
                  </el-col>
                </template>
              </el-row>

              <el-divider content-position="left">点击与快捷跳转</el-divider>
              <el-row :gutter="12">
                <el-col :span="12">
                  <el-form-item label="点击操作">
                    <el-select v-model="selectedBlock.action.type" class="w-full" @change="changeActionType">
                      <el-option v-for="item in currentActionOptions" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col v-if="selectedBlock.action?.type !== 'none'" :span="12">
                  <el-form-item label="按钮文字"><el-input v-model="selectedBlock.action.label" maxlength="30" clearable /></el-form-item>
                </el-col>
                <template v-if="selectedBlock.action?.type === 'school_create'">
                  <el-col :span="12">
                    <el-form-item label="填报活动">
                      <el-select v-model="selectedBlock.action.activityId" filterable class="w-full" @change="changeActionActivity">
                        <el-option v-for="item in activities" :key="String(item.id)" :label="item.activityName" :value="item.id!" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="填报类别">
                      <el-select
                        v-model="selectedBlock.action.categoryId"
                        filterable
                        class="w-full"
                        :loading="categoryLoading"
                        @change="changeActionCategory"
                      >
                        <el-option
                          v-for="item in categories"
                          :key="String(item.id)"
                          :label="item.categoryName"
                          :value="item.id!"
                          :disabled="item.enabled === false"
                        />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="锁定类别"
                      ><el-switch v-model="selectedBlock.action.lockCategory" active-text="锁定" inactive-text="可修改"
                    /></el-form-item>
                  </el-col>
                </template>
                <template v-if="selectedBlock.action?.type === 'audit_list'">
                  <el-col :span="12">
                    <el-form-item label="审核状态">
                      <el-select v-model="selectedBlock.action.status" class="w-full" clearable>
                        <el-option label="全部" value="" />
                        <el-option label="待审核" value="submitted" />
                        <el-option label="审核通过" value="audit_passed" />
                        <el-option label="审核退回" value="returned" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                </template>
                <template v-if="selectedBlock.action?.type === 'review_list'">
                  <el-col :span="12">
                    <el-form-item label="评分状态">
                      <el-select v-model="selectedBlock.action.scoreStatus" class="w-full" clearable>
                        <el-option label="全部" value="" />
                        <el-option label="待评分" value="none" />
                        <el-option label="草稿" value="draft" />
                        <el-option label="已提交" value="submitted" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                </template>
              </el-row>
            </el-form>
          </el-tab-pane>

          <el-tab-pane v-if="role === 'school'" label="预警规则" name="warnings">
            <el-alert title="预警只用于学校首页提示，不改变名额、提交校验或项目状态。" type="info" :closable="false" class="mb-3" />
            <el-table :data="form.warningRules" border>
              <el-table-column label="启用" width="78" align="center">
                <template #default="{ row }"><el-switch v-model="row.enabled" /></template>
              </el-table-column>
              <el-table-column label="预警项" min-width="200">
                <template #default="{ row }"><el-input v-model="row.label" maxlength="40" /></template>
              </el-table-column>
              <el-table-column label="触发阈值" width="160">
                <template #default="{ row }">
                  <el-input-number v-model="row.threshold" :min="1" :max="row.key === 'quota_usage_rate' ? 100 : 9999" controls-position="right" />
                </template>
              </el-table-column>
              <el-table-column label="提示级别" width="150">
                <template #default="{ row }">
                  <el-select v-model="row.severity">
                    <el-option label="提示" value="info" />
                    <el-option label="注意" value="warning" />
                    <el-option label="紧急" value="danger" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="规则" min-width="220">
                <template #default="{ row }">
                  {{
                    row.key === 'quota_usage_rate'
                      ? '名额使用率达到阈值（%）'
                      : row.key === 'returned_count'
                        ? '退回待处理项目达到阈值（项）'
                        : '待提交草稿达到阈值（项）'
                  }}
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="整体样式" name="style">
            <el-form :model="form" label-width="100px" class="dashboard-config__form">
              <el-form-item label="角色模板">
                <el-tag effect="plain">{{ roleLabel }}</el-tag>
                <span class="dashboard-config__hint">该角色首次使用及恢复默认时采用此模板</span>
              </el-form-item>
              <el-form-item label="显示标题"><el-switch v-model="form.showHeader" /></el-form-item>
              <el-form-item label="显示跳转按钮"><el-switch v-model="form.headerAction.visible" /></el-form-item>
              <template v-if="form.headerAction.visible">
                <el-form-item label="按钮名称"><el-input v-model="form.headerAction.label" maxlength="30" placeholder="统一上报" /></el-form-item>
                <el-form-item label="跳转连接">
                  <el-select v-model="form.headerAction.path" class="w-full">
                    <el-option
                      v-for="item in universalDashboardHeaderActionPathOptions"
                      :key="item.value"
                      :label="`${item.label}（${item.value}）`"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
              </template>
              <el-form-item label="看板说明"><el-input v-model="form.subtitle" maxlength="120" show-word-limit /></el-form-item>
              <el-form-item label="顶部标识"><el-input v-model="form.headerKicker" maxlength="30" placeholder="DATA WORKBENCH" /></el-form-item>
              <el-form-item label="刷新文字"><el-input v-model="form.refreshLabel" maxlength="20" placeholder="刷新" /></el-form-item>
              <el-divider content-position="left">空数据与默认按钮文案</el-divider>
              <el-row :gutter="12">
                <el-col :span="12">
                  <el-form-item label="图表空提示"><el-input v-model="form.emptyChartText" maxlength="40" placeholder="暂无统计数据" /></el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="排行空提示"
                    ><el-input v-model="form.emptyRankingText" maxlength="40" placeholder="暂无排行数据"
                  /></el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="列表空提示"><el-input v-model="form.emptyListText" maxlength="40" placeholder="暂无动态数据" /></el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="列表默认按钮"
                    ><el-input v-model="form.listActionFallbackLabel" maxlength="20" placeholder="进入"
                  /></el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="入口默认按钮"
                    ><el-input v-model="form.actionFallbackLabel" maxlength="20" placeholder="立即进入"
                  /></el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="入口默认说明">
                    <el-input v-model="form.actionFallbackDescription" maxlength="60" placeholder="进入相关业务页面" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="主题预设">
                <el-select v-model="form.theme.preset" class="w-full" @change="applyThemePreset">
                  <el-option v-for="item in universalDashboardThemePresets" :key="item.value.preset" :label="item.label" :value="item.value.preset" />
                  <el-option label="自定义" value="custom" />
                </el-select>
              </el-form-item>
              <el-row :gutter="12">
                <el-col v-for="item in themeColorFields" :key="item.key" :span="12">
                  <el-form-item :label="item.label">
                    <div class="dashboard-config__color">
                      <el-color-picker v-model="form.theme[item.key]" show-alpha @change="form.theme.preset = 'custom'" />
                      <el-input v-model="form.theme[item.key]" @change="form.theme.preset = 'custom'" />
                    </div>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="图表色系">
                <div class="dashboard-config__palette">
                  <div v-for="(_, index) in form.theme.chartColors" :key="index" class="dashboard-config__color">
                    <el-color-picker v-model="form.theme.chartColors[index]" @change="form.theme.preset = 'custom'" />
                    <el-input v-model="form.theme.chartColors[index]" @change="form.theme.preset = 'custom'" />
                  </div>
                </div>
              </el-form-item>
              <el-row :gutter="12">
                <el-col :span="8">
                  <el-form-item label="网格列数"
                    ><el-input-number v-model="form.columns" :min="4" :max="12" controls-position="right"
                  /></el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="区块间距"><el-input-number v-model="form.gap" :min="4" :max="28" controls-position="right" /></el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="圆角"><el-input-number v-model="form.radius" :min="0" :max="24" controls-position="right" /></el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="组件外框">
                <el-radio-group v-model="form.frameMode">
                  <el-radio-button label="default">默认卡片</el-radio-button>
                  <el-radio-button label="custom">自定义颜色</el-radio-button>
                  <el-radio-button label="transparent">全透明</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item v-if="form.frameMode === 'custom'" label="组件背景">
                <div class="dashboard-config__color">
                  <el-color-picker v-model="form.frameBackground" show-alpha />
                  <el-input v-model="form.frameBackground" placeholder="支持 transparent 或 rgba" />
                </div>
              </el-form-item>
              <el-row :gutter="12">
                <el-col :span="6">
                  <el-form-item label="组件边框">
                    <el-switch v-model="form.frameBorderVisible" :disabled="form.frameMode === 'transparent'" />
                  </el-form-item>
                </el-col>
                <el-col :span="6">
                  <el-form-item label="组件阴影">
                    <el-switch v-model="form.frameShadowVisible" :disabled="form.frameMode === 'transparent'" />
                  </el-form-item>
                </el-col>
                <el-col :span="6">
                  <el-form-item label="悬浮动画"><el-switch v-model="form.hoverLiftEnabled" /></el-form-item>
                </el-col>
                <el-col :span="6">
                  <el-form-item label="悬浮边框"><el-switch v-model="form.hoverBorderEnabled" /></el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </section>

      <aside class="dashboard-config__preview">
        <div class="dashboard-config__preview-head">
          <strong>实时预览</strong>
          <span>预览使用示例数据，实际数据受登录角色权限控制</span>
        </div>
        <div class="dashboard-config__preview-body">
          <UniversalDashboardWidget :title="title || '通用数据看板'" :config-json="serializedConfig" preview :role-key="roleKey" />
        </div>
      </aside>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" @click="submit">应用配置</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { listActivityOptions, listCategoryOptions } from '@/api/crehn/activity';
import type { ActivityCategoryVO, ActivityVO } from '@/api/crehn/types';
import { ElMessage, ElMessageBox } from 'element-plus';
import UniversalDashboardWidget from '@/views/workbench/components/UniversalDashboardWidget.vue';
import {
  UNIVERSAL_DASHBOARD_CONFIG_MAX_LENGTH,
  applyUniversalDashboardThemePreset,
  cloneUniversalDashboardConfig,
  createUniversalDashboardBlock,
  dashboardActionOptions,
  dashboardSourceOptions,
  defaultUniversalDashboardConfig,
  normalizeUniversalDashboardConfig,
  parseUniversalDashboardConfig,
  resolveDashboardRole,
  universalDashboardBlockTypeOptions,
  universalDashboardThemePresets,
  universalDashboardHeaderActionPathOptions,
  type DashboardBlockConfig,
  type DashboardBlockType,
  type DashboardSourceKind,
  type UniversalDashboardConfig,
  type UniversalDashboardTheme
} from '@/views/workbench/universalDashboard';

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    configJson?: string;
    roleKey?: string;
    title?: string;
  }>(),
  { configJson: '', roleKey: '', title: '' }
);

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
  (event: 'save', value: string): void;
}>();

const activeTab = ref<'content' | 'warnings' | 'style'>('content');
const form = ref<UniversalDashboardConfig>();
const selectedBlockId = ref('');
const dragIndex = ref<number>();
const activities = ref<ActivityVO[]>([]);
const categories = ref<ActivityCategoryVO[]>([]);
const categoryLoading = ref(false);

const role = computed(() => resolveDashboardRole(props.roleKey));
const roleLabel = computed(() => ({ school: '学校', admin: '管理员/运营', auditor: '审核员', reviewer: '评审员', viewer: '项目查看员' })[role.value]);
const selectedBlock = computed(() => form.value?.blocks.find((item) => item.id === selectedBlockId.value));
const currentSourceKind = computed<DashboardSourceKind | undefined>(() => {
  if (!selectedBlock.value || selectedBlock.value.type === 'action') return undefined;
  if (['metric', 'progress'].includes(selectedBlock.value.type)) return 'metric';
  return selectedBlock.value.type === 'list' ? 'list' : 'series';
});
const currentSourceOptions = computed(() => dashboardSourceOptions(role.value, currentSourceKind.value));
const currentActionOptions = computed(() => dashboardActionOptions(role.value));
const serializedConfig = computed(() => (form.value ? JSON.stringify(normalizeUniversalDashboardConfig(form.value, role.value)) : ''));

const themeColorFields: Array<{
  key: keyof Pick<UniversalDashboardTheme, 'accentColor' | 'backgroundColor' | 'surfaceColor' | 'textColor' | 'mutedColor' | 'borderColor'>;
  label: string;
}> = [
  { key: 'accentColor', label: '强调色' },
  { key: 'backgroundColor', label: '看板底色' },
  { key: 'surfaceColor', label: '区块底色' },
  { key: 'textColor', label: '主要文字' },
  { key: 'mutedColor', label: '辅助文字' },
  { key: 'borderColor', label: '边框颜色' }
];

const blockTypeLabel = (type: DashboardBlockType) => universalDashboardBlockTypeOptions.find((item) => item.value === type)?.label || type;

const initialize = async () => {
  form.value = parseUniversalDashboardConfig(props.configJson, role.value, true);
  selectedBlockId.value = form.value.blocks[0]?.id || '';
  activeTab.value = 'content';
  if (!activities.value.length) {
    try {
      const res = await listActivityOptions();
      activities.value = res.data || [];
    } catch {
      activities.value = [];
    }
  }
};

const addBlock = (type: DashboardBlockType) => {
  if (!form.value) return;
  const item = createUniversalDashboardBlock(role.value, type);
  form.value.blocks.push(item);
  selectedBlockId.value = item.id;
};

const duplicateBlock = (item: DashboardBlockConfig) => {
  if (!form.value) return;
  const next = cloneUniversalDashboardConfig({ ...form.value, blocks: [item] }).blocks[0];
  next.id = `dashboard-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;
  next.title = `${item.title}副本`;
  const index = form.value.blocks.findIndex((row) => row.id === item.id);
  form.value.blocks.splice(index + 1, 0, next);
  selectedBlockId.value = next.id;
};

const removeBlock = (id: string) => {
  if (!form.value) return;
  const index = form.value.blocks.findIndex((item) => item.id === id);
  if (index < 0) return;
  form.value.blocks.splice(index, 1);
  selectedBlockId.value = form.value.blocks[Math.min(index, form.value.blocks.length - 1)]?.id || '';
};

const moveBlock = (targetIndex: number) => {
  if (!form.value || dragIndex.value === undefined || dragIndex.value === targetIndex) return;
  const [item] = form.value.blocks.splice(dragIndex.value, 1);
  form.value.blocks.splice(targetIndex, 0, item);
  dragIndex.value = undefined;
};

const changeBlockType = () => {
  const item = selectedBlock.value;
  if (!item) return;
  const kind = ['metric', 'progress'].includes(item.type) ? 'metric' : item.type === 'list' ? 'list' : item.type === 'action' ? undefined : 'series';
  item.sourceKey = kind ? dashboardSourceOptions(role.value, kind)[0]?.key : undefined;
  item.height = item.type === 'metric' ? 126 : item.type === 'action' ? 160 : Math.max(240, item.height);
  item.span = item.type === 'metric' ? 3 : item.type === 'action' ? 3 : Math.max(4, item.span);
};

const changeActionType = () => {
  const action = selectedBlock.value?.action;
  if (!action) return;
  action.label = dashboardActionOptions(role.value).find((item) => item.value === action.type)?.label || '';
  if (action.type === 'school_create') action.lockCategory = true;
};

const loadCategories = async (activityId: string | number) => {
  categoryLoading.value = true;
  try {
    const res = await listCategoryOptions(activityId);
    categories.value = (res.data || []).filter((item) => item.enabled !== false);
  } catch {
    categories.value = [];
  } finally {
    categoryLoading.value = false;
  }
};

const changeActionActivity = async (activityId: string | number) => {
  if (!selectedBlock.value?.action) return;
  selectedBlock.value.action.categoryId = undefined;
  selectedBlock.value.action.categoryName = '';
  categories.value = [];
  if (activityId) await loadCategories(activityId);
};

const changeActionCategory = (categoryId: string | number) => {
  if (!selectedBlock.value?.action) return;
  selectedBlock.value.action.categoryName = categories.value.find((item) => String(item.id) === String(categoryId))?.categoryName || '';
};

watch(selectedBlockId, async () => {
  const activityId = selectedBlock.value?.action?.activityId;
  if (activityId) {
    await loadCategories(activityId);
  } else {
    categories.value = [];
  }
});

const applyThemePreset = (preset: string) => {
  if (!form.value || preset === 'custom') return;
  form.value = applyUniversalDashboardThemePreset(form.value, preset);
};

const restoreDashboardDefault = async () => {
  try {
    await ElMessageBox.confirm('将清除当前看板的区块、颜色和跳转自定义，恢复所选角色的系统默认模板。是否继续？', '恢复角色默认', {
      type: 'warning',
      confirmButtonText: '恢复默认',
      cancelButtonText: '取消'
    });
    form.value = defaultUniversalDashboardConfig(role.value);
    selectedBlockId.value = form.value.blocks[0]?.id || '';
    ElMessage.success('已恢复角色默认模板，点击“应用配置”后生效');
  } catch {
    // 用户取消时保持当前配置。
  }
};

const submit = () => {
  if (!form.value) return;
  const normalized = normalizeUniversalDashboardConfig(form.value, role.value);
  const value = JSON.stringify(normalized);
  if (value.length > UNIVERSAL_DASHBOARD_CONFIG_MAX_LENGTH) {
    ElMessage.error(`看板配置过大，请减少区块或文字内容（最多 ${UNIVERSAL_DASHBOARD_CONFIG_MAX_LENGTH} 字符）`);
    return;
  }
  emit('save', value);
  emit('update:modelValue', false);
};

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) initialize();
  },
  { immediate: true }
);
</script>

<style scoped lang="scss">
.dashboard-config {
  height: min(790px, 82vh);
  display: grid;
  grid-template-columns: minmax(540px, 0.9fr) minmax(620px, 1.1fr);
  gap: 16px;
}

.dashboard-config__editor,
.dashboard-config__preview {
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  border: 1px solid #dfe7f0;
  border-radius: 10px;
  background: #ffffff;
}

.dashboard-config__editor :deep(.el-tabs) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.dashboard-config__editor :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 18px;
}

.dashboard-config__editor :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 16px 18px 22px;
}

.dashboard-config__toolbar,
.dashboard-config__preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.dashboard-config__toolbar > span,
.dashboard-config__preview-head span,
.dashboard-config__hint {
  color: #7a899a;
  font-size: 12px;
}

.dashboard-config__toolbar > div {
  display: flex;
  gap: 8px;
}

.dashboard-config__blocks {
  margin-top: 14px;
  display: grid;
  gap: 8px;
}

.dashboard-config__block-row {
  width: 100%;
  min-height: 46px;
  padding: 8px 10px;
  display: grid;
  grid-template-columns: 24px 86px minmax(120px, 1fr) auto auto 30px 30px;
  align-items: center;
  gap: 8px;
  border: 1px solid #e1e8f0;
  border-radius: 8px;
  color: #334155;
  background: #ffffff;
  text-align: left;
  cursor: pointer;
}

.dashboard-config__block-row:hover,
.dashboard-config__block-row.is-active {
  border-color: #93b4f5;
  background: #f7faff;
}

.dashboard-config__block-row.is-hidden {
  opacity: 0.62;
}

.dashboard-config__block-row > span {
  color: #8a99aa;
  font-size: 12px;
}

.dashboard-config__form {
  padding-top: 4px;
}

.dashboard-config__color {
  width: 100%;
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr);
  align-items: center;
  gap: 8px;
}

.dashboard-config__unit {
  margin-left: 6px;
  color: #8492a6;
}

.dashboard-config__source-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.dashboard-config__source-option small {
  color: #98a5b4;
}

.dashboard-config__palette {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 12px;
}

.dashboard-config__preview {
  display: flex;
  flex-direction: column;
  background: #f3f7fb;
}

.dashboard-config__preview-head {
  padding: 13px 16px;
  border-bottom: 1px solid #dfe7f0;
  background: #ffffff;
}

.dashboard-config__preview-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 14px;
}

.dashboard-config__preview-body :deep(.universal-dashboard) {
  min-width: 720px;
}

@media (max-width: 1180px) {
  .dashboard-config {
    height: auto;
    grid-template-columns: 1fr;
  }

  .dashboard-config__editor,
  .dashboard-config__preview {
    max-height: 70vh;
  }
}
</style>
