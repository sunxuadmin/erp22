<template>
  <section class="workbench-card">
    <div class="workbench-card__head">
      <div>
        <span>SUBMISSION</span>
        <h2>{{ title }}</h2>
      </div>
      <el-select v-model="selectedEdition" class="edition-select" size="small">
        <el-option label="2026 大学生艺术展演节目和作品报送" value="2026" />
      </el-select>
    </div>

    <div class="submission-layout">
      <nav class="submission-tabs" aria-label="报送大类">
        <button v-for="group in submissionGroups" :key="group.key" type="button" :class="{ active: activeKey === group.key }" @click="activeKey = group.key">
          <strong>{{ group.title }}</strong>
          <span>{{ group.desc }}</span>
        </button>
      </nav>

      <div class="submission-content">
        <div class="submission-content-head">
          <h3>{{ activeGroup?.title }}</h3>
          <p>{{ activeGroup?.desc }}</p>
        </div>
        <div class="submission-card-grid">
          <article v-for="item in activeGroup?.items" :key="item.title" class="submission-card">
            <div>
              <h4>{{ item.title }}</h4>
              <p>{{ item.desc }}</p>
            </div>
            <div class="submission-card-foot">
              <span>已报送 {{ statsMap[item.categoryCode]?.totalCount || 0 }} 项 / 已提交 {{ statsMap[item.categoryCode]?.submittedCount || 0 }} 项 / 完成 {{ statsMap[item.categoryCode]?.completedCount || 0 }} 项</span>
            </div>
          </article>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { listMyProjectCategoryStats } from '@/api/crehn/project';

defineProps<{ title: string }>();

const selectedEdition = ref('2026');
const activeKey = ref('workshop');
const statsMap = ref<Record<string, any>>({});

const submissionGroups = [
  {
    key: 'workshop',
    title: '艺术实践工作坊',
    desc: '上传工作坊视频和项目介绍文档。',
    items: [{ title: '艺术实践工作坊', desc: '教师 1-3 人，学生 7-9 人。', categoryCode: 'workshop' }]
  },
  {
    key: 'achievement',
    title: '美育改革创新优秀成果',
    desc: '填写标题、简介、作者信息并上传正文。',
    items: [
      { title: '学术论文', desc: '上传 Word 正文。', categoryCode: 'achievement_paper' },
      { title: '教学改革案例', desc: '可上传正文、视频和图片。', categoryCode: 'achievement_case' }
    ]
  },
  {
    key: 'performance',
    title: '艺术表演类',
    desc: '声乐、器乐、舞蹈、戏剧、朗诵及个人项目。',
    items: [
      { title: '声乐', desc: '合唱可填两首曲目。', categoryCode: 'performance_vocal' },
      { title: '器乐', desc: '伴奏、指挥等身份请在人员备注中说明。', categoryCode: 'performance_instrumental' },
      { title: '舞蹈', desc: '填写节目时长、作品简述并上传视频。', categoryCode: 'performance_dance' },
      { title: '戏剧', desc: '填写节目时长、作品简述并上传视频。', categoryCode: 'performance_drama' },
      { title: '朗诵', desc: '填写节目时长、作品简述并上传视频。', categoryCode: 'performance_recitation' },
      { title: '个人项目', desc: '在备注中说明演唱、演奏等身份。', categoryCode: 'performance_personal' }
    ]
  },
  {
    key: 'artwork',
    title: '艺术作品类',
    desc: '美术、设计、大艺展设计及影像作品报送。',
    items: [
      { title: '美术类', desc: '填写类别、组别、尺寸和创作说明。', categoryCode: 'artwork_fine_art' },
      { title: '大艺展设计类', desc: '额外上传过程性作品。', categoryCode: 'artwork_grand_design' },
      { title: '设计类', desc: '作者信息直接在表单中填写。', categoryCode: 'artwork_design' },
      { title: '影像类', desc: '按影视作品类别校验时长。', categoryCode: 'artwork_film' }
    ]
  },
  {
    key: 'principal',
    title: '高校校长书画作品',
    desc: '校长书画作品作者信息直接在表单中填写。',
    items: [{ title: '高校校长书画作品', desc: '作品类别限定书法或绘画。', categoryCode: 'artwork_principal' }]
  }
];

const activeGroup = computed(() => submissionGroups.find((group) => group.key === activeKey.value));

const loadStats = async () => {
  const res: any = await listMyProjectCategoryStats({});
  const map: Record<string, any> = {};
  (res.data || []).forEach((item: any) => {
    if (item.categoryCode) {
      map[item.categoryCode] = item;
    }
  });
  statsMap.value = map;
};

onMounted(loadStats);
</script>

<style scoped lang="scss">
.edition-select {
  width: 280px;
}

.submission-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 18px;
}

.submission-tabs {
  display: grid;
  gap: 10px;
}

.submission-tabs button {
  min-height: 82px;
  padding: 14px;
  border: 1px solid #dbe7f3;
  border-radius: 8px;
  background: #f8fbff;
  text-align: left;
  cursor: pointer;
}

.submission-tabs button.active {
  border-color: #2563eb;
  background: #eef4ff;
  box-shadow: inset 3px 0 0 #2563eb;
}

.submission-tabs strong {
  display: block;
  margin-bottom: 8px;
  color: #2563eb;
}

.submission-tabs span,
.submission-card p,
.submission-card-foot {
  color: #606f7b;
  font-size: 13px;
  line-height: 1.5;
}

.submission-content-head {
  margin-bottom: 12px;
}

.submission-content-head h3 {
  margin: 0 0 6px;
  font-size: 18px;
}

.submission-content-head p {
  margin: 0;
  color: #606f7b;
}

.submission-card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.submission-card {
  min-height: 126px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #dbe7f3;
  border-radius: 8px;
  background: #f8fbff;
}

.submission-card h4 {
  margin: 0 0 8px;
  font-size: 16px;
}

@media (max-width: 900px) {
  .submission-layout,
  .submission-card-grid {
    grid-template-columns: 1fr;
  }

  .edition-select {
    width: 100%;
  }
}
</style>
