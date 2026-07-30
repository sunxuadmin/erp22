-- M84: Grant OSS image permissions required by the home page config editor.
-- The ImageUpload component calls /resource/oss/upload and then reads/removes
-- the uploaded object by OSS id, so home config editors need these function perms.

insert ignore into sys_role_menu(role_id, menu_id)
select distinct rm.role_id, oss_menu.menu_id
from sys_role_menu rm
join sys_menu home_edit on home_edit.menu_id = rm.menu_id
join sys_menu oss_menu on oss_menu.menu_id in (1600, 1601, 1603)
join sys_role role_info on role_info.role_id = rm.role_id
where home_edit.perms = 'system:homeConfig:edit'
  and role_info.del_flag = '0';

insert ignore into sys_role_menu(role_id, menu_id)
select role_info.role_id, oss_menu.menu_id
from sys_role role_info
join sys_menu oss_menu on oss_menu.menu_id in (1600, 1601, 1603)
where role_info.role_key in ('admin', 'superadmin', 'super_admin', 'crehn_admin')
  and role_info.del_flag = '0';
