import Foundation

public struct ThreadFeedSynthesizer: Sendable {
    public var maxItems: Int
    public var maxRolloutBytes: Int

    public init(maxItems: Int = 12, maxRolloutBytes: Int = 256_000) {
        self.maxItems = maxItems
        self.maxRolloutBytes = maxRolloutBytes
    }

    public func synthesize(snapshot: CodexSnapshot, now: Date = Date()) -> [CodexRealtimeEvent] {
        snapshot.threads
            .sorted { $0.updatedAt > $1.updatedAt }
            .prefix(maxItems)
            .compactMap { event(for: $0, now: now) }
    }

    private func event(for thread: CodexThread, now: Date) -> CodexRealtimeEvent? {
        let latest = latestThreadItem(from: thread.rolloutPath)
        let title = compactText(latest?.title ?? thread.preview, fallback: thread.title)
        let body = compactText(latest?.body ?? thread.preview, fallback: projectName(thread.cwd))
        let kind = latest?.kind ?? inferredKind(for: thread, now: now)

        guard !title.isEmpty || !body.isEmpty else {
            return nil
        }

        return CodexRealtimeEvent(
            timestamp: latest?.timestamp ?? thread.updatedAt,
            cwd: thread.cwd,
            kind: kind,
            title: String(title.prefix(96)),
            body: String(body.prefix(96))
        )
    }

    private func latestThreadItem(from rolloutPath: String) -> ThreadItem? {
        guard !rolloutPath.isEmpty else {
            return nil
        }
        let url = URL(fileURLWithPath: rolloutPath)
        guard let data = try? tailData(url: url, maxBytes: maxRolloutBytes),
              let text = String(data: data, encoding: .utf8) else {
            return nil
        }

        var latest: ThreadItem?
        for line in text.split(separator: "\n", omittingEmptySubsequences: true).reversed() {
            guard let data = line.data(using: .utf8),
                  let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let item = item(from: object) else {
                continue
            }
            if latest == nil || item.rank > latest!.rank {
                latest = item
            }
            if item.rank >= ThreadItem.agentRank {
                return item
            }
        }
        return latest
    }

    private func item(from object: [String: Any]) -> ThreadItem? {
        guard let timestamp = timestamp(from: object["timestamp"]) else {
            return nil
        }

        if let payload = object["payload"] as? [String: Any] {
            switch payload["type"] as? String {
            case "agent_message":
                return ThreadItem(
                    timestamp: timestamp,
                    title: compactText(payload["message"] as? String, fallback: "Codex 有新输出"),
                    body: "Codex 输出",
                    kind: .message,
                    rank: ThreadItem.agentRank
                )
            case "user_message":
                return ThreadItem(
                    timestamp: timestamp,
                    title: compactText(payload["message"] as? String, fallback: "用户发起请求"),
                    body: "用户请求",
                    kind: .message,
                    rank: 50
                )
            case "patch_apply_end":
                return ThreadItem(
                    timestamp: timestamp,
                    title: payload["success"] as? Bool == false ? "文件修改失败" : "文件修改已应用",
                    body: compactText(payload["stdout"] as? String, fallback: "Codex 更新了文件"),
                    kind: payload["success"] as? Bool == false ? .failed : .running,
                    rank: 80
                )
            case "task_complete":
                return ThreadItem(
                    timestamp: timestamp,
                    title: "任务完成",
                    body: compactText(payload["last_agent_message"] as? String, fallback: "Codex 已完成最近任务"),
                    kind: .completed,
                    rank: 70
                )
            default:
                break
            }
        }

        if object["type"] as? String == "response_item",
           let payload = object["payload"] as? [String: Any] {
            switch payload["type"] as? String {
            case "function_call", "custom_tool_call", "mcp_tool_call":
                return ThreadItem(
                    timestamp: timestamp,
                    title: "正在调用工具",
                    body: compactText(payload["name"] as? String ?? payload["arguments"] as? String, fallback: "Codex 正在执行工具调用"),
                    kind: .running,
                    rank: 60
                )
            case "message":
                return ThreadItem(
                    timestamp: timestamp,
                    title: "Codex 有新消息",
                    body: compactText(payload["status"] as? String, fallback: "消息已更新"),
                    kind: .message,
                    rank: 55
                )
            default:
                break
            }
        }

        return nil
    }

    private func tailData(url: URL, maxBytes: Int) throws -> Data {
        let attributes = try FileManager.default.attributesOfItem(atPath: url.path)
        let size = attributes[.size] as? UInt64 ?? 0
        let handle = try FileHandle(forReadingFrom: url)
        defer { try? handle.close() }

        let readSize = min(UInt64(maxBytes), size)
        try handle.seek(toOffset: size - readSize)
        return try handle.readToEnd() ?? Data()
    }

    private func timestamp(from value: Any?) -> Date? {
        guard let raw = value as? String else {
            return nil
        }
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter.date(from: raw)
    }

    private func inferredKind(for thread: CodexThread, now: Date) -> CodexRealtimeEventKind {
        now.timeIntervalSince(thread.updatedAt) <= 90 ? .running : .message
    }

    private func compactText(_ text: String?, fallback: String) -> String {
        let cleaned = (text ?? "")
            .replacingOccurrences(of: "\n", with: " ")
            .replacingOccurrences(of: "\t", with: " ")
            .split(separator: " ")
            .joined(separator: " ")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        if !cleaned.isEmpty {
            return cleaned
        }
        return fallback
    }

    private func projectName(_ cwd: String) -> String {
        let name = URL(fileURLWithPath: cwd).lastPathComponent
        return name.isEmpty ? cwd : name
    }
}

private struct ThreadItem {
    static let agentRank = 90

    let timestamp: Date
    let title: String
    let body: String
    let kind: CodexRealtimeEventKind
    let rank: Int
}
