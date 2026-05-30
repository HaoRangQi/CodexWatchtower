import CoreBluetooth
import Foundation

public final class BluetoothPeripheral: NSObject, CBPeripheralManagerDelegate, @unchecked Sendable {
    public static let deviceName = "Codex Watchtower"
    public static let serviceUUIDString = "4F4C0001-6C6F-6164-696E-672D636F6465"
    public static let statusCharacteristicUUIDString = "4F4C0002-6C6F-6164-696E-672D636F6465"

    private var serviceUUID: CBUUID {
        CBUUID(string: Self.serviceUUIDString)
    }

    private var statusCharacteristicUUID: CBUUID {
        CBUUID(string: Self.statusCharacteristicUUIDString)
    }

    private let queue = DispatchQueue(label: "codex-traffic.bluetooth")
    private var peripheralManager: CBPeripheralManager?
    private var statusCharacteristic: CBMutableCharacteristic?
    private var latestPayload = Data()

    public override init() {
        super.init()
    }

    public func start() {
        diagnosticLog("starting peripheral manager")
        if #available(macOS 11.0, *) {
            diagnosticLog("authorization \(CBManager.authorization.diagnosticName)")
        }
        peripheralManager = CBPeripheralManager(
            delegate: self,
            queue: nil,
            options: [CBPeripheralManagerOptionShowPowerAlertKey: true]
        )
    }

    public func update(payload: Data) {
        queue.async {
            self.latestPayload = payload
            guard let peripheralManager = self.peripheralManager,
                  let characteristic = self.statusCharacteristic else {
                return
            }
            peripheralManager.updateValue(payload, for: characteristic, onSubscribedCentrals: nil)
        }
    }

    public func peripheralManagerDidUpdateState(_ peripheral: CBPeripheralManager) {
        guard peripheral.state == .poweredOn else {
            diagnosticLog("peripheral state \(peripheral.state.diagnosticName), advertising stopped")
            peripheral.stopAdvertising()
            return
        }

        diagnosticLog("peripheral powered on, adding service")
        let characteristic = CBMutableCharacteristic(
            type: statusCharacteristicUUID,
            properties: [.read, .notify],
            value: nil,
            permissions: [.readable]
        )
        let service = CBMutableService(type: serviceUUID, primary: true)
        service.characteristics = [characteristic]
        statusCharacteristic = characteristic

        peripheral.removeAllServices()
        peripheral.add(service)
    }

    public func peripheralManager(
        _ peripheral: CBPeripheralManager,
        didAdd service: CBService,
        error: Error?
    ) {
        guard error == nil else {
            diagnosticLog("failed to add BLE service: \(error!.localizedDescription)")
            return
        }

        diagnosticLog("service added, starting advertising as \(Self.deviceName)")
        peripheral.startAdvertising([
            CBAdvertisementDataLocalNameKey: Self.deviceName,
            CBAdvertisementDataServiceUUIDsKey: [serviceUUID]
        ])
    }

    public func peripheralManagerDidStartAdvertising(
        _ peripheral: CBPeripheralManager,
        error: Error?
    ) {
        if let error {
            diagnosticLog("advertising failed: \(error.localizedDescription)")
        } else {
            diagnosticLog("advertising started")
        }
    }

    public func peripheralManager(
        _ peripheral: CBPeripheralManager,
        didReceiveRead request: CBATTRequest
    ) {
        guard request.characteristic.uuid == statusCharacteristicUUID else {
            peripheral.respond(to: request, withResult: .attributeNotFound)
            return
        }

        if request.offset > latestPayload.count {
            peripheral.respond(to: request, withResult: .invalidOffset)
            return
        }

        request.value = latestPayload.subdata(in: request.offset..<latestPayload.count)
        peripheral.respond(to: request, withResult: .success)
    }

    private func diagnosticLog(_ message: String) {
        let line = "\(Date()) BLE \(message)\n"
        fputs("Codex Watchtower BLE: \(message)\n", stderr)
        let url = FileManager.default.homeDirectoryForCurrentUser
            .appendingPathComponent(".codex-traffic/companion.log")
        do {
            try FileManager.default.createDirectory(
                at: url.deletingLastPathComponent(),
                withIntermediateDirectories: true
            )
            let data = Data(line.utf8)
            if FileManager.default.fileExists(atPath: url.path) {
                let handle = try FileHandle(forWritingTo: url)
                defer { try? handle.close() }
                try handle.seekToEnd()
                try handle.write(contentsOf: data)
            } else {
                try data.write(to: url)
            }
        } catch {
            fputs("Codex Watchtower BLE diagnostic log failed: \(error)\n", stderr)
        }
    }
}

@available(macOS 11.0, *)
private extension CBManagerAuthorization {
    var diagnosticName: String {
        switch self {
        case .notDetermined:
            return "notDetermined"
        case .restricted:
            return "restricted"
        case .denied:
            return "denied"
        case .allowedAlways:
            return "allowedAlways"
        @unknown default:
            return "unknown(\(rawValue))"
        }
    }
}

private extension CBManagerState {
    var diagnosticName: String {
        switch self {
        case .unknown:
            return "unknown"
        case .resetting:
            return "resetting"
        case .unsupported:
            return "unsupported"
        case .unauthorized:
            return "unauthorized"
        case .poweredOff:
            return "poweredOff"
        case .poweredOn:
            return "poweredOn"
        @unknown default:
            return "unknown(\(rawValue))"
        }
    }
}
