# Gallerypro

iOS 26 Fotoğraflar hissini (cam / Liquid Glass) Android'e taşıyan, internetsiz çalışan galeri uygulaması.

- Tasarım tuvali: https://claude.ai/artifact/J9L59HCcDDZVwuod8fYSiw
- Proje dokümanı: https://claude.ai/artifact/7aviV7rE1kBvm4eBtUtzm9

## Durum

| Aşama | İçerik | Durum |
| --- | --- | --- |
| 1 · Temel | İskelet, GitHub Actions, izinler, MediaStore, Kütüphane ızgarası | bu dal |
| 2 · Cam ve görüntüleyici | Fotoğraf görüntüleyici, Bilgi paneli | sırada |
| 3 · Video | Media3 oynatıcı, film şeridi | |
| 4 · Koleksiyonlar ve Düzenle | | |
| 5 · Yapay zekâ | | |
| 6 · Cila ve yayın | | |

## APK nasıl alınır

Her push'ta GitHub Actions APK üretir: **Actions › son çalışma › Artifacts › gallerypro-apk**.

## Teknoloji

Kotlin, Jetpack Compose, AGP 9.3.2, Kotlin 2.4.10, cam efekti için [Kyant Backdrop](https://github.com/Kyant0/AndroidLiquidGlass), küçük resimler için Coil 3. Tüm sürümler `gradle/libs.versions.toml` içinde.

Yazı tipi: Inter (SIL Open Font License 1.1, `INTER_FONT_LICENSE.txt`).
