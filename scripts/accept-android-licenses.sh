#!/usr/bin/env bash
# Android SDK lisanslarını CI için kabul eder.
# cmdline-tools 23.0 ile "sdkmanager --licenses" artık hiçbir şey yapmıyor; bu yüzden
# lisans dosyaları ($ANDROID_HOME/licenses) doğrudan yazılır. Her dosyada, Google'ın
# depo listesindeki güncel lisans metninin SHA-1 özeti ve bilinen eski özetler bulunur.
set -euo pipefail
: "${ANDROID_HOME:?ANDROID_HOME tanımlı değil}"
mkdir -p "$ANDROID_HOME/licenses"
curl -fsSL -o /tmp/repository2.xml https://dl.google.com/android/repository/repository2-3.xml
python3 - "$ANDROID_HOME/licenses" /tmp/repository2.xml <<'PY'
import hashlib
import os
import sys
import xml.etree.ElementTree as ET

out_dir, xml_path = sys.argv[1], sys.argv[2]
known = {
    "android-sdk-license": [
        "8933bad161af4178b1185d1a37fbf41ea5269c55",
        "d56f5187479451eabf01fb78af6dfcb131a6481e",
        "24333f8a63b6825ea9c5514f83c2829b004d1fee",
    ],
    "android-sdk-preview-license": ["84831b9409646a918e30573bab4c9c91346d8abd"],
}
hashes = {k: list(v) for k, v in known.items()}
for el in ET.parse(xml_path).getroot().iter():
    if el.tag.split("}")[-1] != "license":
        continue
    lid, text = el.get("id"), el.text or ""
    if not lid:
        continue
    for variant in (text, text.strip()):
        h = hashlib.sha1(variant.encode("utf-8")).hexdigest()
        hashes.setdefault(lid, [])
        if h not in hashes[lid]:
            hashes[lid].append(h)
for lid, hs in hashes.items():
    with open(os.path.join(out_dir, lid), "w") as f:
        f.write("\n" + "\n".join(hs) + "\n")
print("Lisanslar yazıldı:", ", ".join(sorted(hashes)))
PY
