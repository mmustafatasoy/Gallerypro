#!/usr/bin/env python3
"""GeoNames şehir listesini (cities5000 + il adları) uygulamanın kullandığı küçük TSV'ye çevirir.

Kullanım: build_cities.py cities5000.txt admin1CodesASCII.txt cikti.tsv
Çıktı satırı: ad<TAB>il<TAB>ülke<TAB>enlem<TAB>boylam (4 ondalık).
Veri: GeoNames (https://www.geonames.org), CC BY 4.0.
"""
import sys

cities_path, admin_path, out_path = sys.argv[1:4]

admin = {}
with open(admin_path, encoding="utf-8") as f:
    for line in f:
        parts = line.rstrip("\n").split("\t")
        if len(parts) >= 2:
            admin[parts[0]] = parts[1]

count = 0
with open(cities_path, encoding="utf-8") as f, open(out_path, "w", encoding="utf-8") as out:
    for line in f:
        p = line.rstrip("\n").split("\t")
        if len(p) < 11:
            continue
        name, lat, lon, cc, a1 = p[1], p[4], p[5], p[8], p[10]
        clean = lambda s: s.replace("\t", " ").strip()
        region = admin.get(f"{cc}.{a1}", "")
        out.write(f"{clean(name)}\t{clean(region)}\t{cc}\t{float(lat):.4f}\t{float(lon):.4f}\n")
        count += 1

print(f"{count} şehir yazıldı → {out_path}")
if count < 1000:
    sys.exit("Şehir listesi beklenenden küçük")
