#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

readonly EXIT_BLOCKED=3
readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
readonly BASE_COMPOSE="${DEPLOY_DIR}/compose.yml"
readonly TEST_COMPOSE="${DEPLOY_DIR}/compose.test.yml"
readonly PROD_COMPOSE="${DEPLOY_DIR}/compose.prod.yml"

ACTION="${1:-}"
if [[ -n "${ACTION}" ]]; then
  shift
fi

ENV_FILE=""
APPLY="false"
CONFIRMATION=""
RELEASE_MANIFEST=""
BACKUP_ID=""
DATABASE_OPERATION="plan"
PREFLIGHT_PROJECT=""
PREFLIGHT_ROOT=""
PREFLIGHT_PORTS=""
PREFLIGHT_PROTECTED_PROJECT=""
PREFLIGHT_PROTECTED_WEB_URL=""
PREFLIGHT_PROTECTED_STORAGE_URL=""

status() {
  local level="$1"
  shift
  printf '[%s] %s\n' "${level}" "$*"
}

fail() {
  status FAILED "$*" >&2
  exit 1
}

blocked() {
  status BLOCKED "$*" >&2
  exit "${EXIT_BLOCKED}"
}

usage() {
  cat <<'EOF'
Usage:
  crehn-deploy.sh preflight-local [preflight options]
  crehn-deploy.sh build-local --env-file FILE [--apply --confirmation TEXT]
  crehn-deploy.sh deploy-local --env-file FILE --release-manifest FILE --apply --confirmation TEXT
  crehn-deploy.sh verify-local --env-file FILE
  crehn-deploy.sh preflight-prod [preflight options]
  crehn-deploy.sh deploy-prod --env-file FILE --release-manifest FILE --apply --confirmation TEXT
  crehn-deploy.sh database --env-file FILE [--database-operation plan|initialize]
                           [--apply --confirmation TEXT]
  crehn-deploy.sh rollback --env-file FILE --backup-id ID --apply --confirmation TEXT
  crehn-deploy.sh backup-maintenance --env-file FILE [--apply --confirmation TEXT]

Preflight options:
  --project-name NAME
  --deploy-root /srv/crehn-test|/srv/crehn-prod
  --ports comma-separated-port-list
  --protected-project NAME
  --protected-web-url URL
  --protected-storage-url URL

Mutating actions do nothing unless both --apply and the action-specific
confirmation text are supplied. Database migration SQL is intentionally not
accepted by this entry until a reviewed migration manifest is supplied.
EOF
}

while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --env-file) ENV_FILE="${2:-}"; shift 2 ;;
    --apply) APPLY="true"; shift ;;
    --confirmation) CONFIRMATION="${2:-}"; shift 2 ;;
    --release-manifest) RELEASE_MANIFEST="${2:-}"; shift 2 ;;
    --backup-id) BACKUP_ID="${2:-}"; shift 2 ;;
    --database-operation) DATABASE_OPERATION="${2:-}"; shift 2 ;;
    --project-name) PREFLIGHT_PROJECT="${2:-}"; shift 2 ;;
    --deploy-root) PREFLIGHT_ROOT="${2:-}"; shift 2 ;;
    --ports) PREFLIGHT_PORTS="${2:-}"; shift 2 ;;
    --protected-project) PREFLIGHT_PROTECTED_PROJECT="${2:-}"; shift 2 ;;
    --protected-web-url) PREFLIGHT_PROTECTED_WEB_URL="${2:-}"; shift 2 ;;
    --protected-storage-url) PREFLIGHT_PROTECTED_STORAGE_URL="${2:-}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) fail "Unknown argument: $1" ;;
  esac
done

trap 'status FAILED "Action ${ACTION:-unknown} stopped before completion"' ERR

require_command() {
  command -v "$1" >/dev/null 2>&1 || blocked "Required command is unavailable: $1"
}

validate_project_name() {
  [[ "$1" =~ ^crehn-(test|prod|stage)$ ]] ||
    blocked "Compose project must be crehn-test, crehn-stage, or crehn-prod"
}

validate_deploy_root() {
  [[ "$1" =~ ^/srv/crehn-(test|prod|stage)$ ]] ||
    blocked "Deployment root must be an explicit /srv/crehn-* project directory"
}

