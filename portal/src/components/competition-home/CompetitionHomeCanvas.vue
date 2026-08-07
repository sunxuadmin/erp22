<script setup lang="ts">
import { computed, ref } from "vue";
import type {
  CompetitionHomeAction,
  CompetitionHomeContent,
  CompetitionHomeVersion
} from "../../config/competitionHome";
import type { CompetitionHomeModule } from "../../config/competitionHomeModules";

const props = defineProps<{
  content: CompetitionHomeContent;
  modules: CompetitionHomeModule[];
  version: CompetitionHomeVersion;
}>();

const emit = defineEmits<{
  action: [action: CompetitionHomeAction];
  "change-version": [version: CompetitionHomeVersion];
}>();

const selectedTrackId = ref(props.content.tracks[0]?.id || "track-a");
const selectedTrack = computed(
  () => props.content.tracks.find((track) => track.id === selectedTrackId.value) || props.content.tracks[0]
);
const factFallbacks = ["2 条赛道", "12 个作品方向", props.content.groups, props.content.uploadDeadline];

function text(module: CompetitionHomeModule, key: string, fallback: string) {
  const value = module.config[key];
  return typeof value === "string" && value.trim() ? value : fallback;
}

function flag(module: CompetitionHomeModule, key: string, fallback = true) {
  const value = module.config[key];
  return typeof value === "boolean" ? value : fallback;
}

function rows(module: CompetitionHomeModule, key: string, fallback: string[][], columns: number) {
  const configured = module.config[key];
  if (typeof configured !== "string" || !configured.trim()) return fallback;
  const parsed = configured
    .split(/\r?\n/)
    .map((line) => line.split("|").map((part) => part.trim()))
    .filter((parts) => parts.length >= columns && parts.slice(0, columns).every(Boolean))
    .map((parts) => parts.slice(0, columns));
  return parsed.length ? parsed : fallback;
}

function directionItems(module: CompetitionHomeModule) {
  const track = selectedTrack.value;
  if (!track) return [];
  const key = track.id === "track-a" ? "trackADirections" : "trackBDirections";
  return rows(
    module,
    key,
    track.directions.map((item) => [item.code, item.title, item.description]),
    3
  ).map(([code, title, description]) => ({ code, title, description }));
}

function featureItems(module: CompetitionHomeModule) {
  return rows(module, "items", props.content.artTechFeatures.map((item) => [item.index, item.title, item.description]), 3)
    .map(([index, title, description]) => ({ index, title, description }));
}

function journeyItems(module: CompetitionHomeModule) {
  return rows(module, "items", props.content.steps.map((item) => [item.index, item.mark, item.title, item.description]), 4)
    .map(([index, mark, title, description]) => ({ index, mark, title, description }));
}

function fileItems(module: CompetitionHomeModule) {
  return rows(module, "items", props.content.fileSpecs.map((item) => [item.index, item.label, item.value, item.description]), 4)
    .map(([index, label, value, description]) => ({ index, label, value, description }));
}

function milestoneItems(module: CompetitionHomeModule) {
  return rows(module, "items", props.content.milestones.map((item) => [item.index, item.date, item.year, item.title, item.description]), 5)
    .map(([index, date, year, title, description]) => ({ index, date, year, title, description }));
}

function downloadItems(module: CompetitionHomeModule) {
  return rows(module, "items", props.content.downloads.map((item) => [item.index, item.title, item.description]), 3)
    .map(([index, title, description], itemIndex) => ({
      actionId: props.content.downloads[itemIndex]?.actionId || "download-registration",
      index,
      title,
      description
    }));
}

function contactItems(module: CompetitionHomeModule) {
  return rows(module, "contacts", props.content.contacts.map((item) => [item.label, item.value]), 2)
    .map(([label, value]) => ({ label, value }));
}

function moduleStyle(module: CompetitionHomeModule) {
  return { gridColumn: `span ${module.gridW}` };
}

