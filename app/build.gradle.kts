import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Yayın imzası keystore.properties'ten okunur (git'e girmez). Yoksa debug anahtarı kullanılır.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.macerce.switchguard"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.macerce.switchguard"
        minSdk = 26
        targetSdk = 37
        versionCode = 41
        versionName = "2.21.1"
        // Geliştiricinin kendi telefonları için: ./gradlew assembleRelease -PownerUnlock=true
        // Mağaza derlemesinde her zaman false.
        buildConfigField("boolean", "OWNER_UNLOCK", (project.findProperty("ownerUnlock") == "true").toString())
        // Cihaz üstü testler (Keystore, bildirimler, ses): ./gradlew connectedDebugAndroidTest
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        generateLocaleConfig = true
        // Kütüphanelerin getirdiği onlarca dili at; uygulama yalnızca EN + TR destekliyor.
        localeFilters += listOf("en", "tr")
    }

    // Yerleşik Kotlin, jvmTarget'ı targetCompatibility'den alır.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // TV'de telefondan anahtar girme için QR kodu (yalnızca üretme).
    implementation("com.google.zxing:core:3.5.4")
    // Cihaz kartlarını basılı tutup sürükleyerek sıralama (masaüstüyle aynı kütüphane).
    implementation("sh.calvin.reorderable:reorderable:3.1.0")
    implementation("com.android.billingclient:billing:9.1.0")
    // Billing → play-services-base eski fragment 1.1.0'ı çekiyor; Play Console "eski SDK" uyarısı veriyor.
    implementation("androidx.fragment:fragment:1.9.1")
    // Uygulama içinden Play değerlendirme penceresi (bir kez, uygun anda).
    implementation("com.google.android.play:review:2.0.2")
    implementation("com.google.android.play:app-update:2.1.0")
    // Saat uygulamasıyla (Wear OS) veri alışverişi.
    implementation("com.google.android.gms:play-services-wearable:20.0.1")
    // Bölge otomasyonları: alana giriş/çıkış bildirimi (pil dostu, sistem izler).
    implementation("com.google.android.gms:play-services-location:21.3.0")

    testImplementation("junit:junit:4.13.2")
    // android.jar içindeki org.json JVM testlerinde boş stub; gerçeğini ekliyoruz.
    testImplementation("org.json:json:20260814")

    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
}
