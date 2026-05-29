import CoreBluetooth
import Foundation

public final class BluetoothPeripheral: NSObject, CBPeripheralManagerDelegate, @unchecked Sendable {
    public static let deviceName = "Codex Traffic"
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
        peripheralManager = CBPeripheralManager(delegate: self, queue: queue)
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
            peripheral.stopAdvertising()
            return
        }

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
            fputs("Failed to add BLE service: \(error!.localizedDescription)\n", stderr)
            return
        }

        peripheral.startAdvertising([
            CBAdvertisementDataLocalNameKey: Self.deviceName,
            CBAdvertisementDataServiceUUIDsKey: [serviceUUID]
        ])
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
}
