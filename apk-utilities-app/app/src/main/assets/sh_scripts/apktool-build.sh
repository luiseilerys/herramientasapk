#!/bin/bash
. "$APKU_CONFIG"
SRC=$(select_dir "$WORKDIR" "*-sources") || exit 1
NAME=$(basename "$SRC" -sources)
DEST="$WORKDIR/$NAME-patched.apk"
$APKTOOL b -o "$DEST" "$SRC" "$@" && echo "Built: $DEST"
