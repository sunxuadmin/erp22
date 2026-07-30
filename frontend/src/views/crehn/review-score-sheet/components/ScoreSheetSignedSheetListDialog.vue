<template>
  <ArtPopupDialog v-model="visible" title="我的历史签名表" width="92%" top="5vh" append-to-body destroy-on-close class="score-sheet-signed-list-dialog">
    <el-alert
      class="mb-[10px]"
      type="info"
      show-icon
      :closable="false"
      title="历史评分提交单使用保存时的评分、模板及可选签名快照；无签名提交同样保留审计留痕。替换个人签名不会改写历史记录，已撤回单仅可审计查看。"
    />
    <ScoreSheetSignedSheetManager v-if="visible" ref="managerRef" mode="self" embedded :auto-load="false" @changed="emit('changed')" />
  </ArtPopupDialog>
</template>

<script setup lang="ts">
import type { ReviewScoreSheetSignedSheetQuery } from '@/api/crehn/types';
import ScoreSheetSignedSheetManager from './ScoreSheetSignedSheetManager.vue';

const emit = defineEmits<{
  (e: 'changed'): void;
}>();

const visible = ref(false);
const managerRef = ref<InstanceType<typeof ScoreSheetSignedSheetManager>>();

const open = async (nextQuery: ReviewScoreSheetSignedSheetQuery = {}) => {
  visible.value = true;
  await nextTick();
  await managerRef.value?.open(nextQuery);
};

const load = async () => {
  await managerRef.value?.load();
};

defineExpose({ open, load });
</script>
