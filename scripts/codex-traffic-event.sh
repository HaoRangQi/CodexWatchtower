#!/usr/bin/env zsh
set -euo pipefail

event_kind="${1:-message}"
event_title="${2:-Codex 动态}"
event_body="${3:-收到实时事件}"
event_cwd="${CODEX_TRAFFIC_CWD:-${CODEX_WORKSPACE_DIR:-${PWD}}}"
event_log="${CODEX_TRAFFIC_EVENT_LOG:-${HOME}/.codex-traffic/events.jsonl}"
ntfy_topic="${CODEX_TRAFFIC_NTFY_TOPIC:-}"

case "$event_kind" in
  running|waiting_input|permission_required|completed|failed|network_stall|message) ;;
  *)
    echo "Unsupported event kind: $event_kind" >&2
    echo "Allowed: running waiting_input permission_required completed failed network_stall message" >&2
    exit 2
    ;;
esac

mkdir -p "$(dirname "$event_log")"

json_escape() {
  /usr/bin/python3 -c 'import json,sys; print(json.dumps(sys.stdin.read(), ensure_ascii=False)[1:-1])'
}

ts="$(date +%s)"
cwd_json="$(printf '%s' "$event_cwd" | json_escape)"
kind_json="$(printf '%s' "$event_kind" | json_escape)"
title_json="$(printf '%s' "$event_title" | json_escape)"
body_json="$(printf '%s' "$event_body" | json_escape)"

printf '{"v":1,"ts":%s,"kind":"%s","cwd":"%s","title":"%s","body":"%s"}\n' \
  "$ts" "$kind_json" "$cwd_json" "$title_json" "$body_json" >> "$event_log"

if [[ -n "$ntfy_topic" ]]; then
  curl --silent --show-error --max-time 2 \
    -H "Title: $event_title" \
    -d "$event_body" \
    "https://ntfy.sh/$ntfy_topic" >/dev/null || true
fi
