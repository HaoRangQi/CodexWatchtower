import CodexTrafficCore
import Foundation

let arguments = CommandLine.arguments.dropFirst()
let once = arguments.contains("--once")
let interval = argumentValue("--interval").flatMap(Double.init) ?? 2.0
let eventLogURL = argumentValue("--events")
    .map(URL.init(fileURLWithPath:))
    ?? CodexEventStore.defaultEventLogURL

let store = CodexStatusStore()
let eventStore = CodexEventStore(eventLogURL: eventLogURL)
let evaluator = StatusEvaluator()
let encoder = PayloadEncoder(maxBytes: 480)

func makePayload() throws -> Data {
    let snapshot = try store.loadSnapshot()
    let now = Date()
    let events = try eventStore.loadEvents(now: now)
    let status = evaluator.evaluate(snapshot: snapshot, now: now, events: events)
    return try encoder.encode(status)
}

if once {
    let payload = try makePayload()
    print(String(decoding: payload, as: UTF8.self))
    exit(EXIT_SUCCESS)
}

let peripheral = BluetoothPeripheral()
peripheral.start()

updateOnce()
Timer.scheduledTimer(withTimeInterval: interval, repeats: true) { _ in
    updateOnce()
}

RunLoop.main.run()

private func updateOnce() {
    do {
        let payload = try makePayload()
        peripheral.update(payload: payload)
        print(String(decoding: payload, as: UTF8.self))
    } catch {
        fputs("Codex Traffic update failed: \(error)\n", stderr)
    }
}

private func argumentValue(_ name: String) -> String? {
    let args = Array(CommandLine.arguments.dropFirst())
    guard let index = args.firstIndex(of: name), args.indices.contains(index + 1) else {
        return nil
    }
    return args[index + 1]
}
