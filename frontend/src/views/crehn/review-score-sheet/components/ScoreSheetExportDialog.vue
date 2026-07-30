<template>
  <ArtPopupDialog
    v-model="visible"
    title="评审范围分批提交签字"
    width="90%"
    top="3vh"
    append-to-body
    destroy-on-close
    class="score-sheet-export-dialog"
    @closed="editorVisible = false"
  >
    <div v-loading="loading" class="export-dialog-shell">
      <el-alert class="mb-[10px]" type="info" show-icon :closable="false" :title="previewNotice" />

      <div class="export-toolbar">
        <div class="export-toolbar-item">
          <span>使用模板</span>
          <el-select v-model="selectedTemplateKey" style="width: 260px" :disabled="!!savedSheet" @change="handleTemplateChange">
            <el-option v-for="item in templates" :key="templateKey(item)" :label="item.templateName" :value="templateKey(item)">
              <span>{{ item.templateName }}</span>
              <el-tag v-if="item.defaultTemplate" class="ml-[8px]" size="small" type="success">默认</el-tag>
            </el-option>
          </el-select>
        </div>
        <div class="export-toolbar-item">
          <span>{{ savedSheet ? (savedRecordLabel === '签名表' ? '签名时间' : '提交时间') : '预览时间' }}</span>
          <el-input :model-value="savedSheet?.signedAt || exportTime" readonly style="width: 190px" />
        </div>
        <el-button v-if="!savedSheet && signatureInteractionVisible" plain icon="Edit" @click="openSignatureLibrary()">选择签字</el-button>
        <el-button v-else-if="!savedSheet && hasSignatureSlot" plain icon="Edit" @click="enableOptionalSignature">添加签名（可选）</el-button>
        <el-button plain icon="Tickets" @click="openSignedSheetList">历史提交批次</el-button>
        <el-tag v-if="savedSheet" :type="isWithdrawnSheet(savedSheet) ? 'warning' : 'success'" effect="plain">
          {{ isWithdrawnSheet(savedSheet) ? `${savedRecordLabel}已撤回` : `${savedRecordLabel}已保存` }}
        </el-tag>
        <div class="export-toolbar-spacer" />
        <el-button type="primary" plain icon="Edit" :disabled="!!savedSheet" @click="openEditor">编辑本次版式</el-button>
      </div>

      <el-alert
        v-if="!savedSheet && signatureRequired && !hasValidSignaturePlacement"
        class="mt-[10px]"
        type="warning"
        show-icon
        :closable="false"
        title="点击评分老师签字处即可填入当前签名；也可以点击“选择签字”，点选后直接填入。"
      />

      <div class="simple-preview-panel">
        <div class="preview-section-head">
          <strong>{{ savedSheet ? `已保存${savedRecordLabel}预览` : '本批待提交评分表预览' }}</strong>
          <span>本批共 {{ previewRows.length }} 条，记录过长时可在表格内部滚动</span>
        </div>
        <ScoreSheetPaperPreview
          :template="form"
          :rows="previewRows"
          :export-time="exportTime"
          :signature="activeSignature"
          :signature-placement="signaturePlacement"
          :interactive="!savedSheet"
          :show-optional-signature="optionalSignatureVisible"
          table-max-height="50vh"
          compact
          @signature-click="handleSignatureSlotClick"
        />
      </div>
    </div>

    <template #footer>
      <div class="export-dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <template v-if="savedSheet">
          <el-button type="primary" plain icon="Printer" :disabled="isWithdrawnSheet(savedSheet)" @click="printSignedSheet">打印评分表</el-button>
          <el-button type="success" icon="Download" :loading="exporting" :disabled="isWithdrawnSheet(savedSheet)" @click="downloadSignedSheet">
            下载评分表
          </el-button>
        </template>
        <el-button v-else type="primary" icon="Check" :loading="savingSignedSheet" :disabled="!canSaveSignedSheet" @click="saveSignedSheet">
          {{ saveActionText }}
        </el-button>
      </div>
    </template>
  </ArtPopupDialog>

  <ArtPopupDialog
    v-model="editorVisible"
    title="编辑本次导出版式"
    width="96%"
    top="3vh"
    append-to-body
    destroy-on-close
    class="score-sheet-layout-editor-dialog"
  >
    <el-alert
      class="mb-[10px]"
      type="info"
      show-icon
      :closable="false"
      title="直接应用只影响本次待保存评分表；签名必填和签名时间显示属于共享模板策略，修改后需保存模板才会生效。"
    />

    <div class="editor-dialog-toolbar">
      <div>
        <strong>{{ editingForm.templateName || '当前模板' }}</strong>
        <el-tag type="info" effect="plain">本次评分表版式</el-tag>
      </div>
      <el-button icon="RefreshLeft" @click="restoreEditorTemplate">恢复所选模板</el-button>
    </div>

    <div class="editor-dialog-layout">
      <el-scrollbar class="editor-config-scroll">
        <div class="editor-config-panel">
          <ScoreSheetTemplateEditor v-model="editingForm" :show-template-name="false" />
        </div>
      </el-scrollbar>
      <el-scrollbar class="editor-preview-scroll">
        <div class="editor-preview-panel">
          <div class="preview-section-head">
            <strong>版式效果</strong>
            <span>使用当前 {{ previewRows.length }} 条待签名评分数据</span>
          </div>
          <ScoreSheetPaperPreview
            :template="editingForm"
            :rows="previewRows"
            :export-time="exportTime"
            table-max-height="40vh"
            compact
            show-optional-signature
          />
        </div>
      </el-scrollbar>
    </div>

    <template #footer>
      <div class="editor-dialog-footer">
        <span>保存签名表后，评分、模板、签名和签名时间都会冻结为历史快照。</span>
        <div>
          <el-button @click="editorVisible = false">取消</el-button>
          <el-button v-hasPermi="['crehn:reviewSheetTemplate:edit']" :loading="savingTemplate" @click="saveAsNewTemplate">另存为新模板</el-button>
          <el-button v-hasPermi="['crehn:reviewSheetTemplate:edit']" type="primary" plain :loading="savingTemplate" @click="saveCurrentTemplate">
            保存当前模板
          </el-button>
          <el-button type="primary" icon="Check" @click="applyEditedLayout">应用到本次评分表</el-button>
        </div>
      </div>
    </template>
  </ArtPopupDialog>

  <ScoreSheetSignatureLibraryDialog
    ref="signatureLibraryRef"
    v-model="signatureLibraryVisible"
    v-model:selected-signature-id="selectedSignatureId"
    @select="handleSignatureSelect"
    @fill="handleSignatureFill"
  />
  <ScoreSheetSignedSheetListDialog ref="signedSheetListRef" @changed="reloadAfterSignedSheetWithdraw" />
