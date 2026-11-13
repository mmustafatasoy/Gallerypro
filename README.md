# GalleryPro — Atasoy Yazılım

Android için modern Motion Glass tasarımlı, cihaz içi akıllı arama ve fotoğraf/video düzenleme uygulaması.

## İşlevler ve durum (1.0.0)

- **Kütüphane:** tarihe göre ızgara, favoriler, albümler, sayfalama, izgaradaki konumdan büyüyerek fotoğraf açma.
- **Fotoğraf:** kırpma, filtre, renk düzenleme, EXIF tarih/konum, yakınlaştırma; 48–200 MP destekli JPEG/PNG/WebP için bölgesel çözümleme.
- **Video:** Media3 oynatıcı, ses, hız, 10 saniye ileri/geri, PiP, 4× yakınlaştırma, ayrı MP4 olarak kırpma.
- **Hareketli fotoğraf:** desteklenen Samsung JPEG/HEIC içindeki gömülü MP4'ü tespit edip fotoğrafı değiştirmeden oynatma.
- **Cihaz içi AI:** ML Kit etiket/OCR, Türkçe CLIP cümleyle arama, SFace FP32 yüz grupları, Anılar ve Yerler.
- **Araçlar:** Gizli Albüm, Son Silinenler, Kopyalar, uzun basma/çoklu seçim.

**Durum:** Kod aşamaları tamamlandı; Android telefonlarda bütün özelliklerin uçtan uca test edildiği veya Google Play'de yayımlandığı iddia edilmez. Son sürüm için GitHub Actions test sonucunu kontrol edin.

## APK ve kaynak yedeği

- [En güncel başarıyla derlenmiş APK](https://github.com/mmustafatasoy/Gallerypro/releases/download/son-surum/Gallerypro.apk)
- [Releases — tam kaynak ZIP ve SHA-256](https://github.com/mmustafatasoy/Gallerypro/releases/tag/son-surum)
- [GitHub Actions derleme/test](https://github.com/mmustafatasoy/Gallerypro/actions)

**1.0.0'dan itibaren** uygulama içi GitHub sürüm denetimi, APK indirme/kurma akışı ve ilgili izinler hem kişisel APK'dan hem Play AAB'den tamamen kaldırıldı. Güncellemeleri Play Store dağıtımı için Google Play yönetir. GitHub Releases yalnızca test APK'ları ve kaynak ZIP yedeği dağıtımı içindir. Ayarlar'da sürüm, güncelleme veya lisans listesi gösterilmez; `Ayarlar → Uygulama Hakkında` altında `v1.0.0` ve Açık Kaynak Lisansları bağlantısı bulunur.

Mevcut test APK'ları aynı deneme imzasını kullanır. İlerde özel imzaya geçmek mevcut uygulamanın üzerine kurulumu engelleyebilir: uygulamayı kaldırarak yerel verileri kaybetmeyin.

## Google Play için tamamlanması gerekenler

1.0.0 için yeniden üretilecek upload anahtarıyla imzalı AAB, Play Console geliştirici kimlik onayı, halka açık HTTPS gizlilik politikası, SFace FP32 eğitim/veri lisansı doğrulaması, mağaza varlıkları, gerçek cihaz testleri ve Play Console incelemesi. Ayrıntılar: `play/PLAY-CHECKLIST.md`. Açık eski **PR #21** özel imza dalı mevcut `main` ile çakışır ve başarısız CI içerir, zorla birleştirilmeyecek.

## Teknoloji ve geliştirme

Kotlin, Compose Foundation (Material3 yok), Kyant Backdrop, Coil 3, Media3, ML Kit ve LiteRT. Derleme önce `bash scripts/fetch-fonts.sh` ardından `gradle testDebugUnitTest assembleRelease`. Büyük AI model ağırlıkları Git'e eklenmez; CI'da resmi kaynaklardan alınır. Tam kaynak ZIP, Git geçmişi yedeğini içerir.

- `DEVAM-NOTU.md`: güncel aşamalar ve ertelenmiş kullanıcı/cihaz kararları
- `AGENTS.md`: kodlama ve yayın kuralları
- `CODEX-ILK-MESAJ.md`: tarihsel başlangıç görevleri
- [Tasarım](https://claude.ai/artifact/J9L59HCcDDZVwuod8fYSiw) · [Proje dokümanı](https://claude.ai/artifact/7aviV7rE1kBvm4eBtUtzm9)
