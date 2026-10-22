import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// Saat uygulaması: telefondaki SwitchGuard'ın alarmını bilekte gösterir, kapattırır; cihazları açıp kapatır.
// Veri katmanı (Wearable Data Layer) eşleşmesi için uygulama kimliği ve imza telefonla aynı olmalı.
android {
    namespace = "com.macerce.switchguard.wear"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.macerce.switchguard"
        minSdk = 30
        targetSdk = 36
        // Telefonla aynı mağaza kaydında ayrı APK: sürüm kodu telefonunkilerle çakışmasın.
        versionCode = 100_032
        versionName = "2.15.0"
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    buildFeatures { compose = true }

    androidResources {
        localeFilters += listOf("en", "tr")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.wear.compose:compose-material3:1.7.1")
    implementation("androidx.wear.compose:compose-foundation:1.7.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.wear:wear-phone-interactions:1.1.1")
    implementation("com.google.android.gms:play-services-wearable:20.0.1")
}
