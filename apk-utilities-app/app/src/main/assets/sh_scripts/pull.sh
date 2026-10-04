#!/bin/bash
# adb-pull (adaptado a Android): lista paquetes instalados y sus rutas de APK,
# y copia los APK al inbox para trabajar con ellos localmente.
. "$APKU_CONFIG"
echo "Installed packages:"
pm list packages 2>/dev/null | sed 's/.*://' | sort
read -rp "Enter package name (or substring): " PKG
MATCHES=$(pm list packages "$PKG" 2>/dev/null | sed 's/package://' | sort)
N=$(echo "$MATCHES" | grep -c . || true)
if [ "$N" != "1" ]; then
    echo "Matches:"; echo "$MATCHES"
    [ "$N" = "0" ] && exit 1
    read -rp "Enter exact package: " PKG
fi
pm path "$PKG" 2>/dev/null | tr -d '\r' | awk -F ':' '{print $2}' | while read -r P; do
    DEST="$WORKDIR/$(basename "$P")"
    if cp "/$P" "$DEST" 2>/dev/null; then echo "Copied: $DEST"; else echo "Failed to copy /$P (storage permission?)"; fi
done
