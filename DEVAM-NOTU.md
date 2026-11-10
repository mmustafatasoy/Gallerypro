# Devam notu (devralan yapay zekâ için) — 10 Kasım 2026, 0.8.1

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
- 0.8.1 Silme: her zaman uygulamanın cam onay kartı (`DeleteCard`); Android'in ek penceresi `ManageMedia` izniyle kalkar.
- 0.8.0 Ayarlar (Kütüphane ⋯): sürüm, Yeniden incele, tam cam efektleri anahtarı (`GalleryPerformance`), gizlilik,
  lisanslar; silinen fotoğrafların düzenleme yedekleri temizlenir. Play: `.github/workflows/aab.yml` (elle),
  `:modeller` kurulum-anı asset pack (yalnızca `-PaabBuild=true`), yayın anahtarı Secrets'tan.

**Kalan (kullanıcı kararı bekliyor):** görev 6 Kişiler (yüz modeli lisansı), görev 8'in kararları (uygulama adı, ikon,
gerçek yayın anahtarı → Secrets: RELEASE_KEYSTORE_B64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD). AAB iş akışı
henüz hiç çalıştırılmadı; ilk çalıştırmada hata verirse yalnızca hata veren satır düzeltilir.

## KOD YAPISI (değişmez mimari — devralan araç buna birebir uyar)

Bu bölüm Claude'un kurduğu yapıdır. **Yeni katman, yeni mimari, yeni kütüphane, yeniden adlandırma, dosya taşıma YOK.**
Yeni özellik aşağıdaki kalıplardan birine oturtulur. Oturmuyorsa durulur ve kullanıcıya sorulur (iki seçenek, karar beklenir).

