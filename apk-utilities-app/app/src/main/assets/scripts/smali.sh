#!/bin/bash
. "$APKU_CONFIG"
SRC=$(select_dir "$WORKDIR" "*-smali") || exit 1
NAME=$(basename "$SRC" -smali)
DEST="$WORKDIR/$NAME-patched.dex"
$SMALI a -o "$DEST" "$SRC" "$@" && echo "Assembled: $DEST"
