#!/bin/bash
# lp-pull: copia los APKs parcheados por Lucky Patcher al inbox.
. "$APKU_CONFIG"
if [ -d "$LPDIR/Modified" ]; then
    mkdir -p "$APKU_INBOX/LP-Modified"
    cp -r "$LPDIR/Modified/"* "$APKU_INBOX/LP-Modified/" && echo "Copied to $APKU_INBOX/LP-Modified"
else
    echo "Lucky Patcher dir not found: $LPDIR (is LP installed?)"
fi
