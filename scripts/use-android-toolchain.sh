#!/usr/bin/env zsh

export JAVA_HOME="/Users/macos/Downloads/AndroidToolchain/jdk/Contents/Home"
export ANDROID_HOME="/Users/macos/Downloads/AndroidToolchain/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export GRADLE_USER_HOME="/Users/macos/Downloads/AndroidToolchain/gradle-home"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:/Users/macos/Downloads/AndroidToolchain/gradle/gradle-9.1.0/bin:$PATH"

echo "JAVA_HOME=$JAVA_HOME"
echo "ANDROID_HOME=$ANDROID_HOME"
echo "GRADLE_USER_HOME=$GRADLE_USER_HOME"
