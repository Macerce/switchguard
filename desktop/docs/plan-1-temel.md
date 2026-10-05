# SwitchGuard for Windows — Aşama 1 (Temel) Uygulama Planı

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bu bilgisayara exe ile kurulan, tepside çalışan, demo cihazla tam ekran alarm + ses veren ilk masaüstü sürümü (0.1.0); telefon mantığı kopyalanmış ve testli, izleme motoru testli; Deneme B (Tuya çift tüketici) sonuçlanmış. Deneme A (eWeLink yerel dönüş adresi) yedek hesabın geliştirici onayını bekliyor (Task 13, 2. aşama).

**Architecture:** `desktop/` bağımsız Gradle projesi. `core/` + `api/` telefondan kopyalanır (aynı paket adları), `data/` masaüstü için yeniden yazılır (aynı API), `MonitorService` mantığı `desktop/engine/MonitorEngine`'e taşınır (tek iş parçacıklı zamanlayıcı, platform kancaları `MonitorHost`). Compose Desktop arayüzü tepsi + ana pencere + alarm penceresi.

**Tech Stack:** Kotlin 2.2.21, Compose Multiplatform (Desktop) 1.9.3, Material 3, OkHttp 4.12.0, org.json 20240303, sqlite-jdbc 3.53.4.0, JNA 5.19.1, JUnit 4.13.2, Temurin JDK 21 (jpackage).

**Spec:** `desktop/docs/spec.md`

## Global Constraints

- `app/` altında hiçbir dosya değişmez. Her görev sonunda: `git diff --stat c545395 -- app/` boş (dalın başladığı nokta; master'a göre bakılırsa tv-tablet'in kendi farkları görünür).
- Kotlin 2.2.21 (`org.jetbrains.kotlin.jvm`, `org.jetbrains.kotlin.plugin.compose`), `org.jetbrains.compose` 1.9.3, Gradle 8.14.3 (wrapper kökten kopya), `jvmTarget` 17.
- Derleme JDK'sı Temurin 21 → `C:\Users\macerce\.jdks\temurin-21`. Her Gradle çağrısı: `cd desktop && JAVA_HOME=/c/Users/macerce/.jdks/temurin-21 ./gradlew <görev>`.
- Kopyalanan dosyalar paket adlarını korur: `com.macerce.switchguard.core`, `.api`, `.data`. Masaüstüne özgü kod: `com.macerce.switchguard.desktop.*`.
- Veri klasörü `%APPDATA%\SwitchGuard\`: `settings.json`, `events.db`, `metrics.db`, `logs\switchguard.log`, `instance.lock`, `instance.port`.
- Tüm dosya G/Ç'si açıkça UTF-8.
- DPAPI ile saklanan anahtarlar: `appSecret`, `at`, `rt`, `apikey`, `tuyaSecret`, `tuyaToken` (değer öneki `dpapi:`).
- Zaman sabitleri `MonitorService` ile aynı: döngü 15 s; eWeLink yeniden senkron 5 dk; sorgulama `pollIntervalSec` (vars. 30, en az 15); yeniden bağlanma 5/15/30/60/120 s; aç/kapa doğrulama 5 s; kanal gücü isteği 90 s / 120 s; güç verisi yok uyarısı 5 dk; Tuya senkron 15 dk (canlı) / 5 dk (sorgulama), engelli 30 dk, bağlanma zaman aşımı 60 s; demo dalgalanma 60 s. Yeni: uyku eşiği 60 s, uyku kaydı eşiği 2 dk.
- Kişisel sürüm: `isPro` her zaman `true`.
- Arayüz metinleri yalnızca `Strings` anahtarlarından; Windows'a özgü metinler `strings/values/desktop.xml` + `strings/values-tr/desktop.xml`.
- Exe: `packageName = "SwitchGuard"`, `packageVersion = "0.1.0"`, kullanıcı başına kurulum, kurulumda soru sorulmaz (lisans/klasör ekranı yok), `upgradeUuid = "0adb2f78-fb6a-4df0-9106-404091d283c8"` (asla değişmez).
- Kullanıcının **ana** eWeLink hesabıyla bu bilgisayarda giriş yapılmaz (telefon oturumu düşer). Yalnızca yedek hesap kullanılır.
- Commit'ler `windows` dalına; mesaj sonu `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Yalnızca Windows'ta çalışabilen testler (DPAPI, kayıt defteri) `Assume.assumeTrue(System.getProperty("os.name").startsWith("Windows"))` ile başlar; Linux'ta (bulut) atlanır, hata vermez.

## Review Focus

1. **Yarım yazılmış / bozuk `settings.json`** (elektrik kesintisi) → uygulama yine açılır, bozuk dosya `settings.json.corrupt-<ms>` olarak saklanır, varsayılanlarla devam eder. → Task 3 `corruptFileIsBackedUpAndDefaultsUsed`.
2. **Seçili özel ses dosyası silinmiş / okunamıyor** → varsayılan alarm sesi çalar; alarm asla sessiz kalmaz. → Task 8 `missingCustomFileFallsBackToDefault`.
3. **Türkçe metin ve TR biçimleri paketli exe'de** → "Rögar Su Seviye Bildirimi" JSON/DB'den bozulmadan döner; jlink çalışma ortamında `jdk.localedata` bulunur. → Task 3 `turkishTextRoundTrips`, Task 11 modül kontrolü.
4. **Uygulama ikinci kez açılır** (elle ya da Windows başlangıcında) → yeni süreç kapanır, açık olan pencere öne gelir; iki motor = çift alarm olmaz. → Task 10 `secondAcquireSignalsFirst`.
5. **Duvar saati atlar** (uyku, NTP düzeltmesi, geri alma) → 60 s altı ya da geriye atlamada uyku kaydı/yeniden bağlanma yok; gerçek uykuda yeniden bağlanma + senkron + (≥ 2 dk ise) kayıt. → Task 7 `sleepGapResyncsAndLogs`, `smallOrBackwardClockJumpIsIgnored`.

