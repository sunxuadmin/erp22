<script setup lang="ts">
import { computed, ref } from "vue";
import type {
  CompetitionHomeAction,
  CompetitionHomeContent,
  CompetitionHomeVersion,
  CompetitionTrack
} from "../../config/competitionHome";

const props = defineProps<{ content: CompetitionHomeContent }>();

const emit = defineEmits<{
  action: [action: CompetitionHomeAction];
  "change-version": [version: CompetitionHomeVersion];
}>();

const activeTrack = ref<CompetitionTrack["id"]>(props.content.tracks[0]?.id ?? "track-a");

const selectedTrack = computed(
  () => props.content.tracks.find((track) => track.id === activeTrack.value) ?? props.content.tracks[0]
);

const navItems = [
  { label: "首页", href: "#competition-top" },
  { label: "赛道与方向", href: "#competition-tracks" },
  { label: "报送路径", href: "#competition-path" },
  { label: "时间节点", href: "#competition-timeline" },
  { label: "通知下载", href: "#competition-notice" }
];

function emitAction(id: CompetitionHomeAction["id"], fallbackLabel: string) {
  const configured = props.content.downloads.find((item) => item.actionId === id);
  emit("action", { id, label: configured?.title ?? fallbackLabel });
}

function switchTrack(track: CompetitionTrack["id"]) {
  activeTrack.value = track;
}

function focusNextTrack(event: KeyboardEvent, index: number) {
  if (!['ArrowRight', 'ArrowDown', 'ArrowLeft', 'ArrowUp'].includes(event.key)) return;
  event.preventDefault();
  const nextIndex = event.key === "ArrowRight" || event.key === "ArrowDown" ? index + 1 : index - 1;
  const next = props.content.tracks[(nextIndex + props.content.tracks.length) % props.content.tracks.length];
  if (next) {
    switchTrack(next.id);
    document.getElementById(`track-tab-${next.id}`)?.focus();
  }
}
</script>

