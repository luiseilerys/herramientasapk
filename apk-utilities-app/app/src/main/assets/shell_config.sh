# apk-utilities shell — configuración compartida (versión Android)
# Equivalente a .config.sh del repo original, adaptado para ejecutarse
# 100% dentro del dispositivo Android (sin adb).

WORKDIR="$APKU_HOME/project"
LPDIR="/sdcard/Android/data/___.lp/files/LuckyPatcher"

# Java embebido (JBR) provisto por la app
export JAVA_HOME="$APKU_JAVA"
export PATH="$JAVA_HOME/bin:$APKU_BIN:$PATH"
APKTOOL="java -jar $APKU_ASSETS/apktool_2.12.0.jar"
BAKSMALI="java -jar $APKU_ASSETS/baksmali-2.5.2.jar"
SMALI="java -jar $APKU_ASSETS/smali-2.5.2.jar"
APKSIGNER="java -jar $APKU_ASSETS/uber-apk-signer-1.3.0.jar"
APKEDITOR="java -jar $APKU_ASSETS/APKEditor-1.4.5.jar"
ENJARIFY="python3 $APKU_ASSETS/enjarify/main.py"
AAPT="$APKU_BIN/aapt"

select_file() {
    local dir="$1"; shift
    local glob="$*"
    local files=()
    while IFS= read -r f; do files+=("$f"); done < <(find "$dir" -maxdepth 1 -type f $( [[ -n "$glob" ]] && echo "-name '$glob'" ) | sort)
    if [ ${#files[@]} -eq 0 ]; then
        echo "No files matching '$glob' in $dir" >&2
        return 1
    fi
    local choice
    select choice in "${files[@]}" "CANCEL"; do
        [ "$choice" != "CANCEL" ] && [ -n "$choice" ] && break
        echo "Cancelled." >&2; return 1
    done
    echo "$choice"
}

select_dir() {
    local dir="$1"; shift
    local dirs=()
    while IFS= read -r d; do dirs+=("$d"); done < <(find "$dir" -mindepth 1 -maxdepth 1 -type d | sort)
    if [ ${#dirs[@]} -eq 0 ]; then
        echo "No subdirectories in $dir" >&2
        return 1
    fi
    local choice
    select choice in "${dirs[@]}" "CANCEL"; do
        [ "$choice" != "CANCEL" ] && [ -n "$choice" ] && break
        echo "Cancelled." >&2; return 1
    done
    echo "$choice"
}

echo "apk-utilities shell ready. HOME=$APKU_HOME"
echo "Type 'menu' to list tools, or run one directly: decode, build, sign, baksmali, ..."
