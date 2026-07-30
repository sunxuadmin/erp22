<template>
  <section class="crehn-visual">
    <div class="crehn-visual__toolbar">
      <div>
        <strong>艺术评审可视化编辑器</strong>
        <small>在预览中选择标签或区域，再从右侧修改详情和评分工作台参数；列表列已统一迁入全局表格</small>
      </div>
      <el-segmented v-model="scene" :options="sceneOptions" />
    </div>

    <div class="crehn-visual__workspace">
      <aside class="crehn-structure">
        <header><strong>页面结构</strong><small>拖动调整顺序</small></header>
        <template v-if="scene !== 'review'">
          <span class="crehn-structure__group">详情标签</span>
          <button
            v-for="(tabKey, index) in currentPopupTabs.order"
            :key="tabKey"
            type="button"
            class="crehn-structure__item"
            :class="{
              'is-active': selection.kind === 'tab' && selection.key === tabKey,
              'is-hidden': !currentPopupTabs.visibleTabs.includes(tabKey)
            }"
            draggable="true"
            @click="selectTab(tabKey)"
            @dragstart="tabDragIndex = index"
            @dragover.prevent
            @drop="moveTab(index)"
          >
            <el-icon><Rank /></el-icon>
            <span>{{ currentPopupTabs.labels[tabKey] }}</span>
            <el-tag v-if="currentPopupTabs.defaultTab === tabKey" size="small">默认</el-tag>
          </button>
          <span class="crehn-structure__group">页面区域</span>
          <button
            v-for="region in currentRegionOptions"
            :key="region.key"
            type="button"
            class="crehn-structure__item"
            :class="{ 'is-active': selection.kind === 'region' && selection.key === region.key }"
            @click="selection = { kind: 'region', key: region.key }"
          >
            <el-icon><Grid /></el-icon><span>{{ region.label }}</span>
          </button>
        </template>
        <template v-else>
          <span class="crehn-structure__group">工作台区域</span>
          <button
            v-for="region in reviewRegionOptions"
            :key="region.key"
            type="button"
            class="crehn-structure__item"
            :class="{ 'is-active': selection.kind === 'region' && selection.key === region.key }"
            @click="selection = { kind: 'region', key: region.key }"
          >
            <el-icon><Grid /></el-icon><span>{{ region.label }}</span>
          </button>
          <el-alert class="mt-2" type="info" :closable="false" title="评分列表列已统一迁入“全局外观 → 全局表格”。" />
        </template>
      </aside>

      <main class="crehn-preview">
        <template v-if="scene === 'audit'">
          <div class="art-popup-preview">
            <header><strong>项目审核详情</strong><el-button text icon="Close" /></header>
            <nav class="art-popup-preview__tabs">
              <button
                v-for="tabKey in visiblePopupTabs"
                :key="tabKey"
                type="button"
                :class="{ 'is-active': previewTab === tabKey }"
                @click="
                  previewTab = tabKey;
                  selectTab(tabKey);
                "
              >
                {{ currentPopupTabs.labels[tabKey] }}
              </button>
            </nav>
            <section class="art-popup-preview__body">
              <div v-if="previewTab === 'preview'" class="art-preview-placeholder">作品在线预览区域</div>
              <div v-else-if="previewTab === 'form'" class="art-preview-form-grid">
                <div v-for="field in model.audit.summaryFields" :key="field" @click="selection = { kind: 'region', key: 'auditSummary' }">
                  <small>{{ model.audit.summaryLabels[field] }}</small
                  ><strong>{{ sampleFieldValue(field) }}</strong>
                </div>
              </div>
              <div v-else-if="previewTab === 'members'" class="art-preview-placeholder">成员信息表格</div>
              <div v-else-if="previewTab === 'files'" class="art-preview-table" @click="selection = { kind: 'region', key: 'files' }">
                <div class="is-head">
                  <span v-for="key in model.audit.content.fileColumns" :key="key">{{ optionLabel(auditFileColumnOptions, key) }}</span>
                </div>
                <div><span v-for="key in model.audit.content.fileColumns" :key="key">示例</span></div>
              </div>
              <div v-else class="art-preview-table" @click="selection = { kind: 'region', key: 'auditRecords' }">
                <div class="is-head">
                  <span v-for="key in model.audit.content.recordColumns" :key="key">{{ optionLabel(auditRecordColumnOptions, key) }}</span>
                </div>
                <div><span v-for="key in model.audit.content.recordColumns" :key="key">示例</span></div>
              </div>
            </section>
            <footer v-if="model.audit.actionsVisible" @click="selection = { kind: 'region', key: 'auditActions' }">
              <strong>{{ model.audit.title }}</strong
              ><el-button type="success">通过</el-button><el-button type="danger">退回</el-button>
            </footer>
          </div>
        </template>

        <template v-else-if="scene === 'score'">
          <div class="art-popup-preview">
            <header><strong>专家评分详情</strong><el-button text icon="Close" /></header>
            <nav class="art-popup-preview__tabs">
              <button
                v-for="tabKey in visiblePopupTabs"
                :key="tabKey"
                type="button"
                :class="{ 'is-active': previewTab === tabKey }"
                @click="
                  previewTab = tabKey;
                  selectTab(tabKey);
                "
              >
                {{ currentPopupTabs.labels[tabKey] }}
              </button>
            </nav>
            <section class="art-popup-preview__body">
              <div v-if="previewTab === 'preview'" class="art-preview-placeholder">作品在线预览区域</div>
              <div v-else-if="previewTab === 'form'" class="art-preview-form-grid">
                <div v-for="key in model.score.content.basicSections" :key="key">
                  <small>{{ optionLabel(scoreBasicSectionOptions, key) }}</small
                  ><strong>示例内容</strong>
                </div>
              </div>
              <div v-else-if="previewTab === 'members'" class="art-preview-placeholder">匿名成员信息区域</div>
              <div v-else-if="previewTab === 'files'" class="art-preview-table" @click="selection = { kind: 'region', key: 'files' }">
                <div class="is-head">
                  <span v-for="key in model.score.content.fileColumns" :key="key">{{ optionLabel(scoreFileColumnOptions, key) }}</span>
                </div>
                <div><span v-for="key in model.score.content.fileColumns" :key="key">示例</span></div>
              </div>
              <div v-else class="art-preview-table">
                <div class="is-head">
                  <span v-for="key in model.score.content.recordColumns" :key="key">{{ optionLabel(scoreRecordColumnOptions, key) }}</span>
                </div>
                <div><span v-for="key in model.score.content.recordColumns" :key="key">示例</span></div>
              </div>
            </section>
            <footer class="art-score-card-preview" @click="selection = { kind: 'region', key: 'scoreCard' }">
              <strong>{{ model.score.title }}</strong>
              <span>{{ model.score.scoreLabel }} 90</span><span>{{ model.score.gradeLabel }} 优秀</span>
              <el-button v-if="model.score.saveDraftVisible" type="primary">保存草稿</el-button>
            </footer>
          </div>
        </template>

        <template v-else>
          <div class="crehn-workbench-preview">
            <header @click="selection = { kind: 'region', key: 'reviewTitle' }">
              <strong v-if="model.reviewWorkbench.activityTitle.visible" :style="reviewTitleStyle">河南省大学生艺术展演评审工作台</strong>
              <div class="crehn-workbench-preview__actions">
                <el-input placeholder="请输入项目名称" /><el-button type="primary">搜索</el-button><el-button>重置</el-button>
              </div>
            </header>
            <div class="crehn-workbench-preview__filter" @click="selection = { kind: 'region', key: 'reviewProgress' }">
              <span>类别　全部类别　　组别　全部组别</span>
              <span v-if="model.reviewWorkbench.progress.visible">本类别 7/7 个作品已评分　100%</span>
            </div>
            <div class="crehn-table-preview">
              <div class="is-head">
                <button v-for="column in visibleColumns" :key="column.key" type="button" disabled>
                  {{ column.label }}
                </button>
              </div>
              <div>
                <span v-for="column in visibleColumns" :key="column.key">{{ sampleColumnValue(column.key) }}</span>
              </div>
              <div>
                <span v-for="column in visibleColumns" :key="column.key">{{ sampleColumnValue(column.key) }}</span>
              </div>
            </div>
            <footer @click="selection = { kind: 'region', key: 'reviewSignature' }">
              <span v-if="model.reviewWorkbench.signature.hintVisible">{{ model.reviewWorkbench.signature.hintText }}</span>
              <el-button type="primary">{{ model.reviewWorkbench.signature.buttonText }}</el-button>
            </footer>
          </div>
        </template>
      </main>

      <aside class="crehn-properties">
        <header>
          <strong>属性设置</strong><small>{{ propertyTitle }}</small>
        </header>
        <el-form label-position="top">
          <template v-if="selection.kind === 'tab' && scene !== 'review'">
            <el-form-item label="标签名称"><el-input v-model="currentPopupTabs.labels[selection.key]" maxlength="20" /></el-form-item>
            <el-form-item label="显示标签">
              <el-switch
                :model-value="currentPopupTabs.visibleTabs.includes(selection.key)"
                :disabled="isTabVisibilityLocked(selection.key)"
                @change="setTabVisible(selection.key, $event)"
              />
            </el-form-item>
            <el-form-item label="默认打开"><el-radio v-model="currentPopupTabs.defaultTab" :label="selection.key">设为默认</el-radio></el-form-item>
          </template>

          <template v-else-if="scene === 'audit' && selection.kind === 'region'">
            <template v-if="selection.key === 'auditSummary'">
              <el-form-item label="摘要字段">
                <el-checkbox-group v-model="model.audit.summaryFields" class="crehn-properties__checks">
                  <el-checkbox v-for="field in auditSummaryFieldOptions" :key="field.key" :label="field.key">{{ field.label }}</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item
                v-for="fieldKey in model.audit.summaryFields"
                :key="fieldKey"
                :label="`${optionLabel(auditSummaryFieldOptions, fieldKey)}名称`"
              >
                <el-input v-model="model.audit.summaryLabels[fieldKey]" maxlength="20" />
              </el-form-item>
            </template>
            <template v-else-if="selection.key === 'auditActions'">
              <el-form-item label="区域标题"><el-input v-model="model.audit.title" maxlength="30" /></el-form-item>
              <el-form-item label="显示审核操作区"><el-switch v-model="model.audit.actionsVisible" /></el-form-item>
            </template>
            <el-form-item v-else-if="selection.key === 'auditBasic'" label="基础信息固定区">
              <el-checkbox-group v-model="model.audit.content.basicSections" class="crehn-properties__checks">
                <el-checkbox v-for="item in auditBasicSectionOptions" :key="item.key" :label="item.key">{{ item.label }}</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <template v-else-if="selection.key === 'files'">
              <el-form-item label="作品文件列">
                <el-checkbox-group v-model="model.audit.content.fileColumns" class="crehn-properties__checks">
                  <el-checkbox v-for="item in auditFileColumnOptions" :key="item.key" :label="item.key">{{ item.label }}</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-divider>文件详细参数</el-divider>
              <el-form-item label="显示字段">
                <el-checkbox-group v-model="model.files.fields" class="crehn-properties__checks">
                  <el-checkbox v-for="item in detailFileFieldOptions" :key="item.key" :label="item.key">{{ item.label }}</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item v-for="fieldKey in model.files.fields" :key="fieldKey" :label="`${optionLabel(detailFileFieldOptions, fieldKey)}名称`">
                <el-input v-model="model.files.labels[fieldKey]" maxlength="20" />
              </el-form-item>
            </template>
            <el-form-item v-else-if="selection.key === 'auditRecords'" label="审核记录列">
              <el-checkbox-group v-model="model.audit.content.recordColumns" class="crehn-properties__checks">
                <el-checkbox v-for="item in auditRecordColumnOptions" :key="item.key" :label="item.key">{{ item.label }}</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </template>

          <template v-else-if="scene === 'score' && selection.kind === 'region'">
            <template v-if="selection.key === 'scoreCard'">
              <el-form-item label="卡片标题"><el-input v-model="model.score.title" maxlength="30" /></el-form-item>
              <el-form-item label="分数文字"><el-input v-model="model.score.scoreLabel" maxlength="20" /></el-form-item>
              <el-form-item label="等级文字"><el-input v-model="model.score.gradeLabel" maxlength="20" /></el-form-item>
              <el-form-item label="意见文字"><el-input v-model="model.score.commentLabel" maxlength="20" /></el-form-item>
              <el-form-item label="可选显示项" class="crehn-properties__checks">
                <el-checkbox v-model="model.score.statusVisible">评分状态</el-checkbox>
                <el-checkbox v-model="model.score.commentVisible">评分意见</el-checkbox>
                <el-checkbox v-model="model.score.saveDraftVisible">保存草稿</el-checkbox>
                <el-checkbox v-model="model.score.fileListVisible">文件列表</el-checkbox>
              </el-form-item>
            </template>
            <el-form-item v-else-if="selection.key === 'files'" label="评分文件列">
              <el-checkbox-group v-model="model.score.content.fileColumns" class="crehn-properties__checks">
                <el-checkbox v-for="item in scoreFileColumnOptions" :key="item.key" :label="item.key">{{ item.label }}</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item v-else-if="selection.key === 'scoreRecords'" label="评分记录列">
              <el-checkbox-group v-model="model.score.content.recordColumns" class="crehn-properties__checks">
                <el-checkbox v-for="item in scoreRecordColumnOptions" :key="item.key" :label="item.key">{{ item.label }}</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item v-else label="基础信息区">
              <el-checkbox-group v-model="model.score.content.basicSections" class="crehn-properties__checks">
                <el-checkbox v-for="item in scoreBasicSectionOptions" :key="item.key" :label="item.key">{{ item.label }}</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </template>

          <template v-else-if="scene === 'review' && selection.kind === 'region'">
            <template v-if="selection.key === 'reviewTitle'">
              <el-form-item label="显示活动标题"><el-switch v-model="model.reviewWorkbench.activityTitle.visible" /></el-form-item>
              <el-form-item label="标题模板"><el-input v-model="model.reviewWorkbench.activityTitle.template" maxlength="80" /></el-form-item>
              <el-form-item label="字号"
                ><el-slider v-model="model.reviewWorkbench.activityTitle.fontSize" :min="12" :max="28" show-input
              /></el-form-item>
              <el-form-item label="颜色"><el-color-picker v-model="model.reviewWorkbench.activityTitle.color" /></el-form-item>
              <el-form-item label="对齐">
                <el-radio-group v-model="model.reviewWorkbench.activityTitle.align">
                  <el-radio-button label="left">左</el-radio-button><el-radio-button label="center">中</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </template>
            <template v-else-if="selection.key === 'reviewProgress'">
              <el-form-item label="显示评分进度"><el-switch v-model="model.reviewWorkbench.progress.visible" /></el-form-item>
              <el-form-item label="进度文字"><el-input v-model="model.reviewWorkbench.progress.template" maxlength="120" /></el-form-item>
              <el-form-item label="显示百分比"><el-switch v-model="model.reviewWorkbench.progress.showPercentage" /></el-form-item>
              <el-form-item label="显示剩余数量"><el-switch v-model="model.reviewWorkbench.progress.showRemaining" /></el-form-item>
              <el-form-item label="进度宽度"
                ><el-slider v-model="model.reviewWorkbench.progress.width" :min="120" :max="600" show-input
              /></el-form-item>
              <el-form-item label="进度高度"
                ><el-slider v-model="model.reviewWorkbench.progress.height" :min="4" :max="20" show-input
              /></el-form-item>
              <el-form-item label="文字字号"
                ><el-slider v-model="model.reviewWorkbench.progress.fontSize" :min="12" :max="20" show-input
              /></el-form-item>
              <el-form-item label="进行中颜色"><el-color-picker v-model="model.reviewWorkbench.progress.activeColor" /></el-form-item>
              <el-form-item label="完成颜色"><el-color-picker v-model="model.reviewWorkbench.progress.successColor" /></el-form-item>
              <el-form-item label="已签字颜色"><el-color-picker v-model="model.reviewWorkbench.progress.signedColor" /></el-form-item>
              <el-form-item label="轨道颜色"><el-color-picker v-model="model.reviewWorkbench.progress.trackColor" /></el-form-item>
              <el-form-item label="文字颜色"><el-color-picker v-model="model.reviewWorkbench.progress.textColor" /></el-form-item>
            </template>
            <template v-else-if="selection.key === 'reviewSignature'">
              <el-form-item label="显示提示"><el-switch v-model="model.reviewWorkbench.signature.hintVisible" /></el-form-item>
              <el-form-item label="提示文字"><el-input v-model="model.reviewWorkbench.signature.hintText" maxlength="60" /></el-form-item>
              <el-form-item label="提示字号"
                ><el-slider v-model="model.reviewWorkbench.signature.hintFontSize" :min="12" :max="20" show-input
              /></el-form-item>
              <el-form-item label="提示文字颜色"><el-color-picker v-model="model.reviewWorkbench.signature.hintColor" /></el-form-item>
              <el-form-item label="提示背景"><el-color-picker v-model="model.reviewWorkbench.signature.hintBackgroundColor" /></el-form-item>
              <el-form-item label="提示边框"><el-color-picker v-model="model.reviewWorkbench.signature.hintBorderColor" /></el-form-item>
              <el-form-item label="按钮文字"><el-input v-model="model.reviewWorkbench.signature.buttonText" maxlength="30" /></el-form-item>
              <el-form-item label="按钮字号"
                ><el-slider v-model="model.reviewWorkbench.signature.buttonFontSize" :min="12" :max="20" show-input
              /></el-form-item>
              <el-form-item label="按钮文字颜色"><el-color-picker v-model="model.reviewWorkbench.signature.buttonTextColor" /></el-form-item>
            </template>
            <template v-else>
              <el-alert type="info" :closable="false" title="评分列表列、文字和默认宽度请在“全局外观 → 全局表格”中配置。" />
            </template>
          </template>

          <el-empty v-else :image-size="72" description="从左侧结构或中间预览选择对象" />
        </el-form>
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { ArtAuditDetailTab, ArtDetailDisplayConfigVO, ArtDetailPopupTabConfig, ArtScoreDetailTab } from '@/api/crehn/detailDisplay';
import {
  auditBasicSectionOptions,
  auditDefaultTabOptions,
  auditFileColumnOptions,
  auditRecordColumnOptions,
  auditSummaryFieldOptions,
  detailFileFieldOptions,
  scoreBasicSectionOptions,
  scoreDefaultTabOptions,
  scoreFileColumnOptions,
  scoreRecordColumnOptions
} from './artDetailDisplayConfig';