</template>

<script setup lang="ts">
import {
  createReviewScoreSheetTemplate,
  exportReviewScoreSheetSignedSheet,
  getReviewScoreSheetSignedSheet,
  listReviewScoreSheetTemplates,
  previewReviewScoreSheetSignedSheet,
  saveReviewScoreSheetSignedSheet,
  updateReviewScoreSheetTemplate
} from '@/api/crehn/review';
import type {
  ReviewScoreSheetFooterFieldVO,
  ReviewScoreSheetSignaturePlacementVO,
  ReviewScoreSheetSignatureVO,
  ReviewScoreSheetSignedSheetForm,
  ReviewScoreSheetSignedSheetPreviewVO,
  ReviewScoreSheetSignedSheetVO,
  ReviewScoreSheetTemplateVO
} from '@/api/crehn/types';
import { blobValidate } from '@/utils/ruoyi';
import { ElMessageBox } from 'element-plus';
import FileSaver from 'file-saver';
import ScoreSheetPaperPreview from './ScoreSheetPaperPreview.vue';
import ScoreSheetSignatureLibraryDialog from './ScoreSheetSignatureLibraryDialog.vue';
import ScoreSheetSignedSheetListDialog from './ScoreSheetSignedSheetListDialog.vue';
import ScoreSheetTemplateEditor from './ScoreSheetTemplateEditor.vue';
import {
  cloneScoreSheetTemplate,
  emptyScoreSheetTemplate,
  isScoreSheetSignatureField,
  openScoreSheetPrintPreview,
  scoreSheetSignatureRequired,
  scoreSheetSignatureSlotKey,
  scoreTaskToPreviewRow,
  validateScoreSheetTemplate
} from '../scoreSheet';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const emit = defineEmits<{ (e: 'saved'): void }>();

