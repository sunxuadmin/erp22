<template>
  <div ref="wrapRef" class="signature-pad">
    <div class="signature-pad-head">
      <span>请按住鼠标、触摸板或触摸笔书写签名</span>
      <el-button size="small" link type="primary" :disabled="!hasInk" @click="clear">清空重写</el-button>
    </div>
    <canvas
      ref="canvasRef"
      class="signature-pad-canvas"
      aria-label="手写签名画板"
      @pointerdown="startStroke"
      @pointermove="continueStroke"
      @pointerup="endStroke"
      @pointercancel="endStroke"
      @lostpointercapture="endStroke"
    />
  </div>
</template>

<script setup lang="ts">
const emit = defineEmits<{
  change: [];
}>();

const canvasRef = ref<HTMLCanvasElement>();
const wrapRef = ref<HTMLElement>();
const hasInk = ref(false);

let drawing = false;
let lastPoint: { x: number; y: number } | undefined;
let resizeObserver: ResizeObserver | undefined;

const context = () => canvasRef.value?.getContext('2d') || undefined;

const syncCanvasSize = () => {
  const canvas = canvasRef.value;
  const wrap = wrapRef.value;
  if (!canvas || !wrap) return;
  const width = Math.max(1, Math.floor(wrap.clientWidth));
  const height = 220;
  const ratio = Math.max(1, Math.min(window.devicePixelRatio || 1, 3));
  if (canvas.width === Math.floor(width * ratio) && canvas.height === Math.floor(height * ratio)) return;
  const hadInk = hasInk.value && canvas.width > 0 && canvas.height > 0;
  const snapshot = hadInk ? document.createElement('canvas') : undefined;
  if (snapshot) {
    snapshot.width = canvas.width;
    snapshot.height = canvas.height;
    snapshot.getContext('2d')?.drawImage(canvas, 0, 0);
  }
  canvas.width = Math.floor(width * ratio);
  canvas.height = Math.floor(height * ratio);
  canvas.style.height = `${height}px`;
  const ctx = context();
  if (!ctx) return;
  ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
  ctx.strokeStyle = '#111827';
  ctx.lineWidth = 2.2;
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';
  if (snapshot) {
    ctx.drawImage(snapshot, 0, 0, snapshot.width, snapshot.height, 0, 0, width, height);
  }
  hasInk.value = !!snapshot;
};

const pointOf = (event: PointerEvent) => {
  const rect = canvasRef.value?.getBoundingClientRect();
  if (!rect) return undefined;
  return {
    x: Math.max(0, Math.min(rect.width, event.clientX - rect.left)),
    y: Math.max(0, Math.min(rect.height, event.clientY - rect.top))
  };
};

const startStroke = (event: PointerEvent) => {
  if (event.button !== 0 && event.pointerType !== 'touch') return;
  const point = pointOf(event);
  const canvas = canvasRef.value;
  const ctx = context();
  if (!point || !canvas || !ctx) return;
  event.preventDefault();
  drawing = true;
  lastPoint = point;
  canvas.setPointerCapture?.(event.pointerId);
  ctx.beginPath();
  ctx.moveTo(point.x, point.y);
  ctx.lineTo(point.x + 0.01, point.y + 0.01);
  ctx.stroke();
  hasInk.value = true;
  emit('change');
};

const continueStroke = (event: PointerEvent) => {
  if (!drawing) return;
  const point = pointOf(event);
  const ctx = context();
  if (!point || !ctx || !lastPoint) return;
  event.preventDefault();
  ctx.beginPath();
  ctx.moveTo(lastPoint.x, lastPoint.y);
  ctx.lineTo(point.x, point.y);
  ctx.stroke();
  lastPoint = point;
  hasInk.value = true;
};

const endStroke = (event?: PointerEvent) => {
  if (event) {
    const canvas = canvasRef.value;
    if (canvas?.hasPointerCapture?.(event.pointerId)) {
      canvas.releasePointerCapture(event.pointerId);
    }
  }
  drawing = false;
  lastPoint = undefined;
};

const clear = () => {
  const canvas = canvasRef.value;
  const ctx = context();
  if (!canvas || !ctx) return;
  ctx.save();
  ctx.setTransform(1, 0, 0, 1, 0, 0);
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  ctx.restore();
  if (hasInk.value) emit('change');
  hasInk.value = false;
};

const toBlob = () =>
  new Promise<Blob | undefined>((resolve) => {
    if (!canvasRef.value || !hasInk.value) {
      resolve(undefined);
      return;
    }
    canvasRef.value.toBlob((blob) => resolve(blob || undefined), 'image/png');
  });

defineExpose({
  clear,
  isEmpty: () => !hasInk.value,
  toBlob
});

onMounted(() => {
  nextTick(() => {
    syncCanvasSize();
    resizeObserver = new ResizeObserver(syncCanvasSize);
    if (wrapRef.value) resizeObserver.observe(wrapRef.value);
  });
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  resizeObserver = undefined;
});
</script>

<style scoped lang="scss">
.signature-pad {
  overflow: hidden;
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  background: linear-gradient(#fff, #fbfcff);
}

.signature-pad-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 10px;
  color: var(--el-text-color-secondary);
  border-bottom: 1px solid var(--el-border-color-lighter);
  font-size: 13px;
}

.signature-pad-canvas {
  display: block;
  width: 100%;
  cursor: crosshair;
  touch-action: none;
}
</style>
