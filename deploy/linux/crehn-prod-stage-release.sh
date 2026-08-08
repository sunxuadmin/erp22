#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

readonly CONFIG=/etc/crehn-prod-deploy-gate.conf
readonly DEPLOY_ROOT=/srv/crehn-prod
readonly INBOX_ROOT=/home
readonly TRUSTED_ROOT=/usr/local/libexec/crehn-prod

blocked() { printf '[BLOCKED] %s\n' "$1" >&2; exit 3; }
safe_root_dir() {
  local path="$1" mode
  [[ -d "${path}" && ! -L "${path}" && "$(stat -c '%U:%G' "${path}")" == 'root:root' ]] || blocked "unsafe root directory: ${path}"
  mode="$(stat -c '%a' "${path}")"
  (( (8#${mode} & 8#022) == 0 )) || blocked "root directory is group/world writable: ${path}"
}
env_value() {
  local key="$1" file="$2" line
  line="$(grep -m1 -E "^${key}=" "${file}" 2>/dev/null || true)"
  [[ -n "${line}" ]] || return 1
  printf '%s' "${line#*=}" | tr -d '\r'
}
require_operator_file() {
  local path="$1"
  [[ -f "${path}" && ! -L "${path}" && "$(stat -c '%U:%a' "${path}")" == "${CREHN_PROD_OPERATOR}:600" ]] ||
    blocked "staged input must be a non-symlink ${CREHN_PROD_OPERATOR}:600 file"
}

[[ "$(id -u)" -eq 0 ]] || blocked 'production staging requires root'
[[ "$#" -eq 0 ]] || blocked 'production staging accepts no arguments'
[[ -f "${CONFIG}" && ! -L "${CONFIG}" && "$(stat -c '%U:%G:%a' "${CONFIG}")" == 'root:root:600' ]] || blocked 'production gate configuration is missing or unsafe'
# shellcheck disable=SC1090
source "${CONFIG}"
[[ "${SUDO_USER:-}" == "${CREHN_PROD_OPERATOR:-}" ]] || blocked 'sudo caller is not the configured production operator'
[[ "${CREHN_PROD_OPERATOR:-}" =~ ^[A-Za-z_][A-Za-z0-9_-]*$ ]] || blocked 'configured production operator is invalid'
readonly INBOX="${INBOX_ROOT}/${CREHN_PROD_OPERATOR}/crehn-prod-inbox"
for dir in /srv "${DEPLOY_ROOT}" "${DEPLOY_ROOT}/runtime" "${DEPLOY_ROOT}/releases" "${TRUSTED_ROOT}"; do safe_root_dir "${dir}"; done
[[ -d "${INBOX}" && ! -L "${INBOX}" && "$(stat -c '%U:%a' "${INBOX}")" == "${CREHN_PROD_OPERATOR}:700" ]] || blocked 'production inbox is missing or unsafe'
command -v flock >/dev/null 2>&1 || blocked 'flock is required for immutable production staging'
exec 9>"${DEPLOY_ROOT}/state/promotion-stage.lock"
flock -n 9 || blocked 'another immutable production stage is already running'

readonly METADATA="${INBOX}/promotion.env"
readonly RUNTIME_ENV="${INBOX}/crehn-prod-runtime.env"
readonly MANIFEST="${INBOX}/crehn-images.json"
readonly ARCHIVE="${INBOX}/crehn-images.tar"
readonly ASSETS="${INBOX}/crehn-deploy-assets.tar.gz"
for file in "${METADATA}" "${RUNTIME_ENV}" "${MANIFEST}" "${ARCHIVE}" "${ASSETS}"; do require_operator_file "${file}"; done
"${TRUSTED_ROOT}/validate-crehn-prod-runtime-env.sh" "${RUNTIME_ENV}"

declare -A meta=()
while IFS='=' read -r key value; do
  [[ -n "${key}" && -n "${value}" && -z "${meta[${key}]+x}" ]] || blocked 'promotion metadata is malformed'
  meta["${key}"]="${value}"
done <"${METADATA}"
[[ "${#meta[@]}" -eq 6 && "${meta[version]+x}" && "${meta[source_revision]+x}" && "${meta[architecture]+x}" && "${meta[manifest_sha256]+x}" && "${meta[release_sha256]+x}" && "${meta[deploy_assets_sha256]+x}" ]] ||
  blocked 'promotion metadata must contain only the six required fields'
[[ "${meta[version]}" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]+)?$ && "${meta[source_revision]}" =~ ^[0-9a-fA-F]{40,64}$ && "${meta[architecture]}" == x86_64 ]] ||
  blocked 'promotion metadata version, revision or architecture is invalid'
for key in manifest_sha256 release_sha256 deploy_assets_sha256; do [[ "${meta[${key}]}" =~ ^[0-9a-f]{64}$ ]] || blocked "promotion metadata ${key} is invalid"; done
[[ "$(sha256sum "${MANIFEST}" | awk '{print $1}')" == "${meta[manifest_sha256]}" ]] || blocked 'manifest SHA-256 mismatch'
[[ "$(sha256sum "${ARCHIVE}" | awk '{print $1}')" == "${meta[release_sha256]}" ]] || blocked 'release archive SHA-256 mismatch'
[[ "$(sha256sum "${ASSETS}" | awk '{print $1}')" == "${meta[deploy_assets_sha256]}" ]] || blocked 'deployment assets SHA-256 mismatch'

manifest_version="$(sed -n 's/.*"version"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "${MANIFEST}" | head -1)"
manifest_revision="$(sed -n 's/.*"sourceRevision"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "${MANIFEST}" | head -1)"
manifest_architecture="$(sed -n 's/.*"architecture"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "${MANIFEST}" | head -1)"
manifest_archive="$(sed -n 's/.*"archive"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "${MANIFEST}" | head -1)"
manifest_sha="$(sed -n 's/.*"sha256"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "${MANIFEST}" | head -1)"
[[ "${manifest_version}" == "${meta[version]}" && "${manifest_revision}" == "${meta[source_revision]}" && "${manifest_architecture}" == x86_64 && "${manifest_archive}" == "crehn-images-${meta[version]}.tar" && "${manifest_sha}" == "${meta[release_sha256]}" ]] ||
  blocked 'manifest does not match immutable promotion metadata'
image_reference_count="$(grep -Ec '"reference"[[:space:]]*:[[:space:]]*"[^"]+"' "${MANIFEST}" || true)"
image_id_count="$(grep -Ec '"id"[[:space:]]*:[[:space:]]*"sha256:[0-9a-f]{64}"' "${MANIFEST}" || true)"
[[ "${image_reference_count}" -ge 3 && "${image_reference_count}" == "${image_id_count}" ]] || blocked 'manifest image IDs are missing or invalid'
[[ "$(env_value CREHN_RUNTIME_ENV "${RUNTIME_ENV}" || true)" == prod && "$(env_value CREHN_COMPOSE_PROJECT_NAME "${RUNTIME_ENV}" || true)" == crehn-prod && "$(env_value CREHN_DEPLOY_ROOT "${RUNTIME_ENV}" || true)" == /srv/crehn-prod && "$(env_value CREHN_VERSION "${RUNTIME_ENV}" || true)" == "${meta[version]}" && "$(env_value CREHN_SOURCE_REVISION "${RUNTIME_ENV}" || true)" == "${meta[source_revision]}" ]] ||
  blocked 'production runtime environment does not match the immutable promotion'

while IFS= read -r entry; do
  [[ -n "${entry}" && "${entry}" != /* && "${entry}" != *'..'* && "${entry}" != *$'\r'* && "${entry}" != *$'\n'* ]] || blocked 'deployment asset archive contains an unsafe path'
  [[ "${entry}" =~ ^deploy(/|$) ]] || blocked 'deployment asset archive is outside deploy/'
done < <(tar -tzf "${ASSETS}")
while IFS= read -r listing; do [[ "${listing}" =~ ^[-d] ]] || blocked 'deployment asset archive contains a symbolic link, hard link, or special entry'; done < <(tar -tvzf "${ASSETS}")

work_root="${DEPLOY_ROOT}/.promotion-stage"
rm -rf -- "${work_root}"
install -d -o root -g root -m 0700 "${work_root}"
trap 'rm -rf -- "${work_root}"' EXIT
install -o root -g root -m 0600 "${ASSETS}" "${work_root}/assets.tar.gz"
install -o root -g root -m 0600 "${RUNTIME_ENV}" "${work_root}/runtime.env"
install -o root -g root -m 0600 "${MANIFEST}" "${work_root}/release.json"
install -o root -g root -m 0600 "${ARCHIVE}" "${work_root}/release.tar"
"${TRUSTED_ROOT}/install-assets.sh" --mode prod-release --deploy-root "${DEPLOY_ROOT}" --archive "${work_root}/assets.tar.gz" --runtime-env "${work_root}/runtime.env" --version "${meta[version]}" --source-revision "${meta[source_revision]}" --release-manifest "${work_root}/release.json" --release-archive "${work_root}/release.tar"
printf 'version=%s\nsource_revision=%s\n' "${meta[version]}" "${meta[source_revision]}" >"${DEPLOY_ROOT}/runtime/prod-candidate.env"
chown root:root "${DEPLOY_ROOT}/runtime/prod-candidate.env"
chmod 0600 "${DEPLOY_ROOT}/runtime/prod-candidate.env"
for file in "${METADATA}" "${RUNTIME_ENV}" "${MANIFEST}" "${ARCHIVE}" "${ASSETS}"; do rm -f -- "${file}"; done
printf '[PASS] immutable production release staged version=%s revision=%s inbox=cleared same-version-retry=requires-controlled-recovery\n' "${meta[version]}" "${meta[source_revision]}"
