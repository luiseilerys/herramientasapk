#!/bin/bash
. "$APKU_CONFIG"
SRC=$(select_file "$APKU_INBOX" "*.apk") || exit 1
NAME=$(basename "$SRC" .apk)
DEST="$WORKDIR/$NAME-aapt.txt"
echo "Dumping $SRC ..."
"$AAPT" l -a "$SRC" > "$DEST"
echo "Saved to: $DEST"
