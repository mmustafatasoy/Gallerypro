#!/usr/bin/env bash
# Inter yazı tipini (SIL OFL 1.1) npm'den indirir, TTF'e çevirip res/font altına koyar.
# Derlemeden önce bir kez çalıştırılır. Gerekenler: curl, python3, fonttools, brotli.
set -euo pipefail
VERSION=4.1.1
OUT=app/src/main/res/font
if [ -f "$OUT/inter_extrabold.ttf" ]; then
  echo "Yazı tipleri zaten var."
  exit 0
fi
TMP=$(mktemp -d)
curl -fsSL "https://registry.npmjs.org/inter-ui/-/inter-ui-$VERSION.tgz" | tar -xz -C "$TMP"
mkdir -p "$OUT" app/src/main/assets/licenses
for W in Regular Medium SemiBold Bold ExtraBold; do
  L=$(echo "$W" | tr '[:upper:]' '[:lower:]')
  python3 - "$TMP/package/web/Inter-$W.woff2" "$OUT/inter_$L.ttf" <<'PY'
import sys
from fontTools.ttLib import TTFont
font = TTFont(sys.argv[1])
font.flavor = None
font.save(sys.argv[2])
PY
done
cp "$TMP/package/LICENSE.txt" app/src/main/assets/licenses/inter-OFL.txt
rm -rf "$TMP"
echo "Inter $VERSION hazır."