env_value_from_file() {
  local key="$1"
  local file="$2"
  local line value
  line="$(grep -m1 -E "^${key}=" "${file}" 2>/dev/null || true)"
  [[ -n "${line}" ]] || return 1
  value="${line#*=}"
  value="${value%$'\r'}"
  if [[ "${value}" =~ ^\".*\"$ || "${value}" =~ ^\'.*\'$ ]]; then
    value="${value:1:${#value}-2}"
  fi
  printf '%s' "${value}"
}

env_value() {
  env_value_from_file "$1" "${ENV_FILE}"
}

require_env_value() {
  local name="$1"
  local value
  value="$(env_value "${name}" || true)"
  [[ -n "${value}" ]] || blocked "Runtime environment is missing ${name}"
  [[ "${value}" != *"replace-with"* ]] || blocked "Runtime environment still contains a placeholder for ${name}"
}

load_runtime_context() {
  [[ -n "${ENV_FILE}" ]] || blocked "--env-file is required for ${ACTION}"
  [[ -f "${ENV_FILE}" ]] || blocked "Runtime environment file does not exist: ${ENV_FILE}"

  COMPOSE_PROJECT="$(env_value CREHN_COMPOSE_PROJECT_NAME || true)"
  RUNTIME_ENV="$(env_value CREHN_RUNTIME_ENV || true)"
  DEPLOY_ROOT="$(env_value CREHN_DEPLOY_ROOT || true)"
  DATA_ROOT="$(env_value CREHN_DATA_ROOT || true)"
  RELEASE_ROOT="$(env_value CREHN_RELEASE_ROOT || true)"
  BACKUP_ROOT="$(env_value CREHN_BACKUP_ROOT || true)"
  EVIDENCE_ROOT="$(env_value CREHN_EVIDENCE_ROOT || true)"
  STATE_ROOT="$(env_value CREHN_STATE_ROOT || true)"
  VERSION="$(env_value CREHN_VERSION || true)"
  SOURCE_REVISION="$(env_value CREHN_SOURCE_REVISION || true)"

  validate_project_name "${COMPOSE_PROJECT}"
  validate_deploy_root "${DEPLOY_ROOT}"
  for path_value in "${DATA_ROOT}" "${RELEASE_ROOT}" "${BACKUP_ROOT}" "${EVIDENCE_ROOT}" "${STATE_ROOT}"; do
    [[ "${path_value}" == "${DEPLOY_ROOT}/"* ]] ||
      blocked "All runtime paths must remain below ${DEPLOY_ROOT}"
  done
  [[ "${RUNTIME_ENV}" == "test" || "${RUNTIME_ENV}" == "prod" ]] ||
    blocked "CREHN_RUNTIME_ENV must be test or prod"
  [[ "${VERSION}" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]+)?$ ]] ||
    blocked "CREHN_VERSION is not an immutable release-style tag"
  [[ "${SOURCE_REVISION}" =~ ^[0-9a-fA-F]{40,64}$ ]] ||
    blocked "CREHN_SOURCE_REVISION must be a full Git revision, not UNVERSIONED or an abbreviation"

  if [[ "${RUNTIME_ENV}" == "test" ]]; then
    OVERLAY_COMPOSE="${TEST_COMPOSE}"
  else
    OVERLAY_COMPOSE="${PROD_COMPOSE}"
  fi
  ACTIVE_ENV_FILE="${DEPLOY_ROOT}/runtime/${RUNTIME_ENV}.env"
  COMPOSE=(docker compose --env-file "${ENV_FILE}" -p "${COMPOSE_PROJECT}" -f "${BASE_COMPOSE}" -f "${OVERLAY_COMPOSE}")
}

require_runtime_secrets() {
  local common=(
    CREHN_DB_NAME CREHN_DB_USERNAME CREHN_DB_PASSWORD CREHN_DB_ROOT_PASSWORD
    CREHN_BOOTSTRAP_ADMIN_USERNAME CREHN_BOOTSTRAP_ADMIN_PASSWORD_HASH
    CREHN_REDIS_IMAGE CREHN_REDIS_PASSWORD CREHN_JWT_SECRET
    CREHN_MONITOR_USERNAME CREHN_MONITOR_PASSWORD
  )
  local name
  for name in "${common[@]}"; do
    require_env_value "${name}"
  done
  if [[ "${RUNTIME_ENV}" == "test" ]]; then
    for name in CREHN_MINIO_IMAGE CREHN_MINIO_MC_IMAGE CREHN_MINIO_ROOT_USER \
      CREHN_MINIO_ROOT_PASSWORD CREHN_MINIO_BUCKET CREHN_MINIO_PUBLIC_DOMAIN; do
      require_env_value "${name}"
    done
  else
    for name in ALIYUN_OSS_CONFIG_KEY ALIYUN_OSS_ACCESS_KEY ALIYUN_OSS_SECRET_KEY \
      ALIYUN_OSS_BUCKET ALIYUN_OSS_ENDPOINT ALIYUN_OSS_REGION; do
      require_env_value "${name}"
    done
  fi
}

check_env_permissions() {
  local mode owner
  mode="$(stat -c '%a' "${ENV_FILE}")"
  owner="$(stat -c '%U:%G' "${ENV_FILE}")"
  [[ "${mode}" == "600" && "${owner}" == "root:root" ]] ||
    blocked "Runtime environment must be root:root:600; current=${owner}:${mode}"
}

require_apply() {
  local expected="$1"
  [[ "${APPLY}" == "true" ]] || blocked "Dry run only; add --apply after separate authorization"
  [[ "${CONFIRMATION}" == "${expected}" ]] ||
    blocked "Confirmation mismatch; expected ${expected}"
  [[ "$(id -u)" -eq 0 ]] || blocked "Mutating deployment actions must run as root"
}

acquire_project_lock() {
  require_command flock
  exec 8>"/run/lock/${COMPOSE_PROJECT}.operation.lock"
  flock -n 8 || blocked "Another ${COMPOSE_PROJECT} operation is already running"
}

check_docker_access() {
  require_command docker
  docker info >/dev/null 2>&1 || blocked "Docker daemon is unavailable to this user"
  docker compose version >/dev/null 2>&1 || blocked "Docker Compose v2 is unavailable"
}

