#!/bin/bash
. "$APKU_CONFIG"
SRC=$(select_file "$WORKDIR" "*.apk") || exit 1
$APKSIGNER -a "$SRC" "$@"