const visible = ref(false);
const editorVisible = ref(false);
const signatureLibraryVisible = ref(false);
const loading = ref(false);
const savingTemplate = ref(false);
const savingSignedSheet = ref(false);
const exporting = ref(false);
const activityId = ref<string | number>();
const categoryId = ref<string | number>();
const scopeKey = ref('');
const scopeLabel = ref('');
const categoryName = ref('');
const reviewerName = ref('');
const exportTime = ref('');
const total = ref(0);
const assignedTotal = ref(0);
const completedCount = ref(0);
const submittedCount = ref(0);
const pendingCount = ref(0);
const remainingCount = ref(0);
const completeAfterSubmit = ref(false);
const templates = ref<ReviewScoreSheetTemplateVO[]>([]);
const selectedTemplateKey = ref('');
const selectedSignatureId = ref<string | number>();
const activeSignature = ref<ReviewScoreSheetSignatureVO>();
const signaturePlacement = ref<ReviewScoreSheetSignaturePlacementVO>();
const optionalSignatureVisible = ref(false);
const pendingSignatureSlotKey = ref('');
const savedSheet = ref<ReviewScoreSheetSignedSheetVO>();
const form = ref<ReviewScoreSheetTemplateVO>(emptyScoreSheetTemplate());
const editingForm = ref<ReviewScoreSheetTemplateVO>(cloneScoreSheetTemplate(form.value));
const originalTemplate = ref<ReviewScoreSheetTemplateVO>(cloneScoreSheetTemplate(form.value));
const previewRows = ref<Array<Record<string, string>>>([]);
const signatureLibraryRef = ref<InstanceType<typeof ScoreSheetSignatureLibraryDialog>>();
const signedSheetListRef = ref<InstanceType<typeof ScoreSheetSignedSheetListDialog>>();

const templateKey = (template: ReviewScoreSheetTemplateVO) => String(template.id ?? 'virtual-default');
const sameId = (left?: string | number, right?: string | number) => left != null && right != null && String(left) === String(right);
const sheetIdOf = (sheet?: ReviewScoreSheetSignedSheetVO) => sheet?.id ?? sheet?.sheetId;
const isWithdrawnSheet = (sheet?: ReviewScoreSheetSignedSheetVO) =>
  !!sheet?.withdrawnAt ||
  String(sheet?.status || '')
    .trim()
    .toLowerCase() === 'withdrawn';

const signatureFields = computed(() =>
  form.value.footerRows
    .flatMap((row) => [row.left, row.right])
    .filter((field): field is ReviewScoreSheetFooterFieldVO => isScoreSheetSignatureField(field))
);
const hasSignatureSlot = computed(() => signatureFields.value.length > 0);
const signatureRequired = computed(() => signatureFields.value.some((field) => scoreSheetSignatureRequired(field)));
const hasValidSignaturePlacement = computed(
  () => !!activeSignature.value?.id && !!signaturePlacement.value?.slotKey && sameId(activeSignature.value.id, signaturePlacement.value.signatureId)
);
const signatureInteractionVisible = computed(() => signatureRequired.value || optionalSignatureVisible.value || hasValidSignaturePlacement.value);
const saveActionText = computed(() =>
  signatureRequired.value || hasValidSignaturePlacement.value ? `签字并提交本批 ${total.value} 项` : `提交本批 ${total.value} 项`
);
const savedRecordLabel = computed(() => (String(savedSheet.value?.submissionMode || '').toUpperCase() === 'UNSIGNED' ? '评分提交单' : '签名表'));

const previewNotice = computed(() => {
  if (savedSheet.value) {
    if (isWithdrawnSheet(savedSheet.value)) {
      return `该${savedRecordLabel.value}已撤回，仅可在“历史提交批次”中查看撤回审计信息，不能打印或下载。`;
    }
    if (savedRecordLabel.value === '评分提交单') {
      return `“${scopeLabel.value || categoryName.value || savedSheet.value.categoryName || '当前范围'}”本批 ${total.value} 项评分已归档，提交时间：${savedSheet.value.signedAt || '-'}。`;
    }
    return `“${scopeLabel.value || categoryName.value || savedSheet.value.categoryName || '当前范围'}”本批 ${total.value} 项签名表已保存，签名时间：${savedSheet.value.signedAt || signaturePlacement.value?.signedAt || '-'}。`;
  }
  if (total.value > 0) {
    const completionText = completeAfterSubmit.value
      ? '提交后当前评审范围将全部完成'
      : `当前还有 ${pendingCount.value} 项未评分，本批提交后剩余 ${remainingCount.value} 项可继续评分`;
    return `“${scopeLabel.value || categoryName.value || '当前范围'}”共分配 ${assignedTotal.value} 项，已评分 ${completedCount.value} 项，已提交 ${submittedCount.value} 项，本批可提交 ${total.value} 项；${completionText}。本批评分提交后锁定，结果生成前可按批次撤回。`;
  }
  return '当前评审范围没有待提交的已评分作品；可在“历史提交批次”查看已保存记录。';
});

