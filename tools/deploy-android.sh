#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADB="${ANDROID_HOME:-/home/hessam/Android/Sdk}/platform-tools/adb"
APK="$ROOT/android/app/build/outputs/apk/debug/app-debug.apk"

if ! "$ADB" get-state >/dev/null 2>&1; then
  echo "No Android device is connected."
  exit 1
fi

cd "$ROOT/android"
./gradlew --no-daemon :app:assembleDebug
"$ADB" install -r "$APK"
"$ADB" shell am force-stop com.arisamtunes
"$ADB" shell am start -n com.arisamtunes/.MainActivity

echo "AriSam Tunes is running on the connected phone."