### Katmanlar (paket: `com.atasoy.gallerypro`)
| Klasör | Ne var | Kural |
| --- | --- | --- |
| `GalleryApp.kt` | `AppContainer`: tüm tekil nesneler (repository, store'lar, `index`, `clipText`) | DI kütüphanesi (Hilt/Koin) **eklenmez**; yeni tekil nesne buraya `val` olarak eklenir |
| `GalleryRoot.kt` | İzin kapısı, sekmeler (Kütüphane/Koleksiyonlar), arama katmanı, çökme raporu, ViewModel'lerin `viewModel { }` ile kurulumu | Navigation kütüphanesi **yok**; ekran açma = durum değişkeni + `AnimatedVisibility` |
| `data/` | Saf veri ve iş mantığı. Android'e dokunan sınıflar (Store, Source, Worker) + **saf Kotlin** `object ...Logic` | Arayüz kodu (Composable) `data/`'ya girmez |
| `data/media/` | `MediaItem` (tek medya modeli), `MediaStoreSource` (sorgu), `MediaRepository` (StateFlow: `items`, `hiddenItems`, `trashed`), `MediaDetails` (EXIF), `ManageMedia`, küçük depolar (`CaptionStore`, `HiddenStore`) | Medya listesi yalnızca `MediaRepository`'den okunur |
| `data/index/` | `IndexStore` (SQLite `index.db`, tablolar: `media`, `vectors`, `places`; hepsi bellekte StateFlow), `IndexWorker` (WorkManager, tek dizinleyici), `SearchEngine`/`SearchText`/`LabelDictionary` (saf) | Room/KSP **yok**. Şema değişince `Helper` sürümü +1 ve **veriyi koruyan** `onUpgrade` |
| `data/clip/` | CLIP: `ClipEncoders` (LiteRT), `WordPieceTokenizer`, `VectorMath` (saf) | Model/çalıştırıcı değişmez; ikinci vektör katmanı açılmaz |
| `data/places/`, `data/memories/`, `data/collections/`, `data/edit/` | Yerler (`CityIndex`, `PlacesLogic`), Anılar (`MemoryBuilder`), Koleksiyonlar (`CollectionsLogic`, `SectionOrder`), Düzenleme (`EditModel`, `EditRenderer`, `EditSaver`, `EditStore`) | Yeni hesap mantığı = yeni saf `object` + test |
| `designsystem/` | `Tokens.kt` (renk/boyut/hareket), `Type.kt` (yazı), `Glass.kt` (`Modifier.glass` — TEK cam noktası), `GlassControls.kt` (cam düğme, sekme çubuğu, menü, bildirim), `ProgressiveHeader.kt`, `Performance.kt`, `icons/GalleryIcons.kt` | Yeni renk/boyut önce `Tokens.kt`'ye; ekranda doğrudan blur/renk kodu yazılmaz |
| `feature/<ekran>/` | `XScreen.kt` (Composable) + gerekirse `XViewModel.kt` | Bir ekran = bir klasör. Ekranlar birbirinin içini bilmez; ortak parça `internal` yapılıp aynı yerden kullanılır (örn. `DeleteCard`, `AlbumTile`) |

### NEREDEN NE ALINIR (kaynak tablosu — aynı işi yapan ikinci kod yazılmaz)
Bir şeye ihtiyaç olunca **önce bu tabloya bakılır**. Burada olan bir şey yeniden yazılmaz, kopyalanmaz, sarmalanmaz.
Tabloda olmayan bir veri gerekiyorsa: ilgili `data/` sınıfına eklenir ve bu tabloya bir satır yazılır.

**Medya**
| İhtiyaç | Nereden | Not |
| --- | --- | --- |
| Tüm fotoğraf/videolar (gizliler hariç, eskiden yeniye) | `container.mediaRepository.items` (`StateFlow<List<MediaItem>?>`) | `null` = yükleniyor |
| Gizlenenler / Son Silinenler | `mediaRepository.hiddenItems` / `mediaRepository.trashed` | Çöp yalnızca Android 11+ |
| Listeyi yenile | `mediaRepository.refresh()` | MediaStore değişince kendiliğinden yenilenir |
| Bir öğenin adresi (Uri) | `item.contentUri()` (`data/media/MediaItem.kt`) | Elle `ContentUris` yazılmaz |
| Döndürülmüş genişlik/yükseklik | `item.displayWidth` / `item.displayHeight` | |
| Ekran görüntüsü mü, kamera mı, panorama mı… | `MediaKinds.isScreenshot/isCamera/isPanorama/isRaw/isImported(item)` (`data/collections/CollectionsLogic.kt`) | |
| EXIF (kamera, lens, ISO, konum) | `MediaDetailsReader.read(context, item)` — **IO iş parçacığında** | Konum izni: `MediaDetailsReader.hasLocationPermission(context)` |
| Medya izni durumu | `MediaPermissions.access(context)` → `MediaAccess.Full/Partial/None` | İstenecek izinler: `MediaPermissions.required()` |
| "Medya yönetimi" (sistem onayı olmadan sil/favori) | `ManageMedia.granted(context)`, `ManageMedia.request(context)` | |
| Çöpe at / geri al / kalıcı sil / favori | `MediaStore.createTrashRequest` / `createDeleteRequest` / `createFavoriteRequest` + `StartIntentSenderForResult` | Örnek: `ViewerScreen.launchTrash`, `recover`, `deleteForever`, `toggleFavorite`. Önce **`DeleteCard`** ile onay |
| Paylaş, birlikte aç, duvar kâğıdı yap, panoya kopyala, haritada aç | `context.shareItems(...)`, `openWith`, `setAs`, `copyToClipboard`, `openMap` (`data/media/MediaIntents.kt`) | |
| Küçük resim | `AsyncImage(model = MediaThumb(uri, boyutPx, item.modifiedMillis), imageLoader = LocalGalleryImageLoader.current)` | Başka resim yükleyici yok; `modifiedMillis` mutlaka verilir |
| Gün/ay/yıl gruplama | `PeriodGrouper` (`data/media/Periods.kt`), `CollectionsLogic.epochDay(...)` | |
| Sayı, süre, tarih yazısı | `Formatters.count(n, locale)`, `Formatters.duration(ms)`, `Formatters.longDate(...)`, `Formatters.monthName(...)` | Elle `String.format` yazılmaz |
| EXIF yazısı (ƒ1,8, 26 mm, MP, boyut, koordinat) | `DetailFormat.*` (`data/media/MediaDetails.kt`) | |

**Kullanıcı verisi (cihazda saklanan)**
| İhtiyaç | Nereden |
| --- | --- |
| Fotoğraf başlığı | `stores.captions.caption(id)`, `.set(id, text)`, akış: `.captions` |
| Gizle / göster | `stores.hidden.hide(ids)`, `.unhide(ids)`, akış: `.ids` |
| Düzenlenmiş mi, düzenleme ayarları, orijinal yedek | `stores.edits.editedIds`, `.params(id)`, `.backupFile(id)`, `.hasBackup(id)` |
| Son aramalar | `container.searchHistory` |
| Koleksiyon bölüm sırası | `container.sections` (`SectionPrefs`) |
| Cam efekti seçimi | `GalleryPerformance.fullEffects`, `.setFullEffects(context, açık)` |
| Ekranda bu depolara erişim | `LocalGalleryStores.current` (`captions`, `edits`, `hidden`, `index`) |

**Yapay zekâ dizini (`container.index` = `IndexStore`)**
| İhtiyaç | Nereden | Not |
| --- | --- | --- |
| Etiketler ve fotoğraftaki yazı | `index.entries` → `Map<Long, IndexEntry>` | Türkçe ad: `LabelDictionary.display(label, turkishUi)` |
| CLIP anlam vektörü | `index.vectors` → `Map<Long, VectorEntry>` | Benzerlik: `VectorMath.dot`, `.nearest` (metin→foto), `.similar` (foto→foto), `.mean` |
| Çekildiği yer | `index.places` → `Map<Long, PlaceEntry>` | Ad: `PlaceNames.cityLine/groupTitle/country`; grup: `PlacesLogic.groups/itemsIn` |
| Dizin durumu (ilerleme) | `index.status` | |
| Dizinlemeyi başlat | `IndexScheduler.runNow(context)` | Zamanlama: `IndexScheduler.schedule(context)` (yalnızca GalleryRoot) |
| Metinle arama | `SearchEngine.prepare(...)` + `SearchEngine.search(...)`; cümle: `container.clipText.embed(text)` | Ekran: `SearchViewModel` |
| Arama ekranını bir sözcükle aç | `LocalSearchLauncher.current?.open("kelime")` | |
| Anılar | `MemoryBuilder.build(items, places, vectors, now, zone)` | Ekran: `CollectionsViewModel.memories` |
| Koleksiyon içerikleri | `CollectionsLogic.build(...)`, `.itemsFor(key, ...)`, anahtarlar `CollectionKeys` | |

**Arayüz parçaları (yeniden çizilmez, bunlar kullanılır)**
| İhtiyaç | Nereden (`designsystem/` veya ilgili ekran) |
| --- | --- |
| Renk, boyut, köşe, hareket | `GalleryColors`, `GalleryDimens`, `GalleryMotion` (`Tokens.kt`) |
| Yazı stilleri | `GalleryType.LargeTitle/SectionTitle/Body/Subtitle/Button/Chip/Duration…` (`Type.kt`) |
| İkonlar | `GalleryIcons.*` + `Glyph(icon, renk, size)` |
| Cam yüzey | `Modifier.glass(backdrop, RoundedRectangle(..), GlassTone.Light/Dark/Sheet)` |
| Cam düğme / yazılı düğme | `GlassIconButton`, `GlassTextButton` |
| Alt sekme çubuğu | `GlassTabBar` + `GlassTab` |
| ⋯ menüsü | `GlassMenu` + `GlassMenuItem` |
| Kısa bildirim | `GlassToast(backdrop, mesaj)` |
| Üst/alt kademeli bulanık kenar | `ProgressiveBlurEdge` |
| Silme/onay kartı | `DeleteCard` (`feature/viewer/ViewerScreen.kt`, `internal`) |
| Tam ekran görüntüleyici | `ViewerScreen(items, startId, onClose, mode)` — slayt modu içinde var |
| Koleksiyon ızgarası | `CollectionDetailScreen(viewModel, key, title, onBack)` |
| Yatay kapak kartı | `DayCard` (CollectionsScreen içinde; Anılar ve Yerler de bunu kullanır) |
| Albüm karesi | `AlbumTile` (`internal`) |
| Ayarlar satırı/grubu | `SettingsScreen.kt` içindeki `Group`, `RowItem`, `Switch` |

**Yasak kısa yollar:** `Toast.makeText`, `AlertDialog`, Material bileşenleri, `Glide/Picasso`, ikinci bir `SQLiteOpenHelper`,
yeni `Worker`, `GlobalScope`, ana iş parçacığında disk/bitmap, kodda sabit Türkçe/İngilizce metin, `INTERNET` izni.

### Veri akışı (her yeni özellik böyle)
1. Kayıt: `data/...Store` (SQLite ya da SharedPreferences) → `MutableStateFlow` → dışarıya `StateFlow`.
2. Hesap: saf `object ...Logic` / `...Builder` (JVM'de test edilir, Android sınıfı kullanmaz).
3. ViewModel: `combine(repository.items, store.x) { ... }.flowOn(Dispatchers.Default).stateIn(viewModelScope, WhileSubscribed(5_000), boş)`.
4. Ekran: `collectAsStateWithLifecycle()`. Ağır iş (disk, model, bitmap) **asla** ana iş parçacığında değil.
5. Arka plan işi yalnızca `IndexWorker` içinde; yeni bir Worker açılmaz, `IndexWorker.doWork`'e `needsX()` + `putX()` eklenir.

### Arayüz kuralları
- Compose **foundation** + `BasicText`. Material/Material3 **yok**.
- Cam: `Modifier.glass(backdrop, şekil, GlassTone.Light/Dark/Sheet, refraction = …)`. Bir ekranda aynı anda en çok 4 cam yüzey.
- Ekran açma/kapama: `AnimatedVisibility` + `BackHandler`. Onay = cam kart (`DeleteCard` örneği), sistem diyaloğu değil.
- Koleksiyonlar'a yeni bölüm: `SectionId`'ye ekle → `SectionOrder.Default`'ta yerini ver → `CollectionsScreen`'de `when` dalı → `sectionTitle` → TR/EN metin. Açılan liste: `CollectionKeys`/`...Logic.collectionKey()` + `CollectionsViewModel.items(key)`.
- Bilgi paneline yeni kart: `InfoPanel.kt`'de `Card { }` kalıbı, veri `ViewerScreen`'de `produceState` ile.
- Metin: `res/values/` (TR) + `res/values-en/` (EN), kesme işareti `\'` ile. Sayılı metin `plurals`.
- Animasyonlu Dp/Float: `coerceAtLeast(0.dp)` / `coerceIn(0f, 1f)`.

### Test ve derleme
- Her saf mantık için `app/src/test/java/com/atasoy/gallerypro/<Konu>LogicTest.kt`. Şu an 72 test.
- Derleme yalnızca GitHub Actions: `apk.yml` (her gönderim, APK → Releases), `models.yml` (CLIP), `aab.yml` (elle, Play).
  İndirilen dosyalar (yazı tipi, modeller, GeoNames) **depoya konmaz**.
- Sürüm: her PR'da `versionCode` +1, `versionName` artar (`app/build.gradle.kts`).

### Sıradaki küçük işler (bu sırayla, her biri ayrı dal + PR; tarif dışına çıkılmaz)
1. **Anı slayt gösterisi** (`asama-7a-slayt`): `CollectionDetailScreen` başlığına cam "Oynat" düğmesi → mevcut `ViewerScreen` slayt modu (`slideshow` değişkeni) o listeyle açılır. Yeni oynatıcı yazılmaz.
2. **Uzun basınca önizleme + Albüme ekle** (`asama-7b-onizleme`): `LibraryGrid` hücresinde `combinedClickable(onLongClick)` → büyük önizleme + cam menü (Paylaş, Favori, Albüme ekle, Gizle, Sil). Albüme ekle = `MediaStore` ile seçilen albüm klasörüne **kopya** (`IS_PENDING`), orijinal taşınmaz.
3. **Kopyaları birleştir** (`asama-7c-kopyalar`): Kopyalar ekranında grupla (`CollectionsLogic.duplicates` + CLIP ≥0,97), her grupta en büyük çözünürlük kalır, diğerleri `DeleteCard` onayıyla çöpe.
4. **Tarih/konum düzenle** (`asama-7d-ayarla`): Bilgi panelinde "Ayarla" → `ExifInterface` ile yazma, yalnızca JPEG; önce `EditStore` gibi `files/originals` yedeği; HEIC/video için düğme gizli.
5. **Video kırpma** (`asama-7e-video`): **Yeni kütüphane gerekir (Media3 Transformer)** → başlamadan kullanıcıya sor.
6. Kişiler (yüz tanıma) ve Play Store kararları: kullanıcıya sorulmadan başlanmaz.

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
