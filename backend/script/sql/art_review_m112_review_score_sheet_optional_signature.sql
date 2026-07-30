-- --------------------------------------------------
-- M11.2 optional reviewer-signature score-sheet submissions.
--
-- Existing signed sheets retain their image/placement snapshots and become
-- SIGNED through the compatible non-null default below. New optional-signature
-- submissions may leave all three snapshot fields empty as one atomic state.
-- --------------------------------------------------

alter table review_score_sheet_signed
    modify column signature_id bigint null comment 'personal signature record at save time; null for unsigned submission';

alter table review_score_sheet_signed
    modify column signature_oss_id bigint null comment 'immutable PNG OSS snapshot; null for unsigned submission';

alter table review_score_sheet_signed
    modify column signature_placement_json text null comment 'slot-relative x/y/width/height snapshot; null for unsigned submission';

alter table review_score_sheet_signed
    add column if not exists submission_mode varchar(16) not null default 'SIGNED'
        comment 'SIGNED handwritten snapshot or UNSIGNED optional-signature submission';