host_snapshot() {
  status PASS "host=$(hostname) arch=$(uname -m) kernel=$(uname -r)"
  awk '
    /^MemTotal:/ {mem=$2}
    /^SwapTotal:/ {swap=$2}
    END {printf "[PASS] memory_mib=%d swap_mib=%d\n", mem/1024, swap/1024}
  ' /proc/meminfo
  df -Pm / | awk 'NR==2 {printf "[PASS] root_free_mib=%s root_use_percent=%s\n", $4, $5}'
  docker version --format '[PASS] docker_server={{.Server.Version}}'
  docker compose version --short | sed 's/^/[PASS] compose_version=/'
}

check_build_capacity() {
  local mem_kib swap_kib free_mib
  mem_kib="$(awk '/^MemTotal:/ {print $2}' /proc/meminfo)"
  swap_kib="$(awk '/^SwapTotal:/ {print $2}' /proc/meminfo)"
  free_mib="$(df -Pm / | awk 'NR==2 {print $4}')"
  if ! {
    [[ "${mem_kib}" -ge 7864320 && "${swap_kib}" -ge 1572864 ]] ||
      [[ "${mem_kib}" -ge 5767168 && "${swap_kib}" -ge 3670016 ]]
  }; then
    blocked "Build requires either about 8 GiB RAM + 2 GiB Swap, or 6 GiB RAM + 4 GiB Swap"
  fi
  [[ "${free_mib}" -ge 20480 ]] || blocked "Build requires at least 20 GiB free space"
}

port_is_used_by_other_project() {
  local port="$1"
  local listener ids id project
  listener="$(ss -H -ltn 2>/dev/null | awk '{print $4}' | grep -E "[:.]${port}$" || true)"
  [[ -n "${listener}" ]] || return 1
  ids="$(docker ps --filter "publish=${port}" --format '{{.ID}}' || true)"
  [[ -n "${ids}" ]] || return 0
  while IFS= read -r id; do
    project="$(docker inspect --format '{{index .Config.Labels "com.docker.compose.project"}}' "${id}" 2>/dev/null || true)"
    [[ "${project}" == "${PREFLIGHT_PROJECT}" ]] || return 0
  done <<<"${ids}"
  return 1
}

check_candidate_ports() {
  local item
  IFS=',' read -r -a requested_ports <<<"${PREFLIGHT_PORTS}"
  for item in "${requested_ports[@]}"; do
    [[ "${item}" =~ ^[0-9]{2,5}$ ]] || blocked "Invalid candidate port: ${item}"
    if port_is_used_by_other_project "${item}"; then
      blocked "Candidate port ${item} is already used outside ${PREFLIGHT_PROJECT}"
    fi
    status PASS "candidate_port=${item} available_or_owned_by=${PREFLIGHT_PROJECT}"
  done
}

capture_project_baseline() {
  local project="$1"
  local output="$2"
  local ids
  : >"${output}"
  [[ -n "${project}" ]] || return 0
  ids="$(docker ps -aq --filter "label=com.docker.compose.project=${project}" | sort)"
  if [[ -z "${ids}" ]]; then
    blocked "Protected project ${project} was not found; refusing to continue without a valid Project A baseline"
  fi
  while IFS= read -r id; do
    docker inspect --format \
      '{{.Id}}|{{.Name}}|{{.Config.Image}}|{{.Image}}|{{.State.Status}}|{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}|{{.RestartCount}}|{{.State.OOMKilled}}|{{json .HostConfig.PortBindings}}|{{json .Mounts}}|{{json .NetworkSettings.Networks}}' \
      "${id}" >>"${output}"
  done <<<"${ids}"
}

check_protected_entry() {
  local label="$1"
  local url="$2"
  [[ -n "${url}" ]] || return 0
  curl -fsS --connect-timeout 5 --max-time 15 "${url}" >/dev/null ||
    blocked "Protected ${label} entry is not healthy: ${url}"
  status PASS "protected_${label}_entry=healthy"
}

preflight() {
  require_command awk
  require_command curl
  require_command df
  require_command grep
  require_command ss
  check_docker_access
  validate_project_name "${PREFLIGHT_PROJECT}"
  validate_deploy_root "${PREFLIGHT_ROOT}"
  host_snapshot
  check_candidate_ports

  local baseline
  baseline="$(mktemp)"
  capture_project_baseline "${PREFLIGHT_PROTECTED_PROJECT}" "${baseline}"
  if [[ -s "${baseline}" ]]; then
    status PASS "protected_project=${PREFLIGHT_PROTECTED_PROJECT} containers=$(wc -l <"${baseline}")"
    sed 's/^/[PASS] protected_container=/' "${baseline}"
  fi
  rm -f "${baseline}"
  check_protected_entry web "${PREFLIGHT_PROTECTED_WEB_URL}"
  check_protected_entry storage "${PREFLIGHT_PROTECTED_STORAGE_URL}"

  if [[ "${ACTION}" == "preflight-prod" ]]; then
    [[ "$(uname -m)" == "x86_64" ]] ||
      blocked "ECS 2 architecture must match the x86_64 TEST release"
    status PASS "public_ports_observed_only"
    ss -H -ltn | awk '$4 ~ /:(22|80|443)$/ {print "[PASS] listener=" $4}'
    if command -v nginx >/dev/null 2>&1; then
      status PASS "host_nginx=installed"
    else
      status WARN "host_nginx=not-installed"
    fi
    if command -v 1pctl >/dev/null 2>&1; then
      status WARN "host_1panel=installed-but-not-a-CREHN-runtime-dependency"
    else
      status PASS "host_1panel=not-installed"
    fi
    status WARN "security_group_dns_https_certificate require separate cloud-side verification"
  fi
  status PASS "${ACTION} completed without changing the host"
}