function action(id: CompetitionHomeAction["id"], label: string) {
  emit("action", { id, label });
}
</script>

<template>
  <main class="competition-canvas" :class="`competition-canvas--${version}`">
    <div class="competition-canvas__grid">
      <section
        v-for="module in modules"
        :key="module.componentKey"
        class="competition-module"
        :class="[
          `competition-module--${module.componentType.replace('competition-', '')}`,
          { 'is-sticky': module.componentType === 'competition-nav' && flag(module, 'sticky', true) }
        ]"
        :style="moduleStyle(module)"
        :data-component-key="module.componentKey"
      >
        <template v-if="module.componentType === 'competition-nav'">
          <nav class="module-nav" aria-label="大赛首页导航">
            <a class="module-brand" href="#top">
              <span class="module-brand__mark">CH</span>
              <span><strong>{{ text(module, 'brand', content.brand) }}</strong><small>{{ text(module, 'edition', content.edition) }}</small></span>
            </a>
            <div class="module-nav__links">
              <a href="#tracks">大赛赛道</a><a href="#journey">报送流程</a><a href="#timeline">时间节点</a><a href="#notice">通知附件</a>
            </div>
            <div class="module-nav__actions">
              <button type="button" class="button-link" @click="action('login', '登录')">登录</button>
              <button type="button" class="button-primary" @click="action('submit', text(module, 'actionLabel', '作品报送'))">
                {{ text(module, "actionLabel", "作品报送") }}
              </button>
            </div>
          </nav>
        </template>

        <template v-else-if="module.componentType === 'competition-hero'">
          <div id="top" class="module-hero">
            <div class="module-hero__copy">
              <span class="module-eyebrow">{{ text(module, "eyebrow", content.variants[version].eyebrow) }}</span>
              <h1>{{ text(module, "title", content.variants[version].heroLines.join("\n")) }}</h1>
              <p>{{ text(module, "summary", content.variants[version].heroSummary) }}</p>
              <div class="module-actions">
                <button type="button" class="button-primary" @click="action('submit', text(module, 'primaryActionLabel', '进入作品报送'))">
                  {{ text(module, "primaryActionLabel", "进入作品报送") }}
                </button>
                <button type="button" class="button-link" @click="action('official-notice', text(module, 'secondaryActionLabel', '查看大赛通知'))">
                  {{ text(module, "secondaryActionLabel", "查看大赛通知") }}
                </button>
              </div>
              <div class="module-hero__meta"><span>{{ text(module, "organizer", content.organizer) }}</span><span>{{ text(module, "groups", content.groups) }}</span><span>{{ text(module, "fee", content.fee) }}</span></div>
            </div>
            <div class="module-hero__visual" :data-style="text(module, 'visualStyle', 'sculpture')" aria-hidden="true">
              <i class="visual-loop visual-loop--one" /><i class="visual-loop visual-loop--two" /><i class="visual-loop visual-loop--three" />
              <span class="visual-status">{{ text(module, "status", content.status) }}</span>
              <span class="visual-deadline">截止<br /><strong>{{ text(module, "uploadDeadline", content.uploadDeadline) }}</strong></span>
            </div>
          </div>
        </template>

        <template v-else-if="module.componentType === 'competition-key-facts'">
          <div class="module-facts" :aria-label="text(module, 'title', '赛事关键数据')">
            <span v-for="index in 4" :key="index"><small>0{{ index }}</small><strong>{{ text(module, `item${index}`, factFallbacks[index - 1] || '') }}</strong></span>
          </div>
        </template>

        <template v-else-if="module.componentType === 'competition-tracks'">
          <div id="tracks" class="module-section">
            <header class="module-heading module-heading--split">
              <div><span class="module-eyebrow">{{ text(module, "eyebrow", content.variants[version].trackEyebrow) }}</span><h2>{{ text(module, "title", content.variants[version].trackTitle.join("\n")) }}</h2></div>
              <p>{{ text(module, "summary", content.variants[version].trackSummary) }}</p>
            </header>
            <div class="track-tabs" role="tablist" aria-label="大赛赛道">
              <button v-for="track in content.tracks" :key="track.id" type="button" role="tab" :aria-selected="selectedTrackId === track.id" :class="{ 'is-active': selectedTrackId === track.id }" @click="selectedTrackId = track.id">
                {{ track.badge }} · {{ track.title }}
              </button>
            </div>
            <div v-if="selectedTrack" class="track-layout">
              <article class="track-summary"><span>{{ selectedTrack.shortMark }}</span><small>{{ selectedTrack.english }}</small><h3>{{ text(module, selectedTrack.id === 'track-a' ? 'trackATitle' : 'trackBTitle', selectedTrack.title) }}</h3><p>{{ text(module, selectedTrack.id === 'track-a' ? 'trackADescription' : 'trackBDescription', selectedTrack.description) }}</p><div v-if="flag(module, 'showLimits')"><strong>{{ selectedTrack.perSchoolLimit }}</strong><strong>{{ selectedTrack.authorLimit }}</strong></div></article>
              <div class="direction-grid"><article v-for="direction in directionItems(module)" :key="direction.code"><small>{{ direction.code }}</small><h3>{{ direction.title }}</h3><p>{{ direction.description }}</p></article></div>
            </div>
          </div>
        </template>

        <template v-else-if="module.componentType === 'competition-art-tech'">
          <div class="module-section module-section--tint">
            <header class="module-heading"><span class="module-eyebrow">{{ text(module, "eyebrow", "ART × TECHNOLOGY") }}</span><h2>{{ text(module, "title", "当创意连接算法、材料与空间") }}</h2><p>{{ text(module, "summary", "设计开始拥有新的感官。") }}</p></header>
            <div class="feature-grid"><article v-for="feature in featureItems(module)" :key="feature.index"><small>{{ feature.index }}</small><h3>{{ feature.title }}</h3><p>{{ feature.description }}</p></article></div>
          </div>
        </template>

        <template v-else-if="module.componentType === 'competition-journey'">
          <div id="journey" class="module-section module-section--journey">
            <header class="module-heading module-heading--center"><span class="module-eyebrow">{{ text(module, "eyebrow", content.variants[version].journeyEyebrow) }}</span><h2>{{ text(module, "title", content.variants[version].journeyTitle.join("\n")) }}</h2><p>{{ text(module, "summary", "按学校组织、参赛者填报、校方审核的顺序完成报送。") }}</p></header>
            <div class="step-grid"><article v-for="step in journeyItems(module)" :key="step.index"><small>{{ step.index }}</small><span>{{ step.mark }}</span><h3>{{ step.title }}</h3><p>{{ step.description }}</p></article></div>
          </div>
        </template>

        <template v-else-if="module.componentType === 'competition-file-specs'">
          <div class="module-section module-section--files">
            <header class="module-heading"><span class="module-eyebrow">{{ text(module, "eyebrow", "FILE SPECIFICATION") }}</span><h2>{{ text(module, "title", content.variants[version].fileTitle.join("\n")) }}</h2><p>{{ text(module, "summary", "上传前请核对全部文件要求。") }}</p></header>
            <div class="file-grid"><article v-for="spec in fileItems(module)" :key="spec.index"><small>{{ spec.index }} · {{ spec.label }}</small><h3>{{ spec.value }}</h3><p>{{ spec.description }}</p></article></div>
            <button type="button" class="button-link file-spec-action" @click="action('platform-status', text(module, 'actionLabel', '查看报送规范'))">{{ text(module, "actionLabel", "查看报送规范") }}</button>
          </div>
        </template>

        <template v-else-if="module.componentType === 'competition-timeline'">
          <div id="timeline" class="module-section module-section--timeline">
            <header class="module-heading"><span class="module-eyebrow">{{ text(module, "eyebrow", content.variants[version].timelineEyebrow) }}</span><h2>{{ text(module, "title", content.variants[version].timelineTitle.join("\n")) }}</h2><p>{{ text(module, "summary", "请为审核、修改和材料盖章预留时间。") }}</p></header>
            <ol class="timeline-list"><li v-for="milestone in milestoneItems(module)" :key="milestone.index"><time>{{ milestone.date }}<small>{{ milestone.year }}</small></time><div><small>{{ milestone.index }}</small><h3>{{ milestone.title }}</h3><p>{{ milestone.description }}</p></div></li></ol>
          </div>
        </template>

        <template v-else-if="module.componentType === 'competition-notice-downloads'">
          <div id="notice" class="module-section module-section--notice">
            <header class="module-heading"><span class="module-eyebrow">{{ text(module, "eyebrow", content.variants[version].noticeEyebrow) }}</span><h2>{{ text(module, "title", content.variants[version].noticeTitle.join("\n")) }}</h2><p>{{ text(module, "summary", "附件尚未接入时统一显示平台筹备中。") }}</p></header>
            <div class="notice-layout"><article class="notice-card"><small>{{ text(module, "noticeDate", content.notice.date) }}</small><h3>{{ text(module, "noticeTitle", content.notice.title) }}</h3><p>{{ text(module, "noticeSummary", content.notice.summary) }}</p><button type="button" class="button-link" @click="action('official-notice', text(module, 'actionLabel', '阅读大赛通知'))">{{ text(module, "actionLabel", "阅读大赛通知") }}</button></article><div class="download-list"><button v-for="download in downloadItems(module)" :key="download.actionId" type="button" @click="action(download.actionId, download.title)"><small>{{ download.index }}</small><span><strong>{{ download.title }}</strong><em>{{ download.description }}</em></span><i>↓</i></button></div></div>
          </div>
        </template>

        <template v-else-if="module.componentType === 'competition-contact'">
          <div class="module-section module-section--contact">
            <header class="module-heading"><span class="module-eyebrow">{{ text(module, "eyebrow", "CONTACT") }}</span><h2>{{ text(module, "title", "联系组委会") }}</h2><p>{{ text(module, "summary", content.disclaimer) }}</p></header>
            <div class="contact-grid"><span v-for="contact in contactItems(module)" :key="contact.label"><small>{{ contact.label }}</small><strong>{{ contact.value }}</strong></span></div>
            <p v-if="flag(module, 'showOrganizers')" class="organizers">{{ text(module, "organizers", `主办：${content.organizer}\n承办：${content.contractors.join("、")}`) }}</p>
          </div>
        </template>

        <template v-else-if="module.componentType === 'competition-footer'">
          <footer class="module-footer"><strong>{{ text(module, "brand", content.brand) }}</strong><span>{{ text(module, "slogan", content.theme) }}</span><small>{{ text(module, "copyright", "CREATIVE HENAN · 2026") }}</small><div class="footer-version"><button type="button" :class="{ 'is-active': version === 'v1' }" @click="emit('change-version', 'v1')">液态玻璃</button><button type="button" :class="{ 'is-active': version === 'v2' }" @click="emit('change-version', 'v2')">艺术科技</button></div></footer>
        </template>
      </section>
    </div>
  </main>
