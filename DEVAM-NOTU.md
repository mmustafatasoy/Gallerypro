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