verify_release_manifest() {
  [[ -n "${RELEASE_MANIFEST}" ]] || blocked "--release-manifest is required"
  [[ -f "${RELEASE_MANIFEST}" ]] || blocked "Release manifest does not exist: ${RELEASE_MANIFEST}"
  require_command sha256sum

  local archive expected actual manifest_version manifest_revision architecture
  archive="$(sed -n 's/.*"archive"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "${RELEASE_MANIFEST}" | head -1)"
  expected="$(sed -n 's/.*"sha256"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "${RELEASE_MANIFEST}" | head -1)"
  manifest_version="$(sed -n 's/.*"version"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "${RELEASE_MANIFEST}" | head -1)"
  manifest_revision="$(sed -n 's/.*"sourceRevision"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "${RELEASE_MANIFEST}" | head -1)"
  architecture="$(sed -n 's/.*"architecture"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "${RELEASE_MANIFEST}" | head -1)"
  [[ "${archive}" =~ ^crehn-images-[0-9A-Za-z.-]+\.tar$ ]] || blocked "Manifest archive name is invalid"
  [[ "${expected}" =~ ^[0-9a-f]{64}$ ]] || blocked "Manifest SHA-256 is invalid"
  [[ "${manifest_version}" == "${VERSION}" ]] || blocked "Manifest version does not match runtime environment"
  [[ "${manifest_revision}" == "${SOURCE_REVISION}" ]] || blocked "Manifest revision does not match runtime environment"
  [[ "${architecture}" == "$(uname -m)" ]] || blocked "Release architecture does not match this host"
  RELEASE_ARCHIVE="$(cd "$(dirname "${RELEASE_MANIFEST}")" && pwd)/${archive}"
  [[ -f "${RELEASE_ARCHIVE}" ]] || blocked "Release archive does not exist: ${RELEASE_ARCHIVE}"
  actual="$(sha256sum "${RELEASE_ARCHIVE}" | awk '{print $1}')"
  [[ "${actual}" == "${expected}" ]] || blocked "Release archive SHA-256 mismatch"
  status PASS "release_manifest version=${manifest_version} revision=${manifest_revision} sha256=${actual}"
}

compose_config_check() {
  "${COMPOSE[@]}" config --quiet
  if [[ "${RUNTIME_ENV}" == "prod" ]]; then
    if "${COMPOSE[@]}" config --services | grep -qxE 'minio|minio-init'; then
      blocked "Production Compose must not contain MinIO"
    fi
  fi
  status PASS "compose_config project=${COMPOSE_PROJECT} environment=${RUNTIME_ENV}"
}

image_id() {
  docker image inspect --format '{{.Id}}' "$1"
}

verify_application_image_labels() {
  local image actual_version actual_revision
  for image in "crehn-db:${VERSION}" "crehn-backend:${VERSION}" "crehn-web:${VERSION}"; do
    actual_version="$(docker image inspect --format '{{index .Config.Labels "org.opencontainers.image.version"}}' "${image}")"
    actual_revision="$(docker image inspect --format '{{index .Config.Labels "org.opencontainers.image.revision"}}' "${image}")"
    [[ "${actual_version}" == "${VERSION}" && "${actual_revision}" == "${SOURCE_REVISION}" ]] ||
      blocked "Image label mismatch for ${image}"
  done
  status PASS "application_image_labels version=${VERSION} revision=${SOURCE_REVISION}"
}

build_local() {
  load_runtime_context
  [[ "${RUNTIME_ENV}" == "test" ]] || blocked "build-local is allowed only for TEST"
  require_apply "BUILD:${COMPOSE_PROJECT}:${VERSION}"
  acquire_project_lock
  check_env_permissions
  require_runtime_secrets
  check_docker_access
  require_command flock
  check_build_capacity
  compose_config_check

  mkdir -p "${RELEASE_ROOT}" "${STATE_ROOT}"
  exec 9>/run/lock/crehn-global-build.lock
  flock -n 9 || blocked "Another project build is already running on this host"

  status PASS "sequential_build_started project=${COMPOSE_PROJECT} version=${VERSION}"
  "${COMPOSE[@]}" build db
  "${COMPOSE[@]}" build backend
  "${COMPOSE[@]}" build web
  verify_application_image_labels

  local redis_image minio_image minio_mc_image
  redis_image="$(env_value CREHN_REDIS_IMAGE)"
  docker image inspect "${redis_image}" >/dev/null 2>&1 || docker pull "${redis_image}"
  minio_image="$(env_value CREHN_MINIO_IMAGE)"
  minio_mc_image="$(env_value CREHN_MINIO_MC_IMAGE)"
  docker image inspect "${minio_image}" >/dev/null 2>&1 || docker pull "${minio_image}"
  docker image inspect "${minio_mc_image}" >/dev/null 2>&1 || docker pull "${minio_mc_image}"

  local release_dir staging archive manifest sha created
  release_dir="${RELEASE_ROOT}/${VERSION}"
  staging="${RELEASE_ROOT}/.incomplete-${VERSION}-$$"
  [[ ! -e "${release_dir}" ]] || blocked "Release ${VERSION} already exists and is immutable"
  mkdir -p "${staging}"
  archive="${staging}/crehn-images-${VERSION}.tar"
  docker save -o "${archive}" \
    "crehn-db:${VERSION}" "crehn-backend:${VERSION}" "crehn-web:${VERSION}" \
    "${redis_image}" "${minio_image}" "${minio_mc_image}"
  sha="$(sha256sum "${archive}" | awk '{print $1}')"
  created="$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
  manifest="${staging}/crehn-images-${VERSION}.json"
  cat >"${manifest}" <<EOF
{
  "schemaVersion": 1,
  "version": "${VERSION}",
  "sourceRevision": "${SOURCE_REVISION}",
  "architecture": "$(uname -m)",
  "createdAt": "${created}",
  "archive": "crehn-images-${VERSION}.tar",
  "sha256": "${sha}",
  "images": [
    {"reference": "crehn-db:${VERSION}", "id": "$(image_id "crehn-db:${VERSION}")"},
    {"reference": "crehn-backend:${VERSION}", "id": "$(image_id "crehn-backend:${VERSION}")"},
    {"reference": "crehn-web:${VERSION}", "id": "$(image_id "crehn-web:${VERSION}")"},
    {"reference": "${redis_image}", "id": "$(image_id "${redis_image}")"},
    {"reference": "${minio_image}", "id": "$(image_id "${minio_image}")"},
    {"reference": "${minio_mc_image}", "id": "$(image_id "${minio_mc_image}")"}
  ]
}
EOF
  sha256sum -c <(printf '%s  %s\n' "${sha}" "${archive}") >/dev/null
  touch "${staging}/SUCCESS"
  mv "${staging}" "${release_dir}"
  status PASS "build-local release_manifest=${release_dir}/crehn-images-${VERSION}.json"
}

