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

async function bootstrap() {
  await loadEditorPreviewConfig();
  const { default: App } = await import("./App.vue");
  createApp(App).mount("#app");
}

void bootstrap();
