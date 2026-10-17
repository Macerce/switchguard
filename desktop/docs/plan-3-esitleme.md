# 3. iş: Windows sürümünü Android 2.12 ile eşitleme — plan

Tarih: 2026-10-16 · Dal: `windows` · Kullanıcı isteği: "PC sürümünü Android APK'daki gibi güncelle, arayüz ve özellikler aynı olsun."

## Durum (bu planın yazıldığı an)

- **Bitti:** çekirdek (`core/`, `api/`, metinler) telefon `ee2b1a0` (2.12.1) ile eşit (a7fdef6); motor telefonun 2.9–2.12
  davranışlarını taşıyor (48cada9): hatırlatma (`Store.reminderMin`), kör kalma uyarısı (`blindAlertMin`,
  `MonitorHost.monitorBlind/clearBlind`, `AppState.blind`), kesinti kaydı, Telegram ile haber verme (`Store.telegram*`,
  `escalateMin`), sessiz/gecikmeli test alarmı (`engine.testAlarm(silent, delayMs)`), tepki süresi kaydı, kritik cihaz
  (`DeviceRules.critical`), `runtime.json` (`RuntimeState`).
- **Ortak temel (bu commit):** 2.12 teması (`ui/Theme.kt`: `Brand`, `tileColor()`, `raisedOnTile()`, `LocalDarkTheme`),
  ortak bileşenler (`ui/Components.kt`: `PageHeader`, `SectionTitle`, `SectionCard`, `InfoRow`, `MenuRow`, `Hue`/`HueIcon`,
  `PowerButton`, `IconBadge`, `StatusDot`, `OnlineDot`, `connColor`, `connTitle`, `DeviceKind.icon()`, `BackButton`,
  `RowDivider`), metin erişimi (`ui/Res.kt`: `LocalStrings`, `str("k")`, `str("k", a)`, `plural("k", n, a)`), sol menü
  (`ui/AppNav.kt`: Cihazlar · Uyarılar · Geçmiş · Ayarlar, Geçmiş rozeti, `AppState.openDevice` ile cihaz sayfası,
  `AppState.settingsHome`), `Modifier.readableWidth()`, ekran görüntüsü yardımcısı (`src/test/.../ui/Snapshots.kt`).

## Ortak kurallar (her iş için)

