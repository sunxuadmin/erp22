-- --------------------------------------------------
-- Art Review M6.10 school result visibility by message only.
-- Safe to rerun after M6.0 result publishing and message scripts.
-- --------------------------------------------------

delete rm
from sys_role_menu rm
join sys_role r on r.role_id = rm.role_id
where r.role_key = 'crehn_school'
  and rm.menu_id in (17940, 17945);
