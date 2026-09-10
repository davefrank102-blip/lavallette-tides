#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/gradle/wrapper/gradle-wrapper.jar"
if [[ -f "$DEST" ]]; then
  echo "Already present: $DEST"
  exit 0
fi
mkdir -p "$(dirname "$DEST")"
URL="https://github.com/gradle/gradle/raw/v8.11.1/gradle/wrapper/gradle-wrapper.jar"
echo "Downloading gradle-wrapper.jar..."
curl -fsSL "$URL" -o "$DEST"
echo "Downloaded $DEST"
