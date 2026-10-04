#!/bin/bash
. "$APKU_CONFIG"
SRC=$(select_file "$WORKDIR" "*.dex") || exit 1
NAME=$(basename "$SRC" .dex)
DEST="$WORKDIR/$NAME-smali"
rm -rf "$DEST" >/dev/null 2>&1 || true
$BAKSMALI d --use-locals -o "$DEST" "$SRC" "$@" && echo "Baksmali'd to: $DEST"
