# 2. iş: Ayarlar, hesaplar, sesler, şifreli yedek, sürükle-bırak — Uygulama planı

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bilgisayardaki SwitchGuard gerçek Tuya/eWeLink hesaplarıyla, kendi alarm sesiyle, şifreli yedekle, sürükle-bırak
cihaz düzeniyle ve uygulama içi kurulum anlatımlarıyla kullanılabilsin.

**Architecture:** Mantık arayüzsüz, test edilen küçük birimlerde (`accounts/`, `backup/`, `sound/`, `DragOrder`);
Compose ekranları yalnızca bunları çağırır. Motor durumu yalnızca motor iş parçacığında değişir (`setRules`,
`dismissAlarm(lines)`); ağ/dosya işleri arayüz iş parçacığında asla çalışmaz.

**Tech Stack:** Kotlin 2.2.21, Compose Multiplatform 1.9.3 (Material 3), JDK 21 (Temurin), JUnit 4, org.json,
`sh.calvin.reorderable:reorderable:3.1.0`, `com.googlecode.soundlibs:mp3spi:1.9.5.4`, `javax.crypto`.

**Spec:** `desktop/docs/spec-2-ayarlar.md` (ana tasarım `desktop/docs/spec.md`)

## Global Constraints

- `app/` (telefon) değişmez; `windows` dalı birleştirilmez, hiçbir yere gönderilmez (`git push` yok).
- Derleme/test: `cd desktop && JAVA_HOME=/c/Users/macerce/.jdks/temurin-21 ./gradlew <görev>`; tam takım
  `test --rerun-tasks`, sonuç `build/test-results/test/*.xml` toplamı.
- Metinler: telefonda karşılığı olan satırlar telefon anahtarını kullanır; yalnızca masaüstüne özgüler
  `src/main/resources/strings/values{,-tr}/desktop.xml`'e EN + TR eklenir; masaüstü anahtarı telefon anahtarını gölgeleyemez
  (`StringsTest.desktopKeysNeverShadowPhoneKeys`), EN/TR biçim belirteçleri aynı olmalı (`englishAndTurkishTakeTheSameFormatArguments`).
