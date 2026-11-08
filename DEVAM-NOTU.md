# Devam notu (devralan yapay zekâ için) — 8 Kasım 2026, 3. aşama

Bu projeyi Claude yazdı. Devralan araç **yalnızca aşağıdakileri** yapar; plan dışına çıkmaz.

## Kaynaklar (tek doğru kaynak)
- Proje dokümanı: https://claude.ai/artifact/7aviV7rE1kBvm4eBtUtzm9
- Tasarım tuvali (7 ekran): https://claude.ai/artifact/J9L59HCcDDZVwuod8fYSiw
- Depo: https://bitbucket.org/mmaapps/mmustafatasoy (ana dal: `ana`)

## Şu anki durum
| Dal | İçerik | Durum |
| --- | --- | --- |
| `ana` | Aşama 1 + 2 (PR #1 ve #3 birleşti) | Derleme #5 yeşil |
| `asama-3-video` | Aşama 3: kendi cam video oynatıcısı (Media3 1.11.1) | Derleme #6 yeşil (19 test). `ana`'ya PR açık. |

## Hemen yapılacak (sırayla)
1. `asama-3-video` derlemesi yeşilse `ana`'ya açılan PR'ı kullanıcı birleştirir.
2. Kırmızıysa sadece hata veren satırı düzelt; başka dosyaya dokunma.
3. Sonra 4. aşama: dokümandaki "5. Koleksiyonlar" ve "7. Düzenle" bölümleri, yeni dal `asama-4-koleksiyonlar`.

## Kurallar (değişmez)
- Sürümler `gradle/libs.versions.toml` içinde; **yükseltme yok, yeni kütüphane yok** (Media3 1.11.1 3. aşamada eklendi).
- **INTERNET izni eklenmez.** Hiçbir veri cihazdan çıkmaz.
- Cam sadece `designsystem/Glass.kt` → `Modifier.glass(...)` ile çizilir. Ekranlarda doğrudan blur yazılmaz.
- Renk/boyut/hareket sadece `designsystem/Tokens.kt`. İkonlar `designsystem/icons/GalleryIcons.kt` (Apple ikonu yok).
- Tüm metinler `res/values/strings.xml` (Türkçe) + `res/values-en/strings.xml`. Kodda sabit metin yok.
- Material3 yok; sadece Compose foundation.
- Her aşama ayrı dal + ayrı PR. Commit mesajları Türkçe.
- Pipelines ücretsiz planda ayda 50 dk; yalnızca elle (`custom: apk`). Kasım'da ~26 dk kullanıldı (6 derleme). Bir derleme ~4 dk.

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

## Aşama 3'te yapılanlar
- `feature/viewer/VideoPlayerScreen.kt`: PlayerSurface (TextureView, cam arkasını örnekleyebilsin diye), dikey video Crop / yatay Fit,
  cam kapsül (oynat/duraklat · 10 karelik film şeridi · ses), süre (sabit genişlikli rakam), hız çipi 1× → 1,5× → 2× → 0,5×,
  çözünürlük rozeti (4K/HD · fps · HDR), sağa/sola çift dokun ±10 sn, aşağı çek kapat, 3 sn sonra kontroller gizlenir,
  videolar sessiz başlar (ses açılınca ses odağı istenir), kaldığı yerden devam, Resim içinde Resim (Android 12+ otomatik).
- Tek ExoPlayer: `SharedVideoPlayer`. Görüntüleyicideki video oynat düğmesi bu ekranı açar.
- Manifest: `supportsPictureInPicture`, `configChanges` (döndürmede oynatma durmaz).

## Aşama 3'te bilerek ertelenenler
- Videoda kıstırarak yakınlaştırma (4×).
- Film şeridi kareleri Media3 FrameExtractor yerine yerleşik `MediaMetadataRetriever` ile (ek kütüphane olmasın diye).
- Kaldığı yer SharedPreferences'ta; 5. aşamada Room'a taşınacak. Samsung hareketli fotoğrafların video kısmı.

## Yedek
Her aşama sonunda: kaynak + yazı tipleri + gradle wrapper + `git bundle --all` tek zip, kullanıcının Google Drive'ına.
