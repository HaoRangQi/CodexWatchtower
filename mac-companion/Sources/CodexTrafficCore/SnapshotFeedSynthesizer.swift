import Foundation

public struct SnapshotFeedSynthesizer: Sendable {
    public var workWindowSeconds: TimeInterval
    public var recentWindowSeconds: TimeInterval
    public var staleGraceSeconds: TimeInterval
    public var maxEvents: Int

    public init(
        workWindowSeconds: TimeInterval = 90,
        recentWindowSeconds: TimeInterval = 15 * 60,
        staleGraceSeconds: TimeInterval = 60,
        maxEvents: Int = 12
    ) {
        self.workWindowSeconds = workWindowSeconds
        self.recentWindowSeconds = recentWindowSeconds
        self.staleGraceSeconds = staleGraceSeconds
        self.maxEvents = maxEvents
    }

    public func synthesize(snapshot: CodexSnapshot, now: Date = Date()) -> [CodexRealtimeEvent] {
        var events: [CodexRealtimeEvent] = []
        let threadsByID = Dictionary(uniqueKeysWithValues: snapshot.threads.map { ($0.id, $0) })
        let goalsByThreadID = Dictionary(uniqueKeysWithValues: snapshot.goals.map { ($0.threadId, $0.status) })

        for thread in snapshot.threads {
            if goalsByThreadID[thread.id] == "blocked" {
                events.append(event(
                    kind: .waitingInput,
                    cwd: thread.cwd,
                    timestamp: thread.updatedAt,
                    title: "等待你处理",
                    body: "\(projectName(thread.cwd)) 当前目标被标记为 blocked"
                ))
                continue
            }

            let age = now.timeIntervalSince(thread.updatedAt)
            if age <= workWindowSeconds {
                events.append(event(
                    kind: .running,
                    cwd: thread.cwd,
                    timestamp: thread.updatedAt,
                    title: "正在推进 \(projectName(thread.cwd))",
                    body: "\(max(0, Int(age))) 秒内有结构化状态更新"
                ))
            } else if age <= recentWindowSeconds {
                events.append(event(
                    kind: .message,
                    cwd: thread.cwd,
                    timestamp: thread.updatedAt,
                    title: "\(projectName(thread.cwd)) 刚有动静",
                    body: "\(max(0, Int(age))) 秒前更新，当前未确认持续推进"
                ))
            }
        }

        for job in snapshot.jobs {
            let cwd = resolvedCWD(for: job, threadsByID: threadsByID)
            if isStale(job: job, now: now) {
                events.append(event(
                    kind: .networkStall,
                    cwd: cwd,
                    timestamp: job.updatedAt,
                    title: "\(projectName(cwd)) 可能卡住",
                    body: "运行任务超过预期，且没有进展信号"
                ))
            } else if job.status == "running", now.timeIntervalSince(job.updatedAt) <= workWindowSeconds {
                events.append(event(
                    kind: .running,
                    cwd: cwd,
                    timestamp: job.updatedAt,
                    title: "正在推进 \(projectName(cwd))",
                    body: "检测到运行中的 agent job"
                ))
            } else if job.status == "failed" {
                events.append(event(
                    kind: .failed,
                    cwd: cwd,
                    timestamp: job.updatedAt,
                    title: "\(projectName(cwd)) 任务失败",
                    body: "agent job 报告 failed"
                ))
            }
        }

        if !snapshot.codexProcessRunning, let latestThread = snapshot.threads.max(by: { $0.updatedAt < $1.updatedAt }) {
            events.append(event(
                kind: .networkStall,
                cwd: latestThread.cwd,
                timestamp: now,
                title: "Codex 不在线",
                body: "\(projectName(latestThread.cwd)) 没有检测到 Codex 进程"
            ))
        }

        return deduplicate(events.sorted { $0.timestamp > $1.timestamp })
            .prefix(maxEvents)
            .map { $0 }
    }

    private func event(
        kind: CodexRealtimeEventKind,
        cwd: String,
        timestamp: Date,
        title: String,
        body: String
    ) -> CodexRealtimeEvent {
        CodexRealtimeEvent(
            timestamp: timestamp,
            cwd: cwd,
            kind: kind,
            title: String(title.prefix(96)),
            body: String(body.prefix(96))
        )
    }

    private func resolvedCWD(for job: CodexAgentJob, threadsByID: [String: CodexThread]) -> String {
        if !job.cwd.isEmpty, job.cwd != "agent_jobs" {
            return job.cwd
        }
        return threadsByID[job.threadId]?.cwd ?? job.cwd
    }

    private func isStale(job: CodexAgentJob, now: Date) -> Bool {
        guard job.status == "running", let maxRuntimeSeconds = job.maxRuntimeSeconds else {
            return false
        }
        return now.timeIntervalSince(job.startedAt) > TimeInterval(maxRuntimeSeconds) + staleGraceSeconds
            && now.timeIntervalSince(job.updatedAt) > staleGraceSeconds
    }

    private func deduplicate(_ events: [CodexRealtimeEvent]) -> [CodexRealtimeEvent] {
        var seen = Set<String>()
        var result: [CodexRealtimeEvent] = []
        for event in events {
            let key = "\(event.cwd)-\(event.kind.rawValue)-\(event.title)"
            guard !seen.contains(key) else {
                continue
            }
            seen.insert(key)
            result.append(event)
        }
        return result
    }

    private func projectName(_ cwd: String) -> String {
        let name = URL(fileURLWithPath: cwd).lastPathComponent
        return name.isEmpty ? cwd : name
    }
}
