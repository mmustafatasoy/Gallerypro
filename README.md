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
| 4 · Koleksiyonlar ve Düzenle | Koleksiyonlar sekmesi, Gizli/Son Silinenler, Düzenle ekranı | tamam (0.4.1: kararlılık + çökme raporu) |
| 5 · Yapay zekâ | Cihaz içi dizin (nesne + yazı) ve arama ekranı (0.5.0); sırada cümleyle arama, Kişiler | devam ediyor |
| 6 · Cila ve yayın | | |

## APK nasıl alınır (GitHub Actions)

`main` dalına ve `asama-…` dallarına her gönderimde GitHub APK'yı kendisi derler (depo herkese açık olduğu için ücretsiz, dakika sınırı yok).

- **Son sürüm, doğrudan indirme:** https://github.com/mmustafatasoy/Gallerypro/releases/download/son-surum/Gallerypro.apk
- Bir aşama dalının APK'sı: **Releases** sayfasında dalın adıyla.
- Derleme durumu: depoda **Actions** sekmesi. Elle başlatmak için Actions › APK › Run workflow.

APK hızlı (release, R8) türdendir ve sabit bir deneme anahtarıyla imzalanır; yeni sürüm eskisinin üstüne kurulur.

Kodu başka bir yapay zekâyla sürdürmek için: `AGENTS.md` (kısa kurallar) ve `DEVAM-NOTU.md` (durum ve plan).

## Yerelde derleme (Android Studio / terminal)

Inter yazı tipi depoda durmaz, derlemeden önce bir kez indirilir:

```bash
bash scripts/fetch-fonts.sh   # curl, python3 ve fonttools gerekir
gradle assembleDebug          # veya Android Studio ile aç
```

Gradle sürümü `gradle/wrapper/gradle-wrapper.properties` içindedir (9.7.1).

## Google Play hazırlığı (0.9.2)

`.github/workflows/aab.yml` için `asama-play-*` aşama dalında otomatik **TEST AAB** üretilir; Play Console'a yüklemeyin. Gerçek imzalı yükleme AAB'si yalnızca `main` üzerinde, GitHub Actions → Play AAB → Run workflow → `publish=true` ve 4 yayın anahtarı Secret'ı mevcutsa oluşturulur. Bu işlem Play Console'a kendiliğinden yayımlamaz.

Mağaza metinleri, gizlilik politikası ve yayımlama kontrol listesi `play/` klasöründedir. SFace yüz modelinin ticari dağıtım hakları ve mağaza görselleri tamamlanmadan genel yayın yapılmamalıdır.

## Uygulama içi güncelleme ve özel Termux imzası (0.9.4)

GitHub doğrudan APK artık `direct` kanalındadır: internet yalnızca yeni sürümü denetlemek ve APK indirmek için kullanılır. Uygulama açılınca güncellemeleri kontrol eder; Ayarlar > Uygulama Güncellemesi ile elle de bakılabilir. Android kurulum onayı zorunludur, sessiz kurulum yapılmaz.

Play Store için `release` AAB internetsiz ve APK yükleme izni olmadan kalır. Play güncellemeleri Play Store'a aittir.

Termux: `pkg install openjdk-17 coreutils`; ardından kaynak depoda `bash scripts/termux/gallerypro-imza-olustur.sh`. Kullanıcının oluşturduğu şifre ve keystore sadece GitHub Actions Secrets'ta saklanır: RELEASE_KEYSTORE_B64, KEYSTORE_PASSWORD, KEY_ALIAS=gallerypro, KEY_PASSWORD.

**ÖNEMLİ:** 0.9.3 ve önceki APK'lar ortak test anahtarıyla imzalanmıştır. Yeni özel anahtar farklıysa Android eski kurulumu yerinde güncellemez. Yedek almadan eski uygulamayı silme.

## Teknoloji

Kotlin, Jetpack Compose, AGP 9.3.2, Kotlin 2.4.10, cam efekti için [Kyant Backdrop](https://github.com/Kyant0/AndroidLiquidGlass), küçük resimler için Coil 3. Tüm sürümler `gradle/libs.versions.toml` içinde.

Cümleyle arama: OpenAI CLIP ViT-B/32 (MIT) ve `sentence-transformers/clip-ViT-B-32-multilingual-v1` (Apache-2.0), LiteRT ile telefonda, internetsiz. Modeller depoda değildir; `Modeller` iş akışı (`scripts/models/export_clip.py`) çevirip Releases'taki `modeller` sürümüne koyar, APK derlemesi oradan indirir. Lisans bildirimi: `scripts/models/NOTICE.txt` (APK'da `assets/licenses/clip-models.txt`).

Yazı tipi: Inter (SIL Open Font License 1.1). Lisans metni derlemede `app/src/main/assets/licenses/inter-OFL.txt` olarak uygulamaya eklenir.

## Geçmiş

1. aşama GitHub'da yapıldı. Hesap bir süre askıya alınınca 2.–5. aşamalar Bitbucket'ta (`mmaapps/mmustafatasoy`) sürdü; hesap açılınca tüm geçmiş ve dallarla buraya geri taşındı (9 Kasım 2026).
