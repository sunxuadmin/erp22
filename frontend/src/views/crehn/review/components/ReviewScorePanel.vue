<script setup lang="ts">
import type { ArtDetailDisplayConfigVO } from '@/api/crehn/detailDisplay';
import type { ReviewTaskVO } from '@/api/crehn/types';
import type { DeepReadonly } from 'vue';

type ReviewScoreForm = {
  scoreValue?: number;
  gradeValue: string;
  commentText: string;
};

type NumericRule = {
  min: number;
  max: number;
  step: number;
  precision: number;
};

type TagType = 'success' | 'primary' | 'info' | 'warning' | 'danger';

const props = defineProps<{
  currentTask: ReviewTaskVO;
  detailDisplayConfig: DeepReadonly<ArtDetailDisplayConfigVO>;
  scoreForm: ReviewScoreForm;
  scoreLocked: boolean;
  numericRule: NumericRule;
  gradeOptions: string[];
  quickScoreOptions: number[];
  quickScoreGridStyle: Record<string, string>;
  scoreStatusType: (row: ReviewTaskVO) => TagType;
  scoreStatusLabel: (row: ReviewTaskVO) => string;
  applyQuickScore: (value: number) => void;
  saveScore: () => void | Promise<void>;
  saveScoreAndNext: () => void | Promise<void>;
}>();
</script>

<template>
  <section class="art-detail-workbench__side-section review-score-panel">
    <div class="art-detail-workbench__side-head">
      <strong>{{ props.detailDisplayConfig.score.title }}</strong>
      <div class="art-detail-workbench__side-tools">
        <el-tag v-if="props.detailDisplayConfig.score.statusVisible" size="small" :type="props.scoreStatusType(props.currentTask)">
          {{ props.scoreStatusLabel(props.currentTask) }}
        </el-tag>
      </div>
    </div>
    <div class="review-score-control">
      <label v-if="props.currentTask.scoreMode !== 'comment_only'">{{
        props.currentTask.scoreMode === 'grade'
          ? props.detailDisplayConfig.score.gradeLabel
          : props.detailDisplayConfig.score.scoreLabel
      }}</label>
      <el-radio-group
        v-if="props.currentTask.scoreMode === 'grade'"
        v-model="props.scoreForm.gradeValue"
        class="review-grade-group"
        :disabled="props.scoreLocked"
      >
        <el-radio-button v-for="item in props.gradeOptions" :key="item" :label="item">{{ item }}</el-radio-button>
      </el-radio-group>
      <template v-else-if="props.currentTask.scoreMode !== 'comment_only'">
        <el-input-number
          v-model="props.scoreForm.scoreValue"
          :min="props.numericRule.min"
          :max="props.numericRule.max"
          :step="props.numericRule.step"
          :precision="props.numericRule.precision"
          :disabled="props.scoreLocked"
        />
        <div v-if="props.quickScoreOptions.length" class="review-quick-scores" :style="props.quickScoreGridStyle">
          <el-button
            v-for="item in props.quickScoreOptions"
            :key="item"
            size="small"
            :disabled="props.scoreLocked"
            @click="props.applyQuickScore(item)"
          >
            {{ item }}
          </el-button>
        </div>
      </template>
      <el-alert v-else type="info" :closable="false" show-icon title="仅评语模式：不产生分数或等级，请填写评语。" />
    </div>
    <div
      v-if="props.detailDisplayConfig.score.commentVisible || props.currentTask.scoreMode === 'comment_only'"
      class="review-comment-control"
    >
      <label>{{ props.detailDisplayConfig.score.commentLabel }}</label>
      <el-input v-model="props.scoreForm.commentText" type="textarea" :rows="4" maxlength="1000" show-word-limit :disabled="props.scoreLocked" />
    </div>
    <div class="review-score-actions">
      <el-button
        v-if="!props.scoreLocked && props.detailDisplayConfig.score.saveDraftVisible"
        type="primary"
        icon="DocumentChecked"
        @click="props.saveScore"
      >
        保存评分
      </el-button>
      <el-button v-if="!props.scoreLocked" icon="ArrowRight" @click="props.saveScoreAndNext">保存并查看下一个</el-button>
      <span v-else-if="props.currentTask.scoreStatus === 'submitted'" class="review-score-locked-hint">该类别已签字，评分已锁定</span>
    </div>
  </section>
</template>
