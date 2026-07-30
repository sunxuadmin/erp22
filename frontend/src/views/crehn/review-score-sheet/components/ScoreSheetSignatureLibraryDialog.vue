<template>
  <ArtPopupDialog v-model="visible" title="我的手写签名" width="760px" append-to-body destroy-on-close class="score-sheet-signature-library">
    <el-alert
      class="mb-[12px]"
      type="info"
      show-icon
      :closable="false"
      title="签名只归当前评审老师使用。替换签名只影响以后新保存的签名表，已保存的签名表会保留当时的签名快照。"
    />

    <div class="signature-library-toolbar">
      <span>点击任一签名即可直接填入签字处；默认签名会在下次优先选中。</span>
      <el-button type="primary" icon="Edit" @click="openCreate">新建手写签名</el-button>
    </div>

    <div v-loading="loading" class="signature-library-list">
      <div
        v-for="signature in signatures"
        :key="String(signature.id)"
        role="button"
        tabindex="0"
        class="signature-library-card"
        :class="{ 'is-selected': sameId(signature.id, selectedSignatureId) }"
        @click="chooseAndFillSignature(signature)"
        @keydown.enter="chooseAndFillSignature(signature)"
        @keydown.space.prevent="chooseAndFillSignature(signature)"
      >
        <div class="signature-image-wrap">
          <img :src="signature.imageUrl || signature.url" :alt="signature.signatureName" />
        </div>
        <div class="signature-card-meta">
          <strong>{{ signature.signatureName }}</strong>
          <span>{{ signature.updateTime || signature.createTime || '已保存' }}</span>
        </div>
        <div class="signature-card-actions" @click.stop @keydown.stop>
          <el-tag v-if="signature.defaultSignature" size="small" type="success">默认</el-tag>
          <el-button v-else size="small" link type="primary" @click="makeDefault(signature)">设为默认</el-button>
          <el-button size="small" link type="primary" @click="openReplace(signature)">替换</el-button>
          <el-button size="small" link type="danger" @click="removeSignature(signature)">删除</el-button>
        </div>
      </div>
      <el-empty v-if="!loading && !signatures.length" :image-size="72" description="还没有手写签名，请先新建一个" />
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :disabled="!selectedSignature" @click="fillSignature">填入签字处</el-button>
    </template>
  </ArtPopupDialog>

  <ArtPopupDialog
    v-model="padVisible"
    :title="replaceTarget ? '替换手写签名' : '新建手写签名'"
    width="680px"
    append-to-body
    destroy-on-close
    :dirty="signaturePadDirty"
    @closed="resetPad"
  >
    <el-form label-position="top">
      <el-form-item label="签名名称">
        <el-input v-model="signatureName" maxlength="30" show-word-limit placeholder="例如：我的常用签名" />
      </el-form-item>
      <el-form-item label="手写签名">
        <ScoreSheetSignaturePad ref="signaturePadRef" @change="signaturePadDirty = true" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="padVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="saveSignature">保存签名</el-button>
    </template>
  </ArtPopupDialog>
</template>

<script setup lang="ts">
import {
  createReviewScoreSheetSignature,
  deleteReviewScoreSheetSignature,
  listReviewScoreSheetSignatures,
  replaceReviewScoreSheetSignature,
  setDefaultReviewScoreSheetSignature
} from '@/api/crehn/review';
import type { ReviewScoreSheetSignatureVO } from '@/api/crehn/types';
import ScoreSheetSignaturePad from './ScoreSheetSignaturePad.vue';

const props = defineProps<{
  modelValue: boolean;
  selectedSignatureId?: string | number;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'update:selectedSignatureId', value?: string | number): void;
  (e: 'select', signature?: ReviewScoreSheetSignatureVO): void;
  (e: 'fill', signature?: ReviewScoreSheetSignatureVO): void;
  (e: 'changed'): void;
}>();

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
});
const loading = ref(false);
const saving = ref(false);
const signatures = ref<ReviewScoreSheetSignatureVO[]>([]);
const padVisible = ref(false);
const signaturePadDirty = ref(false);
const signatureName = ref('');
const replaceTarget = ref<ReviewScoreSheetSignatureVO>();
const signaturePadRef = ref<InstanceType<typeof ScoreSheetSignaturePad>>();
const fillAfterFirstCreate = ref(false);

const sameId = (left?: string | number, right?: string | number) => left != null && right != null && String(left) === String(right);
const selectedSignature = computed(() => signatures.value.find((item) => sameId(item.id, props.selectedSignatureId)));

const choosePreferred = (preferredId?: string | number) => {
  const defaultSignature = signatures.value.find((item) => item.defaultSignature);
  const preferred = signatures.value.find((item) => sameId(item.id, preferredId ?? props.selectedSignatureId));
  const selected = preferred || defaultSignature || signatures.value[0];
  emit('update:selectedSignatureId', selected?.id);
  if (selected) emit('select', selected);
  return selected;
};

