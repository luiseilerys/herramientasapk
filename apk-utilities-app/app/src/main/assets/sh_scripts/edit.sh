#!/bin/bash
# editor de texto en terminal (busybox vi embebido). Uso: edit <archivo>
. "$APKU_CONFIG"
[ -z "$1" ] && { echo "Usage: edit <file>"; exit 1; }
busybox vi "$@"