const canSaveSignedSheet = computed(() => total.value > 0 && (!signatureRequired.value || hasValidSignaturePlacement.value));

const validateTemplate = (template: ReviewScoreSheetTemplateVO) => {
  const validationMessage = validateScoreSheetTemplate(template);
  if (!validationMessage) return true;
  proxy?.$modal.msgWarning(validationMessage);
  return false;
};

const applyPreview = (preview: ReviewScoreSheetSignedSheetPreviewVO) => {
  form.value = cloneScoreSheetTemplate(preview.template);
  editingForm.value = cloneScoreSheetTemplate(preview.template);
  originalTemplate.value = cloneScoreSheetTemplate(preview.template);
  selectedTemplateKey.value = templateKey(preview.template);
  reviewerName.value = preview.reviewerName || '评审老师';
  categoryName.value = preview.categoryName || '当前类别';
  exportTime.value = preview.exportTime;
  total.value = Number(preview.total || 0);
  assignedTotal.value = Number(preview.assignedTotal || 0);
  completedCount.value = Number(preview.completedCount || 0);
  submittedCount.value = Number(preview.submittedCount || 0);
  pendingCount.value = Number(preview.pendingCount || 0);
  remainingCount.value = Number(preview.remainingCount || 0);
  completeAfterSubmit.value = preview.completeAfterSubmit === true;
  previewRows.value = (preview.rows || []).map((row) => scoreTaskToPreviewRow(row, reviewerName.value));
};

const signatureFromSheet = (sheet: ReviewScoreSheetSignedSheetVO): ReviewScoreSheetSignatureVO | undefined => {
  if (sheet.signature) return sheet.signature;
  if (!sheet.signatureId || !sheet.signatureUrl) return undefined;
  return {
    id: sheet.signatureId,
    signatureName: sheet.signatureName || '评分老师签名',
    imageUrl: sheet.signatureUrl
  };
};

const placementFromSheet = (sheet: ReviewScoreSheetSignedSheetVO): ReviewScoreSheetSignaturePlacementVO | undefined => {
  const placement = sheet.placement || sheet.signaturePlacement;
  return placement
    ? {
        ...placement,
        slotKey: placement.slotKey || sheet.slotKey || 'reviewer_signature',
        signedAt: placement.signedAt || sheet.signedAt
      }
    : undefined;
};

const applySavedSheet = (sheet: ReviewScoreSheetSignedSheetVO) => {
  savedSheet.value = sheet;
  if (sheet.template) {
    form.value = cloneScoreSheetTemplate(sheet.template);
    editingForm.value = cloneScoreSheetTemplate(sheet.template);
    originalTemplate.value = cloneScoreSheetTemplate(sheet.template);
    selectedTemplateKey.value = templateKey(sheet.template);
  }
  reviewerName.value = sheet.reviewerName || reviewerName.value;
  categoryName.value = sheet.categoryName || categoryName.value;
  exportTime.value = sheet.exportTime || sheet.signedAt || exportTime.value;
  total.value = Number(sheet.total ?? sheet.rows?.length ?? total.value);
  if (sheet.rows?.length) previewRows.value = sheet.rows.map((row) => scoreTaskToPreviewRow(row, reviewerName.value));
  activeSignature.value = signatureFromSheet(sheet) || activeSignature.value;
  selectedSignatureId.value = activeSignature.value?.id;
  signaturePlacement.value = placementFromSheet(sheet);
  optionalSignatureVisible.value = !!signaturePlacement.value;
};

const loadSavedSheet = async (sheetId: string | number) => {
  const { data } = await getReviewScoreSheetSignedSheet(sheetId);
  applySavedSheet(data);
  return data;
};

