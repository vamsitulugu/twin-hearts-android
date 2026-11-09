#!/usr/bin/env bash
# Same as sync-cinema.ps1 for macOS/Linux:  tools/sync-cinema.sh [path/to/us-app] [--skip-build]
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WEB="${1:-$ROOT/../us-app}"; SKIP="${2:-}"
[ -d "$WEB" ] || { echo "Web repo not found: $WEB"; exit 1; }
if [ "$SKIP" != "--skip-build" ]; then (cd "$WEB" && npm run build:cinema); fi
[ -f "$WEB/public/cinema/index.html" ] || { echo "No built cinema in $WEB/public/cinema"; exit 1; }
mkdir -p "$ROOT/app/src/main/assets/cinema"
rsync -a --delete --exclude ads.txt "$WEB/public/cinema/" "$ROOT/app/src/main/assets/cinema/"
echo "Done: cinema copied to app/src/main/assets/cinema. Rebuild the app in Android Studio."
