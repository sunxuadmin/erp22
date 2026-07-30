import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch, type ComputedRef, type Ref } from 'vue';

export interface ArtResizableColumn {
  key: string;
  width: number;
  minWidth: number;
  visible?: boolean;
  fixed?: 'right' | 'left' | boolean;
  resizable?: boolean;
}

const protectedColumnKeys = new Set(['selection', 'serial', 'status', 'scoreStatus', 'actions']);
const isProtectedColumn = (column: Pick<ArtResizableColumn, 'key'>) => protectedColumnKeys.has(column.key);

interface ArtTableColumnWidthOptions {
  columns: ComputedRef<ArtResizableColumn[]>;
  storageKey: ComputedRef<string>;
  fitContainer?: boolean;
  persistLocalWidths?: boolean;
}

export const resolveArtTableFitWidths = (columns: ArtResizableColumn[], preferredWidths: Record<string, number>, containerWidth: number) => {
  const visibleColumns = columns.filter((column) => column.visible !== false);
  if (!visibleColumns.length) return {};
  const available = Math.max(1, Math.round(containerWidth) - 2);
  const floors = Object.fromEntries(
    visibleColumns.map((column) => {
      const preferred = Math.max(1, Number(preferredWidths[column.key]) || Number(column.width) || 1);
      const floor = isProtectedColumn(column) ? Math.max(1, Number(column.minWidth) || 1) : Math.min(preferred, 24);
      return [column.key, floor];
    })
  );
  const floorTotal = Object.values(floors).reduce((sum, width) => sum + width, 0);
  if (floorTotal >= available) {
    const protectedColumns = visibleColumns.filter(isProtectedColumn);
    const flexibleColumns = visibleColumns.filter((column) => !isProtectedColumn(column));
    const protectedTotal = protectedColumns.reduce((sum, column) => sum + floors[column.key], 0);
    const protectedScale = protectedTotal > available ? available / protectedTotal : 1;
    const resolved: Record<string, number> = Object.fromEntries(
      protectedColumns.map((column) => [column.key, Math.max(1, Math.round(floors[column.key] * protectedScale))])
    );
    let remaining = Math.max(0, available - Object.values(resolved).reduce((sum, width) => sum + width, 0));
    flexibleColumns.forEach((column, index) => {
      const width = index === flexibleColumns.length - 1 ? remaining : Math.max(1, Math.floor(remaining / (flexibleColumns.length - index)));
      resolved[column.key] = width;
      remaining = Math.max(0, remaining - width);
    });
    return resolved;
  }
  const extraAvailable = available - floorTotal;
  const extraPreferred = visibleColumns.reduce(
    (sum, column) => sum + Math.max(0, (Number(preferredWidths[column.key]) || Number(column.width) || 1) - floors[column.key]),
    0
  );
  let assigned = 0;
  return Object.fromEntries(
    visibleColumns.map((column, index) => {
      const preferredExtra = Math.max(0, (Number(preferredWidths[column.key]) || Number(column.width) || 1) - floors[column.key]);
      const remainingSlots = visibleColumns.length - index - 1;
      const proposedWidth =
        index === visibleColumns.length - 1
          ? Math.max(1, available - assigned)
          : Math.max(1, Math.round(floors[column.key] + (extraPreferred > 0 ? (extraAvailable * preferredExtra) / extraPreferred : 0)));
      const width = Math.min(proposedWidth, Math.max(1, available - assigned - remainingSlots));
      assigned += width;
      return [column.key, width];
    })
  );
};

