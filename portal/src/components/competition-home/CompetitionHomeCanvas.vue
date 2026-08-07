<script setup lang="ts">
import { computed, nextTick, ref, watch } from "vue";
import type { CompetitionHomeVersion } from "../../config/competitionHome";
import type { CompetitionHomeModule } from "../../config/competitionHomeModules";
import { createCompetitionHomeFallback } from "./competitionHomeFallback";

type DialogKind = "portal" | "notice";
type TextRow = string[];

const props = defineProps<{
  modules: CompetitionHomeModule[];
  version: CompetitionHomeVersion;
}>();

const emit = defineEmits<{
  "change-version": [version: CompetitionHomeVersion];
}>();

const menuOpen = ref(false);
const selectedTrack = ref<"a" | "b">("a");
const activeDialog = ref<DialogKind | null>(null);
const dialogRef = ref<HTMLElement>();
const dialogCloseButton = ref<HTMLButtonElement>();
let previousFocusedElement: HTMLElement | null = null;
const legacyDefaults = computed(() => new Map(createCompetitionHomeFallback(props.version).map((module) => [module.componentType, module.config])));

const byType = (type: string) => computed(() => props.modules.find((module) => module.componentType === type));
const navModule = byType("competition-nav");
const heroModule = byType("competition-hero");
const factModule = byType("competition-key-facts");
const trackModule = byType("competition-tracks");
const artTechModule = byType("competition-art-tech");
const journeyModule = byType("competition-journey");
const fileModule = byType("competition-file-specs");
const timelineModule = byType("competition-timeline");
const noticeModule = byType("competition-notice-downloads");
const contactModule = byType("competition-contact");
const footerModule = byType("competition-footer");

function value(module: CompetitionHomeModule | undefined, key: string, fallback = "", componentType?: string) {
  const configured = module?.config[key] ?? legacyDefaults.value.get(module?.componentType || componentType || "")?.[key];
  return typeof configured === "string" || typeof configured === "number" ? String(configured) : fallback;
}

function flag(module: CompetitionHomeModule | undefined, key: string, fallback = false, componentType?: string) {
  const configured = module?.config[key] ?? legacyDefaults.value.get(module?.componentType || componentType || "")?.[key];
  return typeof configured === "boolean" ? configured : fallback;
}

function rows(module: CompetitionHomeModule | undefined, key: string, fallback: TextRow[], columns: number, componentType?: string) {
  const configured = module?.config[key] ?? legacyDefaults.value.get(module?.componentType || componentType || "")?.[key];
  if (configured === undefined) return fallback;
  if (typeof configured !== "string") return [];
  return configured
    .split(/\r?\n/)
    .map((line) => line.split("|").map((part) => part.trim()))
    .filter((parts) => parts.length >= columns && parts.slice(0, columns).every(Boolean))
    .map((parts) => parts.slice(0, columns));
}

const navigation = computed(() => rows(navModule.value, "navItems", [["大赛首页", "top"], ["赛道设置", "tracks"], ["报送流程", "journey"], ["时间安排", "timeline"], ["通知公告", "notice"]], 2, "competition-nav"));
const artLetters = computed(() => Array.from(value(heroModule.value, "artText", "ART")));
const facts = computed(() => [1, 2, 3, 4].map((index) => ({ label: value(factModule.value, `fact${index}Label`), content: value(factModule.value, `fact${index}Value`) })));
const countdownValue = computed(() => {
  const deadlineAt = value(factModule.value, "deadlineAt");
  const deadline = Date.parse(deadlineAt);
  if (!Number.isFinite(deadline)) return value(factModule.value, "invalidCountdownValue", "—");
  return String(Math.max(0, Math.ceil((deadline - Date.now()) / 86_400_000)));
});
const documentTitle = computed(() => value(heroModule.value, "documentTitle"));

if (typeof document !== "undefined") {
  watch(documentTitle, (title) => {
    document.title = title;
  }, { immediate: true });
}

