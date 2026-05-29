#!/usr/bin/env zsh
set -euo pipefail

project_root="$(cd "$(dirname "$0")/.." && pwd)"
event_script="$project_root/scripts/codex-traffic-event.sh"
payload="$(cat)"

/usr/bin/python3 - "$event_script" "$payload" <<'PY'
import json
import os
import subprocess
import sys

event_script = sys.argv[1]
payload = sys.argv[2] if len(sys.argv) > 2 else ""

try:
    root = json.loads(payload) if payload.strip() else {}
except json.JSONDecodeError:
    root = {}

def first_string(value, names):
    if isinstance(value, dict):
        for name in names:
            candidate = value.get(name)
            if isinstance(candidate, str) and candidate.strip():
                return candidate.strip()
        for child in value.values():
            found = first_string(child, names)
            if found:
                return found
    elif isinstance(value, list):
        for child in value:
            found = first_string(child, names)
            if found:
                return found
    return ""

event_name = first_string(root, [
    "hook_event_name",
    "event_name",
    "event",
    "type",
    "name",
])
event_key = event_name.lower().replace("-", "_")

cwd = first_string(root, [
    "cwd",
    "workspace_dir",
    "workspaceRoot",
    "project_root",
    "projectRoot",
]) or os.environ.get("CODEX_WORKSPACE_DIR") or os.getcwd()

if "permission" in event_key:
    kind = "permission_required"
    title = "等待授权"
    body = "Codex 需要你批准权限或命令"
elif "request_user_input" in event_key or "elicitation" in event_key or "input" in event_key:
    kind = "waiting_input"
    title = "等待你回复"
    body = "Codex 需要用户输入"
elif "error" in event_key or "failed" in event_key:
    kind = "failed"
    title = "任务失败"
    body = "Codex 报告失败，需要检查现场"
elif "stop" in event_key or "complete" in event_key or "turn_completed" in event_key:
    kind = "completed"
    title = "任务完成"
    body = "Codex 触发完成事件，回到项目确认结果"
elif "session_start" in event_key or "user_prompt_submit" in event_key or "tool" in event_key:
    kind = "running"
    title = "Codex 正在运行"
    body = "收到 Codex 实时 hook 事件"
else:
    kind = "message"
    title = "Codex 动态"
    body = "收到 Codex hook 事件"

message = first_string(root, ["message", "status_message", "reason", "summary", "prompt"])
if message:
    body = message[:96]

env = os.environ.copy()
env["CODEX_TRAFFIC_CWD"] = cwd
subprocess.run(
    [event_script, kind, title, body],
    env=env,
    stdout=subprocess.DEVNULL,
    stderr=subprocess.DEVNULL,
    check=False,
)
PY
