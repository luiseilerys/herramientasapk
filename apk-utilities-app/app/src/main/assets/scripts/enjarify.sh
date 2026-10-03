#!/bin/bash
. "$APKU_CONFIG"
SRC=$(select_file "$APKU_INBOX" "*.jar") || exit 1
NAME=$(basename "$SRC" .jar)
DEST="$WORKDIR/$NAME.dex"
$ENJARIFY -o "$DEST" "$SRC" "$@" && echo "Enjarify'd: $DEST"
