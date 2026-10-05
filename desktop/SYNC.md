# SYNC.md

Telefon uygulamasıyla masaüstü uygulamasının `core/` modülü senkronizasyon dosyası.

Kaynak: `tv-tablet` deposu `132e7fe` commit'i (master merge ile aynı içerik `c545395`)

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

## Kopyalanmayan Dosyalar

| dosya | sebep |
|-------|-------|
| AppLanguage.kt | Masaüstü i18n sistemi yazılacak (sonraki aşama) |
| Transfer.kt | 4. aşamada WebSocket haberleşme için uyarlanacak |
| TransferNet.kt | 4. aşamada WebSocket haberleşme için uyarlanacak |

## Senkronizasyon Kuralı

Telefon uygulamasında `core/` veya `api/` klasörlerinde yapılan herhangi bir değişiklik, aynı değişikliğin masaüstü versiyonuna taşınması gerekir. Bu dosya her senkronizasyonda güncellenecektir.
