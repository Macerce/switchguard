plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.macerce.ewelinkalarm"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.macerce.ewelinkalarm"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Kişisel kullanım: release APK'yı debug anahtarıyla imzala ki doğrudan kurulabilsin.
            signingConfig = signingConfigs.getByName("debug")
        }
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
    testImplementation("junit:junit:4.13.2")
    // android.jar içindeki org.json JVM testlerinde boş stub; gerçeğini ekliyoruz.
    testImplementation("org.json:json:20240303")
}
