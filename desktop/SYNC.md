# SYNC.md

Telefon uygulamasıyla masaüstü uygulamasının senkronizasyon dosyası: kopyalanan `core/`, `api/` ve `data/` dosyaları ile masaüstüne yeniden yazılan / taşınan dosyalar (`Store`, `MonitorService`, `Notifier` + `AlarmActivity`).

Kaynak: `master` `4bbc24d` (telefon 2.13.0; 2026-10-17). İlk kopya `tv-tablet` `132e7fe`'den alınmıştı.

## Kopyalanan Dosyalar (core/)

| dosya | kaynak yolu | uyarlama |
|-------|-------------|----------|
| Automation.kt | app/src/main/java/com/macerce/switchguard/core/ | yok |
| ChangeDetector.kt | app/src/main/java/com/macerce/switchguard/core/ | yok |
| Demo.kt | app/src/main/java/com/macerce/switchguard/core/ | yok |
| DeviceLayout.kt | app/src/main/java/com/macerce/switchguard/core/ | yok |
| DeviceParser.kt | app/src/main/java/com/macerce/switchguard/core/ | yok |
| DeviceSnapshot.kt | app/src/main/java/com/macerce/switchguard/core/ | yok |
| DeviceTimers.kt | app/src/main/java/com/macerce/switchguard/core/ | yok |
| EnergyParser.kt | app/src/main/java/com/macerce/switchguard/core/ | yok |
| Entitlement.kt | app/src/main/java/com/macerce/switchguard/core/ | yok |
| EventText.kt | app/src/main/java/com/macerce/switchguard/core/ | yok |
| ExpectedChanges.kt | app/src/main/java/com/macerce/switchguard/core/ | yok |
| Metrics.kt | app/src/main/java/com/macerce/switchguard/core/ | yok |
| OfflineGrace.kt | app/src/main/java/com/macerce/switchguard/core/ | yok |
| Rules.kt | app/src/main/java/com/macerce/switchguard/core/ | yok |
| Signer.kt | app/src/main/java/com/macerce/switchguard/core/ | yok |
| Tuya.kt | app/src/main/java/com/macerce/switchguard/core/ | yok |
| WsMessages.kt | app/src/main/java/com/macerce/switchguard/core/ | yok |
| AlarmReminders.kt | app/src/main/java/com/macerce/switchguard/core/ | yok |
| BlindWatch.kt | app/src/main/java/com/macerce/switchguard/core/ | yok |
| Csv.kt | app/src/main/java/com/macerce/switchguard/core/ | yok |
| DeviceKind.kt | app/src/main/java/com/macerce/switchguard/core/ | yok |
| DurationText.kt | app/src/main/java/com/macerce/switchguard/core/ | yok |
| Escalation.kt | app/src/main/java/com/macerce/switchguard/core/ | yok |
| MonitorGap.kt | app/src/main/java/com/macerce/switchguard/core/ | yok |
| ParseCache.kt | app/src/main/java/com/macerce/switchguard/core/ | yok (masaüstü Store henüz kullanmıyor) |
| TokenRefresh.kt | app/src/main/java/com/macerce/switchguard/core/ | yok |

## Kopyalanan Dosyalar (api/)

| dosya | kaynak yolu | uyarlama |
|-------|-------------|----------|
| EwelinkClient.kt | app/src/main/java/com/macerce/switchguard/api/ | `buildLoginUrl`: `android.net.Uri` → OkHttp `HttpUrl` (`OAUTH_PAGE.toHttpUrl().newBuilder().addQueryParameter(...)`; aynı 8 parametre, aynı sıra); `import android.net.Uri` yerine `import okhttp3.HttpUrl.Companion.toHttpUrl` |
| EwelinkSocket.kt | app/src/main/java/com/macerce/switchguard/api/ | yok |
| TuyaClient.kt | app/src/main/java/com/macerce/switchguard/api/ | yok |
| TelegramClient.kt | app/src/main/java/com/macerce/switchguard/api/ | yok |
| TuyaSocket.kt | app/src/main/java/com/macerce/switchguard/api/ | `android.util.Log` → `com.macerce.switchguard.desktop.Log` (yalnızca import; `Log.w(TAG, msg, e)` çağrısı aynı) |