const model = defineModel<ArtDetailDisplayConfigVO>({ required: true });
type SceneKey = 'audit' | 'score' | 'review';
type PopupTabKey = ArtAuditDetailTab | ArtScoreDetailTab;
type Selection = { kind: 'none' } | { kind: 'tab'; key: PopupTabKey } | { kind: 'region'; key: string };

const scene = ref<SceneKey>('audit');
const previewTab = ref<PopupTabKey>('preview');
const selection = ref<Selection>({ kind: 'tab', key: 'preview' });
const tabDragIndex = ref<number>();
const sceneOptions = [
  { label: '审核详情', value: 'audit' },
  { label: '评分详情', value: 'score' },
  { label: '评审工作台', value: 'review' }
];
const reviewRegionOptions = [
  { key: 'reviewTitle', label: '活动标题与搜索' },
  { key: 'reviewProgress', label: '筛选与评分进度' },
  { key: 'reviewColumns', label: '评分列表配置说明' },
  { key: 'reviewSignature', label: '统一提交签字' }
];
const auditRegionOptions = [
  { key: 'auditSummary', label: '摘要字段' },
  { key: 'auditBasic', label: '基础信息固定区' },
  { key: 'files', label: '作品文件' },
  { key: 'auditRecords', label: '审核记录' },
  { key: 'auditActions', label: '审核处理区' }
];
const scoreRegionOptions = [
  { key: 'scoreBasic', label: '基础信息' },
  { key: 'files', label: '作品文件' },
  { key: 'scoreRecords', label: '评分记录' },
  { key: 'scoreCard', label: '评分卡片' }
];
const currentRegionOptions = computed(() => (scene.value === 'audit' ? auditRegionOptions : scoreRegionOptions));
const currentPopupTabs = computed(() => model.value.tabs[scene.value === 'audit' ? 'audit' : 'score'] as ArtDetailPopupTabConfig<PopupTabKey>);
const visiblePopupTabs = computed(() => currentPopupTabs.value.order.filter((key) => currentPopupTabs.value.visibleTabs.includes(key)));

