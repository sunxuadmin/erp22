-- --------------------------------------------------
-- M11.7 additive persisted project-tag filters for review assignments.
-- Existing assignments have no filter and therefore retain their all-project scope.
-- --------------------------------------------------

alter table review_assignment
    add column if not exists project_filter_json text null comment 'server-enforced school type and dynamic form field filter json';