<template>
  <div class="art-tech-page">
    <header id="competition-top" class="art-tech-header">
      <a class="art-tech-brand" href="#competition-top" aria-label="返回创意河南首页">
        <span class="art-tech-brand__mark" aria-hidden="true">创</span>
        <span>
          <strong>{{ content.brand }}</strong>
          <small>{{ content.variants.v2.brandEn }}</small>
        </span>
      </a>

      <nav class="art-tech-nav" aria-label="页面导航">
        <a v-for="item in navItems" :key="item.href" :href="item.href">{{ item.label }}</a>
      </nav>

      <div class="art-tech-header__actions">
        <button type="button" class="art-tech-button art-tech-button--quiet" @click="emitAction('login', '统一登录入口')">
          登录
        </button>
        <button type="button" class="art-tech-button art-tech-button--primary" @click="emitAction('submit', '作品报送入口')">
          开始报送 <span aria-hidden="true">↗</span>
        </button>
        <button
          type="button"
          class="art-tech-version"
          aria-label="切换至液态玻璃版"
          @click="emit('change-version', 'v1')"
        >
          <span class="art-tech-version__dot" aria-hidden="true" /> V1
        </button>
      </div>
    </header>

    <main>
      <section class="art-tech-hero" aria-labelledby="competition-hero-title">
        <div class="art-tech-hero__copy">
          <div class="art-tech-kicker"><span>CH / 2026</span><i aria-hidden="true" /> {{ content.variants.v2.eyebrow }}</div>
          <h1 id="competition-hero-title">
            <span>{{ content.variants.v2.heroLines[0] }}</span>
            <span class="art-tech-gradient-text">{{ content.variants.v2.heroLines[1] }}</span>
            <span v-if="content.variants.v2.heroLines[2]">{{ content.variants.v2.heroLines[2] }}</span>
          </h1>
          <p class="art-tech-hero__summary">{{ content.variants.v2.heroSummary }}</p>
          <div class="art-tech-hero__buttons">
            <button type="button" class="art-tech-button art-tech-button--primary art-tech-button--large" @click="emitAction('submit', '作品报送入口')">
              <span class="art-tech-button__icon" aria-hidden="true">↗</span> 进入作品报送
            </button>
            <a class="art-tech-text-link" href="#competition-tracks">了解赛事 <span aria-hidden="true">↓</span></a>
          </div>
          <div class="art-tech-hero__meta" aria-label="赛事基本信息">
            <span><b>{{ content.edition }}</b>{{ content.groups }}</span>
            <span><b>{{ content.fee }}</b>公共艺术设计赛事</span>
            <span><b>{{ content.uploadDeadline }}</b>作品上传截止</span>
          </div>
        </div>

        <div class="art-tech-orbit" aria-label="艺术与科技视觉装置" role="img">
          <div class="art-tech-orbit__grid" aria-hidden="true" />
          <div class="art-tech-orbit__halo" aria-hidden="true" />
          <svg class="art-tech-orbit__ribbon" viewBox="0 0 620 540" aria-hidden="true">
            <defs>
              <linearGradient id="art-tech-ribbon-fill" x1="0" x2="1" y1="0" y2="1">
                <stop offset="0" stop-color="#5a7cff" stop-opacity=".82" />
                <stop offset=".34" stop-color="#9d8bff" stop-opacity=".58" />
                <stop offset=".65" stop-color="#70e5d2" stop-opacity=".62" />
                <stop offset="1" stop-color="#d7a6ff" stop-opacity=".72" />
              </linearGradient>
              <linearGradient id="art-tech-ribbon-edge" x1="0" x2="1">
                <stop stop-color="#effcff" />
                <stop offset=".5" stop-color="#9aa8ff" />
                <stop offset="1" stop-color="#ffffff" />
              </linearGradient>
              <filter id="art-tech-glow" x="-40%" y="-40%" width="180%" height="180%">
                <feGaussianBlur stdDeviation="8" result="blur" />
                <feMerge><feMergeNode in="blur" /><feMergeNode in="SourceGraphic" /></feMerge>
              </filter>
            </defs>
            <path d="M78 355C160 82 451 63 523 192c55 99-81 188-194 179-119-10-221-102-152-214 55-89 219-123 315-65" fill="none" stroke="url(#art-tech-ribbon-fill)" stroke-width="72" stroke-linecap="round" filter="url(#art-tech-glow)" />
            <path d="M78 355C160 82 451 63 523 192c55 99-81 188-194 179-119-10-221-102-152-214 55-89 219-123 315-65" fill="none" stroke="url(#art-tech-ribbon-edge)" stroke-opacity=".72" stroke-width="1.6" stroke-dasharray="4 10" />
            <path d="M119 388C252 272 309 154 502 113" fill="none" stroke="#ffffff" stroke-opacity=".68" stroke-width="2" stroke-dasharray="2 14" />
          </svg>
          <div class="art-tech-orbit__sculpture" aria-hidden="true">
            <span /><span /><span /><span /><span />
          </div>
          <div class="art-tech-orbit__readout art-tech-orbit__readout--top"><b>ART / TECH</b><small>01—03</small></div>
          <div class="art-tech-orbit__readout art-tech-orbit__readout--bottom"><b>系统状态</b><small>{{ content.status }}</small></div>
          <span class="art-tech-orbit__particle art-tech-orbit__particle--one" aria-hidden="true" />
          <span class="art-tech-orbit__particle art-tech-orbit__particle--two" aria-hidden="true" />
          <span class="art-tech-orbit__particle art-tech-orbit__particle--three" aria-hidden="true" />
        </div>
      </section>

      <aside class="art-tech-info-strip" aria-label="赛事信息摘要">
        <span><b>01</b> 创意河南</span><span><b>02</b> 艺术赋美乡村</span><span><b>03</b> 12 个创作方向</span><span><b>04</b> <em>{{ content.status }}</em></span>
      </aside>

      <section id="competition-tracks" class="art-tech-section art-tech-tracks" aria-labelledby="track-title">
        <div class="art-tech-section__heading">
          <div><div class="art-tech-kicker"><span>CH / 01</span><i aria-hidden="true" /> {{ content.variants.v2.trackEyebrow }}</div><h2 id="track-title">{{ content.variants.v2.trackTitle[0] }}<br /><strong>{{ content.variants.v2.trackTitle[1] }}</strong></h2></div>
          <p>{{ content.variants.v2.trackSummary }}</p>
        </div>
        <div class="art-tech-track-tabs" role="tablist" aria-label="赛事赛道切换">
          <button
            v-for="(track, index) in content.tracks"
            :id="`track-tab-${track.id}`"
            :key="track.id"
            type="button"
            role="tab"
            :aria-selected="activeTrack === track.id"
            :aria-controls="activeTrack === track.id ? `track-panel-${track.id}` : undefined"
            :tabindex="activeTrack === track.id ? 0 : -1"
            :class="{ 'is-active': activeTrack === track.id }"
            @click="switchTrack(track.id)"
            @keydown="focusNextTrack($event, index)"
          >
            <span class="art-tech-track-tabs__mark">{{ track.shortMark }}</span><span><small>{{ track.badge }}</small><b>{{ track.title }}</b></span><span class="art-tech-track-tabs__arrow" aria-hidden="true">↗</span>
          </button>
        </div>
        <div v-if="selectedTrack" :id="`track-panel-${selectedTrack.id}`" class="art-tech-track-panel" role="tabpanel" :aria-labelledby="`track-tab-${selectedTrack.id}`">
          <div class="art-tech-track-panel__intro"><span>{{ selectedTrack.english }}</span><p>{{ selectedTrack.description }}</p><div><b>{{ selectedTrack.perSchoolLimit }}</b><b>{{ selectedTrack.authorLimit }}</b></div></div>
          <div class="art-tech-directions"><article v-for="direction in selectedTrack.directions" :key="direction.code"><span>{{ direction.code }}</span><h3>{{ direction.title }}</h3><p>{{ direction.description }}</p></article></div>
        </div>
      </section>

      <section class="art-tech-section art-tech-features" aria-labelledby="feature-title">
        <div class="art-tech-section__heading art-tech-section__heading--compact"><div><div class="art-tech-kicker"><span>CH / 01.5</span><i aria-hidden="true" /> ART × TECH</div><h2 id="feature-title">艺术科技，<br /><strong>不是一个标签。</strong></h2></div><p>让感知、算法与材料互相启发，作品在屏幕之外继续生长。</p></div>
        <div class="art-tech-feature-grid"><article v-for="feature in content.artTechFeatures" :key="feature.index"><span>{{ feature.index }}</span><div class="art-tech-feature-grid__orb" aria-hidden="true" /><h3>{{ feature.title }}</h3><p>{{ feature.description }}</p></article></div>
      </section>

      <section id="competition-path" class="art-tech-section art-tech-path" aria-labelledby="path-title">
        <div class="art-tech-section__heading"><div><div class="art-tech-kicker"><span>CH / 02</span><i aria-hidden="true" /> {{ content.variants.v2.journeyEyebrow }}</div><h2 id="path-title">{{ content.variants.v2.journeyTitle[0] }}<br /><strong>{{ content.variants.v2.journeyTitle[1] }}</strong></h2></div><p>从学校联络到省级提交，每一步都清晰可追踪。先准备，再提交，让好作品拥有完整的到达路径。</p></div>
        <div class="art-tech-step-grid"><article v-for="step in content.steps" :key="step.index"><span class="art-tech-step-grid__index">{{ step.index }}</span><span class="art-tech-step-grid__mark">{{ step.mark }}</span><h3>{{ step.title }}</h3><p>{{ step.description }}</p><i aria-hidden="true">↗</i></article></div>
      </section>

      <section class="art-tech-section art-tech-files" aria-labelledby="file-title">
        <div class="art-tech-section__heading art-tech-section__heading--compact"><div><div class="art-tech-kicker"><span>CH / 02.5</span><i aria-hidden="true" /> FILE SYSTEM</div><h2 id="file-title">{{ content.variants.v2.fileTitle[0] }}<br /><strong>{{ content.variants.v2.fileTitle[1] }}</strong></h2></div><p>上传前请留出校验时间，规范的文件会让评审从第一眼就更接近作品本身。</p></div>
        <div class="art-tech-file-grid"><article v-for="file in content.fileSpecs" :key="file.index"><span>{{ file.index }}</span><b>{{ file.label }}</b><strong>{{ file.value }}</strong><p>{{ file.description }}</p></article></div>
      </section>

      <section id="competition-timeline" class="art-tech-section art-tech-timeline" aria-labelledby="timeline-title">
        <div class="art-tech-section__heading"><div><div class="art-tech-kicker"><span>CH / 03</span><i aria-hidden="true" /> {{ content.variants.v2.timelineEyebrow }}</div><h2 id="timeline-title">{{ content.variants.v2.timelineTitle[0] }}<br /><strong>{{ content.variants.v2.timelineTitle[1] }}</strong></h2></div><p>时间以正式通知和平台实际开放状态为准，请以学校联络员和平台公告的最新信息完成安排。</p></div>
        <div class="art-tech-milestones"><article v-for="(milestone, index) in content.milestones" :key="milestone.index" :class="{ 'is-current': index === 0 }"><span class="art-tech-milestones__dot" aria-hidden="true" /><small>{{ milestone.year }} / {{ milestone.index }}</small><strong>{{ milestone.date }}</strong><h3>{{ milestone.title }}</h3><p>{{ milestone.description }}</p></article></div>
      </section>

      <section id="competition-notice" class="art-tech-section art-tech-notice" aria-labelledby="notice-title">
        <slot name="cms" />
        <div class="art-tech-notice__top"><div><div class="art-tech-kicker"><span>CH / 04</span><i aria-hidden="true" /> {{ content.variants.v2.noticeEyebrow }}</div><h2 id="notice-title">{{ content.variants.v2.noticeTitle[0] }}<br /><strong>{{ content.variants.v2.noticeTitle[1] }}</strong></h2></div><button type="button" class="art-tech-button art-tech-button--outline" @click="emitAction('official-notice', '查看官方通知')">查看官方通知 <span aria-hidden="true">↗</span></button></div>
        <article class="art-tech-notice__feature"><span>{{ content.notice.date }}</span><div><h3>{{ content.notice.title }}</h3><p>{{ content.notice.summary }}</p></div><span class="art-tech-notice__feature-arrow" aria-hidden="true">↗</span></article>
        <div class="art-tech-downloads"><button v-for="item in content.downloads" :key="item.actionId" type="button" @click="emitAction(item.actionId, item.title)"><span>{{ item.index }}</span><b>{{ item.title }}</b><small>{{ item.description }}</small><i aria-hidden="true">↓</i></button></div>
      </section>

      <section class="art-tech-contact" aria-labelledby="contact-title"><div><div class="art-tech-kicker"><span>CH / 05</span><i aria-hidden="true" /> CONTACT</div><h2 id="contact-title">有人回答问题，<br /><strong>也有人等待作品。</strong></h2></div><div class="art-tech-contact__details"><div v-for="contact in content.contacts" :key="contact.label"><small>{{ contact.label }}</small><b>{{ contact.value }}</b></div><div><small>承办单位</small><b>{{ content.contractors.join(" · ") }}</b></div></div></section>
    </main>

    <footer class="art-tech-footer"><div><span class="art-tech-brand__mark" aria-hidden="true">创</span><strong>{{ content.brand }}</strong><small>{{ content.variants.v2.brandEn }} · {{ content.edition }}</small></div><p>{{ content.disclaimer }}</p><div class="art-tech-footer__actions"><button type="button" class="art-tech-text-link" @click="emitAction('platform-status', '查看平台状态')">平台状态 <span aria-hidden="true">↗</span></button><button type="button" class="art-tech-text-link art-tech-footer__version" aria-label="切换液态玻璃版" @click="emit('change-version', 'v1')">切换液态玻璃版 <span aria-hidden="true">↗</span></button></div></footer>
  </div>
