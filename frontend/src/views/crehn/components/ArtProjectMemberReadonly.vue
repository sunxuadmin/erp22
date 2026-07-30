<template>
  <el-empty v-if="!members.length" :image-size="54" description="暂无成员信息" />
  <div v-else class="art-member-readonly">
    <table class="art-member-readonly__table">
      <thead>
        <tr>
          <th>类型</th>
          <th v-for="field in fields" :key="field.fieldKey">{{ memberFieldLabel(field) }}</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(member, index) in members" :key="member.id || index">
          <td>{{ memberTypeLabel(member.memberType) }}</td>
          <td v-for="field in fields" :key="field.fieldKey">{{ formatProjectMemberValue(member, field) }}</td>
        </tr>
      </tbody>
    </table>

    <div class="art-member-readonly__stack">
      <section v-for="(member, index) in members" :key="member.id || index" class="art-member-readonly__item">
        <div class="art-member-readonly__item-head">
          <strong>成员 {{ index + 1 }}</strong>
          <el-tag size="small" effect="plain">{{ memberTypeLabel(member.memberType) }}</el-tag>
        </div>
        <dl>
          <div v-for="field in fields" :key="field.fieldKey" class="art-member-readonly__row">
            <dt>{{ memberFieldLabel(field) }}</dt>
            <dd>{{ formatProjectMemberValue(member, field) }}</dd>
          </div>
        </dl>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ProjectVO } from '@/api/crehn/types';
import { buildProjectMemberFields, formatProjectMemberValue, memberFieldLabel, memberTypeLabel } from '@/utils/artProjectDetail';

const props = defineProps<{
  project?: ProjectVO;
}>();

const members = computed(() => props.project?.members || []);
const fields = computed(() => buildProjectMemberFields(props.project));
</script>

<style scoped>
.art-member-readonly {
  width: 100%;
  max-width: 100%;
  overflow: hidden;
  border: 1px solid var(--el-table-border-color);
  border-radius: 4px;
}

.art-member-readonly__table {
  width: 100%;
  table-layout: fixed;
  border-spacing: 0;
  border-collapse: collapse;
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.art-member-readonly__table th,
.art-member-readonly__table td {
  padding: 9px 8px;
  overflow-wrap: anywhere;
  word-break: break-word;
  white-space: normal;
  vertical-align: middle;
  border-right: 1px solid var(--el-table-border-color);
  border-bottom: 1px solid var(--el-table-border-color);
}

.art-member-readonly__table th {
  color: var(--el-text-color-primary);
  font-weight: 700;
  line-height: 1.35;
  background: var(--el-fill-color-light);
}

.art-member-readonly__table th:first-child,
.art-member-readonly__table td:first-child {
  width: 78px;
}

.art-member-readonly__table th:last-child,
.art-member-readonly__table td:last-child {
  border-right: 0;
}

.art-member-readonly__table tbody tr:last-child td {
  border-bottom: 0;
}

.art-member-readonly__stack {
  display: none;
}

@media (max-width: 768px) {
  .art-member-readonly {
    border: 0;
  }

  .art-member-readonly__table {
    display: none;
  }

  .art-member-readonly__stack {
    display: grid;
    gap: 10px;
  }

  .art-member-readonly__item {
    overflow: hidden;
    border: 1px solid var(--el-table-border-color);
    border-radius: 6px;
    background: #fff;
  }

  .art-member-readonly__item-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    padding: 9px 10px;
    border-bottom: 1px solid var(--el-table-border-color);
    background: var(--el-fill-color-light);
  }

  .art-member-readonly__item dl {
    margin: 0;
  }

  .art-member-readonly__row {
    display: grid;
    grid-template-columns: minmax(88px, 34%) minmax(0, 1fr);
    border-bottom: 1px solid var(--el-table-border-color);
  }

  .art-member-readonly__row:last-child {
    border-bottom: 0;
  }

  .art-member-readonly__row dt,
  .art-member-readonly__row dd {
    margin: 0;
    padding: 8px 10px;
    overflow-wrap: anywhere;
    word-break: break-word;
    white-space: normal;
  }

  .art-member-readonly__row dt {
    color: var(--el-text-color-secondary);
    background: var(--el-fill-color-lighter);
  }
}
</style>
