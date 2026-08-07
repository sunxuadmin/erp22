<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { activeVersion, themeConfig } from "../../config/entries";
import {
  competitionHomeContent,
  type CompetitionHomeAction,
  type CompetitionHomeVersion
} from "../../config/competitionHome";
import { competitionHomeModules } from "../../config/competitionHomeModules";
import CompetitionHomeArtTech from "./CompetitionHomeArtTech.vue";
import CompetitionHomeCanvas from "./CompetitionHomeCanvas.vue";
import CompetitionHomeLiquidGlass from "./CompetitionHomeLiquidGlass.vue";

const homeVersion: CompetitionHomeVersion = activeVersion === "v2" ? "v2" : "v1";
const activeAction = ref<CompetitionHomeAction | null>(null);
const dialogPanel = ref<HTMLElement | null>(null);
const closeButton = ref<HTMLButtonElement | null>(null);
let previouslyFocused: HTMLElement | null = null;
const canvasThemeStyle = computed(() => ({
  "--page-bg-start": themeConfig.pageBgStart,
  "--page-bg-middle": themeConfig.pageBgMiddle,
  "--page-bg-end": themeConfig.pageBgEnd,
  "--hero-accent-color": themeConfig.heroAccent,
  "--section-title-color": themeConfig.sectionTitle
}));

const actionDetail: Record<CompetitionHomeAction["id"], string> = {
  login: "统一登录入口尚未接入。正式开放后将从这里进入身份认证。",
  submit: "作品报送入口尚未接入。当前不会提交任何作品或个人信息。",
  "platform-status": "当前页面仅展示筹备状态，实际开放时间以后端活动状态和正式通知为准。",
  "official-notice": "正式通知详情尚未接入 CMS，当前页面内容仅作结构与视觉验收。",
  "download-registration": "参赛作品登记表尚未发布，当前不会生成或下载虚假文件。",
  "download-student-summary": "学生组作品汇总表尚未发布，当前不会生成或下载虚假文件。",
  "download-teacher-summary": "教师组作品汇总表尚未发布，当前不会生成或下载虚假文件。",
  "download-contact": "参赛单位联络人信息表尚未发布，当前不会生成或下载虚假文件。"
};

function openPreparationDialog(action: CompetitionHomeAction) {
  previouslyFocused = document.activeElement instanceof HTMLElement ? document.activeElement : null;
  activeAction.value = action;
  void nextTick(() => closeButton.value?.focus());
}

function closePreparationDialog() {
  activeAction.value = null;
  void nextTick(() => previouslyFocused?.focus());
}

function handleVersionChange(version: CompetitionHomeVersion) {
  if (version === homeVersion) return;
  const url = new URL(window.location.href);
  url.searchParams.set("version", version);
  window.location.assign(url.toString());
}

function handleGlobalKeydown(event: KeyboardEvent) {
  if (!activeAction.value) return;
  if (event.key === "Escape") {
    closePreparationDialog();
    return;
  }
  if (event.key !== "Tab" || !dialogPanel.value) return;

  const focusable = Array.from(
    dialogPanel.value.querySelectorAll<HTMLElement>(
      'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
    )
  );
  if (!focusable.length) {
    event.preventDefault();
    return;
  }

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

onMounted(() => window.addEventListener("keydown", handleGlobalKeydown));
onBeforeUnmount(() => window.removeEventListener("keydown", handleGlobalKeydown));
</script>

<template>
  <CompetitionHomeCanvas
    v-if="competitionHomeModules.length"
    :content="competitionHomeContent"
    :modules="competitionHomeModules"
    :style="canvasThemeStyle"
    :version="homeVersion"
    @action="openPreparationDialog"
    @change-version="handleVersionChange"
  />
  <CompetitionHomeArtTech
    v-else-if="homeVersion === 'v2'"
    :content="competitionHomeContent"
    @action="openPreparationDialog"
    @change-version="handleVersionChange"
  />
  <CompetitionHomeLiquidGlass
    v-else
    :content="competitionHomeContent"
    @action="openPreparationDialog"
    @change-version="handleVersionChange"
  />

  <div
    v-if="activeAction"
    class="preparation-dialog-backdrop"
    data-testid="preparation-dialog"
    @click.self="closePreparationDialog"
  >
    <section
      ref="dialogPanel"
      class="preparation-dialog"
      role="dialog"
      aria-modal="true"
      aria-labelledby="preparation-dialog-title"
      aria-describedby="preparation-dialog-description"
    >
      <span class="preparation-dialog__status" aria-hidden="true">CH / PREPARING</span>
      <h2 id="preparation-dialog-title">平台筹备中</h2>
      <p id="preparation-dialog-description">
        <strong>{{ activeAction.label }}</strong>
        {{ actionDetail[activeAction.id] }}
      </p>
      <button ref="closeButton" type="button" @click="closePreparationDialog">我知道了</button>
    </section>
  </div>
</template>

<style scoped>
.preparation-dialog-backdrop {
  position: fixed;
  z-index: 1000;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(20, 22, 48, 0.28);
  backdrop-filter: blur(12px);
}

.preparation-dialog {
  width: min(440px, 100%);
  padding: 32px;
  border: 1px solid rgba(255, 255, 255, 0.82);
  border-radius: 28px;
  color: #282943;
  background:
    radial-gradient(circle at 90% 10%, rgba(160, 187, 255, 0.3), transparent 40%),
    rgba(255, 255, 255, 0.9);
  box-shadow: 0 28px 90px rgba(53, 55, 107, 0.24);
}

.preparation-dialog__status {
  color: #596cff;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

.preparation-dialog h2 {
  margin: 12px 0 14px;
  font-size: clamp(28px, 7vw, 38px);
  letter-spacing: -0.04em;
}

.preparation-dialog p {
  margin: 0;
  color: #686a83;
  font-size: 14px;
  line-height: 1.8;
}

.preparation-dialog p strong {
  display: block;
  margin-bottom: 3px;
  color: #31334f;
}

.preparation-dialog button {
  min-height: 44px;
  margin-top: 24px;
  padding: 0 22px;
  border: 0;
  border-radius: 999px;
  color: #fff;
  background: linear-gradient(135deg, #586cff, #8a60f2);
  box-shadow: 0 12px 28px rgba(88, 108, 255, 0.26);
  cursor: pointer;
}

.preparation-dialog button:focus-visible {
  outline: 3px solid rgba(88, 108, 255, 0.35);
  outline-offset: 3px;
}

@media (max-width: 480px) {
  .preparation-dialog {
    padding: 26px 22px;
    border-radius: 22px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .preparation-dialog-backdrop {
    backdrop-filter: none;
  }
}
</style>
