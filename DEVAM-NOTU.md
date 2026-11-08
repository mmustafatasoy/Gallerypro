# Devam notu (devralan yapay zekâ için) — 8 Kasım 2026

Bu projeyi Claude yazdı. Devralan araç **yalnızca aşağıdakileri** yapar; plan dışına çıkmaz.

## Kaynaklar (tek doğru kaynak)
- Proje dokümanı: https://claude.ai/artifact/7aviV7rE1kBvm4eBtUtzm9
- Tasarım tuvali (7 ekran): https://claude.ai/artifact/J9L59HCcDDZVwuod8fYSiw
- Depo: https://bitbucket.org/mmaapps/mmustafatasoy (ana dal: `ana`)

## Şu anki durum
| Dal | İçerik | Durum |
| --- | --- | --- |
| `asama-1-temel` | Aşama 1: iskelet, cam bileşenleri, Kütüphane | Derleme #2 yeşil. PR #1 (`ana`'ya) açık, kullanıcı birleştirecek. |
| `asama-2-goruntuleyici` | Aşama 2: cam görüntüleyici + Bilgi paneli | Kod derleniyor. #4'te 19 testten 1'i -0f yüzünden kaldı; düzeltmesi bu notla aynı commit'te. |

## Hemen yapılacak (sırayla)
1. `asama-2-goruntuleyici` dalında Pipelines › Run pipeline › **custom: apk** çalıştır.
2. Yeşilse `asama-2-goruntuleyici` → `ana` PR aç (önce PR #1 birleşmiş olmalı; değilse hedef `asama-1-temel`).
3. Kırmızıysa sadece hata veren satırı düzelt; başka dosyaya dokunma.

## Kurallar (değişmez)
- Sürümler `gradle/libs.versions.toml` içinde; **yükseltme yok, yeni kütüphane yok** (Media3 3. aşamada eklenecek, dokümandaki sürümle).
- **INTERNET izni eklenmez.** Hiçbir veri cihazdan çıkmaz.
- Cam sadece `designsystem/Glass.kt` → `Modifier.glass(...)` ile çizilir. Ekranlarda doğrudan blur yazılmaz.
- Renk/boyut/hareket sadece `designsystem/Tokens.kt`. İkonlar `designsystem/icons/GalleryIcons.kt` (Apple ikonu yok).
- Tüm metinler `res/values/strings.xml` (Türkçe) + `res/values-en/strings.xml`. Kodda sabit metin yok.
- Material3 yok; sadece Compose foundation.
- Her aşama ayrı dal + ayrı PR. Commit mesajları Türkçe.
- Pipelines ücretsiz planda ayda 50 dk; yalnızca elle (`custom: apk`). Kasım'da ~17 dk kullanıldı. Bir derleme ~4 dk.

## Bilinen tuzaklar
- `android.media.ExifInterface`'te `TAG_LENS_MODEL`, `TAG_PHOTOGRAPHIC_SENSITIVITY`, parametresiz `getLatLong()` yok → `"LensModel"`, `"ISOSpeedRatings"`, `getLatLong(FloatArray)`.
- cmdline-tools 23'te `sdkmanager --licenses` çalışmıyor → `scripts/accept-android-licenses.sh`.
- Inter yazı tipi ve Gradle derlemede indirilir (`scripts/fetch-fonts.sh`); ikili dosya depoya konmaz.
- `Offset` eşitliği bit düzeyinde: -0f ≠ 0f.

## Aşama 2'de bilerek ertelenenler
- 4096 px üstü fotoğraflarda parçalı (tile) yükleme; ızgaradan büyüyerek açılma animasyonu.
- Bilgi panelinde "Ayarla" (tarih/konum düzenleme), menüde Gizle / Kopya oluştur.
- Başlıklar şimdilik `CaptionStore` (SharedPreferences); 5. aşamada Room'a taşınacak.
- Akıllı Etiketler kartı 5. aşamada. Harita karosu yok (internet yok): konum kartı + "Haritada aç".
- Video şimdilik sistem oynatıcısında açılır; kendi oynatıcı 3. aşamada.

## Sonraki aşama: 3 · Video
Dokümandaki **"Video oynatıcı"** bölümünü birebir uygula (Media3 1.11.1, `PlayerSurface`, cam kontroller, film şeridi, hız, ses, PiP). Yeni dal: `asama-3-video`. `ViewerPage` içindeki video oynat düğmesi bu ekranı açacak.

## Yedek
Her aşama sonunda: kaynak + yazı tipleri + gradle wrapper + `git bundle --all` tek zip, kullanıcının Google Drive'ına.