const open = async (nextActivityId: string | number, nextCategoryId: string | number, nextScopeKey: string, nextScopeLabel?: string) => {
  activityId.value = nextActivityId;
  categoryId.value = nextCategoryId;
  scopeKey.value = nextScopeKey;
  scopeLabel.value = nextScopeLabel || '';
  visible.value = true;
  editorVisible.value = false;
  savedSheet.value = undefined;
  signaturePlacement.value = undefined;
  optionalSignatureVisible.value = false;
  activeSignature.value = undefined;
  selectedSignatureId.value = undefined;
  loading.value = true;
  try {
    const [templateResponse, previewResponse] = await Promise.all([
      listReviewScoreSheetTemplates(),
      previewReviewScoreSheetSignedSheet({ activityId: nextActivityId, categoryId: nextCategoryId, scopeKey: nextScopeKey })
    ]);
    templates.value = templateResponse.data || [];
    const preview = previewResponse.data;
    applyPreview(preview);
    const existingSheetId = sheetIdOf(preview.existingSheet);
    if (existingSheetId) await loadSavedSheet(existingSheetId);
    await nextTick();
    await signatureLibraryRef.value?.load(activeSignature.value?.id);
  } catch (error) {
    visible.value = false;
    throw error;
  } finally {
    loading.value = false;
  }
};

const handleTemplateChange = (value: string) => {
  if (savedSheet.value) {
    selectedTemplateKey.value = templateKey(form.value);
    return;
  }
  const selected = templates.value.find((item) => templateKey(item) === value);
  if (!selected) return;
  form.value = cloneScoreSheetTemplate(selected);
  originalTemplate.value = cloneScoreSheetTemplate(selected);
  signaturePlacement.value = undefined;
  optionalSignatureVisible.value = false;
};

const openEditor = () => {
  if (savedSheet.value) return;
  editingForm.value = cloneScoreSheetTemplate(form.value);
  editorVisible.value = true;
};

const restoreEditorTemplate = () => {
  editingForm.value = cloneScoreSheetTemplate(originalTemplate.value);
};

const firstSignatureField = (template: ReviewScoreSheetTemplateVO) =>
  template.footerRows
    .flatMap((row) => [row.left, row.right])
    .find((field): field is ReviewScoreSheetFooterFieldVO => isScoreSheetSignatureField(field));

/** A reviewer may tune this one export layout, but cannot relax the saved template's signature policy. */
const preserveCurrentSignaturePolicy = (template: ReviewScoreSheetTemplateVO) => {
  const currentField = firstSignatureField(form.value);
  const nextField = firstSignatureField(template);
  if (!currentField || !nextField) return;
  nextField.signatureRequired = currentField.signatureRequired;
  nextField.signatureTimeVisible = currentField.signatureTimeVisible;
};

const openSignatureLibrary = async (slotKey = '') => {
  pendingSignatureSlotKey.value = slotKey;
  await signatureLibraryRef.value?.open(activeSignature.value?.id, true);
};

const enableOptionalSignature = async () => {
  const fallbackSlot = signatureFields.value.map((field) => scoreSheetSignatureSlotKey(field)).find(Boolean);
  if (!fallbackSlot) {
    proxy?.$modal.msgWarning('当前版式没有评分老师签字处，请先编辑本次版式');
    return;
  }
  optionalSignatureVisible.value = true;
  await nextTick();
  await openSignatureLibrary(fallbackSlot);
};

const handleSignatureSelect = (signature?: ReviewScoreSheetSignatureVO) => {
  if (savedSheet.value) return;
  activeSignature.value = signature;
  if (!signature || !sameId(signature.id, signaturePlacement.value?.signatureId)) {
    signaturePlacement.value = undefined;
    return;
  }
  if (signaturePlacement.value) {
    signaturePlacement.value = {
      ...signaturePlacement.value,
      signatureName: signature.signatureName,
      signatureUrl: signature.imageUrl || signature.url
    };
  }
};

const handleSignatureSlotClick = (field: ReviewScoreSheetFooterFieldVO) => {
  const slotKey = scoreSheetSignatureSlotKey(field);
  if (!slotKey || savedSheet.value) return;
  optionalSignatureVisible.value = true;
  pendingSignatureSlotKey.value = slotKey;
  const alreadyFilled = signaturePlacement.value?.slotKey === slotKey;
  if (!alreadyFilled && activeSignature.value?.id && activeSignature.value.defaultSignature) {
    handleSignatureFill(activeSignature.value);
    return;
  }
  openSignatureLibrary(slotKey);
};

