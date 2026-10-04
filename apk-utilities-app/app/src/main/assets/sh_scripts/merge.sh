#!/bin/bash
# merge: une split APKs en uno solo usando APKEditor.
# Uso: merge app.apk config.en.apk config.xxhdpi.apk ...
. "$APKU_CONFIG"
if [ $# -lt 2 ]; then
    echo "Usage: merge <base.apk> <split1.apk> [split2.apk ...]"
    echo "Tip: use 'pull' to get all split APKs of a package into inbox first."
    exit 1
fi
TMP="$WORKDIR/merge-tmp"
rm -rf "$TMP" >/dev/null 2>&1 || true
mkdir -p "$TMP"
for A in "$@"; do cp "$A" "$TMP/"; done
$APKEDITOR m -i "$TMP" -o "$WORKDIR/project_merged.apk" "$@" && rm -rf "$TMP" && echo "Merged: $WORKDIR/project_merged.apk"
