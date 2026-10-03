#!/bin/bash
. "$APKU_CONFIG"
SRC=$(select_file "$APKU_INBOX" "*.apk") || exit 1
NAME=$(basename "$SRC" .apk)
DEST="$WORKDIR/$NAME-sources"
rm -rf "$DEST" >/dev/null 2>&1 || true
$APKTOOL d -f -o "$DEST" "$SRC" "$@" && echo "Decoded to: $DEST"
