<script setup lang="ts">
import { onMounted, ref } from "vue";

interface PortalArticle {
  id: number;
  articleCode: string;
  title: string;
  summary?: string;
  publishedAt?: string;
}

interface PortalComponent {
  id: number;
  componentKey: string;
  componentType: string;
  configJson?: string;
  dataSourceCode?: string;
  gridW?: number;
  gridH?: number;
  enabled?: boolean;
}

interface PortalSite {
  id: number;
  siteCode: string;
  siteName: string;
}

interface ApiResult<T> {
  code: number;
  msg?: string;
  data: T;
}

interface PublicReviewResult {
  activityName?: string;
  categoryName?: string;
  projectNo?: string;
  projectName?: string;
  schoolName?: string;
  averageScore?: number | null;
  finalGrade?: string;
  awardLevel?: string;
  rankNo?: number | null;
  publishedAt?: string;
}

interface TableResult<T> {
  code: number;
  msg?: string;
  rows?: T[];
}

const apiBase = String(import.meta.env.VITE_PORTAL_API_BASE || "/prod-api").replace(/\/$/, "");
const siteCode = String(import.meta.env.VITE_PORTAL_SITE_CODE || "crehn").trim();
const articles = ref<PortalArticle[]>([]);
const components = ref<PortalComponent[]>([]);
const publishedResults = ref<PublicReviewResult[]>([]);
const loading = ref(true);
const errorMessage = ref("");
const resultErrorMessage = ref("");

async function getData<T>(path: string): Promise<T> {
  const response = await fetch(`${apiBase}${path}`, { headers: { Accept: "application/json" } });
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  const payload = (await response.json()) as ApiResult<T>;
  if (payload.code !== 200) throw new Error(payload.msg || "门户数据读取失败");
  return payload.data;
}

async function getRows<T>(path: string): Promise<T[]> {
  const response = await fetch(`${apiBase}${path}`, { headers: { Accept: "application/json" } });
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  const payload = (await response.json()) as TableResult<T>;
  if (payload.code !== 200) throw new Error(payload.msg || "公开结果读取失败");
  return payload.rows || [];
}

function componentTitle(component: PortalComponent) {
  const parsed = componentConfig(component);
  return typeof parsed.title === "string" && parsed.title.trim() ? parsed.title : component.componentKey;
}

function componentConfig(component: PortalComponent): Record<string, unknown> {
  if (!component.configJson) return {};
  try {
    const parsed = JSON.parse(component.configJson) as unknown;
    return parsed && typeof parsed === "object" && !Array.isArray(parsed)
      ? (parsed as Record<string, unknown>)
      : {};
  } catch {
    return {};
  }
}

function componentTypeLabel(componentType: string) {
  return {
    hero: "主视觉",
    news: "新闻资讯",
    notice: "通知公告",
    activity: "活动信息",
    schedule: "赛事日程",
    media: "媒体展示",
    stat: "数据概览",
    showcase: "成果展示",
    links: "友情链接"
  }[componentType] || "门户组件";
}

function componentStyle(component: PortalComponent) {
  const width = Math.min(12, Math.max(1, Number(component.gridW) || 12));
  const height = Math.min(4, Math.max(1, Number(component.gridH) || 1));
  return {
    gridColumn: `span ${width}`,
    minHeight: `${height * 72}px`
  };
}