- Gizli bilgi (şifre, App/Access Secret, token, OAuth kodu) günlüğe yazılmaz.
- Ağ ve dosya işleri `Dispatchers.IO`'da; sonuç arayüze `withContext(Dispatchers.Main)`/state ile döner.
- Kod yorumları Türkçe (depo alışkanlığı); commit mesajı İngilizce `Desktop: …` + `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Yedek: PBKDF2WithHmacSHA256 600 000 yineleme, 16 bayt salt, AES-256-GCM 12 bayt IV, 128 bit etiket; şifre ≥ 8
  karakter; dosya ≤ 100 MB; uzantı `.sgbackup`; `format = "switchguard-backup"`, `version = 1`.
- Ses yükleme: kaynak ≤ 20 MB, `.wav`/`.mp3`, çıktı 16 bit PCM WAV, ≤ 48 kHz, ≤ 60 sn, `sounds\<12 hex>-<ad>.wav`.
- Sürüm 0.2.0 (Görev 14).

## Review Focus

1. 24 bit / 96 kHz / float WAV ya da stereo MP3 seçilir → hata değil, 16 bit ≤ 48 kHz WAV'a çevrilir (Görev 4 testi).
2. Türkçe karakterli ve boşluklu dosya adları (`Masaüstü\alarm sesi ğş.mp3`, yedek yolu) → sorunsuz (Görev 4, 10 testleri).
3. Pano başka bir programca kilitli ya da metin değil → pano izleyicisi çökmez, sessizce sonraki denemeyi bekler (Görev 8 testi).
4. Sürükleme sürerken motor cihaz listesini değiştirir (cihaz eklenir/çıkar) → sıra zıplamaz, çökme yok (Görev 12 testi).
5. Türkçe karakterli şifre (`Şifreğı1`) → aynı şifreyle başka bilgisayarda açılır (Görev 9 testi).

---

### Task 1: "Kapat" sireni hemen susturur (I6-a)

**Files:**
- Modify: `desktop/src/main/kotlin/com/macerce/switchguard/desktop/platform/DesktopHost.kt`
- Modify: `desktop/src/main/kotlin/com/macerce/switchguard/desktop/engine/MonitorEngine.kt` (`dismissAlarm`)
- Modify: `desktop/src/main/kotlin/com/macerce/switchguard/desktop/AppGraph.kt`, `ui/AlarmWindow.kt`, `Main.kt` (tepsi)
- Test: `desktop/src/test/kotlin/com/macerce/switchguard/desktop/platform/DesktopHostTest.kt`, `engine/MonitorEngineTest.kt`

**Interfaces:**
- Produces: `DesktopHost.silenceNow(lines: List<String>)` (arayüz iş parçacığı; ses durur, pencere kapanır, `silenced = lines`);
  `MonitorEngine.dismissAlarm(lines: List<String>)` (eski parametresiz sürümün yerine); `AppGraph.dismissAlarm()`
  (`appState.alarmLines.value` ile ikisini çağırır). Alarm penceresi düğmesi ve tepsi "Alarmı kapat" `graph.dismissAlarm()` çağırır.

- [ ] **Step 1: Testleri yaz**
  - `DesktopHostTest.silenceNowStopsSoundAndWindowAtOnce`: `alarm("12:00  a")` → `host.silenceNow(listOf("12:00  a"))` →
    `sound.stops == 1`, `state.alarmLines.value` boş.
  - `DesktopHostTest.tickAfterSilenceDoesNotBringTheDismissedAlarmBack`: alarm → `silenceNow` (store.pendingAlarm hâlâ dolu) →
    `ensureAlarmVisible()` → `sound.loops.size == 1`, `alarmLines` boş.
  - `DesktopHostTest.newAlarmAfterSilenceRingsAgain`: alarm a → `silenceNow([a])` → `alarm(a, b)` → `alarmLines == [a, b]`, ses çalıyor.
  - `MonitorEngineTest.dismissKeepsALineThatArrivedAfterTheClick`: alarm satırı L1 → `engine.dismissAlarm(listOf(L1))` gönderilmeden
    önce motor kuyruğunda yeni bir değişiklik L2 alarmı (sahte bulutla) → `scheduler.advance(0)` → `store.pendingAlarm == [L2]`,
    `host.alarms.last().lines == [L2]` (sahte host'ta son showAlarm).
  - `MonitorEngineTest.dismissOfAllLinesStopsTheAlarm`: `dismissAlarm(pending)` → `pendingAlarm` boş, `host.stopped` ≥ 1.
- [ ] **Step 2: Çalıştır, kırmızıyı gör** — `./gradlew test --tests '*DesktopHostTest' --tests '*MonitorEngineTest'` → yeni testler FAIL (derleme hatası değil: önce imzaları boş gövdeyle ekle).
- [ ] **Step 3: Uygula** — `silenced` alanı `showAlarm` ile temizlenir; `ensureAlarmVisible` `pending == silenced` iken hiçbir şey yapmaz.
  Motor: `pendingAlarm` başı `lines` ise `drop(lines.size)`, değilse her satırı bir kez çıkar; kalan boşsa `host.stopAlarm()`, değilse `host.showAlarm(kalan, null)`.
- [ ] **Step 4: Yeşili gör** — aynı komut PASS.
- [ ] **Step 5: Commit** — `Desktop: Dismiss silences the siren at once, even while the engine is busy`

### Task 2: Uzun süren tick "uyku" sayılmaz (I6-b)

**Files:** Modify `engine/MonitorEngine.kt` (`tick`, `sleptFor`); Test `engine/MonitorEngineTest.kt`

**Interfaces:** Produces: yok (iç davranış). Kural: boşluk = duvar saati farkı − bir önceki tick'in kendi süresi (`clock.elapsed()` ile ölçülür); `> SLEEP_GAP_MS` ise uyku.

- [ ] **Step 1: Test** `blockedCloudCallIsNotReportedAsSleep`: sahte eWeLink `getDevices` çağrısı içinde `clock.wallMs += 90_000` ve `clock.elapsedMs += 90_000` (90 sn tıkanma) → sonraki tick → `log.recent("")`'de `EventKind.SYSTEM` yok ve `ewelinkLive.connects` artmamış. Mevcut `sleep*` testleri (gerçek uyku: yalnız duvar saati atlar) değişmeden geçmeli.
- [ ] **Step 2:** Kırmızı — yeni test FAIL (SYSTEM kaydı var).
- [ ] **Step 3:** `tick` başında `val startedAt = clock.elapsed()`, sonunda `lastTickBusyMs = clock.elapsed() - startedAt` (erken `return`'ler dahil: `try/finally`); `sleptFor` boşluktan `lastTickBusyMs` düşer. Yorumda: "uyku bir tick'in ortasında olursa kaçabilir; nadir, bir sonraki senkron yakalar".
- [ ] **Step 4:** Yeşil — `--tests '*MonitorEngineTest'` PASS.
- [ ] **Step 5: Commit** — `Desktop: a slow cloud call is not reported as the computer sleeping`

### Task 3: Geçici DPAPI hatası çıkış sayılmaz (M6)

**Files:** Modify `data/Store.kt` (`secret`); Test `data/StoreTest.kt`

- [ ] **Step 1: Test** `secretThatFailsToDecodeOnceIsRetried`: ilk `decode` çağrısında null, sonrakinde değer döndüren sahte `SecretCodec` → `store.accessToken == "tok"`; her zaman null dönen codec → `""` ve günlükte `W/Store: could not decode` (anahtar adı var, değer yok).
- [ ] **Step 2:** Kırmızı.
- [ ] **Step 3:** `secret()` null'da 50 ms bekleyip bir kez yeniden dener; yine null ise `Log.w(TAG, "could not decode secret '$key'")` ve `""`.
- [ ] **Step 4:** Yeşil — `--tests '*StoreTest'`.
- [ ] **Step 5: Commit** — `Desktop: retry a secret that DPAPI fails to decode once`

### Task 4: Ses yükleme (WAV/MP3 → WAV) ve `user:` sesleri

**Files:**
- Create: `desktop/src/main/kotlin/com/macerce/switchguard/desktop/sound/SoundImport.kt`
- Modify: `platform/Sounds.kt` (`USER_PREFIX`, `resolve(id, default, userDir)`, `SoundPlayer(userDir)`, `displayName`), `AppGraph.kt` (`SoundPlayer(dataDir.resolve("sounds"))`), `data/Store.kt` (`notifySoundUri`, anahtar `notifySound`), `platform/DesktopHost.kt` (`notify`: `sound ?: store.notifySoundUri`), `build.gradle.kts` (`implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4")`), `desktop/SYNC.md` (Store satırı farklarına `notifySound`)
- Create test fixture: `desktop/src/test/resources/sounds/beep-1s.mp3` (ffmpeg: `ffmpeg -f lavfi -i "sine=frequency=880:duration=1" -ac 2 -ar 44100 -b:a 64k beep-1s.mp3`)
- Test: `desktop/src/test/kotlin/com/macerce/switchguard/desktop/sound/SoundImportTest.kt`, `platform/SoundsTest.kt`

**Interfaces:**
- Produces: `class SoundImport(userDir: Path)` · `fun import(source: Path): ImportResult` ·
  `sealed interface ImportResult { data class Ok(val id: String, val trimmed: Boolean); data class Failed(val reason: Reason, val detail: String) }` ·
  `enum class Reason { TOO_BIG, NOT_AUDIO, IO }` · `fun list(): List<String>` (`user:` kimlikleri, ada göre) ·
  `Sounds.USER_PREFIX = "user:"` · `Sounds.displayName(id: String, strings: Strings): String` (yerleşikler için `sound_builtin_<ad>`, `user:` için dosya adındaki `<ad>` kısmı) ·
  `Store.notifySoundUri: String?`.

