<script setup lang="ts">
interface GlassSettings {
  cardOpacity: number;
  topbarOpacity: number;
  topbarBorderOpacity: number;
  topbarShadowOpacity: number;
  logoTileOpacity: number;
  textOpacity: number;
  contrast: number;
  saturation: number;
  blur: number;
  heroTitle: string;
  heroGradientMiddle: string;
  heroAccent: string;
  heroSubtitle: string;
}

type NumericGlassSettingKey =
  | "cardOpacity"
  | "topbarOpacity"
  | "topbarBorderOpacity"
  | "topbarShadowOpacity"
  | "logoTileOpacity"
  | "textOpacity"
  | "contrast"
  | "saturation"
  | "blur";
type ColorGlassSettingKey = "heroTitle" | "heroGradientMiddle" | "heroAccent" | "heroSubtitle";

const props = defineProps<{
  modelValue: GlassSettings;
  defaults: GlassSettings;
  performanceLabel: string;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: GlassSettings];
  close: [];
  reset: [];
}>();

const controls: Array<{
  key: NumericGlassSettingKey;
  label: string;
  min: number;
  max: number;
  step: number;
  hint: string;
}> = [
  { key: "cardOpacity", label: "卡片玻璃浓度", min: 0.18, max: 0.72, step: 0.01, hint: "越高越清晰，越低越通透" },
  { key: "topbarOpacity", label: "顶栏玻璃浓度", min: 0, max: 0.6, step: 0.01, hint: "可调到 0，完全移除顶栏白色底板" },
  { key: "topbarBorderOpacity", label: "顶栏玻璃亮边", min: 0, max: 1, step: 0.01, hint: "控制顶栏和通知窗的边界亮度" },
  { key: "topbarShadowOpacity", label: "顶栏柔和阴影", min: 0, max: 0.4, step: 0.01, hint: "调到 0 可完全关闭顶栏投影" },
  { key: "logoTileOpacity", label: "Logo 小底板", min: 0, max: 0.96, step: 0.01, hint: "调到 0 后 Logo 后方完全透明" },
  { key: "textOpacity", label: "说明文字清晰度", min: 0.5, max: 0.96, step: 0.01, hint: "只增强卡片说明文字，不改变背景" },
  { key: "contrast", label: "背景对比度", min: 0.85, max: 1.3, step: 0.01, hint: "增强玻璃后的明暗分离" },
  { key: "saturation", label: "颜色深度", min: 0.75, max: 1.6, step: 0.01, hint: "控制玻璃后的颜色浓度" },
  { key: "blur", label: "磨砂模糊", min: 6, max: 32, step: 1, hint: "数值越高越柔和，但性能开销更大" }
];

const colorControls: Array<{ key: ColorGlassSettingKey; label: string }> = [
  { key: "heroTitle", label: "标题起始色" },
  { key: "heroGradientMiddle", label: "标题中间色" },
  { key: "heroAccent", label: "标题结束色" },
  { key: "heroSubtitle", label: "副标题颜色" }
];

function updateValue(key: NumericGlassSettingKey, event: Event) {
  const value = Number((event.currentTarget as HTMLInputElement).value);
  emit("update:modelValue", { ...props.modelValue, [key]: value });
}

function updateColor(key: ColorGlassSettingKey, event: Event) {
  const value = (event.currentTarget as HTMLInputElement).value;
  emit("update:modelValue", { ...props.modelValue, [key]: value });
}

function applyPreset(name: "clear" | "balanced" | "readable") {
  if (name === "balanced") {
    emit("update:modelValue", { ...props.defaults });
    return;
  }

  emit("update:modelValue",
    name === "clear"
      ? {
          ...props.modelValue,
          cardOpacity: 0.26,
          topbarOpacity: 0.08,
          topbarBorderOpacity: 0.42,
          topbarShadowOpacity: 0.05,
          logoTileOpacity: 0.18,
          textOpacity: 0.68,
          contrast: 1.03,
          saturation: 1.18,
          blur: 14
        }
      : {
          ...props.modelValue,
          cardOpacity: 0.58,
          topbarOpacity: 0.3,
          topbarBorderOpacity: 0.7,
          topbarShadowOpacity: 0.12,
          logoTileOpacity: 0.5,
          textOpacity: 0.86,
          contrast: 1.1,
          saturation: 1.04,
          blur: 20
        }
  );
}

function formattedValue(key: NumericGlassSettingKey, value: number) {
  if (key === "blur") return `${Math.round(value)}px`;
  if (
    key === "cardOpacity" ||
    key === "topbarOpacity" ||
    key === "topbarBorderOpacity" ||
    key === "topbarShadowOpacity" ||
    key === "logoTileOpacity" ||
    key === "textOpacity"
  ) {
    return `${Math.round(value * 100)}%`;
  }
  return `${value.toFixed(2)}×`;
}
</script>

<template>
  <section
    id="glass-tuner"
    class="glass-tuner"
    role="dialog"
    aria-labelledby="glass-tuner-title"
    data-testid="glass-tuner"
  >
    <header class="glass-tuner__header">
      <span>
        <strong id="glass-tuner-title">液态玻璃调节</strong>
        <small>仅保存在当前浏览器</small>
      </span>
      <button
        type="button"
        class="glass-tuner__close"
        aria-label="关闭液态玻璃调节窗"
        data-testid="glass-tuner-close"
        @click="emit('close')"
      >
        ×
      </button>
    </header>

    <p class="glass-tuner__performance" aria-live="polite">{{ performanceLabel }}</p>

    <div class="glass-tuner__presets" aria-label="玻璃效果预设">
      <button type="button" data-testid="glass-preset-clear" @click="applyPreset('clear')">清透</button>
      <button type="button" data-testid="glass-preset-balanced" @click="applyPreset('balanced')">平衡</button>
      <button type="button" data-testid="glass-preset-readable" @click="applyPreset('readable')">清晰</button>
    </div>

    <div class="glass-tuner__controls">
      <label v-for="control in controls" :key="control.key" class="glass-tuner__control">
        <span>
          <strong>{{ control.label }}</strong>
          <output>{{ formattedValue(control.key, modelValue[control.key]) }}</output>
        </span>
        <input
          :value="modelValue[control.key]"
          :min="control.min"
          :max="control.max"
          :step="control.step"
          :data-testid="`glass-${control.key}`"
          type="range"
          @input="updateValue(control.key, $event)"
        />
        <small>{{ control.hint }}</small>
      </label>
    </div>

    <div class="glass-tuner__colors" aria-label="标题颜色临时预览">
      <strong>标题颜色预览</strong>
      <label v-for="control in colorControls" :key="control.key">
        <span>{{ control.label }}</span>
        <input
          type="color"
          :value="modelValue[control.key]"
          :data-testid="`glass-${control.key}`"
          @input="updateColor(control.key, $event)"
        />
      </label>
    </div>

    <footer class="glass-tuner__footer">
      <button type="button" data-testid="glass-reset" @click="emit('reset')">恢复 config.js 默认值</button>
      <small>满意后再将数值写回配置文件</small>
    </footer>
  </section>
</template>