container_is_running() {
  local service="$1"
  local id
  id="$("${COMPOSE[@]}" ps -q "${service}" 2>/dev/null || true)"
  [[ -n "${id}" ]] && [[ "$(docker inspect --format '{{.State.Running}}' "${id}")" == "true" ]]
}

backup_create() {
  local reason="$1"
  local backup_env="${ENV_FILE}"
  if [[ -f "${ACTIVE_ENV_FILE}" ]]; then
    backup_env="${ACTIVE_ENV_FILE}"
  fi
  local rollback_version rollback_revision backup_name staging final ids
  rollback_version="$(env_value_from_file CREHN_VERSION "${backup_env}" || true)"
  rollback_revision="$(env_value_from_file CREHN_SOURCE_REVISION "${backup_env}" || true)"
  [[ "${rollback_version}" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]+)?$ ]] ||
    rollback_version="${VERSION}"
  backup_name="$(date -u +'%Y%m%dT%H%M%SZ')-${rollback_version}-${reason}"
  staging="${BACKUP_ROOT}/.incomplete-${backup_name}"
  final="${BACKUP_ROOT}/${backup_name}"
  [[ ! -e "${staging}" && ! -e "${final}" ]] ||
    blocked "Backup target already exists: ${backup_name}"
  mkdir -p "${BACKUP_ROOT}" "${staging}/compose"

  local backup_deploy_dir="${DEPLOY_DIR}"
  if [[ -f "${ACTIVE_ENV_FILE}" && -f "${DEPLOY_ROOT}/.source-previous/deploy/compose.yml" ]]; then
    backup_deploy_dir="${DEPLOY_ROOT}/.source-previous/deploy"
  fi
  cp "${backup_deploy_dir}/compose.yml" "${staging}/compose/compose.yml"
  cp "${backup_deploy_dir}/$(basename "${OVERLAY_COMPOSE}")" \
    "${staging}/compose/$(basename "${OVERLAY_COMPOSE}")"
  cp "${backup_env}" "${staging}/runtime.env"
  chmod 0600 "${staging}/runtime.env"
  ids="$(docker ps -aq --filter "label=com.docker.compose.project=${COMPOSE_PROJECT}" | while IFS= read -r id; do
    [[ -n "${id}" ]] && docker inspect --format '{{.Image}}' "${id}"
  done | sort -u)"
  if [[ -n "${ids}" ]]; then
    while IFS= read -r id; do
      docker image inspect --format '{{.Id}} {{json .RepoTags}} {{json .RepoDigests}}' "${id}" \
        >>"${staging}/image-digests.txt"
      docker image inspect --format '{{range .RepoTags}}{{println .}}{{end}}' "${id}" \
        >>"${staging}/image-references.txt"
    done <<<"${ids}"
    docker save -o "${staging}/images.tar" ${ids}
  else
    : >"${staging}/image-digests.txt"
    : >"${staging}/image-references.txt"
  fi

  if container_is_running db; then
    "${COMPOSE[@]}" exec -T db sh -c \
      'MYSQL_PWD="$MARIADB_ROOT_PASSWORD" mariadb-dump -uroot --single-transaction --routines --events "$MARIADB_DATABASE"' \
      >"${staging}/database.sql"
  fi

  if [[ -d "${DATA_ROOT}/backend" ]]; then
    tar -C "${DATA_ROOT}" -czf "${staging}/local-files.tar.gz" backend
  fi
  if [[ "${RUNTIME_ENV}" == "test" && -d "${DATA_ROOT}/minio" ]]; then
    status WARN "TEST MinIO data is not copied live; versioning remains the object rollback mechanism"
    printf 'mode=independent-test-minio\npath=%s\nversioning=enabled\n' \
      "${DATA_ROOT}/minio" >"${staging}/object-storage.txt"
  else
    printf 'mode=aliyun-oss\nbackup_reference=%s\n' \
      "$(env_value CREHN_PROD_OSS_BACKUP_REFERENCE || true)" >"${staging}/object-storage.txt"
  fi
  printf 'reason=%s\nproject=%s\nrollback_version=%s\nrollback_revision=%s\ntarget_version=%s\ntarget_revision=%s\ncreated=%s\n' \
    "${reason}" "${COMPOSE_PROJECT}" "${rollback_version}" "${rollback_revision}" \
    "${VERSION}" "${SOURCE_REVISION}" \
    "$(date -u +'%Y-%m-%dT%H:%M:%SZ')" >"${staging}/BACKUP-METADATA"
  (
    cd "${staging}"
    find . -type f ! -name CHECKSUMS.sha256 ! -name SUCCESS -print0 |
      sort -z |
      xargs -0 sha256sum >CHECKSUMS.sha256
    sha256sum -c CHECKSUMS.sha256 >/dev/null
  )
  touch "${staging}/SUCCESS"
  mv "${staging}" "${final}"
  CREATED_BACKUP_PATH="${final}"
  status PASS "backup_complete=${CREATED_BACKUP_PATH}"
}

