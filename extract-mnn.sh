#!/data/data/com.termux/files/usr/bin/bash
# extract-mnn.sh — Extract native libraries from the MNN Chat APK
#
# Usage:
#   ./extract-mnn.sh                                # uses default ~/mnn_chat_0_8_3.apk
#   ./extract-mnn.sh /path/to/mnn_chat_0_8_3.apk    # explicit APK path
#
# Extracts libMNN.so, libmnnllmapp.so, libc++_shared.so
# from the APK's lib/arm64-v8a/ directory and copies them to:
#   app/src/main/jniLibs/arm64-v8a/
#
# Requirements: unzip (pkg install unzip on Termux)
#
# Download the MNN Chat APK from:
#   https://github.com/alibaba/MNN/releases

set -euo pipefail

APK_PATH="${1:-$HOME/mnn_chat_0_8_3.apk}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
DEST_DIR="$SCRIPT_DIR/app/src/main/jniLibs/arm64-v8a"
NEEDED_LIBS=("libMNN.so" "libmnnllmapp.so" "libc++_shared.so")

if ! command -v unzip >/dev/null 2>&1; then
    echo "Error: 'unzip' not found."
    echo "Install with: pkg install unzip"
    exit 1
fi

if [ ! -f "$APK_PATH" ]; then
    echo "Error: APK not found at: $APK_PATH"
    echo ""
    echo "Download the MNN Chat APK from https://github.com/alibaba/MNN/releases"
    echo "Expected filename: mnn_chat_0_8_3.apk (or any APK containing the MNN libraries)"
    echo ""
    echo "Then re-run: $0 /path/to/mnn_chat_0_8_3.apk"
    exit 1
fi

if [[ ! "$APK_PATH" =~ \.apk$ ]]; then
    echo "Warning: file does not have .apk extension: $APK_PATH"
fi

mkdir -p "$DEST_DIR"

WORK_DIR=$(mktemp -d)
trap "rm -rf '$WORK_DIR'" EXIT

echo "Extracting lib/arm64-v8a/ from: $APK_PATH"
unzip -q "$APK_PATH" "lib/arm64-v8a/*" -d "$WORK_DIR"

if [ ! -d "$WORK_DIR/lib/arm64-v8a" ]; then
    echo "Error: APK has no lib/arm64-v8a/. Make sure this is an ARM64 APK."
    exit 1
fi

EXTRACTED=0
MISSING=0
for lib in "${NEEDED_LIBS[@]}"; do
    SRC="$WORK_DIR/lib/arm64-v8a/$lib"
    if [ -f "$SRC" ]; then
        cp "$SRC" "$DEST_DIR/$lib"
        SIZE=$(du -h "$DEST_DIR/$lib" | cut -f1)
        echo "  + $lib ($SIZE)"
        EXTRACTED=$((EXTRACTED + 1))
    else
        echo "  - $lib (not found in APK)"
        MISSING=$((MISSING + 1))
    fi
done

echo ""
echo "Extracted $EXTRACTED / ${#NEEDED_LIBS[@]} libraries to: $DEST_DIR"
ls -lh "$DEST_DIR"

if [ "$EXTRACTED" -eq 0 ]; then
    echo ""
    echo "Error: no required libraries were extracted."
    exit 1
fi

if [ "$MISSING" -gt 0 ]; then
    echo ""
    echo "Warning: $MISSING library/libraries were not found. The app may not work without them."
fi