const visibleColumns = computed(() =>
  model.value.listTableLayout.pages.review.columns.filter((column) => !column.deleted && (column.visible || column.key === 'actions'))
);
const propertyTitle = computed(() => {
  const currentSelection = selection.value;
  if (currentSelection.kind === 'tab') return `标签：${currentPopupTabs.value.labels[currentSelection.key] || currentSelection.key}`;
  if (currentSelection.kind === 'region') {
    return (
      [...auditRegionOptions, ...scoreRegionOptions, ...reviewRegionOptions].find((item) => item.key === currentSelection.key)?.label || '页面区域'
    );
  }
  return '尚未选择';
});
const reviewTitleStyle = computed(() => ({
  color: model.value.reviewWorkbench.activityTitle.color,
  fontSize: `${model.value.reviewWorkbench.activityTitle.fontSize}px`,
  fontWeight:
    model.value.reviewWorkbench.activityTitle.fontWeight === 'bold'
      ? 700
      : model.value.reviewWorkbench.activityTitle.fontWeight === 'medium'
        ? 600
        : 400,
  textAlign: model.value.reviewWorkbench.activityTitle.align
}));

const optionLabel = (options: Array<{ key: string; label: string }>, key: string) => options.find((item) => item.key === key)?.label || key;
const sampleFieldValue = (key: string) =>
  ({ activity: '河南省大学生艺术展演', category: '声乐', school: '示例学校', submittedAt: '2026-07-22', auditOpinion: '无' })[key] || '示例内容';
