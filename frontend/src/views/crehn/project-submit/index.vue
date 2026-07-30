<template>
  <div class="school-project-submit-page">
    <SchoolProjectSubmitWidget
      :title="componentTitle"
      :config-json="componentConfigJson"
      standalone
      show-back-button
      :initial-status="initialStatus"
      :role-key="componentRoleKey"
    />
  </div>
</template>

<script setup lang="ts">
import { getCurrentWorkbenchComponent } from '@/api/system/workbench';
import SchoolProjectSubmitWidget from '@/views/workbench/components/SchoolProjectSubmitWidget.vue';
import { WORKBENCH_COMPONENT_KEYS } from '@/views/workbench/workbenchComponentRegistry';

const route = useRoute();
const componentTitle = ref('统一提交');
const componentConfigJson = ref<string>();
const componentRoleKey = ref('');

const queryText = (value: unknown) => (Array.isArray(value) ? String(value[0] || '') : String(value || ''));
const initialStatus = computed(() => queryText(route.query.status));

onMounted(async () => {
  try {
    const res = await getCurrentWorkbenchComponent(WORKBENCH_COMPONENT_KEYS.schoolProjectList);
    const component = res.data;
    componentTitle.value = component?.title || componentTitle.value;
    componentConfigJson.value = component?.configJson;
    componentRoleKey.value = component?.roleKey || '';
  } catch {
    componentConfigJson.value = undefined;
  }
});
</script>

<style scoped lang="scss">
.school-project-submit-page {
  position: absolute;
  top: var(--layout-header-height, 92px);
  right: 0;
  bottom: calc(-1 * var(--layout-header-height, 92px));
  left: 0;
  height: auto;
  min-height: 0;
  padding: 18px;
  overflow: hidden;
  box-sizing: border-box;
  background: #f6f8fc;
}

@media (max-width: 700px) {
  .school-project-submit-page {
    padding: 10px;
  }
}
</style>
