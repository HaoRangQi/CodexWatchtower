// swift-tools-version: 6.0

import PackageDescription

let package = Package(
    name: "CodexTrafficCompanion",
    platforms: [
        .macOS(.v13)
    ],
    products: [
        .executable(name: "codex-traffic", targets: ["CodexTraffic"]),
        .executable(name: "codex-traffic-selftest", targets: ["CodexTrafficSelfTest"]),
        .library(name: "CodexTrafficCore", targets: ["CodexTrafficCore"])
    ],
    targets: [
        .target(
            name: "CodexTrafficCore",
            linkerSettings: [
                .linkedLibrary("sqlite3"),
                .linkedFramework("CoreBluetooth")
            ]
        ),
        .executableTarget(
            name: "CodexTraffic",
            dependencies: ["CodexTrafficCore"],
            exclude: ["Info.plist"],
            linkerSettings: [
                .unsafeFlags([
                    "-Xlinker", "-sectcreate",
                    "-Xlinker", "__TEXT",
                    "-Xlinker", "__info_plist",
                    "-Xlinker", "Sources/CodexTraffic/Info.plist"
                ])
            ]
        ),
        .executableTarget(
            name: "CodexTrafficSelfTest",
            dependencies: ["CodexTrafficCore"]
        )
    ]
)