verify_services() {
  local id service status_value health exit_code restart_count failed=0
  while IFS= read -r id; do
    [[ -n "${id}" ]] || continue
    service="$(docker inspect --format '{{index .Config.Labels "com.docker.compose.service"}}' "${id}")"
    status_value="$(docker inspect --format '{{.State.Status}}' "${id}")"
    health="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "${id}")"
    exit_code="$(docker inspect --format '{{.State.ExitCode}}' "${id}")"
    restart_count="$(docker inspect --format '{{.RestartCount}}' "${id}")"
    if [[ "${service}" == "minio-init" && "${status_value}" == "exited" && "${exit_code}" == "0" ]]; then
      status PASS "service=${service} status=completed"
    elif [[ "${status_value}" == "running" && ("${health}" == "healthy" || "${health}" == "none") ]]; then
      status PASS "service=${service} status=${status_value} health=${health}"
    else
      status FAILED "service=${service} status=${status_value} health=${health} exit=${exit_code}"
      failed=1
    fi
    [[ "$(docker inspect --format '{{.State.OOMKilled}}' "${id}")" == "false" ]] ||
      { status FAILED "service=${service} OOMKilled=true"; failed=1; }
    [[ "${restart_count}" == "0" ]] ||
      { status FAILED "service=${service} restart_count=${restart_count}"; failed=1; }

    local expected_image=""
    case "${service}" in
      db) expected_image="crehn-db:${VERSION}" ;;
      backend) expected_image="crehn-backend:${VERSION}" ;;
      web) expected_image="crehn-web:${VERSION}" ;;
      redis) expected_image="$(env_value CREHN_REDIS_IMAGE)" ;;
      minio) expected_image="$(env_value CREHN_MINIO_IMAGE)" ;;
      minio-init) expected_image="$(env_value CREHN_MINIO_MC_IMAGE)" ;;
    esac
    if [[ -n "${expected_image}" ]]; then
      local expected_id
      expected_id="$(docker image inspect --format '{{.Id}}' "${expected_image}" 2>/dev/null || true)"
      if [[ -z "${expected_id}" || "${expected_id}" != "$(docker inspect --format '{{.Image}}' "${id}")" ]]; then
        status FAILED "service=${service} image_id_mismatch"
        failed=1
      else
        status PASS "service=${service} image_id=${expected_id}"
      fi
    fi
  done < <("${COMPOSE[@]}" ps -aq)
  [[ "${failed}" -eq 0 ]] || return 1

  local bind port
  bind="$(env_value CREHN_HTTP_BIND)"
  port="$(env_value CREHN_HTTP_PORT)"
  curl -fsS --connect-timeout 5 --max-time 20 "http://${bind}:${port}/healthz" | grep -q '^ok' ||
    return 1
  curl -fsS --connect-timeout 5 --max-time 20 "http://${bind}:${port}/admin/" >/dev/null ||
    return 1
  "${COMPOSE[@]}" exec -T backend sh -c \
    'curl -fsS -u "$CREHN_MONITOR_USERNAME:$CREHN_MONITOR_PASSWORD" http://127.0.0.1:8080/actuator/health | grep -q UP' ||
    return 1
  status PASS "web_admin_api_health=healthy"
}

restore_backup() {
  local backup="$1"
  [[ -f "${backup}/SUCCESS" && -f "${backup}/CHECKSUMS.sha256" ]] ||
    blocked "Backup is not marked complete: ${backup}"
  (cd "${backup}" && sha256sum -c CHECKSUMS.sha256 >/dev/null) ||
    blocked "Backup checksum verification failed: ${backup}"
  if [[ -f "${backup}/images.tar" ]]; then
    docker load -i "${backup}/images.tar" >/dev/null
  else
    "${COMPOSE[@]}" down --remove-orphans
    status PASS "No prior project images existed; failed first deployment was removed"
    return 0
  fi
  install -o root -g root -m 0600 "${backup}/runtime.env" "${ACTIVE_ENV_FILE}"
  ENV_FILE="${ACTIVE_ENV_FILE}"
  load_runtime_context
  local saved_overlay="${backup}/compose/compose.${RUNTIME_ENV}.yml"
  [[ -f "${backup}/compose/compose.yml" && -f "${saved_overlay}" ]] ||
    blocked "Backup does not contain the Compose files required for rollback"
  COMPOSE=(docker compose --env-file "${ENV_FILE}" -p "${COMPOSE_PROJECT}" \
    -f "${backup}/compose/compose.yml" -f "${saved_overlay}")
  "${COMPOSE[@]}" config --quiet
  "${COMPOSE[@]}" up -d --no-build --pull never
  verify_services
}

