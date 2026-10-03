import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Yayın imzası keystore.properties'ten okunur (git'e girmez). Yoksa debug anahtarı kullanılır.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.macerce.switchguard"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.macerce.switchguard"
        minSdk = 26
        targetSdk = 36
        versionCode = 20
        versionName = "2.8.3"
        // Geliştiricinin kendi telefonları için: ./gradlew assembleRelease -PownerUnlock=true
        // Mağaza derlemesinde her zaman false.
        buildConfigField("boolean", "OWNER_UNLOCK", (project.findProperty("ownerUnlock") == "true").toString())
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.12.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.android.billingclient:billing:9.1.0")
    // Billing → play-services-base eski fragment 1.1.0'ı çekiyor; Play Console "eski SDK" uyarısı veriyor.
    implementation("androidx.fragment:fragment:1.9.1")

    testImplementation("junit:junit:4.13.2")
    // android.jar içindeki org.json JVM testlerinde boş stub; gerçeğini ekliyoruz.
    testImplementation("org.json:json:20240303")
}
