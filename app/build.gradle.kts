plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.atasoy.gallerypro"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.atasoy.gallerypro"
        minSdk = 29
        targetSdk = 36
        versionCode = 8
        versionName = "0.5.2"
        // Deneme APK'sı yalnızca 64 bit ARM telefonlar için (Samsung ve Tecno bu türde): ML Kit'in yerel
        // kütüphaneleri dört işlemci türü için ayrı ayrı yer kaplamasın. Play Store'da AAB'ye geçince kaldırılır.
        ndk { abiFilters += "arm64-v8a" }
    }

    signingConfigs {
        create("release") {
            val keystore = file("release.jks")
            if (keystore.exists()) {
                storeFile = keystore
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Gerçek yayın anahtarı yoksa sabit debug anahtarıyla imzalanır: telefona kurulabilen, hızlı
            // (hata ayıklama kapalı, R8 ile küçültülmüş) deneme sürümü. Yayından önce release.jks eklenir.
            signingConfig = if (file("release.jks").exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += setOf(
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json",
                "META-INF/*.version",
                "META-INF/**/LICENSE.txt"
            )
        }
    }

    lint {
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.coil.compose)

    implementation(libs.kyant.backdrop)
    implementation(libs.kyant.shapes)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui.compose)

    // 5. aşama: cihaz içi yapay zekâ (modeller APK'nın içinde, internet yok) ve arka plan dizinleme.
    implementation(libs.androidx.work.runtime)
    implementation(libs.mlkit.image.labeling)
    implementation(libs.mlkit.text.recognition)

    testImplementation(libs.junit)
}
