#!/usr/bin/env zsh
set -euo pipefail

TOOLCHAIN_DIR="${TOOLCHAIN_DIR:-/Users/macos/Downloads/AndroidToolchain}"
ANDROID_PROJECT_DIR="${ANDROID_PROJECT_DIR:-/Users/macos/Downloads/Projects/loading/android}"

echo "This will remove:"
echo "  $TOOLCHAIN_DIR"
echo "  $ANDROID_PROJECT_DIR/local.properties"
echo
echo "Press Ctrl-C to cancel, or Enter to continue."
read -r

rm -rf "$TOOLCHAIN_DIR"
rm -f "$ANDROID_PROJECT_DIR/local.properties"

echo "Removed Android toolchain."

