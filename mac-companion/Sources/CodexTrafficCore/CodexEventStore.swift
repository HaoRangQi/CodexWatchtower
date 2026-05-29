import Foundation

public struct CodexEventStore: Sendable {
    public static let defaultEventLogURL = FileManager.default.homeDirectoryForCurrentUser
        .appendingPathComponent(".codex-traffic/events.jsonl")

    public let eventLogURL: URL
    public let maxEventAgeSeconds: TimeInterval
    public let maxEvents: Int

    public init(
        eventLogURL: URL = CodexEventStore.defaultEventLogURL,
        maxEventAgeSeconds: TimeInterval = 15 * 60,
        maxEvents: Int = 24
    ) {
        self.eventLogURL = eventLogURL
        self.maxEventAgeSeconds = maxEventAgeSeconds
        self.maxEvents = maxEvents
    }

    public func loadEvents(now: Date = Date()) throws -> [CodexRealtimeEvent] {
        guard FileManager.default.fileExists(atPath: eventLogURL.path) else {
            return []
        }

        let data = try Data(contentsOf: eventLogURL)
        guard let contents = String(data: data, encoding: .utf8) else {
            return []
        }

        let decoder = JSONDecoder()
        let events = contents
            .split(separator: "\n", omittingEmptySubsequences: true)
            .suffix(maxEvents * 3)
            .compactMap { line -> CodexRealtimeEvent? in
                guard let lineData = String(line).data(using: .utf8),
                      let row = try? decoder.decode(EventRow.self, from: lineData),
                      let event = row.event else {
                    return nil
                }
                return event
            }
            .filter { now.timeIntervalSince($0.timestamp) <= maxEventAgeSeconds }
            .sorted { $0.timestamp > $1.timestamp }

        return Array(events.prefix(maxEvents))
    }
}

private struct EventRow: Decodable {
    let v: Int?
    let ts: Double?
    let kind: CodexRealtimeEventKind?
    let cwd: String?
    let title: String?
    let body: String?

    var event: CodexRealtimeEvent? {
        guard (v ?? 1) == 1,
              let ts,
              let kind,
              let cwd,
              cwd.isEmpty == false else {
            return nil
        }

        return CodexRealtimeEvent(
            timestamp: Date(timeIntervalSince1970: ts),
            cwd: cwd,
            kind: kind,
            title: sanitized(title, fallback: kind.defaultTitle),
            body: sanitized(body, fallback: kind.defaultBody)
        )
    }

    private func sanitized(_ value: String?, fallback: String) -> String {
        let trimmed = (value ?? "")
            .replacingOccurrences(of: "\n", with: " ")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed.isEmpty == false else {
            return fallback
        }
        return String(trimmed.prefix(96))
    }
}

private extension CodexRealtimeEventKind {
    var defaultTitle: String {
        switch self {
        case .running:
            return "Codex 正在推进"
        case .waitingInput:
            return "等待你回复"
        case .permissionRequired:
            return "等待授权"
        case .completed:
            return "任务完成"
        case .failed:
            return "任务失败"
        case .networkStall:
            return "网络可能卡住"
        case .message:
            return "Codex 动态"
        }
    }

    var defaultBody: String {
        switch self {
        case .running:
            return "收到实时运行事件"
        case .waitingInput:
            return "Codex 需要你回到项目处理输入"
        case .permissionRequired:
            return "Codex 需要你批准权限或命令"
        case .completed:
            return "Codex 已经结束这一轮"
        case .failed:
            return "Codex 报告失败，需要检查现场"
        case .networkStall:
            return "长时间没有新进展，可能是网络或服务卡住"
        case .message:
            return "收到实时事件"
        }
    }
}
