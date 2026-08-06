import type { PortalHomeComponent, PortalPageLayout } from '@/api/crehn/cms';

const cloneComponents = (items: PortalHomeComponent[]) => {
  return JSON.parse(JSON.stringify(items)) as PortalHomeComponent[];
};

export const parsePortalLayoutComponents = (componentJson?: string) => {
  if (!componentJson) return undefined;
  try {
    const parsed = JSON.parse(componentJson) as unknown;
    return Array.isArray(parsed) ? (parsed as PortalHomeComponent[]) : undefined;
  } catch {
    return undefined;
  }
};

export const createPortalLayoutComponentDraft = (layout: PortalPageLayout | undefined, currentComponents: PortalHomeComponent[]) => {
  const savedComponents = parsePortalLayoutComponents(layout?.componentJson);
  if (layout && !savedComponents) return undefined;
  const source = layout?.active ? currentComponents : savedComponents;
  return cloneComponents(source || currentComponents);
};

export const clonePortalLayoutComponents = cloneComponents;
