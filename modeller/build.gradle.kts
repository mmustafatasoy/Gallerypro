// Play Asset Delivery: cümleyle arama modelleri (≈230 MB) AAB'de ayrı bir "kurulum anında" paket olur.
// Play, uygulamayla birlikte indirir; uygulama onları yine AssetManager'dan okur (assets/models/…).
// Dosyalar derlemede "modeller" sürümünden src/main/assets/models/ altına indirilir; depoya konmaz.
plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("modeller")
    dynamicDelivery {
        deliveryType.set("install-time")
    }
}
