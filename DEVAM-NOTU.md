# Devam notu (devralan yapay zekâ için) — 9 Kasım 2026, 5. aşama ilk adım (0.5.2)

Bu projeyi Claude yazdı. Devralan araç (Codex, ChatGPT…) **yalnızca aşağıdakileri** yapar; plan dışına çıkmaz.
Kısa kurallar `AGENTS.md`'de (Codex onu kendiliğinden okur); ayrıntı burada.

## Kaynaklar (tek doğru kaynak)
- Proje dokümanı: https://claude.ai/artifact/7aviV7rE1kBvm4eBtUtzm9
- Tasarım tuvali (7 ekran): https://claude.ai/artifact/J9L59HCcDDZVwuod8fYSiw
- Depo: https://github.com/mmustafatasoy/Gallerypro (ana dal: `main`). Bitbucket'taki `mmaapps/mmustafatasoy` eski kopyadır; artık kullanılmaz.

## Şu anki durum
| Dal | İçerik | Durum |
| --- | --- | --- |
| `main` | Aşama 1–4 + 5. aşama ilk adım (cihaz içi dizin + arama), sürüm 0.5.2 (hızlı release APK) | GitHub Actions yeşil (48 test) |
| `asama-2…`, `asama-3…`, `asama-4…`, `asama-5a-arama` | Bitbucket'tan taşınan aşama dalları (geçmiş için) | — |

## Hemen yapılacak (sırayla)
1. Kullanıcı 0.5.2'yi dener (kasma düzeldi mi?). Çökme olursa uygulama bir sonraki açılışta hata raporunu gösterir; kullanıcı "Paylaş" ile gönderir.
   Rapordaki yığın izinde adı geçen satırı düzelt; tahminle başka dosyaya dokunma.
2. Derleme kırmızıysa sadece hata veren satırı düzelt; başka dosyaya dokunma.
3. Yeni iş ayrı dalda (`asama-5b-…`) yapılır, PR ile `main`'e girer; kullanıcı birleştirir. Dalın APK'sı Releases'ta dal adıyla çıkar.
4. Sonra 5b: "Cümleyle arama" (MobileCLIP S0/S2 int8 + LiteRT) ve Bilgi panelindeki "Benzer Fotoğraflar". Model dosyası depoya
   konmaz; derlemede indirilir (fetch-fonts gibi bir betik). Lisans kontrolü yayından önce. Sonra 5c: Kişiler, Anılar, Harita.

## Kurallar (değişmez)
- Sürümler `gradle/libs.versions.toml` içinde; **yükseltme yok**. Yeni kütüphane sadece dokümandaki tablodan.
- **INTERNET izni eklenmez.** Hiçbir veri cihazdan çıkmaz.
- Cam sadece `designsystem/Glass.kt` → `Modifier.glass(...)` ile çizilir. Ekranlarda doğrudan blur yazılmaz.
- Renk/boyut/hareket sadece `designsystem/Tokens.kt`. İkonlar `designsystem/icons/GalleryIcons.kt` (Apple ikonu yok).
- Tüm metinler `res/values/strings.xml` (Türkçe) + `res/values-en/strings.xml`. Kodda sabit metin yok.
- Material3 yok; sadece Compose foundation.
- Her aşama ayrı dal + ayrı PR. Commit mesajları Türkçe.
- Derleme GitHub Actions'ta (`.github/workflows/apk.yml`): `main` ve `asama-*` dallarına her gönderimde; depo herkese açık, dakika sınırı yok.
  APK Releases'a yüklenir: `main` → `son-surum` (https://github.com/mmustafatasoy/Gallerypro/releases/download/son-surum/Gallerypro.apk), dallar → dal adı.
- Kullanıcıya verilen APK **release** türüdür (hata ayıklama kapalı, R8, `-dontobfuscate`), `release.jks` yoksa sabit
  debug anahtarıyla imzalanır. Debug APK Compose'da belirgin kasar; performans hiçbir zaman debug APK ile değerlendirilmez.
  Yalnızca `arm64-v8a` (defaultConfig `abiFilters`); Play Store'a AAB ile çıkarken kaldırılır.
- Debug APK sabit anahtarla imzalanır (`scripts/debug-keystore.b64` → `~/.android/debug.keystore`, iş akışında); yeni APK eskisinin üstüne kurulur. Anahtarı değiştirme.
- Saf mantık (CollectionsLogic, EditModel) yerelde Gradle'ın kendi Kotlin derleyicisiyle derlenip test edilebilir
  (`/opt/gradle/lib/kotlin-compiler-embeddable-*.jar`, android/compose/junit için küçük taslaklarla). Android kodu sadece GitHub Actions'ta derlenir.

