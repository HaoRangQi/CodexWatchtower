import Foundation
import SQLite3

public struct CodexStatusStore: Sendable {
    public let stateDatabaseURL: URL
    public let goalsDatabaseURL: URL
    private let processChecker: @Sendable () -> Bool

    public init(
        stateDatabaseURL: URL = FileManager.default.homeDirectoryForCurrentUser
            .appendingPathComponent(".codex/state_5.sqlite"),
        goalsDatabaseURL: URL = FileManager.default.homeDirectoryForCurrentUser
            .appendingPathComponent(".codex/goals_1.sqlite"),
        processChecker: @escaping @Sendable () -> Bool = CodexStatusStore.defaultProcessChecker
    ) {
        self.stateDatabaseURL = stateDatabaseURL
        self.goalsDatabaseURL = goalsDatabaseURL
        self.processChecker = processChecker
    }

    public func loadSnapshot() throws -> CodexSnapshot {
        let threads = try loadThreads()
        let jobs = try loadJobs(threads: threads)
        let goals = try loadGoals()

        return CodexSnapshot(
            threads: threads,
            jobs: jobs,
            goals: goals,
            codexProcessRunning: processChecker()
        )
    }

    private func loadThreads() throws -> [CodexThread] {
        try withReadOnlyDatabase(stateDatabaseURL) { db in
            let updatedColumn = try existingColumn(
                db: db,
                table: "threads",
                candidates: ["updated_at_ms", "updated_at"]
            )
            let columns = Set(tableColumns(db: db, table: "threads"))
            let archivedFilter = columns.contains("archived")
                ? "WHERE archived = 0 OR archived IS NULL"
                : ""
            let titleExpression = columns.contains("title") ? "COALESCE(title, '')" : "''"
            let previewExpression = columns.contains("preview") ? "COALESCE(preview, '')" : "''"
            let rolloutPathExpression = columns.contains("rollout_path") ? "COALESCE(rollout_path, '')" : "''"
            let divisor = updatedColumn == "updated_at_ms" ? 1_000.0 : 1.0
            let sql = """
            SELECT id, cwd, \(updatedColumn),
                   \(titleExpression),
                   \(previewExpression),
                   \(rolloutPathExpression)
            FROM threads
            \(archivedFilter)
            ORDER BY \(updatedColumn) DESC
            LIMIT 100
            """

            return try query(db: db, sql: sql) { statement in
                CodexThread(
                    id: columnText(statement, 0),
                    cwd: columnText(statement, 1),
                    updatedAt: Date(timeIntervalSince1970: Double(sqlite3_column_int64(statement, 2)) / divisor),
                    title: columnText(statement, 3),
                    preview: columnText(statement, 4),
                    rolloutPath: columnText(statement, 5)
                )
            }
        }
    }

