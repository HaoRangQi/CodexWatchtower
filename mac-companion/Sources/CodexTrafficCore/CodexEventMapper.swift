import Foundation

public struct CodexEventMapper: Sendable {
    public init() {}

    public func feedItem(for event: CodexRealtimeEvent, now: Date) -> PetFeedItem {
        let mapped = projectStatus(for: event.kind)
        return PetFeedItem(
            projectID: String(SHA1.hexDigest(event.cwd).prefix(8)),
            title: event.title,
            body: event.body,
            light: mapped.0,
            ageSeconds: max(0, Int(now.timeIntervalSince(event.timestamp))),
            reason: mapped.1
        )
    }

    public func projectStatus(for eventKind: CodexRealtimeEventKind) -> (TrafficLight, ReasonCode) {
        switch eventKind {
        case .running:
            return (.green, .work)
        case .waitingInput, .permissionRequired:
            return (.red, .blocked)
        case .completed, .message:
            return (.yellow, .recent)
        case .failed, .networkStall:
            return (.red, .stale)
        }
    }
}
