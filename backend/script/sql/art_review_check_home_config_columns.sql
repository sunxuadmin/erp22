-- Home config production schema check.
-- Read-only. Run against the production database selected by the mysql command.
--
-- Example:
--   mysql --default-character-set=utf8mb4 -N -B art_review < /opt/crehn/source/framework-ruoyi-vue-plus/script/sql/art_review_check_home_config_columns.sql
--
-- If rows are returned from the "missing_table" or "missing_column" queries,
-- apply the matching migration before checking /system/home-config again:
--   art_review_m80_home_module_builder.sql
--   art_review_m85_home_login_state_config.sql
--   art_review_m86_home_custom_css.sql

select 'missing_table' as issue, expected.table_name, null as column_name
from (
    select 'home_site_config' as table_name
    union all select 'home_page_module'
    union all select 'home_content_card'
) expected
left join information_schema.tables t
    on t.table_schema = database()
   and t.table_name = expected.table_name
where t.table_name is null
order by expected.table_name;

select 'missing_column' as issue, expected.table_name, expected.column_name
from (
    select 'home_site_config' as table_name, 'login_state_config_json' as column_name
    union all select 'home_site_config', 'custom_css'
    union all select 'home_content_card', 'card_type'
    union all select 'home_content_card', 'template_code'
    union all select 'home_content_card', 'slot_key'
    union all select 'home_content_card', 'config_json'
    union all select 'home_page_module', 'module_code'
    union all select 'home_page_module', 'module_title'
    union all select 'home_page_module', 'module_type'
    union all select 'home_page_module', 'template_code'
    union all select 'home_page_module', 'config_json'
) expected
left join information_schema.columns c
    on c.table_schema = database()
   and c.table_name = expected.table_name
   and c.column_name = expected.column_name
where c.column_name is null
order by expected.table_name, expected.column_name;

select 'home_site_config_rows' as metric, count(*) as value
from home_site_config
where config_code = 'portal_home';

select 'home_hero_rows' as metric, count(*) as value
from home_content_card
where section_key = 'home'
  and card_code = 'home_hero'
  and del_flag = '0';

select 'home_cards_by_section' as metric, section_key, count(*) as value
from home_content_card
where del_flag = '0'
group by section_key
order by section_key;
