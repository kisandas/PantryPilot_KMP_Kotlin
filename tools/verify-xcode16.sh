#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
# Set DEVELOPER_DIR before invoking this script if Xcode 16.0 is not selected globally.
version=$(xcodebuild -version | head -n 1)
case "$version" in
  'Xcode 16.0') ;;
  *) printf '%s\n' "Expected Xcode 16.0, found: $version" "Select Xcode 16.0 with DEVELOPER_DIR or xcode-select before testing."; exit 1 ;;
esac
case "$(uname -m)" in
  arm64) target=IosSimulatorArm64 ;;
  x86_64) target=IosX64 ;;
  *) printf '%s\n' 'Unsupported Mac architecture'; exit 1 ;;
esac
printf '%s\n' "Checking Kotlin framework with $version ($target)"
sh ./gradlew ":shared:linkDebugFramework$target" --no-configuration-cache
printf '%s\n' 'Framework build passed. Open iosApp/iosApp.xcodeproj, choose your team and an iOS 18.0 simulator, then Run.'