- **Kaynak:** telefonun 2.12.1 arayüzü. Okumak için: `git show ee2b1a0:app/src/main/java/com/macerce/switchguard/ui/<Dosya>.kt`
  ya da hazır kopyalar `C:\Users\macerce\ewelink-alarm\.work\ref\` (ui/, core/, data/, service/, strings.xml, strings-tr.xml).
  Görünüm ve davranış telefondakiyle aynı olmalı; yalnızca masaüstüne uymayanlar uyarlanır (dokunmatik → fare/klavye,
  TV odak halkası `tvFocusRing` yok, Android izin/pil/üretici ayarları yok, Toast yerine Snackbar ya da satır içi metin,
  Intent yerine `desktop/platform/Links`, `Dispatchers.IO` + state).
- **Masaüstü kararları korunur:** otomasyon sekmesinin adı arayüzde **"Uyarılar"** (`desktop_alerts_*` anahtarları;
  kullanıcı kararı — 2026-10-17'de değişti: telefon 2.13 ile Uyarılar ve Otomasyon ayrı sekmeler, `ui/alerts/`,
  `AutomationMode`); Pro her zaman açık (Pro/satın alma ekranı yok); eWeLink girişi tarayıcı + pano izleme
  (`ui/settings/EwelinkDialogs.kt`, `accounts/`), Tuya bağlama (`ui/settings/TuyaDialog.kt`), ses yükleme
  (`sound/`, `ui/settings/SoundPicker.kt`), şifreli yedek (`backup/`, `ui/settings/BackupSection.kt`), Windows ile başlat /
  uyanık tut / Tuya canlı (`platform/`). Bunların mantığı değişmez, arayüzü yeni düzene taşınır.
- **Derleme/test:** `cd desktop && JAVA_HOME=/c/Users/macerce/.jdks/temurin-21 ./gradlew test` (tam takım her işin sonunda
  yeşil olmalı; sonuç `build/test-results/test/*.xml`). Yeni mantık birim testli; ekranlar için Compose UI testi
  (`compose.desktop.uiTestJUnit4`, örnekler `src/test/.../ui/*Test.kt`) ve açık + koyu ekran görüntüsü
  (`Snapshots.render("ad", dark = …) { … }` → `desktop/build/snapshots/ad.png`; Read aracıyla bakılır).
- **Metinler:** telefonda karşılığı olan her metin telefon anahtarıyla (`str("quiet_hours")`). Yalnızca masaüstüne özgü
  metinler işin kendi dosyasına EN + TR: Cihazlar/Geçmiş → `desktop-devices.xml`, Ayarlar → `desktop-settings.xml`,
  Uyarılar/alarm → `desktop-alerts.xml` (`src/main/resources/strings/values{,-tr}/`). Anahtarlar `desktop_` ile başlar,
  telefon anahtarını gölgeleyemez, EN/TR biçim belirteçleri aynı (StringsTest denetler). Telefon metni "telefon" diyorsa
  masaüstü karşılığı yazılır ("bilgisayar").
- **Simgeler:** `Icons.Rounded.*` / `Icons.AutoMirrored.Rounded.*` (telefondaki gibi). Genişletilmiş simgeler ince jar'dan
  gelir: listede olmayan simge derlenmez. Yeni simge gerekirse işin kendi listesine ekle: Cihazlar/Geçmiş →
  `desktop/icons-devices.txt`, Ayarlar → `desktop/icons-settings.txt`, Uyarılar → `desktop/icons-alerts.txt`
  (satır biçimi `rounded/Ad` ya da `automirrored/rounded/Ad`; temel simgeler — Add, Settings, Delete, Edit, Check, Close,
  Info, Warning, Refresh, Search, Star, PlayArrow, KeyboardArrowUp/Down, MoreVert, Email, Lock, Home, Notifications,
  AccountCircle, CheckCircle, ArrowBack/Forward, KeyboardArrowRight, Send — listeye yazılmaz).
- **Motor kuralı:** arayüz durumu motor iş parçacığında değiştirir (`graph.engine.*`); ağ/dosya işleri arayüz iş
  parçacığında çalışmaz. `graph.store` okumaları `graph.store.version`'a bağlı `remember(version)` ile.
- **Kod:** yorumlar Türkçe, mevcut dosyaların üslubunda (sınıf/işlev başına kısa KDoc, "neden" yorumları). `app/`
  (telefon) klasörüne dokunulmaz. Commit mesajı İngilizce `Desktop: …` + son satır
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. `git push` yok.
- **Ortak dosyalar:** `ui/Components.kt`, `ui/Theme.kt`, `ui/Res.kt`, `ui/AppNav.kt`, `AppState.kt`, `engine/`, `data/`,
  `core/`, `api/` paylaşılır: değiştirmek gerekirse en küçük ekleme yapılır ve commit mesajında belirtilir (başka işle
  çakışabilir). Bileşenin yalnız bir ekranda gereken çeşidi o ekranın dosyasında yazılır.

## İşler

### A. Cihazlar, cihaz sayfası, Geçmiş

Telefon: `DevicesScreen.kt`, `DeviceDetailScreen.kt`, `DeviceExtras.kt`, `DeviceLayoutUi.kt`, `DemoUi.kt`,
`HistoryScreen.kt`, `HistoryExport.kt`, `core/Csv.kt`. Masaüstü bugün: `ui/DevicesScreen.kt`, `ui/DeviceOptionsDialog.kt`,
`ui/CardStyle.kt`, `ui/DragOrder.kt`; yer tutucular `ui/DeviceDetailScreen.kt`, `ui/HistoryScreen.kt`.

- Cihazlar: üstte koruma kartı (izleme durumu, cihaz/açık/çevrimdışı/güç sayıları, Başlat/Durdur), oturum uyarısı
  (`AppState.warning`) ve kör kalma uyarısı (`AppState.blind`) kartları, kutucuk ızgarası (DeviceKind simgesi, durum,
  güç, yuvarlak güç düğmesi; çok kanallılar geniş kutucuk ve kanal düğmeleri), öncelikli ve kategori bölümleri,
  düzenleme modu (sıralama — masaüstündeki sürükle-bırak korunur —, öncelikli, kart boyutu küçük/büyük =
  `DeviceLayout.toggleSize`, kategori). 2.12'de genel kart boyutu yok: `Store.cardSize` ve `CardStyle` kalkar
  (ayar dosyasındaki eski değer zararsız). Kutucuğa tıklayınca cihaz sayfası (`AppState.openDevice`).
- Cihaz sayfası: büyük güç düğmesi (çok kanallıda kanal düğmeleri), bağlantı/izleme/güç özeti, kurallar (izle,
  olay başına Alarm/Bildirim/Yok, cihaz sesi, **kritik cihaz** anahtarı), enerji (bugün/7 gün), eWeLink
  zamanlayıcıları (saat seçimi masaüstüne uygun: saat/dakika alanı ya da adımlayıcı), ham veriyi kopyala, demo
  simülasyonu, bu cihazın son olayları. `ui/DeviceOptionsDialog.kt`'deki her şey bu sayfaya taşınır, pencere kalkar.
- Geçmiş: telefondaki liste (renkli dairede olay simgesi: alarm kırmızı, bildirim turkuaz; gün başlıkları; cihaz
  filtresi), yeni olay türleri (DISMISSED, REMINDER, ESCALATION, MONITOR_GAP, MONITOR_BLIND, SYSTEM) için simgeler,
  "Geçmişi temizle", **CSV dışa aktar** (aralık: gün/hafta/ay/tümü; `core/Csv`, `EventLog.export`; dosya kaydetme
  penceresi — masaüstü `BackupSection`'daki dosya seçici yaklaşımı).

### B. Ayarlar

Telefon: `SettingsScreen.kt`, `TelegramSetup.kt`, `BackupUi.kt`, `TuyaSetup.kt`, `SoundPicker.kt`. Masaüstü bugün:
`ui/settings/*` (SettingsScreen, SettingsRows, AccountsSection, BackupSection, HelpSection, SetupSteps, SoundPicker,
TuyaDialog, EwelinkDialogs).

- Telefondaki gibi kategori listesi + alt sayfalar (`MenuRow`, `Hue`; her satırda tek satırlık özet, sorunlu
  durum turuncu `warning`): Hesaplar · Alarm · Sessiz saatler · İkinci kişiye haber ver · Günlük özet · İzleme ·
  Windows (telefondaki İzinler yerine: Windows ile başlat, uyanık tut) · Yedekleme ve aktarım · Dil ve görünüm
  (dil + tema) · Yardım ve hakkında. Ayarlar sekmesine yeniden tıklayınca listeye dönülür (`AppState.settingsHome`).
- Alarm: alarm sesi + bildirim sesi (mevcut SoundPicker), ses düzeyi, **hatırlatma** (kapalı/10/30/60 dk,
  `Store.reminderMin`), **test alarmı penceresi** (Sesli / Sessiz, "10 saniye sonra çal";
  `graph.engine.testAlarm(silent, delayMs)`).
- Sessiz saatler: başlangıç/bitiş (fare/klavyeye uygun saat seçimi), kritik cihaz açıklaması.
- İkinci kişiye haber ver: telefondaki TelegramSetup akışı (bot anahtarı → dene (`TelegramClient.botUsername`) →
  kişiler (`findChats`) → bekleme süresi (`Escalation.DELAYS`) → kapat), `Store.telegram*`, `escalateMin`,
  `disableEscalation`. t.me bağlantısı tarayıcıda açılır.
- İzleme: sorgulama aralığı, çevrimdışı bekleme, **kör kalma uyarısı** (`Store.blindAlertMin`), Tuya canlı.
- Günlük özet: telefondaki ayarlar (aç/kapa, saat, önizleme). Gönderimi (seçilen saatte Windows bildirimi) ana
  oturum motora ekliyor: `graph.engine.previewSummary()` önizleme bildirimini hemen gösterir, ayar değişince başka
  bir şey çağırmak gerekmez (motor her tick'te saate bakar).
- Yardım ve hakkında: rehberler, videolar, iletişim, gizlilik, sürüm (mevcut HelpSection içeriği).

### C. Uyarılar ve alarm penceresi

Telefon: `AutomationsScreen.kt`, `AlarmActivity.kt`. Masaüstü bugün: `ui/automations/*`, `ui/AlarmWindow.kt`.

- Uyarılar listesi: telefondaki "Eğer … / O zaman …" kartları, eylemin renkli simgesi, aç/kapa, boş durum.
- Düzenleyici: telefondaki 2.12 düzenleyici (cihaz kartları, olay ve işlem kutucukları, numaralı kanal düğmeleri,
  dakikalar için − / + adımlayıcı, güç eşiği alanı, "Eğer / O zaman" özeti, Kaydet/Sil). Mevcut masaüstü
  `AutomationForm` mantığı (doğrulama, kaydetme motor üzerinden) korunur; arayüz yenilenir. Masaüstünde
  düzenleyici pencere ya da sayfa olabilir — telefondaki tam sayfa düzeni tercih edilir.
- Alarm penceresi: telefondaki alarm ekranının görünümü (zilin çevresinden yayılan halkalar, satırlar, büyük
  Kapat); masaüstü davranışları korunur (her zaman üstte, tam ekran, pencere kapatma yok sayılır, kaydırılabilir
  satır listesi). Sessiz test alarmı satırı ses çalmadığını gösterir.

### D. Son (birleştirme sonrası)

Sürüm 0.4.0, `SYNC.md` kaynak `ee2b1a0`, `docs/kurulum.tr.txt`, exe, kurulum ve demo ile elle doğrulama.
