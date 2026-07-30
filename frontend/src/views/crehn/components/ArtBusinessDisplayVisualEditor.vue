<template>
  <section v-loading="loading" class="art-business-visual-editor">
    <el-alert
      v-if="mode === 'all' && !compact"
      type="info"
      :closable="false"
      show-icon
      title="两个可视化编辑器共用同一份草稿；切换 Tab 不会丢失修改，点击页面底部“保存全部设置”后统一生效。"
    />

    <el-tabs v-if="mode === 'all'" v-model="activeTab" class="art-business-visual-editor__tabs" :class="{ 'is-navigation-hidden': !showNavigation }">
      <el-tab-pane name="projectList">
        <template #label>
          <span class="art-business-visual-editor__tab-label"
            ><el-icon><Filter /></el-icon>项目列表可视化</span
          >
        </template>
        <ArtProjectListVisualEditor v-if="draft" v-model="draft" />
      </el-tab-pane>
      <el-tab-pane name="reviewDisplay">
        <template #label>
          <span class="art-business-visual-editor__tab-label"><svg-icon icon-class="eye-open" />艺术评审可视化</span>
        </template>
        <ArtReviewDisplayVisualEditor v-if="draft" v-model="draft" />
      </el-tab-pane>
    </el-tabs>
    <ArtProjectListVisualEditor v-else-if="mode === 'projectList' && draft" v-model="draft" :page-key="pageKey" :compact="compact" />
    <ArtReviewDisplayVisualEditor v-else-if="mode === 'reviewDisplay' && draft" v-model="draft" />

    <footer v-if="showFooter" class="art-business-visual-editor__footer">
      <div class="art-business-visual-editor__dirty">
        <el-icon :class="{ 'is-dirty': isDirty }"><WarningFilled v-if="isDirty" /><CircleCheckFilled v-else /></el-icon>
        <span>{{ isDirty ? '当前存在尚未保存的显示配置' : '当前配置已保存' }}</span>
      </div>
      <div class="art-business-visual-editor__footer-actions">
        <el-button :disabled="!draft || saving" @click="restoreDefaults()">恢复系统默认</el-button>
        <el-button :disabled="!isDirty || saving" @click="cancelChanges()">取消本次修改</el-button>
        <el-button type="primary" :disabled="!draft || !isDirty" :loading="saving" @click="saveAll">保存全部设置</el-button>
      </div>
    </footer>
  </section>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus';
import type { ArtDetailDisplayConfigVO, ArtWorkspaceHeaderPageKey } from '@/api/crehn/detailDisplay';
import ArtProjectListVisualEditor from './ArtProjectListVisualEditor.vue';
import ArtReviewDisplayVisualEditor from './ArtReviewDisplayVisualEditor.vue';
import {
  cloneArtDetailDisplayConfig,
  loadArtDetailDisplayConfig,
  resetArtDetailDisplayConfig,
  saveArtDetailDisplayConfig,
  useArtDetailDisplayConfig
} from './artDetailDisplayConfig';

const props = withDefaults(
  defineProps<{
    mode?: 'all' | 'projectList' | 'reviewDisplay';
    pageKey?: ArtWorkspaceHeaderPageKey;
    compact?: boolean;
    showNavigation?: boolean;
    showFooter?: boolean;
  }>(),
  {
    mode: 'all',
    compact: false,
    showNavigation: true,
    showFooter: true
  }
);
const mode = computed(() => props.mode);
const pageKey = computed(() => props.pageKey);
const compact = computed(() => props.compact);
const showNavigation = computed(() => props.showNavigation);
const showFooter = computed(() => props.showFooter);
const activeTab = defineModel<'projectList' | 'reviewDisplay'>('activeTab', { default: 'projectList' });
const draft = ref<ArtDetailDisplayConfigVO>();
const originalSnapshot = ref('');
const loading = ref(false);
const saving = ref(false);
const { detailDisplayConfig } = useArtDetailDisplayConfig();

const currentSnapshot = computed(() => (draft.value ? JSON.stringify(draft.value) : ''));
const isDirty = computed(() => Boolean(draft.value) && currentSnapshot.value !== originalSnapshot.value);
const saveActionLabel = computed(() =>
  mode.value === 'projectList' ? '保存页面设置' : mode.value === 'reviewDisplay' ? '保存艺术评审设置' : '保存全部设置'
);
const syncPersistedManagedActivityId = (value?: string | number) => {
  if (!draft.value) return;
  const normalized = String(value ?? '').trim();
  draft.value.workspaceHeader.managedActivityId = normalized || undefined;
  if (!originalSnapshot.value) return;
  const original = JSON.parse(originalSnapshot.value) as ArtDetailDisplayConfigVO;
  original.workspaceHeader.managedActivityId = normalized || undefined;
  originalSnapshot.value = JSON.stringify(original);
};

const initialize = async () => {
  loading.value = true;
  try {
    await loadArtDetailDisplayConfig(true);
    draft.value = cloneArtDetailDisplayConfig(detailDisplayConfig.value);
    originalSnapshot.value = JSON.stringify(draft.value);
  } finally {
    loading.value = false;
  }
};

