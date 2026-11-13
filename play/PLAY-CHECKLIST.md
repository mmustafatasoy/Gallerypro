# GalleryPro — Google Play yayına hazırlık kontrol listesi (10 Kasım 2026)

**Durum:** Teknik ön hazırlık. Bu dosya bir yayımlama/Play Console onayı değildir.

## Mevcut yapı
- Paket: `com.atasoy.gallerypro` — mevcut kullanıcı sürümleriyle tutarlı tutulur.
- Yeni geliştirme sürümü: `1.0.0`, versionCode `32`; henüz Play Console'da onaylanmadı. Daha önce oluşturulan `0.9.9` yükleme AAB'si bu sürümün yerine kullanılamaz.
- minSdk 29; targetSdk **36** — Google Play'in 31 Ağustos 2026 sonrası yeni uygulama hedef API düzeyiyle uyumlu.
- Geliştirme APK'sı: sabit **deneme/debug imzası**, yalnızca GitHub üzerinden cihazda test içindir.
- Play: `AAB` gerekir; gerçek **upload key** GitHub Secrets'da olmalı, Play App Signing ayrıca etkinleştirilir.
- `.github/workflows/aab.yml`: `asama-play-*` dalında otomatik **TEST** AAB ve paket içerik kontrolü; test AAB **Play'e yüklenmez**. `workflow_dispatch publish=true`, yalnızca `main` + 4 Secret varsa **gerçek imzalı upload AAB** üretir (GitHub Release olarak), Play Console'a yüklemez.
- AAB: CLIP kurulum-anı `modeller` asset pack; YuNet + SFace FP32 ve ilgili lisanslar `base/assets/face` ve `base/assets/licenses` içinde. İş akışı AAB ZIP'i açıp bu içerikleri doğrular.
- Kaynak ZIP + SHA-256: `apk.yml` ana ve aşama dallarında her başarılı APK sonrası otomatik.

## Yayından önce çözülmesi gerekenler
- [ ] **SFace FP32 model ağırlıklarının ticari dağıtım hakkı** ve eğitim verileri kaynağı yazılı olarak incelendi. OpenCV Zoo model dizini Apache-2.0 olarak işaretli olsa da köken/sublicensing konusunda açık sorular vardır. Hukuki teyit olmadan Play genel dağıtıma geçilmez.
- [ ] Uygulama adı **GalleryPro**, mağaza ikonu (hazır: `play/simge/gallerypro-play-simge-512.png`), öne çıkan grafik, gerçek cihaz ekran görüntüleri ve destek bilgileri kesinleştirildi.
- [ ] `play/privacy-policy.html` dosyası herkese açık HTTPS URL'ye (ör. kullanıcının kendi sitesine) yüklendi, URL anonim tarayıcıda açılıyor; metin son Android/AAB özellikleriyle karşılaştırıldı.
- [ ] Play Console **Data safety** formunda mevcut bağımlılıkların gerçek veri akışları doğrulandı. Uygulamanın yerel işlediği veriler otomatik olarak dışarı veri toplama sayılmaz; ama paket içindeki SDK'ler ve tüm sürümler denetlenmelidir.
- [ ] `READ_MEDIA_IMAGES` ve `READ_MEDIA_VIDEO` için Play Console **Fotoğraf ve Video İzin Beyanı**: galerinin temel işlevi tüm kitaplığa erişim, arama ve albüm yönetimidir; sistem fotoğraf seçicisi sürekli galeri işlevi için yeterli değildir. Son karar Google Play incelemesine aittir.
- [ ] İsteğe bağlı `MANAGE_MEDIA`, `ACCESS_MEDIA_LOCATION`, `USE_BIOMETRIC` izinleri Play Console bildirimleri ve gerçek kullanım açısından tekrar denetlendi.
- [ ] Google Play geliştirici hesabı ve geliştirici doğrulaması tamamlandı. Yeni kişisel hesap 13 Kasım 2023 sonrası açıldıysa **en az 12 test kullanıcısı 14 gün kesintisiz kapalı test** şartı uygulanabilir.
- [ ] **Upload key** sahibi tarafından güvenli şekilde oluşturuldu; şifreler ve .jks dosyası Git deposuna konmadı. `RELEASE_KEYSTORE_B64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` GitHub Secrets olarak saklandı. Anahtar ayrıca kullanıcının güvenli çevrimdışı kasasında yedeklendi.
- [ ] Play App Signing / upload key sertifikası uyumu Play Console'da doğrulandı; önceki GitHub debug-imzalı uygulamanın Play sürümüyle yerinde güncellenemeyebileceği test kullanıcılarına bildirildi.
- [ ] `Play AAB` iş akışının **TEST** modu ve AAB paket içeriği yeşil. `publish=true` ancak tüm yukarıdaki maddeler tamamlandığında çalıştırılır.
- [ ] Samsung Motion Photo, video yakınlaştırma, ızgaradan açılış animasyonu, çevrimdışı yüz tarama, Android 16 kurulum/güncelleme ve 108/200 MP fotoğraflar gerçek cihazlarda doğrulandı.
- [ ] Play Console **internal testing / closed testing** üzerinde uygulama açılma, video kırpma, yüz indeksleme, EXIF yazma, gizli albüm ve kaldırma/yeniden kurma davranışları gerçek cihazlarda incelendi.
- [ ] Play Console uygulama erişim, reklam, hedef kitle, içerik derecelendirme, veri güvenliği ve varsa finansal/biometrik sorular doğru dolduruldu.
- [ ] İmzalı AAB'den üretilen Play split APK'larında **SFace FP32** ve `modeller` kurulum-anı varlıkları erişilebilir; `PeopleScanner` ile gerçek tarama başarılı.

## Resmî bağlantılar
- Hedef API: https://support.google.com/googleplay/android-developer/answer/11926878
- Play App Signing: https://support.google.com/googleplay/android-developer/answer/9842756
- Fotoğraf/video izinleri: https://support.google.com/googleplay/android-developer/answer/18265346
- Data safety: https://support.google.com/googleplay/android-developer/answer/10144311
- Kapalı test: https://support.google.com/googleplay/android-developer/answer/14151465
- Asset packs: https://developer.android.com/guide/playcore/asset-delivery

## 13 Kasım 2026 — 1.0.0 yayın uyumluluğu
- [ ] `1.0.0` için GitHub Actions APK testleri ve Play TEST AAB başarılı. APK ve AAB birleşik manifestlerinde `INTERNET` / `REQUEST_INSTALL_PACKAGES` olmadığı `bundletool` / merged manifest ile doğrulandı.
- [ ] Gizlilik politikası 1.0.0 içerik değişiklikleriyle güncellendi; HTTPS'te yayımlandı.
- [ ] `1.0.0` gerçek upload-key AAB'si test ve yasal incelemeler sonrası üretildi. Önceki 0.9.9 Play paketi artık güncel değil.
- [ ] Yeni `Uygulama Hakkında` ekranı, sürüm ve lisans alt ekranı gerçek Android cihazında test edildi.
- [ ] Güncelleme API çağrısı, otomatik APK indirme/kurma veya GitHub yükseltme ekranı bulunmadığı doğrulandı.
