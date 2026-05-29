import Foundation

public struct PayloadEncoder: Sendable {
    public let maxBytes: Int

    public init(maxBytes: Int = 480) {
        self.maxBytes = maxBytes
    }

    public func encode(_ status: TrafficStatus) throws -> Data {
        var projectRows = status.projects.map(row)
        var moreCount = status.moreCount

        while true {
            let data = try dataFor(status: status, projects: projectRows, moreCount: moreCount)
            if data.count <= maxBytes {
                return data
            }
            guard !projectRows.isEmpty else {
                throw PayloadEncodingError.payloadCannotFit(maxBytes: maxBytes)
            }
            projectRows.removeLast()
            moreCount = status.moreCount + status.projects.count - projectRows.count
        }
    }

    private func dataFor(status: TrafficStatus, projects: [[Any]], moreCount: Int) throws -> Data {
        let rows = try projects.map { row -> String in
            let encoded = try row.map(jsonValue).joined(separator: ",")
            return "[\(encoded)]"
        }.joined(separator: ",")

        let payload = """
        {"v":\(status.version),"t":\(Int(status.timestamp.timeIntervalSince1970)),"o":\(try jsonString(status.overall.rawValue)),"p":[\(rows)],"m":\(moreCount)}
        """
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
