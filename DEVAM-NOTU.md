# Devam notu (devralan yapay zekâ için) — 10 Kasım 2026, 0.8.0 (görev 1–5 ve 7 bitti)

Bu projeyi Claude yazdı. Devralan araç (Codex, ChatGPT…) **yalnızca aşağıdakileri** yapar; plan dışına çıkmaz.
Kısa kurallar `AGENTS.md`'de (Codex onu kendiliğinden okur); ayrıntı burada.

## Devralma (kullanıcı için)
1. PR #2 (`asama-5b-cumleyle-arama`, 0.6.0) `main`'e birleştirildi (bu notu `main`'de okuyorsan birleşmiştir). Yeni işler her zaman güncel `main`'den dal açılarak başlar.
2. Codex'e GitHub bağlanır, `mmustafatasoy/Gallerypro` seçilir, ilk mesaj olarak `CODEX-ILK-MESAJ.md` içeriği (İngilizce) verilir.
3. Actions kırmızıysa PR birleştirilmez. Uygulama çökerse açılıştaki hata raporu devralan araca aynen yapıştırılır.

## 10 Kasım 2026: 0.8.0'a kadar yapılanlar (Claude)
`CODEX-ILK-MESAJ.md` görevlerinden 1, 2, 3, 4, 5, 7 ve 8'in kod kısmı bitti, hepsi `main`'de:
- 0.6.1 Bilgi panelinde Akıllı Etiketler (dokununca arama) ve Benzer Fotoğraflar (`VectorMath.similar`, ≥0,80).
- 0.6.2 Videolar 3 kareden (`FrameLabels.merge`, `VectorMath.mean`, `VideoIndexVersion`/`VideoVectorVersion` = 2).
- 0.7.0 Yerler: GeoNames cities5000 (CC BY 4.0) derlemede indirilir (`scripts/places/build_cities.py`), `CityIndex` (60 km),
  `index.db` sürüm 3 `places` tablosu, şehir/il/ülke ile arama, Koleksiyonlar › Yerler, konum izni kartı.
- 0.7.1 Anılar (`data/memories/MemoryBuilder`): Bu Gün, geziler, yılın özeti; Koleksiyonlar › Anılar.
- 0.8.0 Ayarlar (Kütüphane ⋯): sürüm, Yeniden incele, tam cam efektleri anahtarı (`GalleryPerformance`), gizlilik,
  lisanslar; silinen fotoğrafların düzenleme yedekleri temizlenir. Play: `.github/workflows/aab.yml` (elle),
  `:modeller` kurulum-anı asset pack (yalnızca `-PaabBuild=true`), yayın anahtarı Secrets'tan.

**Kalan (kullanıcı kararı bekliyor):** görev 6 Kişiler (yüz modeli lisansı), görev 8'in kararları (uygulama adı, ikon,
gerçek yayın anahtarı → Secrets: RELEASE_KEYSTORE_B64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD). AAB iş akışı
henüz hiç çalıştırılmadı; ilk çalıştırmada hata verirse yalnızca hata veren satır düzeltilir.

## Kaynaklar (tek doğru kaynak)
- Proje dokümanı: https://claude.ai/artifact/7aviV7rE1kBvm4eBtUtzm9
- Tasarım tuvali (7 ekran): https://claude.ai/artifact/J9L59HCcDDZVwuod8fYSiw
- Depo: https://github.com/mmustafatasoy/Gallerypro (ana dal: `main`). Bitbucket'taki `mmaapps/mmustafatasoy` eski kopyadır; artık kullanılmaz.

## Şu anki durum
| Dal | İçerik | Durum |
| --- | --- | --- |
| `main` | Aşama 1–4 + 5a (cihaz içi dizin + arama) + 5b cümleyle arama (CLIP, Türkçe dahil), sürüm 0.6.0 | PR #2 ile girdi |
| `asama-5b-cihaz-ici-arama` | **TERK EDİLDİ — kullanma, birleştirme, üstüne iş yapma.** Eski `main`'den (0.5.2) açıldı; 5b zaten bittiği hâlde aynı işi MobileCLIP + LiteRT ile baştan kurmaya çalışır. Çalışan özellik içermez. | Yalnızca geçmiş için duruyor |
| `asama-2…`, `asama-3…`, `asama-4…`, `asama-5a-arama` | Bitbucket'tan taşınan aşama dalları (geçmiş için) | — |

## Hemen yapılacak (sırayla)
**Uygulamanın bitişine kadar tam yol haritası (görev 1–8, ayrıntılı, İngilizce): `CODEX-ILK-MESAJ.md`.** Aşağıdaki maddeler onun özetidir; çelişirse o dosya geçerlidir.

1. Kullanıcı 0.6.0'ı dener (APK: https://github.com/mmustafatasoy/Gallerypro/releases/download/son-surum/Gallerypro.apk).
   Aramada "Şimdi incele" → tüm fotoğraflar yeniden incelenir (etiketi olanlara yalnızca anlam vektörü eklenir).
   Çökme olursa uygulama bir sonraki açılışta hata raporunu gösterir; yığın izinde adı geçen satırı düzelt, tahminle başka dosyaya dokunma.
