-- --------------------------------------------------
-- M10.5 additive per-project exclusions for category-wide review assignments.
-- Existing assignment rows keep their current project scope.
-- --------------------------------------------------

alter table review_assignment
    add column if not exists excluded_project_ids_json text null;