    private func loadJobs(threads: [CodexThread]) throws -> [CodexAgentJob] {
        try withReadOnlyDatabase(stateDatabaseURL) { db in
            guard tableExists(db: db, table: "agent_jobs") else {
                return []
            }

            let columns = Set(tableColumns(db: db, table: "agent_jobs"))
            let hasThreadID = columns.contains("thread_id")
            let hasCWD = columns.contains("cwd")
            let startedColumn = columns.contains("started_at_ms") ? "started_at_ms" : "started_at"
            let updatedColumn = columns.contains("updated_at_ms") ? "updated_at_ms" : "updated_at"
            let divisor = startedColumn.hasSuffix("_ms") || updatedColumn.hasSuffix("_ms") ? 1_000.0 : 1.0

            if !hasThreadID && !hasCWD && tableExists(db: db, table: "agent_job_items") {
                return try loadJobsViaItems(
                    db: db,
                    threads: threads,
                    startedColumn: startedColumn,
                    updatedColumn: updatedColumn,
                    divisor: divisor,
                    hasMaxRuntime: columns.contains("max_runtime_seconds")
                )
            }

            let threadExpression = hasThreadID ? "thread_id" : "''"
            let cwdExpression = hasCWD ? "cwd" : "''"
            let maxRuntimeExpression = columns.contains("max_runtime_seconds") ? "max_runtime_seconds" : "NULL"
            let sql = """
            SELECT \(threadExpression), \(cwdExpression), status, \(startedColumn), \(updatedColumn), \(maxRuntimeExpression)
            FROM agent_jobs
            ORDER BY \(updatedColumn) DESC
            LIMIT 100
            """
            let cwdByThreadID = Dictionary(uniqueKeysWithValues: threads.map { ($0.id, $0.cwd) })

            return try query(db: db, sql: sql) { statement in
                let threadID = columnText(statement, 0)
                let cwd = columnText(statement, 1)
                let resolvedCWD = cwd.isEmpty ? (cwdByThreadID[threadID] ?? "agent_jobs") : cwd
                let maxRuntimeSeconds = sqlite3_column_type(statement, 5) == SQLITE_NULL
                    ? nil
                    : Int(sqlite3_column_int(statement, 5))

                return CodexAgentJob(
                    threadId: threadID,
                    cwd: resolvedCWD,
                    status: columnText(statement, 2),
                    startedAt: Date(timeIntervalSince1970: Double(sqlite3_column_int64(statement, 3)) / divisor),
                    updatedAt: Date(timeIntervalSince1970: Double(sqlite3_column_int64(statement, 4)) / divisor),
                    maxRuntimeSeconds: maxRuntimeSeconds
                )
            }
        }
    }

    private func loadJobsViaItems(
        db: OpaquePointer?,
        threads: [CodexThread],
        startedColumn: String,
        updatedColumn: String,
        divisor: Double,
        hasMaxRuntime: Bool
    ) throws -> [CodexAgentJob] {
        let maxRuntimeExpression = hasMaxRuntime ? "jobs.max_runtime_seconds" : "NULL"
        let sql = """
        SELECT COALESCE(items.assigned_thread_id, ''),
               COALESCE(threads.cwd, ''),
               jobs.status,
               jobs.\(startedColumn),
               jobs.\(updatedColumn),
               \(maxRuntimeExpression)
        FROM agent_jobs jobs
        LEFT JOIN agent_job_items items ON items.job_id = jobs.id
        LEFT JOIN threads ON threads.id = items.assigned_thread_id
        ORDER BY jobs.\(updatedColumn) DESC
        LIMIT 100
        """
        let cwdByThreadID = Dictionary(uniqueKeysWithValues: threads.map { ($0.id, $0.cwd) })

        return try query(db: db, sql: sql) { statement in
            let threadID = columnText(statement, 0)
            let cwd = columnText(statement, 1)
            let resolvedCWD = cwd.isEmpty ? (cwdByThreadID[threadID] ?? "agent_jobs") : cwd
            let maxRuntimeSeconds = sqlite3_column_type(statement, 5) == SQLITE_NULL
                ? nil
                : Int(sqlite3_column_int(statement, 5))

            return CodexAgentJob(
                threadId: threadID,
                cwd: resolvedCWD,
                status: columnText(statement, 2),
                startedAt: Date(timeIntervalSince1970: Double(sqlite3_column_int64(statement, 3)) / divisor),
                updatedAt: Date(timeIntervalSince1970: Double(sqlite3_column_int64(statement, 4)) / divisor),
                maxRuntimeSeconds: maxRuntimeSeconds
            )
        }
    }

    private func loadGoals() throws -> [CodexGoal] {
        guard goalsDatabaseURL.path(percentEncoded: false).isEmpty == false else {
            return []
        }
        guard FileManager.default.fileExists(atPath: goalsDatabaseURL.path) else {
            return []
        }

        return try withReadOnlyDatabase(goalsDatabaseURL) { db in
            guard tableExists(db: db, table: "thread_goals") else {
                return []
            }

            return try query(db: db, sql: "SELECT thread_id, status FROM thread_goals") { statement in
                CodexGoal(
                    threadId: columnText(statement, 0),
                    status: columnText(statement, 1)
                )
            }
        }
    }