deploy_release() {
  local environment_label="$1"
  load_runtime_context
  [[ "${RUNTIME_ENV}" == "${environment_label}" ]] ||
    blocked "${ACTION} does not match CREHN_RUNTIME_ENV=${RUNTIME_ENV}"
  require_apply "DEPLOY:${COMPOSE_PROJECT}:${VERSION}"
  acquire_project_lock
  check_env_permissions
  require_runtime_secrets
  check_docker_access
  compose_config_check
  verify_release_manifest
  [[ -f "${STATE_ROOT}/database-initialized" ]] ||
    blocked "Database initialization/migration is a separate authorization and has not completed"

  mkdir -p "${DATA_ROOT}/mariadb" "${DATA_ROOT}/redis" \
    "${DATA_ROOT}/backend/logs" "${DATA_ROOT}/backend/tmp" \
    "${DATA_ROOT}/backend/workbench-assets" "${DATA_ROOT}/backend/portal-pdf" \
    "${BACKUP_ROOT}" "${EVIDENCE_ROOT}" "${STATE_ROOT}"
  if [[ "${RUNTIME_ENV}" == "test" ]]; then
    mkdir -p "${DATA_ROOT}/minio"
  fi

  local evidence before after rollback_point
  evidence="${EVIDENCE_ROOT}/$(date -u +'%Y%m%dT%H%M%SZ')-${ACTION}"
  mkdir -p "${evidence}"
  before="${evidence}/protected-before.txt"
  after="${evidence}/protected-after.txt"
  if [[ "${RUNTIME_ENV}" == "test" ]]; then
    capture_project_baseline "$(env_value CREHN_PROTECTED_COMPOSE_PROJECT || true)" "${before}"
    check_protected_entry web "$(env_value CREHN_PROTECTED_WEB_URL || true)"
    check_protected_entry storage "$(env_value CREHN_PROTECTED_STORAGE_HEALTH_URL || true)"
  fi
  backup_create pre-deploy
  rollback_point="${CREATED_BACKUP_PATH}"

  docker load -i "${RELEASE_ARCHIVE}" >/dev/null
  if ! "${COMPOSE[@]}" up -d --no-build --pull never || ! verify_services; then
    status WARN "Deployment failed; restoring only ${COMPOSE_PROJECT} from ${rollback_point}"
    restore_backup "${rollback_point}" || true
    fail "Deployment failed and project-specific rollback was attempted"
  fi

  if [[ "${RUNTIME_ENV}" == "test" ]]; then
    capture_project_baseline "$(env_value CREHN_PROTECTED_COMPOSE_PROJECT || true)" "${after}"
    if ! cmp -s "${before}" "${after}"; then
      status WARN "Protected project baseline changed; restoring only ${COMPOSE_PROJECT}"
      restore_backup "${rollback_point}" || true
      fail "Protected project changed during deployment"
    fi
    check_protected_entry web "$(env_value CREHN_PROTECTED_WEB_URL || true)"
    check_protected_entry storage "$(env_value CREHN_PROTECTED_STORAGE_HEALTH_URL || true)"
  fi
  install -o root -g root -m 0600 "${ENV_FILE}" "${ACTIVE_ENV_FILE}"
  touch "${evidence}/SUCCESS"
  status PASS "${ACTION} completed project=${COMPOSE_PROJECT} version=${VERSION}"
}

verify_local() {
  load_runtime_context
  [[ "${RUNTIME_ENV}" == "test" ]] || blocked "verify-local is allowed only for TEST"
  check_docker_access
  compose_config_check
  verify_services
  check_protected_entry web "$(env_value CREHN_PROTECTED_WEB_URL || true)"
  check_protected_entry storage "$(env_value CREHN_PROTECTED_STORAGE_HEALTH_URL || true)"
  status WARN "Browser role smoke tests remain NEEDS_BROWSER"
  status PASS "verify-local completed"
}

