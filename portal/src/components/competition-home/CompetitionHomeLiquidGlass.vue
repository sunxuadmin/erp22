<script setup lang="ts">
import { computed, ref } from "vue";
import type {
  CompetitionHomeAction,
  CompetitionHomeContent,
  CompetitionHomeVersion
} from "../../config/competitionHome";

const props = defineProps<{ content: CompetitionHomeContent }>();

const emit = defineEmits<{
  action: [action: CompetitionHomeAction];
  "change-version": [version: CompetitionHomeVersion];
}>();

const selectedVersion = ref<CompetitionHomeVersion>("v1");
const selectedTrackId = ref<CompetitionHomeContent["tracks"][number]["id"]>("track-a");

const variant = computed(() => props.content.variants[selectedVersion.value]);
const activeTrack = computed(
  () => props.content.tracks.find((track) => track.id === selectedTrackId.value) ?? props.content.tracks[0]
);

function changeVersion(version: CompetitionHomeVersion) {
  selectedVersion.value = version;
  emit("change-version", version);
}

function selectTrack(trackId: CompetitionHomeContent["tracks"][number]["id"]) {
  selectedTrackId.value = trackId;
}

function handleTrackKeydown(event: KeyboardEvent, index: number) {
  const trackCount = props.content.tracks.length;
  if (!trackCount || !["ArrowRight", "ArrowDown", "ArrowLeft", "ArrowUp", "Home", "End"].includes(event.key)) return;
  event.preventDefault();
  const nextIndex = event.key === "Home"
    ? 0
    : event.key === "End"
      ? trackCount - 1
      : (index + (event.key === "ArrowRight" || event.key === "ArrowDown" ? 1 : -1) + trackCount) % trackCount;
  const nextTrack = props.content.tracks[nextIndex];
  if (!nextTrack) return;
  selectTrack(nextTrack.id);
  document.getElementById(`tab-${nextTrack.id}`)?.focus();
}

function sendAction(id: CompetitionHomeAction["id"], label: string) {
  emit("action", { id, label });
}
</script>

