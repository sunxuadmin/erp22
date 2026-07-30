#!/bin/bash
set -Eeuo pipefail

if [[ "${CREHN_RUNTIME_ENV:-}" != "test" ]]; then
  exit 0
fi

: "${CREHN_MINIO_ROOT_USER:?CREHN_MINIO_ROOT_USER is required for TEST}"
: "${CREHN_MINIO_ROOT_PASSWORD:?CREHN_MINIO_ROOT_PASSWORD is required for TEST}"
: "${CREHN_MINIO_BUCKET:?CREHN_MINIO_BUCKET is required for TEST}"
: "${CREHN_MINIO_PUBLIC_DOMAIN:?CREHN_MINIO_PUBLIC_DOMAIN is required for TEST}"

safe_value='^[A-Za-z0-9._~!@#%^*+=:/-]{3,160}$'
for value in \
  "${CREHN_MINIO_ROOT_USER}" \
  "${CREHN_MINIO_ROOT_PASSWORD}" \
  "${CREHN_MINIO_BUCKET}" \
  "${CREHN_MINIO_PUBLIC_DOMAIN}"; do
  if [[ ! "${value}" =~ ${safe_value} ]]; then
    echo "TEST MinIO configuration contains unsupported characters" >&2
    exit 1
  fi
done

MYSQL_PWD="${MARIADB_ROOT_PASSWORD}" mariadb --protocol=tcp -h 127.0.0.1 -uroot "${MARIADB_DATABASE}" <<EOSQL
update sys_oss_config
set config_key = 'crehn-test-minio',
    access_key = '${CREHN_MINIO_ROOT_USER}',
    secret_key = '${CREHN_MINIO_ROOT_PASSWORD}',
    bucket_name = '${CREHN_MINIO_BUCKET}',
    prefix = 'crehn-test',
    endpoint = 'minio:9000',
    domain = '${CREHN_MINIO_PUBLIC_DOMAIN}',
    is_https = 'N',
    status = '1',
    access_policy = '0',
    update_time = sysdate()
where oss_config_id = 1 and tenant_id = '000000';

update sys_oss_config
set status = '0', update_time = sysdate()
where tenant_id = '000000' and oss_config_id <> 1;
EOSQL
