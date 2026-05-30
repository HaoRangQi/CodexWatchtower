import AppKit
import CodexTrafficCore
import Foundation

let arguments = CommandLine.arguments.dropFirst()
let once = arguments.contains("--once")
let interval = argumentValue("--interval").flatMap(Double.init) ?? 2.0
let httpDisabled = arguments.contains("--no-http")
let bleDisabled = arguments.contains("--no-ble")
let httpPort = argumentValue("--http-port").flatMap(UInt16.init) ?? HTTPStatusServer.defaultPort
let eventLogURL = argumentValue("--events")
    .map(URL.init(fileURLWithPath:))
    ?? CodexEventStore.defaultEventLogURL

let store = CodexStatusStore()
let eventStore = CodexEventStore(eventLogURL: eventLogURL)
let snapshotFeedSynthesizer = SnapshotFeedSynthesizer()
let eventMapper = CodexEventMapper()
let evaluator = StatusEvaluator()
let encoder = PayloadEncoder(maxBytes: 480)

func makePayload() throws -> Data {
    let snapshot = try store.loadSnapshot()
    let now = Date()
    let events = try eventStore.loadEvents(now: now)
    let synthesizedEvents = snapshotFeedSynthesizer.synthesize(snapshot: snapshot, now: now)
    let status = evaluator.evaluate(snapshot: snapshot, now: now, events: events)
    let feedStatus = TrafficStatus(
        version: status.version,
        timestamp: status.timestamp,
        overall: status.overall,
        projects: status.projects,
        moreCount: status.moreCount,
        feedItems: mergeFeedItems(primary: events, secondary: synthesizedEvents, now: now),
        moreFeedCount: 0
    )
    return try encoder.encode(feedStatus)
}

if once {
    let payload = try makePayload()
    print(String(decoding: payload, as: UTF8.self))
    exit(EXIT_SUCCESS)
}

let statusWindow = prepareMacApplication()

let peripheral = BluetoothPeripheral()
let httpServer = HTTPStatusServer(port: httpPort, payloadProvider: makePayload)
DispatchQueue.main.async {
    if !bleDisabled {
        peripheral.start()
    } else {
        fputs("Codex Watchtower BLE: disabled via --no-ble\n", stderr)
    }
    if !httpDisabled {
        do {
            try httpServer.start()
        } catch {
            fputs("Codex Watchtower HTTP failed to start: \(error)\n", stderr)
        }
    }
    updateOnce(bleEnabled: !bleDisabled)
    Timer.scheduledTimer(withTimeInterval: interval, repeats: true) { _ in
        updateOnce(bleEnabled: !bleDisabled)
    }
}

private func updateOnce(bleEnabled: Bool) {
    do {
        let payload = try makePayload()
        if bleEnabled {
            peripheral.update(payload: payload)
        }
        print(String(decoding: payload, as: UTF8.self))
    } catch {
        fputs("Codex Watchtower update failed: \(error)\n", stderr)
    }
}

private func argumentValue(_ name: String) -> String? {
    let args = Array(CommandLine.arguments.dropFirst())
    guard let index = args.firstIndex(of: name), args.indices.contains(index + 1) else {
        return nil
    }
    return args[index + 1]
}

@MainActor
private func prepareMacApplication() -> NSWindow {
    let application = NSApplication.shared
    application.setActivationPolicy(.regular)

    let window = NSWindow(
        contentRect: NSRect(x: 0, y: 0, width: 320, height: 128),
        styleMask: [.titled, .closable, .miniaturizable],
        backing: .buffered,
        defer: false
    )
    window.title = "Codex 守望台 / Codex Watchtower"
    window.center()

    let label = NSTextField(labelWithString: "Codex 守望台正在广播本机 Codex 状态")
    label.alignment = .center
    label.font = .systemFont(ofSize: 14, weight: .medium)
    label.frame = NSRect(x: 20, y: 56, width: 280, height: 24)

    let detail = NSTextField(labelWithString: "如果系统询问蓝牙权限，请选择允许。")
    detail.alignment = .center
    detail.textColor = .secondaryLabelColor
    detail.font = .systemFont(ofSize: 12)
    detail.frame = NSRect(x: 20, y: 32, width: 280, height: 20)

    let contentView = NSView(frame: window.contentView?.bounds ?? .zero)
    contentView.addSubview(label)
    contentView.addSubview(detail)

    let httpDetail = NSTextField(labelWithString: "HTTP 备用通道：http://<Mac IP>:\(httpPort)/status")
    httpDetail.alignment = .center
    httpDetail.textColor = .tertiaryLabelColor
    httpDetail.font = .systemFont(ofSize: 11)
    httpDetail.frame = NSRect(x: 20, y: 12, width: 280, height: 18)
    contentView.addSubview(httpDetail)

    window.contentView = contentView
    window.makeKeyAndOrderFront(nil)

    application.activate(ignoringOtherApps: true)
    return window
}

@MainActor
private func runMacApplication() -> Never {
    NSApplication.shared.run()
    exit(EXIT_SUCCESS)
}

private func mergeFeedItems(
    primary: [CodexRealtimeEvent],
    secondary: [CodexRealtimeEvent],
    now: Date
) -> [PetFeedItem] {
    var seen = Set<String>()
    var result: [PetFeedItem] = []
    for event in (primary + secondary).sorted(by: { $0.timestamp > $1.timestamp }) {
        let key = "\(event.cwd)-\(event.kind.rawValue)-\(event.title)"
        guard !seen.contains(key) else {
            continue
        }
        seen.insert(key)
        result.append(feedItem(for: event, now: now))
    }
    return result
}

private func feedItem(for event: CodexRealtimeEvent, now: Date) -> PetFeedItem {
    eventMapper.feedItem(for: event, now: now)
}

runMacApplication()