</template>

<style scoped>
:global(*) { box-sizing: border-box; }
:global(html) { scroll-behavior: smooth; }
:global(body) { margin: 0; background: #f6f8ff; }
.art-tech-page { --ink: #202740; --muted: #68738e; --line: rgba(82, 104, 160, .18); --blue: #536dff; --purple: #a28bf2; --mint: #62d9ce; --paper: #f7f9ff; min-height: 100vh; overflow: hidden; color: var(--ink); background: radial-gradient(circle at 10% 0%, #fff 0 16%, transparent 40%), linear-gradient(135deg, #f7fbff 0%, #eef2ff 49%, #f9f3ff 100%); font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; }
.art-tech-page::before { position: fixed; z-index: 0; inset: 0; pointer-events: none; opacity: .24; content: ""; background-image: linear-gradient(rgba(92, 110, 168, .08) 1px, transparent 1px), linear-gradient(90deg, rgba(92, 110, 168, .08) 1px, transparent 1px); background-size: 48px 48px; mask-image: linear-gradient(to bottom, #000, transparent 72%); }
.art-tech-header, main, .art-tech-footer { position: relative; z-index: 1; width: min(1240px, calc(100% - 48px)); margin: 0 auto; }
.art-tech-header { display: flex; align-items: center; justify-content: space-between; min-height: 78px; border-bottom: 1px solid var(--line); }
.art-tech-brand { display: inline-flex; align-items: center; gap: 10px; color: var(--ink); text-decoration: none; }
.art-tech-brand__mark { display: grid; width: 34px; height: 34px; place-items: center; border: 1px solid rgba(83, 109, 255, .35); border-radius: 10px 4px 10px 4px; color: #fff; background: linear-gradient(135deg, #506eff, #af8bf1 60%, #72dbd0); box-shadow: 0 8px 24px rgba(83, 109, 255, .2); font-weight: 800; }
.art-tech-brand strong, .art-tech-brand small { display: block; line-height: 1.05; }
.art-tech-brand strong { font-size: 15px; letter-spacing: .04em; }
.art-tech-brand small { margin-top: 4px; color: var(--muted); font-size: 8px; letter-spacing: .2em; }
.art-tech-nav { display: flex; gap: clamp(14px, 2.4vw, 34px); margin-left: auto; margin-right: 34px; }
.art-tech-nav a, .art-tech-text-link { color: #5e6884; font-size: 12px; text-decoration: none; transition: color .2s ease; }
.art-tech-nav a:hover, .art-tech-nav a:focus-visible, .art-tech-text-link:hover, .art-tech-text-link:focus-visible { color: var(--blue); }
.art-tech-header__actions { display: flex; align-items: center; gap: 9px; }
.art-tech-button, .art-tech-version { border: 1px solid transparent; border-radius: 999px; cursor: pointer; font: inherit; transition: transform .2s ease, box-shadow .2s ease, background .2s ease; }
.art-tech-button { padding: 10px 17px; font-size: 11px; font-weight: 700; }
.art-tech-button:hover, .art-tech-version:hover { transform: translateY(-2px); }
.art-tech-button--quiet { color: var(--ink); background: rgba(255,255,255,.54); border-color: var(--line); }
.art-tech-button--primary { color: #fff; background: linear-gradient(110deg, #4969f4, #8b78ec); box-shadow: 0 12px 25px rgba(83, 109, 255, .22); }
.art-tech-button--large { padding: 15px 23px; }
.art-tech-button--outline { color: var(--blue); background: transparent; border-color: rgba(83, 109, 255, .42); }
.art-tech-button__icon { display: inline-grid; width: 20px; height: 20px; margin-right: 5px; place-items: center; border-radius: 50%; background: rgba(255,255,255,.2); }
.art-tech-version { display: inline-flex; align-items: center; gap: 7px; padding: 8px 11px; color: #6b7290; background: rgba(255,255,255,.45); border-color: var(--line); font-size: 10px; font-weight: 800; }
.art-tech-version__dot { width: 7px; height: 7px; border-radius: 50%; background: var(--mint); box-shadow: 0 0 0 4px rgba(98, 217, 206, .16); }
.art-tech-hero { display: grid; grid-template-columns: minmax(0, .94fr) minmax(420px, 1.06fr); align-items: center; min-height: 620px; gap: clamp(25px, 5vw, 80px); padding: 56px 0 38px; }
.art-tech-kicker { display: flex; align-items: center; gap: 10px; color: #68749b; font-size: 10px; font-weight: 800; letter-spacing: .14em; text-transform: uppercase; }
.art-tech-kicker span { color: var(--blue); }
.art-tech-kicker i { display: inline-block; width: 26px; height: 1px; background: #9a9be2; }
.art-tech-hero h1, .art-tech-section h2, .art-tech-contact h2 { max-width: 680px; margin: 19px 0 18px; font-size: clamp(44px, 6vw, 82px); letter-spacing: -.075em; line-height: .99; }
.art-tech-hero h1 span, .art-tech-section h2 strong, .art-tech-contact h2 strong { display: block; font-weight: 400; }
.art-tech-hero h1 { font-weight: 800; }
.art-tech-gradient-text, .art-tech-section h2 strong, .art-tech-contact h2 strong { background: linear-gradient(100deg, #576fff, #b085e8 52%, #46cfc5); -webkit-background-clip: text; background-clip: text; color: transparent; }
.art-tech-hero__summary { max-width: 440px; margin: 0; color: var(--muted); font-size: 15px; line-height: 1.9; }
.art-tech-hero__buttons { display: flex; align-items: center; gap: 25px; margin-top: 29px; }
.art-tech-text-link { display: inline-flex; align-items: center; gap: 10px; padding: 4px; border: 0; background: none; cursor: pointer; font-weight: 700; }
.art-tech-hero__meta { display: flex; flex-wrap: wrap; gap: 25px; margin-top: 48px; color: var(--muted); font-size: 10px; }
.art-tech-hero__meta span { display: flex; flex-direction: column; gap: 6px; }
.art-tech-hero__meta b { color: var(--ink); font-size: 11px; }
.art-tech-orbit { position: relative; min-height: 490px; border: 1px solid rgba(125, 139, 199, .23); border-radius: 42% 58% 44% 56% / 50% 38% 62% 50%; overflow: hidden; background: linear-gradient(140deg, rgba(255,255,255,.6), rgba(219, 224, 255, .44) 45%, rgba(248, 223, 255, .55)); box-shadow: 0 32px 80px rgba(89, 105, 175, .19), inset 0 0 55px rgba(255,255,255,.82); }
.art-tech-orbit::after { position: absolute; inset: 0; pointer-events: none; content: ""; background: linear-gradient(118deg, transparent 20%, rgba(255,255,255,.56) 42%, transparent 50%); transform: translateX(-90%); animation: art-tech-scan 9s ease-in-out infinite; }
.art-tech-orbit__grid { position: absolute; inset: 7%; opacity: .55; background-image: linear-gradient(rgba(87, 108, 188, .12) 1px, transparent 1px), linear-gradient(90deg, rgba(87, 108, 188, .12) 1px, transparent 1px); background-size: 34px 34px; mask-image: radial-gradient(ellipse, #000 18%, transparent 70%); }
.art-tech-orbit__halo { position: absolute; top: 16%; left: 19%; width: 58%; aspect-ratio: 1; border: 1px solid rgba(99, 113, 208, .3); border-radius: 50%; box-shadow: 0 0 0 28px rgba(255,255,255,.2), 0 0 0 56px rgba(140, 120, 234, .1); transform: rotate(-20deg) scaleX(1.34); }
.art-tech-orbit__ribbon { position: absolute; inset: 6% 2%; width: 96%; height: 88%; transform: rotate(-8deg); }
.art-tech-orbit__sculpture { position: absolute; top: 31%; left: 44%; width: 125px; height: 125px; transform: rotate(24deg); }
.art-tech-orbit__sculpture span { position: absolute; inset: 0; border: 1px solid rgba(81, 98, 195, .5); border-radius: 42% 58% 56% 44%; background: linear-gradient(135deg, rgba(255,255,255,.45), rgba(102, 117, 236, .15) 50%, rgba(108, 231, 207, .25)); box-shadow: inset -12px -12px 20px rgba(101, 84, 208, .16); transform: rotate(calc(var(--i, 0) * 19deg)) scale(calc(1 - var(--i, 0) * .075)); }
.art-tech-orbit__sculpture span:nth-child(2){--i:1}.art-tech-orbit__sculpture span:nth-child(3){--i:2}.art-tech-orbit__sculpture span:nth-child(4){--i:3}.art-tech-orbit__sculpture span:nth-child(5){--i:4}
.art-tech-orbit__readout { position: absolute; z-index: 2; display: grid; gap: 4px; padding: 10px 13px; border: 1px solid rgba(100, 116, 186, .2); border-radius: 12px; color: #536185; background: rgba(255,255,255,.46); backdrop-filter: blur(12px); font-size: 8px; letter-spacing: .12em; }
.art-tech-orbit__readout b { color: var(--blue); font-size: 9px; }.art-tech-orbit__readout small { font-size: 8px; }.art-tech-orbit__readout--top { top: 14%; right: 12%; }.art-tech-orbit__readout--bottom { bottom: 16%; left: 12%; }.art-tech-orbit__particle { position: absolute; width: 6px; height: 6px; border-radius: 50%; background: #706fff; box-shadow: 0 0 0 8px rgba(112,111,255,.1), 0 0 18px rgba(112,111,255,.7); animation: art-tech-float 5s ease-in-out infinite; }.art-tech-orbit__particle--one { top: 26%; left: 25%; }.art-tech-orbit__particle--two { right: 24%; bottom: 29%; width: 4px; height: 4px; animation-delay: -1.8s; }.art-tech-orbit__particle--three { top: 66%; left: 30%; width: 3px; height: 3px; animation-delay: -3.2s; }
.art-tech-info-strip { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; padding: 16px 0; border-top: 1px solid var(--line); border-bottom: 1px solid var(--line); color: var(--muted); font-size: 10px; letter-spacing: .04em; }.art-tech-info-strip span { display: flex; gap: 9px; align-items: center; }.art-tech-info-strip b { color: var(--blue); font-size: 9px; }.art-tech-info-strip em { color: #28a897; font-style: normal; }
.art-tech-section { padding: 118px 0 0; scroll-margin-top: 20px; }.art-tech-section__heading { display: grid; grid-template-columns: 1.06fr .75fr; align-items: end; gap: 70px; }.art-tech-section__heading h2, .art-tech-contact h2 { margin: 16px 0 0; font-size: clamp(38px, 5vw, 66px); }.art-tech-section__heading p { max-width: 390px; margin: 0 0 7px; color: var(--muted); font-size: 13px; line-height: 1.85; }.art-tech-section__heading--compact { grid-template-columns: 1fr .6fr; }
.art-tech-track-tabs { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; margin-top: 52px; }.art-tech-track-tabs button { display: grid; grid-template-columns: 54px 1fr 24px; align-items: center; gap: 15px; width: 100%; padding: 17px; border: 1px solid var(--line); border-radius: 18px; color: var(--muted); background: rgba(255,255,255,.43); cursor: pointer; text-align: left; transition: .25s ease; }.art-tech-track-tabs button:hover, .art-tech-track-tabs button:focus-visible, .art-tech-track-tabs button.is-active { border-color: rgba(83,109,255,.55); color: var(--ink); background: rgba(255,255,255,.76); box-shadow: 0 16px 30px rgba(89,105,175,.12); }.art-tech-track-tabs__mark { display: grid; width: 46px; height: 46px; place-items: center; border-radius: 14px; color: #fff; background: linear-gradient(135deg, #526df8, #b892ed); font-size: 20px; font-weight: 800; }.art-tech-track-tabs button:nth-child(2) .art-tech-track-tabs__mark { background: linear-gradient(135deg, #4cc9c1, #9f91f2); }.art-tech-track-tabs small, .art-tech-track-tabs b { display: block; }.art-tech-track-tabs small { margin-bottom: 6px; color: var(--blue); font-size: 8px; letter-spacing: .12em; }.art-tech-track-tabs b { font-size: 14px; }.art-tech-track-tabs__arrow { font-size: 19px; }
.art-tech-track-panel { display: grid; grid-template-columns: .72fr 1.28fr; gap: 35px; margin-top: 22px; padding: 34px; border: 1px solid var(--line); border-radius: 22px; background: rgba(255,255,255,.5); }.art-tech-track-panel__intro > span { color: var(--blue); font-size: 10px; font-weight: 800; letter-spacing: .12em; }.art-tech-track-panel__intro p { margin: 19px 0; color: var(--muted); font-size: 13px; line-height: 1.8; }.art-tech-track-panel__intro div { display: flex; flex-wrap: wrap; gap: 8px; }.art-tech-track-panel__intro b { padding: 7px 9px; border: 1px solid rgba(83,109,255,.2); border-radius: 7px; color: #596786; font-size: 9px; }.art-tech-directions { display: grid; grid-template-columns: repeat(2, 1fr); gap: 11px; }.art-tech-directions article { min-height: 122px; padding: 15px; border-left: 2px solid #a996f0; background: rgba(248,249,255,.76); }.art-tech-directions article:nth-child(3n) { border-color: #66d4cb; }.art-tech-directions span { color: #7d85a8; font-size: 9px; letter-spacing: .12em; }.art-tech-directions h3 { margin: 10px 0 7px; font-size: 14px; }.art-tech-directions p { margin: 0; color: var(--muted); font-size: 10px; line-height: 1.65; }
.art-tech-feature-grid, .art-tech-step-grid, .art-tech-file-grid { display: grid; gap: 13px; margin-top: 50px; }.art-tech-feature-grid { grid-template-columns: repeat(3, 1fr); }.art-tech-feature-grid article { position: relative; min-height: 220px; padding: 22px; overflow: hidden; border: 1px solid var(--line); border-radius: 20px; background: rgba(255,255,255,.52); }.art-tech-feature-grid article > span { color: var(--blue); font-size: 10px; font-weight: 800; }.art-tech-feature-grid h3 { position: relative; margin: 84px 0 7px; font-size: 17px; }.art-tech-feature-grid p { position: relative; max-width: 230px; margin: 0; color: var(--muted); font-size: 11px; line-height: 1.7; }.art-tech-feature-grid__orb { position: absolute; top: 21px; right: 22px; width: 64px; height: 64px; border: 1px solid rgba(83,109,255,.35); border-radius: 50%; background: radial-gradient(circle at 30% 25%, #fff, #b4a5f4 42%, #6ce1d4); box-shadow: 10px 13px 25px rgba(87,94,203,.2); }.art-tech-feature-grid__orb::after { position: absolute; inset: 11px; border: 1px dashed rgba(255,255,255,.85); border-radius: 50%; content: ""; }
.art-tech-step-grid { grid-template-columns: repeat(4, 1fr); }.art-tech-step-grid article { position: relative; min-height: 225px; padding: 22px; border-top: 2px solid #697eff; background: rgba(255,255,255,.43); }.art-tech-step-grid article:nth-child(2){border-color:#8b7fec}.art-tech-step-grid article:nth-child(3){border-color:#6ccfc9}.art-tech-step-grid article:nth-child(4){border-color:#d19bea}.art-tech-step-grid__index { color: var(--blue); font-size: 10px; font-weight: 800; }.art-tech-step-grid__mark { display: grid; width: 37px; height: 37px; margin-top: 37px; place-items: center; border: 1px solid rgba(83,109,255,.3); border-radius: 10px; color: var(--blue); font-weight: 800; }.art-tech-step-grid h3 { margin: 19px 0 8px; font-size: 15px; }.art-tech-step-grid p { margin: 0; color: var(--muted); font-size: 11px; line-height: 1.7; }.art-tech-step-grid i { position: absolute; right: 19px; bottom: 17px; color: #9aa4c4; font-style: normal; }
.art-tech-file-grid { grid-template-columns: repeat(3, 1fr); }.art-tech-file-grid article { padding: 23px; border: 1px solid var(--line); border-radius: 18px; background: rgba(255,255,255,.56); }.art-tech-file-grid span { color: var(--blue); font-size: 10px; }.art-tech-file-grid b, .art-tech-file-grid strong { display: block; }.art-tech-file-grid b { margin-top: 35px; color: var(--muted); font-size: 11px; }.art-tech-file-grid strong { margin-top: 10px; font-size: clamp(20px, 2vw, 27px); letter-spacing: -.04em; }.art-tech-file-grid p { margin: 12px 0 0; color: var(--muted); font-size: 10px; line-height: 1.7; }
.art-tech-milestones { display: grid; grid-template-columns: repeat(5, 1fr); gap: 0; margin-top: 57px; border-top: 1px solid rgba(83,109,255,.35); }.art-tech-milestones article { position: relative; padding: 25px 16px 0 0; }.art-tech-milestones__dot { position: absolute; top: -6px; left: 0; width: 11px; height: 11px; border: 3px solid var(--paper); border-radius: 50%; background: #9ca7c5; box-shadow: 0 0 0 1px #9ca7c5; }.art-tech-milestones article.is-current .art-tech-milestones__dot { background: var(--blue); box-shadow: 0 0 0 1px var(--blue), 0 0 0 8px rgba(83,109,255,.12); }.art-tech-milestones small, .art-tech-milestones strong { display: block; }.art-tech-milestones small { color: var(--blue); font-size: 9px; }.art-tech-milestones strong { margin-top: 12px; font-size: clamp(22px, 3vw, 35px); letter-spacing: -.07em; }.art-tech-milestones h3 { margin: 15px 0 8px; font-size: 13px; }.art-tech-milestones p { max-width: 180px; margin: 0; color: var(--muted); font-size: 10px; line-height: 1.65; }
.art-tech-notice { padding-bottom: 116px; }.art-tech-notice__top { display: flex; align-items: end; justify-content: space-between; gap: 20px; }.art-tech-notice__feature { display: grid; grid-template-columns: 105px 1fr 35px; gap: 20px; align-items: center; margin-top: 48px; padding: 25px 0; border-top: 1px solid var(--line); border-bottom: 1px solid var(--line); }.art-tech-notice__feature > span:first-child { color: var(--blue); font-size: 11px; font-weight: 800; }.art-tech-notice__feature h3 { margin: 0 0 8px; font-size: 17px; }.art-tech-notice__feature p { max-width: 700px; margin: 0; color: var(--muted); font-size: 11px; line-height: 1.7; }.art-tech-notice__feature-arrow { color: var(--blue); font-size: 24px; }.art-tech-downloads { display: grid; grid-template-columns: repeat(4, 1fr); gap: 11px; margin-top: 17px; }.art-tech-downloads button { display: grid; grid-template-columns: auto 1fr auto; gap: 5px 10px; align-items: center; min-height: 112px; padding: 16px; border: 1px solid var(--line); border-radius: 15px; color: var(--ink); background: rgba(255,255,255,.45); cursor: pointer; text-align: left; }.art-tech-downloads button:hover, .art-tech-downloads button:focus-visible { border-color: rgba(83,109,255,.48); background: rgba(255,255,255,.8); }.art-tech-downloads span { color: var(--blue); font-size: 9px; }.art-tech-downloads b { grid-column: 1 / -1; font-size: 12px; }.art-tech-downloads small { grid-column: 1 / 2; color: var(--muted); font-size: 9px; line-height: 1.5; }.art-tech-downloads i { grid-column: 3; grid-row: 2; color: var(--blue); font-style: normal; }
.art-tech-contact { display: grid; grid-template-columns: 1fr 1fr; gap: 80px; padding: 77px 0 90px; border-top: 1px solid var(--line); }.art-tech-contact__details { display: grid; grid-template-columns: repeat(2, 1fr); gap: 25px 20px; align-content: center; }.art-tech-contact__details div { display: grid; gap: 7px; }.art-tech-contact__details small { color: #7d87a6; font-size: 9px; }.art-tech-contact__details b { font-size: 12px; }.art-tech-footer { display: flex; align-items: center; justify-content: space-between; min-height: 100px; padding: 28px 34px; border-top: 1px solid rgba(255,255,255,.14); border-radius: 28px 28px 0 0; color: #f4f6ff; background: #232a49; }.art-tech-footer > div { display: grid; grid-template-columns: 34px auto; align-items: center; gap: 4px 10px; }.art-tech-footer .art-tech-brand__mark { grid-row: span 2; }.art-tech-footer strong { align-self: end; font-size: 13px; }.art-tech-footer small { align-self: start; color: #aeb8d5; font-size: 8px; letter-spacing: .13em; }.art-tech-footer p { margin: 0; color: #b7c0da; font-size: 10px; }.art-tech-footer__actions { display: flex !important; flex-wrap: wrap; gap: 14px 22px !important; }.art-tech-footer .art-tech-text-link { color: #a9c1ff; font-size: 10px; }
button:focus-visible, a:focus-visible { outline: 3px solid rgba(83,109,255,.42); outline-offset: 3px; }
@keyframes art-tech-scan { 0%, 26% { transform: translateX(-90%); } 60%, 100% { transform: translateX(90%); } }
@keyframes art-tech-float { 0%, 100% { transform: translate3d(0, 0, 0); } 50% { transform: translate3d(8px, -12px, 0); } }
@media (max-width: 1020px) { .art-tech-nav { gap: 14px; margin-right: 14px; }.art-tech-nav a:nth-child(3) { display: none; }.art-tech-hero { grid-template-columns: 1fr .9fr; }.art-tech-orbit { min-height: 410px; }.art-tech-section__heading { gap: 30px; }.art-tech-milestones strong { font-size: 25px; } }
@media (max-width: 760px) { .art-tech-header, main, .art-tech-footer { width: min(100% - 30px, 620px); }.art-tech-header { min-height: 68px; flex-wrap: wrap; gap: 10px; padding: 11px 0; }.art-tech-nav { order: 3; width: 100%; justify-content: space-between; gap: 7px; margin: 0; overflow-x: auto; }.art-tech-nav a { flex: 0 0 auto; font-size: 10px; }.art-tech-header__actions .art-tech-version { display: none; }.art-tech-hero { grid-template-columns: 1fr; min-height: auto; padding-top: 55px; }.art-tech-hero h1 { font-size: clamp(48px, 13vw, 72px); }.art-tech-orbit { min-height: 360px; order: -1; border-radius: 35% 65% 40% 60% / 45% 35% 65% 55%; }.art-tech-hero__meta { gap: 15px; margin-top: 35px; }.art-tech-info-strip { grid-template-columns: repeat(2, 1fr); gap: 12px; }.art-tech-section { padding-top: 82px; }.art-tech-section__heading, .art-tech-section__heading--compact, .art-tech-contact { grid-template-columns: 1fr; gap: 24px; }.art-tech-section__heading p { max-width: none; }.art-tech-track-tabs, .art-tech-track-panel { grid-template-columns: 1fr; }.art-tech-track-panel { padding: 22px; }.art-tech-feature-grid, .art-tech-file-grid { grid-template-columns: 1fr; }.art-tech-feature-grid article { min-height: 185px; }.art-tech-step-grid { grid-template-columns: repeat(2, 1fr); }.art-tech-milestones { grid-template-columns: 1fr; gap: 0; margin-left: 6px; border-top: 0; border-left: 1px solid rgba(83,109,255,.35); }.art-tech-milestones article { min-height: 132px; padding: 0 0 30px 27px; }.art-tech-milestones__dot { top: 2px; left: -6px; }.art-tech-notice__top { align-items: start; flex-direction: column; }.art-tech-downloads { grid-template-columns: repeat(2, 1fr); }.art-tech-contact__details { gap: 18px 10px; }.art-tech-footer { align-items: start; flex-direction: column; gap: 20px; padding: 28px 0 35px; } }
@media (max-width: 420px) { .art-tech-header__actions .art-tech-button--quiet { padding: 9px 10px; font-size: 10px; }.art-tech-header__actions .art-tech-button--primary { padding: 9px 12px; font-size: 10px; }.art-tech-hero { padding-top: 38px; }.art-tech-orbit { min-height: 285px; }.art-tech-orbit__readout { transform: scale(.82); transform-origin: center; }.art-tech-hero h1 { font-size: 49px; }.art-tech-hero__summary { font-size: 13px; }.art-tech-hero__buttons { align-items: flex-start; flex-direction: column; gap: 14px; }.art-tech-track-tabs button { grid-template-columns: 43px 1fr 18px; padding: 12px; }.art-tech-track-tabs__mark { width: 38px; height: 38px; font-size: 16px; }.art-tech-track-tabs b { font-size: 12px; }.art-tech-directions { grid-template-columns: 1fr; }.art-tech-step-grid { grid-template-columns: 1fr; }.art-tech-downloads { grid-template-columns: 1fr; }.art-tech-notice__feature { grid-template-columns: 1fr 25px; }.art-tech-notice__feature > span:first-child { grid-column: 1 / -1; }.art-tech-notice__feature p { font-size: 10px; }.art-tech-contact__details { grid-template-columns: 1fr; }.art-tech-footer p { line-height: 1.6; } }
@media (prefers-reduced-motion: reduce) { :global(html) { scroll-behavior: auto; }.art-tech-page *, .art-tech-page *::before, .art-tech-page *::after { animation-duration: .001ms !important; animation-iteration-count: 1 !important; transition-duration: .001ms !important; } }
</style>
