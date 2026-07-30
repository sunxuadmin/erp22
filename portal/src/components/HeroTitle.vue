<script setup lang="ts">
import { computed } from "vue";
import type { HeroContent } from "../config/entries";

const props = defineProps<{ content: HeroContent }>();
const titleSegments = computed(() =>
  props.content.title === "艺术 · 多元 · 创造"
    ? [
        { key: "art", className: "hero-title-segment hero-title-segment--deep", text: "艺术" },
        { key: "dot-1", className: "hero-title-separator", text: "·" },
        { key: "diversity", className: "hero-title-segment hero-title-segment--accent", text: "多元" },
        { key: "dot-2", className: "hero-title-separator", text: "·" },
        { key: "create", className: "hero-title-segment hero-title-segment--deep", text: "创造" }
      ]
    : [{ key: "title", className: "hero-title-segment hero-title-segment--deep", text: props.content.title }]
);
</script>

<template>
  <aside class="hero-copy" aria-label="页面主题">
    <p class="hero-eyebrow">{{ content.eyebrow }}</p>
    <h1 class="hero-title" :aria-label="content.title">
      <span class="hero-title-glass" aria-hidden="true">
        <span v-for="segment in titleSegments" :key="segment.key" :class="segment.className">
          {{ segment.text }}
        </span>
      </span>
    </h1>
    <p class="hero-subtitle">{{ content.subtitle }}</p>
    <p class="hero-description">{{ content.description }}</p>
  </aside>
</template>
