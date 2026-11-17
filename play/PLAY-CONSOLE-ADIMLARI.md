# GalleryPro — Google Play Console 1.0.0 yayın dosyası (17 Kasım 2026)

> Teknik hazırlık belgesidir. Uygulama **henüz Google Play'de yayınlanmış değildir**.
> Onaylanmamış ticari SFace model hakları ve gerçek cihaz testleri nedeniyle üretim yayını açma.

## Doğrulananlar

- Uygulama adı: **GalleryPro**
- Android applicationId: `com.atasoy.gallerypro`
- Versiyon: `1.0.0`, versionCode: `32`; minSdk 29, targetSdk 36
- Google Play geliştirici kimlik doğrulaması: **17.11.2026 04:05 tarihli Google Play Console e-postasıyla başarılı** (e-posta hesap sahibine ait; kişisel adresi burada paylaşmayın)
- Eski 0.9.9 gerçek upload-key AAB: GitHub Release `play-upload-v0.9.9-4`.
- Yeni 1.0.0 gerçek upload-key **QA AAB**: Play AAB GitHub Actions içinde oluşturuldu; sertifikası 0.9.9 ile **SHA-256 olarak aynı**. Güncel signed QA ve APK kontrolü başarılı olmadan sürüm kararlı ilan edilmez.
- Kamera/Fotoğraf/Video izinleri: release birleşik manifest denetimi var. `INTERNET` / `REQUEST_INSTALL_PACKAGES` yok.
- 16 KB: 64-bit .so ELF kontrolü Play AAB ve APK iş akışlarında.
- Play 512 × 512 simge: onaylı kaynak `play/simge/gallerypro-play-simge-512.png` (24-bit), Google Play'e yüklemek için **32-bit RGBA** çıktı `play/generated/GalleryPro-play-icon-512-RGBA.png` (piksel içeriği değişmeden yalnızca alpha kanalı eklenir).
- Feature graphic: `play/generated/GalleryPro-feature-1024x500.png` dosyası **Play Store Assets** iş akışında onaylı simgeden **24-bit RGB** üretilir; Play Console'da iki dosya farklı alanlara yüklenir.
- TR ve EN mağaza başlıkları/açıklamaları: `play/store-listing.md`
- Fotoğraf/video izin beyanı taslağı: `play/PHOTO-VIDEO-IZIN-BEYANI.md`

## Play Console'da yapılacaklar

1. [Google Play Console](https://play.google.com/console/) içinde GalleryPro uygulamasını oluştur/varsa aç. **Paket adını değiştirmeden** önce hesabında önceden oluşturulan uygulama olup olmadığını kontrol et. `com.atasoy.gallerypro` başka hesapta kayıtlı görünürse sahiplik/hesap sorununu çözmeden yeni paket adı uydurma.
2. Uygulama adı **GalleryPro**, kategori **Fotoğrafçılık** (Photo & Video / Photography; Console karşılığına göre), iletişim e-postası **destek@atasoyyazilim.com**.
3. Mağaza ana listesi: `play/store-listing.md` Türkçe ve İngilizce metinlerini, 512 PNG simgeyi ve 1024 × 500 tanıtım görselini yükle. En az iki **gerçek uygulama** telefon ekran görüntüsü ekle; `play/GERCEK-EKRAN-GORUNTULERI.md`.
4. Gizlilik politikası: `https://atasoyyazilim.com/gallerypro-gizlilik.html` ancak **site PR #36 canlıya çıktığında ve URL anonim HTTPS'te açıldığında** yazılabilir. Uygulama içi gizlilik bağlantısının da görünürlüğü ayrıca doğrulanmalı.
5. İçerik: uygulama erişimi (giriş hesabı istenmiyor), reklam (kodda reklam izni/SDK görünmüyor; son paketle doğrula), hedef kitle, içerik derecelendirme ve Veri Güvenliği formunu **uygulamanın gerçek davranışına göre** doldur.
6. `READ_MEDIA_IMAGES` ve `READ_MEDIA_VIDEO` için fotoğraf/video izin beyanında galerinin tüm cihaz arşivi/album yönetimi/yerel arama temel işlevini açıkla. İsteğe bağlı medya, konum ve biyometri izinlerini gerçeğe uygun bildir.
7. Play App Signing etkinleştir; önceki 0.9.9 AAB sertifikasıyla aynı **upload certificate** kullanıldığı CI'da kontrol edildi. Play Console sertifikasını da karşılaştır. Test/debug anahtarlı AAB'yi kesinlikle yükleme.
8. SFace FP32 ONNX **ticari kullanım ve eğitim veri kaynağı belirsiz**. Bu netleşmeden **herkese açık üretim yayınına geçme**. OpenCV Zoo `face_recognition_sface` dizini Apache-2.0 ibaresi taşıyor, fakat eğitim veri yetkisi ayrı bir soru.
9. Uygulamanın gerçek cihazda video kırpma, yüz tanıma, EXIF, gizli albüm, Samsung Motion Photo ve 16 KB destekli cihazda açılması test edilmeden herkese açık yayın yapılmamalı.
10. Gerekli bütün kontroller tamamlanınca son `main` dalında **Actions > Play AAB > Run workflow > publish=true** ile gerçek upload-key AAB üret. AAB yalnızca GitHub Release'e yüklenir; Play Console'a elle yüklenir.
11. 13 Kasım 2023 sonrası açılan uygun kişisel hesaplarda **12 test kullanıcısı / 14 gün kesintisiz opt-in** gerekliliği bulunabilir. Hesabındaki Play Console yayın erişim şartını esas al.

## Güvenli anahtar kullanımı

**Yeni keystore üretme.** 13 Kasım 2026 gerçek imza Secret'ları mevcut ve yeni 1.0.0 test AAB'si bunlarla başarıyla imzalandı. `RELEASE_KEYSTORE_B64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` değerlerini sohbet, sorun kaydı, Git deposu veya ekran görüntüsü ile paylaşma. Anahtar kaynağının cihazda/şifreli offline yedekte korunması önemlidir. Secret'ları kaybedersen aynı özel anahtarı sadece public sertifikadan geri getiremezsin.

Eski sabit deneme anahtarıyla imzalı yerel GalleryPro APK'sını yeni Play sürümüyle doğrudan güncellemek mümkün olmayabilir; kullanıcı verisi yedeği alınmadan eski uygulamayı kaldırma.

## Bağlantılar

- Play signed QA: https://github.com/mmustafatasoy/Gallerypro/actions/runs/44057117430
- 0.9.9 signed upload AAB: https://github.com/mmustafatasoy/Gallerypro/releases/tag/play-upload-v0.9.9-4
- Yayın hazırlığı: https://github.com/mmustafatasoy/Gallerypro/pull/31
- Gizlilik sitesi: https://github.com/mmustafatasoy/atasoyyazilim-web/pull/36
- Resmî Play App Signing: https://developer.android.com/studio/publish/app-signing
- Resmî kapalı test: https://support.google.com/googleplay/android-developer/answer/14151465
