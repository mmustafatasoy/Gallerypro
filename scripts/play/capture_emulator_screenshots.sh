#!/usr/bin/env bash
set -euo pipefail

# Actual screenshots, captured from a running GalleryPro Android emulator.
# Never generate a mock screenshot with an image-generation tool.
mkdir -p build/store-screenshots
gradle --no-daemon -PaabBuild=true assembleDebug
APK=app/build/outputs/apk/debug/app-debug.apk
test -s "$APK"

adb shell wm size 1080x1920
adb shell wm density 420
adb shell mkdir -p /sdcard/Pictures/GalleryPro-Samples
for photo in build/store-sample-photos/*.jpg; do
  basename="$(basename "$photo")"
  adb push "$photo" "/sdcard/Pictures/GalleryPro-Samples/$basename" >/dev/null
  adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE \
    -d "file:///sdcard/Pictures/GalleryPro-Samples/$basename" >/dev/null || true
done
adb install -r "$APK"
for perm in READ_MEDIA_IMAGES READ_MEDIA_VIDEO READ_MEDIA_VISUAL_USER_SELECTED; do
  adb shell pm grant com.atasoy.gallerypro.debug "android.permission.$perm" || true
done

adb logcat -c
adb shell am force-stop com.atasoy.gallerypro.debug >/dev/null || true
adb shell am start -n com.atasoy.gallerypro.debug/com.atasoy.gallerypro.MainActivity
sleep 25

adb exec-out screencap -p > build/store-screenshots/01-GalleryPro-Kutuphane.png

# Try accessibility node bounds first (real Compose UI text), fall back to a
# centrally placed tap inside the bottom-left tabs, not the search circle.
adb shell uiautomator dump /sdcard/gallerypro-ui.xml >/dev/null || true
adb exec-out cat /sdcard/gallerypro-ui.xml > build/store-screenshots/screen-ui.xml || true
python3 - <<'PY'
import re, subprocess
from pathlib import Path
import xml.etree.ElementTree as ET
screen = Path('build/store-screenshots/screen-ui.xml')
target = None
try:
    tree = ET.parse(screen)
    for element in tree.iter('node'):
        fields = (element.get('text') or '') + ' ' + (element.get('content-desc') or '')
        if any(key in fields.casefold() for key in ('koleksiyon', 'collections')):
            coords = list(map(int, re.findall(r'\d+', element.get('bounds') or '')))
            if len(coords) == 4:
                target = ((coords[0]+coords[2])//2, (coords[1]+coords[3])//2)
                break
except Exception:
    pass
if target is None:
    target = (475, 1690)
print("Collections tab tap:", target)
subprocess.run(['adb','shell','input','tap',str(target[0]),str(target[1])],check=True)
PY
sleep 12
adb exec-out screencap -p > build/store-screenshots/02-GalleryPro-Koleksiyonlar.png

adb logcat -d -s AndroidRuntime:E > build/store-screenshots/android-runtime.txt || true
if grep -E 'FATAL EXCEPTION|Process: com\.atasoy\.gallerypro\.debug' build/store-screenshots/android-runtime.txt; then
  echo "::error::GalleryPro emulator run crashed"
  exit 1
fi

python3 - <<'PY'
from pathlib import Path
from PIL import Image, ImageChops
p1 = Path('build/store-screenshots/01-GalleryPro-Kutuphane.png')
p2 = Path('build/store-screenshots/02-GalleryPro-Koleksiyonlar.png')
with Image.open(p1) as a, Image.open(p2) as b:
    if a.size != (1080, 1920) or b.size != (1080, 1920):
        raise SystemExit('Emulator screenshots have unexpected dimensions')
    if ImageChops.difference(a.convert('RGB'), b.convert('RGB')).getbbox() is None:
        raise SystemExit('Both emulator screens are identical; tab navigation did not work')
print('PASS: two distinct authentic Android emulator screenshots, 1080x1920.')
PY
rm -f build/store-screenshots/screen-ui.xml