    public static func defaultProcessChecker() -> Bool {
        let process = Process()
        process.executableURL = URL(fileURLWithPath: "/bin/ps")
        process.arguments = ["-axo", "comm,args"]

        let pipe = Pipe()
        process.standardOutput = pipe
        process.standardError = Pipe()

        do {
            try process.run()
        } catch {
            return false
        }

        let data = pipe.fileHandleForReading.readDataToEndOfFile()
        process.waitUntilExit()
        guard let output = String(data: data, encoding: .utf8) else {
            return false
        }

        return output
            .split(separator: "\n")
            .contains { line in
                let lowercased = line.lowercased()
                return lowercased.contains("codex")
                    && !lowercased.contains("codex-traffic")
                    && !lowercased.contains("rg -i")
            }
    }

    private func withReadOnlyDatabase<T>(_ url: URL, _ body: (OpaquePointer?) throws -> T) throws -> T {
        guard FileManager.default.fileExists(atPath: url.path) else {
            throw CodexStatusStoreError.databaseMissing(url.path)
        }

        var db: OpaquePointer?
        let flags = SQLITE_OPEN_READONLY | SQLITE_OPEN_FULLMUTEX
        let result = sqlite3_open_v2(url.path, &db, flags, nil)
        guard result == SQLITE_OK else {
            let message = db.flatMap { sqlite3_errmsg($0) }.map { String(cString: $0) } ?? "unknown sqlite error"
            sqlite3_close(db)
            throw CodexStatusStoreError.sqlite(message)
        }
        defer { sqlite3_close(db) }

        return try body(db)
    }

    private func existingColumn(db: OpaquePointer?, table: String, candidates: [String]) throws -> String {
        let columns = tableColumns(db: db, table: table)
        for candidate in candidates where columns.contains(candidate) {
            return candidate
        }
        throw CodexStatusStoreError.missingColumn(table: table, columns: candidates)
    }

    private func tableExists(db: OpaquePointer?, table: String) -> Bool {
        let sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=? LIMIT 1"
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &statement, nil) == SQLITE_OK else {
            return false
        }
        defer { sqlite3_finalize(statement) }

        sqlite3_bind_text(statement, 1, table, -1, SQLITE_TRANSIENT)
        return sqlite3_step(statement) == SQLITE_ROW
    }

    private func tableColumns(db: OpaquePointer?, table: String) -> [String] {
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(db, "PRAGMA table_info(\(table))", -1, &statement, nil) == SQLITE_OK else {
            return []
        }
        defer { sqlite3_finalize(statement) }

        var columns: [String] = []
        while sqlite3_step(statement) == SQLITE_ROW {
            columns.append(columnText(statement, 1))
        }
        return columns
    }

    private func query<T>(
        db: OpaquePointer?,
        sql: String,
        mapper: (OpaquePointer?) throws -> T
    ) throws -> [T] {
        var statement: OpaquePointer?
        let result = sqlite3_prepare_v2(db, sql, -1, &statement, nil)
        guard result == SQLITE_OK else {
            let message = db.flatMap { sqlite3_errmsg($0) }.map { String(cString: $0) } ?? "unknown sqlite error"
            throw CodexStatusStoreError.sqlite(message)
        }
        defer { sqlite3_finalize(statement) }

        var rows: [T] = []
        while true {
            let step = sqlite3_step(statement)
            if step == SQLITE_ROW {
                rows.append(try mapper(statement))
            } else if step == SQLITE_DONE {
                return rows
            } else {
                let message = db.flatMap { sqlite3_errmsg($0) }.map { String(cString: $0) } ?? "unknown sqlite error"
                throw CodexStatusStoreError.sqlite(message)
            }
        }
    }
}

public enum CodexStatusStoreError: Error, Equatable {
    case databaseMissing(String)
    case missingColumn(table: String, columns: [String])
    case sqlite(String)
}

private func columnText(_ statement: OpaquePointer?, _ index: Int32) -> String {
    guard let pointer = sqlite3_column_text(statement, index) else {
        return ""
    }
    return String(cString: pointer)
}

private let SQLITE_TRANSIENT = unsafeBitCast(-1, to: sqlite3_destructor_type.self)
