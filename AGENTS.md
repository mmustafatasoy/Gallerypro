# Gallerypro — kodu devralan yapay zekâ için (Codex, ChatGPT, Claude…)

**Başlamadan önce `DEVAM-NOTU.md`'yi baştan sona oku.** Tek doğru kaynak odur: şu anki durum, sıradaki iş,
kurallar, bilinen tuzaklar ve proje dokümanı + tasarım tuvali bağlantıları orada. Plan dışına çıkma; emin
değilsen kullanıcıya sor (iki seçenek sun, kararı bekle).

## Kısa kurallar
- Kotlin + Jetpack Compose **foundation** (Material3 yok). minSdk 29, targetSdk 36, compileSdk 37.
- Sürümler yalnızca `gradle/libs.versions.toml`; yükseltme yok, alfa/beta yok. Yeni kütüphane için önce sor.
- **INTERNET izni yok** (manifestte `tools:node="remove"` ile kesin kapalı). Hiçbir veri cihazdan çıkmaz.
- Cam yalnızca `designsystem/Glass.kt` → `Modifier.glass(...)`. Renk/boyut/hareket yalnızca `designsystem/Tokens.kt`.
- Ekrandaki her metin `res/values*/strings.xml` (Türkçe) ve `res/values-en/` (İngilizce) içinde; kodda sabit metin yok.
- Animasyonlu `Dp`/`Float` değerleri `padding`/`size`/`Color.copy(alpha)`'ya verilmeden önce sınırlanır
  (`coerceAtLeast(0.dp)`, `coerceIn(0f, 1f)`); yay animasyonları hedefi aşar ve uygulamayı kapatır.
- Her iş ayrı dalda (`asama-…`), PR ile `main`'e girer; kullanıcı birleştirir. Commit mesajları Türkçe.
- GitHub Actions (`.github/workflows/apk.yml`) yeşil olmadan "bitti" deme.
- İşin sonunda `DEVAM-NOTU.md`'yi güncelle (durum tablosu, yapılanlar, ertelenenler).

## Derleme
- GitHub Actions her gönderimde test + hızlı (release, R8) APK üretir; APK Releases'a yüklenir
  (`main` → `son-surum`). Yerelde: JDK 21, Gradle 9.7.1, `bash scripts/fetch-fonts.sh`,
  `gradle testReleaseUnitTest assembleRelease`.
- Saf mantık testleri: `app/src/test/` (CollectionsLogic, EditLogic, SearchEngine).

## Hata ayıklama
- Uygulama çökerse bir sonraki açılışta hata raporu gösterilir; kullanıcı "Paylaş" ile gönderir.
  Yığın izinde adı geçen satırı düzelt; tahminle başka dosyalara dokunma. R8 adları korur (`-dontobfuscate`).
