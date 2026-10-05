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
    implementation(compose.materialIconsExtended)
    implementation("org.json:json:20240303")
    testImplementation("junit:junit:4.13.2")
}

compose.desktop {
    application {
        mainClass = "com.macerce.switchguard.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe)
            packageName = "SwitchGuard"
            packageVersion = "0.1.0"
            vendor = "Macerce"
            description = "SwitchGuard"

            windows {
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