const handleSignatureFill = (signature?: ReviewScoreSheetSignatureVO) => {
  if (!signature?.id || savedSheet.value) return;
  const fallbackSlot = form.value.footerRows
    .flatMap((row) => [row.left, row.right])
    .map((field) => scoreSheetSignatureSlotKey(field))
    .find(Boolean);
  const slotKey = pendingSignatureSlotKey.value || fallbackSlot;
  if (!slotKey) {
    proxy?.$modal.msgWarning('当前版式没有评分老师签字处，请先编辑本次版式');
    return;
  }
  activeSignature.value = signature;
  selectedSignatureId.value = signature.id;
  optionalSignatureVisible.value = true;
  signaturePlacement.value = {
    slotKey,
    signatureId: signature.id,
    signatureName: signature.signatureName,
    signatureUrl: signature.imageUrl || signature.url,
    x: 0.23,
    y: 0.06,
    width: 0.54,
    height: 0.76,
    signedAt: undefined
  };
  pendingSignatureSlotKey.value = '';
};

const saveSignedSheet = async () => {
  if (!validateTemplate(form.value) || !canSaveSignedSheet.value || !activityId.value || !categoryId.value) {
    if (!canSaveSignedSheet.value) {
      proxy?.$modal.msgWarning(signatureRequired.value ? '请先点击签字处填入手写签名' : '当前评审范围没有待提交的已评分作品');
    }
    return;
  }
  if (signatureInteractionVisible.value && !hasValidSignaturePlacement.value && signatureRequired.value) {
    proxy?.$modal.msgWarning('请先点击签字处填入手写签名');
    return;
  }
  try {
    await ElMessageBox.confirm(
      `确认提交“${scopeLabel.value || categoryName.value || '当前范围'}”本批 ${total.value} 项评分吗？提交后仅锁定本批作品，剩余 ${remainingCount.value} 项可在后续批次提交。`,
      '确认提交本批评分',
      { confirmButtonText: hasValidSignaturePlacement.value ? '确认签字并提交本批' : '确认提交本批', cancelButtonText: '取消', type: 'warning' }
    );
  } catch {
    return;
  }
  savingSignedSheet.value = true;
  try {
    const payload: ReviewScoreSheetSignedSheetForm = {
      activityId: activityId.value,
      categoryId: categoryId.value,
      scopeKey: scopeKey.value,
      templateId: form.value.id,
      template: cloneScoreSheetTemplate(form.value)
    };
    if (hasValidSignaturePlacement.value && signaturePlacement.value && activeSignature.value?.id) {
      payload.signatureId = activeSignature.value.id;
      payload.placement = {
        slotKey: signaturePlacement.value.slotKey,
        x: signaturePlacement.value.x,
        y: signaturePlacement.value.y,
        width: signaturePlacement.value.width,
        height: signaturePlacement.value.height
      };
    }
    const { data } = await saveReviewScoreSheetSignedSheet(payload);
    const savedId = sheetIdOf(data);
    if (savedId) {
      await loadSavedSheet(savedId);
    } else {
      applySavedSheet(data);
    }
    proxy?.$modal.msgSuccess(hasValidSignaturePlacement.value ? '本批签名表已保存并锁定对应评分' : '本批评分已提交归档并锁定，剩余作品仍可继续评分');
    emit('saved');
  } finally {
    savingSignedSheet.value = false;
  }
};

const printSignedSheet = async () => {
  const sheetId = sheetIdOf(savedSheet.value);
  if (!sheetId || isWithdrawnSheet(savedSheet.value)) {
    proxy?.$modal.msgWarning(`已撤回${savedRecordLabel.value}仅供审计查看，不能打印`);
    return;
  }
  try {
    const latestSheet = await loadSavedSheet(sheetId);
    if (isWithdrawnSheet(latestSheet)) {
      proxy?.$modal.msgWarning(`该${savedRecordLabel.value}已撤回，仅可审计查看，不能打印`);
      return;
    }
    const opened = openScoreSheetPrintPreview(form.value, previewRows.value, exportTime.value, signaturePlacement.value, activeSignature.value);
    if (!opened) proxy?.$modal.msgWarning('浏览器拦截了打印窗口，请允许当前网站打开弹窗后重试');
  } catch {
    // The request layer displays a current permission or state error.
  }
};

