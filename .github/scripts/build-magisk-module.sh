#!/usr/bin/env bash
#
# Package a built APK as a Magisk module that replaces the system package
# installer (system/priv-app/PackageInstaller).
#
# Usage: build-magisk-module.sh <apk> <version-name> <version-code> <flavor-label> <output.zip>
#
# The template lives in .github/magisk and contains @VERSION_NAME@,
# @VERSION_CODE@ and @FLAVOR_LABEL@ placeholders.

set -euo pipefail

if [ "$#" -ne 5 ]; then
  echo "Usage: $0 <apk> <version-name> <version-code> <flavor-label> <output.zip>" >&2
  exit 2
fi

APK=$1
VERSION_NAME=$2
VERSION_CODE=$3
FLAVOR_LABEL=$4
OUTPUT=$5

TEMPLATE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../magisk" && pwd)"

[ -f "$APK" ] || { echo "error: apk not found: $APK" >&2; exit 1; }
[ -f "$TEMPLATE_DIR/module.prop" ] || { echo "error: template not found: $TEMPLATE_DIR" >&2; exit 1; }

WORK_DIR=$(mktemp -d)
trap 'rm -rf "$WORK_DIR" 2>/dev/null || true' EXIT

# Copy only what belongs in the flashed module; README.md stays repo-side.
for item in module.prop customize.sh service.sh uninstall.sh action.sh launcher.png META-INF; do
  cp -a "$TEMPLATE_DIR/$item" "$WORK_DIR/"
done

mkdir -p "$WORK_DIR/system/priv-app/PackageInstaller"
cp "$APK" "$WORK_DIR/system/priv-app/PackageInstaller/PackageInstaller.apk"

sed -i \
  -e "s|@VERSION_NAME@|${VERSION_NAME}|g" \
  -e "s|@VERSION_CODE@|${VERSION_CODE}|g" \
  -e "s|@FLAVOR_LABEL@|${FLAVOR_LABEL}|g" \
  "$WORK_DIR/module.prop"

if grep -q '@[A-Z_]*@' "$WORK_DIR/module.prop"; then
  echo "error: unresolved placeholder in module.prop" >&2
  cat "$WORK_DIR/module.prop" >&2
  exit 1
fi

# Magisk executes these with the device shell; CRLF would break them.
find "$WORK_DIR" -type f \( -name '*.sh' -o -name 'update-binary' \) -exec sed -i 's/\r$//' {} +
chmod 0755 \
  "$WORK_DIR/customize.sh" \
  "$WORK_DIR/service.sh" \
  "$WORK_DIR/uninstall.sh" \
  "$WORK_DIR/action.sh" \
  "$WORK_DIR/META-INF/com/google/android/update-binary"

OUT_DIR=$(cd "$(dirname "$OUTPUT")" && pwd)
OUT_FILE="$OUT_DIR/$(basename "$OUTPUT")"
mkdir -p "$OUT_DIR"
rm -f -- "$OUT_FILE" 2>/dev/null || true
# Everything must sit at the zip root, which is what Magisk expects.
if command -v zip >/dev/null 2>&1; then
  ( cd "$WORK_DIR" && zip -r9 -q "$OUT_FILE" . )
else
  # Fallback for environments without the zip binary (the CI runner has it).
  PYTHON=$(command -v python3 || command -v python)
  ( cd "$WORK_DIR" && "$PYTHON" - "$OUT_FILE" <<'PY'
import os, sys, zipfile
out = os.path.abspath(sys.argv[1])
with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
    for root, _dirs, files in os.walk("."):
        for name in sorted(files):
            path = os.path.join(root, name)
            z.write(path, os.path.relpath(path, "."))
PY
  )
fi

for required in module.prop customize.sh system/priv-app/PackageInstaller/PackageInstaller.apk META-INF/com/google/android/update-binary; do
  if ! unzip -l "$OUT_FILE" "$required" >/dev/null 2>&1; then
    echo "error: $required missing from module zip" >&2
    exit 1
  fi
done

echo "Built $OUT_FILE"
echo "  version: $VERSION_NAME ($VERSION_CODE)"
unzip -l "$OUT_FILE"