- [ ] **Step 1: Testler**
  - `importsAWavAs16BitPcm`: testte `AudioSystem.write` ile 2 sn 44.1 kHz 16 bit mono WAV → `Ok`, `id` `user:` ile başlar, `userDir`'de dosya, `AudioSystem.getAudioFileFormat` 16 bit.
  - `convertsHighResWav`: 24 bit 96 kHz stereo WAV → `Ok`, çıktı 16 bit, örnekleme ≤ 48 000.
  - `importsMp3`: `beep-1s.mp3` → `Ok`, çıktı WAV ~1 sn (kare/örnekleme 0.9–1.1).
  - `trimsTo60Seconds`: 61 sn WAV → `Ok(trimmed = true)`, çıktı kare sayısı `60 * rate`.
  - `rejectsNonAudio`: metin dosyası `.mp3` adıyla → `Failed(NOT_AUDIO)`, `userDir` boş.
  - `rejectsOver20Mb`: 20 MB + 1 bayt seyrek dosya → `Failed(TOO_BIG)`.
  - `sameSoundTwiceIsOneFile`: aynı WAV iki kez → aynı `id`, `userDir`'de tek dosya.
  - `turkishFileNameKeepsItsName`: `alarm sesi ğş.wav` → `id` `-alarm sesi ğş.wav` ile biter; `displayName` = `alarm sesi ğş`.
  - `SoundsTest.userIdResolvesInsideUserDir`: `resolve("user:<dosya>", DEFAULT_ALARM, userDir)` → `SoundSource.File(userDir/<dosya>)`; `user:../x.wav` ve `user:a\b.wav` → varsayılan.
  - `DesktopHostTest.notifyUsesTheNotificationSound`: `store.notifySoundUri = "builtin:alarm_beep"`, `host.notify("t", null)` → `sound.onces == ["builtin:alarm_beep"]`; cihaz sesi verilirse o.
- [ ] **Step 2:** Kırmızı (`SoundImport` boş gövdeyle derlenir, testler FAIL).
- [ ] **Step 3:** Uygula — `AudioSystem.getAudioInputStream(file)` (mp3spi ServiceLoader ile MP3'ü tanır) → hedef `PCM_SIGNED`, 16 bit, aynı kanal, `min(rate, 48000)`, little-endian (`AudioSystem.getAudioInputStream(target, src)`; doğrudan dönüşüm yoksa önce aynı hızda 16 bit PCM'e, sonra hıza) → en çok `60 * rate` kare oku → bellekteki baytlardan `AudioSystem.write(…, WAVE, tmp)` → ad `<sha256(çıktı) ilk 12 hex>-<güvenli ad>.wav` (güvenli ad: uzantısız kaynak adı, harf/rakam/boşluk/`-_` dışındakiler `_`, en çok 40 karakter) → `ATOMIC_MOVE`. Hata günlüğünde dosya adı var, içerik yok.
- [ ] **Step 4:** Yeşil — `--tests '*SoundImportTest' --tests '*SoundsTest' --tests '*DesktopHostTest'`.
- [ ] **Step 5: Commit** — `Desktop: load alarm sounds from WAV/MP3 files into the app's own folder`

### Task 5: Sol menü ve Ayarlar iskeleti (İzleme, Alarm, Genel, Hakkında)

**Files:**
- Create: `ui/AppNav.kt`, `ui/settings/SettingsScreen.kt`, `ui/settings/SettingsRows.kt` (satır bileşenleri: başlık, anahtarlı satır, seçimli satır)
- Modify: `AppState.kt` (`val tab = MutableStateFlow(Tab.DEVICES)`, `enum class Tab { DEVICES, SETTINGS }`), `ui/MainWindow.kt` (içerik `AppNav`), `build.gradle.kts` (`testImplementation(compose.desktop.uiTestJUnit4)`), strings
- Test: `desktop/src/test/kotlin/com/macerce/switchguard/desktop/ui/SettingsScreenTest.kt` (`runComposeUiTest`)

**Interfaces:**
- Produces: `@Composable fun AppNav(graph: AppGraph, strings: Strings)`; `@Composable fun SettingsScreen(graph: AppGraph, strings: Strings)`
  (Görev 6–11 kendi bölümlerini buraya ekler: `AccountsSection`, `SoundRows`, `BackupSection`, `HelpSection`);
  `fun applyEngineSettings(graph: AppGraph)` = izleme açıksa `engine.restart()`; `fun appVersion(): String` = `System.getProperty("jpackage.app-version") ?: "dev"`.
