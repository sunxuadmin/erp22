<template>
  <WorkbenchNoticeStatsWidget :title="title" :config-json="configJson" mode="reviewer" />
</template>

<script setup lang="ts">
import WorkbenchNoticeStatsWidget from './WorkbenchNoticeStatsWidget.vue';

defineProps<{ title: string; configJson?: string }>();
</script>
