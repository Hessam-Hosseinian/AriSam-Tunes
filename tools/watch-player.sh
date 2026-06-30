#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PLAYER="$ROOT/android/app/src/main/java/com/arisamtunes/feature/player"
RESOURCES="$ROOT/android/app/src/main/res"

"$ROOT/tools/deploy-android.sh"

echo "Watching player UI. Save a player or resource file to deploy again."
while inotifywait -q -r -e close_write,moved_to,create "$PLAYER" "$RESOURCES"; do
  sleep 0.35
  if ! "$ROOT/tools/deploy-android.sh"; then
    echo "Deploy failed. Fix the error and save again."
  fi
done