const sampleColumnValue = (key: string) =>
  ({
    projectNo: 'AR20260001',
    projectName: '项目名称',
    activityName: '艺术展演',
    categoryName: '声乐',
    scoreResult: '90',
    status: '已评分',
    actions: '查看评分'
  })[key] || '示例';
const selectTab = (key: PopupTabKey) => {
  selection.value = { kind: 'tab', key };
  if (currentPopupTabs.value.visibleTabs.includes(key)) previewTab.value = key;
};
const moveTab = (targetIndex: number) => {
  if (tabDragIndex.value === undefined || tabDragIndex.value === targetIndex) return;
  const [tab] = currentPopupTabs.value.order.splice(tabDragIndex.value, 1);
  currentPopupTabs.value.order.splice(targetIndex, 0, tab);
  tabDragIndex.value = undefined;
};
const isTabVisibilityLocked = (key: PopupTabKey) => {
  const visible = currentPopupTabs.value.visibleTabs;
  if (!visible.includes(key)) return false;
  if (visible.length <= 1) return true;
  return (key === 'preview' || key === 'files') && !visible.some((item) => item !== key && (item === 'preview' || item === 'files'));
};
const setTabVisible = (key: PopupTabKey, rawValue: boolean | string | number) => {
  const visible = currentPopupTabs.value.visibleTabs;
  const enabled = Boolean(rawValue);
  if (enabled && !visible.includes(key)) visible.push(key);
  if (!enabled && !isTabVisibilityLocked(key)) {
    const index = visible.indexOf(key);
    if (index >= 0) visible.splice(index, 1);
    if (currentPopupTabs.value.defaultTab === key) currentPopupTabs.value.defaultTab = visible[0];
    if (previewTab.value === key) previewTab.value = visible[0];
  }
};
watch(scene, (value) => {
  if (value === 'audit' || value === 'score') {
    previewTab.value = model.value.tabs[value].defaultTab;
    selection.value = { kind: 'tab', key: previewTab.value };
  } else {
    selection.value = { kind: 'region', key: 'reviewTitle' };
  }
});
</script>

