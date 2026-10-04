#!/bin/bash
. "$APKU_CONFIG"
SRC=$(select_file "$WORKDIR" "*.jar") || exit 1
NAME=$(basename "$SRC" .jar)
DEST="$WORKDIR/$NAME.dex"
$ENJARIFY -o "$DEST" "$SRC" "$@" && echo "Enjarify'd: $DEST"
