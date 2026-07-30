<template>
  <el-tag class="art-list-status-tag" :class="`art-list-status-tag--${semanticClass}`" :type="tagType">
    <el-icon v-if="statusAppearance.iconVisible" class="art-list-status-tag__icon">
      <component :is="iconComponent" />
    </el-icon>
    <span>{{ label }}</span>
  </el-tag>
</template>

<script setup lang="ts">
import { Back, CircleCheck, Clock, Document, EditPen, Lock } from '@element-plus/icons-vue';
import type { ArtListStatusSemantic } from '@/api/crehn/detailDisplay';
import { useArtDetailDisplayConfig } from './artDetailDisplayConfig';

type ElTagType = 'primary' | 'success' | 'warning' | 'danger' | 'info';

const props = defineProps<{
  semantic: ArtListStatusSemantic;
  label: string;
  tagType?: ElTagType;
}>();

const { detailDisplayConfig } = useArtDetailDisplayConfig();
const statusAppearance = computed(() => detailDisplayConfig.value.listTableAppearance.statusAppearance);
const semanticClass = computed(() => (props.semantic === 'scoreDraft' ? 'score-draft' : props.semantic));
const statusIcons = {
  draft: Document,
  pending: Clock,
  approved: CircleCheck,
  returned: Back,
  unscored: EditPen,
  scored: CircleCheck,
  scoreDraft: EditPen,
  locked: Lock
};
const iconComponent = computed(() => statusIcons[props.semantic]);
</script>
