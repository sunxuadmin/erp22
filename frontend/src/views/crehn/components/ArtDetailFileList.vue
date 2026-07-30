<template>
  <section class="art-detail-file-list">
    <div class="art-detail-file-list__head">
      <strong>{{ title }}</strong>
      <span>{{ counterText || `${files.length} 个` }}</span>
    </div>

    <div class="art-detail-file-list__items">
      <article v-for="(file, index) in files" :key="fileKey(file, index)" :class="fileItemClass(file, index)">
        <button type="button" class="art-detail-file-list__main" @click="selectFile(file)">
          <span class="art-detail-file-list__index">{{ index + 1 }}</span>
          <span class="art-detail-file-list__content">
            <strong class="art-detail-file-list__name">{{ fileTitle(file, index) }}</strong>
            <span v-if="fileMediaType(file) || compactSummary(file).length" class="art-detail-file-list__summary">
              <span v-if="fileMediaType(file)" class="art-detail-file-list__type">{{ fileMediaType(file) }}</span>
              <span v-for="item in compactSummary(file)" :key="item">{{ item }}</span>
            </span>
          </span>
        </button>

        <el-tooltip
          v-if="hasDetailItems(file)"
          :content="expandedFileKey === fileKey(file, index) ? '收起详细参数' : '查看详细参数'"
          placement="left"
        >
          <button
            type="button"
            class="art-detail-file-list__detail-toggle"
            :class="{ 'is-open': expandedFileKey === fileKey(file, index) }"
            :aria-label="expandedFileKey === fileKey(file, index) ? '收起详细参数' : '查看详细参数'"
            :aria-pressed="expandedFileKey === fileKey(file, index)"
            @click.stop="toggleFileDetails(file, index)"
          >
            <el-icon><WarningFilled /></el-icon>
          </button>
        </el-tooltip>
      </article>
    </div>

    <section v-if="expandedFile && expandedDetailItems.length" class="art-detail-file-list__details">
      <div class="art-detail-file-list__details-head">
        <strong>详细参数</strong>
        <span>再次点击信息按钮可收起</span>
      </div>
      <dl class="art-detail-file-list__detail-grid">
        <div v-for="item in expandedDetailItems" :key="item.key" class="art-detail-file-list__detail-item" :class="{ 'is-full': item.fullWidth }">
          <dt>{{ item.label }}</dt>
          <dd>
            <template v-if="item.key === 'checkStatus'">
              <el-tag size="small" :type="checkTagType(expandedFile.checkStatus)">{{ item.value }}</el-tag>
              <el-popover v-if="expandedFailureMessage" placement="left-start" trigger="click" :width="320">
                <div class="art-detail-file-list__failure">{{ expandedFailureMessage }}</div>
                <template #reference>
                  <el-button class="art-detail-file-list__failure-link" link type="danger" size="small">查看原因</el-button>
                </template>
              </el-popover>
            </template>
            <template v-else>{{ item.value }}</template>
          </dd>
        </div>
      </dl>
    </section>
  </section>
</template>

<script setup lang="ts">
import type { ProjectFileVO, ProjectVO } from '@/api/crehn/types';
import {
  detailCheckFailureMessage,
  detailFileCompactSummary,
  detailFileFieldValue,
  detailFileTypeLabel,
  detailMediaTypeLabel
} from './artDetailFileDisplay';

const props = withDefaults(
  defineProps<{
    files: readonly ProjectFileVO[];
    project?: ProjectVO;
    title: string;
    counterText?: string;
    selectedKey?: string | number;
    viewedKeys?: readonly string[];
    detailFields?: readonly string[];
    detailLabels?: Readonly<Record<string, string>>;
  }>(),
  {
    counterText: '',
    selectedKey: '',
    viewedKeys: () => [],
    detailFields: () => [],
    detailLabels: () => ({})
  }
);

const emit = defineEmits<{
  select: [file: ProjectFileVO];
}>();

const expandedFileKey = ref('');
const fullWidthDetailFields = new Set(['fileType', 'mimeType', 'checkStatus', 'technicalRequirements']);
const fileKey = (file: ProjectFileVO, index = 0) => String(file.id || file.originalName || file.storagePath || file.previewPath || index);
const fileTitle = (file: ProjectFileVO, index: number) =>
  file.originalName || detailFileTypeLabel(props.project, file) || `${props.title} ${index + 1}`;
const fileMediaType = (file: ProjectFileVO) => detailMediaTypeLabel(file);
const compactSummary = (file: ProjectFileVO) => detailFileCompactSummary(props.project, file);
const detailItems = (file: ProjectFileVO) =>
  props.detailFields
    .filter((key) => key !== 'fileName')
    .map((key) => ({
      key,
      label: props.detailLabels[key] || key,
      value: detailFileFieldValue(props.project, file, key),
      fullWidth: fullWidthDetailFields.has(key)
    }))
    .filter((item) => item.value);
const hasDetailItems = (file: ProjectFileVO) => detailItems(file).length > 0;
const expandedFile = computed(() => props.files.find((file, index) => fileKey(file, index) === expandedFileKey.value));
const expandedDetailItems = computed(() => (expandedFile.value ? detailItems(expandedFile.value) : []));
const expandedFailureMessage = computed(() => (expandedFile.value ? detailCheckFailureMessage(props.project, expandedFile.value) : ''));

const fileItemClass = (file: ProjectFileVO, index: number) => {
  const key = fileKey(file, index);
  return {
    'art-detail-file-list__item': true,
    'is-active': key === String(props.selectedKey || ''),
    'is-viewed': props.viewedKeys.includes(key),
    'is-pending': ['queued', 'converting', 'processing'].includes(file.previewStatus || ''),
    'is-failed': file.previewStatus === 'failed'
  };
};

