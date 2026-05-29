#!/usr/bin/env zsh
set -euo pipefail

TOOLCHAIN_DIR="${TOOLCHAIN_DIR:-/Users/macos/Downloads/AndroidToolchain}"
ANDROID_PROJECT_DIR="${ANDROID_PROJECT_DIR:-/Users/macos/Downloads/Projects/loading/android}"

JDK_ARCHIVE="$TOOLCHAIN_DIR/downloads/jdk17.tar.gz"
CMDLINE_ARCHIVE="$TOOLCHAIN_DIR/downloads/commandlinetools-mac.zip"
GRADLE_ARCHIVE="$TOOLCHAIN_DIR/downloads/gradle-9.1.0-bin.zip"

JDK_DIR="$TOOLCHAIN_DIR/jdk"
SDK_DIR="$TOOLCHAIN_DIR/android-sdk"
GRADLE_DIR="$TOOLCHAIN_DIR/gradle"

echo "Toolchain: $TOOLCHAIN_DIR"

for file in "$JDK_ARCHIVE" "$CMDLINE_ARCHIVE" "$GRADLE_ARCHIVE"; do
  if [[ ! -f "$file" ]]; then
    echo "Missing required archive: $file" >&2
    exit 1
  fi
done

mkdir -p "$JDK_DIR" "$SDK_DIR/cmdline-tools" "$GRADLE_DIR"

if [[ ! -x "$JDK_DIR/Contents/Home/bin/java" ]]; then
  echo "Extracting JDK 17..."
  rm -rf "$JDK_DIR"
  mkdir -p "$JDK_DIR"
  tar -xzf "$JDK_ARCHIVE" -C "$JDK_DIR" --strip-components=1
fi

if [[ ! -x "$GRADLE_DIR/gradle-9.1.0/bin/gradle" ]]; then
  echo "Extracting Gradle 9.1.0..."
  rm -rf "$GRADLE_DIR/gradle-9.1.0"
  unzip -q "$GRADLE_ARCHIVE" -d "$GRADLE_DIR"
fi

if [[ ! -x "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" ]]; then
  echo "Extracting Android command-line tools..."
  rm -rf "$SDK_DIR/cmdline-tools/latest" "$SDK_DIR/cmdline-tools/cmdline-tools"
  unzip -q "$CMDLINE_ARCHIVE" -d "$SDK_DIR/cmdline-tools"
  mv "$SDK_DIR/cmdline-tools/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"
fi

export JAVA_HOME="$JDK_DIR/Contents/Home"
export ANDROID_HOME="$SDK_DIR"
export ANDROID_SDK_ROOT="$SDK_DIR"
export GRADLE_USER_HOME="$TOOLCHAIN_DIR/gradle-home"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$GRADLE_DIR/gradle-9.1.0/bin:$PATH"

echo "Accepting Android SDK licenses..."
yes | sdkmanager --licenses >/dev/null

echo "Installing Android SDK packages..."
sdkmanager \
  "platform-tools" \
  "platforms;android-36" \
  "build-tools;36.0.0" \
  "cmdline-tools;latest"

echo "Writing android/local.properties..."
mkdir -p "$ANDROID_PROJECT_DIR"
cat > "$ANDROID_PROJECT_DIR/local.properties" <<EOF
sdk.dir=$SDK_DIR
EOF

echo "Generating Gradle wrapper..."
cd "$ANDROID_PROJECT_DIR"
gradle wrapper --gradle-version 9.1.0 --distribution-type bin

echo "Verifying toolchain..."
java -version
adb version
./gradlew --version

echo "Android toolchain installed."
echo "Add this to ~/.zshrc if you want global shell access:"
echo "export JAVA_HOME=\"$JAVA_HOME\""
echo "export ANDROID_HOME=\"$ANDROID_HOME\""
echo "export ANDROID_SDK_ROOT=\"$ANDROID_SDK_ROOT\""
echo "export GRADLE_USER_HOME=\"$GRADLE_USER_HOME\""
echo "export PATH=\"\$JAVA_HOME/bin:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:$GRADLE_DIR/gradle-9.1.0/bin:\$PATH\""
