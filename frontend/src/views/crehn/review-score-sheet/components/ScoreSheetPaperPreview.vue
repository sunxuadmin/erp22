<template>
  <div :class="['paper-preview', { 'is-compact': compact }]">
    <h2>{{ template.title || '评审打分表' }}</h2>
    <el-table :data="rows" :max-height="tableMaxHeight" border size="small" empty-text="暂无预览数据">
      <el-table-column v-for="column in visibleColumns" :key="column.key" :label="column.label" :prop="column.key" min-width="105" />
    </el-table>
    <div v-if="template.footerRows?.length" class="paper-footer-list">
      <div v-for="(row, index) in template.footerRows" :key="index" class="paper-footer-row">
        <div :class="['paper-footer-side', 'is-left', { 'has-signature-slot': shouldShowSignatureField(row.left) }]">
          <template v-if="row.left && shouldShowFooterField(row.left)">
            <span class="footer-label">{{ row.left.label }}</span>
            <span v-if="row.left.type === 'exportTime'" class="footer-time-value">{{ exportTime }}</span>
            <div
              v-else-if="isScoreSheetSignatureField(row.left)"
              :class="[
                'paper-signature-slot',
                `is-${row.left.lineLength}`,
                { 'is-drop-ready': interactive, 'has-signature': placementFor(row.left) }
              ]"
              @click="handleSlotClick(row.left)"
            >
              <span v-if="row.left.lineLength !== 'none'" :class="['footer-line', `is-${row.left.lineLength}`]" />
              <div v-if="interactive" :class="['signature-edit-controls', { 'has-signature': placementFor(row.left) }]">
                <el-button class="signature-action-button" type="primary" plain size="small" @click.stop="requestSignature(row.left)">
                  {{ placementFor(row.left) ? '更换签名' : scoreSheetSignatureButtonText(row.left) }}
                </el-button>
                <span v-if="!placementFor(row.left)" class="signature-drop-hint"> 点击签字或选择签字后直接填入 </span>
              </div>
              <img
                v-if="signatureUrlFor(row.left)"
                class="signature-image"
                :src="signatureUrlFor(row.left)"
                alt="评分老师签名"
                :draggable="false"
                :style="placementStyle(row.left)"
                @click.stop
              />
              <span v-if="placementFor(row.left)?.signedAt && scoreSheetSignatureTimeVisible(row.left)" class="signature-time">
                {{ scoreSheetSignatureTimeText(row.left) }}{{ placementFor(row.left)?.signedAt }}
              </span>
            </div>
            <span v-else-if="row.left.lineLength !== 'none'" :class="['footer-line', `is-${row.left.lineLength}`]" />
          </template>
        </div>
        <div :class="['paper-footer-side', 'is-right', { 'has-signature-slot': shouldShowSignatureField(row.right) }]">
          <template v-if="row.right && shouldShowFooterField(row.right)">
            <span class="footer-label">{{ row.right.label }}</span>
            <span v-if="row.right.type === 'exportTime'" class="footer-time-value">{{ exportTime }}</span>
            <div
              v-else-if="isScoreSheetSignatureField(row.right)"
              :class="[
                'paper-signature-slot',
                `is-${row.right.lineLength}`,
                { 'is-drop-ready': interactive, 'has-signature': placementFor(row.right) }
              ]"
              @click="handleSlotClick(row.right)"
            >
              <span v-if="row.right.lineLength !== 'none'" :class="['footer-line', `is-${row.right.lineLength}`]" />
              <div v-if="interactive" :class="['signature-edit-controls', { 'has-signature': placementFor(row.right) }]">
                <el-button class="signature-action-button" type="primary" plain size="small" @click.stop="requestSignature(row.right)">
                  {{ placementFor(row.right) ? '更换签名' : scoreSheetSignatureButtonText(row.right) }}
                </el-button>
                <span v-if="!placementFor(row.right)" class="signature-drop-hint"> 点击签字或选择签字后直接填入 </span>
              </div>
              <img
                v-if="signatureUrlFor(row.right)"
                class="signature-image"
                :src="signatureUrlFor(row.right)"
                alt="评分老师签名"
                :draggable="false"
                :style="placementStyle(row.right)"
                @click.stop
              />
              <span v-if="placementFor(row.right)?.signedAt && scoreSheetSignatureTimeVisible(row.right)" class="signature-time">
                {{ scoreSheetSignatureTimeText(row.right) }}{{ placementFor(row.right)?.signedAt }}
              </span>
            </div>
            <span v-else-if="row.right.lineLength !== 'none'" :class="['footer-line', `is-${row.right.lineLength}`]" />
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type {
  ReviewScoreSheetFooterFieldVO,
  ReviewScoreSheetSignaturePlacementVO,
  ReviewScoreSheetSignatureVO,
  ReviewScoreSheetTemplateVO
} from '@/api/crehn/types';
import {
  isScoreSheetSignatureField,
  scoreSheetSignatureButtonText,
  scoreSheetSignatureRequired,
  scoreSheetSignatureSlotKey,
  scoreSheetSignatureTimeText,
  scoreSheetSignatureTimeVisible
} from '../scoreSheet';

