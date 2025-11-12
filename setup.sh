#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GRADLE_JAR="$PROJECT_DIR/gradle/wrapper/gradle-wrapper.jar"
GRADLE_DIST_URL="https://services.gradle.org/distributions/gradle-8.4-bin.zip"
GRADLE_DIST_SHA256="3e1af3ae886920c3ac87f7a91f816c0c7c436f276a6eefdb3da152100fef72ae"
SDK_ROOT="$PROJECT_DIR/.android-sdk"
CMDLINE_TOOLS_ZIP_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
CMDLINE_TOOLS_SHA256="2d2d50857e4eb553af5a6dc3ad507a17adf43d115264b1afc116f95c92e5e258"
CMDLINE_TOOLS_DIR="$SDK_ROOT/cmdline-tools"

mkdir -p "$PROJECT_DIR/gradle/wrapper"

GRADLE_SHARED_JAR="$PROJECT_DIR/gradle/wrapper/gradle-wrapper-shared.jar"
GRADLE_CLI_JAR="$PROJECT_DIR/gradle/wrapper/gradle-cli.jar"

if [ ! -f "$GRADLE_JAR" ] || [ ! -f "$GRADLE_SHARED_JAR" ] || [ ! -f "$GRADLE_CLI_JAR" ]; then
  echo "Downloading Gradle wrapper jars..."
  tmp_dir="$(mktemp -d)"
  curl -L "$GRADLE_DIST_URL" -o "$tmp_dir/gradle.zip"
  echo "$GRADLE_DIST_SHA256  $tmp_dir/gradle.zip" | sha256sum --check --status
  unzip -q "$tmp_dir/gradle.zip" "gradle-*/lib/plugins/gradle-wrapper-*.jar" "gradle-*/lib/gradle-wrapper-shared-*.jar" "gradle-*/lib/gradle-cli-*.jar" -d "$tmp_dir"
  cp "$tmp_dir"/gradle-*/lib/plugins/gradle-wrapper-*.jar "$GRADLE_JAR"
  cp "$tmp_dir"/gradle-*/lib/gradle-wrapper-shared-*.jar "$GRADLE_SHARED_JAR"
  cp "$tmp_dir"/gradle-*/lib/gradle-cli-*.jar "$GRADLE_CLI_JAR"
  rm -rf "$tmp_dir"
fi

chmod +x "$PROJECT_DIR/gradlew"

mkdir -p "$SDK_ROOT"

if [ ! -d "$CMDLINE_TOOLS_DIR/latest" ]; then
  echo "Installing Android command line tools..."
  tmp_dir_tools="$(mktemp -d)"
  curl -L "$CMDLINE_TOOLS_ZIP_URL" -o "$tmp_dir_tools/tools.zip"
  echo "$CMDLINE_TOOLS_SHA256  $tmp_dir_tools/tools.zip" | sha256sum --check --status
  mkdir -p "$CMDLINE_TOOLS_DIR"
  unzip -q "$tmp_dir_tools/tools.zip" -d "$tmp_dir_tools"
  rm -rf "$CMDLINE_TOOLS_DIR/latest"
  mv "$tmp_dir_tools/cmdline-tools" "$CMDLINE_TOOLS_DIR/latest"
  rm -rf "$tmp_dir_tools"
fi

export ANDROID_SDK_ROOT="$SDK_ROOT"
export ANDROID_HOME="$SDK_ROOT"

SDKMANAGER="$CMDLINE_TOOLS_DIR/latest/bin/sdkmanager"
if [ ! -x "$SDKMANAGER" ]; then
  echo "sdkmanager not found at $SDKMANAGER" >&2
  exit 1
fi

set +o pipefail
yes | "$SDKMANAGER" --sdk_root="$SDK_ROOT" --licenses >/dev/null || true
set -o pipefail
"$SDKMANAGER" --sdk_root="$SDK_ROOT" "platform-tools" "platforms;android-34" "build-tools;34.0.0"

export JAVA_OPTS="${JAVA_OPTS:-}"

"$PROJECT_DIR/gradlew" --no-daemon assembleDebug