---

### Task 1: Proje iskeleti, JDK ve exe hattı

**Files:**
- Create: `desktop/settings.gradle.kts`, `desktop/build.gradle.kts`, `desktop/gradle.properties`, `desktop/.gitignore` (`build/`, `.gradle/`, `.kotlin/`)
- Create (kökten kopya): `desktop/gradlew`, `desktop/gradlew.bat`, `desktop/gradle/wrapper/*`
- Create: `desktop/src/main/kotlin/com/macerce/switchguard/desktop/Main.kt`

**Interfaces:**
- Produces: derlenen boş Compose penceresi; `packageExe` görevi; `mainClass = "com.macerce.switchguard.desktop.MainKt"`.

- [ ] **Step 1: JDK'yı indir.** `https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse` zip'ini indir, `C:\Users\macerce\.jdks\temurin-21` olarak aç (iç klasör adı `jdk-21.0.12.1+1` → yeniden adlandır).
  Run: `/c/Users/macerce/.jdks/temurin-21/bin/jpackage --version` → Expected: `21.0.12.1`
- [ ] **Step 2: Gradle dosyalarını yaz.** `settings.gradle.kts`: `rootProject.name = "switchguard-desktop"`, eklenti depoları `gradlePluginPortal()`, `google()`, `mavenCentral()`. `build.gradle.kts`: üç eklenti (sürümler Global Constraints), bağımlılıklar `compose.desktop.currentOs`, `compose.material3`, `compose.materialIconsExtended`; `compose.desktop.application { mainClass; nativeDistributions { targetFormats(TargetFormat.Exe); packageName; packageVersion; vendor = "Macerce"; description = "SwitchGuard"; windows { perUserInstall = true; menu = true; menuGroup = "SwitchGuard"; shortcut = true; dirChooser = false; upgradeUuid } } }`. `gradle.properties`: `org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8`, `kotlin.code.style=official`.
- [ ] **Step 3: `Main.kt`** — `fun main()` içinde `application { Window(onCloseRequest = ::exitApplication, title = "SwitchGuard") { Text("SwitchGuard") } }`.
- [ ] **Step 4: Derle.** Run: `./gradlew compileKotlin` → Expected: `BUILD SUCCESSFUL`
- [ ] **Step 5: Exe üret.** Run: `./gradlew packageExe` → Expected: `desktop/build/compose/binaries/main/exe/SwitchGuard-0.1.0.exe` var. WiX indirilemezse WiX 3.14'ü kur ve tekrar dene. `0.1.0` sürümü reddedilirse durup raporla (sürüm kararını kullanıcı verir).
- [ ] **Step 6: Duman testi.** Exe'yi çalıştır; `%LOCALAPPDATA%\SwitchGuard\SwitchGuard.exe` oluştu mu, Başlat menüsünde "SwitchGuard" var mı kontrol et; uygulamayı aç (pencere görünür), kapat; Ayarlar → Uygulamalar'dan ya da `HKCU\...\Uninstall` kaydındaki komutla kaldır.
- [ ] **Step 7: Commit** — `git add desktop/ && git commit -m "Desktop: Gradle skeleton and exe packaging smoke test"` (+ attribution).

### Task 2: `core/` kopyası ve 102 test

**Files:**
- Create (kopya): `desktop/src/main/kotlin/com/macerce/switchguard/core/` ← `app/src/main/java/com/macerce/switchguard/core/` içinden **AppLanguage.kt, Transfer.kt, TransferNet.kt hariç** 17 dosya
- Create (kopya): `desktop/src/test/kotlin/com/macerce/switchguard/core/` ← `app/src/test/java/com/macerce/switchguard/core/*.kt` (15 dosya)
- Create: `desktop/SYNC.md`
- Modify: `desktop/build.gradle.kts` (`implementation("org.json:json:20240303")`, `testImplementation("junit:junit:4.13.2")`)

**Interfaces:**
- Produces: telefonla aynı `core` API'si (DeviceSnapshot, ChangeDetector, RuleEngine, AutomationEngine, EventArgs/EventStrings/ChangeLabels, Demo, OfflineGrace, ExpectedChanges, WsMessages, TuyaParser, EnergyMath/UsageMath, Signer, Entitlement …).

- [ ] **Step 1: Kopyala** (`cp`, içerik değişmeden).
- [ ] **Step 2: Testleri koş.** Run: `./gradlew test` → Expected: `BUILD SUCCESSFUL`, `build/test-results/test/*.xml` toplamı 102 test, 0 hata.
- [ ] **Step 3: `SYNC.md` yaz.** Kaynak: `tv-tablet @ 132e7fe` (master birleşimi `c545395` ile aynı içerik). Tablo: dosya · kaynak yolu · uyarlama ("yok" / ne değişti). Kopyalanmayanlar ve nedenleri (AppLanguage → `desktop/i18n`, Transfer/TransferNet → 4. aşama). Kural: telefonda `core/`/`api/` değişince aynı değişiklik buraya taşınır.
- [ ] **Step 4: Commit** — "Desktop: copy core logic and its 102 unit tests from the phone app".

### Task 3: `Store` ve DPAPI

**Files:**
- Create: `desktop/src/main/kotlin/com/macerce/switchguard/desktop/data/SecretCodec.kt`
- Create: `desktop/src/main/kotlin/com/macerce/switchguard/data/Store.kt`
- Test: `desktop/src/test/kotlin/com/macerce/switchguard/data/StoreTest.kt`, `desktop/src/test/kotlin/com/macerce/switchguard/desktop/data/DpapiCodecTest.kt`
- Modify: `desktop/build.gradle.kts` (`net.java.dev.jna:jna:5.19.1`, `net.java.dev.jna:jna-platform:5.19.1`, `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2`)