## Bilinen tuzaklar
- `android.media.ExifInterface`'te `TAG_LENS_MODEL`, `TAG_PHOTOGRAPHIC_SENSITIVITY`, parametresiz `getLatLong()` yok → `"LensModel"`, `"ISOSpeedRatings"`, `getLatLong(FloatArray)`.
- cmdline-tools 23'te `sdkmanager --licenses` çalışmıyor → `scripts/accept-android-licenses.sh`.
- Inter yazı tipi ve Gradle derlemede indirilir (`scripts/fetch-fonts.sh`); ikili dosya depoya konmaz.
- `Offset` eşitliği bit düzeyinde: -0f ≠ 0f.
- Küçük resim önbelleği `MediaThumb(uri, boyut, item.modifiedMillis)`: düzenlenen fotoğraf eski görünmesin diye sürüm anahtara girer.

## Aşama 2'de bilerek ertelenenler
- 4096 px üstü fotoğraflarda parçalı (tile) yükleme; ızgaradan büyüyerek açılma animasyonu.
- Bilgi panelinde "Ayarla" (tarih/konum düzenleme), menüde Kopya oluştur.
- Başlıklar `CaptionStore` (SharedPreferences); 5. aşamada Room'a taşınacak.
- Akıllı Etiketler kartı 5. aşamada. Harita karosu yok (internet yok): konum kartı + "Haritada aç".

## Aşama 3'te yapılanlar / ertelenenler
- `feature/viewer/VideoPlayerScreen.kt`: cam kapsül, 10 karelik film şeridi, hız çipi, ±10 sn, sessiz başlama, kaldığı yerden devam, PiP.
- Ertelendi: videoda kıstırarak yakınlaştırma; Samsung hareketli fotoğrafların video kısmı; konumlar Room'a.

## Aşama 4'te yapılanlar
- **Koleksiyonlar sekmesi** (`feature/collections/`): Son Günler (yatay kartlar), Sabitlenenler (Favoriler, Videolar,
  Son Kaydedilenler), Albümler (klasöre göre, kamera başta; 4'ten fazlaysa "Tüm albümler"), Medya Türleri (Videolar,
  Panoramalar, Ekran Görüntüleri, RAW; boş olanlar gizli), Araçlar (Gizli, Son Silinenler, Kopyalar, İçe Aktarılanlar).
  "⋯" → bölümleri göster/gizle ve tutamaçtan sürükleyerek sırala (`SectionPrefs`).
- Koleksiyon ayrıntısı Kütüphane ızgarasını kullanır; görüntüleyici `ViewerMode` ile açılır:
  Gizli → menüde "Göster"; Son Silinenler → alt çubuk "Sil (kalıcı) · N gün · Kurtar".
- **Gizle**: görüntüleyici ⋯ menüsü. `HiddenStore`; gizlenen öğe Kütüphane ve koleksiyonlardan çıkar.
  Gizli albüm Android'in yerleşik BiometricPrompt'uyla açılır (parmak izi/yüz/ekran kilidi; ek kütüphane yok).
- **Düzenle** (`feature/edit/`): Vazgeç · SIFIRLA/ORİJİNAL · Bitti; Ayarla (8 araç, −100…+100 cetvel, halka göstergesi),
  Filtreler (8, CSS formülleriyle birebir), Kırp (Serbest/Kare/4:3/16:9/9:16, ±45° düzelt, köşeden boyutlandır, içinden taşı).
  Önizleme GPU'da (ColorMatrix; Android 13+'ta keskinlik AGSL). Kayıt `EditSaver` ile tam çözünürlükte, aynı formüllerle.
- **Yıkıcı olmayan düzenleme**: JPEG/PNG/WebP'de orijinal ilk kayıtta `files/originals/<id>` olarak birebir yedeklenir,
  ayarlar `EditStore`'da. Tekrar açınca yedekten + ayarlarla başlar. SIFIRLA + Bitti yedeği geri yazar.
  HEIC/RAW/GIF veya konum izni yoksa orijinale dokunulmaz, yanına `_duzenlendi.jpg` kopya eklenir. EXIF (tarih, kamera, konum) korunur.
- Kütüphane "⋯" menüsünde "Yalnızca düzenlenenler".
- Testler: `CollectionsLogicTest` (9), `EditLogicTest` (12) → toplam 40 birim testi.

## Kararlılık (0.4.1, `asama-4c-kararlilik`)
- **Çökme raporu** (`CrashReporter.kt`): yakalanmayan hata `files/crash.txt`'ye yazılır, bir sonraki açılışta
  `GalleryRoot` üstünde gösterilir (Paylaş / Kapat). İnternet yok; rapor yalnızca kullanıcı paylaşırsa çıkar.
- **Kırpma**: `CropMath` artık hiç `coerceIn` hatası fırlatmaz (`sanitize` + `within`); oran kilitliyken küçük kutuyu
  kenarda taşımak uygulamayı kapatıyordu. Test: `crop_neverThrowsOnBrokenRects`.
- **Cetvel**: genişlik 0 iken saydamlık NaN → `Color.copy` hata veriyordu; dar alanda çizim atlanır.
- Keskinlik AGSL efekti oluşturulamazsa atlanır. Yazma izni / konum izni penceresi açılamazsa kopya olarak kaydedilir;
  kaydetmeden önce Coil bellek önbelleği boşaltılır.
- Görüntüleyicide açılışta çökmenin kesin nedeni rapor olmadan bulunamadı; rapor gelince düzeltilecek.