const tracks = computed(() => (["A", "B"] as const).map((name) => {
  const key = name === "A" ? "trackA" : "trackB";
  const quota = rows(trackModule.value, `${key}Quota`, [[name === "A" ? "≤ 20" : "≤ 5", "每校每组限额 / 件（套）"]], 2)[0] || ["", ""];
  const authorQuota = rows(trackModule.value, `${key}AuthorQuota`, [["≤ 3", "每件作品设计者 / 人"]], 2)[0] || ["", ""];
  return {
    id: name.toLowerCase() as "a" | "b",
    label: value(trackModule.value, `${key}Label`, `赛道 ${name}`),
    mark: value(trackModule.value, `${key}Mark`, name),
    badge: value(trackModule.value, `${key}Badge`, name === "A" ? "7 个类别" : "5 个方向"),
    title: value(trackModule.value, `${key}Title`),
    english: value(trackModule.value, `${key}English`),
    description: value(trackModule.value, `${key}Description`),
    quota,
    authorQuota,
    directions: rows(trackModule.value, `${key}Directions`, [], 3).map(([code, title, description]) => ({ code, title, description }))
  };
}));
const activeTrack = computed(() => tracks.value.find((track) => track.id === selectedTrack.value) || tracks.value[0]);
const artTechItems = computed(() => rows(artTechModule.value, "items", [], 3).map(([index, title, description]) => ({ index, title, description })));
const journeyItems = computed(() => rows(journeyModule.value, "items", [], 4).map(([index, mark, title, description]) => ({ index, mark, title, description })));
const fileItems = computed(() => rows(fileModule.value, "items", [], 4).map(([index, label, specification, description]) => ({ index, label, specification, description })));
const timelineItems = computed(() => rows(timelineModule.value, "items", [], 5).map(([index, date, year, title, description]) => ({ index, date, year, title, description })));
const axisLabels = computed(() => value(timelineModule.value, "axisLabels", "").split("|").map((item) => item.trim()).filter(Boolean));
const downloadItems = computed(() => rows(noticeModule.value, "items", [], 3).map(([index, title, description]) => ({ index, title, description })));
const contactItems = computed(() => rows(contactModule.value, "contacts", [], 2).map(([label, content]) => ({ label, content })));
const organizers = computed(() => value(contactModule.value, "organizers", "").split(/\r?\n/).filter(Boolean));
const contractors = computed(() => value(footerModule.value, "contractors", "").split(/\r?\n/).filter(Boolean));
const noticeRules = computed(() => value(navModule.value, "noticeModalRules", "", "competition-nav").split(/\r?\n/).filter(Boolean));

function gridStyle(module: CompetitionHomeModule) {
  return {
    gridColumn: `${module.gridX + 1} / span ${module.gridW}`,
    gridRow: `${module.gridY + 1} / span ${module.gridH}`
  };
}

function scrollTo(id: string) {
  document.getElementById(id)?.scrollIntoView({ behavior: "smooth", block: "start" });
  menuOpen.value = false;
}

function openDialog(kind: DialogKind) {
  menuOpen.value = false;
  previousFocusedElement = document.activeElement instanceof HTMLElement ? document.activeElement : null;
  activeDialog.value = kind;
  void nextTick(() => dialogCloseButton.value?.focus());
}

async function closeDialog() {
  activeDialog.value = null;
  await nextTick();
  previousFocusedElement?.focus();
  previousFocusedElement = null;
}

async function noticeDialogAction() {
  await closeDialog();
  scrollTo("timeline");
}

async function portalDialogAction() {
  await closeDialog();
  scrollTo("contact");
}

function handleDialogKeydown(event: KeyboardEvent) {
  if (event.key === "Escape") {
    event.preventDefault();
    void closeDialog();
    return;
  }
  if (event.key !== "Tab" || !dialogRef.value) return;
  const focusable = Array.from(dialogRef.value.querySelectorAll<HTMLElement>("button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex='-1'])"));
  if (!focusable.length) return;
  const first = focusable[0];
  const last = focusable[focusable.length - 1];
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault();
    last.focus();
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault();
    first.focus();
  }
}
</script>

