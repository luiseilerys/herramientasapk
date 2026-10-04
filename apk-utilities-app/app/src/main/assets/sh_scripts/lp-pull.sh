#!/bin/bash
# lp-pull: copia los APKs parcheados por Lucky Patcher al inbox.
. "$APKU_CONFIG"
if [ -d "$LPDIR/Modified" ]; then
    mkdir -p "$WORKDIR/LP-Modified"
    cp -r "$LPDIR/Modified/"* "$WORKDIR/LP-Modified/" && echo "Copied to $WORKDIR/LP-Modified"
else
    echo "Lucky Patcher dir not found: $LPDIR (is LP installed?)"
fi
