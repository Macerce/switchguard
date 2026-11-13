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

// sqlite-jdbc ~11 MB, çoğu 20+ platformun yerel kütüphanesi. Paketlenen JVM yalnızca Windows x64 olduğundan
// diğerleri çıkarılmış ince jar kullanılır (META-INF/services sürücü kaydı korunur).
val sqliteJdbc: Configuration by configurations.creating { isTransitive = false }

val slimSqlite by tasks.registering(Jar::class) {
    archiveFileName.set("sqlite-jdbc-win64.jar")
    destinationDirectory.set(layout.buildDirectory.dir("sqlite"))
    manifest { attributes("Multi-Release" to "true") } // asıl jar çoklu sürüm (META-INF/versions/9)
    from({ zipTree(sqliteJdbc.singleFile) }) {
        exclude("META-INF/MANIFEST.MF")
        exclude { e -> e.path.startsWith("org/sqlite/native/") && !e.path.startsWith("org/sqlite/native/Windows/x86_64") && !e.isDirectory }
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
    sqliteJdbc("org.xerial:sqlite-jdbc:3.53.4.0")
    implementation(files(slimSqlite))
    // MP3 → PCM çözümü yalnızca ses yüklerken (sound/SoundImport); LGPL, ayrı jar olarak paketlenir.
    implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4") { exclude(group = "junit") }
    // Cihaz kartlarını basılı tutup sürükleyerek sıralama (Apache-2.0).
    implementation("sh.calvin.reorderable:reorderable:3.1.0")
    // Raporları PDF olarak kaydetmek (Apache-2.0): telefondaki Canvas çizimi Graphics2D üzerinden vektör PDF'e.
    implementation("org.apache.pdfbox:pdfbox:3.0.5")
    implementation("de.rototor.pdfbox:graphics2d:3.0.5")
    testImplementation("junit:junit:4.13.2")
    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    testImplementation(compose.desktop.uiTestJUnit4)
}

compose.desktop {
    application {
        mainClass = "com.macerce.switchguard.desktop.MainKt"

        // Bellek: tepside canlı yığın ~15 MB iken varsayılan G1 ~150 MB yığın + ~55 MB GC yükü ayırıp geri vermiyordu
        // (özel bellek ~370 MB). Serial GC + küçük başlangıç yığını + sıkı boş oran ölçümde ~150 MB (0.15.2, 13 Kas 2026).
        // C1-only (TieredStopAtLevel=1) kod önbelleğini ve derleyici iş parçacıklarını küçültür; arayüz için hız yeterli.
        jvmArgs += listOf(
            "-XX:+UseSerialGC", "-Xms8m", "-Xmx256m",
            "-XX:MinHeapFreeRatio=10", "-XX:MaxHeapFreeRatio=20",
            "-XX:TieredStopAtLevel=1",
        )

        nativeDistributions {
            targetFormats(TargetFormat.Exe)
            packageName = "SwitchGuard"
            packageVersion = "0.15.2"
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

// Windows ekran koruyucusu başlatıcısı (packaging/saver/SwitchGuardSaver.cs → SwitchGuard.scr). Her Windows 10/11'de
// bulunan .NET Framework derleyicisiyle (csc) derlenir; uygulama kaynağı olarak gömülür, kullanıcı ayarı açınca
// %APPDATA%\SwitchGuard'a çıkarılır (bkz. platform/ScreenSaver.kt).
val saverOut = layout.buildDirectory.dir("saver")
val compileSaver by tasks.registering(Exec::class) {
    val src = file("packaging/saver/SwitchGuardSaver.cs")
    val ico = file("packaging/SwitchGuard.ico")
    val out = saverOut.get().file("saver/SwitchGuard.scr").asFile
    inputs.files(src, ico)
    outputs.file(out)
    doFirst { out.parentFile.mkdirs() }
    val csc = (System.getenv("WINDIR") ?: "C:/Windows") + "/Microsoft.NET/Framework64/v4.0.30319/csc.exe"
    commandLine(csc, "/nologo", "/target:winexe", "/optimize+", "/win32icon:${ico.absolutePath}", "/out:${out.absolutePath}", src.absolutePath)
}
sourceSets["main"].resources.srcDir(files(saverOut).builtBy(compileSaver))
