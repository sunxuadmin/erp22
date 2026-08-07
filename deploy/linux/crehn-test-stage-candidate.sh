#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

readonly CONFIG=/etc/crehn-test-deploy-gate.conf
readonly DEPLOY_ROOT=/srv/crehn-test

fail() { printf '[FAILED] %s\n' "$1" >&2; exit 1; }
blocked() { printf '[BLOCKED] %s\n' "$1" >&2; exit 3; }
safe_root_dir() {
  local path="$1" mode
  [[ -d "${path}" && ! -L "${path}" && "$(stat -c '%U:%G' "${path}")" == 'root:root' ]] || blocked "unsafe root directory: ${path}"
  mode="$(stat -c '%a' "${path}")"
  (( (8#${mode} & 8#022) == 0 )) || blocked "root directory is group/world writable: ${path}"
}

[[ "$(id -u)" -eq 0 ]] || blocked 'candidate staging requires root'
[[ -r "${CONFIG}" && ! -L "${CONFIG}" && "$(stat -c '%U:%G:%a' "${CONFIG}")" == 'root:root:600' ]] ||
  blocked 'root gate configuration is missing or unsafe'
# shellcheck disable=SC1090
source "${CONFIG}"
[[ "${CREHN_TEST_OPERATOR:-}" =~ ^[A-Za-z_][A-Za-z0-9_-]*$ ]] || blocked 'invalid configured operator'
readonly INBOX="/home/${CREHN_TEST_OPERATOR}/crehn-test-inbox"
readonly ARCHIVE="${INBOX}/crehn-test-assets.tar.gz"
readonly RUNTIME_ENV="${INBOX}/crehn-test-runtime.env"
readonly METADATA="${INBOX}/crehn-test-candidate.env"
readonly WORK_ROOT="${DEPLOY_ROOT}/.gate-candidate"
readonly WORK_ARCHIVE="${WORK_ROOT}/assets.tar.gz"
readonly WORK_RUNTIME_ENV="${WORK_ROOT}/runtime.env"
readonly WORK_METADATA="${WORK_ROOT}/candidate.env"
for dir in /srv "${DEPLOY_ROOT}" "${DEPLOY_ROOT}/runtime" "${DEPLOY_ROOT}/state"; do safe_root_dir "${dir}"; done

require_regular_operator_file() {
  local path="$1" mode owner
  [[ ! -L "${path}" && -f "${path}" ]] || blocked "candidate must be a non-symlink regular file: ${path}"
  owner="$(stat -c '%U' "${path}")"
  mode="$(stat -c '%a' "${path}")"
  [[ "${owner}" == "${CREHN_TEST_OPERATOR}" && "${mode}" == '600' ]] ||
    blocked "candidate ownership/mode must be ${CREHN_TEST_OPERATOR}:600: ${path}"
}

[[ ! -L "${INBOX}" && -d "${INBOX}" ]] || blocked 'candidate inbox is missing or symlinked'
[[ "$(stat -c '%U' "${INBOX}")" == "${CREHN_TEST_OPERATOR}" && "$(stat -c '%a' "${INBOX}")" == '700' ]] ||
  blocked 'candidate inbox must be operator-owned mode 700'
require_regular_operator_file "${ARCHIVE}"
require_regular_operator_file "${RUNTIME_ENV}"
require_regular_operator_file "${METADATA}"

cleanup_work_root() {
  rm -f -- "${WORK_ARCHIVE}" "${WORK_RUNTIME_ENV}" "${WORK_METADATA}"
  rmdir "${WORK_ROOT}" 2>/dev/null || true
}
trap cleanup_work_root EXIT
cleanup_work_root
install -d -o root -g root -m 0700 "${WORK_ROOT}"
safe_root_dir "${WORK_ROOT}"
install -o root -g root -m 0600 "${ARCHIVE}" "${WORK_ARCHIVE}"
install -o root -g root -m 0600 "${RUNTIME_ENV}" "${WORK_RUNTIME_ENV}"
install -o root -g root -m 0600 "${METADATA}" "${WORK_METADATA}"

declare -A meta=()
while IFS='=' read -r key value; do
  [[ -n "${key}" && -n "${value}" && -z "${meta[${key}]+x}" ]] || blocked 'candidate metadata is malformed'
  meta["${key}"]="${value}"
done <"${WORK_METADATA}"
[[ "${#meta[@]}" -eq 3 && "${meta[version]+x}" && "${meta[source_revision]+x}" && "${meta[archive_sha256]+x}" ]] ||
  blocked 'candidate metadata must contain only version, source_revision and archive_sha256'
[[ "${meta[version]}" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]+)?$ ]] || blocked 'candidate version is invalid'
[[ "${meta[source_revision]}" =~ ^[0-9a-fA-F]{40,64}$ ]] || blocked 'candidate revision must be complete'
[[ "${meta[archive_sha256]}" =~ ^[0-9a-fA-F]{64}$ ]] || blocked 'candidate archive SHA-256 is invalid'
[[ "$(sha256sum "${WORK_ARCHIVE}" | awk '{print $1}')" == "${meta[archive_sha256],,}" ]] || blocked 'candidate archive SHA-256 mismatch'
[[ "$(grep -m1 '^CREHN_VERSION=' "${WORK_RUNTIME_ENV}" | cut -d= -f2- | tr -d '\r')" == "${meta[version]}" ]] ||
  blocked 'candidate runtime version does not match metadata'
[[ "$(grep -m1 '^CREHN_SOURCE_REVISION=' "${WORK_RUNTIME_ENV}" | cut -d= -f2- | tr -d '\r')" == "${meta[source_revision]}" ]] ||
  blocked 'candidate runtime revision does not match metadata'

while IFS= read -r entry; do
  [[ -n "${entry}" && "${entry}" != /* && "${entry}" != *'..'* && "${entry}" != *$'\r'* && "${entry}" != *$'\n'* ]] ||
    blocked 'archive contains an unsafe path'
  [[ "${entry}" =~ ^(backend|frontend|portal|deploy)(/|$)|^\.dockerignore$ ]] ||
    blocked "archive entry is outside the approved source scope: ${entry}"
done < <(tar -tzf "${WORK_ARCHIVE}")
while IFS= read -r listing; do
  [[ "${listing}" =~ ^[-d] ]] || blocked 'archive contains a symbolic link, hard link, or special entry'
done < <(tar -tvzf "${WORK_ARCHIVE}")

/usr/local/libexec/crehn-test/install-assets.sh \
  --mode test-source --deploy-root "${DEPLOY_ROOT}" --archive "${WORK_ARCHIVE}" \
  --runtime-env "${WORK_RUNTIME_ENV}" --version "${meta[version]}" \
  --source-revision "${meta[source_revision]}"
