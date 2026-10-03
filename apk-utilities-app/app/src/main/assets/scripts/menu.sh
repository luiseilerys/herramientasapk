menu() {
    cat <<'EOF'
=== apk-utilities on Android ===
INBOX   = ~/inbox   (importa APK/dex/jar con el botón "Import file")
WORKDIR = ~/project (salidas de todas las herramientas)

Flujo principal:  pull/aapt-dump/decode → *editar* → build/smali → sign → install
Otros:            baksmali/smali, enjarify/dexify, merge, lp-pull/lp-push, clean

Herramientas:
  aapt-dump        vuelca manifest+recursos de un APK a texto
  apktool-decode   decodifica APK (recursos + smali)
  apktool-build    recompila desde *-sources
  sign             re-firma cualquier APK (uber-apk-signer)
  baksmali         dex -> smali          smali: smali -> dex
  enjarify         dex/jar -> jar Java    dexify: jar -> dex (d8)
  merge            split APKs -> APK único (APKEditor)
  pull             copia APKs de apps instaladas al inbox
  install          lanza el diálogo de instalación del APK elegido
  lp-pull / lp-push  sincronizar con Lucky Patcher
  edit <file>      editor vi en terminal
  clean            limpia project/
  ls cd cat grep ...  shell bash completo (busybox + java + python3)
EOF
}
