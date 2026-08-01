FROM mariadb:11.4.5
ARG CREHN_VERSION=local
ARG CREHN_SOURCE_REVISION=UNVERSIONED
LABEL org.opencontainers.image.title="CREHN Database Runtime" \
      org.opencontainers.image.version="${CREHN_VERSION}" \
      org.opencontainers.image.revision="${CREHN_SOURCE_REVISION}"
COPY deploy/db/migration-manifest.json /opt/crehn/migration-manifest.json
COPY backend/script/sql/ry_vue_5.X.sql /opt/crehn/init/001_framework.sql
COPY deploy/db/bootstrap-admin.sh /opt/crehn/init/002_bootstrap_admin.sh
COPY deploy/db/bootstrap-test-oss.sh /opt/crehn/init/003_bootstrap_test_oss.sh
COPY backend/script/sql/art_review_core.sql /opt/crehn/init/010_business_core.sql
COPY backend/script/sql/art_review_config_rules.sql /opt/crehn/init/011_business_rules.sql
COPY deploy/db/migrations/V001__crehn_identity_workflow_cms.sql /opt/crehn/init/012_crehn_identity_workflow_cms.sql
COPY deploy/db/migrations/V002__crehn_role_keys.sql /opt/crehn/init/013_crehn_roles.sql
COPY backend/script/sql/art_review_menu.sql /opt/crehn/init/020_business_menu.sql
COPY backend/script/sql/art_review_config_menu.sql /opt/crehn/init/021_config_menu.sql
COPY backend/script/sql/art_review_m47_audit_message.sql /opt/crehn/init/030_messages.sql
COPY backend/script/sql/art_review_m50_review.sql /opt/crehn/init/031_review.sql
COPY backend/script/sql/art_review_m60_result_publish.sql /opt/crehn/init/032_results.sql
COPY backend/script/sql/art_review_m61_award_export_score.sql /opt/crehn/init/033_awards.sql
COPY backend/script/sql/art_review_m68_recycle_audit_scope.sql /opt/crehn/init/034_audit_scope.sql
COPY backend/script/sql/art_review_m70_school_project_group_menu.sql /opt/crehn/init/034a_school_project_menu.sql
COPY backend/script/sql/art_review_m75_workbench_layout.sql /opt/crehn/init/035_workbench.sql
COPY backend/script/sql/art_review_m97_review_score_visibility_schema.sql /opt/crehn/init/040_score_visibility.sql
COPY backend/script/sql/art_review_m100_review_score_sheet_template_schema.sql /opt/crehn/init/041_score_sheet.sql
COPY backend/script/sql/art_review_m101_review_score_sheet_library_schema.sql /opt/crehn/init/042_score_library.sql
COPY backend/script/sql/art_review_m103_assignment_school_scope_schema.sql /opt/crehn/init/043_assignment_scope.sql
COPY backend/script/sql/art_review_m105_review_assignment_project_exclusion_schema.sql /opt/crehn/init/044_assignment_exclusion.sql
COPY backend/script/sql/art_review_m106_review_score_sheet_signature_schema.sql /opt/crehn/init/045_signatures.sql
COPY backend/script/sql/art_review_m107_review_score_sheet_withdrawal_schema.sql /opt/crehn/init/046_withdrawals.sql
COPY backend/script/sql/art_review_m109_review_score_warning_config_schema.sql /opt/crehn/init/047_score_warning.sql
COPY backend/script/sql/art_review_m111_review_score_summary_custom_formula_schema.sql /opt/crehn/init/048_score_formula.sql
COPY backend/script/sql/art_review_m112_review_score_sheet_optional_signature.sql /opt/crehn/init/049_optional_signature.sql
COPY backend/script/sql/art_review_m117_review_assignment_project_filter_schema.sql /opt/crehn/init/050_project_filter.sql
COPY backend/script/sql/art_review_m73_flatten_school_project_menu.sql /opt/crehn/init/060_flatten_project_menu.sql
COPY backend/script/sql/art_review_m74_project_view_menu.sql /opt/crehn/init/061_project_view_menu.sql
COPY backend/script/sql/art_review_m76_menu_icons_i18n.sql /opt/crehn/init/062_menu_icons.sql
COPY backend/script/sql/art_review_m82_activity_menu_name.sql /opt/crehn/init/063_activity_menu_name.sql
COPY backend/script/sql/art_review_m83_hide_project_view_menu.sql /opt/crehn/init/064_hide_project_view.sql
COPY backend/script/sql/art_review_m93_report_progress_menu.sql /opt/crehn/init/065_report_progress_menu.sql
COPY backend/script/sql/art_review_m100_review_score_sheet_menu.sql /opt/crehn/init/066_score_sheet_menu.sql
COPY backend/script/sql/art_review_m108_review_score_sheet_signed_sheet_menu.sql /opt/crehn/init/067_signed_sheet_menu.sql
COPY backend/script/sql/art_review_m110_review_management_signature_admin.sql /opt/crehn/init/068_signature_admin_menu.sql
COPY backend/script/sql/art_review_m111_review_score_summary_export_menu.sql /opt/crehn/init/069_score_summary_export_menu.sql
COPY backend/script/sql/art_review_m113_review_management_menu_reconcile.sql /opt/crehn/init/070_review_menu_reconcile.sql
COPY backend/script/sql/art_review_m114_review_management_menu_hierarchy.sql /opt/crehn/init/071_review_menu_hierarchy.sql
COPY backend/script/sql/art_review_m115_review_management_menu_full_hierarchy.sql /opt/crehn/init/072_review_menu_full_hierarchy.sql
COPY backend/script/sql/art_review_m116_detail_display_config_permission.sql /opt/crehn/init/073_detail_display_permission.sql
COPY backend/script/sql/art_review_m118_review_workbench_menu_name.sql /opt/crehn/init/074_review_workbench_name.sql
COPY deploy/db/migrations/V003__crehn_legacy_home_schema.sql /opt/crehn/init/090_crehn_legacy_home_schema.sql
COPY deploy/db/migrations/V004__crehn_menu_role_grants.sql /opt/crehn/init/091_crehn_menu_role_grants.sql
COPY deploy/db/migrations/V005__crehn_workbench_role_key.sql /opt/crehn/init/092_crehn_workbench_role_key.sql
RUN chmod 0755 /opt/crehn/init/*.sh
