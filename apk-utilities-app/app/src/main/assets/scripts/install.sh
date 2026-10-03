#!/bin/bash
# install: dispara el Intent de instalación del APK elegido (reemplaza a adb-install).
. "$APKU_CONFIG"
SRC=$(select_file "$WORKDIR" "*.apk") || exit 1
am start -a android.intent.action.VIEW -d "file://$SRC" -t application/vnd.android.package-archive \
    --grant-read-uri-permission >/dev/null 2>&1 \
  && echo "Install dialog launched for: $SRC" \
  || am start -a android.intent.action.INSTALL_PACKAGE -d "file://$SRC"
