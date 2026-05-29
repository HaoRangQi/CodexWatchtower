import Foundation

public struct StatusEvaluator: Sendable {
    public var workWindowSeconds: TimeInterval
    public var recentWindowSeconds: TimeInterval
    public var staleGraceSeconds: TimeInterval
    public var maxProjects: Int

    public init(
        workWindowSeconds: TimeInterval = 90,
        recentWindowSeconds: TimeInterval = 15 * 60,
        staleGraceSeconds: TimeInterval = 60,
        maxProjects: Int = 6
    ) {
        self.workWindowSeconds = workWindowSeconds
        self.recentWindowSeconds = recentWindowSeconds
        self.staleGraceSeconds = staleGraceSeconds
        self.maxProjects = maxProjects
    }

    public func evaluate(snapshot: CodexSnapshot, now: Date = Date()) -> TrafficStatus {
        var threadsByProject: [String: [CodexThread]] = [:]
        for thread in snapshot.threads {
            threadsByProject[thread.cwd, default: []].append(thread)
        }

        let projectCWDs = Set(threadsByProject.keys).union(snapshot.jobs.map(\.cwd))
        var statuses: [ProjectStatus] = projectCWDs.map { cwd in
            let threads = threadsByProject[cwd] ?? []
            return statusForProject(cwd: cwd, threads: threads, snapshot: snapshot, now: now)
        }

        if statuses.isEmpty, snapshot.codexProcessRunning {
            statuses = [
                ProjectStatus(
                    id: "codex",
                    name: "codex",
                    light: .yellow,
                    ageSeconds: 0,
                    reason: .recent
                )
            ]
        }

        statuses.sort {
            if $0.light.priority != $1.light.priority {
                return $0.light.priority > $1.light.priority
            }
            return $0.ageSeconds < $1.ageSeconds
        }

        let overall = overallLight(for: statuses)
        let projects = Array(statuses.prefix(maxProjects))
        let moreCount = max(0, statuses.count - projects.count)
        let feedItems = statuses.map(feedItem)

        return TrafficStatus(
            version: 1,
            timestamp: now,
            overall: overall,
            projects: projects,
            moreCount: moreCount,
            feedItems: feedItems,
            moreFeedCount: 0
        )
    }

    private func statusForProject(
        cwd: String,
        threads: [CodexThread],
        snapshot: CodexSnapshot,
        now: Date
    ) -> ProjectStatus {
        let latestThread = threads.max { $0.updatedAt < $1.updatedAt }
        let latestThreadAge = latestThread.map { max(0, Int(now.timeIntervalSince($0.updatedAt))) } ?? Int.max
        let threadIds = Set(threads.map(\.id))
        let jobs = snapshot.jobs.filter { job in
            job.cwd == cwd || threadIds.contains(job.threadId)
        }
        let goals = snapshot.goals.filter { threadIds.contains($0.threadId) }

        let projectID = String(SHA1.hexDigest(cwd).prefix(8))
        let projectName = URL(fileURLWithPath: cwd).lastPathComponent.isEmpty
            ? cwd
            : URL(fileURLWithPath: cwd).lastPathComponent

        let status: (TrafficLight, ReasonCode, Int)
        if goals.contains(where: { $0.status == "blocked" }) {
            status = (.red, .blocked, latestThreadAge)
        } else if let staleJob = staleRunningJob(in: jobs, now: now) {
            status = (.red, .stale, max(0, Int(now.timeIntervalSince(staleJob.updatedAt))))
        } else if !snapshot.codexProcessRunning {
            status = (.red, .codexOff, latestThreadAge)
        } else if jobs.contains(where: { $0.status == "running" && now.timeIntervalSince($0.updatedAt) <= workWindowSeconds }) {
            let age = jobs
                .filter { $0.status == "running" }
                .map { max(0, Int(now.timeIntervalSince($0.updatedAt))) }
                .min() ?? latestThreadAge
            status = (.green, .work, age)
        } else if latestThreadAge <= Int(workWindowSeconds) {
            status = (.green, .work, latestThreadAge)
        } else if latestThreadAge <= Int(recentWindowSeconds) {
            status = (.yellow, .recent, latestThreadAge)
        } else {
            let jobAge = jobs.map { max(0, Int(now.timeIntervalSince($0.updatedAt))) }.min()
            status = (.red, .idle, jobAge ?? latestThreadAge)
        }

        return ProjectStatus(
            id: projectID,
            name: projectName,
            light: status.0,
            ageSeconds: status.2,
            reason: status.1
        )
    }

    private func staleRunningJob(in jobs: [CodexAgentJob], now: Date) -> CodexAgentJob? {
        jobs.first { job in
            guard job.status == "running", let maxRuntimeSeconds = job.maxRuntimeSeconds else {
                return false
            }
            return now.timeIntervalSince(job.startedAt) > TimeInterval(maxRuntimeSeconds) + staleGraceSeconds
                && now.timeIntervalSince(job.updatedAt) > staleGraceSeconds
        }
    }

    private func overallLight(for projects: [ProjectStatus]) -> TrafficLight {
        if projects.contains(where: { $0.light == .green }) {
            return .green
        }
        if projects.contains(where: { $0.light == .yellow }) {
            return .yellow
        }
        return .red
    }

    private func feedItem(for project: ProjectStatus) -> PetFeedItem {
        PetFeedItem(
            projectID: project.id,
            title: feedTitle(for: project),
            body: feedBody(for: project),
            light: project.light,
            ageSeconds: project.ageSeconds,
            reason: project.reason
        )
    }

    private func feedTitle(for project: ProjectStatus) -> String {
        switch project.reason {
        case .work:
            return "正在推进 \(project.name)"
        case .recent:
            return "\(project.name) 刚有动静"
        case .idle:
            return "\(project.name) 暂时安静"
        case .stale:
            return "\(project.name) 可能卡住"
        case .blocked:
            return "\(project.name) 需要处理阻塞"
        case .codexOff:
            return "\(project.name) 的 Codex 不在线"
        }
    }

    private func feedBody(for project: ProjectStatus) -> String {
        switch project.reason {
        case .work:
            return "\(project.ageSeconds) 秒内有新动作"
        case .recent:
            return "\(project.ageSeconds) 秒前更新，当前没确认推进"
        case .idle:
            return "超过近期窗口没有新活动"
        case .stale:
            return "运行任务超过预期，且没有进展信号"
        case .blocked:
            return "目标状态是 blocked，需要回到 Codex 看原因"
        case .codexOff:
            return "没有检测到 Codex 进程"
        }
    }
}

private extension TrafficLight {
    var priority: Int {
        switch self {
        case .green: 3
        case .yellow: 2
        case .red: 1
        }
    }
}
