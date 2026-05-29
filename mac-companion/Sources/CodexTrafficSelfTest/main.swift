import CodexTrafficCore
import Foundation
import SQLite3

@main
struct CodexTrafficSelfTest {
    static func main() throws {
        try testFreshThreadActivityIsGreenWork()
        try testRecentInactiveThreadIsYellowRecent()
        try testBlockedGoalTurnsProjectRed()
        try testCodexOffIsRed()
        try testStalledRunningJobIsRed()
        try testRunningJobWithoutThreadIsGreen()
        try testOverallPrefersGreen()
        try testEncodesCompactPayloadContract()
        try testEncodesPetFeedContract()
        try testTruncatesProjectsToFitByteBudgetAndReportsMoreCount()
        try testTruncatesFeedToFitByteBudgetAndReportsMoreCount()
        try testLoadsSnapshotFromSQLiteStores()
        print("codex-traffic-selftest: all checks passed")
    }

    private static func testFreshThreadActivityIsGreenWork() throws {
        let now = Date(timeIntervalSince1970: 1_780_039_000)
        let snapshot = CodexSnapshot(
            threads: [
                CodexThread(id: "thread-1", cwd: "/tmp/loading", updatedAt: now.addingTimeInterval(-4))
            ],
            jobs: [],
            goals: [],
            codexProcessRunning: true
        )

        let result = StatusEvaluator().evaluate(snapshot: snapshot, now: now)

        try expect(result.overall == .green, "fresh thread overall")
        try expect(result.projects == [
            ProjectStatus(
                id: "dbf343c9",
                name: "loading",
                light: .green,
                ageSeconds: 4,
                reason: .work
            )
        ], "fresh thread project")
    }

    private static func testRecentInactiveThreadIsYellowRecent() throws {
        let now = Date(timeIntervalSince1970: 1_780_039_000)
        let snapshot = CodexSnapshot(
            threads: [
                CodexThread(id: "thread-1", cwd: "/tmp/loading", updatedAt: now.addingTimeInterval(-120))
            ],
            jobs: [],
            goals: [],
            codexProcessRunning: true
        )

        let result = StatusEvaluator().evaluate(snapshot: snapshot, now: now)

        try expect(result.overall == .yellow, "recent inactive overall")
        try expect(result.projects.first?.light == .yellow, "recent inactive light")
        try expect(result.projects.first?.reason == .recent, "recent inactive reason")
        try expect(result.projects.first?.ageSeconds == 120, "recent inactive age")
    }

    private static func testBlockedGoalTurnsProjectRed() throws {
        let now = Date(timeIntervalSince1970: 1_780_039_000)
        let snapshot = CodexSnapshot(
            threads: [
                CodexThread(id: "thread-1", cwd: "/tmp/loading", updatedAt: now.addingTimeInterval(-30))
            ],
            jobs: [],
            goals: [
                CodexGoal(threadId: "thread-1", status: "blocked")
            ],
            codexProcessRunning: true
        )

        let result = StatusEvaluator().evaluate(snapshot: snapshot, now: now)

        try expect(result.overall == .red, "blocked overall")
        try expect(result.projects.first?.light == .red, "blocked light")
        try expect(result.projects.first?.reason == .blocked, "blocked reason")
    }

    private static func testCodexOffIsRed() throws {
        let now = Date(timeIntervalSince1970: 1_780_039_000)
        let snapshot = CodexSnapshot(
            threads: [
                CodexThread(id: "thread-1", cwd: "/tmp/loading", updatedAt: now.addingTimeInterval(-2_000))
            ],
            jobs: [],
            goals: [],
            codexProcessRunning: false
        )

        let result = StatusEvaluator().evaluate(snapshot: snapshot, now: now)

        try expect(result.overall == .red, "codex off overall")
        try expect(result.projects.first?.reason == .codexOff, "codex off reason")
    }

    private static func testStalledRunningJobIsRed() throws {
        let now = Date(timeIntervalSince1970: 1_780_039_000)
        let snapshot = CodexSnapshot(
            threads: [
                CodexThread(id: "thread-1", cwd: "/tmp/loading", updatedAt: now.addingTimeInterval(-1_000))
            ],
            jobs: [
                CodexAgentJob(
                    threadId: "thread-1",
                    cwd: "/tmp/loading",
                    status: "running",
                    startedAt: now.addingTimeInterval(-500),
                    updatedAt: now.addingTimeInterval(-500),
                    maxRuntimeSeconds: 300
                )
            ],
            goals: [],
            codexProcessRunning: true
        )

        let result = StatusEvaluator().evaluate(snapshot: snapshot, now: now)

        try expect(result.overall == .red, "stalled job overall")
        try expect(result.projects.first?.reason == .stale, "stalled job reason")
    }