**Interfaces:**
- Produces:
  - `interface SecretCodec { fun encode(plain: String): String; fun decode(stored: String): String? }`
  - `class DpapiCodec : SecretCodec` — `"dpapi:" + Base64(Crypt32Util.cryptProtectData(utf8))`; çözülemeyen blob → `null`; öneksiz değer olduğu gibi döner.
  - `object PlainCodec : SecretCodec` (testler için, değeri aynen saklar).
  - `class Store(file: Path, secrets: SecretCodec)` — telefondaki `app/.../data/Store.kt` (satır 42–266) ile **aynı ad, tür, varsayılan ve JSON anahtarları**; farklar: `prefs` ve `vibrate` yok; `isPro` her zaman `true`; `alwaysAwake` varsayılanı `true`; yeni alanlar `autostart: Boolean` (vars. `true`), `tuyaLive: Boolean` (vars. `false`), `language: String` (vars. `""`), `theme: String` (vars. `"system"`); `val version: StateFlow<Int>`; `companion { DEFAULT_REDIRECT = "https://127.0.0.1"; MIN_POLL = 15; MAX_ALARM_LINES = 20; SECRET_KEYS }`.
- Saklama: bellekte `JSONObject`, tek kilit; her yazma geçici dosyaya yazılıp `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)`; sonra `version++`. Açılışta ayrıştırılamazsa dosya `settings.json.corrupt-<ms>` olarak taşınır, boş başlanır.