2. Derleme kırmızıysa sadece hata veren satırı düzelt; başka dosyaya dokunma.
3. Sıradaki iş **5b-2: Bilgi panelinde "Benzer Fotoğraflar"** (ayrı dal `asama-5b2-benzer`):
   `IndexStore.vectors`'tan açık fotoğrafın vektörüne en yakın 12 öğe (`VectorMath.nearest`, kendisi hariç, eşik ~0,80),
   yatay küçük resim şeridi; vektör yoksa kart gizli. Yeni kütüphane yok.
4. Sonra 5c: Anılar (tarih + yer kümeleri), yer adıyla arama (EXIF konumu → çevrimdışı şehir listesi), Kişiler.
   **Kişiler için yüz modeli lisansı önce kullanıcıya sorulur** (ML Kit yüz algılama yüzü bulur ama tanımaz; tanıma modeli
   seçimi lisans kararıdır). Video anahtar kareleri ve "Akıllı Etiketler" kartı da 5c'de.

## Dikkat: 5b'yi yeniden yapma
Cümleyle arama `main`'de hazır: `data/clip/` (ClipEncoders, WordPieceTokenizer, VectorMath), `IndexStore.vectors`,
`scripts/models/export_clip.py`. Model ve çalıştırıcı seçimi kapanmış karardır; MobileCLIP'e dönülmez, LiteRT eklenmez,
`data/semantic/` gibi ikinci bir vektör katmanı açılmaz. Benzer Fotoğraflar mevcut `VectorMath.nearest` ile yapılır.

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
- Saf mantık (CollectionsLogic, EditModel, WordPieceTokenizer) yerelde Gradle'ın kendi Kotlin derleyicisiyle derlenip test edilebilir
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

