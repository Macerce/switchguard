# SYNC.md

Telefon uygulamasıyla masaüstü uygulamasının `core/` ve `api/` modüllerinin senkronizasyon dosyası.

Kaynak: `tv-tablet` dalı `132e7fe` commit'i (master merge ile aynı içerik `c545395`)

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

## Kopyalanan Dosyalar (api/)

| dosya | kaynak yolu | uyarlama |
|-------|-------------|----------|
| EwelinkClient.kt | app/src/main/java/com/macerce/switchguard/api/ | `buildLoginUrl`: `android.net.Uri` → OkHttp `HttpUrl` (`OAUTH_PAGE.toHttpUrl().newBuilder().addQueryParameter(...)`; aynı 8 parametre, aynı sıra); `import android.net.Uri` yerine `import okhttp3.HttpUrl.Companion.toHttpUrl` |
| EwelinkSocket.kt | app/src/main/java/com/macerce/switchguard/api/ | yok |
| TuyaClient.kt | app/src/main/java/com/macerce/switchguard/api/ | yok |
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

## Kopyalanan Dosyalar (metinler)

| dosya | kaynak yolu | uyarlama |
|-------|-------------|----------|
| desktop/src/main/resources/strings/values/strings.xml | app/src/main/res/values/strings.xml | yok (bayt bayt aynı) |
| desktop/src/main/resources/strings/values-tr/strings.xml | app/src/main/res/values-tr/strings.xml | yok (bayt bayt aynı) |

`values/desktop.xml` ve `values-tr/desktop.xml` yalnızca masaüstüne özgüdür (telefonda karşılığı yok, senkronize edilmez); anahtarları telefon `strings.xml` anahtarlarıyla çakışmamalıdır (`StringsTest.desktopKeysNeverShadowPhoneKeys`). `Strings.eventStrings()` telefondaki `EventLog.strings(context)` ile, `Strings.demoNames()` `Actions.enableDemo` ile aynı anahtarları kullanır; telefonda bunlar değişirse masaüstü karşılıkları da güncellenir.

## Kopyalanmayan Dosyalar

| dosya | sebep |
|-------|-------|
| AppLanguage.kt | Yerine `desktop/i18n/Strings` yazıldı (1. aşamanın 5. görevi) |
| Transfer.kt | Kurulum aktarımı özelliği (telefondan masaüstüne); 4. aşamada uyarlanacak (`desktop/docs/spec.md` §3.1): `Build.MODEL` → bilgisayar adı, `android.util.Base64` → `java.util.Base64` |
| TransferNet.kt | Kurulum aktarımı özelliği; 4. aşamada uyarlanacak (`desktop/docs/spec.md` §3.1): Android NSD → JmDNS |

## Senkronizasyon Kuralı

Telefon uygulamasında `core/` veya `api/` klasörlerinde yapılan herhangi bir değişiklik, aynı değişikliğin masaüstü versiyonuna taşınması gerekir. Masaüstünde yapılan her uyarlama bu dosyadaki "uyarlama" sütununa yazılır (uyarlama yoksa "yok"); her senkronizasyonda yukarıdaki "Kaynak" commit satırı da güncellenir.
