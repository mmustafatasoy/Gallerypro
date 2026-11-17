# GalleryPro 1.0.0 — Google Play Console form ve dosya rehberi
17 Kasım 2026 • Paket: `com.atasoy.gallerypro` • versionCode 32 • targetSdk 36

Bu metin hesap sahibinin Play Console'da doğru beyanlarda bulunmasına yardımcı olmak içindir.
Bir formun doldurulduğunu, Google tarafından kabul edildiğini veya uygulamanın yayımlandığını iddia etmez.

## Dosyalar
- **AAB:** Yalnızca ana `main` dalında gerçek upload key ile imzalanmış `Gallerypro.aab`; APK yüklemeyin.
- **512 x 512 PNG icon:** `Play Store Assets` Actions artifact dosyasındaki RGBA icon.
- **1024 x 500 feature graphic:** Aynı Actions artifact dosyasındaki RGB feature graphic.
- **Gerçek ekran görüntüleri:** `Play Real Emulator Screenshots` Actions artifact içindeki
  `01-GalleryPro-Kutuphane.png` ve `02-GalleryPro-Koleksiyonlar.png`.
  Bunlar çalışan Android emülatörünün gerçek GalleryPro arayüzüdür; örnek manzara
  fotoğrafları yalnızca test ortamı için üretilmiştir. Play Console bunları kabul etmeyebilir;
  gerektiğinde fiziksel cihazdan ek ekran görüntüleri çekin.
- **Gizlilik politikası:** https://atasoyyazilim.com/gallerypro-gizlilik.html
- **İletişim:** destek@atasoyyazilim.com
- **Mağaza metinleri:** `play/store-listing.md`
- **Fotoğraf / video izin beyanı:** `play/PHOTO-VIDEO-IZIN-BEYANI.md`

## Data Safety — gerçek release paketiyle doğrulanacak beyan
- Uygulama hesabı: **Yok**.
- Kullanıcıdan topladığı/veri işleyicisine ilettiği veriler: **Yok**, 1.0.0 yayın
  manifestinde `INTERNET` izni yok; yerel medya ve yüz vektörleri sunucuya aktarılmıyor.
- Üçüncü taraflarla kendi başına veri paylaşma: **Yok**.
- Fotoğraf/video, EXIF, yer bilgileri ve SFace yüz gruplama kayıtları: **Cihaz üzerinde**.
- Kullanıcı verileri cihazından temizleyebilir: Android Ayarlar > Uygulamalar >
  GalleryPro > Depolama > Verileri temizle; orijinal medya otomatik silinmez.
- Kullanıcının özellikle Android Paylaş menüsünden dış uygulamaya gönderdiği medya
  kullanıcının açık tercihidir. Diğer uygulamanın gizlilik politikası uygulanır.
- Güncelleme: Google Play; uygulama GitHub'a bağlanmaz.
- Reklam: **Yok** (mevcut kaynaklarda reklam SDK'sı veya reklam kodu bulunmuyor).
- Uygulama içi satın alma: **Yok** (mevcut pakette bulunmuyor).
- Hesap açma ve giriş: **Yok** (tam uygulama erişimi).

**Dikkat:** Play Console soruları zamanla değişir. Her "Evet/Hayır" seçimini final paket,
Android izinleri ve eklenen SDK'lara göre doğrulayın; buradaki taslak otomatik beyan değildir.
Play Console bazı cihaz içi işlemler için de ek açıklama isteyebilir.

## Play özel izin beyanları
- `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`: Bir defalık fotoğraf seçimi yetersiz;
  GalleryPro'nun asıl işlevi cihazdaki tüm fotoğrafları/videoları gezme, albümler,
  düzenleme, arama, gizleme ve silme. Kullanıcının talebine bağlı sürekli arşiv erişimi.
- `READ_MEDIA_VISUAL_USER_SELECTED`: Sistem destekliyorsa kısmi medya erişimi.
- `ACCESS_MEDIA_LOCATION`: Bilgi panelinde konum EXIF'i için kullanıcı isteği.
- `MANAGE_MEDIA`: Sistem özel erişimi, yalnızca isteğe bağlı yönetim işlemleri.
- `USE_BIOMETRIC`: Gizli albümü Android'in kendi sistem istemiyle açma; uygulama
  biyometrik kalıbı toplamaz.
- `INTERNET`, `REQUEST_INSTALL_PACKAGES`: Yayın manifestinde **yok**.
- **Yüz gruplama**: Açıkça kullanıcı başlatır, sadece cihazda işlenir;
  eşleştirmeler hatalı olabilir. Model kaynakları ve ticari eğitim veri kökeni
  belirsizlik notu `scripts/play/SFACE-MODEL-PROVENANCE.txt` içinde açıklanır.

## Hesap sahibinin yapacakları
1. Play Console > GalleryPro (paket `com.atasoy.gallerypro`) kaydı açılır.
2. App content: reklamlara, uygulama erişimine, veri güvenliğine, yaş grubuna
   ve içerik derecelendirmesine ilişkin **gerçek** beyanlar doldurulur.
3. Store listing: Türkçe/İngilizce metinler, 512 ikon, 1024x500 grafik,
   2+ gerçek ekran görüntüsü, gizlilik URL'si, destek e-postası.
4. App signing etkinse **yükleme sertifikası** 0.9.9 ile uyumlu
   SHA-256 `2bdfaeac96e36faf05141c320b8ebc64fd1e6c258436e44f34eec4311aa2c28a` ile karşılaştırılır.
   Google Play uygulama imzalama sertifikası, upload sertifikasından farklı olabilir.
5. **Önce Internal testing** bölümüne son **imzalı AAB** yüklenir ve Play tarafından
   oluşturulan gerçek split sürüm Android telefonda sınanır. Test APK'sı debug
   anahtarıyla imzalı olduğundan Play sürümü onun üzerine doğrudan kurulamayabilir.
6. Testler (video kırpma, yüz tarama, EXIF, Motion Photo, gizli albüm) geçtikten,
   izinler ve model hakları incelendikten sonra üretim yayınına geçilir.
7. Gerekli kişisel hesaplarda üretim erişiminden önce **12 testçi/14 gün** kuralı
   uygulanır; kimlik doğrulama bu koşulu otomatik kaldırmaz.

## Önemli sınır
AAB'yi GitHub Release'e koymak **Play'e otomatik yükleme değildir**.
Play Console, hesap sahibi oturumu ve Google'ın inceleme/onay süreci gerekir.
Model için açık kaynak lisansın bulunması, eğitim veri kaynakları hakkında bilinmeyen
hakların otomatik teyit edildiği anlamına gelmez.