- Satırlar: Sorgulama aralığı seçenekleri 15/30/60/120/300 sn (`poll_interval`, `poll_interval_value`); Çevrimdışı tolerans Kapalı/30/60/120/300 sn
  (`offline_grace`, `grace_off`, `seconds_n`); Bilgisayarı uyanık tut (`desktop_keep_awake`); Windows ile başlat (`desktop_autostart`, `Autostart().setEnabled`);
  Ses düzeyi kaydırıcı %5–100 (`alarm_volume`); Sessiz saatler aç/kapa + başlangıç/bitiş saat:dakika (15 dk adım; `quiet_hours`, `quiet_start`, `quiet_end`);
  Test alarmı (`test_alarm`, `test_alarm_desc`); Dil Sistem/Türkçe/English (`language`, `desktop_language_system`); Tema Sistem/Açık/Koyu (`desktop_theme*`);
  Sürüm (`version_x`), Günlük klasörünü aç (`desktop_open_logs`; burada doğrudan `java.awt.Desktop.getDesktop().open(logs)`, Görev 7 bunu `Links.openFolder`'a taşır). Hesaplar bölümü şimdilik yalnızca Demo cihazları (`demo_devices`, `demo_devices_desc`).

| anahtar | EN | TR |
|---|---|---|
| `desktop_keep_awake` | Keep the computer awake | Bilgisayarı uyanık tut |
| `desktop_keep_awake_desc` | While monitoring, the computer does not go to sleep | İzleme açıkken bilgisayar uykuya geçmez |
| `desktop_autostart` | Start with Windows | Windows ile başlat |
| `desktop_autostart_desc` | Opens in the tray when you sign in to Windows | Windows'ta oturum açınca tepside açılır |
| `desktop_language_system` | System language | Sistem dili |
| `desktop_theme` | Theme | Tema |
| `desktop_theme_system` | System | Sistem |
| `desktop_theme_light` | Light | Açık |
| `desktop_theme_dark` | Dark | Koyu |
| `desktop_open_logs` | Open the log folder | Günlük klasörünü aç |
| `desktop_section_backup` | Backup | Yedekleme |
| `desktop_section_help` | Help and setup | Yardım ve kurulum |

- [ ] **Step 1: Testler** (`runComposeUiTest { setContent { SettingsScreen(graph, strings) } }`, `AppGraph(tmp)`):
  `autostartSwitchWritesTheSetting` (tıkla → `store.autostart == false`), `pollIntervalChoiceIsSaved` (60 sn seç → `pollIntervalSec == 60`),
  `themeChoiceIsSaved` (Koyu → `store.theme == "dark"`), `volumeSliderStaysWithinLimits` (en sola → `alarmVolume == 0.05f`).
- [ ] **Step 2:** Kırmızı.
- [ ] **Step 3:** Uygula. `NavigationRail` öğeleri `tab_devices` / `tab_settings` (telefon anahtarları), seçili sekme `appState.tab`.
- [ ] **Step 4:** Yeşil — `--tests '*SettingsScreenTest' --tests '*StringsTest'`.
- [ ] **Step 5: Commit** — `Desktop: left navigation and a Settings page (monitoring, alarm, general, about)`

### Task 6: Alarm ve bildirim sesi seçici

**Files:** Create `ui/settings/SoundPicker.kt`; Modify `ui/settings/SettingsScreen.kt` (Alarm bölümüne iki satır); strings; Test `ui/SoundPickerTest.kt`

**Interfaces:**
- Consumes: `SoundImport` (Görev 4), `SoundPlayer.once`, `Sounds.BUILTIN`, `Sounds.displayName`.
- Produces: `@Composable fun SoundPickerDialog(graph: AppGraph, strings: Strings, current: String?, default: String, onPick: (String) -> Unit, onDismiss: () -> Unit)`;
  `fun chooseSoundFile(owner: java.awt.Window?, strings: Strings): Path?` (`java.awt.FileDialog` LOAD, `*.wav;*.mp3`).
- Liste: yerleşikler (alarm için `alarm_*`, bildirim için `notify_chime` önce) + `SoundImport.list()`; her satırda ▶ (`desktop_sound_play`) ve seçim; "Dosyadan yükle…" içe aktarır, başarıda yeni sesi seçer, `trimmed` ise bilgi, `Failed` ise hata metni.

| anahtar | EN | TR |
|---|---|---|
| `desktop_notify_sound` | Notification sound | Bildirim sesi |
| `desktop_sound_load` | Load from file… | Dosyadan yükle… |
| `desktop_sound_play` | Play | Dinle |
| `desktop_sound_trimmed` | The sound is longer than 60 seconds; its first 60 seconds are used. | Ses 60 saniyeden uzun; ilk 60 saniyesi kullanılıyor. |
| `desktop_sound_too_big` | The file is larger than 20 MB. | Dosya 20 MB'tan büyük. |
| `desktop_sound_not_audio` | This file could not be read as a sound (WAV or MP3). | Bu dosya ses olarak okunamadı (WAV ya da MP3). |
| `desktop_sound_io` | The sound could not be saved (%1$s). | Ses kaydedilemedi (%1$s). |
| `sound_builtin_alarm_siren` | Siren | Siren |
| `sound_builtin_alarm_beep` | Beep | Bip |
| `sound_builtin_alarm_pulse` | Pulse | Nabız |
| `sound_builtin_notify_chime` | Chime | Çan |

- [ ] **Step 1: Test** `pickingABuiltInSoundSavesIt` (Bip seç → `alarmSoundUri == "builtin:alarm_beep"`), `importedSoundsAreListed` (`userDir`'e Görev 4 ile bir ses → listede adı görünür).
- [ ] **Step 2–4:** Kırmızı → uygula → yeşil.
- [ ] **Step 5: Commit** — `Desktop: choose the alarm and notification sound, or load one from a file`

### Task 7: Bağlantılar ve kurulum adımları (ortak parçalar)

**Files:** Create `platform/Links.kt`, `ui/settings/SetupSteps.kt`; strings; Test `platform/LinksTest.kt`

**Interfaces:**
- Produces: `object Links { const val TUYA_PLATFORM = "https://platform.tuya.com/cloud"; const val EWELINK_DEV = "https://dev.ewelink.cc"; const val TUYA_VIDEO = "https://youtu.be/sCdvMs7CSic"; const val EWELINK_VIDEO = "https://youtu.be/jK_KcG2OPao"; fun guide(cloud: String, language: String): String; fun open(url: String, copy: (String) -> Unit = ::copyToClipboard): Boolean; fun openFolder(dir: Path): Boolean; fun copyToClipboard(text: String) }`
  — `guide("tuya","tr") == "https://github.com/Macerce/switchguard/blob/master/docs/guide/tuya.tr.md"`; dil `tr` dışı → `en`.
  `open` `Desktop.browse` başarısızsa `copy(url)` çağırır ve false döner (arayüz "Adres panoya kopyalandı" der: `desktop_link_copied`).
- `@Composable fun SetupSteps(title: String, steps: List<String>, links: List<Pair<String, String>>, strings: Strings)` — katlanır kart, numaralı adımlar, bağlantı düğmeleri.
- Adım metinleri (`desktop_tuya_step_1..6`, `desktop_ewelink_step_1..6`, `desktop_windows_step_1..7`) spec §7a'daki adımların EN + TR cümleleridir;
  düğmeler `desktop_open_tuya_platform`, `desktop_open_ewelink_dev`, `desktop_copy_redirect`, `desktop_open_guide`, `desktop_watch_video`, `desktop_link_copied`, `desktop_how_to_setup` (Nasıl kurulur? / How to set up).

- [ ] **Step 1: Test** `guideUrlFollowsTheLanguage` (tr/en/de→en), `failedBrowseCopiesTheAddress` (`Desktop` yerine verilen tarayıcı işlevi atar → `copy` çağrıldı, `false`).
  `Links.open` iç tarayıcı çağrısı test için `browse: (URI) -> Unit` parametresiyle verilir.
- [ ] **Step 2–4:** Kırmızı → uygula (Görev 5'teki doğrudan `Desktop.open` çağrısı `Links.openFolder`'a taşınır) → yeşil (`--tests '*LinksTest' --tests '*StringsTest'`).
- [ ] **Step 5: Commit** — `Desktop: links and step-by-step setup texts for Tuya, eWeLink and Windows`

### Task 8: Tuya hesabı ekle / kaldır

**Files:** Create `accounts/TuyaConnect.kt`, `ui/settings/AccountsSection.kt` (Tuya kısmı + Demo satırı buraya taşınır), `ui/settings/TuyaDialog.kt`; strings; Test `accounts/TuyaConnectTest.kt`

**Interfaces:**
- Produces: `class TuyaConnect(store: Store, probe: () -> Int = { TuyaClient(store).test() })` ·
  `fun connect(accessId: String, secret: String, region: String): TuyaResult` · `fun disconnect()` ·
  `sealed interface TuyaResult { data class Connected(val devices: Int); data object AuthError; data object Expired; data object NoInternet; data class Failed(val message: String) }`
  (`TuyaAuthException`→AuthError, `TuyaExpiredException`→Expired, `IOException`→NoInternet). Başarısızlıkta `tuyaAccessId/tuyaSecret/tuyaRegion/tuyaToken` önceki değerlerine döner.
- Arayüz: satır `tuya_account` + `tuya_connected_x` / `desktop_tuya_not_connected`; "Bağla" penceresi `tuya_dialog_title`, `tuya_dialog_desc`, alanlar (Access ID, Access Secret gizli), `tuya_region` + `tuya_region_eu/us/cn/in`, `tuya_checking`, sonuç `tuya_connected_ok` / `tuya_connected_none` + `tuya_shared_hint`, hatalar `tuya_auth_error` / `tuya_expired` / `desktop_no_internet`; altında `SetupSteps` (Tuya adımları + `TUYA_PLATFORM`, `guide("tuya")`, `TUYA_VIDEO`).
  Başarıda `engine.restart()`. "Bağlantıyı kaldır" (`tuya_disconnect`) onay `desktop_tuya_disconnect_confirm` → `disconnect()`; `store.hasAnyAccount` false ise `engine.stop()`, değilse `applyEngineSettings`.
  "Tuya canlı bildirimleri bu bilgisayarda" anahtarı (`desktop_tuya_live`, `desktop_tuya_live_desc`); açarken onay `desktop_tuya_live_warning`, sonra `applyEngineSettings`.

| anahtar | EN | TR |
|---|---|---|
| `desktop_tuya_not_connected` | Not connected | Bağlı değil |
| `desktop_no_internet` | No internet connection. | İnternet bağlantısı yok. |
| `desktop_tuya_disconnect_confirm` | Remove the Tuya connection? Tuya devices will no longer be monitored on this computer. | Tuya bağlantısı kaldırılsın mı? Tuya cihazları bu bilgisayarda artık izlenmez. |
| `desktop_tuya_live` | Tuya live updates on this computer | Tuya canlı bildirimleri bu bilgisayarda |
| `desktop_tuya_live_desc` | Off: Tuya devices are checked every 5 minutes | Kapalı: Tuya cihazları 5 dakikada bir sorgulanır |
| `desktop_tuya_live_warning` | Your phone may then miss some or all of its live Tuya messages. Turn it on anyway? | Telefonunuz bu durumda canlı Tuya mesajlarının bir kısmını veya tamamını alamayabilir. Yine de açılsın mı? |

- [ ] **Step 1: Testler** `successSavesTheKeysAndCountsDevices` (probe 3 → `Connected(3)`, store'da yeni değerler), `authErrorRestoresThePreviousKeys` (önceden bağlı A anahtarları; probe `TuyaAuthException` → `AuthError`, store'da yine A), `noInternetIsReportedAndRestores`, `disconnectClearsTheKeys`.
- [ ] **Step 2–4:** Kırmızı → uygula → yeşil.
- [ ] **Step 5: Commit** — `Desktop: connect and remove the Tuya account from Settings`

### Task 9: eWeLink hesabı (API bilgileri, tarayıcıyla giriş, çıkış, kaldır)

**Files:** Create `accounts/EwelinkLogin.kt`, `platform/ClipboardWatcher.kt`, `ui/settings/EwelinkDialogs.kt`; Modify `ui/settings/AccountsSection.kt`; strings; Test `accounts/EwelinkLoginTest.kt`, `platform/ClipboardWatcherTest.kt`

**Interfaces:**
- Produces: `object EwelinkRedirect { fun newState(): String (32 hex, SecureRandom); fun parse(text: String, redirectUrl: String, expectedState: String): Parsed }` ·
  `sealed interface Parsed { data class Code(val code: String, val region: String?); data object WrongState; data object NotRedirect }` ·
  `class EwelinkLogin(store: Store, exchange: (String, String) -> Unit = { c, r -> EwelinkClient(store).exchangeCode(c, r) })` ·
  `fun complete(code: String, region: String?): LoginResult` (`region ?: store.region`) ·
  `sealed interface LoginResult { data object Ok; data class Rejected(val message: String); data object NoInternet }` ·
  `fun removeAccount()` (`store.logout()`, `appId = ""`, `appSecret = ""`) ·
  `class ClipboardWatcher(read: () -> String?, onText: (String) -> Unit) { fun poll() }` (değişmeyen metinde tekrar çağırmaz; `read` atarsa yutar) ·
  `fun readClipboardText(): String?` (AWT pano, `IllegalStateException`/`UnsupportedFlavorException` → null).
- Ayrıştırma: metin kırpılır; `redirectUrl` ile başlamalı (sondaki `/` farkı ve `http`→`https` eşdeğer değildir: birebir önek, isteğe bağlı tek `/`); sorgu `code` zorunlu, `state` beklenenle aynı değilse `WrongState`.
- Arayüz: satır `ewelink` başlığı + durum (`desktop_ewelink_keys_missing` / `not_logged_in` / `desktop_ewelink_logged_in_x` maskeli e-posta + bölge), her zaman görünen uyarı `desktop_ewelink_pc_notice`;
  "API bilgileri" (`api_credentials`; App ID, App Secret gizli, Redirect URL + `redirect_hint`, bölge eu/us/as/cn); "Giriş yap" (`action_login`) penceresi: `desktop_ewelink_login_steps`, tarayıcıda açar (`Links.open(EwelinkClient(store).buildLoginUrl(state))`), `LaunchedEffect` döngüsünde 500 ms'de bir `watcher.poll()`, yapıştırma alanı `desktop_ewelink_paste_hint`, hatalar `desktop_ewelink_wrong_address`, `desktop_ewelink_login_failed`, `desktop_no_internet`; başarıda pencere kapanır, `engine.restart()`.
  "Çıkış yap" (`action_logout`, onay `logout_title`/`logout_body`) → `store.logout()` + `applyEngineSettings`/`stop`; "Hesabı kaldır" (`desktop_ewelink_remove`, onay `desktop_ewelink_remove_confirm`) → `removeAccount()`. `SetupSteps` (eWeLink adımları + `EWELINK_DEV`, `desktop_copy_redirect`, `guide("ewelink")`, `EWELINK_VIDEO`).

| anahtar | EN | TR |
|---|---|---|
| `desktop_ewelink_pc_notice` | This computer is a separate device: it needs its own eWeLink account, developer access approved on dev.ewelink.cc, and that account's App ID and App Secret. | Bu bilgisayar ayrı bir cihazdır: kendi eWeLink hesabı, o hesapta dev.ewelink.cc geliştirici onayı ve o hesabın App ID / App Secret bilgisi gerekir. |
| `desktop_ewelink_keys_missing` | No API credentials yet | API bilgileri henüz girilmedi |
| `desktop_ewelink_logged_in_x` | Signed in · %1$s · region %2$s | Giriş yapıldı · %1$s · bölge %2$s |
| `desktop_ewelink_login_steps` | 1. Sign in on the page that opened in your browser.\n2. The browser then shows a "can't connect" page; that is expected.\n3. Copy the address from the address bar. SwitchGuard picks it up by itself. | 1. Tarayıcıda açılan sayfada giriş yapın.\n2. Tarayıcı ardından "bağlanılamıyor" sayfası gösterir; bu normaldir.\n3. Adres çubuğundaki adresi kopyalayın. SwitchGuard kendiliğinden alır. |
| `desktop_ewelink_paste_hint` | Or paste the address here | Ya da adresi buraya yapıştırın |
| `desktop_ewelink_open_again` | Open the sign-in page again | Giriş sayfasını yeniden aç |
| `desktop_ewelink_wrong_address` | This address does not belong to this sign-in. Open the sign-in page again. | Bu adres bu girişe ait değil. Giriş sayfasını yeniden açın. |
| `desktop_ewelink_login_failed` | eWeLink did not accept the sign-in (%1$s). | eWeLink girişi kabul etmedi (%1$s). |
| `desktop_ewelink_remove` | Remove account | Hesabı kaldır |
| `desktop_ewelink_remove_confirm` | Sign out and delete the App ID and App Secret from this computer? eWeLink devices will no longer be monitored here. | Oturum kapatılıp App ID ve App Secret bu bilgisayardan silinsin mi? eWeLink cihazları burada artık izlenmez. |

- [ ] **Step 1: Testler** `parsesCodeRegionAndState` (`https://127.0.0.1/?code=abc&region=eu&state=S` → `Code("abc","eu")`; `/` olmadan da), `wrongStateIsRefused`, `otherAddressIsNotARedirect` (`https://example.com/?code=x&state=S`, `http://127.0.0.1/?…`), `missingRegionUsesTheStoredRegion` (complete), `apiRejectionIsReported` (exchange `ApiException(…)` → `Rejected`), `noInternetIsReported`, `removeAccountDeletesKeysAndSession`;
  `ClipboardWatcherTest.sameTextIsReportedOnce`, `lockedClipboardIsIgnored` (`read` `IllegalStateException` atar → `onText` çağrılmaz, sonraki poll normal), `nullIsIgnored`.
- [ ] **Step 2–4:** Kırmızı → uygula → yeşil.
- [ ] **Step 5: Commit** — `Desktop: eWeLink credentials, browser sign-in with clipboard pickup, sign out and remove`

### Task 10: Yedek çekirdeği (şifreleme + içerik)

**Files:** Create `backup/BackupCrypto.kt`, `backup/BackupContent.kt`; Modify `data/Store.kt` (`exportSettings`, `replaceSettings`), `desktop/SYNC.md` (Store farkları); Test `backup/BackupCryptoTest.kt`, `backup/BackupContentTest.kt`, `data/StoreTest.kt`

**Interfaces:**
- Produces: `object BackupCrypto { fun seal(plain: ByteArray, password: CharArray, created: java.time.Instant, iterations: Int = 600_000): ByteArray; fun open(sealed: ByteArray, password: CharArray): ByteArray }` ·
  `class BackupException(val kind: Kind, cause: Throwable? = null) : Exception(kind.name, cause)` · `enum class Kind { NOT_BACKUP, TOO_NEW, WRONG_PASSWORD_OR_DAMAGED, TOO_BIG, IO }` ·
  `object BackupContent { val KEYS: Set<String>; val CLEARED: Set<String>; fun build(store: Store, userDir: Path, appVersion: String): JSONObject; fun summary(content: JSONObject): Summary; fun soundIds(settings: JSONObject): Set<String> }` ·
  `data class Summary(val created: String, val appVersion: String, val tuya: Boolean, val ewelinkKeys: Boolean, val rules: Int, val automations: Int, val sounds: Int)` ·
  `Store.exportSettings(keys: Set<String>): JSONObject` (gizliler çözülmüş) · `Store.replaceSettings(values: JSONObject, keys: Set<String>, clear: Set<String>)` (tek `edit`, gizliler kodlanır; `keys` ∪ `clear` önce silinir, sonra `values ∩ keys` yazılır).
- `KEYS` = spec §6.2 "Girenler" listesi birebir (+ `notifySound`); `CLEARED` = spec §6.2 "Girmeyenler" `Store` anahtarları (`at, rt, apikey, email, sessionLost, tuyaToken, tuyaTokenExp, snapshots, pendingAlarm, historySeen, transferReceived, transferSource`).
- Zarf alanları ve AAD: spec §6.1; AAD = `"$format|$version|$created|$kdfName|$iterations|$saltB64|$cipherName|$ivB64"` UTF-8. `open`: JSON değil ya da `format` farklı → `NOT_BACKUP`; `version > 1` → `TOO_NEW`; `AEADBadTagException` → `WRONG_PASSWORD_OR_DAMAGED`.

- [ ] **Step 1: Testler** (testlerde `iterations = 1_000`):
  `sealedDataOpensWithThePassword`, `wrongPasswordIsRefused`, `turkishPasswordWorks` (`"Şifreğı1"`), `changedEnvelopeFieldIsDetected` (`created` değiştir → `WRONG_PASSWORD_OR_DAMAGED`), `newerVersionIsRefused` (`version: 2` → `TOO_NEW`), `notABackupIsRefused` (rastgele JSON / metin);
  `BackupContentTest.containsKeysAndSecretsButNoSession` (Tuya secret, App Secret içeride düz; `at`, `rt`, `tuyaToken`, `snapshots`, `pendingAlarm` yok),
  `embedsUserSoundsReferencedAnywhere` (`alarmSound`, `notifySound` ve bir kuraldaki `soundUri` `user:` → `sounds` içinde 3 kayıt; `builtin:` yok),
  `summaryCounts` (2 kural, 1 otomasyon, 1 ses);
  `StoreTest.replaceSettingsRemovesKeysMissingFromTheBackup` (yerelde `quietEnabled` var, yedekte yok → silinir; `snapshots` temizlenir), `replaceSettingsReEncodesSecrets` (diskteki `tuyaSecret` codec ile kodlu).
- [ ] **Step 2–4:** Kırmızı → uygula → yeşil.
- [ ] **Step 5: Commit** — `Desktop: encrypted backup format and its contents`

### Task 11: Yedekle / geri yükle (servis + arayüz)

**Files:** Create `backup/BackupService.kt`, `ui/settings/BackupSection.kt`; strings; Test `backup/BackupServiceTest.kt`

**Interfaces:**
- Consumes: Görev 10.
- Produces: `class BackupService(store: Store, userDir: Path, settingsFile: Path, appVersion: String, clock: () -> Long = System::currentTimeMillis, iterations: Int = 600_000)` ·
  `fun write(target: Path, password: CharArray)` · `fun read(source: Path, password: CharArray): Loaded` (`> 100 MB` → `TOO_BIG`) ·
  `data class Loaded(val content: JSONObject, val summary: Summary)` · `fun apply(loaded: Loaded)`: `settings.json` → `settings.json.before-restore-<ms>` kopyası, sesler `userDir`'e (atomik), `replaceSettings(…, KEYS, CLEARED)`.
  Hata `Store`'a dokunmadan önce olursa hiçbir şey değişmez; `replaceSettings` sonrası hata olmaz (son adım). `fun defaultFileName(date: LocalDate) = "SwitchGuard-yedek-$date.sgbackup"`.
- Arayüz (`BackupSection`, Ayarlar → Yedekleme): "Dosyaya yedekle…" → `FileDialog` SAVE (varsayılan ad) → şifre ×2 penceresi (`desktop_backup_password`, `_again`, `_rules`, `_mismatch`, `_short`) → `Dispatchers.IO`'da `write` → `desktop_backup_saved_x` / `desktop_backup_failed_x`.
  "Dosyadan geri yükle…" → `FileDialog` LOAD (`*.sgbackup`) → şifre → `read` → özet penceresi (`desktop_restore_summary` + `desktop_restore_warning`, düğme `desktop_restore_confirm`) →
  `engine.stop()` → `apply` → `syncAutostart` (Main'deki işlev `internal` yapılır) → `store.monitoringEnabled && store.hasAnyAccount` ise `engine.restart()` → `desktop_restore_done` ya da eWeLink anahtarı varsa `desktop_restore_done_login`; hatalar `desktop_restore_wrong_password`, `_not_backup`, `_too_new`, `_too_big`, `desktop_restore_failed_x`.

| anahtar | EN | TR |
|---|---|---|
| `desktop_backup_create` | Back up to a file… | Dosyaya yedekle… |
| `desktop_backup_create_desc` | All settings, account keys and sounds in one password-protected file | Tüm ayarlar, hesap anahtarları ve sesler tek bir şifreli dosyada |
| `desktop_backup_restore` | Restore from a file… | Dosyadan geri yükle… |
| `desktop_backup_restore_desc` | Replaces this computer's settings with the backup | Bu bilgisayarın ayarlarını yedektekiyle değiştirir |
| `desktop_backup_password` | Password | Şifre |
| `desktop_backup_password_again` | Password again | Şifre (tekrar) |
| `desktop_backup_password_rules` | At least 8 characters. The backup cannot be opened without it, and it cannot be recovered. | En az 8 karakter. Şifre olmadan yedek açılamaz ve şifre kurtarılamaz. |
| `desktop_backup_password_mismatch` | The passwords do not match. | Şifreler aynı değil. |
| `desktop_backup_password_short` | Use at least 8 characters. | En az 8 karakter kullanın. |
| `desktop_backup_saved_x` | Backup saved: %1$s | Yedek kaydedildi: %1$s |
| `desktop_backup_failed_x` | The backup could not be saved (%1$s). | Yedek kaydedilemedi (%1$s). |
| `desktop_restore_summary` | Created %1$s with SwitchGuard %2$s\nTuya: %3$s · eWeLink keys: %4$s\n%5$d device rules · %6$d automations · %7$d sounds | %1$s tarihinde SwitchGuard %2$s ile alındı\nTuya: %3$s · eWeLink anahtarları: %4$s\n%5$d cihaz kuralı · %6$d otomasyon · %7$d ses |
| `desktop_yes` / `desktop_no` | yes / no | var / yok |
| `desktop_restore_warning` | All settings on this computer will be replaced with the backup. | Bu bilgisayardaki tüm ayarlar yedektekiyle değiştirilecek. |
| `desktop_restore_confirm` | Restore | Geri yükle |
| `desktop_restore_done` | Settings restored. | Ayarlar geri yüklendi. |
| `desktop_restore_done_login` | Settings restored. Sign in to eWeLink again in Settings. | Ayarlar geri yüklendi. eWeLink'e Ayarlar'dan yeniden giriş yapın. |
| `desktop_restore_wrong_password` | Wrong password or damaged file. | Şifre yanlış ya da dosya bozuk. |
| `desktop_restore_not_backup` | This is not a SwitchGuard backup. | Bu bir SwitchGuard yedeği değil. |
| `desktop_restore_too_new` | This backup was made by a newer SwitchGuard; update the app first. | Bu yedek daha yeni bir SwitchGuard ile alınmış; önce uygulamayı güncelleyin. |
| `desktop_restore_too_big` | The file is too large to be a SwitchGuard backup. | Dosya bir SwitchGuard yedeği olamayacak kadar büyük. |
| `desktop_restore_failed_x` | Restore failed; nothing was changed (%1$s). | Geri yükleme başarısız oldu; hiçbir şey değişmedi (%1$s). |

- [ ] **Step 1: Testler** `roundTripToAnotherComputer` (A store'u: Tuya + eWeLink anahtarları + oturum + kural + `user:` alarm sesi + `monitoring = true` → `write` → **ayrı klasördeki** B store'una `read`+`apply` → B'de anahtarlar, kural, ses dosyası (aynı bayt), `monitoring` var; oturum yok),
  `turkishPathWorks` (`…\Masaüstü\yedek ğ.sgbackup`), `restoreKeepsACopyOfTheOldSettings`, `restoreClearsThePendingAlarm`, `wrongPasswordChangesNothing` (B'nin `settings.json` baytları aynı), `oversizedFileIsRefused` (seyrek 101 MB → `TOO_BIG`).
- [ ] **Step 2–4:** Kırmızı → uygula → yeşil.
- [ ] **Step 5: Commit** — `Desktop: back up all settings to a password-protected file and restore them`

### Task 12: Cihazlar ekranında sürükle-bırak

**Files:** Create `ui/DragOrder.kt`; Modify `ui/DevicesScreen.kt`, `build.gradle.kts` (`implementation("sh.calvin.reorderable:reorderable:3.1.0")`); strings (`desktop_drag_hint`: "Hold a card to move it" / "Kartı taşımak için basılı tutun"); Test `ui/DragOrderTest.kt`

**Interfaces:**
- Produces: `class DragOrder { val ids: List<String> (Compose state); val dragging: Boolean; fun show(sorted: List<String>); fun begin(); fun move(fromId: String, toId: String); fun end(): List<String>? }`
  — `show` sürükleme yokken listeyi değiştirir, sürüklemedeyken birleştirir (bilinenler yerel sırada, yeni gelenler sona, gidenler çıkar); `end` sıra değiştiyse yeni listeyi, değişmediyse null döner.
- Ekran: gösterilen sıra `store.deviceLayout.sorted(snapshots)` → `order.show(…)`; `rememberReorderableLazyGridState(gridState) { from, to -> order.move(from.key as String, to.key as String) }`;
  her öğe `ReorderableItem(state, key = id) { isDragging -> … }`, kart gövdesinde `Modifier.longPressDraggableHandle(onDragStarted = { order.begin() }, onDragStopped = { order.end()?.let { store.deviceLayout = store.deviceLayout.copy(order = it) } })`;
  kalkan kartın gölgesi/ölçeği `animateDpAsState`/`animateFloatAsState` ile, `graphicsLayer` içinde. Kart üstündeki `Switch` ve düğmeler sürüklemeyi başlatmaz (tutamaç yalnızca kart gövdesinde).

- [ ] **Step 1: Testler** `moveReordersById`, `endReturnsTheNewOrderOnce` (iki kez `end` → ikincisi null), `noMoveEndsWithNull`, `listChangesWhileDraggingKeepTheLocalOrder` (sürüklemede `show` yeni cihaz ekler/birini çıkarır → yerel sıra korunur, yeni sona, giden yok), `showWhileIdleReplacesTheList`.
- [ ] **Step 2–4:** Kırmızı → uygula → yeşil.
- [ ] **Step 5: Elle akıcılık denetimi** — `./gradlew run`, demo cihazları açık, 3 demo + (varsa) gerçek cihazlarla: basılı tut → kalkar, hızlı sürükleme, satır atlama, pencereyi küçültüp kenar kaydırma; takılma/zıplama yok; bırakınca sıra kalıcı (uygulamayı kapatıp aç).
- [ ] **Step 6: Commit** — `Desktop: hold a device card to drag it to a new place`

### Task 13: Kartta "İzle" ve "Kurallar…"

**Files:** Modify `engine/MonitorEngine.kt` (`setRules`, `setDemo`), `ui/DevicesScreen.kt` (`DeviceCard`), Create `ui/RulesDialog.kt`; strings (`desktop_rules`: "Rules…" / "Kurallar…", `desktop_rules_title_x`: "Rules · %1$s" / "Kurallar · %1$s"); Test `engine/MonitorEngineTest.kt`

**Interfaces:**
- Produces: `MonitorEngine.setRules(deviceId: String, rules: DeviceRules)` (motor iş parçacığında `store.setRules`) · `MonitorEngine.setDemo(on: Boolean, names: Demo.Names)` (açma/kapama + hesap kalmadıysa izlemeyi durdurma motor iş parçacığında; ekrandaki doğrudan çağrının yerine).
- Kart: başlık satırında "İzle" anahtarı (`monitored`), ⋮ menüsünde `desktop_rules`; `RulesDialog`: `monitor_this_device` + `_desc`, dört satır `event_turned_off`, `event_turned_on`, `event_went_offline`, `event_came_online` × (`action_alarm` / `action_notify` / `action_ignore`), `rules_hint`.

- [ ] **Step 1: Testler** `ignoredEventDoesNotAlarm` (`setRules(id, DeviceRules().with(TURNED_OFF, IGNORE))` → kapanma → alarm yok, geçmişte `IGNORE`), `unmonitoredDeviceDoesNotAlarm`, `turningDemoOffWithoutAccountsStopsMonitoring`.
- [ ] **Step 2–4:** Kırmızı → uygula → yeşil.
- [ ] **Step 5: Commit** — `Desktop: per-device Monitor switch and alarm rules on the card`

### Task 14: Yardım bölümü, 0.2.0, kurulum ve masaüstü dosyaları

**Files:** Create `ui/settings/HelpSection.kt`, `desktop/docs/kurulum.tr.txt`; Modify `ui/settings/SettingsScreen.kt` (Yardım ve kurulum, Yedekleme bölümleri yerinde), `build.gradle.kts` (`packageVersion = "0.2.0"`)

- [ ] **Step 1:** `HelpSection`: üç `SetupSteps` (Windows'a kurulum, Tuya kurulumu, eWeLink kurulumu; Görev 7 metinleri ve bağlantıları).
- [ ] **Step 2:** `kurulum.tr.txt`: spec §7a "Windows'a kurulum" metni + Tuya/eWeLink rehber ve video bağlantıları, UTF-8 BOM'lu, CRLF.
- [ ] **Step 3:** Tam takım — `./gradlew test --rerun-tasks`; XML toplamı 0 failure / 0 error.
- [ ] **Step 4:** `./gradlew packageExe` → `build/compose/binaries/main/exe/SwitchGuard-0.2.0.exe`; çalışan SwitchGuard kapatılır; `explorer.exe "<exe>"` ile kurulur; Uninstall kaydında tek `0.2.0`; uygulama yönetici olmadan açılır; günlükte W/ yok.
- [ ] **Step 5: Elle kontrol listesi (kurulu uygulamada):** Ayarlar'ın tüm satırları; MP3 yükle → test alarmı o sesi çalar; yedekle → bir ayarı değiştir → geri yükle → ayar geri geldi; sürükle-bırak akıcılığı; kural değiştirip demo simülasyonu; tepsi "Alarmı kapat" ve alarm penceresinde "Kapat" sireni anında susturur; Yardım bağlantıları tarayıcıda açılır.
- [ ] **Step 6:** Masaüstüne `SwitchGuard-0.2.0-Kurulum.exe` (eski `SwitchGuard-0.1.1-Kurulum.exe` silinir) ve `SwitchGuard Kurulum Talimatları.txt`; SHA-256 karşılaştırması.
- [ ] **Step 7: Commit** — `Desktop: help and setup section, version 0.2.0`
