#!/bin/bash
# dexify: recompila un .jar (Java modificado) de vuelta a .dex usando d8 embebido.
. "$APKU_CONFIG"
SRC=$(select_file "$APKU_INBOX" "*.jar") || exit 1
NAME=$(basename "$SRC" .jar)
DEST="$WORKDIR/$NAME.dex"
d8 --min-api 26 --output "$WORKDIR" "$SRC" "$@" && mv -f "$WORKDIR/classes.dex" "$DEST" && echo "Dexified: $DEST"
