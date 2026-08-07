import { createApp } from "vue";
import "./styles/global.css";

async function loadEditorPreviewConfig() {
  const query = new URLSearchParams(window.location.search);
  const isLocalEditor =
    query.get("editorPreview") === "1" &&
    (window.location.hostname === "127.0.0.1" || window.location.hostname === "localhost");

  if (!isLocalEditor) return;

  document.documentElement.dataset.editorPreview = "true";

  try {
    const response = await fetch("/api/preview-config", { cache: "no-store" });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    window.CREHN_PORTAL_CONFIG = await response.json();
  } catch (error) {
    console.warn("Unable to load local editor preview config; using public/config.js.", error);
  }
}

async function loadPublishedHomeConfig() {
  const query = new URLSearchParams(window.location.search);
  if (query.get("editorPreview") === "1") return;

  const apiBase = String(import.meta.env.VITE_PORTAL_API_BASE || "/prod-api").replace(/\/$/, "");
  const siteCode = String(import.meta.env.VITE_PORTAL_SITE_CODE || "crehn").trim();
  if (!siteCode) return;

  try {
    const siteResponse = await fetch(
      `${apiBase}/crehn/cms/public/site/code/${encodeURIComponent(siteCode)}`,
      { headers: { Accept: "application/json" } }
    );
    if (!siteResponse.ok) return;
    const sitePayload = (await siteResponse.json()) as { code?: number; data?: { id?: number } };
    if (sitePayload.code !== 200 || !sitePayload.data?.id) return;

    const homeResponse = await fetch(
      `${apiBase}/crehn/cms/public/site/${sitePayload.data.id}/home/config`,
      { headers: { Accept: "application/json" } }
    );
    if (!homeResponse.ok) return;
    const homePayload = (await homeResponse.json()) as {
      code?: number;
      data?: {
        layoutCode?: string;
        renderVersion?: string;
        theme?: Record<string, unknown>;
        components?: unknown[];
      };
    };
    if (homePayload.code !== 200 || !homePayload.data) return;

    const current = window.CREHN_PORTAL_CONFIG || {};
    window.CREHN_PORTAL_CONFIG = {
      ...current,
      theme: { ...current.theme, ...(homePayload.data.theme || {}) },
      version: {
        ...current.version,
        ...(homePayload.data.renderVersion ? { active: homePayload.data.renderVersion } : {})
      },
      homeComponents: Array.isArray(homePayload.data.components) ? homePayload.data.components : [],
      ...(homePayload.data.layoutCode ? { publishedLayoutCode: homePayload.data.layoutCode } : {})
    };
  } catch {
    // Public pages must retain the static config.js/default theme when CMS is unavailable.
  }
}

async function bootstrap() {
  await loadEditorPreviewConfig();
  await loadPublishedHomeConfig();
  const { default: App } = await import("./App.vue");
  createApp(App).mount("#app");
}

void bootstrap();
