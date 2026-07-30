#!/bin/bash
set -Eeuo pipefail

: "${CREHN_BOOTSTRAP_ADMIN_USERNAME:?CREHN_BOOTSTRAP_ADMIN_USERNAME is required}"
: "${CREHN_BOOTSTRAP_ADMIN_PASSWORD_HASH:?CREHN_BOOTSTRAP_ADMIN_PASSWORD_HASH is required}"

if [[ ! "${CREHN_BOOTSTRAP_ADMIN_USERNAME}" =~ ^[A-Za-z0-9_.-]{4,30}$ ]]; then
  echo "CREHN bootstrap administrator username format is invalid" >&2
  exit 1
fi

if [[ ! "${CREHN_BOOTSTRAP_ADMIN_PASSWORD_HASH}" =~ ^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$ ]]; then
  echo "CREHN bootstrap administrator password must be a BCrypt hash" >&2
  exit 1
fi

MYSQL_PWD="${MARIADB_ROOT_PASSWORD}" mariadb --protocol=tcp -h 127.0.0.1 -uroot "${MARIADB_DATABASE}" <<EOSQL
set @bootstrap_user_id = (select coalesce(max(user_id), 0) + 1 from sys_user);
insert into sys_user
(user_id, tenant_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex,
 avatar, password, status, del_flag, login_ip, login_date, create_dept, create_by,
 create_time, update_by, update_time, remark)
values
(@bootstrap_user_id, '000000', null, '${CREHN_BOOTSTRAP_ADMIN_USERNAME}', 'CREHN超级管理员',
 'sys_user', '', '', '2', null, '${CREHN_BOOTSTRAP_ADMIN_PASSWORD_HASH}', '0', '0', '',
 null, null, null, sysdate(), null, null, '部署时创建的首个管理员');

insert into sys_user_role(user_id, role_id)
select @bootstrap_user_id, role_id
from sys_role
where tenant_id = '000000' and role_key = 'superadmin' and del_flag = '0'
order by role_id
limit 1;
EOSQL