- [ ] **Step 1: Failing testleri yaz** (`StoreTest`, geçici klasör, `PlainCodec` ya da `"enc:"` önekli test codec'i):
  - `valuesSurviveReload` — `appId="abc"`, `pollIntervalSec=45`, `quietHours=QuietHours(true, 1380, 420)` → yeni `Store` aynı dosyadan → eşit.
  - `defaultsMatchSpec` — `region=="eu"`, `pollIntervalSec==30`, `alwaysAwake`, `autostart`, `!tuyaLive`, `redirectUrl=="https://127.0.0.1"`, `alarmVolume==1f`, `isPro`.
  - `pollIntervalIsClampedToMinimum` — `5` → `15`.
  - `secretsAreStoredEncoded` — `accessToken="tok"` → ham dosyada `"enc:tok"` var, `"tok"` çıplak yok; yeniden açınca `accessToken=="tok"`.
  - `undecodableSecretReadsAsEmpty` — decode `null` dönerse `accessToken==""`, `!isLoggedIn`.
  - `corruptFileIsBackedUpAndDefaultsUsed` — dosyaya `{not json` → `appId==""` ve klasörde `settings.json.corrupt-*` var.
  - `turkishTextRoundTrips` — `snapshots` adı "Rögar Su Seviye Bildirimi" → yeniden açınca eşit; dosya baytları UTF-8 çözülünce "Rögar" içerir.
  - `versionIncrementsOnWrite`, `pendingAlarmKeepsLast20` (25 satır → son 20).
  - `DpapiCodecTest`: `roundTrip` ("gizli-Şifre-123"; kodlanmış hali `dpapi:` ile başlar), `foreignBlobDecodesToNull` (`"dpapi:AAAA"`), `plainValueWithoutPrefixIsReturnedAsIs`.
- [ ] **Step 2: Koş, kırmızı gör.** Run: `./gradlew test --tests "*StoreTest" --tests "*DpapiCodecTest"` → FAIL (sınıflar yok).
- [ ] **Step 3: `SecretCodec.kt` ve `Store.kt`'yi yaz.**
- [ ] **Step 4: Koş, yeşil gör.** Aynı komut → PASS; `./gradlew test` tümü PASS.
- [ ] **Step 5: Commit** — "Desktop: settings store (atomic JSON) with DPAPI-encrypted secrets".

### Task 4: `api/` kopyası ve günlük

**Files:**
- Create (kopya): `desktop/src/main/kotlin/com/macerce/switchguard/api/{EwelinkClient,EwelinkSocket,TuyaClient,TuyaSocket}.kt`
- Modify (kopyada): `EwelinkClient.buildLoginUrl` — `Uri.parse(...).buildUpon()` → `OAUTH_PAGE.toHttpUrl().newBuilder().addQueryParameter(...)` (aynı 8 parametre, aynı sıra); `TuyaSocket` — `android.util.Log` → `com.macerce.switchguard.desktop.Log`
- Create: `desktop/src/main/kotlin/com/macerce/switchguard/desktop/Log.kt`
- Test: `desktop/src/test/kotlin/com/macerce/switchguard/api/EwelinkClientTest.kt`
- Modify: `desktop/build.gradle.kts` (`com.squareup.okhttp3:okhttp:4.12.0`), `desktop/SYNC.md` (uyarlamalar)

**Interfaces:**
- Consumes: `Store` (Task 3).
- Produces: telefonla aynı `EwelinkClient`, `EwelinkSocket(store, Listener)`, `TuyaClient`, `TuyaSocket(store, Listener)`; `object Log { fun init(dir: Path); fun i(tag: String, msg: String); fun w(tag: String, msg: String, t: Throwable? = null) }` — `logs\switchguard.log`, en fazla 5 MB × 3 dosya, UTF-8; `init` öncesi yalnızca stderr.

- [ ] **Step 1: Failing test** `loginUrlHasOAuthParams` — Store(`appId="APPID"`, `appSecret="SECRET"`): `buildLoginUrl("st").toHttpUrl()` → `queryParameter("clientId")=="APPID"`, `redirectUrl=="https://127.0.0.1"`, `grantType=="authorization_code"`, `state=="st"`, `showQRCode=="false"`, `authorization == Signer.sign("APPID_"+seq, "SECRET")` (`seq` URL'den okunur).
- [ ] **Step 2: Koş, kırmızı.** `./gradlew test --tests "*EwelinkClientTest"` → FAIL.
- [ ] **Step 3: Kopyala ve uyarla; `Log.kt`'yi yaz.**
- [ ] **Step 4: Koş, yeşil.** Aynı komut → PASS; `./gradlew test` tümü PASS.
- [ ] **Step 5: Commit** — "Desktop: copy eWeLink/Tuya clients (HttpUrl instead of Uri, file logger)".

### Task 5: Metinler (`Strings`)

**Files:**
- Create (kopya): `desktop/src/main/resources/strings/values/strings.xml`, `desktop/src/main/resources/strings/values-tr/strings.xml` ← `app/src/main/res/values{,-tr}/strings.xml`
- Create: `desktop/src/main/resources/strings/values/desktop.xml`, `.../values-tr/desktop.xml` — anahtarlar: `tray_open`, `tray_start`, `tray_stop`, `tray_test`, `tray_exit`, `tray_exit_confirm`, `tray_exit_confirm_yes`, `tray_minimized_hint`, `alarm_dismiss`, `desktop_slept` (`%1$d` dk), `devices_empty`, `demo_simulate_toggle`, `demo_simulate_online`, `demo_simulate_power`, `monitoring_start`, `monitoring_stop`, `settings_autostart`, `settings_keep_awake`, `settings_tuya_live`
- Create: `desktop/src/main/kotlin/com/macerce/switchguard/desktop/i18n/Strings.kt`
- Test: `desktop/src/test/kotlin/com/macerce/switchguard/desktop/i18n/StringsTest.kt`

**Interfaces:**
- Produces: `class Strings` — `val language: String` ("tr"/"en"); `operator fun get(key: String): String` (TR'de yoksa EN, EN'de de yoksa anahtarın kendisi + `Log.w`); `fun format(key: String, vararg args: Any): String` (`String.format(Locale.forLanguageTag(language), …)`); `fun eventStrings(): EventStrings` (telefondaki `EventLog.strings(context)` ile aynı anahtarlar, `app/.../data/EventLog.kt` 128–141); `fun demoNames(): Demo.Names` (`app/.../ui/Actions.kt` 173–179 anahtarları); `companion { fun load(language: String): Strings; internal fun parse(xml: InputStream): Map<String, String>; internal fun unescape(raw: String): String }`. `load("")` → `Locale.getDefault().language == "tr"` ise `"tr"`, değilse `"en"`.
- `unescape` Android kuralları: dıştaki çift tırnaklar atılır; `\'` → `'`, `\"` → `"`, `\n` → satır sonu, `\t` → sekme, `\\` → `\`.

- [ ] **Step 1: Failing testler:**
  - `unescapesAndroidEscapes` — `"Smart Life\\'ta"` → `"Smart Life'ta"`; `"a\\nb"` → `"a\nb"`; `"\\\"x\\\""` → `"\"x\""`; `"\"  x \""` → `"  x "`.
  - `turkishTableLoaded` — `load("tr")["label_online"] == "ÇEVRİMİÇİ"`.
  - `missingTrKeyFallsBackToEnglish` — `load("tr")["app_name"] == "SwitchGuard"`.
  - `formatsPositionalArgs` — `load("en").format("status_polling", 3, 30)` "3" ve "30" içerir.
  - `everyEnglishKeyHasTurkish` — `translatable="false"` olanlar hariç EN anahtarlarının hepsi TR'de var (strings + desktop).
  - `eventStringsRenderInBothLanguages` — `EventArgs.Test.render("x", load("tr").eventStrings())` ≠ EN karşılığı.
- [ ] **Step 2: Koş, kırmızı.** `./gradlew test --tests "*StringsTest"` → FAIL.
- [ ] **Step 3: Kopyala, `desktop.xml` metinlerini (EN + TR) yaz, `Strings.kt`'yi yaz** (`javax.xml.parsers.DocumentBuilderFactory`, `<string name>` öğeleri).
- [ ] **Step 4: Koş, yeşil.** Aynı komut → PASS.
- [ ] **Step 5: Commit** — "Desktop: string tables (phone EN/TR + desktop) and loader".

### Task 6: `EventLog` ve `MetricsLog` (SQLite)

**Files:**
- Create: `desktop/src/main/kotlin/com/macerce/switchguard/data/EventLog.kt`, `.../data/MetricsLog.kt`
- Create (kopya): `.../data/LiveState.kt` ← telefon, değişmeden
- Test: `desktop/src/test/kotlin/com/macerce/switchguard/data/EventLogTest.kt`, `.../data/MetricsLogTest.kt`
- Modify: `desktop/build.gradle.kts` (`org.xerial:sqlite-jdbc:3.53.4.0`), `desktop/SYNC.md`

**Interfaces:**
- Produces:
  - `object EventKind` (telefondaki 4 sabit + `SYSTEM = "SYSTEM"`), `data class LoggedEvent` (kopya).
  - `class EventLog(file: Path)` — telefonla aynı: `version: StateFlow<Int>`, `add(deviceId, deviceName, kind, text, action, args: EventArgs? = null, time: Long = System.currentTimeMillis())`, `recent(deviceId: String? = null, limit: Int = 500)`, `latestId()`, `unseenAlertCount(afterId)`, `clear()`, `MAX_ROWS = 2000`. Şema = telefon v2 (`events` + `idx_events_ts`); eski sürüm taşıması yok.
  - `class MetricsLog(file: Path)` — telefondaki `MetricsLog.kt` 43–125 ile aynı genel API (`record`, `powerSamples`, `kwh`, `usage`, `hasPowerData`, `prune`, `clear`, `version`), aynı tablolar ve `POWER_EVERY_MS = 60_000`, `KEEP_DAYS = 400`.
  - Her veritabanı tek JDBC bağlantısı, `synchronized` yöntemler, `PRAGMA journal_mode=WAL`.

- [ ] **Step 1: Failing testler:** `addAndRecentNewestFirst`; `keepsAtMost2000Rows` (2005 ekle → 2000, en eski düşmüş); `unseenAlertCountCountsOnlyAlarmAndNotify`; `recentFiltersByDevice`; `turkishNamesRoundTrip`. `MetricsLogTest`: `constantPowerGivesKwh` (100 W, 1 saat → 0.1 ± 0.001 kWh); `usageCountsOnTime` (kanal 1 saat açık → `onMillis` ≈ 3 600 000); `pruneRemovesOlderThan400Days`.
- [ ] **Step 2: Koş, kırmızı.** `./gradlew test --tests "*EventLogTest" --tests "*MetricsLogTest"` → FAIL.
- [ ] **Step 3: Yaz** (sorgular telefondaki SQL'in aynısı).
- [ ] **Step 4: Koş, yeşil.** → PASS; `./gradlew test` tümü PASS.
- [ ] **Step 5: Commit** — "Desktop: event history and metrics on SQLite (same schema as phone)".

### Task 7: İzleme motoru (`MonitorEngine`)

**Files:**
- Create: `desktop/src/main/kotlin/com/macerce/switchguard/desktop/engine/{MonitorHost.kt, Scheduler.kt, Sources.kt, MonitorEngine.kt}`
- Test: `desktop/src/test/kotlin/com/macerce/switchguard/desktop/engine/{MonitorEngineTest.kt, Fakes.kt}`

**Interfaces:**
- Consumes: `Store`, `EventLog`, `MetricsLog`, `LiveState`, `Strings`, `api/*`, `core/*`.
- Produces:
  ```kotlin
  interface MonitorHost {
      fun showAlarm(lines: List<String>, sound: String?)
      fun ensureAlarmVisible()
      fun stopAlarm()
      fun notify(text: String, sound: String?)
      fun sessionLost(text: String, detail: String)
      fun clearWarning()
      fun statusChanged(state: ConnState, text: String)
      fun keepAwake(on: Boolean)          // motor iş parçacığında çağrılır (Windows durumu iş parçacığına bağlı)
  }
  interface Clock { fun wall(): Long; fun elapsed(): Long }           // SystemClock: currentTimeMillis / nanoTime÷1e6
  interface Scheduler { fun post(task: () -> Unit); fun postDelayed(delayMs: Long, task: () -> Unit); fun cancelAll(); fun shutdown() }
  class ExecutorScheduler(name: String) : Scheduler                 // tek iş parçacıklı ScheduledExecutorService, daemon
  interface EwelinkApi { fun getDevices(): List<DeviceSnapshot>; fun loadProfile(); fun setSwitch(d: DeviceSnapshot, channel: Int, on: Boolean); fun requestLiveEnergy(d: DeviceSnapshot, seconds: Int) }
  interface EwelinkLive { val ready: Boolean; fun connect(); fun close(); fun sendPingIfDue(now: Long); fun isStale(now: Long): Boolean }
  interface TuyaApi { val syncServerTime: Long; fun getDevices(): List<DeviceSnapshot>; fun setSwitch(d: DeviceSnapshot, channel: Int, on: Boolean); fun scalesFor(deviceId: String): Map<String, Int> }
  interface TuyaLive { val ready: Boolean; fun connect(); fun close() }
  class Sources(val ewelink: EwelinkApi, val ewelinkLive: (EwelinkSocket.Listener) -> EwelinkLive,
                val tuya: TuyaApi, val tuyaLive: (TuyaSocket.Listener) -> TuyaLive) { companion object { fun real(store: Store): Sources } }
  class MonitorEngine(store: Store, log: EventLog, metrics: MetricsLog, strings: () -> Strings, host: MonitorHost,
                      sources: Sources, clock: Clock = SystemClock, scheduler: Scheduler = ExecutorScheduler("monitor")) {
      val running: Boolean
      fun start(); fun stop(); fun dismissAlarm(); fun testAlarm(); fun syncNow(); fun restart()
      fun demo(op: String, deviceId: String, channel: Int = 0)      // DEMO_TOGGLE / DEMO_ONLINE / DEMO_POWER (telefondaki değerler)
      fun control(deviceId: String, channel: Int, on: Boolean, onResult: (Throwable?) -> Unit = {})
      fun shutdown()                                                  // monitoringEnabled'a dokunmaz (Çıkış için)
  }
  ```
- Davranış: `app/.../service/MonitorService.kt` 189–858'i **yöntem yöntem aynı adlarla** taşı. Değişimler: `handler.post/postDelayed` → `scheduler`; `SystemClock.elapsedRealtime()` → `clock.elapsed()`; `System.currentTimeMillis()` → `clock.wall()`; `getString(R.string.x, …)` → `strings().format("x", …)`; `notifier.*` → `host.*`; `AlarmActivity.ensureShown` → `host.ensureAlarmVisible()`; `applyWakeMode` → `host.keepAwake(running && store.alwaysAwake)`; `Log` → `desktop.Log`; `stop()` = `stopMonitoring` (monitoringEnabled=false, keepAwake(false)). Silinenler: `AlarmManager` tick, ağ geri çağrısı, `startForeground`, `SummaryScheduler`, `onConfigurationChanged`. `control` = telefondaki arayüz akışı (`LiveState.expected.expect` → `controlSwitch` → `onControlled`), hata `onResult`'a.
- Eklemeler: (1) `store.tuyaLive == false` → Tuya soketi hiç bağlanmaz, `TUYA_POLL_MS` ile sorgulanır. (2) Uyku: `tick` başında `gap = wall - lastTickWall`; `gap > 60 000` → `reconnectNow()`, `resetTuya()`, `sync()`; `gap ≥ 120 000` → `log.add("", appName, EventKind.SYSTEM, strings.format("desktop_slept", gap/60000), "")`. `gap ≤ 60 000` ya da negatif → hiçbir şey.

- [ ] **Step 1: `Fakes.kt`** — `FakeClock` (wall/elapsed ayrı ilerletilebilir), `ManualScheduler` (`advance(ms)` zamanı gelen işleri sırayla çalıştırır), `FakeEwelink` (değiştirilebilir cihaz listesi, `getDevices` sayacı), `FakeEwelinkLive`/`FakeTuyaLive` (`connect` sayacı, `ready`, listener'a erişim), `FakeTuya`, `FakeHost` (alarm/bildirim/durum/keepAwake kayıtları).
- [ ] **Step 2: Failing testler** (geçici klasörde gerçek `Store(PlainCodec)`, `EventLog`, `MetricsLog`, `Strings.load("en")`; kurallar açıkça `DeviceRules(actions = …)` ile verilir; her test kendi cihaz kimliğini kullanır):
  - `externalSwitchOffRaisesAlarm` — WebSocket güncellemesi kanal 0 kapalı → `host.alarms.size == 1`, satır cihaz adını içerir, son olay `action == "ALARM"`.
  - `appControlDoesNotAlarm` — `control(id, 0, false)` → alarm yok, olay `kind == EventKind.USER`.
  - `quietHoursTurnAlarmIntoNotification` — saat sessiz aralıkta → 1 bildirim, 0 alarm.
  - `shortDropWithinGraceIsLoggedNotAlarmed` — `offlineGraceSec = 60`; çevrimdışı, 30 s sonra çevrimiçi → alarm yok, `SHORT_DROP` olayı.
  - `offlineBeyondGraceAlarms` — çevrimdışı, `advance(61_000)` → 1 alarm.
  - `stayedOnAutomationFiresAfterDuration` — STAYED_ON 10 dk → ALARM otomasyonu; 9 dk'da alarm yok, 10 dk'da 1 alarm.
  - `sleepGapResyncsAndLogs` — `start`, wall +10 dk sıçrar → `ewelinkLive.connects` +1, `getDevices` +1, `SYSTEM` olayı "10" içerir.
  - `smallOrBackwardClockJumpIsIgnored` — +30 s, sonra −5 dk → `SYSTEM` olayı yok, ek `connect` yok.
  - `pendingAlarmRestoredOnStart` — `store.pendingAlarm = listOf("12:00  x")` → `start()` → `showAlarm` bu satırla çağrılır.
  - `fatalSocketCloseFallsBackToPolling` — `onClosed("x", fatal = true)` → durum `POLLING`; `pollIntervalSec` sonra `getDevices` tekrar çağrılır.
  - `tuyaLiveOffNeverConnectsSocket` — Tuya anahtarları var, `tuyaLive = false` → `tuyaLive.connects == 0`, `getDevices` 5 dk'da bir.
  - `stopClearsAlarmAndKeepAwake` — `start` → `keepAwake(true)`; `stop` → `keepAwake(false)`, `stopAlarm`, durum `STOPPED`, `!store.monitoringEnabled`.
  - `shutdownKeepsMonitoringFlag` — `start`, `shutdown` → `store.monitoringEnabled == true`.
- [ ] **Step 3: Koş, kırmızı.** `./gradlew test --tests "*MonitorEngineTest"` → FAIL.
- [ ] **Step 4: `MonitorHost`, `Scheduler`, `Sources` (gerçek bağdaştırıcılar dahil) ve `MonitorEngine`'i yaz.**
- [ ] **Step 5: Koş, yeşil.** Aynı komut → PASS; `./gradlew test` tümü PASS.
- [ ] **Step 6: Commit** — "Desktop: monitoring engine ported from MonitorService, with sleep detection".

### Task 8: Sesler

**Files:**
- Create: `desktop/tools/gen_sounds.py` (yalnızca standart kütüphane: `wave`, `math`, `struct`)
- Create (üretilmiş, commit'lenir): `desktop/src/main/resources/sounds/{alarm_siren,alarm_beep,alarm_pulse,notify_chime}.wav` — 16 bit mono 44.1 kHz; siren 2 s 600→1200 Hz süpürme; beep 3 × 880 Hz; pulse 440 Hz darbeler; chime 660 + 990 Hz, 0.6 s
- Create: `desktop/src/main/kotlin/com/macerce/switchguard/desktop/platform/Sounds.kt`
- Test: `desktop/src/test/kotlin/com/macerce/switchguard/desktop/platform/SoundsTest.kt`

**Interfaces:**
- Produces: `sealed interface SoundSource { data class Builtin(val name: String); data class File(val path: Path) }`; `object Sounds { val BUILTIN: List<String>; const val DEFAULT_ALARM = "builtin:alarm_siren"; const val DEFAULT_NOTIFY = "builtin:notify_chime"; fun resolve(id: String?, default: String): SoundSource }`; `class SoundPlayer { fun loop(id: String?, volume: Float); fun once(id: String?, volume: Float, maxMs: Long = 6_000); fun stop() }` (`javax.sound.sampled.Clip`, `MASTER_GAIN = 20·log10(volume)` alt sınıra kırpılır).
- `resolve`: boş/null → varsayılan; `builtin:<bilinen>` → Builtin; `builtin:<bilinmeyen>` → varsayılan; diğerleri dosya yolu: var, okunabilir ve `AudioSystem.getAudioFileFormat` açabiliyorsa File, yoksa varsayılan.

- [ ] **Step 1: `gen_sounds.py`'yi yaz ve çalıştır**; 4 dosya oluşur, her biri < 400 KB.
- [ ] **Step 2: Failing testler:** `nullUsesDefault`, `unknownBuiltinUsesDefault`, `missingCustomFileFallsBackToDefault`, `nonAudioFileFallsBackToDefault`, `existingWavFileIsUsed`, `everyBuiltinResourceIsReadable`.
- [ ] **Step 3: Koş, kırmızı.** `./gradlew test --tests "*SoundsTest"` → FAIL.
- [ ] **Step 4: `Sounds.kt`'yi yaz.**
- [ ] **Step 5: Koş, yeşil.** → PASS.
- [ ] **Step 6: Commit** — "Desktop: generated alarm sounds and player with safe fallback".

### Task 9: Masaüstü kabuğu — tepsi, alarm penceresi, bildirim, Cihazlar, demo

**Files:**
- Create: `desktop/src/main/kotlin/com/macerce/switchguard/desktop/{AppGraph.kt, AppState.kt}`, `.../desktop/platform/{DesktopHost.kt, TrayIcons.kt}`, `.../desktop/ui/{Theme.kt, MainWindow.kt, DevicesScreen.kt, AlarmWindow.kt}`
- Modify: `.../desktop/Main.kt`

**Interfaces:**
- Consumes: Task 3–8.
- Produces: `class AppGraph(dataDir: Path)` (Store + DpapiCodec, EventLog, MetricsLog, Strings, SoundPlayer, AppState, DesktopHost, MonitorEngine; `Log.init(dataDir)`); `dataDir = Path(System.getenv("APPDATA"), "SwitchGuard")`; `class AppState { alarmLines: StateFlow<List<String>>; status: StateFlow<Pair<ConnState, String>>; windowVisible: MutableStateFlow<Boolean> }`; `class DesktopHost(...) : MonitorHost` (alarm → `AppState.alarmLines` + `SoundPlayer.loop(sound ?: store.alarmSoundUri, store.alarmVolume)`; notify → `TrayState.sendNotification` + `SoundPlayer.once`; sessionLost → Warning bildirimi; `keepAwake` Task 10'a kadar boş).
- Davranış (spec §5): pencere kapatma → gizle (ilk seferde `tray_minimized_hint` bildirimi); tepsi menüsü Aç / İzlemeyi başlat-durdur / Test alarmı / Çıkış (izleme açıksa onay); Çıkış = `engine.shutdown()` + `exitApplication` (`monitoringEnabled` değişmez); tepsi simgesi `ConnState` rengi (yeşil LIVE, sarı CONNECTING/POLLING, kırmızı NO_INTERNET/ERROR/AUTH_ERROR, gri STOPPED); `AlarmWindow`: çerçevesiz, `alwaysOnTop`, `WindowPlacement.Maximized`, arka plan `#B71C1C`, satırlar + büyük `alarm_dismiss` düğmesi → `engine.dismissAlarm()`; `ensureAlarmVisible` → `window.toFront()`. Cihazlar: üstte durum metni + izleme başlat/durdur + test alarmı + demo aç/kapa (`store.enableDemo(strings.demoNames())` / `disableDemo()`); `store.version` ile yenilenen kart ızgarası (ad, çevrim içi etiketi, kanal anahtarları → `engine.control`; demo cihazlarında üç simülasyon düğmesi → `engine.demo`). Tema: telefondaki `ui/theme/Theme.kt` renkleri, `isSystemInDarkTheme()`.

- [ ] **Step 1: Yaz.**
- [ ] **Step 2: Derle.** `./gradlew compileKotlin` → BUILD SUCCESSFUL.
- [ ] **Step 3: Elle doğrula** (`./gradlew run`; ekran görüntüsü PowerShell `CopyFromScreen` ile alınıp incelenir):
  1. Demo aç + izleme başlat → durum "Live · 3 devices" (EN) / TR karşılığı.
  2. Dondurucu kartında "dışarıdan kapat" → tam ekran kırmızı alarm + siren döngüde; Kapat → ses durur, pencere kapanır.
  3. Pencereyi kapat → tepside kalır, ipucu bildirimi görünür; tepsi → Test alarmı → alarm penceresi.
  4. Tepsi simgesi rengi izleme durunca griye döner.
  5. Koyu ve açık Windows temasında ana pencere okunur.
- [ ] **Step 4: Commit** — "Desktop: tray, alarm window, notifications and a first Devices screen with demo".

### Task 10: Tek örnek, Windows ile başlat, uyanık tut, `--tray`

**Files:**
- Create: `desktop/src/main/kotlin/com/macerce/switchguard/desktop/platform/{SingleInstance.kt, Autostart.kt, KeepAwake.kt}`
- Modify: `.../desktop/Main.kt`, `.../desktop/platform/DesktopHost.kt`
- Test: `desktop/src/test/kotlin/com/macerce/switchguard/desktop/platform/{SingleInstanceTest.kt, AutostartTest.kt}`

**Interfaces:**
- Produces: `class SingleInstance(dir: Path) { fun acquire(onShowRequest: () -> Unit): Boolean; fun release() }` (`instance.lock` `FileChannel.tryLock`; aynı JVM'de `OverlappingFileLockException` = kilitli; sahibi 127.0.0.1 rastgele portu `instance.port`'a yazar; ikinci süreç `show\n` gönderip `false` döner); `class Autostart(valueName: String = "SwitchGuard") { val available: Boolean; fun isEnabled(): Boolean; fun setEnabled(on: Boolean, command: String? = defaultCommand()) }` + `companion fun defaultCommand(): String? = System.getProperty("jpackage.app-path")?.let { "\"$it\" --tray" }` (HKCU `Software\Microsoft\Windows\CurrentVersion\Run`, `Advapi32Util`); `object KeepAwake { fun set(on: Boolean) }` (`Kernel32.SetThreadExecutionState(ES_CONTINUOUS or ES_SYSTEM_REQUIRED)` / `ES_CONTINUOUS`).
- `Main`: `--tray` → pencere gizli başlar; tek örnek kilidi alınamazsa çık; açılışta `store.autostart && available` → kayıt değeri `defaultCommand()` ile eşitlenir (yeniden kurulum yolu düzelsin), `!store.autostart` → silinir; `store.monitoringEnabled` → `engine.start()`. `DesktopHost.keepAwake` → `KeepAwake.set`.

- [ ] **Step 1: Failing testler:** `secondAcquireSignalsFirst` (a `true`; b `false`; 2 s içinde a'nın geri çağrısı çalışır); `releaseAllowsReacquire`; `AutostartTest` (değer adı `SwitchGuardTest`, `@After` siler): `setEnabledWritesRunValue` (komut `"\"C:\\x\\SwitchGuard.exe\" --tray"` aynen okunur), `disableRemovesValue`.
- [ ] **Step 2: Koş, kırmızı.** `./gradlew test --tests "*SingleInstanceTest" --tests "*AutostartTest"` → FAIL.
- [ ] **Step 3: Yaz.**
- [ ] **Step 4: Koş, yeşil.** → PASS; `./gradlew test` tümü PASS.
- [ ] **Step 5: Elle:** izleme açıkken `powercfg /requests` çıktısında SYSTEM altında java/SwitchGuard görünür; izleme durunca kaybolur. İkinci `./gradlew run` → yeni süreç kapanır, mevcut pencere öne gelir.
- [ ] **Step 6: Commit** — "Desktop: single instance, start with Windows, keep awake, --tray".

### Task 11: Paketleme ve kurulum doğrulaması

**Files:**
- Create: `desktop/tools/make_icon.py` (PIL; `docs/brand/play-icon-512.png` → 16/24/32/48/64/128/256 px), `desktop/packaging/SwitchGuard.ico`
- Modify: `desktop/build.gradle.kts` (`windows { iconFile }`, `modules(...)`)

- [ ] **Step 1: Simgeyi üret.**
- [ ] **Step 2: Modülleri belirle.** Run: `./gradlew suggestRuntimeModules` → çıktıdaki modüller + `jdk.crypto.ec`, `jdk.localedata`, `jdk.accessibility` → `modules(...)`.
- [ ] **Step 3: Exe.** `./gradlew packageExe` → `SwitchGuard-0.1.0.exe` < 120 MB.
- [ ] **Step 4: Kur ve doğrula:** exe'yi çalıştır (soru sormadan kurulur); `%LOCALAPPDATA%\SwitchGuard\runtime\release` dosyasındaki `MODULES` satırında `jdk.localedata` var; Başlat menüsünden aç; demo → dışarıdan kapat → alarm penceresi + ses (ekran görüntüsü); TR dilde alarm satırındaki saat `HH:mm`; `HKCU\...\Run\SwitchGuard` kurulu exe'yi `--tray` ile gösterir; Çıkış → kayıttaki komutu elle çalıştır → yalnızca tepsi simgesi, izleme sürer.
- [ ] **Step 5: Kullanıcıya bırak:** bilgisayarı uygun bir anda yeniden başlattığında SwitchGuard'ın tepside kendiliğinden açıldığını kontrol etmesi.
- [ ] **Step 6: Commit** — "Desktop: app icon, trimmed runtime, installer verified".

### Task 12: Deneme B — Tuya çift tüketici

**Files:**
- Create: `desktop/tools/pulsar_consumers.py` (`tools/tuya_pulsar_probe.py` bağlantı/çözme mantığı + argümanlar `--label`, `--sub <ad>`, `--seconds`; her mesajda etiket, cihaz kimliği, zaman yazar; sonunda toplam)
- Create: `desktop/docs/spikes.md`
- Modify: `desktop/docs/spec.md` §6 (sonuca göre)

- [ ] **Step 1:** Kazıma alanında venv + `websocket-client`; anahtarlar `tuya.properties`'ten.
- [ ] **Step 2:** A ve B aynı abonelikle (`{accessId}-sub`) 5 dk eşzamanlı → mesaj sayıları. (Telefon üçüncü tüketicidir; deneme süresince telefon Tuya canlı mesajlarını kaçırabilir — eWeLink etkilenmez.)
- [ ] **Step 3:** B farklı abonelik adıyla (`{accessId}-sub-desktop`) → bağlantı kabul ediliyor mu, mesajlar iki tarafa da geliyor mu.
- [ ] **Step 4:** Sonucu `spikes.md`'ye yaz. Priz çevrimdışıysa / 5 dk'da hiç mesaj yoksa "sonuçsuz" yaz, 2. aşamada tekrarla. Farklı abonelik çalışıyorsa spec §6'yı "masaüstü kendi aboneliğiyle canlı, varsayılan açık" diye güncelle (uygulaması 2. aşama).
- [ ] **Step 5: Commit** — "Desktop: Tuya multi-consumer spike results".

### Task 13: Deneme A — eWeLink yerel dönüş adresi (yedek hesap) — **2. aşamaya ertelendi**

> 2026-10-06: Yedek eWeLink hesabının geliştirici onayı henüz gelmedi. Bu görev onay gelince (2. aşamanın başında) yapılır; 1. aşama onsuz tamamlanır.

**Files:**
- Modify: `desktop/docs/spikes.md`, gerekirse `desktop/docs/spec.md` §6

- [ ] **Step 1: Ön koşul (kullanıcı):** Yedek eWeLink hesabıyla Chrome'da dev.ewelink.cc'ye giriş. Hesabın geliştirici onayı ve kendi uygulaması (App ID/Secret) var mı bak. Yoksa başvuru yapılır, onay beklenirken Deneme A 2. aşamaya kalır — `spikes.md`'ye yaz, görevi kapat.
- [ ] **Step 2:** Yedek hesabın uygulamasında Redirect URL alanına `http://127.0.0.1:53682/callback` yazılabiliyor mu (form kabul ediyor mu)?
- [ ] **Step 3:** Kabul ediyorsa: o App ID/Secret ve bu dönüş adresiyle `buildLoginUrl` adresi tarayıcıda açılır, **yedek hesapla** giriş yapılır; `127.0.0.1:53682`'de dinleyen tek seferlik sunucu (`com.sun.net.httpserver`, kazıma alanında küçük bir JVM betiği) `code` ve `region`'u yakalıyor mu? Ardından `exchangeCode` başarılı mı?
- [ ] **Step 4:** Sonucu `spikes.md`'ye yaz; spec §6'da seçilen giriş yolunu (yerel sunucu / pano yedeği) kesinleştir.
- [ ] **Step 5: Commit** — "Desktop: eWeLink loopback redirect spike results".
