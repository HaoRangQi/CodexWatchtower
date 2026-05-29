import Foundation

enum SHA1 {
    static func hexDigest(_ input: String) -> String {
        let bytes = Array(input.utf8)
        let messageBitLength = UInt64(bytes.count * 8)
        var message = bytes
        message.append(0x80)
        while (message.count % 64) != 56 {
            message.append(0)
        }
        message += messageBitLength.bigEndianBytes

        var h0: UInt32 = 0x67452301
        var h1: UInt32 = 0xEFCDAB89
        var h2: UInt32 = 0x98BADCFE
        var h3: UInt32 = 0x10325476
        var h4: UInt32 = 0xC3D2E1F0

        for chunkStart in stride(from: 0, to: message.count, by: 64) {
            var words = [UInt32](repeating: 0, count: 80)
            for index in 0..<16 {
                let base = chunkStart + index * 4
                words[index] =
                    UInt32(message[base]) << 24 |
                    UInt32(message[base + 1]) << 16 |
                    UInt32(message[base + 2]) << 8 |
                    UInt32(message[base + 3])
            }
            for index in 16..<80 {
                words[index] = rotateLeft(words[index - 3] ^ words[index - 8] ^ words[index - 14] ^ words[index - 16], by: 1)
            }

            var a = h0
            var b = h1
            var c = h2
            var d = h3
            var e = h4

            for index in 0..<80 {
                let f: UInt32
                let k: UInt32
                switch index {
                case 0...19:
                    f = (b & c) | ((~b) & d)
                    k = 0x5A827999
                case 20...39:
                    f = b ^ c ^ d
                    k = 0x6ED9EBA1
                case 40...59:
                    f = (b & c) | (b & d) | (c & d)
                    k = 0x8F1BBCDC
                default:
                    f = b ^ c ^ d
                    k = 0xCA62C1D6
                }

                let temp = rotateLeft(a, by: 5)
                    &+ f
                    &+ e
                    &+ k
                    &+ words[index]
                e = d
                d = c
                c = rotateLeft(b, by: 30)
                b = a
                a = temp
            }

            h0 = h0 &+ a
            h1 = h1 &+ b
            h2 = h2 &+ c
            h3 = h3 &+ d
            h4 = h4 &+ e
        }

        return [h0, h1, h2, h3, h4]
            .map { String(format: "%08x", $0) }
            .joined()
    }

    private static func rotateLeft(_ value: UInt32, by amount: UInt32) -> UInt32 {
        (value << amount) | (value >> (32 - amount))
    }
}

private extension UInt64 {
    var bigEndianBytes: [UInt8] {
        (0..<8).reversed().map { UInt8((self >> UInt64($0 * 8)) & 0xff) }
    }
}

