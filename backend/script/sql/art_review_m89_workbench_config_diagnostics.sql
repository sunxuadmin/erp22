-- Diagnose workbench config rows before/after deploying the exact upsert fix.
-- This script is read-only and does not change data.

select tenant_id,
       config_key,
       count(*) as row_count,
       min(config_id) as first_config_id,
       max(config_id) as last_config_id
from sys_config
where config_key in ('crehn.workbench.style', 'crehn.workbench.navbar.title')
group by tenant_id, config_key
having count(*) > 1;

select config_id,
       tenant_id,
       config_key,
       config_value,
       create_time,
       update_time
from sys_config
where config_key in ('crehn.workbench.style', 'crehn.workbench.navbar.title')
order by tenant_id, config_key, config_id;
