#!/data/data/com.termux/files/usr/bin/bash
# GalleryPro özel imza anahtarı: Termux üzerinde, kullanıcının kendisi üretir.
# Anahtar / parolayı Git reposuna ya da sohbet ekranına asla yazmayın.
set -euo pipefail
umask 077
if ! command -v keytool >/dev/null 2>&1; then
  echo "Önce Termux'ta: pkg update && pkg install openjdk-17 coreutils"
  exit 1
fi
PRIVATE_DIR="$HOME/.gallerypro-imza"
KEYSTORE="$PRIVATE_DIR/gallerypro-release.jks"
B64="$PRIVATE_DIR/RELEASE_KEYSTORE_B64.txt"
mkdir -p "$PRIVATE_DIR"
chmod 700 "$PRIVATE_DIR"
if [ -e "$KEYSTORE" ]; then
  echo "ÖNEMLİ: Mevcut imza dosyası bulundu: $KEYSTORE"
  echo "Üzerine yazılmadı. İmzayı değiştirmek eski APK'ların güncellenmesini engeller."
  exit 1
fi
echo "GalleryPro için 4096-bit RSA özel yayın anahtarı oluşturulacak."
echo "Parolanı kendin belirle, güvenli bir kasada yedekle. Kimseyle paylaşma."
keytool -genkeypair -v \
  -keystore "$KEYSTORE" -storetype PKCS12 \
  -alias gallerypro -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=GalleryPro, OU=Apps, O=Atasoy Yazilim, C=TR"
chmod 600 "$KEYSTORE"
base64 -w 0 "$KEYSTORE" > "$B64"
chmod 600 "$B64"
echo ""
echo "Anahtar oluşturuldu. Özel dosyalar:"
echo "  $KEYSTORE"
echo "  $B64"
echo ""
echo "GitHub > Repository Settings > Secrets and variables > Actions içine ekle:"
echo "  RELEASE_KEYSTORE_B64 : $B64 dosyasının içeriği"
echo "  KEYSTORE_PASSWORD    : az önce belirlediğin kasa parolası"
echo "  KEY_ALIAS            : gallerypro"
echo "  KEY_PASSWORD         : aynı parola (PKCS12)"
echo ""
echo "Uyarı: İmza anahtarını kimseye, sohbete veya herkese açık depoya gönderme."
echo "Bir kez özel imzaya geçince aynı anahtarı tüm sonraki sürümlerde kullan."
echo "Şimdiki deneme imzalı APK'nın üzerine özel imzalı APK kurulamayabilir."
echo "Yedek alınmadan önceki kurulumu SİLME."
