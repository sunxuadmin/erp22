<script setup lang="ts">
import { computed } from "vue";
import type {
  DecorElement,
  MotionSettings,
  VisualBackgroundSettings
} from "../config/entries";

const props = defineProps<{
  motion: MotionSettings;
  version: string;
  visual: VisualBackgroundSettings;
}>();
const dustCount = computed(() => Math.min(16, Math.max(0, props.motion.dustCount)));
const glassBubbleCount = computed(() => Math.min(7, Math.max(0, props.motion.glassBubbleCount)));

const backgroundImageStyle = computed(() => ({
  backgroundImage: props.visual.image ? `url("${props.visual.image}")` : "none",
  backgroundPosition: props.visual.imagePosition,
  backgroundSize: props.visual.imageSize,
  opacity: props.visual.mode === "image" ? props.visual.imageOpacity : 0,
  filter: `blur(${props.visual.imageBlur}px) brightness(${props.visual.brightness}) contrast(${props.visual.contrast}) saturate(${props.visual.saturation})`
}));

const overlayStyle = computed(() => ({
  background: props.visual.overlayColor,
  opacity: props.visual.overlayOpacity
}));

function elementStyles(element: DecorElement) {
  return {
    left: `${element.x}%`,
    top: `${element.y}%`,
    width: `${element.width}px`,
    height: `${element.height}px`,
    zIndex: element.zIndex,
    "--decor-color": element.color,
    "--decor-secondary-color": element.secondaryColor,
    "--decor-opacity": element.opacity,
    "--decor-blur": `${element.blur}px`,
    "--decor-backdrop-blur": `${element.backdropBlur}px`,
    "--decor-border-opacity": element.borderOpacity,
    "--decor-rotation": `${element.rotation}deg`,
    "--decor-duration": `${element.motion.duration}s`,
    "--decor-delay": `${element.motion.delay}s`,
    "--decor-x": `${element.motion.distanceX}px`,
    "--decor-x-negative": `${-element.motion.distanceX}px`,
    "--decor-y": `${element.motion.distanceY}px`,
    "--decor-y-negative": `${-element.motion.distanceY}px`,
    "--decor-motion-rotation": `${element.motion.rotation}deg`,
    "--decor-easing": element.motion.easing
  };
}
</script>

<template>
  <div
    class="background-layer"
    :class="`background-mode-${visual.mode}`"
    aria-hidden="true"
  >
    <div class="custom-background-image" :style="backgroundImageStyle" />
    <div class="background-tone-overlay" />
    <div class="background-color-wash" />
    <div class="background-atmosphere" />
    <div v-if="visual.builtInDecorEnabled" class="background-gallery-ring" />
    <div class="custom-background-overlay" :style="overlayStyle" />

    <div class="custom-decor-layer">
      <span
        v-for="element in visual.elements"
        :key="element.id"
        class="custom-decor-element"
        :class="[
          `custom-decor-element--${element.type}`,
          `custom-decor-motion--${element.motion.enabled ? element.motion.type : 'none'}`,
          { 'custom-decor-element--mobile-hidden': element.hideOnMobile }
        ]"
        :style="elementStyles(element)"
        :data-decor-id="element.id"
      >
        <span class="custom-decor-element__visual">
          <img v-if="element.type === 'image' && element.image" :src="element.image" alt="" />
          <span v-else-if="element.type === 'sparkle'" aria-hidden="true">✦</span>
        </span>
      </span>
    </div>

    <template v-if="visual.builtInDecorEnabled">
      <div class="decor-shape decor-shape--mint-triangle" />
      <div class="decor-shape decor-shape--sun" />
      <div class="decor-shape decor-shape--blue-orb" />
      <div class="decor-shape decor-shape--violet-cube" />
      <div class="decor-shape decor-shape--cyan-swoosh" />
      <div class="decor-spark decor-spark--left">✦</div>
      <div class="decor-spark decor-spark--right">✦</div>
    </template>

    <div v-if="version === 'v2' && visual.builtInDecorEnabled" class="version-two-background">
      <div class="v2-decor v2-decor--aqua-ring" />
      <div class="v2-decor v2-decor--hero-glass-orb" />
      <div class="v2-decor v2-decor--lavender-bar" />
      <div class="v2-decor v2-decor--bottom-wave" />
      <div class="v2-decor v2-decor--blue-glass-orb" />
    </div>

    <div v-if="motion.glassBubblesEnabled" class="background-glass-bubbles">
      <span
        v-for="index in glassBubbleCount"
        :key="index"
        class="background-glass-bubble"
        :class="`background-glass-bubble--${index}`"
      />
    </div>

    <div v-if="motion.dustEnabled" class="stage-dust">
      <span
        v-for="index in dustCount"
        :key="index"
        class="stage-dust-particle"
        :class="`stage-dust-particle--${index}`"
      />
    </div>
    <div v-if="motion.lightCurtainEnabled" class="stage-light-sweep" />
  </div>
</template>