const load = async (preferredId?: string | number) => {
  loading.value = true;
  try {
    const { data } = await listReviewScoreSheetSignatures();
    signatures.value = data || [];
    return choosePreferred(preferredId);
  } finally {
    loading.value = false;
  }
};

const chooseAndFillSignature = (signature: ReviewScoreSheetSignatureVO) => {
  emit('update:selectedSignatureId', signature.id);
  emit('select', signature);
  emit('fill', signature);
  visible.value = false;
};

const fillSignature = () => {
  const signature = selectedSignature.value;
  if (!signature) return;
  emit('select', signature);
  emit('fill', signature);
  visible.value = false;
};

const open = async (preferredId?: string | number, fillOnFirstCreate = false) => {
  fillAfterFirstCreate.value = fillOnFirstCreate;
  visible.value = true;
  await nextTick();
  const selected = await load(preferredId);
  if (selected || signatures.value.length) return;
  visible.value = false;
  await nextTick();
  openCreate();
};

const openCreate = () => {
  replaceTarget.value = undefined;
  signatureName.value = `手写签名${signatures.value.length + 1}`;
  signaturePadDirty.value = false;
  padVisible.value = true;
};

const openReplace = (signature: ReviewScoreSheetSignatureVO) => {
  replaceTarget.value = signature;
  signatureName.value = signature.signatureName;
  signaturePadDirty.value = false;
  padVisible.value = true;
};

const resetPad = () => {
  signatureName.value = '';
  signaturePadDirty.value = false;
  replaceTarget.value = undefined;
  signaturePadRef.value?.clear();
};

const saveSignature = async () => {
  const name = signatureName.value.trim();
  if (!name) {
    proxy?.$modal.msgWarning('请输入签名名称');
    return;
  }
  const blob = await signaturePadRef.value?.toBlob();
  if (!blob) {
    proxy?.$modal.msgWarning('请先在画板中完成手写签名');
    return;
  }
  const file = new File([blob], `${name}.png`, { type: 'image/png' });
  saving.value = true;
  try {
    const replacing = !!replaceTarget.value?.id;
    const response = replaceTarget.value?.id
      ? await replaceReviewScoreSheetSignature(replaceTarget.value.id, file, name)
      : await createReviewScoreSheetSignature(file, name, signatures.value.length === 0);
    const saved = response.data as ReviewScoreSheetSignatureVO | undefined;
    padVisible.value = false;
    const selected = await load(saved?.id);
    emit('changed');
    proxy?.$modal.msgSuccess(
      replacing ? '签名已替换，后续新签名表会使用新版本' : saved?.reused ? '相同手写签名已在签名库中，已复用现有签名' : '手写签名已保存'
    );
    if (!replacing && fillAfterFirstCreate.value && selected) {
      emit('fill', selected);
      fillAfterFirstCreate.value = false;
    }
  } finally {
    saving.value = false;
  }
};

const makeDefault = async (signature: ReviewScoreSheetSignatureVO) => {
  if (!signature.id) return;
  await setDefaultReviewScoreSheetSignature(signature.id);
  await load(signature.id);
  emit('changed');
  proxy?.$modal.msgSuccess('已设为默认签名');
};

const removeSignature = async (signature: ReviewScoreSheetSignatureVO) => {
  if (!signature.id) return;
  try {
    await proxy?.$modal.confirm(`确认删除手写签名“${signature.signatureName}”吗？`);
  } catch {
    return;
  }
  await deleteReviewScoreSheetSignature(signature.id);
  await load();
  emit('changed');
  proxy?.$modal.msgSuccess('签名已删除；已保存的签名表不受影响');
};

defineExpose({ load, open });
</script>

<style scoped lang="scss">
.signature-library-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.signature-library-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 12px;
  min-height: 150px;
}

.signature-library-card {
  display: flex;
  min-width: 0;
  padding: 10px;
  overflow: hidden;
  color: inherit;
  cursor: pointer;
  text-align: left;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  transition:
    border-color 0.16s ease,
    box-shadow 0.16s ease;

  &:hover,
  &.is-selected {
    border-color: var(--el-color-primary);
    box-shadow: 0 0 0 2px rgb(var(--el-color-primary-rgb) / 10%);
  }
}

.signature-image-wrap {
  display: flex;
  flex: 0 0 92px;
  align-items: center;
  justify-content: center;
  height: 58px;
  overflow: hidden;
  background: repeating-linear-gradient(135deg, #f8fafc, #f8fafc 6px, #f1f5f9 6px, #f1f5f9 12px);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;

  img {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
}

.signature-card-meta {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  gap: 4px;
  padding: 1px 0 0 8px;

  strong,
  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }
}

.signature-card-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  margin-left: 4px;
}

@media (max-width: 680px) {
  .signature-library-toolbar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