## Kopyalanan Dosyalar (data/)

| dosya | kaynak yolu | uyarlama |
|-------|-------------|----------|
| EventLog.kt | app/src/main/java/com/macerce/switchguard/data/ | uyarlandı: `SQLiteOpenHelper(Context)` → `EventLog(file: Path)` + tek JDBC bağlantısı (`org.xerial:sqlite-jdbc`), tüm yöntemler `@Synchronized`, `PRAGMA journal_mode=WAL`; sorgular hazır ifade + bağlı parametre; aynı `events` tablosu ve `idx_events_ts` (şema sürümü 2, `PRAGMA user_version`; `CREATE ... IF NOT EXISTS`); `onUpgrade`/eski kayıt taşıması yok; `EventLog.strings(context)` yok (yerine `Strings.eventStrings()`); `get(context)` tekili yok (çağıran tek örnek kurar); `EventKind.SYSTEM` eklendi; `close()` eklendi (`AutoCloseable`). Genel API, `MAX_ROWS` ve SQL aynı |
| MetricsLog.kt | app/src/main/java/com/macerce/switchguard/data/ | uyarlandı: `SQLiteOpenHelper(Context)` → `MetricsLog(file: Path)` + tek JDBC bağlantısı, tüm yöntemler `@Synchronized` (telefonda yalnızca `record`), `PRAGMA journal_mode=WAL`; sorgular hazır ifade + bağlı parametre (`prune` kesim zamanı metin yerine `Long` bağlanır); aynı `power`/`state` tabloları ve indeksleri (şema sürümü 1, `PRAGMA user_version`); `onUpgrade` yok; `get(context)` tekili yok; `close()` eklendi. Genel API, `POWER_EVERY_MS`, `KEEP_DAYS`, `record` mantığı aynı |
| LiveState.kt | app/src/main/java/com/macerce/switchguard/data/ | yok |

## Test Dosyaları (core/)

| dosya | kaynak yolu | uyarlama |
|-------|-------------|----------|
| AutomationTest.kt | app/src/test/java/com/macerce/switchguard/core/ | yok |
| ChangeDetectorTest.kt | app/src/test/java/com/macerce/switchguard/core/ | yok |
| DemoTest.kt | app/src/test/java/com/macerce/switchguard/core/ | yok |
| DeviceLayoutTest.kt | app/src/test/java/com/macerce/switchguard/core/ | yok |
| DeviceParserTest.kt | app/src/test/java/com/macerce/switchguard/core/ | yok |
| DeviceTimersTest.kt | app/src/test/java/com/macerce/switchguard/core/ | yok |
| EnergyTest.kt | app/src/test/java/com/macerce/switchguard/core/ | yok |
| EntitlementTest.kt | app/src/test/java/com/macerce/switchguard/core/ | yok |
| EventTextTest.kt | app/src/test/java/com/macerce/switchguard/core/ | yok |
| ExpectedChangesTest.kt | app/src/test/java/com/macerce/switchguard/core/ | yok |
| OfflineGraceTest.kt | app/src/test/java/com/macerce/switchguard/core/ | yok |
| RulesTest.kt | app/src/test/java/com/macerce/switchguard/core/ | yok |
| SignerTest.kt | app/src/test/java/com/macerce/switchguard/core/ | yok |
| TuyaTest.kt | app/src/test/java/com/macerce/switchguard/core/ | yok |
| WsMessagesTest.kt | app/src/test/java/com/macerce/switchguard/core/ | yok |
| AlarmRemindersTest.kt, BlindWatchTest.kt, CsvTest.kt, DeviceKindTest.kt, DurationTextTest.kt, EscalationTest.kt, MonitorGapTest.kt, ParseCacheTest.kt, TokenRefreshTest.kt | app/src/test/java/com/macerce/switchguard/core/ | yok |

