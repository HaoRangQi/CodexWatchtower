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
{"v":1,"t":1780039000,"o":"g","p":[["a1b2c3d4","loading","g",4,"work"]],"m":0}
```

| Field | Meaning |
| --- | --- |
| `v` | Protocol version. |
| `t` | Unix timestamp in seconds. |
| `o` | Overall light: `g`, `y`, or `r`. |
| `p` | Project rows: `[id, name, light, ageSeconds, reason]`. |
| `m` | Number of omitted project rows after truncation. |

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

