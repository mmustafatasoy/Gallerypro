# Gallerypro

iOS 26 Fotoğraflar hissini (cam / Liquid Glass) Android'e taşıyan, internetsiz çalışan galeri uygulaması.

- Tasarım tuvali: https://claude.ai/artifact/J9L59HCcDDZVwuod8fYSiw
- Proje dokümanı: https://claude.ai/artifact/7aviV7rE1kBvm4eBtUtzm9

## Durum

| Aşama | İçerik | Durum |
| --- | --- | --- |
| 1 · Temel | İskelet, izinler, MediaStore, cam bileşenleri, Kütüphane ekranı | tamam |
| 2 · Cam ve görüntüleyici | Fotoğraf görüntüleyici, Bilgi paneli | tamam |
| 3 · Video | Media3 oynatıcı, film şeridi | tamam |
| 4 · Koleksiyonlar ve Düzenle | | sırada |
| 5 · Yapay zekâ | | |
| 6 · Cila ve yayın | | |

## APK nasıl alınır (Bitbucket Pipelines)

1. Sol menü › **Pipelines** › **Run pipeline**
2. Dal: `ana` (veya çalıştığın dal), Pipeline: **custom: apk** › **Run**
3. Bitince adıma tıkla › **Artifacts** › `app-debug.apk` indir

Ücretsiz planda ayda 50 dakika derleme süresi var; önbellek doluyken bir derleme yaklaşık 4–6 dakika.

## Yerelde derleme (Android Studio / terminal)

Inter yazı tipi depoda durmaz, derlemeden önce bir kez indirilir:

```bash
bash scripts/fetch-fonts.sh   # curl, python3 ve fonttools gerekir
gradle assembleDebug          # veya Android Studio ile aç
```

Gradle sürümü `gradle/wrapper/gradle-wrapper.properties` içindedir (9.7.1).

## Teknoloji

Kotlin, Jetpack Compose, AGP 9.3.2, Kotlin 2.4.10, cam efekti için [Kyant Backdrop](https://github.com/Kyant0/AndroidLiquidGlass), küçük resimler için Coil 3. Tüm sürümler `gradle/libs.versions.toml` içinde.

Yazı tipi: Inter (SIL Open Font License 1.1). Lisans metni derlemede `app/src/main/assets/licenses/inter-OFL.txt` olarak uygulamaya eklenir.

## Geçmiş

1. aşama önce GitHub'da (`mmustafatasoy/Gallerypro`) yapıldı; orada derleme yeşildi ve emülatör ekran görüntüleriyle kontrol edildi. GitHub hesabı askıya alınınca proje buraya taşındı.
