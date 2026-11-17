# GalleryPro 1.0.0 — Google Play fotoğraf/video izin beyanı TASLAĞI

> Bu metin Play Console'da **Fotoğraflar ve videolar** izin açıklaması için editoryal taslaktır.
> Gerçek mağaza beyanı, cihaz testleri ve Play Console ekranı doğrulanmadan “onaylandı” denmez.

## Neden sürekli medya erişimi gerekiyor?

GalleryPro bağımsız bir **galeri ve medya yönetimi** uygulamasıdır. Kullanıcının tüm kitaplığını tarihe ve albüme göre göstermesi, yeni fotoğrafları cihaz üzerinde dizinlemesi, arama/benzer fotoğraflar ve isteğe bağlı yüz gruplama yapması, favori/son silinenler yönetimi ve fotoğraf/video düzenlemesi için kitaplığa sürekli erişim gerekir. Bunlar isteğe bağlı dosya ekleri değil, uygulamanın temel işlevleridir.

Android Photo Picker tekil fotoğrafları başka bir iş akışına seçip vermeye uygundur; ancak GalleryPro'nun cihazdaki tüm kitaplığı izlemesi ve albümleri yönetmesi için tek başına yeterli değildir. Android'in **yalnızca seçilen fotoğraflar** erişim seçeneği desteklenir; bu durumda özellikler erişilebilen öğelerle sınırlı çalışır.

- `READ_MEDIA_IMAGES`: kullanıcı galerisine ait fotoğrafların listelenmesi, görüntülenmesi, düzenlenmesi ve cihaz içi indeksleme.
- `READ_MEDIA_VIDEO`: video listesi, oynatma, zaman aralığı kırpma ve galeri koleksiyonları.
- `READ_MEDIA_VISUAL_USER_SELECTED`: Android'in kısmi erişim seçeneği.
- `ACCESS_MEDIA_LOCATION`: kullanıcı izin verdiğinde EXIF konumunu okumak/düzenlemek.
- `MANAGE_MEDIA`: kullanıcı tarafından isteğe bağlı etkinleştirilen medya yönetimi erişimi.
- `USE_BIOMETRIC`: gizli albüme sistem üzerinden biyometrik doğrulama; biyometrik şablonlar uygulama tarafından alınmaz.

## Play Console kontrolü

1. Kurulumdan gelen **son 1.0.0 AAB** üzerinde birleşik manifest izinlerini doğrula.
2. Fotoğraf/video izin beyanını bu gerçek işlevlerle eşleştir ve gerekli ekran kayıtlarını istenirse hazırla.
3. Gizlilik politikası URL'sini ve veri güvenliği formunu **son AAB'nin SDK/veri akışına** göre doğrula.
4. Kullanıcının paylaştığı medyanın hedef uygulamada işlenmesi ile GalleryPro'nun veri toplaması aynı şey değildir; beyanları karıştırma.
5. Yüz tanıma modeli SFace FP32'nin ticari ağırlık/eğitim veri hakkı netleşmeden genel üretim yayını yapma.

**Durum (17.11.2026):** Metin hazır; Play Console'a gönderilmiş veya Google tarafından onaylanmış değildir.