<template>
  <main class="competition-canvas" :class="`competition-canvas--${version}`">
    <div class="competition-canvas__grid">
      <section
        v-for="module in modules"
        :key="module.componentKey"
        class="competition-module"
        :class="[`competition-module--${module.componentType.replace('competition-', '')}`, { 'is-sticky': module.componentType === 'competition-nav' && flag(module, 'sticky', true) }]"
        :style="gridStyle(module)"
        :data-component-key="module.componentKey"
      >
        <header v-if="module.componentType === 'competition-nav'" class="site-header">
          <div class="site-nav">
            <button class="brand-lockup" type="button" @click="scrollTo('top')">
              <span class="brand-mark" aria-hidden="true">{{ value(module, 'brandMark', 'CH') }}</span>
              <span><strong>{{ value(module, 'brand') }}</strong><small>{{ value(module, 'slogan') }}</small></span>
            </button>
            <nav class="desktop-nav" aria-label="主导航">
              <button v-for="item in navigation" :key="item.join('|')" type="button" @click="scrollTo(item[1])">{{ item[0] }}</button>
            </nav>
            <div class="nav-actions">
              <button type="button" class="quiet-button" @click="openDialog('portal')">{{ value(module, 'loginLabel') }}</button>
              <button type="button" class="primary-button primary-button--compact" @click="scrollTo('journey')">{{ value(module, 'actionLabel') }} <span aria-hidden="true">↗</span></button>
              <button type="button" class="menu-button" :aria-label="menuOpen ? value(module, 'menuCloseLabel') : value(module, 'menuOpenLabel')" :aria-expanded="menuOpen" @click="menuOpen = !menuOpen"><i /><i /></button>
            </div>
          </div>
          <nav v-if="menuOpen" class="mobile-nav" aria-label="移动导航">
            <button v-for="item in navigation" :key="item.join('|')" type="button" @click="scrollTo(item[1])">{{ item[0] }}</button>
          </nav>
        </header>

        <section v-else-if="module.componentType === 'competition-hero'" id="top" class="hero-section" :class="`hero-section--${version}`" :aria-label="value(module, 'documentTitle')">
          <div class="hero-copy">
            <div class="hero-motto"><span class="motto-mark" aria-hidden="true" /><span><strong>{{ value(module, 'motto') }}</strong><small>{{ value(module, 'mottoEn') }}</small></span></div>
            <h1 class="event-title">
              <span class="art-year-row"><span class="art-word"><span v-for="(letter, index) in artLetters" :key="`${letter}-${index}`" class="art-letter">{{ letter }}</span></span><span class="year-text">{{ value(module, 'yearText') }}</span></span>
              <span class="edition-line"><i />{{ value(module, 'editionText') }} <b>{{ value(module, 'editionEn') }}</b></span>
              <span class="competition-title">{{ value(module, 'competitionTitle') }}</span>
            </h1>
            <p class="hero-summary">{{ value(module, 'summary') }}</p>
            <div class="hero-actions"><button type="button" class="primary-button" @click="scrollTo('journey')">{{ value(module, 'primaryActionLabel') }} <span aria-hidden="true">↗</span></button><button type="button" class="text-button" @click="version === 'v2' ? scrollTo('tracks') : scrollTo('notice')">{{ value(module, 'secondaryActionLabel') }} <span aria-hidden="true">→</span></button></div>
            <div class="hero-meta"><span><small>{{ value(module, 'organizerLabel') }}</small><strong>{{ value(module, 'organizer') }}</strong></span><i v-if="version === 'v2'" aria-hidden="true" /><span><small>{{ value(module, 'groupsLabel') }}</small><strong>{{ value(module, 'groups') }}</strong></span><span v-if="value(module, 'feeLabel') || value(module, 'fee')"><small>{{ value(module, 'feeLabel') }}</small><strong>{{ value(module, 'fee') }}</strong></span></div>
          </div>
          <div class="hero-visual">
            <img v-if="version === 'v1'" class="hero-artwork" src="/creative-henan-hero.png" width="1448" height="1086" :alt="value(module, 'artAlt')" fetchpriority="high" />
            <img v-else class="hero-artwork" src="/arttech-hero.png" width="1584" height="990" :alt="value(module, 'artAlt')" fetchpriority="high" />
            <div class="hero-axis hero-axis--x" aria-hidden="true"><span>{{ value(module, 'axisStart') }}</span><i /><span>{{ value(module, 'axisEnd') }}</span></div><div class="hero-axis hero-axis--y" aria-hidden="true"><span>{{ value(module, 'axisArt') }}</span><i /><span>{{ value(module, 'axisTech') }}</span></div>
            <template v-if="version === 'v1'"><div class="float-card float-card--deadline"><small>{{ value(module, 'uploadDeadlineLabel') }}</small><strong>{{ value(module, 'uploadDeadline') }}</strong></div><div class="float-card float-card--tracks"><small>{{ value(module, 'trackCardLabel') }}</small><strong>{{ value(module, 'trackCardValue') }}</strong></div><div class="float-card float-card--status"><i aria-hidden="true" /><span><small>{{ value(module, 'statusLabel') }}</small><strong>{{ value(module, 'status') }}</strong></span></div></template>
            <template v-else><div class="telemetry telemetry--top"><span>{{ value(module, 'trackCardLabel') }}</span><strong>{{ value(module, 'trackCardValue').split('\n')[0] }}</strong><small>{{ value(module, 'trackCardValue').split('\n')[1] }}</small></div><div class="telemetry telemetry--side"><span>{{ value(module, 'uploadDeadlineLabel') }}</span><strong>{{ countdownValue }} {{ value(factModule, 'countdownLabel', 'DAYS', 'competition-key-facts') }}</strong><small>{{ value(module, 'uploadDeadline') }}</small></div><div class="spectrum-chip" aria-hidden="true"><i /><i /><i /><i /><i /></div></template>
          </div>
        </section>

        <section v-else-if="module.componentType === 'competition-key-facts'" class="facts-band" :class="`facts-band--${version}`">
          <div v-if="version === 'v1'" class="countdown"><i aria-hidden="true" /><strong>{{ countdownValue }}</strong><span>{{ value(module, 'countdownLabel') }}<br />{{ value(module, 'countdownSuffix') }}</span></div>
          <template v-for="(fact, index) in facts" :key="index"><div v-if="fact.label || fact.content" class="fact"><small>{{ fact.label }}</small><strong>{{ fact.content }}</strong></div></template>
          <button type="button" aria-label="继续浏览" @click="scrollTo(version === 'v1' ? 'timeline' : 'tracks')">↓</button>
        </section>

        <section v-else-if="module.componentType === 'competition-tracks'" id="tracks" class="content-section tracks-section" :class="`tracks-section--${version}`">
          <header class="section-heading section-heading--split"><div><p>{{ value(module, 'eyebrow') }}</p><h2>{{ value(module, 'title') }}</h2></div><span>{{ value(module, 'summary') }}</span></header>
          <div class="track-tabs" role="tablist" aria-label="选择赛道"><button v-for="track in tracks" :key="track.id" type="button" role="tab" :aria-selected="activeTrack?.id === track.id" :class="{ 'is-active': activeTrack?.id === track.id }" @click="selectedTrack = track.id"><small>{{ track.label }}</small><strong>{{ track.title }}</strong><em>{{ track.badge }}</em></button></div>
          <div v-if="activeTrack" class="track-panel" role="tabpanel"><article class="track-intro"><span>{{ activeTrack.mark }}</span><small>{{ activeTrack.english }}</small><h3>{{ activeTrack.title }}</h3><p>{{ activeTrack.description }}</p><div><b>{{ activeTrack.quota[0] }}</b><small>{{ activeTrack.quota[1] }}</small><b>{{ activeTrack.authorQuota[0] }}</b><small>{{ activeTrack.authorQuota[1] }}</small></div></article><div class="direction-grid"><article v-for="item in activeTrack.directions" :key="item.code"><small>{{ item.code }}</small><h3>{{ item.title }}</h3><p>{{ item.description }}</p><i aria-hidden="true">↗</i></article></div></div>
        </section>

        <section v-else-if="module.componentType === 'competition-art-tech'" class="art-tech-section" :class="`art-tech-section--${version}`"><header><p>{{ value(module, 'eyebrow') }}</p><h2>{{ value(module, 'title') }}</h2><span v-if="value(module, 'summary')">{{ value(module, 'summary') }}</span></header><div class="art-tech-grid"><article v-for="item in artTechItems" :key="item.index"><small>{{ item.index }}</small><i aria-hidden="true" /><h3>{{ item.title }}</h3><p>{{ item.description }}</p></article></div><div class="manifesto-ribbon" aria-hidden="true"><i /><i /><i /></div></section>

        <section v-else-if="module.componentType === 'competition-journey'" id="journey" class="content-section journey-section" :class="`journey-section--${version}`"><header class="section-heading" :class="version === 'v1' ? 'section-heading--center' : 'section-heading--split'"><div><p>{{ value(module, 'eyebrow') }}</p><h2>{{ value(module, 'title') }}</h2><span v-if="value(module, 'summary')">{{ value(module, 'summary') }}</span></div><button v-if="version === 'v2' && value(module, 'actionLabel')" type="button" class="outline-button" @click="openDialog('portal')">{{ value(module, 'actionLabel') }} <span aria-hidden="true">↗</span></button></header><div class="journey-grid"><article v-for="item in journeyItems" :key="item.index"><small>{{ version === 'v2' && value(module, 'stepPrefix') ? value(module, 'stepPrefix') + ' ' + item.index : item.index }}</small><b>{{ item.mark }}</b><h3>{{ item.title }}</h3><p>{{ item.description }}</p></article></div><aside v-if="version === 'v1'" class="journey-status"><small>{{ value(module, 'statusLabel') }}</small><strong>{{ value(module, 'deskTitle') }}</strong><p>{{ value(module, 'deskSummary') }}</p><button type="button" @click="openDialog('portal')">{{ value(module, 'actionLabel') }} <span aria-hidden="true">↗</span></button></aside></section>

        <section v-else-if="module.componentType === 'competition-file-specs'" class="content-section files-section" :class="`files-section--${version}`"><header v-if="version === 'v2'" class="section-heading"><p>{{ value(module, 'eyebrow') }}</p><h2>{{ value(module, 'title') }}</h2><span>{{ value(module, 'summary') }}</span></header><div class="file-grid"><article v-for="item in fileItems" :key="item.index"><small>{{ item.label }}</small><span>{{ item.index }}</span><h3>{{ item.specification }}</h3><p>{{ item.description }}</p></article></div></section>

        <section v-else-if="module.componentType === 'competition-timeline'" id="timeline" class="content-section timeline-section" :class="`timeline-section--${version}`"><header class="section-heading"><p>{{ value(module, 'eyebrow') }}</p><h2>{{ value(module, 'title') }}</h2><span>{{ value(module, 'summary') }}</span><button v-if="value(module, 'actionLabel')" type="button" class="text-button" @click="openDialog('notice')">{{ value(module, 'actionLabel') }} <span aria-hidden="true">→</span></button></header><div class="timeline-board"><div v-if="axisLabels.length" class="timeline-axis"><span v-for="label in axisLabels" :key="label">{{ label }}</span></div><article v-for="item in timelineItems" :key="item.index"><time><strong>{{ item.date }}</strong><small>{{ item.year }}</small></time><div><small>{{ item.index }}</small><h3>{{ item.title }}</h3><p>{{ item.description }}</p></div></article></div></section>

        <section v-else-if="module.componentType === 'competition-notice-downloads'" id="notice" class="content-section notice-section" :class="`notice-section--${version}`"><header v-if="version === 'v1'" class="section-heading section-heading--split"><div><p>{{ value(module, 'eyebrow') }}</p><h2>{{ value(module, 'title') }}</h2></div><button type="button" class="outline-button" @click="openDialog('notice')">{{ value(module, 'actionLabel') }} <span aria-hidden="true">↗</span></button></header><div v-else class="notice-hero"><div><p>{{ value(module, 'eyebrow') }}</p><h2>{{ value(module, 'title') }}</h2><span>{{ value(module, 'summary') }}</span><button type="button" class="primary-button" @click="openDialog('notice')">{{ value(module, 'actionLabel') }} <span aria-hidden="true">↗</span></button></div><div v-if="value(module, 'noticeArtMark')" class="notice-art" aria-hidden="true"><i /><span>{{ value(module, 'noticeArtMark') }}</span></div></div><div class="notice-layout"><article v-if="version === 'v1'" class="notice-card"><small>{{ value(module, 'noticeEyebrow') }} · {{ value(module, 'noticeDate') }}</small><h3>{{ value(module, 'noticeTitle') }}</h3><p>{{ value(module, 'noticeSummary') }}</p><button v-if="value(module, 'noticeActionLabel')" type="button" class="text-button" @click="openDialog('notice')">{{ value(module, 'noticeActionLabel') }} <span aria-hidden="true">→</span></button></article><div class="download-list"><button v-for="item in downloadItems" :key="item.index" type="button" @click="openDialog('portal')"><small>{{ item.index }}</small><span><strong>{{ item.title }}</strong><em>{{ item.description }}</em></span><i aria-hidden="true">↓</i></button></div></div></section>

        <section v-else-if="module.componentType === 'competition-contact'" id="contact" class="content-section contact-section" :class="`contact-section--${version}`"><header v-if="value(module, 'eyebrow') || value(module, 'title') || value(module, 'summary')" class="section-heading"><p>{{ value(module, 'eyebrow') }}</p><h2>{{ value(module, 'title') }}</h2><span>{{ value(module, 'summary') }}</span></header><div class="contact-grid"><span v-for="item in contactItems" :key="item.label"><small>{{ item.label }}</small><strong>{{ item.content }}</strong></span></div><p v-if="organizers.length" class="organizer-list"><span v-for="item in organizers" :key="item">{{ item }}</span></p></section>

        <footer v-else-if="module.componentType === 'competition-footer'" class="site-footer" :class="`site-footer--${version}`"><div class="footer-main"><div class="footer-brand"><span v-if="value(module, 'brandMark')" class="brand-mark" aria-hidden="true">{{ value(module, 'brandMark') }}</span><span><strong>{{ value(module, 'brand') }}</strong><small>{{ value(module, 'slogan') }}</small></span></div><div><small>{{ value(module, 'organizerLabel') }}</small><strong>{{ value(module, 'organizer') }}</strong></div><div><small>{{ value(module, 'contractorLabel') }}</small><strong v-for="item in contractors" :key="item">{{ item }}</strong></div><button type="button" @click="scrollTo('top')">{{ value(module, 'backTopLabel') }}</button></div><div class="footer-bottom"><span>{{ value(module, 'disclaimer') }}</span><span>{{ value(module, 'signature') }}</span><div class="version-switch"><button type="button" :class="{ 'is-active': version === 'v1' }" @click="emit('change-version', 'v1')">{{ value(module, 'versionV1Label') }}</button><button type="button" :class="{ 'is-active': version === 'v2' }" @click="emit('change-version', 'v2')">{{ value(module, 'versionV2Label') }}</button></div></div></footer>
      </section>
    </div>

    <div v-if="activeDialog" class="dialog-backdrop" @click.self="closeDialog">
      <section ref="dialogRef" class="competition-dialog" role="dialog" aria-modal="true" :aria-labelledby="`${activeDialog}-dialog-title`" @keydown="handleDialogKeydown">
        <button ref="dialogCloseButton" type="button" class="dialog-close" :aria-label="value(navModule, 'modalCloseLabel', '关闭', 'competition-nav')" @click="closeDialog">×</button>
        <template v-if="activeDialog === 'portal'"><small>{{ value(navModule, 'portalModalEyebrow', '', 'competition-nav') }}</small><h2 :id="`${activeDialog}-dialog-title`">{{ value(navModule, 'portalModalTitle', '', 'competition-nav') }}</h2><p>{{ value(navModule, 'portalModalBody', '', 'competition-nav') }}</p><aside><strong>{{ value(navModule, 'portalModalPromptLabel', '', 'competition-nav') }}</strong><span>{{ value(navModule, 'portalModalPrompt', '', 'competition-nav') }}</span></aside><button type="button" class="primary-button" @click="portalDialogAction">{{ value(navModule, 'portalModalButton', '', 'competition-nav') }}</button></template>
        <template v-else><small>{{ value(navModule, 'noticeModalEyebrow', '', 'competition-nav') }}</small><h2 :id="`${activeDialog}-dialog-title`">{{ value(navModule, 'noticeModalTitle', '', 'competition-nav') }}</h2><p>{{ value(navModule, 'noticeModalBody', '', 'competition-nav') }}</p><ul><li v-for="rule in noticeRules" :key="rule">{{ rule }}</li></ul><button type="button" class="primary-button" @click="noticeDialogAction">{{ value(navModule, 'noticeModalButton', '', 'competition-nav') }}</button></template>
      </section>
    </div>
  </main>
</template>

<style src="./CompetitionHomeCanvas.css"></style>