onMounted(async () => {
  try {
    const site = await getData<PortalSite>(`/crehn/cms/public/site/code/${encodeURIComponent(siteCode)}`);
    [articles.value, components.value] = await Promise.all([
      getData<PortalArticle[]>(`/crehn/cms/public/site/${site.id}/articles`),
      getData<PortalComponent[]>(`/crehn/cms/public/site/${site.id}/home`)
    ]);
    try {
      publishedResults.value = await getRows<PublicReviewResult>("/crehn/result/public/published?pageNum=1&pageSize=100");
    } catch (error) {
      resultErrorMessage.value = error instanceof Error ? error.message : "公开结果读取失败";
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "门户数据读取失败";
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <section id="news" class="portal-content" aria-label="门户公开内容">
    <div class="portal-content__heading">
      <span>CREHN NEWS</span>
      <h2>通知与资讯</h2>
    </div>
    <p v-if="loading" class="portal-content__state">正在加载公开内容…</p>
    <p v-else-if="errorMessage" class="portal-content__state portal-content__state--error">
      {{ errorMessage }}
    </p>
    <template v-else>
      <div v-if="components.length" class="portal-content__components" aria-label="已发布首页组件">
        <article
          v-for="component in components"
          :key="component.id"
          class="portal-component"
          :style="componentStyle(component)"
          :data-component-type="component.componentType"
        >
          <small>{{ componentTypeLabel(component.componentType) }}</small>
          <strong>{{ componentTitle(component) }}</strong>
          <p v-if="typeof componentConfig(component).description === 'string'">
            {{ componentConfig(component).description }}
          </p>
          <span v-if="component.dataSourceCode" class="portal-component__source">{{ component.dataSourceCode }}</span>
        </article>
      </div>
      <div class="portal-content__articles">
        <a
          v-for="article in articles"
          :key="article.id"
          class="portal-article"
          :href="`?article=${encodeURIComponent(article.articleCode)}`"
        >
          <span>{{ article.publishedAt?.slice(0, 10) || "公告" }}</span>
          <strong>{{ article.title }}</strong>
          <p>{{ article.summary || "查看详情" }}</p>
        </a>
        <p v-if="!articles.length" class="portal-content__state">暂无已发布内容。</p>
      </div>
      <div class="portal-results" aria-label="已发布评审结果">
        <div class="portal-content__heading portal-results__heading">
          <span>CREHN RESULTS</span>
          <h2>已发布结果</h2>
        </div>
        <p v-if="resultErrorMessage" class="portal-content__state portal-content__state--error">{{ resultErrorMessage }}</p>
        <div v-else-if="publishedResults.length" class="portal-results__list">
          <article v-for="result in publishedResults" :key="`${result.projectNo}-${result.publishedAt}`" class="portal-result">
            <small>{{ result.activityName || "评审结果" }} · {{ result.categoryName || "未分类" }}</small>
            <strong>{{ result.projectName || result.projectNo || "未命名项目" }}</strong>
            <p>{{ result.schoolName || "-" }}</p>
            <div class="portal-result__facts">
              <span v-if="result.awardLevel">奖项：{{ result.awardLevel }}</span>
              <span v-if="result.finalGrade">等级：{{ result.finalGrade }}</span>
              <span v-if="result.averageScore != null">平均分：{{ result.averageScore }}</span>
              <span v-if="result.rankNo != null">排名：{{ result.rankNo }}</span>
            </div>
          </article>
        </div>
        <p v-else class="portal-content__state">暂无已发布结果。</p>
      </div>
    </template>
  </section>
</template>

<style scoped>
.portal-content {
  position: relative;
  z-index: 3;
  width: min(1180px, calc(100% - 32px));
  margin: 48px auto 24px;
  padding: 32px;
  border: 1px solid rgba(255, 255, 255, 0.65);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 18px 60px rgba(24, 76, 150, 0.12);
  backdrop-filter: blur(18px);
}
.portal-content__heading span,
.portal-component small,
.portal-article span {
  color: #286bd4;
  font-size: 12px;
  letter-spacing: 0.12em;
}
.portal-content__heading h2 {
  margin: 8px 0 24px;
  color: #0f2f5f;
}
.portal-content__components,
.portal-content__articles {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}
.portal-content__components {
  margin-bottom: 20px;
}
.portal-component,
.portal-article {
  display: grid;
  gap: 8px;
  padding: 18px;
  border: 1px solid rgba(80, 133, 207, 0.2);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.76);
  color: #153761;
  text-decoration: none;
}
.portal-article p,
.portal-content__state {
  margin: 0;
  color: #60738e;
}
.portal-content__state--error {
  color: #b42318;
}
.portal-results {
  margin-top: 32px;
}
.portal-results__heading h2 {
  margin-bottom: 16px;
}
.portal-results__list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}
.portal-result {
  display: grid;
  gap: 8px;
  padding: 18px;
  border: 1px solid rgba(80, 133, 207, 0.2);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.76);
  color: #153761;
}
.portal-result small,
.portal-result__facts {
  color: #286bd4;
}
.portal-result p {
  margin: 0;
  color: #60738e;
}
.portal-result__facts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  font-size: 13px;
}
@media (max-width: 640px) {
  .portal-content {
    width: calc(100% - 20px);
    margin-top: 24px;
    padding: 20px;
    border-radius: 18px;
  }
  .portal-content__components .portal-component {
    grid-column: span 12 !important;
    min-height: 0 !important;
  }
}
</style>
