#!/bin/bash
# lp-push <archivo>: sube un archivo (p.ej. frida-gadget) al directorio de Lucky Patcher.
. "$APKU_CONFIG"
[ -z "$1" ] && SRC=$(select_file "$APKU_HOME" "*") || SRC="$1"
mkdir -p "$LPDIR"
cp "$SRC" "$LPDIR/" && echo "Pushed $SRC -> $LPDIR"
