#!/usr/bin/env bash
# Emülatörde uygulamayı açıp ekran görüntüsü alır. Görüntüler shots/ klasörüne yazılır.
set -x

PKG=com.atasoy.gallerypro.debug
ACT=com.atasoy.gallerypro.MainActivity
mkdir -p shots

shot() {
  sleep "${2:-2}"
  adb exec-out screencap -p > "shots/$1.png"
}

dump_ui() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
}

# Metne ya da içerik açıklamasına göre ortasına dokun.
tap_match() {
  dump_ui
  local xy
  xy=$(python3 - "$1" <<'EOF'
import re, sys
target = sys.argv[1]
xml = open('/tmp/ui.xml', encoding='utf-8', errors='ignore').read()
for node in re.finditer(r'<node [^>]*>', xml):
    n = node.group(0)
    t = re.search(r' text="([^"]*)"', n)
    d = re.search(r' content-desc="([^"]*)"', n)
    if (t and t.group(1) == target) or (d and d.group(1) == target):
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
        if b:
            x1, y1, x2, y2 = map(int, b.groups())
            print((x1 + x2) // 2, (y1 + y2) // 2)
            break
EOF
)
  if [ -n "$xy" ]; then
    adb shell input tap $xy
  else
    echo "bulunamadi: $1"
  fi
}

adb install -r app/build/outputs/apk/debug/app-debug.apk

# Türkçe: uygulamaya özel dil (Android 13+)
adb shell cmd locale set-app-locales $PKG --locales tr-TR || true

# 1) İzin ekranı
adb shell am start -W -n $PKG/$ACT
shot 01_izin 5
adb shell am force-stop $PKG

# 2) Örnek medya
adb shell mkdir -p /sdcard/DCIM/Camera
for f in .github/screenshot-media/*; do
  adb push "$f" /sdcard/DCIM/Camera/ >/dev/null
done
adb shell content call --method scan_volume --uri content://media --arg external_primary || true
sleep 10

adb shell pm grant $PKG android.permission.READ_MEDIA_IMAGES
adb shell pm grant $PKG android.permission.READ_MEDIA_VIDEO

adb shell am start -W -n $PKG/$ACT
shot 02_kutuphane 7

tap_match "Aylar"
shot 03_aylar 3

tap_match "Yıllar"
shot 04_yillar 3

tap_match "Tümü"
sleep 2
tap_match "Diğer seçenekler"
shot 05_menu 2
adb shell input keyevent 4
sleep 1

tap_match "Seç"
sleep 1
W=$(adb shell wm size | grep -o '[0-9]*x[0-9]*' | tail -1 | cut -dx -f1)
H=$(adb shell wm size | grep -o '[0-9]*x[0-9]*' | tail -1 | cut -dx -f2)
adb shell input tap $((W / 2)) $((H / 2))
adb shell input tap $((W / 6)) $((H / 2 + W / 3))
shot 06_secim 2

tap_match "Vazgeç"
sleep 1
tap_match "Koleksiyonlar"
shot 07_bildirim 1

ls -la shots