<style scoped>
.crehn-visual {
  display: grid;
  gap: 12px;
  min-width: 0;
}

.crehn-visual__toolbar,
.crehn-visual__toolbar > div,
.crehn-structure > header,
.crehn-properties > header,
.crehn-scope-bar,
.art-popup-preview > header,
.art-popup-preview > footer,
.crehn-workbench-preview > header,
.crehn-workbench-preview > footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.crehn-visual__toolbar > div,
.crehn-structure > header,
.crehn-properties > header {
  align-items: flex-start;
  flex-direction: column;
  gap: 3px;
}

.crehn-visual__toolbar small,
.crehn-structure small,
.crehn-properties small {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.crehn-scope-bar {
  justify-content: flex-start;
  padding: 10px 12px;
  background: var(--el-fill-color-extra-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--app-radius-sm, var(--el-border-radius-small));
}

.crehn-scope-bar .el-select {
  width: 240px;
}

.crehn-visual__workspace {
  display: grid;
  grid-template-columns: 220px minmax(520px, 1fr) 300px;
  gap: 12px;
  min-height: 600px;
}

.crehn-structure,
.crehn-properties,
.crehn-preview {
  min-width: 0;
  padding: 12px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--app-radius-md, var(--el-border-radius-base));
}

.crehn-structure {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.crehn-structure__group {
  margin-top: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 700;
}

.crehn-structure__item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 7px;
  align-items: center;
  min-height: 38px;
  padding: 7px 9px;
  color: var(--el-text-color-primary);
  text-align: left;
  background: var(--el-fill-color-extra-light);
  border: 1px solid transparent;
  border-radius: var(--app-radius-sm, var(--el-border-radius-small));
  cursor: pointer;
}

