import Foundation

public enum TrafficLight: String, Codable, Sendable {
    case green = "g"
    case yellow = "y"
    case red = "r"
}

public enum ReasonCode: String, Codable, Sendable {
    case work
    case recent
    case idle
    case stale
    case blocked
    case codexOff = "codex_off"
}

public struct CodexThread: Equatable, Sendable {
    public let id: String
    public let cwd: String
    public let updatedAt: Date

    public init(id: String, cwd: String, updatedAt: Date) {
        self.id = id
        self.cwd = cwd
        self.updatedAt = updatedAt
    }
}

public struct CodexAgentJob: Equatable, Sendable {
    public let threadId: String
    public let cwd: String
    public let status: String
    public let startedAt: Date
    public let updatedAt: Date
    public let maxRuntimeSeconds: Int?

    public init(
        threadId: String,
        cwd: String,
        status: String,
        startedAt: Date,
        updatedAt: Date,
        maxRuntimeSeconds: Int?
    ) {
        self.threadId = threadId
        self.cwd = cwd
        self.status = status
        self.startedAt = startedAt
        self.updatedAt = updatedAt
        self.maxRuntimeSeconds = maxRuntimeSeconds
    }
}

public struct CodexGoal: Equatable, Sendable {
    public let threadId: String
    public let status: String

    public init(threadId: String, status: String) {
        self.threadId = threadId
        self.status = status
    }
}

public struct CodexSnapshot: Equatable, Sendable {
    public let threads: [CodexThread]
    public let jobs: [CodexAgentJob]
    public let goals: [CodexGoal]
    public let codexProcessRunning: Bool

    public init(
        threads: [CodexThread],
        jobs: [CodexAgentJob],
        goals: [CodexGoal],
        codexProcessRunning: Bool
    ) {
        self.threads = threads
        self.jobs = jobs
        self.goals = goals
        self.codexProcessRunning = codexProcessRunning
    }
}

public enum CodexRealtimeEventKind: String, Codable, Sendable {
    case running
    case waitingInput = "waiting_input"
    case permissionRequired = "permission_required"
    case completed
    case failed
    case networkStall = "network_stall"
    case message
}

public struct CodexRealtimeEvent: Equatable, Sendable {
    public let timestamp: Date
    public let cwd: String
    public let kind: CodexRealtimeEventKind
    public let title: String
    public let body: String

    public init(
        timestamp: Date,
        cwd: String,
        kind: CodexRealtimeEventKind,
        title: String,
        body: String
    ) {
        self.timestamp = timestamp
        self.cwd = cwd
        self.kind = kind
        self.title = title
        self.body = body
    }
}

public struct ProjectStatus: Equatable, Sendable {
    public let id: String
    public let name: String
    public let light: TrafficLight
    public let ageSeconds: Int
    public let reason: ReasonCode

    public init(id: String, name: String, light: TrafficLight, ageSeconds: Int, reason: ReasonCode) {
        self.id = id
        self.name = name
        self.light = light
        self.ageSeconds = ageSeconds
        self.reason = reason
    }
}

public struct PetFeedItem: Equatable, Sendable {
    public let projectID: String
    public let title: String
    public let body: String
    public let light: TrafficLight
    public let ageSeconds: Int
    public let reason: ReasonCode

    public init(
        projectID: String,
        title: String,
        body: String,
        light: TrafficLight,
        ageSeconds: Int,
        reason: ReasonCode
    ) {
        self.projectID = projectID
        self.title = title
        self.body = body
        self.light = light
        self.ageSeconds = ageSeconds
        self.reason = reason
    }
}

public struct TrafficStatus: Equatable, Sendable {
    public let version: Int
    public let timestamp: Date
    public let overall: TrafficLight
    public let projects: [ProjectStatus]
    public let moreCount: Int
    public let feedItems: [PetFeedItem]
    public let moreFeedCount: Int

    public init(
        version: Int,
        timestamp: Date,
        overall: TrafficLight,
        projects: [ProjectStatus],
        moreCount: Int,
        feedItems: [PetFeedItem] = [],
        moreFeedCount: Int = 0
    ) {
        self.version = version
        self.timestamp = timestamp
        self.overall = overall
        self.projects = projects
        self.moreCount = moreCount
        self.feedItems = feedItems
        self.moreFeedCount = moreFeedCount
    }
}