const restoreDefaults = async (confirm = true) => {
  const scopeLabel = mode.value === 'projectList' ? '当前页面的表头与按钮' : mode.value === 'reviewDisplay' ? '艺术评审显示配置' : '两个可视化 Tab';
  if (confirm) {
    try {
      await ElMessageBox.confirm(`将${scopeLabel}恢复为系统默认值，确认继续？`, '恢复系统默认', {
        confirmButtonText: '恢复默认',
        cancelButtonText: '取消',
        type: 'warning'
      });
    } catch {
      return;
    }
  }
  const defaults = resetArtDetailDisplayConfig();
  if (mode.value === 'projectList' && pageKey.value && draft.value) {
    draft.value.workspaceHeader.pages[pageKey.value] = cloneArtDetailDisplayConfig(defaults).workspaceHeader.pages[pageKey.value];
    const navigatorKey = pageKey.value === 'schoolSubmit' || pageKey.value === 'project' ? undefined : pageKey.value;
    if (navigatorKey) {
      draft.value.navigator.pages[navigatorKey] = cloneArtDetailDisplayConfig(defaults).navigator.pages[navigatorKey];
    }
  } else if (mode.value === 'reviewDisplay' && draft.value) {
    draft.value.tabs = defaults.tabs;
    draft.value.audit = defaults.audit;
    draft.value.files = defaults.files;
    draft.value.score = defaults.score;
    draft.value.reviewWorkbench = defaults.reviewWorkbench;
  } else {
    draft.value = defaults;
  }
  ElMessage.success('已恢复到系统默认草稿，保存后生效');
};

const cancelChanges = async (confirm = true) => {
  if (!isDirty.value) return;
  if (confirm) {
    try {
      await ElMessageBox.confirm('确认放弃当前范围尚未保存的修改？', '取消本次修改', {
        confirmButtonText: '放弃修改',
        cancelButtonText: '继续编辑',
        type: 'warning'
      });
    } catch {
      return;
    }
  }
  draft.value = cloneArtDetailDisplayConfig(JSON.parse(originalSnapshot.value) as ArtDetailDisplayConfigVO);
};

const saveAll = async () => {
  if (!draft.value || !isDirty.value) return;
  saving.value = true;
  try {
    const saved = await saveArtDetailDisplayConfig(draft.value);
    draft.value = cloneArtDetailDisplayConfig(saved);
    originalSnapshot.value = JSON.stringify(draft.value);
    ElMessage.success(mode.value === 'projectList' ? '页面表头与按钮已保存' : '业务页面显示配置已保存');
  } finally {
    saving.value = false;
  }
};

const handleBeforeUnload = (event: BeforeUnloadEvent) => {
  if (!isDirty.value) return;
  event.preventDefault();
  event.returnValue = '';
};

onMounted(() => {
  void initialize();
  window.addEventListener('beforeunload', handleBeforeUnload);
});

onBeforeRouteLeave(async () => {
  if (!isDirty.value) return true;
  try {
    await ElMessageBox.confirm('当前业务显示配置尚未保存，确认离开并放弃修改？', '离开当前页面', {
      confirmButtonText: '放弃并离开',
      cancelButtonText: '继续编辑',
      type: 'warning'
    });
    return true;
  } catch {
    return false;
  }
});

onBeforeUnmount(() => window.removeEventListener('beforeunload', handleBeforeUnload));

defineExpose({
  cancelChanges,
  initialize,
  isDirty,
  loading,
  restoreDefaults,
  saveActionLabel,
  saveAll,
  saving,
  syncPersistedManagedActivityId
});
</script>

<style scoped>
.art-business-visual-editor {
  display: grid;
  gap: 14px;
  min-width: 0;
}

.art-business-visual-editor__tabs :deep(.el-tabs__header) {
  margin: 0 0 14px;
}

.art-business-visual-editor__tabs.is-navigation-hidden :deep(.el-tabs__header) {
  display: none;
}

.art-business-visual-editor__tabs :deep(.el-tabs__item) {
  height: 46px;
  padding: 0 26px;
  font-size: 15px;
  font-weight: 700;
}

.art-business-visual-editor__tab-label,
.art-business-visual-editor__dirty,
.art-business-visual-editor__footer,
.art-business-visual-editor__footer-actions {
  display: flex;
  align-items: center;
}

.art-business-visual-editor__tab-label {
  gap: 8px;
}

.art-business-visual-editor__footer {
  position: sticky;
  z-index: 8;
  bottom: 0;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
  background: color-mix(in srgb, var(--el-bg-color) 96%, transparent);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--app-radius-md, var(--el-border-radius-base));
  box-shadow: 0 -8px 24px rgb(15 23 42 / 6%);
  backdrop-filter: blur(8px);
}

.art-business-visual-editor__dirty {
  gap: 7px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.art-business-visual-editor__dirty .is-dirty {
  color: var(--el-color-warning);
}

.art-business-visual-editor__footer-actions {
  gap: 10px;
}

@media (max-width: 900px) {
  .art-business-visual-editor__footer {
    position: static;
    align-items: stretch;
    flex-direction: column;
  }

  .art-business-visual-editor__footer-actions {
    flex-wrap: wrap;
  }
}
</style>
