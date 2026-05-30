import Foundation

public struct PayloadEncoder: Sendable {
    public let maxBytes: Int
    public let minimumFeedRows: Int

    public init(maxBytes: Int = 480, minimumFeedRows: Int = 3) {
        self.maxBytes = maxBytes
        self.minimumFeedRows = minimumFeedRows
    }

    public func encode(_ status: TrafficStatus) throws -> Data {
        var projectRows = status.projects.map(row)
        var feedRows = status.feedItems.map(row)
        var moreCount = status.moreCount
        var moreFeedCount = status.moreFeedCount

        while true {
            let data = try dataFor(
                status: status,
                projects: projectRows,
                moreCount: moreCount,
                feedItems: feedRows,
                moreFeedCount: moreFeedCount
            )
            if data.count <= maxBytes {
                return data
            }
            if feedRows.count > minimumFeedRows {
                feedRows.removeLast()
                moreFeedCount = status.moreFeedCount + status.feedItems.count - feedRows.count
                continue
            }
            if !projectRows.isEmpty {
                projectRows.removeLast()
                moreCount = status.moreCount + status.projects.count - projectRows.count
                continue
            }
            if !feedRows.isEmpty {
                feedRows.removeLast()
                moreFeedCount = status.moreFeedCount + status.feedItems.count - feedRows.count
                continue
            }
            throw PayloadEncodingError.payloadCannotFit(maxBytes: maxBytes)
        }
    }

    private func dataFor(
        status: TrafficStatus,
        projects: [[Any]],
        moreCount: Int,
        feedItems: [[Any]],
        moreFeedCount: Int
    ) throws -> Data {
        let rows = try projects.map { row -> String in
            let encoded = try row.map(jsonValue).joined(separator: ",")
            return "[\(encoded)]"
        }.joined(separator: ",")
        let feedRows = try feedItems.map { row -> String in
            let encoded = try row.map(jsonValue).joined(separator: ",")
            return "[\(encoded)]"
        }.joined(separator: ",")

        let feedPayload = feedItems.isEmpty && moreFeedCount == 0
            ? ""
            : #","f":["# + feedRows + #"],"n":"# + String(moreFeedCount)
        let payload = #"{"v":"# + String(status.version)
            + #","t":"# + String(Int(status.timestamp.timeIntervalSince1970))
            + #","o":"# + (try jsonString(status.overall.rawValue))
            + #","p":["# + rows
            + #"],"m":"# + String(moreCount)
            + feedPayload
            + "}"
        return Data(payload.utf8)
    }

    private func row(_ project: ProjectStatus) -> [Any] {
        [
            project.id,
            project.name,
            project.light.rawValue,
            project.ageSeconds,
            project.reason.rawValue
        ]
    }

    private func row(_ item: PetFeedItem) -> [Any] {
        [
            item.projectID,
            item.title,
            item.body,
            item.light.rawValue,
            item.ageSeconds,
            item.reason.rawValue
        ]
    }

    private func jsonValue(_ value: Any) throws -> String {
        switch value {
        case let string as String:
            return try jsonString(string)
        case let integer as Int:
            return String(integer)
        default:
            throw PayloadEncodingError.unsupportedValue(String(describing: value))
        }
    }

    private func jsonString(_ value: String) throws -> String {
        let data = try JSONSerialization.data(withJSONObject: [value], options: [])
        guard let encoded = String(data: data, encoding: .utf8),
              encoded.count >= 2 else {
            throw PayloadEncodingError.unsupportedValue(value)
        }
        return String(encoded.dropFirst().dropLast())
    }
}

public enum PayloadEncodingError: Error, Equatable {
    case payloadCannotFit(maxBytes: Int)
    case unsupportedValue(String)
}
