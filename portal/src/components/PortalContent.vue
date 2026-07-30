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

const apiBase = String(import.meta.env.VITE_PORTAL_API_BASE || "/prod-api").replace(/\/$/, "");
const siteCode = String(import.meta.env.VITE_PORTAL_SITE_CODE || "crehn").trim();
const articles = ref<PortalArticle[]>([]);
const components = ref<PortalComponent[]>([]);
const loading = ref(true);
const errorMessage = ref("");

async function getData<T>(path: string): Promise<T> {
  const response = await fetch(`${apiBase}${path}`, { headers: { Accept: "application/json" } });
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  const payload = (await response.json()) as ApiResult<T>;
  if (payload.code !== 200) throw new Error(payload.msg || "门户数据读取失败");
  return payload.data;
}

function componentTitle(component: PortalComponent) {
  if (!component.configJson) return component.componentKey;
  const parsed = JSON.parse(component.configJson) as { title?: unknown };
  return typeof parsed.title === "string" && parsed.title.trim() ? parsed.title : component.componentKey;
}

onMounted(async () => {
  try {
    const site = await getData<PortalSite>(`/crehn/cms/public/site/code/${encodeURIComponent(siteCode)}`);
    [articles.value, components.value] = await Promise.all([
      getData<PortalArticle[]>(`/crehn/cms/public/site/${site.id}/articles`),
      getData<PortalComponent[]>(`/crehn/cms/public/site/${site.id}/home`)
    ]);
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
      <div v-if="components.length" class="portal-content__components">
        <article v-for="component in components" :key="component.id" class="portal-component">
          <small>{{ component.componentType }}</small>
          <strong>{{ componentTitle(component) }}</strong>
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
@media (max-width: 640px) {
  .portal-content {
    width: calc(100% - 20px);
    margin-top: 24px;
    padding: 20px;
    border-radius: 18px;
  }
}
</style>
