#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

# Validate dotenv syntax without sourcing it. This is intentionally a separate
# fixed helper so the production staging gate never executes operator input.
blocked() { printf '[BLOCKED] %s\n' "$1" >&2; exit 3; }
[[ "$#" -eq 1 ]] || blocked 'runtime validator accepts exactly one file path'
readonly ENV_FILE="$1"
[[ -f "${ENV_FILE}" && ! -L "${ENV_FILE}" ]] || blocked 'runtime environment file is missing or unsafe'

declare -A seen=()
line_number=0
while IFS= read -r line || [[ -n "${line}" ]]; do
  line_number=$((line_number + 1))
  # A terminal CR is the harmless CRLF line ending used by Windows editors;
  # any remaining CR is data and is rejected as a shell-control character.
  line="${line%$'\r'}"
  [[ "${line}" != *$'\r'* ]] || blocked "runtime environment contains CR at line ${line_number}"
  [[ -z "${line}" || "${line}" == \#* ]] && continue
  [[ "${line}" == *=* ]] || blocked "runtime environment line ${line_number} is not KEY=VALUE"
  key="${line%%=*}"
  value="${line#*=}"
  [[ "${key}" =~ ^(CREHN|ALIYUN)_[A-Z0-9_]+$ ]] || blocked "runtime environment key is outside the approved prefixes at line ${line_number}"
  [[ -z "${seen[${key}]+x}" ]] || blocked "runtime environment contains duplicate key ${key}"
  seen["${key}"]=1
  # Values are data, never shell. The existing bootstrap-admin contract uses a
  # bcrypt value, so this key is accepted only as one complete bounded bcrypt
  # expression; it is still never sourced or evaled.
  if [[ "${key}" == CREHN_BOOTSTRAP_ADMIN_PASSWORD_HASH ]]; then
    [[ "${value}" =~ ^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$ ]] || blocked "bootstrap admin password hash must be a complete bcrypt value at line ${line_number}"
    continue
  fi
  [[ "${value}" =~ ^[A-Za-z0-9+/=:\._@%,\-]*$ ]] || blocked "runtime environment value contains a shell metacharacter at line ${line_number}"
done <"${ENV_FILE}"
[[ "${#seen[@]}" -gt 0 ]] || blocked 'runtime environment contains no approved values'
printf '[PASS] production runtime dotenv syntax is safe keys=%s\n' "${#seen[@]}"
