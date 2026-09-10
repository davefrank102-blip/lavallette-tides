#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/gradle/wrapper/gradle-wrapper.jar"
B64="$ROOT/gradle/wrapper/gradle-wrapper.jar.b64"
if [[ -f "$DEST" ]]; then
  echo "Already present: $DEST"
  exit 0
fi
mkdir -p "$(dirname "$DEST")"
if [[ -f "$B64" ]]; then
  base64 -d < "$B64" > "$DEST"
  echo "Decoded $DEST from gradle-wrapper.jar.b64"
  exit 0
fi
URL="https://github.com/gradle/gradle/raw/v8.11.1/gradle/wrapper/gradle-wrapper.jar"
curl -fsSL "$URL" -o "$DEST"
echo "Downloaded $DEST"