    private static func testRunningJobWithoutThreadIsGreen() throws {
        let now = Date(timeIntervalSince1970: 1_780_039_000)
        let snapshot = CodexSnapshot(
            threads: [],
            jobs: [
                CodexAgentJob(
                    threadId: "",
                    cwd: "/tmp/agent-only",
                    status: "running",
                    startedAt: now.addingTimeInterval(-20),
                    updatedAt: now.addingTimeInterval(-3),
                    maxRuntimeSeconds: 300
                )
            ],
            goals: [],
            codexProcessRunning: true
        )

        let result = StatusEvaluator().evaluate(snapshot: snapshot, now: now)

        try expect(result.overall == .green, "running job without thread overall")
        try expect(result.projects.first?.name == "agent-only", "running job without thread name")
        try expect(result.projects.first?.reason == .work, "running job without thread reason")
    }

    private static func testOverallPrefersGreen() throws {
        let now = Date(timeIntervalSince1970: 1_780_039_000)
        let snapshot = CodexSnapshot(
            threads: [
                CodexThread(id: "green", cwd: "/tmp/loading", updatedAt: now.addingTimeInterval(-5)),
                CodexThread(id: "yellow", cwd: "/tmp/other", updatedAt: now.addingTimeInterval(-300))
            ],
            jobs: [],
            goals: [],
            codexProcessRunning: true
        )

        let result = StatusEvaluator().evaluate(snapshot: snapshot, now: now)

        try expect(result.overall == .green, "overall priority")
        try expect(result.projects.count == 2, "overall project count")
    }

    private static func testEncodesCompactPayloadContract() throws {
        let status = TrafficStatus(
            version: 1,
            timestamp: Date(timeIntervalSince1970: 1_780_039_000),
            overall: .green,
            projects: [
                ProjectStatus(
                    id: "a1b2c3d4",
                    name: "loading",
                    light: .green,
                    ageSeconds: 4,
                    reason: .work
                )
            ],
            moreCount: 0,
            feedItems: [],
            moreFeedCount: 0
        )

        let data = try PayloadEncoder(maxBytes: 480).encode(status)
        let payload = String(decoding: data, as: UTF8.self)

        try expect(payload == #"{"v":1,"t":1780039000,"o":"g","p":[["a1b2c3d4","loading","g",4,"work"]],"m":0}"#, "payload contract")
        try expect(data.count <= 480, "payload max bytes")
    }

    private static func testEncodesPetFeedContract() throws {
        let status = TrafficStatus(
            version: 1,
            timestamp: Date(timeIntervalSince1970: 1_780_039_000),
            overall: .green,
            projects: [],
            moreCount: 0,
            feedItems: [
                PetFeedItem(
                    projectID: "a1b2c3d4",
                    title: "正在推进 loading",
                    body: "4 秒内有新动作",
                    light: .green,
                    ageSeconds: 4,
                    reason: .work
                )
            ],
            moreFeedCount: 0
        )

        let data = try PayloadEncoder(maxBytes: 480).encode(status)
        let payload = String(decoding: data, as: UTF8.self)

        try expect(payload == #"{"v":1,"t":1780039000,"o":"g","p":[],"m":0,"f":[["a1b2c3d4","正在推进 loading","4 秒内有新动作","g",4,"work"]],"n":0}"#, "feed payload contract")
        try expect(data.count <= 480, "feed payload max bytes")
    }

    private static func testTruncatesProjectsToFitByteBudgetAndReportsMoreCount() throws {
        let projects = (0..<20).map { index in
            ProjectStatus(
                id: String(format: "%08x", index),
                name: "project-\(index)-with-long-name",
                light: index == 0 ? .green : .yellow,
                ageSeconds: index,
                reason: index == 0 ? .work : .recent
            )
        }
        let status = TrafficStatus(
            version: 1,
            timestamp: Date(timeIntervalSince1970: 1_780_039_000),
            overall: .green,
            projects: projects,
            moreCount: 0,
            feedItems: [],
            moreFeedCount: 0
        )

        let data = try PayloadEncoder(maxBytes: 180).encode(status)
        guard let object = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let encodedProjects = object["p"] as? [[Any]],
              let moreCount = object["m"] as? Int else {
            throw SelfTestError.failed("payload truncation shape")
        }

        try expect(data.count <= 180, "truncated max bytes")
        try expect(encodedProjects.count < projects.count, "truncated project count")
        try expect(moreCount == projects.count - encodedProjects.count, "truncated more count")
    }

    private static func testTruncatesFeedToFitByteBudgetAndReportsMoreCount() throws {
        let feedItems = (0..<20).map { index in
            PetFeedItem(
                projectID: String(format: "%08x", index),
                title: "动态 \(index)",
                body: "这是一条比较长的桌宠动态 \(index)",
                light: index == 0 ? .green : .yellow,
                ageSeconds: index,
                reason: index == 0 ? .work : .recent
            )
        }
        let status = TrafficStatus(
            version: 1,
            timestamp: Date(timeIntervalSince1970: 1_780_039_000),
            overall: .green,
            projects: [],
            moreCount: 0,
            feedItems: feedItems,
            moreFeedCount: 0
        )

        let data = try PayloadEncoder(maxBytes: 220).encode(status)
        guard let object = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let encodedFeed = object["f"] as? [[Any]],
              let moreFeedCount = object["n"] as? Int else {
            throw SelfTestError.failed("feed truncation shape")
        }

        try expect(data.count <= 220, "truncated feed max bytes")
        try expect(encodedFeed.count < feedItems.count, "truncated feed count")
        try expect(moreFeedCount == feedItems.count - encodedFeed.count, "truncated feed more count")
    }

    private static func testLoadsSnapshotFromSQLiteStores() throws {
        let root = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString, isDirectory: true)
        try FileManager.default.createDirectory(at: root, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: root) }

        let stateURL = root.appendingPathComponent("state_5.sqlite")
        let goalsURL = root.appendingPathComponent("goals_1.sqlite")
        try makeStateDatabase(at: stateURL)
        try makeGoalsDatabase(at: goalsURL)

        let store = CodexStatusStore(
            stateDatabaseURL: stateURL,
            goalsDatabaseURL: goalsURL,
            processChecker: { true }
        )

        let snapshot = try store.loadSnapshot()

        try expect(snapshot.codexProcessRunning, "snapshot process running")
        try expect(snapshot.threads == [
            CodexThread(
                id: "thread-1",
                cwd: "/tmp/loading",
                updatedAt: Date(timeIntervalSince1970: 1_780_038_996)
            )
        ], "snapshot threads")
        try expect(snapshot.jobs == [
            CodexAgentJob(
                threadId: "thread-1",
                cwd: "/tmp/loading",
                status: "running",
                startedAt: Date(timeIntervalSince1970: 1_780_038_000),
                updatedAt: Date(timeIntervalSince1970: 1_780_038_990),
                maxRuntimeSeconds: 1200
            )
        ], "snapshot jobs")
        try expect(snapshot.goals == [
            CodexGoal(threadId: "thread-1", status: "active")
        ], "snapshot goals")
    }

