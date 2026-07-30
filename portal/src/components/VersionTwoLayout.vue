<script setup lang="ts">
import { computed } from "vue";
import type { EntryItem, HeroContent } from "../config/entries";
import PortraitEntryCard from "./PortraitEntryCard.vue";

const props = defineProps<{ content: HeroContent; entries: EntryItem[] }>();
const emit = defineEmits<{ activate: [entry: EntryItem] }>();

const columnCount = computed(() => {
  const count = props.entries.length;
  if (count <= 1) return 1;
  if (count === 2) return 2;
  if (count === 3) return 3;
  if (count === 4) return 4;
  if (count <= 6) return 3;
  return 4;
});

const layoutStyles = computed(() => ({
  "--portrait-columns": columnCount.value,
  "--portrait-layout-width": `${Math.min(1460, columnCount.value * 330 + (columnCount.value - 1) * 18)}px`
}));
</script>

<template>
  <section
    class="version-two-layout"
    :class="{ 'version-two-layout--many': entries.length > 4 }"
    :style="layoutStyles"
    aria-label="艺术展演活动入口"
  >
    <div class="version-two-heading">
      <span class="version-two-spark version-two-spark--gold" aria-hidden="true">✦</span>
      <h1 :data-title="content.title" :aria-label="content.title">{{ content.title }}</h1>
      <p><i aria-hidden="true" />{{ content.subtitle }}<i aria-hidden="true" /></p>
    </div>

    <div class="portrait-entry-grid" :data-entry-count="entries.length">
      <PortraitEntryCard
        v-for="(entry, index) in entries"
        :key="entry.id"
        :entry="entry"
        :index="index"
        @activate="emit('activate', $event)"
      />
    </div>
  </section>
</template>
