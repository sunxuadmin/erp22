-- Move the interim clean white-blue default to the clean blue-white government-style default.
-- This targets only the previously shipped clean default, leaving customized
-- workbench style JSON untouched.

update sys_config
set config_value = '{"enabled":true,"preset":"sky","pageBackground":"linear-gradient(180deg,#f5f9ff 0%,#f8fbff 44%,#ffffff 100%)","cardBackground":"#ffffff","cardBorder":"#d8e6f5","accentColor":"#2563eb","headingColor":"#0f2f5f","calendarBackground":"linear-gradient(180deg,#2563eb 0%,#0f62b9 100%)","shadow":"0 4px 14px rgba(37,99,235,0.05)","sBg":"#f4f8ff","sBl":0,"sIt":"transparent","sHv":"#e7f0ff","sBd":"#d9e6f6"}',
    update_time = sysdate(),
    remark = 'Workbench home background/card/sidebar style config JSON'
where config_key = 'crehn.workbench.style'
  and config_value like '%"preset":"sky"%'
  and config_value like '%#f6fbff%'
  and config_value like '%#087fbb%'
  and config_value like '%"sBg":"#f8fbff"%';
