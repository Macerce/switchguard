import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"
    id("org.jetbrains.compose") version "1.9.3"
}

repositories {
    google()
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    // Yalnizca Icons.Filled.Warning kullaniliyor (core icinde); materialIconsExtended ~38 MB tuttugu icin cikarildi.
    implementation("org.jetbrains.compose.material:material-icons-core:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")
    implementation("net.java.dev.jna:jna:5.19.1")
    implementation("net.java.dev.jna:jna-platform:5.19.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.xerial:sqlite-jdbc:3.53.4.0")
    testImplementation("junit:junit:4.13.2")
}

compose.desktop {
    application {
        mainClass = "com.macerce.switchguard.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe)
            packageName = "SwitchGuard"
            packageVersion = "0.1.1"
            vendor = "Macerce"
            description = "SwitchGuard"

            // suggestRuntimeModules: java.instrument, java.sql, jdk.unsupported
            // + jdk.crypto.ec (OkHttp TLS), jdk.localedata (Turkce yerel ayar), jdk.accessibility (AWT erisilebilirlik)
            modules("java.instrument", "java.sql", "jdk.unsupported", "jdk.crypto.ec", "jdk.localedata", "jdk.accessibility")

            windows {
                iconFile.set(project.file("packaging/SwitchGuard.ico"))
                perUserInstall = true
                menu = true
                menuGroup = "SwitchGuard"
                shortcut = true
                dirChooser = false
                upgradeUuid = "0adb2f78-fb6a-4df0-9106-404091d283c8"
            }
        }
    }
}
