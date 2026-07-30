import type { ArtListTableAppearanceConfig } from '@/api/crehn/detailDisplay';
import { defaultArtListActionColors, defaultArtListStatusColors, useArtDetailDisplayConfig } from '@/views/crehn/components/artDetailDisplayConfig';

export interface ArtListTableAppearanceLabels {
  statusLabels?: Array<string | undefined>;
  actionLabels?: Array<string | undefined>;
}

interface ArtListTableAppearanceOptions {
  statusLabels?: () => ArtListTableAppearanceLabels['statusLabels'];
  actionLabels?: () => ArtListTableAppearanceLabels['actionLabels'];
}

const longestLabelLength = (labels: Array<string | undefined> | undefined) =>
  Math.max(1, ...(labels || []).map((label) => Array.from(String(label || '').trim()).length));

const defaultColors = {
  headerBackground: '#eef4ff',
  headerTextColor: '#1f3b64',
  textColor: '#334155',
  hoverBackground: '#f8fbff',
  borderColor: '#e8eef6',
  status: defaultArtListStatusColors(),
  action: defaultArtListActionColors()
} as const;

export const resolveArtListTableAppearance = (appearance: ArtListTableAppearanceConfig, labels: ArtListTableAppearanceLabels = {}) => {
  const statusLabelLength = longestLabelLength(labels.statusLabels);
  const actionLabelLength = longestLabelLength(labels.actionLabels);
  const override = appearance.globalColorOverride;
  const statusColors = override ? appearance.statusAppearance.colors : defaultColors.status;
  const actionColors = override ? appearance.rowActionAppearance.colors : defaultColors.action;
  return {
    className: [
      'art-configurable-list-table',
      `is-wrap-${appearance.wrapMode}`,
      {
        'has-horizontal-border': appearance.horizontalBorderVisible,
        'has-vertical-border': appearance.verticalBorderVisible,
        'has-outer-border': appearance.outerBorderVisible,
        'is-header-wrap': appearance.headerWrap,
        'is-striped': appearance.striped,
        'has-global-color-override': appearance.globalColorOverride,
        'is-action-button': appearance.actionStyle === 'button',
        'has-status-border': appearance.statusAppearance.borderVisible,
        'has-status-icon': appearance.statusAppearance.iconVisible,
        'has-uniform-status-width': appearance.statusAppearance.uniformWidth && !!labels.statusLabels,
        'has-action-border': appearance.rowActionAppearance.borderVisible,
        'has-action-icon': appearance.rowActionAppearance.iconVisible,
        'has-uniform-action-width': appearance.rowActionAppearance.uniformWidth && !!labels.actionLabels
      }
    ],
    style: {
      '--art-table-header-background': override ? appearance.headerBackground : defaultColors.headerBackground,
      '--art-table-header-color': override ? appearance.headerTextColor : defaultColors.headerTextColor,
      '--art-table-text-color': override ? appearance.textColor : defaultColors.textColor,
      '--art-table-hover-background': `color-mix(in srgb, ${override ? appearance.hoverBackground : defaultColors.hoverBackground} ${
        appearance.hoverBackgroundOpacity
      }%, transparent)`,
      '--art-table-border-color': override ? appearance.borderColor : defaultColors.borderColor,
      '--art-table-horizontal-border-color': `color-mix(in srgb, ${
        override ? appearance.horizontalBorderColor : defaultColors.borderColor
      } ${appearance.horizontalBorderOpacity}%, transparent)`,
      '--art-table-vertical-border-color': `color-mix(in srgb, ${override ? appearance.verticalBorderColor : defaultColors.borderColor} ${
        appearance.verticalBorderOpacity
      }%, transparent)`,
      '--art-table-outer-border-color': `color-mix(in srgb, ${override ? appearance.outerBorderColor : defaultColors.borderColor} ${
        appearance.outerBorderOpacity
      }%, transparent)`,
      '--art-table-header-font-size': `${appearance.headerFontSize}px`,
      '--art-table-body-font-size': `${appearance.bodyFontSize}px`,
      '--art-list-button-font-size': `${appearance.buttonFontSize}px`,
      '--art-table-row-height': `${appearance.rowHeight}px`,
      '--art-table-radius': `${appearance.borderRadius}px`,
      '--art-status-label-width': `${statusLabelLength}em`,
      '--art-action-label-width': `${actionLabelLength}em`,
      '--art-action-disabled-opacity': appearance.rowActionAppearance.disabledOpacity / 100,
      '--art-status-draft-color': statusColors.draft,
      '--art-status-pending-color': statusColors.pending,
      '--art-status-approved-color': statusColors.approved,
      '--art-status-returned-color': statusColors.returned,
      '--art-status-unscored-color': statusColors.unscored,
      '--art-status-scored-color': statusColors.scored,
      '--art-status-score-draft-color': statusColors.scoreDraft,
      '--art-status-locked-color': statusColors.locked,
      '--art-action-view-color': actionColors.view,
      '--art-action-score-color': actionColors.score,
      '--art-action-edit-color': actionColors.edit,
      '--art-action-return-color': actionColors.return,
      '--art-action-withdraw-color': actionColors.withdraw,
      '--art-action-submit-color': actionColors.submit,
      '--art-action-delete-color': actionColors.delete
    } as Record<string, string | number>
  };
};

export const useArtListTableAppearance = (options: ArtListTableAppearanceOptions = {}) => {
  const { detailDisplayConfig } = useArtDetailDisplayConfig();
  const tableAppearance = computed(() => detailDisplayConfig.value.listTableAppearance);
  const resolvedAppearance = computed(() =>
    resolveArtListTableAppearance(tableAppearance.value, {
      statusLabels: options.statusLabels?.(),
      actionLabels: options.actionLabels?.()
    })
  );
  const tableAppearanceClass = computed(() => resolvedAppearance.value.className);
  const tableAppearanceStyle = computed(() => resolvedAppearance.value.style);
  const actionIcon = (icon: string) => (tableAppearance.value.rowActionAppearance.iconVisible ? icon : undefined);

  return { tableAppearance, tableAppearanceClass, tableAppearanceStyle, actionIcon };
};
