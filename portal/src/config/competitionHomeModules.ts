export interface CompetitionHomeModule {
  componentKey: string;
  componentType: string;
  config: Record<string, string | number | boolean>;
  gridX: number;
  gridY: number;
  gridW: number;
  gridH: number;
  sortOrder: number;
}

export const competitionModuleTypes = [
  "competition-nav",
  "competition-hero",
  "competition-key-facts",
  "competition-tracks",
  "competition-art-tech",
  "competition-journey",
  "competition-file-specs",
  "competition-timeline",
  "competition-notice-downloads",
  "competition-contact",
  "competition-footer"
] as const;

const allowedTypes = new Set<string>(competitionModuleTypes);

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === "object" && !Array.isArray(value);
}

function numberInRange(value: unknown, fallback: number, min: number, max: number) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? Math.min(max, Math.max(min, parsed)) : fallback;
}

function parseConfig(value: unknown): Record<string, string | number | boolean> {
  if (typeof value !== "string") return {};
  try {
    const parsed = JSON.parse(value) as unknown;
    if (!isRecord(parsed)) return {};
    return Object.fromEntries(
      Object.entries(parsed).filter(([, item]) =>
        typeof item === "string" || typeof item === "number" || typeof item === "boolean"
      )
    ) as Record<string, string | number | boolean>;
  } catch {
    return {};
  }
}

function normalizeModule(value: unknown, index: number): CompetitionHomeModule | null {
  if (!isRecord(value) || value.enabled === false || !allowedTypes.has(String(value.componentType || ""))) {
    return null;
  }
  const componentKey = String(value.componentKey || "").trim();
  if (!componentKey) return null;
  return {
    componentKey,
    componentType: String(value.componentType),
    config: parseConfig(value.configJson),
    gridX: numberInRange(value.gridX, 0, 0, 11),
    gridY: numberInRange(value.gridY, index, 0, 10000),
    gridW: numberInRange(value.gridW, 12, 1, 12),
    gridH: numberInRange(value.gridH, 1, 1, 100),
    sortOrder: numberInRange(value.sortOrder, index, 0, 10000)
  };
}

const runtimeComponents =
  typeof window === "undefined" ? undefined : window.CREHN_PORTAL_CONFIG?.homeComponents;

export const hasPublishedCompetitionHomeSnapshot = Array.isArray(runtimeComponents);
const publishedRuntimeComponents = Array.isArray(runtimeComponents) ? runtimeComponents : [];

export const competitionHomeModules = publishedRuntimeComponents
  .map(normalizeModule)
  .filter((item): item is CompetitionHomeModule => item !== null)
  .filter((item, index, items) => items.findIndex((other) => other.componentKey === item.componentKey) === index)
  .sort((a, b) => a.gridY - b.gridY || a.gridX - b.gridX || a.sortOrder - b.sortOrder);