## 5. aşama ilk adım (0.5.0, `asama-5a-arama`)
- **Dizin** (`data/index/`): `IndexWorker` (WorkManager) MediaStore'un 640 px küçük kopyasını ML Kit ile inceler:
  etiketler (image-labeling 17.0.9, güven ≥ 0,62, en çok 8) ve yazı (text-recognition 16.0.1, Latin). Modeller APK'da.
  Şarjda + boştayken 6 saatte bir; aramadaki "Şimdi incele" hemen başlatır. Yeni/değişen öğeler, en yeniden eskiye;
  20'lik gruplarla kaydeder, yarıda kalırsa devam eder. Silinenler dizinden çıkar. `IndexVersion` artarsa hepsi yenilenir.
- **Kayıt**: Room yerine uygulamanın kendi SQLite dosyası (`IndexStore`, `index.db`, platform API; KSP/derleme riski yok).
  Arama bellekte (10.000 öğede milisaniyeler). FTS kullanılmadı. Room'a geçiş istenirse 6. aşamada.
- **Arama** (`SearchEngine`, saf Kotlin, `SearchEngineTest` 8 test): Türkçe sadeleştirme (`SearchText`), etiketlerin Türkçe
  karşılıkları (`LabelDictionary`, ~250 etiket), ay/yıl/bugün/dün, video/fotoğraf/ekran görüntüsü/favori süzgeçleri,
  başlık + albüm + OCR eşleşmesi, basit Türkçe ek kırpma ("kediler" → "kedi"). Tüm sözcükler eşleşmeli (VE).
- **Ekran** (`feature/search/`): sekme çubuğundaki arama dairesi `LocalSearchLauncher` ile açar (GalleryRoot sağlar).
  Altta cam arama kutusu + kapatma dairesi (klavyenin üstüne kalkar); boşken Akıllı arama kartı, öneriler, son aramalar;
  sonuçlarda Tümü/Fotoğraflar/Videolar; dokununca görüntüleyici. Gizli öğeler çıkmaz.
- **INTERNET izni** ML Kit kütüphaneleri eklediği için manifestte `tools:node="remove"` ile kesin olarak kaldırıldı.
- **0.5.1:** çökme raporu (Tecno, Android 16) → `ThumbStrip`'te yay animasyonu boşluğu eksiye düşürüyordu ("Padding must be non-negative").
  Animasyonlu Dp değerleri padding/size'a verilmeden önce `coerceAtLeast(0.dp)` ile sınırlanır.
- Ertelendi (5b/5c): CLIP ile cümleyle arama ve benzer fotoğraflar; ML Kit çeviri (model indirmek internet ister, kullanılmaz;
  Türkçe sorgu sözlükle çözülür); videolarda 2 sn'de bir anahtar kare (şimdilik videonun küçük resmi); yer adıyla arama ve
  Yerler haritası; Kişiler; Bilgi panelinde "Akıllı Etiketler" kartı.

## Kasma düzeltmesi (0.5.2)
- Asıl neden: kullanıcıya **debug** APK veriliyordu (hata ayıklanabilir uygulamada Compose ve ART çok daha yavaş).
  Artık release APK (R8 ile küçültülmüş, hata ayıklama kapalı). Debug kurulumu (`.debug` sonekli) ayrı uygulamadır; kaldırılabilir.
- Dizinleyici: ML Kit tek, düşük öncelikli iş parçacığında (`setExecutor`), worker `THREAD_PRIORITY_BACKGROUND`;
  uygulama ekrandayken her fotoğraf arasında 150 ms bekler (`IndexWorker.appVisible`).
- Hâlâ kasarsa sıradaki şüpheli: aynı anda çok sayıda cam yüzeyin (üst bulanık başlık + düğmeler + sekme çubuğu)
  her karede bulanıklık/kırılma çizmesi. Doküman "en fazla 4 cam yüzey" diyor; sayılıp azaltılır veya kırılma kapatılır.

## Aşama 4'te bilerek ertelenenler
- Anılar, Kişiler ve Evcil Hayvanlar, Haritada (konum dizini + MapLibre), Selfie/Hareketli/Portre türleri → 5. aşama (dizinleyici).
- Video düzenleme (Media3 Transformer ile kırpma/süre kesme) → 6. aşama.
- Uzun basınca önizleme menüsü ve "Albüme ekle"; Kopyalar'da birleştirme.
- Bölüm sırası, gizliler ve düzenleme ayarları SharedPreferences'ta; 5. aşamada Room/DataStore'a taşınacak.
- Çok büyük fotoğraflar (uygulamanın bellek payını aşanlar) kaydederken yarı çözünürlüğe örneklenir.
- Silinen fotoğrafların `files/originals` yedeklerini temizleme.

## Yedek
Her aşama sonunda: kaynak + yazı tipleri + gradle wrapper + `git bundle --all` tek zip, kullanıcının Google Drive'ına.
**Her yedekte `DEVAM-NOTU.md` ayrıca dosya olarak kullanıcıya gönderilir** (kullanıcı bunu ChatGPT/Codex'e verip devam ettirebilsin).