database_action() {
  load_runtime_context
  check_docker_access
  compose_config_check
  if [[ "${DATABASE_OPERATION}" == "plan" ]]; then
    status PASS "database plan only; no container or SQL was executed"
    status PASS "ordered initialization source=/opt/crehn/init"
    status WARN "future migration requires a reviewed checksum manifest and separate implementation"
    return 0
  fi
  [[ "${DATABASE_OPERATION}" == "initialize" ]] ||
    blocked "Only plan or one-time initialize is supported; speculative migration execution is forbidden"
  require_apply "INITIALIZE:${COMPOSE_PROJECT}"
  acquire_project_lock
  check_env_permissions
  require_runtime_secrets
  if [[ -n "${RELEASE_MANIFEST}" ]]; then
    verify_release_manifest
    docker load -i "${RELEASE_ARCHIVE}" >/dev/null
  fi
  docker image inspect "crehn-db:${VERSION}" >/dev/null 2>&1 ||
    blocked "Database image is unavailable; provide --release-manifest or run the authorized TEST build first"
  [[ ! -f "${STATE_ROOT}/database-initialized" ]] ||
    blocked "Database initialization marker already exists"
  mkdir -p "${DATA_ROOT}/mariadb" "${STATE_ROOT}"
  "${COMPOSE[@]}" up -d --no-build --pull never db
  local attempts=60 table_count
  until container_is_running db && "${COMPOSE[@]}" exec -T db sh -c \
    'MYSQL_PWD="$MARIADB_ROOT_PASSWORD" mariadb-admin -uroot ping --silent' >/dev/null 2>&1; do
    attempts=$((attempts - 1))
    [[ "${attempts}" -gt 0 ]] || fail "Database did not become ready"
    sleep 2
  done
  table_count="$("${COMPOSE[@]}" exec -T db sh -c \
    'MYSQL_PWD="$MARIADB_ROOT_PASSWORD" mariadb -N -uroot "$MARIADB_DATABASE" -e "select count(*) from information_schema.tables where table_schema = database();"')"
  [[ "${table_count}" == "0" ]] ||
    blocked "Target database is not empty; initialization refused"

  while IFS= read -r init_file; do
    status PASS "database_apply=$(basename "${init_file}")"
    case "${init_file}" in
      *.sql)
        "${COMPOSE[@]}" exec -T db sh -c \
          'MYSQL_PWD="$MARIADB_ROOT_PASSWORD" mariadb -uroot "$MARIADB_DATABASE"' \
          < <("${COMPOSE[@]}" exec -T db cat "${init_file}")
        ;;
      *.sh) "${COMPOSE[@]}" exec -T db bash "${init_file}" ;;
      *) blocked "Unsupported database initialization file: ${init_file}" ;;
    esac
  done < <("${COMPOSE[@]}" exec -T db find /opt/crehn/init -maxdepth 1 -type f | sort)
  printf 'project=%s\nversion=%s\nrevision=%s\ncompleted=%s\n' \
    "${COMPOSE_PROJECT}" "${VERSION}" "${SOURCE_REVISION}" \
    "$(date -u +'%Y-%m-%dT%H:%M:%SZ')" >"${STATE_ROOT}/database-initialized"
  status PASS "database initialization completed"
}

rollback_action() {
  load_runtime_context
  check_docker_access
  require_apply "ROLLBACK:${COMPOSE_PROJECT}:${BACKUP_ID}"
  acquire_project_lock
  [[ "${BACKUP_ID}" =~ ^[0-9]{8}T[0-9]{6}Z-[0-9A-Za-z.-]+-[0-9A-Za-z.-]+$ ]] ||
    blocked "Backup ID format is invalid"
  local backup="${BACKUP_ROOT}/${BACKUP_ID}"
  [[ "${backup}" == "${BACKUP_ROOT}/"* ]] || blocked "Backup escaped the project backup root"
  restore_backup "${backup}"
  status WARN "Database and OSS data were not rolled back"
  status PASS "rollback completed project=${COMPOSE_PROJECT} backup=${BACKUP_ID}"
}

backup_maintenance() {
  load_runtime_context
  require_command sha256sum
  local successful=()
  local dir
  if [[ -d "${BACKUP_ROOT}" ]]; then
    while IFS= read -r dir; do
      if [[ -f "${dir}/SUCCESS" && -f "${dir}/CHECKSUMS.sha256" ]] &&
        (cd "${dir}" && sha256sum -c CHECKSUMS.sha256 >/dev/null 2>&1); then
        successful+=("${dir}")
      else
        status WARN "backup_not_counted=$(basename "${dir}")"
      fi
    done < <(find "${BACKUP_ROOT}" -mindepth 1 -maxdepth 1 -type d -not -name '.incomplete-*' | sort -r)
  fi
  status PASS "successful_complete_backups=${#successful[@]}"
  printf '[PASS] retained_candidate=%s\n' "${successful[@]:0:3}"
  if [[ "${#successful[@]}" -le 3 ]]; then
    status PASS "No successful backup is eligible for cleanup"
    return 0
  fi
  if [[ "${APPLY}" != "true" ]]; then
    printf '[WARN] cleanup_candidate=%s\n' "${successful[@]:3}"
    blocked "Cleanup dry run only; add separately authorized --apply"
  fi
  require_apply "CLEAN-BACKUPS:${COMPOSE_PROJECT}"
  acquire_project_lock
  for dir in "${successful[@]:3}"; do
    [[ "${dir}" == "${BACKUP_ROOT}/"* ]] || fail "Cleanup target escaped project backup root"
    rm -rf -- "${dir}"
    status PASS "removed_project_backup=$(basename "${dir}")"
  done
  status PASS "backup-maintenance retained latest 3 successful complete backups"
}

case "${ACTION}" in
  preflight-local|preflight-prod) preflight ;;
  build-local) build_local ;;
  deploy-local) deploy_release test ;;
  verify-local) verify_local ;;
  deploy-prod) deploy_release prod ;;
  database) database_action ;;
  rollback) rollback_action ;;
  backup-maintenance) backup_maintenance ;;
  ""|-h|--help) usage ;;
  *) usage >&2; fail "Unknown action: ${ACTION}" ;;
esac
