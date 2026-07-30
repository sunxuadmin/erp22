-- Expand system config value storage for the art review workbench style JSON.
-- The style editor stores page, card, calendar, and sidebar CSS variables in
-- crehn.workbench.style; the default value is already close to the historical
-- varchar(500) limit, so custom gradients need a larger value column.

alter table sys_config
    modify column config_value text null comment 'config value';