const props = withDefaults(
  defineProps<{
    template: ReviewScoreSheetTemplateVO;
    rows?: Array<Record<string, string | number | undefined>>;
    exportTime?: string;
    tableMaxHeight?: string | number;
    compact?: boolean;
    signature?: ReviewScoreSheetSignatureVO;
    signaturePlacement?: ReviewScoreSheetSignaturePlacementVO | null;
    interactive?: boolean;
    /** Shows an optional signature slot before a signature is selected. */
    showOptionalSignature?: boolean;
  }>(),
  {
    rows: () => [],
    exportTime: '2026-07-14 10:35',
    tableMaxHeight: undefined,
    compact: false,
    signature: undefined,
    signaturePlacement: undefined,
    interactive: false,
    showOptionalSignature: false
  }
);

const emit = defineEmits<{
  (e: 'signature-click', field: ReviewScoreSheetFooterFieldVO): void;
}>();

const visibleColumns = computed(() => (props.template.columns || []).filter((column) => column.visible));
const clamp = (value: number, min: number, max: number) => Math.min(Math.max(value, min), max);

const placementFor = (field?: ReviewScoreSheetFooterFieldVO | null) => {
  const slotKey = scoreSheetSignatureSlotKey(field);
  if (!slotKey || props.signaturePlacement?.slotKey !== slotKey) return undefined;
  return props.signaturePlacement;
};

const shouldShowSignatureField = (field?: ReviewScoreSheetFooterFieldVO | null) =>
  isScoreSheetSignatureField(field) && (props.showOptionalSignature || scoreSheetSignatureRequired(field) || !!placementFor(field));

const shouldShowFooterField = (field?: ReviewScoreSheetFooterFieldVO | null) => !isScoreSheetSignatureField(field) || shouldShowSignatureField(field);

const signatureUrlFor = (field?: ReviewScoreSheetFooterFieldVO | null) => {
  const placement = placementFor(field);
  return placement ? placement.signatureUrl || props.signature?.imageUrl || props.signature?.url || '' : '';
};

const placementStyle = (field?: ReviewScoreSheetFooterFieldVO | null) => {
  const placement = placementFor(field);
  if (!placement) return undefined;
  return {
    left: `${clamp(Number(placement.x || 0), 0, 1) * 100}%`,
    top: `${clamp(Number(placement.y || 0), 0, 1) * 100}%`,
    width: `${clamp(Number(placement.width || 0.54), 0.01, 1) * 100}%`,
    height: `${clamp(Number(placement.height || 0.76), 0.01, 1) * 100}%`
  };
};

const requestSignature = (field: ReviewScoreSheetFooterFieldVO) => {
  if (!props.interactive) return;
  emit('signature-click', field);
};

const handleSlotClick = (field: ReviewScoreSheetFooterFieldVO) => requestSignature(field);

</script>

<style scoped lang="scss">
.paper-preview {
  min-height: 420px;
  padding: 28px 24px 34px;
  overflow-x: auto;
  background: #fff;
  border: 1px solid var(--el-border-color-light);
  box-shadow: 0 6px 18px rgb(30 64 175 / 8%);

  h2 {
    margin: 0 0 24px;
    color: #1f2937;
    font-size: 22px;
    text-align: center;
  }

  &.is-compact {
    min-height: 0;
    padding-bottom: 24px;
  }
}

.paper-footer-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 28px;
  color: #303133;
}

.paper-footer-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 24px;
  min-height: 34px;
}

.paper-footer-side {
  display: flex;
  min-width: 0;
  align-items: flex-end;
  gap: 6px;
  white-space: nowrap;

  .footer-label {
    flex: 0 1 auto;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &.is-right {
    justify-content: flex-end;
    text-align: right;
  }

  &.has-signature-slot .footer-label {
    margin-bottom: 17px;
  }
}

.footer-time-value {
  flex: 0 1 auto;
  overflow: hidden;
  text-overflow: ellipsis;
}

.footer-line {
  display: inline-block;
  flex: 0 1 auto;
  min-width: 36px;
  max-width: 100%;
  border-bottom: 1px solid #303133;

  &.is-short {
    width: 72px;
  }

  &.is-medium {
    width: 120px;
  }

  &.is-long {
    width: 180px;
  }
}

.paper-signature-slot {
  position: relative;
  flex: 0 1 auto;
  width: 180px;
  max-width: 100%;
  height: 70px;
  overflow: hidden;
  vertical-align: bottom;
  border-radius: 3px;

  &.is-short {
    width: 72px;
  }

  &.is-medium {
    width: 120px;
  }

  &.is-drop-ready {
    background: rgb(var(--el-color-primary-rgb) / 5%);
    outline: 1px dashed rgb(var(--el-color-primary-rgb) / 45%);
  }

  .footer-line {
    position: absolute;
    right: 0;
    bottom: 17px;
    width: 100%;
  }
}

.signature-edit-controls {
  position: absolute;
  z-index: 2;
  inset: 4px 2px 22px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  pointer-events: none;

  .signature-action-button {
    pointer-events: auto;
  }

  &.has-signature {
    inset: 4px 4px auto auto;
  }
}

.signature-drop-hint {
  color: var(--el-color-primary);
  font-size: 12px;
  pointer-events: none;
}

.signature-image {
  position: absolute;
  z-index: 1;
  object-fit: contain;
  object-position: center;
  cursor: grab;
  user-select: none;

  &:active {
    cursor: grabbing;
  }
}

.signature-time {
  position: absolute;
  right: 0;
  bottom: 0;
  max-width: 100%;
  overflow: hidden;
  color: #64748b;
  font-size: 11px;
  text-overflow: ellipsis;
}

@media (max-width: 680px) {
  .paper-footer-row {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .paper-footer-side.is-right {
    justify-content: flex-start;
    text-align: left;
  }
}
</style>
