<script setup lang="ts">
import { computed } from 'vue';

type ScoreConfigurationModel = {
  scoreMode?: string;
  scoreRuleJson?: string;
  exclusiveMode?: string;
  scoreVisibilityPolicy?: string;
};

const props = defineProps<{
  modelValue: ScoreConfigurationModel;
  layout?: 'single' | 'mixed';
  quickScoreText?: string;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: ScoreConfigurationModel];
  'update:quickScoreText': [value: string];
}>();

const layout = computed(() => props.layout || 'single');
const scoreMode = computed({
  get: () => props.modelValue.scoreMode,
  set: (value: string | undefined) => emit('update:modelValue', { ...props.modelValue, scoreMode: value })
});
const exclusiveMode = computed({
  get: () => props.modelValue.exclusiveMode,
  set: (value: string | undefined) => emit('update:modelValue', { ...props.modelValue, exclusiveMode: value })
});
const scoreVisibilityPolicy = computed({
  get: () => props.modelValue.scoreVisibilityPolicy,
  set: (value: string | undefined) => emit('update:modelValue', { ...props.modelValue, scoreVisibilityPolicy: value })
});
const scoreRuleJson = computed({
  get: () => props.modelValue.scoreRuleJson,
  set: (value: string | undefined) => emit('update:modelValue', { ...props.modelValue, scoreRuleJson: value })
});
const quickScoreText = computed({
  get: () => props.quickScoreText || '',
  set: (value: string) => emit('update:quickScoreText', value)
});
</script>

<template>
  <el-row :gutter="layout === 'mixed' ? 12 : 10">
    <el-col :span="layout === 'mixed' ? 8 : 12">
      <el-form-item label="评分模式">
        <el-select v-model="scoreMode">
          <el-option label="百分制" value="numeric_100" />
          <el-option label="等级制" value="grade" />
          <el-option label="仅评语" value="comment_only" />
        </el-select>
      </el-form-item>
    </el-col>
    <el-col :span="layout === 'mixed' ? 8 : 12">
      <el-form-item label="评分互斥">
        <el-select v-model="exclusiveMode">
          <el-option label="单评委互斥" value="single" />
          <el-option label="多评委可评" value="multi" />
        </el-select>
      </el-form-item>
    </el-col>
    <el-col v-if="layout === 'mixed'" :span="8">
      <el-form-item label="评分可见">
        <el-select v-model="scoreVisibilityPolicy">
          <el-option label="评分后可见" value="after_submit" />
          <el-option label="一直不可见" value="hidden" />
          <el-option label="一直可见" value="always" />
        </el-select>
      </el-form-item>
    </el-col>
  </el-row>
  <el-form-item v-if="layout === 'single' && scoreMode === 'numeric_100'" label="预设分数">
    <el-input v-model="quickScoreText" placeholder="例如：30,60,80,90,100" clearable />
  </el-form-item>
  <el-form-item v-if="layout === 'single'" label="全部评分可见">
    <el-select v-model="scoreVisibilityPolicy">
      <el-option label="评分后可见" value="after_submit" />
      <el-option label="一直不可见" value="hidden" />
      <el-option label="一直可见" value="always" />
    </el-select>
  </el-form-item>
  <el-form-item label="高级评分规则">
    <el-input v-model="scoreRuleJson" type="textarea" :rows="layout === 'mixed' ? 3 : 4" />
  </el-form-item>
</template>