const downloadSignedSheet = async () => {
  const sheetId = sheetIdOf(savedSheet.value);
  if (!sheetId || isWithdrawnSheet(savedSheet.value)) {
    proxy?.$modal.msgWarning(`已撤回${savedRecordLabel.value}仅供审计查看，不能下载`);
    return;
  }
  exporting.value = true;
  try {
    const latestSheet = await loadSavedSheet(sheetId);
    if (isWithdrawnSheet(latestSheet)) {
      proxy?.$modal.msgWarning(`该${savedRecordLabel.value}已撤回，仅可审计查看，不能下载`);
      return;
    }
    const blob = await exportReviewScoreSheetSignedSheet(sheetId);
    if (!blobValidate(blob)) {
      const text = await new Blob([blob as any]).text();
      const error = JSON.parse(text || '{}');
      proxy?.$modal.msgError(error.msg || '下载评分表失败');
      return;
    }
    FileSaver.saveAs(
      new Blob([blob as any], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }),
      `${categoryName.value.replace(/[\\/:*?"<>|]/g, '_')}-签名评审打分表.xlsx`
    );
  } catch {
    proxy?.$modal.msgError('下载评分表失败');
  } finally {
    exporting.value = false;
  }
};

const openSignedSheetList = () => {
  signedSheetListRef.value?.open({ activityId: activityId.value, categoryId: categoryId.value });
};

/** A withdrawal frees only the effective signed-sheet occupancy. Refresh the
 * open export dialog so its next save sees the newly signable submitted scores. */
const reloadAfterSignedSheetWithdraw = async () => {
  if (!visible.value || !activityId.value || !categoryId.value) return;
  loading.value = true;
  try {
    savedSheet.value = undefined;
    signaturePlacement.value = undefined;
    optionalSignatureVisible.value = false;
    activeSignature.value = undefined;
    selectedSignatureId.value = undefined;
    const { data } = await previewReviewScoreSheetSignedSheet({
      activityId: activityId.value,
      categoryId: categoryId.value,
      scopeKey: scopeKey.value
    });
    applyPreview(data);
    const existingSheetId = sheetIdOf(data.existingSheet);
    if (existingSheetId) await loadSavedSheet(existingSheetId);
    await nextTick();
    await signatureLibraryRef.value?.load(activeSignature.value?.id);
  } finally {
    loading.value = false;
  }
};

const syncSavedTemplate = (template: ReviewScoreSheetTemplateVO) => {
  const saved = cloneScoreSheetTemplate(template);
  const index = templates.value.findIndex((item) => templateKey(item) === templateKey(saved));
  if (index >= 0) {
    templates.value.splice(index, 1, cloneScoreSheetTemplate(saved));
  } else {
    templates.value = templates.value.filter((item) => item.id != null);
    templates.value.push(cloneScoreSheetTemplate(saved));
  }
  form.value = cloneScoreSheetTemplate(saved);
  editingForm.value = cloneScoreSheetTemplate(saved);
  originalTemplate.value = cloneScoreSheetTemplate(saved);
  selectedTemplateKey.value = templateKey(saved);
  signaturePlacement.value = undefined;
  optionalSignatureVisible.value = false;
};

const applyEditedLayout = () => {
  if (!validateTemplate(editingForm.value)) return;
  const nextTemplate = cloneScoreSheetTemplate(editingForm.value);
  const currentSignatureField = firstSignatureField(form.value);
  if (currentSignatureField && scoreSheetSignatureRequired(currentSignatureField) && !firstSignatureField(nextTemplate)) {
    proxy?.$modal.msgWarning('当前模板要求手写签名，不能在本次版式中移除评分老师签字处；如需调整，请先保存模板配置');
    return;
  }
  preserveCurrentSignaturePolicy(nextTemplate);
  form.value = nextTemplate;
  signaturePlacement.value = undefined;
  optionalSignatureVisible.value = false;
  editorVisible.value = false;
  proxy?.$modal.msgSuccess('本次评分表版式已应用；如需签名，请点击签字处选择');
};

const saveCurrentTemplate = async () => {
  if (!validateTemplate(editingForm.value)) return;
  savingTemplate.value = true;
  try {
    const response = editingForm.value.id
      ? await updateReviewScoreSheetTemplate(editingForm.value.id, editingForm.value)
      : await createReviewScoreSheetTemplate(editingForm.value);
    syncSavedTemplate(response.data);
    editorVisible.value = false;
    proxy?.$modal.msgSuccess('当前模板已保存并应用');
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error || '');
    if (message.includes('模板已被其他用户修改')) {
      proxy?.$modal.msgWarning('模板已被其他用户修改，请关闭预览后重新打开');
    }
  } finally {
    savingTemplate.value = false;
  }
};

