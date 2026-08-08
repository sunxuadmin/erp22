#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

# Fixed, root-owned export boundary for a TEST release.  It deliberately has no
# path, shell, Docker, or production-target arguments.
readonly CONFIG=/etc/crehn-test-deploy-gate.conf
readonly DEPLOY_ROOT=/srv/crehn-test
readonly EXPORT_ROOT=/srv/crehn-test/exports

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

[[ "$(id -u)" -eq 0 ]] || blocked 'export gate must run through sudo'
[[ "$#" -eq 4 && "$1" == 'export' && "$2" == '--apply' ]] || blocked 'only export --apply CONFIRMATION VERSION is accepted'
readonly CONFIRMATION="$3"
readonly VERSION="$4"
[[ "${VERSION}" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]+)?$ ]] || blocked 'release version is invalid'
[[ "${CONFIRMATION}" == "PROMOTE:crehn-prod:${VERSION}" ]] || blocked 'export confirmation is invalid'
[[ "$(readlink -f "$0")" == '/usr/local/sbin/crehn-test-release-export-gate' ]] || blocked 'gate path is not the fixed installed path'
for dir in /srv "${DEPLOY_ROOT}" "${DEPLOY_ROOT}/runtime" "${DEPLOY_ROOT}/releases" "${DEPLOY_ROOT}/exports" "${DEPLOY_ROOT}/state"; do safe_root_dir "${dir}"; done
[[ -f "${CONFIG}" && ! -L "${CONFIG}" && "$(stat -c '%U:%G:%a' "${CONFIG}")" == 'root:root:600' ]] ||
  blocked 'TEST root gate configuration is missing or unsafe'
# shellcheck disable=SC1090
source "${CONFIG}"
[[ "${SUDO_USER:-}" == "${CREHN_TEST_OPERATOR:-}" ]] || blocked 'sudo caller is not the configured TEST operator'

readonly ACTIVE_ENV="${DEPLOY_ROOT}/runtime/test.env"
readonly RELEASE_DIR="${DEPLOY_ROOT}/releases/${VERSION}"
readonly MANIFEST="${RELEASE_DIR}/crehn-images-${VERSION}.json"
readonly ARCHIVE="${RELEASE_DIR}/crehn-images-${VERSION}.tar"
readonly SOURCE_DEPLOY="${DEPLOY_ROOT}/source/deploy"
[[ -f "${ACTIVE_ENV}" && ! -L "${ACTIVE_ENV}" && "$(stat -c '%U:%G:%a' "${ACTIVE_ENV}")" == 'root:root:600' ]] ||
  blocked 'active TEST runtime file is missing or unsafe'
[[ -f "${RELEASE_DIR}/SUCCESS" && ! -L "${RELEASE_DIR}/SUCCESS" && -f "${MANIFEST}" && ! -L "${MANIFEST}" && -f "${ARCHIVE}" && ! -L "${ARCHIVE}" ]] ||
  blocked 'TEST release is incomplete; SUCCESS, manifest and archive are required'
[[ -d "${SOURCE_DEPLOY}" && ! -L "${SOURCE_DEPLOY}" ]] || blocked 'active TEST deployment assets are missing'
command -v flock >/dev/null 2>&1 || blocked 'flock is required for immutable TEST export'
exec 9>"${DEPLOY_ROOT}/state/release-export.lock"
flock -n 9 || blocked 'another TEST release export is already running'

runtime_version="$(env_value CREHN_VERSION "${ACTIVE_ENV}" || true)"
runtime_revision="$(env_value CREHN_SOURCE_REVISION "${ACTIVE_ENV}" || true)"
candidate_env="${DEPLOY_ROOT}/runtime/test-candidate.env"
[[ -f "${candidate_env}" && ! -L "${candidate_env}" && "$(stat -c '%U:%G:%a' "${candidate_env}")" == 'root:root:600' ]] ||
  blocked 'root-owned TEST candidate metadata is missing or unsafe'
candidate_version="$(env_value version "${candidate_env}" || true)"
candidate_revision="$(env_value source_revision "${candidate_env}" || true)"
manifest_version="$(sed -n 's/.*"version"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "${MANIFEST}" | head -1)"
manifest_revision="$(sed -n 's/.*"sourceRevision"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "${MANIFEST}" | head -1)"
manifest_architecture="$(sed -n 's/.*"architecture"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "${MANIFEST}" | head -1)"
manifest_archive="$(sed -n 's/.*"archive"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "${MANIFEST}" | head -1)"
manifest_sha="$(sed -n 's/.*"sha256"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "${MANIFEST}" | head -1)"
[[ "${runtime_version}" == "${VERSION}" && "${manifest_version}" == "${VERSION}" ]] || blocked 'active TEST/runtime/manifest version mismatch'
[[ "${runtime_revision}" =~ ^[0-9a-fA-F]{40,64}$ && "${runtime_revision}" == "${manifest_revision}" ]] || blocked 'active TEST/runtime/manifest revision mismatch'
[[ "${candidate_version}" == "${runtime_version}" && "${candidate_revision}" == "${runtime_revision}" ]] ||
  blocked 'TEST candidate metadata differs from active runtime; deployment assets may belong to another revision'
[[ "${manifest_architecture}" == 'x86_64' && "${manifest_archive}" == "crehn-images-${VERSION}.tar" && "${manifest_sha}" =~ ^[0-9a-f]{64}$ ]] ||
  blocked 'TEST manifest architecture, archive name or SHA-256 is invalid'
[[ "$(sha256sum "${ARCHIVE}" | awk '{print $1}')" == "${manifest_sha}" ]] || blocked 'TEST archive SHA-256 mismatch'
image_reference_count="$(grep -Ec '"reference"[[:space:]]*:[[:space:]]*"[^"]+"' "${MANIFEST}" || true)"
image_id_count="$(grep -Ec '"id"[[:space:]]*:[[:space:]]*"sha256:[0-9a-f]{64}"' "${MANIFEST}" || true)"
[[ "${image_reference_count}" -ge 3 && "${image_reference_count}" == "${image_id_count}" ]] || blocked 'TEST manifest image IDs are missing or invalid'

readonly EXPORT_DIR="${EXPORT_ROOT}/${VERSION}"
[[ ! -e "${EXPORT_DIR}" ]] || blocked 'immutable TEST export already exists for this version; do not retry without controlled recovery'
staging="${EXPORT_ROOT}/.incomplete-${VERSION}-$$"
trap 'rm -rf -- "${staging}"' EXIT
mkdir -p "${staging}"
tar -C "${DEPLOY_ROOT}/source" -czf "${staging}/crehn-deploy-${VERSION}.tar.gz" deploy
assets_sha="$(sha256sum "${staging}/crehn-deploy-${VERSION}.tar.gz" | awk '{print $1}')"
install -o root -g root -m 0644 "${MANIFEST}" "${staging}/crehn-images-${VERSION}.json"
install -o root -g root -m 0644 "${ARCHIVE}" "${staging}/crehn-images-${VERSION}.tar"
cat >"${staging}/promotion.env" <<EOF
version=${VERSION}
source_revision=${runtime_revision}
architecture=x86_64
manifest_sha256=$(sha256sum "${MANIFEST}" | awk '{print $1}')
release_sha256=${manifest_sha}
deploy_assets_sha256=${assets_sha}
EOF
chown root:root "${staging}/promotion.env"
chmod 0644 "${staging}/promotion.env"
touch "${staging}/SUCCESS"
mv "${staging}" "${EXPORT_DIR}"
trap - EXIT
printf '[PASS] exported_test_release version=%s revision=%s files=5\n' "${VERSION}" "${runtime_revision}"
