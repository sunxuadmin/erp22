-- --------------------------------------------------
-- M10.3 additive school scope for audit and score assignments.
-- Existing assignment rows remain unrestricted by school.
-- --------------------------------------------------

alter table audit_assignment
    add column if not exists school_scope_mode varchar(16) not null default 'all';

alter table audit_assignment
    add column if not exists school_ids_json text null;

alter table review_assignment
    add column if not exists school_scope_mode varchar(16) not null default 'all';

alter table review_assignment
    add column if not exists school_ids_json text null;
