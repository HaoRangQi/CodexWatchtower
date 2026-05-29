# Codex Traffic BLE Protocol v1

## UUIDs

- Service: `4F4C0001-6C6F-6164-696E-672D636F6465`
- Status characteristic: `4F4C0002-6C6F-6164-696E-672D636F6465`

## Characteristic

- Properties: `read`, `notify`
- Max payload: 480 UTF-8 bytes
- Android clients should request MTU 517. If MTU negotiation fails, the client
  continues with normal reads/notifications.

## Payload

```json
{"v":1,"t":1780039000,"o":"g","p":[["a1b2c3d4","loading","g",4,"work"]],"m":0,"f":[["a1b2c3d4","正在推进 loading","4 秒内有新动作","g",4,"work"]],"n":0}
```

| Field | Meaning |
| --- | --- |
| `v` | Protocol version. |
| `t` | Unix timestamp in seconds. |
| `o` | Overall light: `g`, `y`, or `r`. |
| `p` | Project rows: `[id, name, light, ageSeconds, reason]`. |
| `m` | Number of omitted project rows after truncation. |
| `f` | Optional pet feed rows: `[projectId, title, body, light, ageSeconds, reason]`. |
| `n` | Number of omitted pet feed rows after truncation. |

`f` prefers realtime event rows from `~/.codex-traffic/events.jsonl`, then falls
back to derived structured Codex status rows. It does not carry full conversation
text, `history.jsonl`, or large logs.

## Realtime Event Spool

Mac companion reads recent JSON Lines from:

```text
~/.codex-traffic/events.jsonl
```

Each row:

```json
{"v":1,"ts":1780039000,"kind":"permission_required","cwd":"/tmp/loading","title":"等待授权","body":"需要批准命令或权限"}
```

Allowed `kind` values:

- `running`
- `waiting_input`
- `permission_required`
- `completed`
- `failed`
- `network_stall`
- `message`

Events older than 15 minutes are ignored. Rows with malformed JSON or missing
`cwd`/`kind`/`ts` are ignored.

## Light Semantics

- `g`: Codex is doing work.
- `y`: Recent activity exists, but current state is uncertain or waiting.
- `r`: Idle, blocked, Codex is not running, or stale.

## Reason Codes

- `work`
- `recent`
- `idle`
- `stale`
- `blocked`
- `codex_off`