## Kopyalanan Dosyalar (metinler)

| dosya | kaynak yolu | uyarlama |
|-------|-------------|----------|
| desktop/src/main/resources/strings/values/strings.xml | app/src/main/res/values/strings.xml | yok (bayt bayt aynı) |
| desktop/src/main/resources/strings/values-tr/strings.xml | app/src/main/res/values-tr/strings.xml | yok (bayt bayt aynı) |

`<plurals>` desteklenir (`Strings.plural`, "one"/"other"). `values/desktop*.xml` ve `values-tr/desktop*.xml` (`desktop.xml`, `desktop-devices.xml`, `desktop-settings.xml`, `desktop-alerts.xml`; liste `Strings.FILES`) yalnızca masaüstüne özgüdür (telefonda karşılığı yok, senkronize edilmez); anahtarları telefon `strings.xml` anahtarlarıyla çakışmamalıdır (`StringsTest.desktopKeysNeverShadowPhoneKeys`). `Strings.eventStrings()` telefondaki `EventLog.strings(context)` ile, `Strings.demoNames()` `Actions.enableDemo` ile aynı anahtarları kullanır; telefonda bunlar değişirse masaüstü karşılıkları da güncellenir.

## Yeniden Yazılan / Taşınan Dosyalar

Bunlar kopya değildir: telefondaki dosyanın davranışı masaüstüne elle taşındı (kaynak `c545395`). Telefonda bu
dosyalarda yapılan bir mantık değişikliği, aşağıdaki farklar dışında masaüstü karşılığına da işlenir.
`EventLog.kt` ve `MetricsLog.kt` yukarıdaki "data/" tablosundadır.