const selectFile = (file: ProjectFileVO) => {
  expandedFileKey.value = '';
  emit('select', file);
};

const toggleFileDetails = (file: ProjectFileVO, index: number) => {
  const key = fileKey(file, index);
  if (expandedFileKey.value === key) {
    expandedFileKey.value = '';
    return;
  }
  expandedFileKey.value = key;
  emit('select', file);
};

const checkTagType = (status?: string) => {
  if (status === 'passed') return 'success';
  if (status === 'failed') return 'danger';
  return 'warning';
};

watch(
  () => props.selectedKey,
  (key) => {
    if (expandedFileKey.value && String(key || '') !== expandedFileKey.value) expandedFileKey.value = '';
  }
);

watch(
  () => props.project?.id,
  () => {
    expandedFileKey.value = '';
  }
);
</script>

<style scoped>
.art-detail-file-list {
  min-width: 0;
}

.art-detail-file-list__head,
.art-detail-file-list__details-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.art-detail-file-list__head {
  margin-bottom: 5px;
}

.art-detail-file-list__head strong,
.art-detail-file-list__details-head strong {
  color: #082f49;
  font-size: 14px;
}

.art-detail-file-list__head span,
.art-detail-file-list__details-head span {
  color: #64748b;
  font-size: 11px;
}

.art-detail-file-list__items {
  display: grid;
  max-height: min(190px, 25vh);
  overflow-y: auto;
  border: 1px solid #e2e8f0;
  border-radius: 7px;
}

.art-detail-file-list__item {
  display: grid;
  min-width: 0;
  grid-template-columns: minmax(0, 1fr) 30px;
  align-items: stretch;
  background: #fff;
  border-bottom: 1px solid #edf2f7;
}

.art-detail-file-list__item:last-child {
  border-bottom: 0;
}

.art-detail-file-list__item:hover {
  background: #f8fbff;
}

.art-detail-file-list__item.is-active {
  background: #eef5ff;
  box-shadow: inset 3px 0 0 var(--el-color-primary);
}

.art-detail-file-list__item.is-viewed:not(.is-active) {
  background: #f0fdf4;
}

.art-detail-file-list__item.is-pending:not(.is-active) {
  background: #fffbeb;
}

.art-detail-file-list__item.is-failed:not(.is-active) {
  background: #fff1f2;
}

.art-detail-file-list__main {
  display: grid;
  min-width: 0;
  grid-template-columns: 21px minmax(0, 1fr);
  align-items: start;
  gap: 7px;
  padding: 8px 4px 8px 8px;
  color: inherit;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.art-detail-file-list__index {
  display: inline-flex;
  width: 20px;
  height: 20px;
  align-items: center;
  justify-content: center;
  margin-top: 1px;
  color: #64748b;
  font-size: 11px;
  font-weight: 700;
  background: #f1f5f9;
  border-radius: 50%;
}

.art-detail-file-list__content,
.art-detail-file-list__name {
  min-width: 0;
}

.art-detail-file-list__name {
  display: -webkit-box;
  overflow: hidden;
  color: #0f2f4a;
  font-size: 14px;
  font-weight: 750;
  line-height: 1.35;
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.art-detail-file-list__summary {
  display: flex;
  min-width: 0;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px 8px;
  margin-top: 3px;
  color: #64748b;
  font-size: 11px;
  line-height: 1.35;
}

.art-detail-file-list__summary > span:not(.art-detail-file-list__type)::before {
  margin-right: 8px;
  color: #cbd5e1;
  content: '·';
}

.art-detail-file-list__type {
  color: #2563eb;
  font-weight: 650;
}

.art-detail-file-list__detail-toggle {
  display: inline-flex;
  width: 28px;
  height: 28px;
  align-items: center;
  justify-content: center;
  align-self: start;
  margin: 6px 2px 0 0;
  padding: 0;
  color: #64748b;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 50%;
}

.art-detail-file-list__detail-toggle:hover,
.art-detail-file-list__detail-toggle.is-open {
  color: var(--el-color-primary);
  background: #dbeafe;
}

.art-detail-file-list__details {
  max-height: min(220px, 30vh);
  margin-top: 7px;
  padding: 7px;
  overflow-y: auto;
  background: #f8fafc;
  border: 1px solid #dbe5f0;
  border-radius: 7px;
}

.art-detail-file-list__details-head {
  margin-bottom: 6px;
}

.art-detail-file-list__detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 5px 9px;
  margin: 0;
}

.art-detail-file-list__detail-item {
  min-width: 0;
  padding: 4px 5px;
  background: #fff;
  border-radius: 4px;
}

.art-detail-file-list__detail-item.is-full {
  grid-column: 1 / -1;
}

.art-detail-file-list__detail-item dt {
  margin-bottom: 1px;
  color: #64748b;
  font-size: 11px;
}

.art-detail-file-list__detail-item dd {
  min-width: 0;
  margin: 0;
  color: #25364b;
  font-size: 12px;
  line-height: 1.45;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}

.art-detail-file-list__failure-link {
  height: auto;
  margin-left: 5px;
  padding: 0;
  font-size: 11px;
}

.art-detail-file-list__failure {
  max-height: 180px;
  overflow-y: auto;
  color: #b42318;
  font-size: 12px;
  line-height: 1.55;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

@media (max-width: 768px) {
  .art-detail-file-list__items {
    max-height: 170px;
  }

  .art-detail-file-list__details {
    max-height: 190px;
  }

  .art-detail-file-list__detail-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .art-detail-file-list__detail-item.is-full {
    grid-column: auto;
  }
}
</style>