.crehn-structure__item.is-active {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-5);
}

.crehn-structure__item.is-hidden {
  opacity: 0.5;
}

.crehn-preview {
  display: grid;
  align-content: start;
  padding: 18px;
  background-color: #f4f7fb;
  background-image: radial-gradient(var(--el-border-color-light) 0.7px, transparent 0.7px);
  background-size: 14px 14px;
}

.art-popup-preview,
.crehn-workbench-preview {
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--app-radius-md, var(--el-border-radius-base));
  box-shadow: 0 14px 34px rgb(15 23 42 / 8%);
}

.art-popup-preview > header,
.art-popup-preview > footer,
.crehn-workbench-preview > header,
.crehn-workbench-preview > footer {
  padding: 12px 14px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.art-popup-preview > footer,
.crehn-workbench-preview > footer {
  justify-content: flex-end;
  border-top: 1px solid var(--el-border-color-lighter);
  border-bottom: 0;
}

.art-popup-preview__tabs {
  display: flex;
  gap: 4px;
  padding: 0 12px;
  overflow-x: auto;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.art-popup-preview__tabs button {
  padding: 11px 12px 9px;
  color: var(--el-text-color-regular);
  background: transparent;
  border: 0;
  border-bottom: 2px solid transparent;
}

.art-popup-preview__tabs button.is-active {
  color: var(--el-color-primary);
  border-bottom-color: var(--el-color-primary);
}

.art-popup-preview__body {
  min-height: 360px;
  padding: 16px;
}

.art-preview-placeholder {
  display: grid;
  min-height: 330px;
  place-items: center;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-extra-light);
  border: 1px dashed var(--el-border-color);
  border-radius: var(--app-radius-sm, var(--el-border-radius-small));
}

.art-preview-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.art-preview-form-grid > div {
  display: grid;
  gap: 4px;
  padding: 12px;
  background: var(--el-fill-color-extra-light);
  border-radius: var(--app-radius-sm, var(--el-border-radius-small));
  cursor: pointer;
}

.art-preview-form-grid small {
  color: var(--el-text-color-secondary);
}

.art-preview-table,
.crehn-table-preview {
  overflow-x: auto;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--app-radius-sm, var(--el-border-radius-small));
}

