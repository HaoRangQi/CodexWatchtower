import Foundation
import Network

public final class HTTPStatusServer: @unchecked Sendable {
    public static let defaultPort: UInt16 = 8765
    public static let bonjourType = "_codextraffic._tcp"
    public static let bonjourName = "Codex Watchtower"

    private let port: UInt16
    private let payloadProvider: @Sendable () throws -> Data
    private let queue = DispatchQueue(label: "com.codextraffic.http-status")
    private var listener: NWListener?

    public init(
        port: UInt16 = HTTPStatusServer.defaultPort,
        payloadProvider: @escaping @Sendable () throws -> Data
    ) {
        self.port = port
        self.payloadProvider = payloadProvider
    }

    public func start() throws {
        guard listener == nil else {
            return
        }

        let listenerPort = NWEndpoint.Port(rawValue: port)!
        let listener = try NWListener(using: .tcp, on: listenerPort)
        listener.service = NWListener.Service(
            name: HTTPStatusServer.bonjourName,
            type: HTTPStatusServer.bonjourType
        )
        listener.newConnectionHandler = { [weak self] connection in
            self?.handle(connection)
        }
        listener.stateUpdateHandler = { state in
            switch state {
            case .ready:
                let actualPort = listener.port?.rawValue ?? listenerPort.rawValue
                fputs("Codex Watchtower HTTP: listening on port \(actualPort)\n", stderr)
            case .failed(let error):
                fputs("Codex Watchtower HTTP: listener failed: \(error)\n", stderr)
            case .waiting(let error):
                fputs("Codex Watchtower HTTP: listener waiting: \(error)\n", stderr)
            default:
                break
            }
        }
        self.listener = listener
        listener.start(queue: queue)
    }

    public func stop() {
        listener?.cancel()
        listener = nil
    }

    private func handle(_ connection: NWConnection) {
        connection.receive(minimumIncompleteLength: 1, maximumLength: 4096) { [weak self] data, _, _, error in
            guard let self else {
                connection.cancel()
                return
            }

            if let error {
                fputs("Codex Watchtower HTTP: receive failed: \(error)\n", stderr)
                connection.cancel()
                return
            }

            let request = data.flatMap { String(data: $0, encoding: .utf8) } ?? ""
            let response = self.response(for: request)
            connection.send(content: response, completion: .contentProcessed { _ in
                connection.cancel()
            })
        }
        connection.start(queue: queue)
    }

    private func response(for request: String) -> Data {
        let requestLine = request
            .split(separator: "\r\n", maxSplits: 1, omittingEmptySubsequences: false)
            .first
            .map(String.init) ?? ""
        let parts = requestLine.split(separator: " ", maxSplits: 2).map(String.init)
        let method = parts.first ?? ""
        let path = parts.dropFirst().first ?? "/"

        guard method == "GET" else {
            return httpResponse(
                status: "405 Method Not Allowed",
                contentType: "application/json; charset=utf-8",
                body: Data(#"{"error":"method_not_allowed"}"#.utf8)
            )
        }

        if path == "/health" {
            return httpResponse(
                status: "200 OK",
                contentType: "application/json; charset=utf-8",
                body: Data(#"{"ok":true}"#.utf8)
            )
        }

        guard path == "/" || path == "/status" else {
            return httpResponse(
                status: "404 Not Found",
                contentType: "application/json; charset=utf-8",
                body: Data(#"{"error":"not_found"}"#.utf8)
            )
        }

        do {
            return httpResponse(
                status: "200 OK",
                contentType: "application/json; charset=utf-8",
                body: try payloadProvider()
            )
        } catch {
            let message = String(describing: error)
                .replacingOccurrences(of: "\"", with: "'")
                .prefix(160)
            return httpResponse(
                status: "500 Internal Server Error",
                contentType: "application/json; charset=utf-8",
                body: Data(#"{"error":"payload_failed","message":"\#(message)"}"#.utf8)
            )
        }
    }

    private func httpResponse(status: String, contentType: String, body: Data) -> Data {
        var response = Data()
        response.append(Data("""
        HTTP/1.1 \(status)\r
        Content-Type: \(contentType)\r
        Content-Length: \(body.count)\r
        Cache-Control: no-store\r
        Access-Control-Allow-Origin: *\r
        Connection: close\r
        \r

        """.utf8))
        response.append(body)
        return response
    }
}