export const useArtTableColumnWidths = ({
  columns,
  storageKey,
  fitContainer = false,
  persistLocalWidths: persistWidths = true
}: ArtTableColumnWidthOptions) => {
  const tableShellRef = ref<HTMLElement>();
  const containerWidth = ref(0);
  const localWidths = ref<Record<string, number>>({});
  let resizeObserver: ResizeObserver | undefined;

  const normalizedWidth = (column: ArtResizableColumn, value: unknown) => {
    const numeric = Number(value);
    const fallback = Number(column.width) || 120;
    const min = fitContainer && !isProtectedColumn(column) ? 24 : Number(column.minWidth) || 48;
    return Math.min(800, Math.max(min, Number.isFinite(numeric) ? Math.round(numeric) : fallback));
  };

  const preferredWidths = computed(() =>
    Object.fromEntries(
      columns.value.map((column) => [
        column.key,
        normalizedWidth(column, column.fixed ? column.width : (localWidths.value[column.key] ?? column.width))
      ])
    )
  );

  const effectiveWidths = computed(() => {
    const visibleColumns = columns.value.filter((column) => column.visible !== false);
    const preferred = preferredWidths.value;
    if (!containerWidth.value || !visibleColumns.length) return preferred;
    if (fitContainer) {
      return resolveArtTableFitWidths(visibleColumns, preferred, containerWidth.value);
    }
    const fixedColumns = visibleColumns.filter((column) => Boolean(column.fixed));
    const flexibleColumns = visibleColumns.filter((column) => !column.fixed);
    const fixedWidth = fixedColumns.reduce((sum, column) => sum + preferred[column.key], 0);
    const flexiblePreferred = flexibleColumns.reduce((sum, column) => sum + preferred[column.key], 0);
    const available = Math.max(0, containerWidth.value - fixedWidth - 2);
    const scale = flexiblePreferred > 0 ? Math.min(1, available / flexiblePreferred) : 1;
    const result: Record<string, number> = { ...preferred };
    flexibleColumns.forEach((column) => {
      result[column.key] = Math.max(Number(column.minWidth) || 48, Math.round(preferred[column.key] * scale));
    });
    return result;
  });

  const loadLocalWidths = () => {
    const allowed = new Map(columns.value.map((column) => [column.key, column]));
    if (!persistWidths) {
      localWidths.value = {};
      return;
    }
    try {
      const parsed = JSON.parse(window.localStorage.getItem(storageKey.value) || '{}') as Record<string, unknown>;
      localWidths.value = Object.fromEntries(
        Object.entries(parsed)
          .filter(([key]) => allowed.has(key))
          .map(([key, value]) => [key, normalizedWidth(allowed.get(key)!, value)])
      );
    } catch {
      localWidths.value = {};
    }
  };

  const persistLocalWidths = () => {
    if (!persistWidths) return;
    try {
      window.localStorage.setItem(storageKey.value, JSON.stringify(localWidths.value));
    } catch {
      // Browsers may deny local storage; the current drag still remains active.
    }
  };

  const handleColumnResize = (newWidth: number, _oldWidth: number, column: { columnKey?: string; property?: string }) => {
    const key = String(column?.columnKey || column?.property || '');
    const config = columns.value.find((item) => item.key === key);
    if (!config || config.resizable === false || config.fixed) return;
    let preferredWidth = newWidth;
    if (fitContainer && containerWidth.value > 0) {
      const flexibleColumns = columns.value.filter((item) => item.visible !== false && !item.fixed);
      const fixedWidth = columns.value
        .filter((item) => item.visible !== false && Boolean(item.fixed))
        .reduce((sum, item) => sum + preferredWidths.value[item.key], 0);
      const otherPreferredWidth = flexibleColumns.filter((item) => item.key !== key).reduce((sum, item) => sum + preferredWidths.value[item.key], 0);
      const available = Math.max(1, containerWidth.value - fixedWidth - 2);
      const desiredWidth = Math.max(1, Math.min(newWidth, available - 1));
      if (otherPreferredWidth > 0 && desiredWidth < available) {
        preferredWidth = (desiredWidth * otherPreferredWidth) / (available - desiredWidth);
      }
    }
    localWidths.value = { ...localWidths.value, [key]: normalizedWidth(config, preferredWidth) };
    persistLocalWidths();
  };

  const restoreColumnWidths = () => {
    localWidths.value = {};
    if (!persistWidths) return;
    try {
      window.localStorage.removeItem(storageKey.value);
    } catch {
      // Ignore unavailable local storage.
    }
  };

  const observeContainer = async () => {
    await nextTick();
    resizeObserver?.disconnect();
    const element = tableShellRef.value;
    if (!element) return;
    containerWidth.value = element.clientWidth;
    if (typeof ResizeObserver === 'undefined') return;
    resizeObserver = new ResizeObserver((entries) => {
      containerWidth.value = Math.round(entries[0]?.contentRect.width || element.clientWidth);
    });
    resizeObserver.observe(element);
  };

  watch(storageKey, loadLocalWidths);
  watch(
    () => columns.value.map((column) => `${column.key}:${column.width}:${column.minWidth}:${column.visible}`).join('|'),
    () => {
      loadLocalWidths();
      void observeContainer();
    }
  );
  onMounted(() => {
    loadLocalWidths();
    void observeContainer();
  });
  onBeforeUnmount(() => resizeObserver?.disconnect());

  return {
    tableShellRef: tableShellRef as Ref<HTMLElement | undefined>,
    columnWidth: (key: string) => effectiveWidths.value[key],
    handleColumnResize,
    restoreColumnWidths
  };
};