| telefon | masaüstü | farklar |
|---------|----------|---------|
| `app/.../data/Store.kt` | `desktop/.../data/Store.kt` | Yeniden yazıldı; aynı özellik adları, türleri, varsayılanlar ve JSON anahtarları (`StoreTest.jsonKeysMatchThePhoneStore`). Farklar: `SharedPreferences` (`prefs`) yerine tek `settings.json` (atomik yazma: geçici dosya + `fsync` + atomik taşıma; tek kilit; değişiklik bildirimi `version: StateFlow`); `Store.get(context)` tekili yok, kurucu `Store(file, secrets)`; gizli alanlar (`SECRET_KEYS`: appSecret, at, rt, apikey, tuyaSecret, tuyaToken, tgToken) diske DPAPI ile kodlanmış yazılır (`DpapiCodec`; telefonda Keystore — `SecretValues`/`SecretBox` kopyalanmadı); `isLoggedIn`/`hasTuya` telefondaki gibi şifre çözmeden bakar, çözülemeyen değer `secretsUnreadable` ile bildirilir; 2.12 alanları `blindAlertMin`, `reminderMin`, `telegramToken/Recipients/Bot`, `escalateMin`, `escalationReady`, `disableEscalation` aynı; telefona özgü `vibrate`, `alarmBoost`, `autostartConfirmed`, `twinAcknowledged` yok; `"pro"` anahtarı yok, `isPro` her zaman `true` (`val`); `alwaysAwake` varsayılanı `true` (telefonda `false`); yeni alanlar `autostart` (`true`), `tuyaLive` (`false`), `language` (`""`), `theme` (`"system"`), `cardSize` (`"large"`: Cihazlar ekranı kart boyutu), `notifySound` (`null`: bildirim sesi; ses kimliklerine `user:<dosya>` eklendi, `%APPDATA%\SwitchGuard\sounds` altındaki yüklenmiş WAV); okuma hatası politikası: yalnızca ayrıştırılamayan dosya `settings.json.corrupt-<ms>` adıyla kenara alınıp boş başlanır, okuma hatası (kilitli dosya vb.) 3 kez denenir sonra `IOException` atılır ve uygulama açılmaz (asıl dosyanın üzerine asla yazılmaz); UTF-8 BOM kabul edilir; uyarılar `desktop.Log`'a; yedek için `exportSettings`/`replaceSettings` (masaüstüne özgü, izin listesi `desktop/backup/BackupContent.KEYS`); çözülemeyen gizli alan bir kez yeniden denenir |
| `app/.../data/RuntimeState.kt` | `desktop/.../data/RuntimeState.kt` | `SharedPreferences` yerine `runtime.json` (atomik yazma, değer değişmedikçe yazılmaz; okunamazsa boş başlar); `savedAlarmVolume` ve `stoppedWarnedAt` yok; masaüstüne özgü `summarySentFor`, `summaryKey` (günlük özetin zamanlaması) |
| `app/.../service/DailySummary.kt` | `desktop/.../desktop/engine/DailySummary.kt` | `build` aynı metin ve kurallar (`Strings` ile); `SummaryScheduler`/`SummaryReceiver` (AlarmManager) yerine motor her tick'te `dueAt`'e bakar (`MonitorEngine.checkSummary`): o günün zamanı geçtiyse bir kez, uykudan sonra en çok `LATE_MS` (2 sa) geç; ayar değişince o anki zaman atlanır (telefondaki "geçtiyse yarın") |
| `app/.../service/MonitorService.kt` | `desktop/.../desktop/engine/MonitorEngine.kt` (+ `MonitorHost`, `Scheduler`, `Sources`) | 2.12.1 eşitlemesi: `restoreRuntime`/`reportGap`/`checkSecrets`/`checkBlind`/`heartbeat`/`persistRuntime`/`checkReminders`/`checkEscalation`/`logDismissal` aynı; Telegram gönderimi `Sources.telegram` + `telegramRunner` (telefonda tek `telegram` iş parçacığı; testlerde aynı iş parçacığı); mesajın altındaki gönderen = bilgisayar adı (`COMPUTERNAME`); `dismissAlarm(lines)`: yalnız görülen satırlar kapanır, arada yeni satır geldiyse alarm sürer ve tepki süresi ondan sayılır; kesinti kaydı uygulama kapatılıp açılınca da (bilgisayar kapalıydı, Çıkış) yazılır; kör kalma uyarısı `MonitorHost.monitorBlind` → Cihazlar ekranındaki kart + Windows bildirimi; metinler `desktop_secrets_unreadable`, `desktop_blind_hint` (telefondakiler "telefon"/"İzinler (pil)" diyor). Yöntem adları ve sırası aynı (yan yana karşılaştırılabilir). Android'e bağlı kısımlar yok (ön plan servisi, AlarmManager tick'i, ağ geri çağrısı, günlük özet, `onBind`, `onConfigurationChanged`); `Notifier`/`AlarmActivity`/WakeLock yerine `MonitorHost`. Farklar: saat — sessiz saat dakikası, alarm satırı saati ve `LiveState.expected` zamanları `clock.wall()`'dan, sistem saat diliminde (`Calendar.getInstance()`/`Date()` yerine; testler saati sabitleyebilsin, üretimde davranış aynı); uyku algılama — iki tick arasında duvar saati 60 sn'den fazla ilerlerse bağlantılar baştan kurulur ve iki bulut senkronlanır, 2 dk ve üstü uyku geçmişe `SYSTEM` olarak yazılır; sıra — `tick`'te süresi dolan çevrimdışı bekletmeleri ve süreli otomasyonlar uyku senkronundan ÖNCE, uyku öncesi son durumla değerlendirilir (telefondaki sıra: uykuda süresi dolan bekletme "kısa kopma" sayılmasın, alarm çalsın); Tuya anlık bağlantısı yalnızca `store.tuyaLive` açıkken kurulur (kapalıysa yalnızca 5 dk'da bir sorgu); `loopGeneration` — motor `stop()` sonrası yaşamaya devam ettiğinden her başlatma yeni döngü kuşağı açar, eski döngü kendiliğinden biter; döngü ve başlatma bloğu `try/finally` ile korunur (telefonda süreç çöker, servis yeniden başlar); `control()` — `ui/Actions.setSwitch` + `onControlled` motor iş parçacığında (demo cihazına komut gitmez, izleme kapalıysa durum doğrudan güncellenir); `shutdown()` — telefondaki `onDestroy` (`monitoringEnabled` ve kapatılmamış alarm korunur); `stopMonitoring` ayrıca `host.statusChanged(STOPPED)` ve uyanık tutmayı bırakır; olay geçmişine yazma hatası ölümcül değildir: alarm/bildirim önce verilir, geçmiş sonra `record()` ile yazılır, hata yalnızca günlüğe gider (telefonda `SQLiteDatabase.insert` hatayı yutar); `metrics.record`/`prune` hataları günlüğe yazılıp geçilir |
| `app/.../service/Notifier.kt` + `app/.../ui/AlarmActivity.kt` | `desktop/.../desktop/platform/DesktopHost.kt` + `desktop/.../desktop/ui/AlarmWindow.kt` (+ `platform/Sounds.kt`, `platform/DisplayWake.kt`, `platform/NotificationQueue.kt`) | Alarm bildirimi yerine her zaman tam ekran alarm penceresi (telefonda yalnızca TV'de tam ekran): çerçevesiz, her zaman üstte, birincil ekranı kaplar, pencere kapatma isteği yok sayılır, yalnızca "Kapat" düğmesiyle kapanır, klavye odağı düğmeye verilmez; tüm satırlar kaydırılabilir listede (telefonda son 4 + "+N"); "Uygulamayı aç" düğmesi ve kaydırarak kapatma yok. Pencere her tick öne getirilir / kapandıysa yeniden kurulur (`ensureAlarmVisible`; telefonda `AlarmActivity.ensureShown`, yalnızca TV). Ses `MediaPlayer`/`RingtoneManager` yerine `javax.sound` (`SoundPlayer`): yerleşik WAV sesleri, bozuk/eksik ses varsayılana, o da olmazsa sistem bip'ine düşer; alarm sesi kendini toparlar — başlatılamazsa ya da çalarken durursa (çalma konumu ilerlemiyor: ses cihazı kayboldu) alarm sürdükçe her tick yeniden başlatılır (telefonda `startSound` yalnızca yeni alarm satırında, oynatıcı yoksa yeniden dener). Titreşim yok. Ekran: telefondaki `setTurnScreenOn`/`FLAG_KEEP_SCREEN_ON` yerine her yeni alarm satırında ekran uyandırılır (`ES_DISPLAY_REQUIRED` + sıfır mesafeli fare hareketi) ve alarm sürdükçe her tick ekranın kapanma sayacı sıfırlanır. Bildirimler Windows tepsi bildirimi olarak sıraya alınıp aralıklı gösterilir; bildirim sesi en çok 6 sn |

## Kopyalanmayan Dosyalar

| dosya | sebep |
|-------|-------|
| AppLanguage.kt | Yerine `desktop/i18n/Strings` yazıldı (1. aşamanın 5. görevi) |
| Transfer.kt | Kurulum aktarımı özelliği (telefondan masaüstüne); 4. aşamada uyarlanacak (`desktop/docs/spec.md` §3.1): `Build.MODEL` → bilgisayar adı, `android.util.Base64` → `java.util.Base64` |
| TransferNet.kt | Kurulum aktarımı özelliği; 4. aşamada uyarlanacak (`desktop/docs/spec.md` §3.1): Android NSD → JmDNS |

## Senkronizasyon Kuralı

Telefon uygulamasında `core/`, `api/`, `data/` veya `service/` klasörlerinde ya da alarm ekranının davranışında (`ui/AlarmActivity.kt`) yapılan herhangi bir değişiklik, aynı değişikliğin masaüstü versiyonuna taşınması gerekir (karşılıkları yukarıdaki tablolarda; "Yeniden Yazılan / Taşınan Dosyalar"daki farklar korunur). Masaüstünde yapılan her uyarlama bu dosyadaki "uyarlama" ya da "farklar" sütununa yazılır (uyarlama yoksa "yok"); her senkronizasyonda yukarıdaki "Kaynak" commit satırı da güncellenir.