</template>

<style scoped>
.competition-canvas{--accent:var(--hero-accent-color,#6558f5);--accent-2:#e75bb8;--ink:var(--section-title-color,#191b35);--muted:#6c6f86;--line:rgba(70,75,130,.13);min-height:100vh;color:var(--ink);background:radial-gradient(circle at 80% 7%,rgba(155,132,255,.22),transparent 26%),linear-gradient(180deg,var(--page-bg-start,#fff) 0%,var(--page-bg-middle,#f6f9ff) 56%,var(--page-bg-end,#f3f2ff) 100%)}
.competition-canvas--v2{--accent:#4b73ff;--accent-2:#1bc9bd;background:radial-gradient(circle at 20% 15%,rgba(137,225,255,.22),transparent 25%),linear-gradient(180deg,#fff 0%,#f5faff 55%,#fff7f2 100%)}
.competition-canvas__grid{display:grid;grid-template-columns:repeat(12,minmax(0,1fr));width:100%}.competition-module{grid-column:span 12;min-width:0}.competition-module--nav.is-sticky{position:sticky;z-index:30;top:0}.module-nav,.module-hero,.module-facts,.module-section,.module-footer{width:min(1180px,calc(100% - 40px));margin:0 auto}.module-nav{position:relative;z-index:10;display:flex;align-items:center;justify-content:space-between;gap:24px;min-height:72px;margin-top:16px;padding:10px 16px;border:1px solid rgba(255,255,255,.8);border-radius:18px;background:rgba(255,255,255,.74);box-shadow:0 14px 50px rgba(64,69,128,.11);backdrop-filter:blur(18px)}.module-brand{display:flex;align-items:center;gap:10px;color:inherit;text-decoration:none}.module-brand__mark{display:grid;width:38px;height:38px;place-items:center;border-radius:12px;color:#fff;background:linear-gradient(135deg,var(--accent),var(--accent-2));font-size:12px;font-weight:800}.module-brand span:last-child{display:grid}.module-brand small{color:var(--muted);font-size:9px;letter-spacing:.1em}.module-nav__links,.module-nav__actions,.module-actions,.module-hero__meta{display:flex;align-items:center;gap:22px}.module-nav__links a{color:#555972;font-size:13px;text-decoration:none}.button-primary,.button-link{min-height:42px;padding:0 19px;border:0;border-radius:12px;font-weight:650;cursor:pointer}.button-primary{color:#fff;background:linear-gradient(135deg,var(--accent),#7b61f3);box-shadow:0 10px 24px rgba(91,80,232,.24)}.button-link{color:var(--accent);background:rgba(95,82,241,.08)}.module-hero{display:grid;grid-template-columns:1.05fr .95fr;align-items:center;gap:60px;min-height:620px;padding:100px 0}.module-eyebrow{display:block;margin-bottom:18px;color:var(--accent);font-size:10px;font-weight:800;letter-spacing:.18em}.module-hero h1,.module-heading h2{white-space:pre-line}.module-hero h1{margin:0;font-size:clamp(54px,6.5vw,92px);line-height:.96;letter-spacing:-.07em;background:linear-gradient(135deg,var(--ink) 10%,var(--accent) 54%,var(--accent-2));background-clip:text;color:transparent}.module-hero__copy>p,.module-heading>p,.module-heading--split>p{max-width:610px;color:var(--muted);line-height:1.8}.module-actions{margin:30px 0}.module-hero__meta{flex-wrap:wrap;color:#454963;font-size:12px}.module-hero__visual{position:relative;min-height:430px;border:1px solid rgba(255,255,255,.8);border-radius:72px 24px 72px 24px;background:radial-gradient(circle at 50% 45%,rgba(255,255,255,.95),rgba(191,219,255,.48) 44%,rgba(202,181,255,.32) 75%,rgba(255,255,255,.55));box-shadow:0 35px 90px rgba(74,91,164,.2);overflow:hidden}.module-hero__visual[data-style="liquid-ring"]{border-radius:50%;background:radial-gradient(circle,rgba(255,255,255,.98),rgba(186,232,255,.54) 48%,rgba(213,188,255,.38))}.visual-loop{position:absolute;inset:24% 13%;border:22px solid transparent;border-top-color:rgba(99,107,255,.65);border-right-color:rgba(71,211,235,.55);border-radius:50%;filter:drop-shadow(0 16px 12px rgba(83,88,210,.2));transform:rotate(-18deg)}.visual-loop--two{inset:18% 22%;border-width:17px;border-top-color:rgba(255,178,92,.75);border-left-color:rgba(235,85,178,.54);transform:rotate(58deg)}.visual-loop--three{inset:32% 8%;border-width:12px;border-bottom-color:rgba(66,185,244,.55);transform:rotate(145deg)}.visual-status,.visual-deadline{position:absolute;padding:12px 15px;border:1px solid rgba(255,255,255,.8);border-radius:13px;background:rgba(255,255,255,.76);box-shadow:0 10px 28px rgba(43,57,123,.12);backdrop-filter:blur(14px);font-size:11px}.visual-status{top:36px;left:24px}.visual-deadline{right:22px;bottom:34px}.module-facts{display:grid;grid-template-columns:repeat(4,1fr);padding:18px 24px;border:1px solid rgba(255,255,255,.75);border-radius:20px;background:rgba(255,255,255,.65);box-shadow:0 18px 50px rgba(45,58,125,.08)}.module-facts span{display:grid;gap:7px;padding:8px 22px;border-right:1px solid var(--line)}.module-facts span:last-child{border:0}.module-facts small{color:var(--accent);font-size:9px}.module-section{padding:120px 0}.module-section--tint,.module-section--notice{width:100%;padding-inline:max(20px,calc((100% - 1180px)/2));background:linear-gradient(120deg,rgba(233,240,255,.75),rgba(255,245,241,.68))}.module-section--journey{width:100%;padding-inline:max(20px,calc((100% - 1180px)/2));color:#fff;background:radial-gradient(circle at 80% 25%,rgba(109,84,212,.3),transparent 30%),#1d173d}.module-section--journey .module-eyebrow,.module-section--journey .module-heading p{color:#b9b2ee}.module-heading{margin-bottom:45px}.module-heading--split{display:grid;grid-template-columns:1.1fr .9fr;align-items:end;gap:60px}.module-heading--center{text-align:center}.module-heading--center>p{margin-inline:auto}.module-heading h2{max-width:820px;margin:0;font-size:clamp(40px,5vw,68px);line-height:1.02;letter-spacing:-.055em}.track-tabs{display:flex;gap:8px;margin-bottom:28px;padding:7px;border:1px solid var(--line);border-radius:16px;background:rgba(240,241,252,.7)}.track-tabs button{flex:1;min-height:48px;border:0;border-radius:11px;color:var(--muted);background:transparent;cursor:pointer}.track-tabs button.is-active{color:var(--accent);background:#fff;box-shadow:0 8px 22px rgba(57,67,128,.09)}.track-layout{display:grid;grid-template-columns:.85fr 1.55fr;gap:22px}.track-summary,.direction-grid article,.feature-grid article,.step-grid article,.file-grid article,.notice-card,.download-list button{border:1px solid var(--line);border-radius:20px;background:rgba(255,255,255,.74);box-shadow:0 14px 40px rgba(48,57,112,.07)}.track-summary{padding:34px}.track-summary>span{display:grid;width:84px;height:84px;place-items:center;margin-bottom:54px;border-radius:50%;color:#fff;background:linear-gradient(135deg,var(--accent),var(--accent-2));font-size:38px}.track-summary small,.direction-grid small,.feature-grid small,.step-grid small,.file-grid small,.notice-card small{color:var(--accent);font-size:10px;letter-spacing:.12em}.track-summary p,.direction-grid p,.feature-grid p,.step-grid p,.file-grid p,.notice-card p,.timeline-list p{color:var(--muted);line-height:1.7}.track-summary div{display:flex;gap:25px;margin-top:32px;color:var(--accent);font-size:12px}.direction-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:12px}.direction-grid article{padding:22px}.direction-grid h3,.feature-grid h3,.step-grid h3,.file-grid h3,.timeline-list h3,.notice-card h3{margin:9px 0}.feature-grid,.step-grid,.file-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:16px}.feature-grid article,.file-grid article{min-height:190px;padding:26px}.file-spec-action{margin-top:20px}.step-grid{grid-template-columns:repeat(4,1fr)}.step-grid article{min-height:230px;padding:24px;color:var(--ink)}.step-grid article>span{display:grid;width:42px;height:42px;place-items:center;margin:20px 0;border-radius:12px;color:#fff;background:linear-gradient(135deg,var(--accent),var(--accent-2))}.timeline-list{display:grid;gap:0;width:min(720px,100%);margin:-150px 0 0 auto;padding:0;list-style:none}.timeline-list li{display:grid;grid-template-columns:120px 1fr;gap:22px;padding:20px 0;border-bottom:1px solid var(--line)}.timeline-list time{display:grid;align-content:center;min-height:74px;padding:12px;border-radius:16px;color:var(--accent);background:rgba(104,88,241,.09);font-size:18px;font-weight:800}.timeline-list time small{font-size:10px}.timeline-list div>small{color:#aaaaca}.notice-layout{display:grid;grid-template-columns:1.05fr .95fr;gap:18px}.notice-card{padding:34px}.download-list{display:grid;gap:10px}.download-list button{display:flex;align-items:center;gap:14px;padding:17px;border-radius:15px;text-align:left;cursor:pointer}.download-list button span{display:grid;flex:1}.download-list em{color:var(--muted);font-size:11px;font-style:normal}.download-list i{color:var(--accent);font-style:normal}.contact-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:12px}.contact-grid span{display:grid;gap:8px;padding:20px;border-top:1px solid var(--line)}.contact-grid small{color:var(--muted)}.organizers{margin-top:28px;color:var(--muted);white-space:pre-line}.module-footer{display:grid;grid-template-columns:1fr 1fr 1fr auto;align-items:center;gap:22px;padding:42px 0;border-top:1px solid var(--line)}.module-footer span,.module-footer small{color:var(--muted)}.footer-version{display:flex;gap:6px}.footer-version button{padding:8px 10px;border:1px solid var(--line);border-radius:9px;background:transparent;color:var(--muted);cursor:pointer}.footer-version button.is-active{color:#fff;background:var(--accent)}
@media(max-width:900px){.module-nav__links{display:none}.module-hero{grid-template-columns:1fr;min-height:0;padding:80px 0}.module-hero__visual{min-height:360px}.module-heading--split,.track-layout,.notice-layout{grid-template-columns:1fr}.module-facts{grid-template-columns:repeat(2,1fr)}.module-facts span:nth-child(2){border-right:0}.direction-grid,.feature-grid,.file-grid,.contact-grid{grid-template-columns:repeat(2,1fr)}.step-grid{grid-template-columns:repeat(2,1fr)}.timeline-list{margin:30px 0 0}.module-footer{grid-template-columns:1fr 1fr}.competition-module{grid-column:span 12!important}}
@media(max-width:560px){.module-nav,.module-hero,.module-facts,.module-section,.module-footer{width:calc(100% - 24px)}.module-nav{min-height:62px;margin-top:8px}.module-nav__actions .button-link{display:none}.module-brand small{display:none}.module-hero{padding:58px 0}.module-hero h1{font-size:48px}.module-hero__visual{min-height:280px;border-radius:42px 18px}.module-section{padding-block:76px}.module-section--tint,.module-section--notice,.module-section--journey{width:100%;padding-inline:12px}.module-heading h2{font-size:38px}.module-heading--split{gap:12px}.track-tabs{display:grid}.direction-grid,.feature-grid,.step-grid,.file-grid,.contact-grid,.module-footer{grid-template-columns:1fr}.track-summary>span{margin-bottom:28px}.module-facts{padding:12px}.module-facts span{padding:10px}.timeline-list li{grid-template-columns:90px 1fr}.module-actions{align-items:stretch;flex-direction:column}.module-hero__meta{gap:10px}.footer-version{flex-wrap:wrap}}
@media(prefers-reduced-motion:reduce){.visual-loop{filter:none}.competition-canvas *{scroll-behavior:auto!important}}
</style>