<template>
  <main class="competition-home" aria-labelledby="competition-home-title">
    <div class="ambient ambient-one" aria-hidden="true"></div>
    <div class="ambient ambient-two" aria-hidden="true"></div>

    <header class="glass-nav" id="competition-top">
      <a class="brand-lockup" href="#competition-top" aria-label="返回创意河南首页">
        <span class="brand-mark" aria-hidden="true"><span></span><span></span><span></span></span>
        <span>
          <strong>{{ content.brand }}</strong>
          <small>{{ variant.brandEn }}</small>
        </span>
      </a>

      <nav class="desktop-nav" aria-label="主导航">
        <a href="#competition-tracks">赛道方向</a>
        <a href="#competition-path">报送流程</a>
        <a href="#competition-timeline">重要时间</a>
        <a href="#competition-notice">通知材料</a>
      </nav>

      <div class="nav-actions">
        <button class="quiet-button" type="button" @click="sendAction('platform-status', '平台状态')">平台状态</button>
        <button class="primary-button nav-login" type="button" @click="sendAction('login', '登录平台')">登录平台 <span aria-hidden="true">↗</span></button>
      </div>
    </header>

    <section class="hero section-wrap" aria-labelledby="competition-home-title">
      <div class="hero-copy">
        <p class="eyebrow"><span class="eyebrow-dot"></span>{{ variant.eyebrow }}</p>
        <h1 id="competition-home-title">
          <span v-for="line in variant.heroLines" :key="line" class="hero-line">{{ line }}</span>
        </h1>
        <p class="hero-summary">{{ variant.heroSummary }}</p>
        <div class="hero-actions">
          <button class="primary-button hero-submit" type="button" @click="sendAction('submit', '开始报送')">
            开始报送 <span aria-hidden="true">↗</span>
          </button>
          <a class="text-link" href="#competition-tracks">浏览赛道 <span aria-hidden="true">↓</span></a>
        </div>
        <div class="hero-meta" aria-label="赛事概览">
          <span><b>{{ content.edition }}</b><small>赛事届次</small></span>
          <span><b>{{ content.groups }}</b><small>参赛组别</small></span>
          <span><b>{{ content.fee }}</b><small>报名说明</small></span>
        </div>
      </div>

      <div class="hero-art" aria-label="参数化彩色玻璃雕塑视觉" role="img">
        <div class="art-orbit orbit-one"></div>
        <div class="art-orbit orbit-two"></div>
        <svg class="glass-sculpture" viewBox="0 0 620 540" aria-hidden="true">
          <defs>
            <linearGradient id="sculptureBlue" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0" stop-color="#b4e9ff" stop-opacity=".94" />
              <stop offset=".52" stop-color="#4f91ff" stop-opacity=".77" />
              <stop offset="1" stop-color="#3144a4" stop-opacity=".9" />
            </linearGradient>
            <linearGradient id="sculpturePink" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0" stop-color="#ffd5dc" stop-opacity=".92" />
              <stop offset=".56" stop-color="#ef8da9" stop-opacity=".74" />
              <stop offset="1" stop-color="#9b4bbd" stop-opacity=".88" />
            </linearGradient>
            <linearGradient id="sculptureGold" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0" stop-color="#fff2b0" stop-opacity=".98" />
              <stop offset=".55" stop-color="#f8c75d" stop-opacity=".86" />
              <stop offset="1" stop-color="#e56f7d" stop-opacity=".72" />
            </linearGradient>
            <linearGradient id="sculptureLilac" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0" stop-color="#e4d5ff" stop-opacity=".82" />
              <stop offset=".58" stop-color="#aa9df0" stop-opacity=".64" />
              <stop offset="1" stop-color="#5e61ba" stop-opacity=".84" />
            </linearGradient>
            <filter id="glassShadow" x="-30%" y="-30%" width="160%" height="170%">
              <feGaussianBlur stdDeviation="18" />
            </filter>
            <filter id="softGlow" x="-30%" y="-30%" width="160%" height="160%">
              <feGaussianBlur stdDeviation="8" result="blur" />
              <feMerge><feMergeNode in="blur" /><feMergeNode in="SourceGraphic" /></feMerge>
            </filter>
          </defs>
          <ellipse cx="316" cy="476" rx="190" ry="28" fill="#4654a7" opacity=".2" filter="url(#glassShadow)" />
          <path d="M302 435 C250 358 256 290 315 217 C357 165 401 117 398 64 C451 121 462 197 419 251 C380 300 356 343 374 432 Z" fill="url(#sculptureBlue)" opacity=".9" filter="url(#softGlow)" />
          <path d="M301 427 C247 383 209 328 224 261 C238 199 299 184 315 108 C352 162 358 220 329 267 C302 310 293 361 338 429 Z" fill="url(#sculpturePink)" opacity=".84" filter="url(#softGlow)" />
          <path d="M336 428 C387 367 436 338 449 275 C462 213 425 166 454 104 C501 167 524 237 487 301 C462 344 429 376 428 435 Z" fill="url(#sculptureGold)" opacity=".8" filter="url(#softGlow)" />
          <path d="M290 431 C292 367 266 336 283 286 C303 226 347 211 355 152 C384 223 378 287 344 333 C329 354 324 392 346 433 Z" fill="url(#sculptureLilac)" opacity=".86" />
          <path d="M316 428 C294 355 330 291 381 247 C421 213 442 170 434 132" fill="none" stroke="#f9ffff" stroke-opacity=".68" stroke-width="4" stroke-linecap="round" />
          <path d="M276 420 C239 355 242 302 282 244 C308 205 318 168 307 128" fill="none" stroke="#ffffff" stroke-opacity=".48" stroke-width="3" stroke-linecap="round" />
          <path d="M360 432 C403 374 457 347 470 291 C482 241 467 196 479 153" fill="none" stroke="#fff9dc" stroke-opacity=".56" stroke-width="3" stroke-linecap="round" />
          <circle cx="409" cy="83" r="7" fill="#fff" opacity=".82" />
          <circle cx="211" cy="246" r="5" fill="#fff" opacity=".74" />
          <circle cx="485" cy="311" r="4" fill="#fff" opacity=".72" />
        </svg>
        <div class="hero-art-caption"><span>CH / 2026</span><span>DESIGN IN MOTION</span></div>
        <aside class="floating-status">
          <span class="status-pulse" aria-hidden="true"></span>
          <div><small>当前状态</small><strong>{{ content.status }}</strong></div>
          <span class="status-arrow" aria-hidden="true">↗</span>
        </aside>
      </div>
    </section>

    <div class="info-strip section-wrap" aria-label="关键信息">
      <div><span class="strip-label">ORGANIZER</span><strong>{{ content.organizer }}</strong></div>
      <div><span class="strip-label">SUBMISSION</span><strong>{{ content.uploadDeadline }} 上传截止</strong></div>
      <div><span class="strip-label">EDITION</span><strong>{{ content.edition }}</strong></div>
      <div><span class="strip-label">MODE</span><strong>线上报送 · 线下展览</strong></div>
    </div>

    <section id="competition-tracks" class="content-section section-wrap tracks-section" aria-labelledby="tracks-title">
      <div class="section-heading split-heading">
        <div><p class="eyebrow">{{ variant.trackEyebrow }}</p><h2 id="tracks-title"><span>{{ variant.trackTitle[0] }}</span><span>{{ variant.trackTitle[1] }}</span></h2></div>
        <p class="section-summary">{{ variant.trackSummary }}</p>
      </div>
      <div class="track-tabs" role="tablist" aria-label="赛事赛道">
        <button
          v-for="track in content.tracks"
          :id="`tab-${track.id}`"
          :key="track.id"
          class="track-tab"
          :class="{ active: activeTrack.id === track.id }"
          type="button"
          role="tab"
          :aria-selected="activeTrack.id === track.id"
          :aria-controls="activeTrack.id === track.id ? `panel-${track.id}` : undefined"
          :tabindex="activeTrack.id === track.id ? 0 : -1"
          @click="selectTrack(track.id)"
          @keydown="handleTrackKeydown($event, content.tracks.indexOf(track))"
        >
          <span class="tab-mark">{{ track.shortMark }}</span><span><small>{{ track.badge }}</small><strong>{{ track.title }}</strong></span><span class="tab-arrow" aria-hidden="true">↗</span>
        </button>
      </div>
      <div :id="`panel-${activeTrack.id}`" class="track-panel" role="tabpanel" :aria-labelledby="`tab-${activeTrack.id}`">
        <div class="track-intro"><span class="track-english">{{ activeTrack.english }}</span><p>{{ activeTrack.description }}</p><div class="track-limits"><span><b>{{ activeTrack.perSchoolLimit }}</b><small>报送上限</small></span><span><b>{{ activeTrack.authorLimit }}</b><small>作者人数</small></span></div></div>
        <div class="direction-grid">
          <article v-for="direction in activeTrack.directions" :key="direction.code" class="direction-card">
            <span class="direction-code">{{ direction.code }}</span><h3>{{ direction.title }}</h3><p>{{ direction.description }}</p><span class="card-plus" aria-hidden="true">+</span>
          </article>
        </div>
      </div>
      <div class="art-tech-row" aria-label="艺术与科技亮点">
        <article v-for="feature in content.artTechFeatures" :key="feature.index"><span>{{ feature.index }}</span><div><strong>{{ feature.title }}</strong><p>{{ feature.description }}</p></div></article>
      </div>
    </section>

    <section id="competition-path" class="content-section path-section section-wrap" aria-labelledby="path-title">
      <div class="section-heading split-heading"><div><p class="eyebrow">{{ variant.journeyEyebrow }}</p><h2 id="path-title"><span>{{ variant.journeyTitle[0] }}</span><span>{{ variant.journeyTitle[1] }}</span></h2></div><p class="section-summary">报名、上传、审核，每一步都有明确的责任人与截止时间。请按学校组织流程提前准备。</p></div>
      <div class="steps-grid">
        <article v-for="step in content.steps" :key="step.index" class="step-card"><span class="step-index">{{ step.index }}</span><span class="step-mark">{{ step.mark }}</span><h3>{{ step.title }}</h3><p>{{ step.description }}</p><span class="step-line" aria-hidden="true"></span></article>
      </div>
      <div class="file-layout"><div class="file-heading"><p class="eyebrow">FILE SPECIFICATION</p><h2><span>{{ variant.fileTitle[0] }}</span><span>{{ variant.fileTitle[1] }}</span></h2></div><div class="file-grid"><article v-for="file in content.fileSpecs" :key="file.index" class="file-card"><span>{{ file.index }}</span><div><strong>{{ file.label }}</strong><b>{{ file.value }}</b><p>{{ file.description }}</p></div></article></div></div>
    </section>

    <section id="competition-timeline" class="content-section timeline-section section-wrap" aria-labelledby="timeline-title">
      <div class="section-heading"><p class="eyebrow">{{ variant.timelineEyebrow }}</p><h2 id="timeline-title"><span>{{ variant.timelineTitle[0] }}</span><span>{{ variant.timelineTitle[1] }}</span></h2></div>
      <div class="timeline-list"><article v-for="milestone in content.milestones" :key="milestone.index" class="timeline-item"><span class="timeline-dot" aria-hidden="true"></span><span class="timeline-index">{{ milestone.index }}</span><time><b>{{ milestone.date }}</b><small>{{ milestone.year }}</small></time><div><h3>{{ milestone.title }}</h3><p>{{ milestone.description }}</p></div></article></div>
    </section>

    <section id="competition-notice" class="content-section notice-section section-wrap" aria-labelledby="notice-title">
      <div class="notice-header"><div><p class="eyebrow">{{ variant.noticeEyebrow }}</p><h2 id="notice-title"><span>{{ variant.noticeTitle[0] }}</span><span>{{ variant.noticeTitle[1] }}</span></h2></div><button class="primary-button" type="button" @click="sendAction('official-notice', '查看正式通知')">查看正式通知 <span aria-hidden="true">↗</span></button></div>
      <slot name="cms"></slot>
      <div class="notice-card"><div class="notice-date">{{ content.notice.date }}</div><div><span class="notice-tag">OFFICIAL NOTICE</span><h3>{{ content.notice.title }}</h3><p>{{ content.notice.summary }}</p></div><span class="notice-card-arrow" aria-hidden="true">↗</span></div>
      <div class="download-grid"><button v-for="item in content.downloads" :key="item.actionId" class="download-card" type="button" @click="sendAction(item.actionId, item.title)"><span>{{ item.index }}</span><div><strong>{{ item.title }}</strong><small>{{ item.description }}</small></div><b aria-hidden="true">↓</b></button></div>
    </section>

    <section class="contact-section section-wrap" aria-labelledby="contact-title">
      <div><p class="eyebrow">CONTACT / ORGANIZATION</p><h2 id="contact-title">一起，让河南<br /><em>被更多人看见。</em></h2></div>
      <div class="contact-details"><div class="contact-list"><p v-for="contact in content.contacts" :key="contact.label"><span>{{ contact.label }}</span><strong>{{ contact.value }}</strong></p></div><div class="contractors"><span>承办单位</span><p v-for="contractor in content.contractors" :key="contractor">{{ contractor }}</p></div></div>
    </section>

    <footer class="dark-footer"><div class="footer-inner section-wrap"><div><span class="footer-brand">{{ content.brand }}</span><p>{{ content.theme }}</p></div><div class="footer-links"><a href="#competition-top">返回顶部 ↑</a><button type="button" @click="selectedVersion !== 'v1' && changeVersion('v1')">V1 液态玻璃</button><button type="button" @click="changeVersion('v2')">V2 艺术 × 科技</button></div><small>© 2026 {{ content.brand }} · {{ content.disclaimer }}</small></div></footer>
  </main>
