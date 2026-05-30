#!/usr/bin/env zsh
set -euo pipefail

project_root="$(cd "$(dirname "$0")/.." && pwd)"
event_script="$project_root/scripts/codex-traffic-event.sh"
existing_notify="${CODEX_TRAFFIC_EXISTING_NOTIFY:-/Users/macos/.codex/computer-use/Codex Computer Use.app/Contents/SharedSupport/SkyComputerUseClient.app/Contents/MacOS/SkyComputerUseClient}"
event_cwd="${CODEX_WORKSPACE_DIR:-${CODEX_PROJECT_DIR:-${PWD}}}"

if [[ "$event_cwd" == "/" || -z "$event_cwd" ]]; then
  event_cwd="$project_root"
fi

CODEX_TRAFFIC_CWD="$event_cwd" \
  "$event_script" completed "Codex 需要你看一眼" "Codex 触发了 notify hook，回到项目确认结果。" || true

if [[ -x "$existing_notify" ]]; then
  "$existing_notify" "$@" || true
fi
