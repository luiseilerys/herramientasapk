#!/bin/bash
. "$APKU_CONFIG"
rm -rf "${WORKDIR:?}/"*
echo "Cleaned $WORKDIR"
