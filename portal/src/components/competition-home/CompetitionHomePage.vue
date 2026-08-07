<script setup lang="ts">
import { computed } from "vue";
import { activeVersion } from "../../config/entries";
import type { CompetitionHomeVersion } from "../../config/competitionHome";
import { competitionHomeModules, hasPublishedCompetitionHomeSnapshot } from "../../config/competitionHomeModules";
import CompetitionHomeCanvas from "./CompetitionHomeCanvas.vue";
import { createCompetitionHomeFallback } from "./competitionHomeFallback";

const homeVersion: CompetitionHomeVersion = activeVersion === "v2" ? "v2" : "v1";
const renderedModules = computed(() => hasPublishedCompetitionHomeSnapshot ? competitionHomeModules : createCompetitionHomeFallback(homeVersion));

function handleVersionChange(version: CompetitionHomeVersion) {
  if (version === homeVersion) return;
  const url = new URL(window.location.href);
  url.searchParams.set("version", version);
  window.location.assign(url.toString());
}
</script>

<template>
  <CompetitionHomeCanvas
    :modules="renderedModules"
    :version="homeVersion"
    @change-version="handleVersionChange"
  />
</template>
