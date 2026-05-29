#!/usr/bin/env bash
set -euo pipefail

APP_ASAR="${CODEX_APP_ASAR:-/Applications/Codex.app/Contents/Resources/app.asar}"
OUT_DIR="android/app/src/main/assets"
OUT_FILE="${OUT_DIR}/codex_bsod_spritesheet.webp"

if [[ ! -f "${APP_ASAR}" ]]; then
  echo "Codex app.asar not found: ${APP_ASAR}" >&2
  exit 1
fi

if ! command -v npx >/dev/null 2>&1; then
  echo "npx is required to read app.asar" >&2
  exit 1
fi

tmp_dir="$(mktemp -d)"
trap 'rm -rf "${tmp_dir}"' EXIT

npx --yes asar extract "${APP_ASAR}" "${tmp_dir}/asar" >/dev/null

src="$(find "${tmp_dir}/asar/webview/assets" -maxdepth 1 -name 'bsod-spritesheet-v4-*.webp' -print | head -n 1)"
if [[ ! -f "${src}" ]]; then
  echo "BSOD spritesheet not found in ${APP_ASAR}" >&2
  exit 1
fi

mkdir -p "${OUT_DIR}"
cp "${src}" "${OUT_FILE}"
echo "Synced ${OUT_FILE}"