## 5b: cümleyle arama (0.6.0, `asama-5b-cumleyle-arama`)
- **Modeller**: görsel kodlayıcı OpenAI CLIP ViT-B/32 (MIT), metin kodlayıcı `sentence-transformers/clip-ViT-B-32-multilingual-v1`
  (Apache-2.0; 50+ dil, Türkçe dahil; CLIP'in metin uzayına damıtılmış DistilBERT). İkisi de 512 boyutlu birim vektör verir.
  MobileCLIP **kullanılmadı**: ağırlık lisansı (Apple AMLR) yalnızca araştırma içindir.
- **Çevirme**: `scripts/models/export_clip.py` (litert-torch + ai-edge-quantizer, `dynamic_wi8_afp32` int8 ağırlık).
  `.github/workflows/models.yml` çalıştırır, çıktıyı **`modeller` sürümüne** (Releases) yükler: `clip_image.tflite` (90 MB),
  `clip_text.tflite` (137 MB), `clip_text_vocab.txt`, `clip_tokens_test.json`. Doğruluk: TFLite~PyTorch kosinüs 0,995 (görsel), 0,9995 (metin).
  Betik değişirse iş akışı kendiliğinden yeniden çalışır; elle: Actions › Modeller › Run workflow.
- **Derleme**: `apk.yml` modelleri `app/src/main/assets/models/`'e indirir (depoya girmez, `.gitignore`). Yoksa APK yine derlenir,
  cümleyle arama kapalı kalır (`ClipAssets.available`). `.tflite` sıkıştırılmadan paketlenir, belleğe eşlenir. APK ≈ 260 MB (kullanıcı onayladı).
- **Çalışma zamanı**: LiteRT 2.3.0 (`com.google.ai.edge.litert:litert`, `org.tensorflow.lite.Interpreter`).
  `data/clip/ClipEncoders.kt`: `ClipImageEncoder` (ortadan kare kırp → 224 px, NHWC 0..1; normalizasyon modelin içinde),
  `ClipTextEncoder` (ilk aramada yüklenir, son sorguyu önbellekte tutar). `WordPieceTokenizer.kt` Hugging Face ile birebir
  (CI'da `clip_tokens_test.json` ile karşılaştırılır; 64 belirteç, büyük/küçük harf korunur).
- **Dizin**: `IndexWorker` küçük resmi bir kez yükler; gerekiyorsa etiket + yazı, model varsa vektör çıkarır. `index.db` sürüm 2:
  yeni `vectors` tablosu (id, modified, vec BLOB, version); etiketler korunur. `VectorVersion` artarsa vektörler yenilenir.
  Ekrandayken öğe arası bekleme 250 ms.
- **Arama**: `SearchEngine.semanticText` süzgeç sözcüklerini (yıl, ay, video, favori, ekran görüntüsü…) çıkarır, yazım korunur.
  Sonuç = sözcük eşleşmeleri (önce) + `VectorMath.nearest` (en az 0,20 benzerlik ve en iyiden en çok 0,06 aşağısı, en çok 300).
  Tarih/tür süzgeçleri anlam sonuçlarına da uygulanır. Ekranda "Cümleyle Dene" örnekleri (vektör varsa) ve kartta açıklama.
- **Testler**: `ClipLogicTest` (belirteçleyici, vektör, sorgu metni, birleştirme).
- Eşikler gerçek fotoğraflarla ayarlanmadı; kullanıcı "alakasız sonuç çok" derse `VectorMath.MinSimilarity`'i 0,22–0,24'e çıkar.

## Yedek
Her aşama sonunda: kaynak + yazı tipleri + gradle wrapper + `git bundle --all` tek zip, kullanıcının Google Drive'ına.
**Her yedekte `DEVAM-NOTU.md` ayrıca dosya olarak kullanıcıya gönderilir** (kullanıcı bunu ChatGPT/Codex'e verip devam ettirebilsin).

## Aşama 7a — Anı slayt gösterisi (0.8.2, `asama-7a-slayt`)

- PR #9'daki "Sıradaki küçük işler" sırasının **1. adımı** uygulandı; kullanıcı incelemesi ve birleştirmesi bekleniyor.
- Yalnızca Anılar koleksiyonlarının `CollectionDetailScreen` başlığında mevcut `GlassTextButton` ile **Oynat** gösterilir; boş Anılar koleksiyonunda gösterilmez.
- Düğme o anının ilk öğesinden `ViewerScreen`'i açar ve **mevcut** `slideshow` modunu başlatır. Yeni oynatıcı, Worker, model, mimari veya kütüphane eklenmedi. Fotoğrafa normal dokunmak eski görüntüleme davranışını korur.
- `ViewerScreen`'in diğer çağrıları, varsayılan `startSlideshow = false` ile değişmeden çalışır.
- Yeni saf hesap mantığı eklenmedi; mevcut JVM testleri ve GitHub Actions APK derlemesi kontrol edilmelidir.
- **Sıradaki iş:** 7b Uzun basınca önizleme + Albüme ekle. PR #9 mimari/kaynak tablosu esas alınır.
 
## Aşama 7b — Uzun basınca önizleme ve Albüme ekle (0.8.3)

- `asama-7b-onizleme`: PR #9'un kod yapısı ve kaynak tablosuna uygun ayrı dal.
- `LibraryGrid` mevcut `combinedClickable(onLongClick)` kullanılarak Kütüphane ve normal koleksiyonlarda ortak büyük önizleme + cam işlem menüsüne bağlandı (Paylaş, Favori, Albüme ekle, Gizle, Sil). Seçim modu ve Gizli/Çöp davranışı korundu.
- `CollectionsLogic.albums` mevcut sonucu kullanılır, ikinci MediaStore sorgusu yok. `MediaStore` `IS_PENDING` ile `Dispatchers.IO` üzerinden kopya yazılır; orijinal taşınmaz. Yarım kopya hata durumunda silinir. Kopya menüsü sistemin izin verdiği albüm köklerini gösterir.
- `DeleteCard`, `ManageMedia` ve MediaStore sistem istekleri yeniden kullanıldı. `AlbumCopyLogicTest` eklendi.
- Her başarılı APK iş akışından sonra ZIP kaynak yedeği, fontlar, Gradle wrapper, Git geçmişi bundle, `DEVAM-NOTU.md` ve SHA-256 Releases'a eklenir. CLIP modelleri ayrı `modeller` release'inden erişilir.
- Sıradaki iş: **7c Kopyaları birleştir**. Kullanıcı onayı olmadan main'e birleştirme yok.

## Aşama 7c — Kopya Fotoğrafları Birleştirme (0.8.4, `asama-7c-kopyalar`)

- PR #11 / 7b, başarılı CI ve tam kaynak ZIP yedeğinden sonra kullanıcı onayıyla `main`'e birleştirildi (commit `5096b743d490a1f16acc38490b31675373180336`).
- 7c, **ayrı dal + ayrı PR** olarak başlatıldı. PR #9'un kod yapısı ve "NEREDEN NE ALINIR" kaynak tablosuna uyulur; yeni mimari, veritabanı, Worker, kütüphane veya oynatıcı yok.
- Mevcut `CollectionsLogic.duplicates` adaylarından `DuplicateReviewLogic` ile, değişmemiş fotoğrafların **CLIP görsel kosinüsü ≥0,97** olanları güvenli ve birbirleriyle uyumlu gruplar hâlinde gösterilir; dizinlenmeyen veya eski vektörler toplu silmeye önerilmez. Sadece fotoğraflar; videolar bu otomatik incelemeye girmez.
- Gruplar `Kopyalar` ekranında yan yana küçük resimlerle gösterilir. En yüksek çözünürlükteki görsel `Koru` olarak işaretlenir (eşitlikte favori, dosya boyutu, tarih); diğerleri yalnızca **DeleteCard** onayıyla `MediaStore.createTrashRequest` üzerinden Son Silinenler'e taşınır. Onay öncesi gruptaki medya kimlikleri tekrar kontrol edilir; işlem otomatik çalışmaz.
- `CollectionsViewModel` mevcut `MediaRepository.items` + `IndexStore.vectors` akışını arka plan `Dispatchers.Default` üzerinde birleştirir; kullanıcıya ait veriler cihaz dışına çıkmaz. `DuplicateReviewLogicTest` saf JVM testleri eklendi.
- GitHub Actions başarılı olduğunda otomatik kaynak ZIP + SHA-256 aynı sürümün Releases sayfasına yüklenir. ZIP'de Git geçmişi `.bundle` dosyası da vardır.
- Sıradaki aşama **7d Tarih/konum düzenle**. Kullanıcı onayı, yeşil CI ve ZIP yedeği olmadan `main` ile birleştirilmez.

## Aşama 7d — JPEG tarih ve konum bilgilerini düzenleme (0.8.5, `asama-7d-tarih-konum`)

- PR #12 / 7c, başarılı GitHub Actions testi ve ZIP yedeği doğrulanarak `main`'e birleştirildi (commit `00e3f9d923a3a00b9a6063839844987d5b03676b`).
- Bilgi panelindeki **Ayarla** yalnızca normal görüntüleyicide **JPEG** fotoğraflarda görünür. HEIC/PNG/RAW/video ve Gizli/Son Silinenler için gösterilmez.
- Açılan formda çekim tarihi/saat, enlem-boylam girilir veya konum açık onayla kaldırılır. Geçersiz tarih ve aralık dışı koordinatlar yazmadan önce reddedilir (`MetadataEditLogicTest`).
- `android.media.ExifInterface` ile geçici JPEG üzerinde `DateTime`, `DateTimeOriginal`, `DateTimeDigitized` ve isteğe bağlı GPS değiştirilir; geçici dosya doğrulanmadan orijinale yazılmaz. Android'in `MediaStore.createWriteRequest` sistem izni ve `ACCESS_MEDIA_LOCATION` gerekir.
- Tam dosya önce `EditStore.backupFile(id)` üzerinden `files/originals/<id>` konumuna birebir yedeklenir; mevcut yedeklerin üzerine yazılmaz. Hatalı yazmada o anki fotoğraf geçici kopyadan geri yüklenmeye çalışılır; geri yükleme de başarısızsa kalıcı yedek **silinmez**.
- Disk işlemleri `Dispatchers.IO` üzerinde yapılır; internet erişimi, yeni bağımlılık, yeni veritabanı veya Worker yoktur. Kaydedildikten sonra mevcut MediaStore taraması ve dizinleme isteği kullanılır.
- GitHub Actions başarılı APK + testleri sonrasında `Gallerypro-kaynak.zip` ve SHA-256 aynı sürümde yayımlanır. Başarılı olduğunda PR kullanıcı talimatı gereği otomatik `main` dalına birleştirilir.
- Sıradaki aşama **7e Video kırpma** için **Media3 Transformer** adlı yeni kütüphane gerekir: başlanmadan önce kullanıcı kararı alınmalıdır.

## Aşama 7e — Video Kırpma (0.8.6, `asama-7e-video-kirpma`)

- Kullanıcı, önerilen **Media3 Transformer** ile video kırpma özelliğine onay verdi. Mevcut `media3 = 1.11.1` sürümü değişmedi; yalnızca aynı sürümden `androidx.media3:media3-transformer` modülü eklendi (`gradle/libs.versions.toml`).
- Video görüntüleyicisindeki mevcut **Düzenle** düğmesi artık `VideoTrimScreen` ekranını açar. Oynatıcı yeniden yazılmadı: `SharedVideoPlayer`, `extractFrames` (10 karelik film şeridi), `GlassTextButton`, `GlassIconButton` ve `designsystem/Tokens.kt` yeniden kullanıldı.
- İki tutma koluyla başlangıç/bitiş seçilir (en az 1 saniye), video yalnızca seçilen aralıkta önizlenir. `VideoTrimLogic` saf Kotlin, `VideoTrimLogicTest` JVM testleri var.
- `VideoTrimExporter`, `Transformer` ile yalnızca **yeni** yerel geçici MP4 oluşturur; başarılı olduğunda `MediaStore.IS_PENDING` üzerinden orijinal klasörüne veya `Movies/Gallerypro/` klasörüne ayrı video kaydeder. Orijinal video hiç değiştirilmez, hatalı çıktılar temizlenir, iş iptal edilebilir.
- Foundation Compose, cihaz içi çalışma, internetsiz yapı, Türkçe/İngilizce string kaynakları korunur. İşlem sırasında uygulama açık kalmalıdır, cihazın kodek destekleri dışa aktarmayı etkileyebilir.
- Başarılı GitHub Actions test + APK + ZIP + SHA-256 doğrulamasından sonra kullanıcı talimatına uygun şekilde ayrı PR, ardından `main` dalına birleştirme. Gerçek Android 16 telefon kurulumu ve video dışa aktarma testleri ayrıca yapılmalıdır.
- **Sıradaki karar:** Görev 6 Kişiler/yüz tanıma için lisanslı yüz modeli veya görev 8 Google Play için isim, ikon ve imza anahtarı kullanıcı tarafından seçilir. İzin almadan başlamayın.

## Video kırpma kararlılık düzeltmesi — 0.8.7 (`duzeltme-video-kirpma-kararlilik`)

- Kullanıcı, 0.8.6'da video kırpma sırasında uygulamanın kapandığını bildirdi. Cihazın gerçek hata yığını henüz sağlanmadığı için kesin kök neden olarak iddia edilmez.
- Kod incelemesinde iki somut eşzamanlılık/bellek riski bulundu: Eski `VideoPlayerScreen` ile `VideoTrimScreen` **aynı `SharedVideoPlayer`** örneğinin `DisposableEffect` işlemlerini aynı geçişte çalıştırıyordu; ayrıca kırpma dışa aktarılırken ExoPlayer dekoderi yalnızca duraklatılmış olarak açık kalıyordu.
- Video ekranından kırpmaya geçerken `pendingTrimId` ile eski oynatıcıyı hemen kompozisyondan çıkarıp bir sonraki karede kırpma ekranını açma; aynı ExoPlayer'a aynı anda dokunan iki sahip kalmaz.
- Dışa aktarma öncesi ExoPlayer stop + clear; Transformer tek başına çalışır. Hata/iptalde önizleme yeniden hazırlanır. İptalde çıktı temizlenmeden ikinci dışa aktarma başlatılamaz.
- Yeni model, kütüphane, internet izni veya tasarım değişikliği yapılmadı; 0.8.7 `versionCode 21`. GitHub Actions testleri ve ZIP/sha doğrulanır, sonra kullanıcı talimatıyla main'e birleştirilir.
- **Kişiler / yüz tanıma:** Kullanıcı istedi. Ancak `CODEX-ILK-MESAJ.md` görev 6 gereği yüz-gömme modelinin lisansı kullanıcı tarafından seçilmeden koduna başlanmaz. Onaydan sonra ayrı dal/PR.
- Kesin çökme kök nedeni için kullanıcı uygulamayı yeniden açınca çıkan `CrashReportSheet` raporunun metnini paylaşabilir. Android 16 cihaz kurulumu ve gerçek kırpma testi ayrıca gereklidir.

## Görev 6 — Kişiler / yüksek doğruluklu yüz tanıma (0.9.0, `asama-6-kisiler-sface-fp32`)

- Kullanıcı **yüksek doğruluk** tercih etti, APK boyutu ikincil. Seçilen **OpenCV Zoo SFace 2021dec FP32 (~36,9 MB)**, YuNet 2023mar yüz algılama ve OpenCV Android **4.14.0**; INT8 yüz tanıma kullanılmadı.
- Model dosyaları Git'e eklenmedi. `apk.yml` resmî `opencv/opencv_zoo` kaynağından modelleri indirir, boyutlarını denetler ve ilgili Apache-2.0 / MIT lisans dosyalarını APK'ya kopyalar. OpenCV AAR resmi Maven Central dağıtımıdır.
- Koleksiyonlar > Araçlar > **Kişiler** açılır. Kullanıcının açık **Kişileri Tara** eylemiyle görüntüler `PeopleScanner` içinde `Dispatchers.Default` üzerinde incelenir; orijinal medya değiştirilmez. OpenCV yüz algılama + beş noktalı hizalama + 128 boyutlu FP32 vektör + yerel eşleştirme yapılır. Dizin ve atanan isimler özel uygulama depolamasında, sunucu veya internet yoktur. Gizli/çöp fotoğraflar dizine alınmaz.
- `PeopleLogic` sahte eşleşmeleri azaltmak için korumacı benzerlik eşiği kullanır; kesin kişi kimlik doğrulaması değildir. Gruplar kullanıcıya gösterilir, elle isimlendirilebilir ve fotoğraflar normal `ViewerScreen` ile açılır. Saf JVM testleri `PeopleLogicTest` eklendi.
- **Lisans riski / yayın öncesi kontrol:** OpenCV Zoo SFace model dizininde Apache-2.0 yazsa da ticari dağıtımda eğitim veri seti kaynağının hakları hakkındaki GitHub OpenCV Zoo #318 sorusu çözülmeden Google Play ticari yayın uyumluluğu garanti edilmez. Bu sürüm geliştirme / test amaçlıdır.
- Gerçek Android 16 üzerinde model açma / yüz eşleştirme doğrulaması gerekir. CI APK + kaynak ZIP başarıyla tamamlanırsa kullanıcı talimatıyla main'e birleştirilebilir.

## Kritik çökmeyi giderme — Motion Glass / RenderThread (0.9.1, `asama-kritik-cam-katman-crash`)

- Kullanıcının 18,9 saniyelik ekran kaydında 5. saniyede video görüntüleme/kırpma geçişinde, 11. ve yaklaşık 16–17. saniyelerde Koleksiyonlar > Kişiler çevresinde **uygulamanın Android tarafından kapatıldığı** görülüyor. Başarısızlık yalnızca video dışa aktarmaya özgü değil. Cihazın `logcat`/tombstone dökümü olmadan kesin kök neden belirlenmiş sayılmıyor.
- Kod incelemesinde gerçek bir mimari hata bulundu: `VideoTrimScreen`, `PeopleScreen`, `MetadataEditorScreen` içindeki `.layerBackdrop(backdrop)` **aynı backdrop'u örnekleyen Glass buton ve sheet'lerin üst katmanı/atasıydı**. Backdrop geliştiricisinin dokümantasyonunda bu döngüsel çizim RenderThread üzerinde native `SIGSEGV` ile ilişkilendiriliyor (AndroidLiquidGlass #54, Kyant Backdrop Glass Bottom Sheet örneği).
- Bu ekranlarda kaynak `Box(Modifier.layerBackdrop(...).background(...))` artık yalnızca zemin çiziyor; cam butonlar/alanlar üstteki **kardeş composable öğeler** olarak çiziliyor. `CollectionDetailScreen` Kopyalar görünümünde de `DuplicateGroupsContent` cam butonları yakalayan backdrop'un dışına taşındı. Orijinal Gallery Motion Glass tasarımının ikonları, boyutları ve renk tokenları değiştirilmedi.
- Sadece Compose katmanlama değişikliği: yeni kütüphane, internet izni, model, video kodek sürümü veya API izni yok. `versionCode=23`, `versionName=0.9.1`.
- Test ve APK + tam kaynak ZIP + SHA-256 başarılı olmadan `main` ile birleştirilmez. Gerçek Android 16 telefon testi gereklidir. Hâlâ kapanırsa cihazdan `logcat` fatal signal/tombstone kayıtları alınmalı; Java `CrashReporter` native SIGSEGV'i yakalayamaz.

## Aşama 8a — Google Play güvenli AAB ön hazırlığı (0.9.2, `asama-play-yayin-hazirlik`)

- Kullanıcı 0.9.1'in cihazda çalıştığını bildirdi; sıradaki adım Google Play yayınına **hazırlık**, henüz gönderim/yayın yapılmıyor.
- `.github/workflows/aab.yml` güvenliğe alındı: asama-play-* dalına push ile yalnızca Actions artifact olarak **TEST AAB** (debug imzalı, Play için kullanılamaz), `workflow_dispatch publish=true` ile sadece `main` ve gerçek 4 GitHub Secrets varlığında imzalı yükleme AAB'si üretilir. Sessiz debug-key fallback ile Play'e hazırmış gibi AAB yayımlanmaz. Play Console'a otomatik gönderme yok.
- 0.9.0'da eklenen **YuNet ve SFace FP32** resmi OpenCV Zoo ağırlıkları artık AAB'nin `base/assets/face` içinde, ilgili lisanslarla beraber; CLIP `modeller` install-time asset pack'te. AAB açılarak bütün model yolları ve JAR imzası doğrulanır. Git'e büyük model ağırlığı/özel anahtar eklenmez.
- `play/privacy-policy.html` Türkçe+İngilizce sitene koyulmaya hazır politika taslağı (henüz canlı URL yok). `play/store-listing.md` mağaza metinleri ve `play/PLAY-CHECKLIST.md` imza, izin beyanları, SFace lisansı, kapalı test, varlıklar, veri güvenliği incelemesi.
- `versionCode 24`, `versionName 0.9.2`. Test APK'sı önceki sabit imza ile güncellenebilir; gerçek Play upload anahtarı geliştirici tarafından yönetilmelidir.
- Yayına geçmeden kullanıcıdan **gerçek yükleme anahtarı ve mağaza kimliği**, halka açık gizlilik sayfasının yayını ve SFace modelinin ticari dağıtım haklarına ilişkin doğrulama gerekir. Play Console incelemesi, gerçek cihaz testi ve mağaza varlıkları tamamlanmadan “Google Play'de yayımlandı” denmez.
- Her aşama yeşil CI + APK + kaynak ZIP + SHA-256 sonrası otomatik `main` dalına birleştirilir. AAB doğrulaması da ayrıca yeşil olmalıdır.

## Aşama 9a — Büyük fotoğraflar için bölgesel yükleme (0.9.3, `asama-093-buyuk-fotograf-tile`)

- Kullanıcı önce özelliğin amacını öğrendi ve açıkça uygulanmasını istedi. Orijinal fotoğrafı yeniden sıkıştırmadan, görüntüleyicinin bellek tüketimini azaltmak hedeflendi.
- `ViewerPage.kt`: 4096px üzeri JPEG/PNG/WebP için fotoğrafın tamamı çözünürlükte açılmaz. Önce Coil ile **2048px önizleme** çizilir. 1,25× üstü yakınlaştırmada ekranın gördüğü orijinal piksel alanı hesaplanır; Android `BitmapRegionDecoder.decodeRegion` ile yalnızca bu alan en fazla 2048px kenarlı örneklenerek `Dispatchers.IO` üzerinde çözülür ve aynı ZoomState grafiğinin içinde üstüne çizilir.
- Kaydırma/yakınlaştırma `snapshotFlow + debounce + collectLatest` üzerinden işlenir, hızlı hareket sırasında eski bölgelerden gereksiz decode sürdürülmez; fotoğraf veya sayfa değişiminde parça bellekte tutulmaz. Orijinal dosyaya yazma ve internet yok.
- `LargePhotoTileMath` saf Kotlin viewport/taşma/örnekleme hesabı, EXIF 0/90/180/270 derece ham koordinat dönüşümü, `LargePhotoTileMathTest` JVM testleri; `LargePhotoRegionDecoder` hasarlı dosyada düşük çözünürlüklü önizlemeye döner.
- Küçük/orta fotoğrafların var olan Coil 4096px yüksek çözünürlük yolu, çift dokunma, pinch-to-zoom, fotoğraf sayfalama, cam düğmeler ve video görüntüleyicisi değiştirilmedi. Kullanıcının onayladığı Motion Glass ölçüleri korunuyor.
- Bilinçli sınır: HEIC/RAW/GIF için bölgesel decoder desteği garanti olmadığından mevcut akış kalır; bazı fotoğraflarda MediaStore boyutu ham dosyayla uyuşmazsa yalnızca önizleme gösterilir.
- Yayın öncesi **gerçek 48/108/200 MP JPEG**, döndürülmüş çekim, panorama, düşük RAM, hızlı yakınlaştırma ve sayfalama testleri yapılmalı. GitHub Actions başarılı test + APK + tam kaynak ZIP + SHA-256 doğrulamadan main'e birleştirme yapılmaz.
- Sıradaki ertelenenler: video pinch-to-zoom, Samsung hareketli fotoğraf video bölümü, ızgaradan açılış animasyonu (bunların kapsamına bu aşamada girilmez).

## Aşama 9b — Kişisel APK için geçici, güvenli güncelleme (0.9.4)

- Kullanıcı GalleryPro'yu GitHub APK'sı olarak kullanıyor; **Google Play'e geçene kadar** uygulama içi güncelleme istedi. Önceki INTERNET yasak kuralı yalnızca kişisel APK için açık kullanıcı kararıyla dar kapsamlı değiştirildi.
- **İki Android manifesti:** `app/src/main/AndroidManifest.xml` doğrudan APK için `INTERNET` ve `REQUEST_INSTALL_PACKAGES` içerir. `app/src/main/AndroidManifest-play.xml` ise `-PaabBuild=true` ile seçilir ve `tools:node="remove"` ile iki izni kaldırır. `BuildConfig.SIDELOAD_UPDATER=false` olduğundan Play sürümünde güncelleme bileşeni çalışmaz. Manifest merger `tools:node` içinde placeholder kabul etmediği için ayrı dosyalar kullanıldı.
- `UpdateLogic` saf Kotlin: yalnızca `Gallerypro X.Y.Z (main)` sürüm adını ve resmî HTTPS `son-surum/Gallerypro.apk` adresini kabul eder; 64 basamaklı GitHub asset SHA-256 ve dosya boyutu şarttır. `UpdateLogicTest` eklendi.
- `UpdateManager`: yalnızca herkese açık GitHub Releases metadata API'sini sorgular, başka medya/kimlik verisi göndermez. Açılışta kontrol, 30 dk önbellek, Ayarlar'da manuel kontrol; yeni sürüm uyarısı bir defa gösterilir.
- Kullanıcı isterse Android `DownloadManager` ile APK indirir. **SHA-256 ve boyut doğrulaması bitmeden kurulum ekranını açmaz.** Android 8+ bilinmeyen kaynak yükleme izni ve kullanıcının sistem kurulum onayı gerekir. Eski APK ile yeni APK imzası eşleşmezse Android kurulumunu reddeder.
- `GalleryRoot` açılış uyarısı ve kurulum teklifi, `SettingsScreen` güncelleme kartı, Türkçe/İngilizce kaynaklar eklendi. Görsel/arayüz kodu Compose foundation; yeni kütüphane yok.
- Google Play AAB'de `REQUEST_INSTALL_PACKAGES` yasak ve INTERNET izni yok; AAB manifesti ve Play derlemesi ayrı doğrulanmalıdır. CI `testDebugUnitTest assembleRelease` + tam kaynak ZIP + SHA-256 başarıyla tamamlanmadan ana dala birleştirilmez.

## Aşama 9c — Video oynatıcıda iki parmakla yakınlaştırma (0.9.5, `asama-095-video-yakinlastirma`)

- Kullanıcının `devam et` talebiyle, 0.9.4 güncelleme özelliğini ve geçici kişisel APK imzasını bozmadan sıradaki ertelenmiş özellik olan video pinch-to-zoom geliştirildi.
- `VideoPlayerScreen` mevcut Media3 `TextureView` yüzeyini ve ilk kare küçük resmini **aynı grafik katmanında** 1–4× yakınlaştırır; genişletilmiş videoda tek parmakla kaydırır. Dikey `Crop` / yatay `Fit` görüntü düzeni ve cam Motion Glass kontrollere dokunulmadı. Çift dokunma ±10 sn, tek dokunma kontroller, normal ölçekte aşağı çekme kapat özelliği korunur. İki parmak sonrası kalan tek parmak yanlışlıkla kapatma başlatmaz.
- `VideoZoomMath` ekran ve gerçek video yüzeyinin boyutlarına göre parmak merkezi korunarak dönüşüm ve pan sınırlarını hesaplar; sınırlı/bozuk sayılarda ekrana NaN gitmez. `VideoZoomMathTest` ile genişletme, kaydırma, Crop/Fit ve sıfırlama testleri.
- Yeni kütüphane, ağ izni, model/üçüncü taraf SDK veya video düzenleme/kodlama algoritması eklenmedi. Var olan GitHub APK güncellemesi korunuyor; Play AAB kendini güncellemiyor.
- `versionCode 27`, `versionName 0.9.5`. CI yeşil + APK + kaynak ZIP + SHA-256 sonrası PR ana dala alınabilir. Gerçek telefonda Media3 TextureView yakınlaştırma/gestures/Android 16 ve ekran döndürme testi gerekir.
- Açık `PR #21` (Termux özel imza) mevcut `main` ile çatışıyor ve CI başarısız: **birleştirilmedi**. Özel imza değiştirmek kurulu 0.9.4 üstüne yüklemeyi engelleyebileceği için kullanıcı verisi yedeklenmeden zorla uygulanmayacak.
- Sonraki ertelenenler: Samsung hareketli fotoğrafların video kısmı, ızgaradan açılma animasyonu. Google Play geçişinde kişisel APK'nın kendini güncelleme akışı kaldırılacak.

## Aşama 9d — Samsung/Google Motion Photo gömülü MP4 oynatma (0.9.6)

- Yalnızca açık olan JPEG/HEIC fotoğraf, asenkron olarak gömülü `ftyp` tabanlı MP4 bakımından taranır (`MotionPhotoLogic`, `MotionPhotoReader`). Tam 108 MP fotoğraf belleğe yüklenmez: 64 KiB'lık pencerelerle son 128 MiB taranır, kutu başlıkları doğrulanır ve `moov`+`mdat` aranır. Samsung `MotionPhoto_Data` işaretçisi eksik olsa da MP4 başlangıcı bulunabilir; SEF trailer MP4 dosyasına alınmaz. Bu, sınırsız tüm Samsung HEIC/AVIF varyantlarının çalışacağını garanti etmez.
- Fotoğraf içinde gömülü video bulunduğunda mevcut Oynat düğmesi görünür. Kullanıcı bastığında yalnızca MP4 bölümü uygulamanın özel cache klasörüne geçici kopyalanır (yeniden kodlama yok, orijinal dosya değiştirilmez). Var olan ExoPlayer/TextureView, film şeridi, PiP, iki parmak video yakınlaştırma, cam kontroller tekrar kullanılır. Kamera sesleri kullanıcının var olan Ses düğmesiyle etkinleştirilebilir.
- Gömülü MP4 bozuksa veya erişilemiyorsa fotoğraf normal statik görüntülenir; orijinal korunur. Ekran kaydırılırsa tamamlanan eski video otomatik açılmaz. `MotionPhotoLogicTest` işaretçili, işaretçisiz, SEF trailer ve bozuk veri örneklerini test eder.
- Versiyon `0.9.6`, `versionCode 28`. Yeni kütüphane veya ağ izni yok; Play AAB sınırlamaları korunuyor. Gerçek Samsung Galaxy S24/S25/Fold Motion Photo örnekleri ile uçtan uca test ayrıca gereklidir.

## Aşama 9e — Izgaradaki fotoğrafın yerinden büyüyerek açılması (0.9.7)

- `LibraryGrid` yalnızca Kütüphane Tümü görünümünde dokunulan medya hücresinin `boundsInRoot()` konumunu tıklama anında iletir. Seçme/uzun basma, kaydırma ve sütun değiştirme işlemleri korunur; Kopyalar ve koleksiyonların `LibraryGrid` çağrılarına isteğe bağlı argüman gerekmez.
- `LibraryScreen` tam ekran `ViewerScreen` katmanını dokunulan karenin merkezine ve boyutlarına göre `Animatable`/var olan `GalleryMotion.standard()` yay animasyonuyla büyütür; cam kontrollerin özellikleri değiştirilmez. Kaynak görünür değilse, bozuk/çok küçük ölçü gelirse sade açılış kullanılır. Kapanışta mevcut ızgaraya geri dönme/son görülen öğeyi ortaya alma korunur.
- Geometri `GalleryEntranceMath` saf Kotlin, testler `GalleryEntranceMathTest` (merkez, kaynak konumu, NaN, aşırı ölçüler).
- Yeni kitaplık/izin yok; cihaz içi fotoğraf düzenleme, Güncelleme, Samsung Motion Photo ve videoların 4× yakınlaştırma kodu korunuyor.
- Sürüm `0.9.7` / versionCode 29. CI yeşil + kaynak ZIP/digest/APK sonrası ana dala birleştirme. Gerçek cihazda kaydırırken hücreye basma, hızlı kapatma, yön değişimi ve düşük RAM test edilmeli.
