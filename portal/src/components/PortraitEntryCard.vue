<script setup lang="ts">
import { computed, ref } from "vue";
import type { EntryItem } from "../config/entries";

const props = defineProps<{ entry: EntryItem; index: number }>();
const emit = defineEmits<{ activate: [entry: EntryItem] }>();
const logoLoadFailed = ref(false);
const backgroundLoadFailed = ref(false);
const cssVars = computed<Record<string, string | number>>(() => ({
  "--tone-rgb": props.entry.tone.rgb,
  "--tone-deep": props.entry.tone.deep,
  "--tone-soft": props.entry.tone.soft,
  "--tone-edge": props.entry.tone.edge,
  "--entry-background-opacity": props.entry.backgroundOpacity,
  "--entry-background-position": props.entry.backgroundPosition,
  "--entry-background-size": props.entry.backgroundSize,
  "--entry-background-blur": `${props.entry.backgroundBlur}px`,
  "--entry-index": props.index
}));

const titleLines = computed(() => {
  const title = props.entry.title.trim();
  const marker = title.includes("艺术设计") ? "艺术设计" : title.includes("艺术展演") ? "艺术展演" : "";
  const splitAt = marker ? title.lastIndexOf(marker) : -1;
  return splitAt > 0 ? [title.slice(0, splitAt), title.slice(splitAt)] : [title];
});

function handleActivate(event: MouseEvent) {
  event.preventDefault();
  emit("activate", props.entry);
}
</script>

<template>
  <a
    class="portrait-entry-card"
    :class="`portrait-entry-card--${entry.tone.name}`"
    :href="entry.href || '#'"
    :target="entry.target"
    :rel="entry.target === '_blank' ? 'noopener noreferrer' : undefined"
    :style="cssVars"
    :aria-label="`进入${entry.title}`"
    @click="handleActivate"
  >
    <span
      v-if="entry.backgroundImage && !backgroundLoadFailed"
      class="portrait-entry-background-media"
      aria-hidden="true"
    >
      <img :src="entry.backgroundImage" alt="" @error="backgroundLoadFailed = true" />
    </span>
    <span class="portrait-entry-top">
      <span class="portrait-entry-icon" aria-hidden="true">
        <img v-if="entry.logo && !logoLoadFailed" :src="entry.logo" alt="" @error="logoLoadFailed = true" />
        <svg v-else viewBox="0 0 64 64" role="presentation">
          <path d="M32 6 54 18v28L32 58 10 46V18L32 6Z" :fill="`rgba(${entry.tone.rgb}, .2)`" stroke="#fff" stroke-opacity=".66" />
          <path d="m32 13 14 9-14 9-14-9 14-9Z" fill="#fff" opacity=".94" />
          <path d="m18 22 14 9v18l-14-7V22Z" fill="#fff" opacity=".56" />
          <path d="m46 22-14 9v18l14-7V22Z" :fill="`rgb(${entry.tone.rgb})`" opacity=".88" />
        </svg>
      </span>
      <span class="portrait-entry-number">{{ entry.number }}</span>
    </span>

    <span class="portrait-entry-copy">
      <strong><span v-for="line in titleLines" :key="line">{{ line }}</span></strong>
      <span>{{ entry.description }}</span>
    </span>

    <span class="portrait-entry-action" aria-hidden="true">
      <span class="portrait-entry-action-icon"></span>
    </span>
  </a>
</template>