    private static func makeStateDatabase(at url: URL) throws {
        var db: OpaquePointer?
        try open(url, &db)
        defer { sqlite3_close(db) }

        try exec(db, """
        CREATE TABLE threads (
            id TEXT PRIMARY KEY,
            cwd TEXT NOT NULL,
            updated_at INTEGER NOT NULL
        );
        CREATE TABLE agent_jobs (
            id TEXT PRIMARY KEY,
            thread_id TEXT NOT NULL,
            cwd TEXT,
            status TEXT NOT NULL,
            started_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL,
            max_runtime_seconds INTEGER
        );
        CREATE TABLE agent_job_items (
            id TEXT PRIMARY KEY,
            job_id TEXT NOT NULL
        );
        INSERT INTO threads VALUES ('thread-1', '/tmp/loading', 1780038996);
        INSERT INTO agent_jobs VALUES ('job-1', 'thread-1', '/tmp/loading', 'running', 1780038000, 1780038990, 1200);
        INSERT INTO agent_job_items VALUES ('item-1', 'job-1');
        """)
    }

    private static func makeGoalsDatabase(at url: URL) throws {
        var db: OpaquePointer?
        try open(url, &db)
        defer { sqlite3_close(db) }

        try exec(db, """
        CREATE TABLE thread_goals (
            thread_id TEXT PRIMARY KEY,
            status TEXT NOT NULL
        );
        INSERT INTO thread_goals VALUES ('thread-1', 'active');
        """)
    }

    private static func open(_ url: URL, _ db: inout OpaquePointer?) throws {
        let result = sqlite3_open(url.path, &db)
        try expect(result == SQLITE_OK, "sqlite open")
    }

    private static func exec(_ db: OpaquePointer?, _ sql: String) throws {
        var error: UnsafeMutablePointer<CChar>?
        let result = sqlite3_exec(db, sql, nil, nil, &error)
        if result != SQLITE_OK {
            let message = error.map { String(cString: $0) } ?? "unknown"
            sqlite3_free(error)
            throw SelfTestError.failed("sqlite exec: \(message)")
        }
    }

    private static func expect(_ condition: @autoclosure () -> Bool, _ message: String) throws {
        if !condition() {
            throw SelfTestError.failed(message)
        }
    }
}

private enum SelfTestError: Error, CustomStringConvertible {
    case failed(String)

    var description: String {
        switch self {
        case .failed(let message):
            return message
        }
    }
}
