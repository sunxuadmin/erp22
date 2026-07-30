#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

MODE=""
DEPLOY_ROOT=""
ARCHIVE=""
RUNTIME_ENV=""
VERSION=""
RELEASE_MANIFEST=""
RELEASE_ARCHIVE=""
SOURCE_REVISION=""

status() {
  printf '[%s] %s\n' "$1" "$2"
}

fail() {
  status FAILED "$1" >&2
  exit 1
}

while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --mode) MODE="${2:-}"; shift 2 ;;
    --deploy-root) DEPLOY_ROOT="${2:-}"; shift 2 ;;
    --archive) ARCHIVE="${2:-}"; shift 2 ;;
    --runtime-env) RUNTIME_ENV="${2:-}"; shift 2 ;;
    --version) VERSION="${2:-}"; shift 2 ;;
    --source-revision) SOURCE_REVISION="${2:-}"; shift 2 ;;
    --release-manifest) RELEASE_MANIFEST="${2:-}"; shift 2 ;;
    --release-archive) RELEASE_ARCHIVE="${2:-}"; shift 2 ;;
    *) fail "Unknown argument: $1" ;;
  esac
done

[[ "$(id -u)" -eq 0 ]] || fail "Asset staging must run as root"
[[ "${MODE}" == "test-source" || "${MODE}" == "prod-release" ]] ||
  fail "Mode must be test-source or prod-release"
[[ "${DEPLOY_ROOT}" =~ ^/srv/crehn-(test|prod|stage)$ ]] ||
  fail "Deployment root is outside the allowed project directories"
[[ "${VERSION}" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]+)?$ ]] ||
  fail "Version format is invalid"
[[ "${SOURCE_REVISION}" =~ ^[0-9a-fA-F]{40,64}$ ]] ||
  fail "Source revision format is invalid"
[[ -f "${ARCHIVE}" && -f "${RUNTIME_ENV}" ]] ||
  fail "Source/deployment archive or runtime environment is missing"

SOURCE_ROOT="${DEPLOY_ROOT}/source"
RUNTIME_ROOT="${DEPLOY_ROOT}/runtime"
STAGING="${DEPLOY_ROOT}/.source-${SOURCE_REVISION}.incomplete"
PREVIOUS="${DEPLOY_ROOT}/.source-previous"

mkdir -p "${DEPLOY_ROOT}" "${RUNTIME_ROOT}"
rm -rf -- "${STAGING}"
mkdir -p "${STAGING}"
tar -xzf "${ARCHIVE}" -C "${STAGING}"
[[ -f "${STAGING}/deploy/compose.yml" ]] ||
  fail "Staged archive does not contain deploy/compose.yml"
[[ -f "${STAGING}/deploy/linux/crehn-deploy.sh" ]] ||
  fail "Staged archive does not contain the Linux deployment entry"
chmod 0755 "${STAGING}/deploy/linux/"*.sh

if [[ -e "${PREVIOUS}" ]]; then
  rm -rf -- "${PREVIOUS}"
fi
if [[ -e "${SOURCE_ROOT}" ]]; then
  mv "${SOURCE_ROOT}" "${PREVIOUS}"
fi
mv "${STAGING}" "${SOURCE_ROOT}"

RUNTIME_NAME="$([[ "${MODE}" == "test-source" ]] && printf test || printf prod)"
RUNTIME_TARGET="${RUNTIME_ROOT}/${RUNTIME_NAME}-${VERSION}.env"
install -o root -g root -m 0600 "${RUNTIME_ENV}" "${RUNTIME_TARGET}"

if [[ "${MODE}" == "prod-release" ]]; then
  [[ -f "${RELEASE_MANIFEST}" && -f "${RELEASE_ARCHIVE}" ]] ||
    fail "Production staging requires both release manifest and archive"
  RELEASE_TARGET="${DEPLOY_ROOT}/releases/${VERSION}"
  [[ ! -e "${RELEASE_TARGET}" ]] ||
    fail "Production release ${VERSION} already exists and is immutable"
  mkdir -p "${RELEASE_TARGET}"
  install -o root -g root -m 0600 "${RELEASE_MANIFEST}" "${RELEASE_TARGET}/crehn-images-${VERSION}.json"
  install -o root -g root -m 0600 "${RELEASE_ARCHIVE}" "${RELEASE_TARGET}/crehn-images-${VERSION}.tar"
  touch "${RELEASE_TARGET}/SUCCESS"
fi

status PASS "assets_staged mode=${MODE} root=${DEPLOY_ROOT} revision=${SOURCE_REVISION}"