</template>

<style scoped>
:global(html) { scroll-behavior: smooth; }
:global(body) { background: #f7f4f1; }

.competition-home { --ink: #272540; --muted: #77758a; --purple: #6556c5; --line: rgba(53, 45, 101, .12); position: relative; overflow: hidden; color: var(--ink); background: linear-gradient(145deg, #fbfaf6 0%, #f1ecf6 42%, #faf7f3 100%); }
.competition-home::before { content: ""; position: absolute; inset: 0; pointer-events: none; background-image: radial-gradient(rgba(82, 73, 134, .1) .7px, transparent .7px); background-size: 24px 24px; opacity: .28; }
.ambient { position: absolute; pointer-events: none; border-radius: 50%; filter: blur(1px); opacity: .65; }
.ambient-one { width: 460px; height: 460px; top: 160px; right: -220px; background: radial-gradient(circle, rgba(225, 183, 245, .48), transparent 68%); }
.ambient-two { width: 420px; height: 420px; top: 740px; left: -230px; background: radial-gradient(circle, rgba(185, 215, 255, .4), transparent 70%); }
.section-wrap { width: min(1180px, calc(100% - 64px)); margin: 0 auto; position: relative; z-index: 1; }
.glass-nav { position: sticky; top: 0; z-index: 20; display: flex; align-items: center; justify-content: space-between; gap: 28px; min-height: 78px; padding: 13px max(32px, calc((100vw - 1300px) / 2)); border-bottom: 1px solid rgba(255, 255, 255, .72); background: rgba(250, 248, 247, .7); box-shadow: 0 12px 35px rgba(80, 67, 112, .06); backdrop-filter: blur(20px) saturate(1.2); }
.brand-lockup { display: inline-flex; align-items: center; gap: 12px; color: var(--ink); min-width: max-content; }
.brand-lockup strong, .brand-lockup small { display: block; }
.brand-lockup strong { font-size: 16px; letter-spacing: .14em; }
.brand-lockup small { margin-top: 3px; color: #9087b1; font-size: 8px; letter-spacing: .2em; }
.brand-mark { position: relative; display: inline-block; width: 28px; height: 31px; transform: rotate(32deg); }
.brand-mark span { position: absolute; width: 11px; height: 25px; border-radius: 9px 9px 2px 9px; background: linear-gradient(160deg, #ffc2b7, #9a62c6); box-shadow: inset -2px -2px 3px rgba(67, 46, 131, .22); }
.brand-mark span:nth-child(1) { left: 0; top: 3px; transform: rotate(-16deg); background: linear-gradient(160deg, #b6dcff, #7066c6); }
.brand-mark span:nth-child(2) { left: 9px; top: 0; transform: rotate(8deg); }
.brand-mark span:nth-child(3) { right: 0; top: 6px; transform: rotate(29deg); background: linear-gradient(160deg, #fff0a7, #f1879d); }
.desktop-nav { display: flex; gap: clamp(18px, 3vw, 43px); margin-left: auto; }
.desktop-nav a, .quiet-button, .footer-links a, .footer-links button { color: #656276; font-size: 12px; letter-spacing: .05em; transition: color .2s ease; }
.desktop-nav a:hover, .desktop-nav a:focus-visible, .footer-links a:hover, .footer-links button:hover { color: var(--purple); }
.nav-actions, .hero-actions, .notice-header { display: flex; align-items: center; gap: 14px; }
button { border: 0; cursor: pointer; }
.quiet-button { padding: 11px 4px; background: transparent; }
.primary-button { display: inline-flex; align-items: center; gap: 14px; min-height: 42px; padding: 0 18px; border-radius: 999px; color: #fff; background: linear-gradient(120deg, #6554c5, #8b69bd); box-shadow: 0 10px 23px rgba(99, 78, 170, .24); font-size: 12px; letter-spacing: .06em; transition: transform .2s ease, box-shadow .2s ease; }
.primary-button span { font-size: 17px; line-height: 1; }
.primary-button:hover { transform: translateY(-2px); box-shadow: 0 13px 28px rgba(99, 78, 170, .32); }
.hero { display: grid; grid-template-columns: minmax(0, .92fr) minmax(440px, 1.08fr); gap: 52px; align-items: center; min-height: 650px; padding-top: 74px; padding-bottom: 68px; }
.eyebrow { margin: 0 0 20px; color: #8d7fac; font-size: 10px; font-weight: 700; letter-spacing: .2em; text-transform: uppercase; }
.eyebrow-dot { display: inline-block; width: 7px; height: 7px; margin-right: 10px; border-radius: 50%; vertical-align: 1px; background: #ed9a9b; box-shadow: 0 0 0 4px rgba(237, 154, 155, .14); }
.hero h1, .section-heading h2, .file-heading h2, .contact-section h2 { margin: 0; color: #292745; font-weight: 500; letter-spacing: -.07em; }
.hero h1 { max-width: 580px; font-size: clamp(54px, 7.5vw, 100px); line-height: .98; }
.hero-line, .section-heading h2 span, .file-heading h2 span, .contact-section h2 em { display: block; }
.hero-line:nth-child(2), .section-heading h2 span:last-child, .file-heading h2 span:last-child { color: #7963be; }
.hero-summary { max-width: 430px; margin: 28px 0 29px; color: #747184; font-size: 14px; line-height: 1.9; }
.hero-actions { margin-bottom: 48px; }
.hero-submit { min-height: 48px; padding-inline: 23px; }
.text-link { color: #625d77; font-size: 12px; }
.text-link span { display: inline-block; margin-left: 8px; color: #947cc3; font-size: 16px; transition: transform .2s ease; }
.text-link:hover span { transform: translateY(3px); }
.hero-meta { display: flex; flex-wrap: wrap; gap: 26px; }
.hero-meta span { display: grid; gap: 4px; padding-right: 24px; border-right: 1px solid var(--line); }
.hero-meta span:last-child { padding-right: 0; border-right: 0; }
.hero-meta b { color: #46425e; font-size: 11px; font-weight: 600; }
.hero-meta small { color: #a09bad; font-size: 9px; }
.hero-art { position: relative; min-height: 500px; }
.glass-sculpture { position: absolute; inset: 0; width: 100%; height: 100%; overflow: visible; }
.art-orbit { position: absolute; left: 50%; top: 52%; border: 1px solid rgba(132, 109, 189, .2); border-radius: 50%; transform: translate(-50%, -50%) rotate(-19deg); }
.orbit-one { width: 75%; height: 46%; }
.orbit-two { width: 85%; height: 61%; transform: translate(-50%, -50%) rotate(61deg); border-color: rgba(244, 163, 164, .2); }
.hero-art-caption { position: absolute; right: 6%; bottom: 5%; left: 6%; display: flex; justify-content: space-between; color: #aaa2bb; font-size: 9px; letter-spacing: .16em; }
.floating-status { position: absolute; right: 0; bottom: 20%; display: flex; align-items: center; gap: 12px; min-width: 187px; padding: 13px 14px; border: 1px solid rgba(255, 255, 255, .8); border-radius: 16px; background: rgba(255, 255, 255, .54); box-shadow: 0 16px 34px rgba(76, 55, 120, .12); backdrop-filter: blur(16px); }
.status-pulse { width: 8px; height: 8px; border-radius: 50%; background: #79c9a7; box-shadow: 0 0 0 5px rgba(121, 201, 167, .13); }
.floating-status small, .floating-status strong { display: block; }
.floating-status small { color: #9c95ad; font-size: 9px; }
.floating-status strong { margin-top: 4px; color: #4a426c; font-size: 12px; font-weight: 600; }
.status-arrow { margin-left: auto; color: #8d73c3; font-size: 18px; }
.info-strip { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; padding: 25px 28px; border: 1px solid rgba(255, 255, 255, .76); border-radius: 18px; background: rgba(255, 255, 255, .45); box-shadow: 0 12px 26px rgba(76, 55, 120, .05); backdrop-filter: blur(16px); }
.info-strip > div { display: grid; gap: 7px; padding-left: 17px; border-left: 2px solid rgba(139, 103, 189, .3); }
.strip-label { color: #a39bb3; font-size: 8px; letter-spacing: .15em; }
.info-strip strong { color: #504a68; font-size: 12px; font-weight: 600; }
.content-section { padding-top: 142px; }
.split-heading { display: grid; grid-template-columns: minmax(0, .94fr) minmax(240px, .7fr); gap: 80px; align-items: end; }
.section-heading h2, .file-heading h2, .contact-section h2 { font-size: clamp(39px, 5vw, 68px); line-height: 1.02; }
.section-summary { max-width: 330px; margin: 0 0 4px; color: #7b7789; font-size: 13px; line-height: 1.9; }
.track-tabs { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; margin-top: 56px; }
.track-tab { display: grid; grid-template-columns: 44px 1fr auto; gap: 14px; align-items: center; padding: 16px; border: 1px solid rgba(109, 88, 170, .13); border-radius: 16px; text-align: left; background: rgba(255,255,255,.3); color: var(--ink); transition: background .2s ease, border .2s ease, transform .2s ease; }
.track-tab:hover, .track-tab.active { border-color: rgba(107, 82, 183, .3); background: rgba(255,255,255,.7); transform: translateY(-2px); }
.tab-mark { display: grid; place-items: center; width: 44px; height: 44px; border-radius: 13px; color: #fff; background: linear-gradient(140deg, #719ce4, #7964bc); font-size: 20px; }
.track-tab:nth-child(2) .tab-mark { background: linear-gradient(140deg, #ec9aa1, #bd78b0); }
.track-tab small, .track-tab strong { display: block; }
.track-tab small { color: #a096b1; font-size: 9px; letter-spacing: .08em; }
.track-tab strong { margin-top: 6px; color: #4c4763; font-size: 14px; font-weight: 600; }
.tab-arrow { color: #8a74be; font-size: 18px; }
.track-panel { display: grid; grid-template-columns: minmax(210px, .58fr) 1.42fr; gap: 30px; margin-top: 18px; padding: 30px; border-radius: 18px; background: rgba(255, 255, 255, .45); box-shadow: 0 19px 35px rgba(81, 60, 123, .06); }
.track-intro { padding: 8px 15px 8px 4px; }
.track-english { color: #9279c1; font-size: 10px; letter-spacing: .16em; }
.track-intro p { color: #716d82; font-size: 13px; line-height: 1.85; }
.track-limits { display: grid; gap: 12px; margin-top: 28px; }
.track-limits span { display: grid; gap: 4px; }
.track-limits b { color: #4f486b; font-size: 12px; font-weight: 600; }
.track-limits small { color: #aaa2b1; font-size: 10px; }
.direction-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 11px; }
.direction-card { position: relative; min-height: 144px; padding: 17px; border: 1px solid rgba(108, 88, 163, .11); border-radius: 13px; background: rgba(250, 248, 255, .55); }
.direction-card:nth-child(3n+2) { background: rgba(255, 244, 244, .58); }
.direction-card:nth-child(3n) { background: rgba(244, 244, 255, .62); }
.direction-code { color: #a095b6; font-size: 9px; letter-spacing: .1em; }
.direction-card h3 { margin: 18px 0 8px; color: #51496c; font-size: 14px; font-weight: 600; }
.direction-card p { margin: 0; color: #898498; font-size: 11px; line-height: 1.6; }
.card-plus { position: absolute; right: 14px; bottom: 11px; color: #a58bc9; font-size: 19px; }
.art-tech-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 24px; margin-top: 31px; padding-top: 23px; border-top: 1px solid var(--line); }
.art-tech-row article { display: grid; grid-template-columns: 32px 1fr; gap: 11px; }
.art-tech-row article > span { color: #a58ac9; font-size: 10px; }
.art-tech-row strong { color: #5a5270; font-size: 12px; }
.art-tech-row p { margin: 7px 0 0; color: #8f899a; font-size: 11px; line-height: 1.55; }
.path-section { padding-bottom: 20px; }
.steps-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px; margin-top: 57px; }
.step-card { position: relative; min-height: 225px; padding: 23px 20px; border-radius: 15px; background: linear-gradient(140deg, rgba(255,255,255,.7), rgba(240, 235, 250, .6)); }
.step-card:nth-child(2) { background: linear-gradient(140deg, rgba(255,255,255,.7), rgba(255, 237, 238, .63)); }
.step-card:nth-child(3) { background: linear-gradient(140deg, rgba(255,255,255,.7), rgba(231, 239, 255, .7)); }
.step-card:nth-child(4) { background: linear-gradient(140deg, rgba(255,255,255,.7), rgba(255, 244, 213, .68)); }
.step-index { color: #a39ab3; font-size: 10px; letter-spacing: .1em; }
.step-mark { display: grid; place-items: center; width: 39px; height: 39px; margin: 26px 0 19px; border-radius: 12px; color: #fff; background: #8b74c4; font-size: 15px; }
.step-card:nth-child(2) .step-mark { background: #d7839d; }.step-card:nth-child(3) .step-mark { background: #678bcf; }.step-card:nth-child(4) .step-mark { background: #dbac55; }
.step-card h3 { margin: 0 0 10px; color: #514a66; font-size: 14px; font-weight: 600; }.step-card p { max-width: 210px; margin: 0; color: #888294; font-size: 11px; line-height: 1.7; }.step-line { position: absolute; right: 20px; bottom: 20px; left: 20px; height: 1px; background: rgba(96, 77, 150, .13); }
.file-layout { display: grid; grid-template-columns: .66fr 1.34fr; gap: 72px; align-items: start; margin-top: 111px; padding: 47px 0 13px; border-top: 1px solid var(--line); }
.file-heading h2 { font-size: clamp(35px, 4vw, 53px); }.file-grid { display: grid; gap: 12px; }.file-card { display: grid; grid-template-columns: 35px 1fr; gap: 18px; align-items: start; padding: 17px 0; border-bottom: 1px solid var(--line); }.file-card > span { color: #a38bc7; font-size: 10px; }.file-card strong, .file-card b, .file-card p { display: block; }.file-card strong { color: #726b81; font-size: 12px; font-weight: 600; }.file-card b { margin-top: 8px; color: #4b4564; font-size: 22px; font-weight: 500; letter-spacing: -.04em; }.file-card p { margin: 8px 0 0; color: #96909e; font-size: 11px; }
.timeline-section { padding-bottom: 11px; }.timeline-list { margin-top: 62px; border-top: 1px solid var(--line); }.timeline-item { display: grid; grid-template-columns: 20px 48px 145px 1fr; gap: 17px; align-items: center; min-height: 103px; border-bottom: 1px solid var(--line); }.timeline-dot { width: 8px; height: 8px; border: 2px solid #9b83c4; border-radius: 50%; background: #faf7f4; box-shadow: 0 0 0 4px rgba(155, 131, 196, .1); }.timeline-item:nth-child(2) .timeline-dot { border-color: #e394a0; }.timeline-item:nth-child(3) .timeline-dot { border-color: #6c97d7; }.timeline-item:nth-child(4) .timeline-dot { border-color: #d5a34a; }.timeline-item:nth-child(5) .timeline-dot { border-color: #83bb9a; }.timeline-index { color: #a69daf; font-size: 10px; }.timeline-item time { display: grid; gap: 3px; }.timeline-item time b { color: #504967; font-size: 18px; font-weight: 500; }.timeline-item time small { color: #a19aaa; font-size: 10px; }.timeline-item h3 { margin: 0 0 6px; color: #514a66; font-size: 14px; font-weight: 600; }.timeline-item p { margin: 0; color: #8e8999; font-size: 11px; }
.notice-section { padding-bottom: 146px; }.notice-header { justify-content: space-between; align-items: end; }.notice-header h2 { margin-bottom: 0; }.notice-card { display: grid; grid-template-columns: 120px 1fr 30px; gap: 24px; align-items: center; margin-top: 57px; padding: 28px; border: 1px solid rgba(106, 86, 162, .13); border-radius: 17px; background: rgba(255,255,255,.58); }.notice-date { color: #9279bd; font-size: 13px; letter-spacing: .05em; }.notice-tag { color: #a397af; font-size: 9px; letter-spacing: .12em; }.notice-card h3 { margin: 12px 0 8px; color: #4d4764; font-size: 15px; font-weight: 600; }.notice-card p { max-width: 710px; margin: 0; color: #888294; font-size: 12px; line-height: 1.7; }.notice-card-arrow { color: #8c72be; font-size: 22px; }.download-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-top: 14px; }.download-card { display: grid; grid-template-columns: 27px 1fr auto; gap: 8px; align-items: start; min-height: 106px; padding: 16px; border: 1px solid rgba(108, 88, 163, .1); border-radius: 13px; text-align: left; background: rgba(255,255,255,.42); transition: background .2s ease, transform .2s ease; }.download-card:hover { background: rgba(255,255,255,.75); transform: translateY(-2px); }.download-card > span { color: #a491bd; font-size: 10px; }.download-card strong, .download-card small { display: block; }.download-card strong { color: #5b536d; font-size: 12px; font-weight: 600; }.download-card small { margin-top: 9px; color: #9c96a2; font-size: 10px; line-height: 1.5; }.download-card b { color: #9c82c2; font-size: 17px; font-weight: 400; }
.contact-section { display: grid; grid-template-columns: .9fr 1.1fr; gap: 100px; padding-bottom: 112px; }.contact-section h2 { font-size: clamp(42px, 5vw, 67px); }.contact-section h2 em { color: #8c6bc3; font-style: normal; }.contact-details { display: grid; grid-template-columns: 1fr .85fr; gap: 55px; align-items: start; padding-top: 17px; }.contact-list p { display: grid; grid-template-columns: 90px 1fr; gap: 15px; margin: 0; padding: 13px 0; border-bottom: 1px solid var(--line); }.contact-list span, .contractors > span { color: #aaa2ae; font-size: 10px; }.contact-list strong { color: #635b73; font-size: 11px; font-weight: 500; }.contractors { padding-left: 24px; border-left: 1px solid var(--line); }.contractors p { margin: 13px 0; color: #5c5571; font-size: 12px; }
.dark-footer { color: rgba(255,255,255,.72); background: #26243b; }.footer-inner { display: grid; grid-template-columns: 1fr auto; gap: 30px; padding-top: 48px; padding-bottom: 39px; }.footer-brand { color: #fff; font-size: 18px; letter-spacing: .14em; }.footer-inner p { margin: 8px 0 0; color: rgba(255,255,255,.45); font-size: 11px; }.footer-links { display: flex; align-items: center; gap: 23px; }.footer-links a, .footer-links button { padding: 0; border: 0; color: rgba(255,255,255,.6); background: transparent; font-size: 11px; }.footer-inner > small { grid-column: 1 / -1; padding-top: 17px; border-top: 1px solid rgba(255,255,255,.12); color: rgba(255,255,255,.34); font-size: 9px; }
:focus-visible { outline: 3px solid rgba(115, 86, 205, .72); outline-offset: 4px; }

@media (max-width: 1100px) { .desktop-nav { gap: 18px; }.hero { grid-template-columns: .9fr 1.1fr; gap: 22px; }.hero-art { min-height: 430px; }.direction-grid { grid-template-columns: repeat(2, 1fr); }.contact-section { gap: 55px; } }
@media (max-width: 820px) { .section-wrap { width: min(100% - 40px, 620px); }.glass-nav { padding: 12px 20px; }.desktop-nav { display: none; }.hero { grid-template-columns: 1fr; min-height: 0; padding-top: 62px; }.hero-copy { max-width: 610px; }.hero-art { min-height: 430px; margin-top: -6px; }.info-strip { grid-template-columns: repeat(2, 1fr); }.content-section { padding-top: 94px; }.split-heading, .file-layout, .contact-section { grid-template-columns: 1fr; gap: 30px; }.section-summary { max-width: 480px; }.steps-grid { grid-template-columns: repeat(2, 1fr); }.file-layout { margin-top: 76px; }.contact-section { gap: 40px; }.notice-section { padding-bottom: 95px; } }
@media (max-width: 560px) { .section-wrap { width: min(100% - 28px, 480px); }.glass-nav { min-height: 64px; padding: 9px 14px; }.brand-lockup strong { font-size: 13px; }.brand-lockup small { font-size: 7px; }.nav-actions { gap: 8px; }.quiet-button { display: none; }.nav-login { min-height: 36px; padding: 0 13px; font-size: 10px; }.hero { padding-top: 48px; }.hero h1 { font-size: clamp(48px, 15vw, 75px); }.hero-summary { margin-top: 22px; font-size: 13px; }.hero-actions { margin-bottom: 36px; }.hero-meta { gap: 13px; }.hero-meta span { padding-right: 13px; }.hero-meta b { font-size: 10px; }.hero-art { min-height: 330px; margin-top: 25px; }.floating-status { right: 0; bottom: 9%; min-width: 158px; padding: 10px; }.floating-status strong { font-size: 10px; }.info-strip { gap: 0; padding: 17px 15px; }.info-strip > div { margin: 7px 0; padding-left: 10px; }.info-strip strong { font-size: 10px; }.track-tabs { grid-template-columns: 1fr; margin-top: 36px; }.track-panel { grid-template-columns: 1fr; padding: 20px 15px; }.track-intro { padding: 0; }.direction-grid { grid-template-columns: 1fr 1fr; }.direction-card { min-height: 136px; padding: 13px; }.direction-card h3 { margin-top: 13px; font-size: 12px; }.direction-card p { font-size: 10px; }.art-tech-row { grid-template-columns: 1fr; gap: 16px; }.steps-grid { grid-template-columns: 1fr; gap: 10px; }.step-card { min-height: 190px; }.file-layout { padding-top: 35px; }.timeline-item { grid-template-columns: 14px 32px 78px 1fr; gap: 8px; min-height: 120px; }.timeline-item time b { font-size: 15px; }.timeline-item h3 { font-size: 12px; }.timeline-item p { font-size: 10px; line-height: 1.5; }.notice-header { align-items: start; flex-direction: column; gap: 22px; }.notice-card { grid-template-columns: 1fr 22px; gap: 12px; padding: 19px; }.notice-date { grid-column: 1 / -1; }.notice-card h3 { font-size: 13px; line-height: 1.55; }.notice-card p { font-size: 11px; }.download-grid { grid-template-columns: 1fr 1fr; }.download-card { min-height: 118px; padding: 13px; }.download-card strong { font-size: 11px; }.contact-details { grid-template-columns: 1fr; gap: 28px; }.contractors { padding: 20px 0 0; border-top: 1px solid var(--line); border-left: 0; }.footer-inner { grid-template-columns: 1fr; padding-top: 35px; }.footer-links { flex-wrap: wrap; gap: 13px 20px; }.footer-inner > small { grid-column: 1; line-height: 1.5; } }
@media (prefers-reduced-motion: reduce) { :global(html) { scroll-behavior: auto; } *, *::before, *::after { animation-duration: .01ms !important; animation-iteration-count: 1 !important; transition-duration: .01ms !important; } }
</style>