.art-preview-table > div,
.crehn-table-preview > div {
  display: flex;
  min-width: max-content;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.art-preview-table span,
.crehn-table-preview span,
.crehn-table-preview button {
  display: flex;
  min-width: 100px;
  min-height: 42px;
  align-items: center;
  padding: 8px 10px;
  color: inherit;
  background: transparent;
  border: 0;
  border-right: 1px solid var(--el-border-color-lighter);
}

.art-preview-table .is-head,
.crehn-table-preview .is-head {
  font-weight: 700;
  background: #eef4ff;
}

.art-score-card-preview {
  flex-wrap: wrap;
}

.crehn-workbench-preview {
  display: grid;
  gap: 0;
}

.crehn-workbench-preview > header {
  align-items: center;
}

.crehn-workbench-preview__actions {
  display: flex;
  gap: 8px;
}

.crehn-workbench-preview__actions .el-input {
  width: 190px;
}

.crehn-workbench-preview__filter {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.crehn-table-preview {
  margin: 14px;
}

.crehn-properties {
  max-height: 690px;
  overflow: auto;
}

.crehn-properties :deep(.el-form-item) {
  margin-bottom: 14px;
}

.crehn-properties__checks,
.crehn-properties__checks :deep(.el-form-item__content) {
  display: grid;
}

.crehn-properties__trash-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 7px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

@media (max-width: 1200px) {
  .crehn-visual__workspace {
    grid-template-columns: 200px minmax(460px, 1fr);
  }

  .crehn-properties {
    grid-column: 1 / -1;
    max-height: none;
  }
}

@media (max-width: 800px) {
  .crehn-visual__toolbar,
  .crehn-scope-bar,
  .crehn-workbench-preview > header,
  .crehn-workbench-preview__filter {
    align-items: stretch;
    flex-direction: column;
  }

  .crehn-visual__workspace {
    grid-template-columns: 1fr;
  }

  .crehn-scope-bar .el-select {
    width: 100%;
  }
}
</style>
