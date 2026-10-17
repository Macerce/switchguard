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

// Genişletilmiş Material simgeleri ~38 MB: tamamı yerine yalnızca icons.txt'deki simgelerin sınıflarını içeren ince jar.
// Listede olmayan bir simge derlenmez (sınıfı yok), böylece çalışırken eksik simge hatası olamaz.
val iconsExtended: Configuration by configurations.creating { isTransitive = false }

val slimIcons by tasks.registering(Jar::class) {
    // icons.txt + ekran gruplarının kendi listeleri (icons-*.txt).
    val lists = projectDir.listFiles { f -> f.name == "icons.txt" || (f.name.startsWith("icons-") && f.name.endsWith(".txt")) }!!.sorted()
    val names = lists.flatMap { it.readLines() }.map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.distinct()
    inputs.files(lists)
    archiveFileName.set("icons-slim.jar")
    destinationDirectory.set(layout.buildDirectory.dir("icons"))
    from({ zipTree(iconsExtended.singleFile) }) {
        // Kotlin derleyicisi üst düzey tanımları (Icons.Rounded.X uzantıları) bu dosyadan bulur.
        include("META-INF/*.kotlin_module")
        names.forEach { include("androidx/compose/material/icons/${it}Kt.class", "androidx/compose/material/icons/${it}Kt$*.class") }
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("org.jetbrains.compose.material:material-icons-core:1.7.3")
    iconsExtended("org.jetbrains.compose.material:material-icons-extended-desktop:1.7.3")
    implementation(files(slimIcons))
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")
    implementation("net.java.dev.jna:jna:5.19.1")
    implementation("net.java.dev.jna:jna-platform:5.19.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.xerial:sqlite-jdbc:3.53.4.0")
    // MP3 → PCM çözümü yalnızca ses yüklerken (sound/SoundImport); LGPL, ayrı jar olarak paketlenir.
    implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4") { exclude(group = "junit") }
    // Cihaz kartlarını basılı tutup sürükleyerek sıralama (Apache-2.0).
    implementation("sh.calvin.reorderable:reorderable:3.1.0")
    testImplementation("junit:junit:4.13.2")
    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    testImplementation(compose.desktop.uiTestJUnit4)
}

compose.desktop {
    application {
        mainClass = "com.macerce.switchguard.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe)
            packageName = "SwitchGuard"
            packageVersion = "0.5.0"
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