const saveAsNewTemplate = async () => {
  if (!validateTemplate(editingForm.value)) return;
  try {
    const result = await ElMessageBox.prompt('请输入新模板名称', '另存为新模板', {
      inputValue: `${editingForm.value.templateName}-副本`,
      inputPattern: /\S+/,
      inputErrorMessage: '模板名称不能为空',
      inputValidator: (value) => (String(value || '').trim().length <= 100 ? true : '模板名称不能超过100个字符')
    });
    savingTemplate.value = true;
    const copy = cloneScoreSheetTemplate(editingForm.value);
    delete copy.id;
    copy.templateName = String(result.value || '').trim();
    copy.defaultTemplate = false;
    copy.version = 0;
    delete copy.updateBy;
    delete copy.updatedByName;
    delete copy.updateTime;
    const { data } = await createReviewScoreSheetTemplate(copy);
    syncSavedTemplate(data);
    editorVisible.value = false;
    proxy?.$modal.msgSuccess('已另存为新模板并应用');
  } catch (error: any) {
    if (error === 'cancel' || error === 'close') return;
  } finally {
    savingTemplate.value = false;
  }
};

defineExpose({ open });
</script>

<style scoped lang="scss">
.export-toolbar,
.export-toolbar-item,
.export-dialog-footer,
.editor-dialog-toolbar,
.editor-dialog-footer,
.preview-section-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.export-toolbar,
.editor-dialog-toolbar {
  flex-wrap: wrap;
  padding: 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  background: var(--el-fill-color-extra-light);
}

.export-toolbar-item > span:first-child {
  color: var(--el-text-color-regular);
  font-weight: 600;
}

.export-toolbar-spacer {
  flex: 1 1 auto;
}

.active-signature-source {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
  max-width: 250px;
  padding: 4px 8px 4px 4px;
  cursor: grab;
  user-select: none;
  background: var(--el-bg-color);
  border: 1px dashed var(--el-color-primary-light-5);
  border-radius: 5px;

  &:active {
    cursor: grabbing;
  }

  img {
    width: 72px;
    height: 36px;
    object-fit: contain;
    background: #fff;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 3px;
  }

  span {
    display: flex;
    flex-direction: column;
    min-width: 0;
    gap: 2px;
  }

  strong,
  small {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  small {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }
}

.simple-preview-panel {
  margin-top: 10px;
  padding: 14px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  background: var(--el-fill-color-extra-light);
}

.preview-section-head {
  justify-content: space-between;
  margin-bottom: 10px;

  span {
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }
}

.export-dialog-footer {
  justify-content: flex-end;
}

.editor-dialog-toolbar {
  justify-content: space-between;

  > div {
    display: flex;
    align-items: center;
    gap: 10px;
  }
}

.editor-dialog-layout {
  display: grid;
  grid-template-columns: minmax(460px, 0.9fr) minmax(560px, 1.15fr);
  height: 68vh;
  min-height: 520px;
  margin-top: 10px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  overflow: hidden;
}

.editor-config-scroll {
  border-right: 1px solid var(--el-border-color-light);
}

.editor-config-panel,
.editor-preview-panel {
  padding: 14px;
}

.editor-preview-panel {
  min-width: 720px;
}

.editor-dialog-footer {
  justify-content: space-between;

  > span {
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }
}

@media (max-width: 1180px) {
  .editor-dialog-layout {
    grid-template-columns: 1fr;
    height: auto;
    max-height: 70vh;
    overflow-y: auto;
  }

  .editor-config-scroll,
  .editor-preview-scroll {
    height: auto;
    max-height: none;
  }

  .editor-config-scroll {
    border-right: 0;
    border-bottom: 1px solid var(--el-border-color-light);
  }
}

@media (max-width: 760px) {
  .export-toolbar,
  .export-toolbar-item,
  .editor-dialog-footer,
  .editor-dialog-footer > div {
    align-items: stretch;
    flex-direction: column;
  }

  .export-toolbar-item :deep(.el-select),
  .export-toolbar-item :deep(.el-date-editor) {
    width: 100% !important;
  }

  .preview-section-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .active-signature-source {
    max-width: none;
  }
}
</style>
