<template>
  <div>
    <el-dropdown trigger="click" @command="handleSetSize">
      <div class="size-icon--style">
        <svg-icon class-name="size-icon" icon-class="size" />
      </div>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item v-for="item of sizeOptions" :key="item.value" :disabled="mode === item.value" :command="item.value">
            {{ item.label }}
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup lang="ts">
import { useAppStore } from '@/store/modules/app';
import type { LayoutDisplayScaleMode } from '@/utils/layoutDisplayScale';

const appStore = useAppStore();
const mode = computed(() => appStore.layoutDisplayScaleMode);

const sizeOptions = computed<Array<{ label: string; value: LayoutDisplayScaleMode }>>(() => [
  { label: `自动适配（当前 ${appStore.layoutDisplayScalePercent}%）`, value: 'auto' },
  { label: '固定默认（100%）', value: 'default' },
  { label: '固定稍小（90%）', value: 'small' },
  { label: '固定较大（110%）', value: 'large' },
  { label: `自定义（${appStore.layoutDisplayCustomScalePercent}%）`, value: 'custom' }
]);

const handleSetSize = (value: LayoutDisplayScaleMode) => {
  appStore.setLayoutDisplayScaleMode(value);
};
</script>

<style lang="scss" scoped>
.size-icon--style {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  line-height: 1;
}
</style>
