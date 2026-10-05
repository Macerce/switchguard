# SwitchGuard for Windows — Tasarım (spec)

Tarih: 2026-10-06 · Dal: `windows` (tv-tablet @ c545395 üstünden) · Durum: onay bekliyor

## 1. Amaç

SwitchGuard'ın telefondaki bütün özelliklerini Windows bilgisayarda çalıştıran, `.exe` ile kurulan bir masaüstü uygulaması.

- **İlk hedef:** kullanıcının kendi bilgisayarları (ör. resepsiyon / ofis PC'si); alarm bilgisayarda çalsın.
- **Sonraki hedef:** herkese dağıtım (GitHub Releases) ve ücretli Pro (lisans anahtarı). Tasarım buna izin verir, ilk sürüm bunu içermez.
- **Başarı ölçütü:** Bilgisayara exe ile kurulur; Windows açılınca tepside başlar; izlenen cihaz dışarıdan kapatılınca / çevrimdışı olunca telefondaki kurallarla aynı şekilde birkaç saniye içinde tam ekran alarm ve ses verir; telefondaki her ekranın masaüstü karşılığı vardır.

## 2. Kararlar

| Konu | Karar | Neden |
|---|---|---|
| Telefon uygulaması | `app/` klasörüne **dokunulmaz** | Play'de kapalı testte; risk sıfır |
| Proje yeri | Aynı depoda bağımsız Gradle projesi `desktop/` | Lisans, sürüm sayfası, rehberler ortak; Android derlemesi `desktop/`'u görmez |
| Dil / arayüz | Kotlin + Compose Desktop (Material 3) | Telefonda denenmiş mantık kopyalanabilir; görünüm aynı dilde |
| Mantığın paylaşımı | **Kopyala ve uyarla** (ortak modül yok) | Kullanıcı tercihi; bedeli: telefondaki mantık düzeltmeleri elle taşınır (bkz. §13) |
| Kopyanın kaynağı | `tv-tablet` @ `132e7fe` (aktarma özelliği burada) | En güncel mantık |
| Pro | İlk sürüm kişisel: Pro açık. Herkese açılışta Lemon Squeezy lisans anahtarı | Play Billing Windows'ta yok |
| Lisans | Depo lisansı (PolyForm Noncommercial 1.0.0, 2026-10-06'dan itibaren) | master `3edc27b` |
| Sürüm | Windows kendi çizgisi: `1.0.0`'dan başlar | Telefon sürümünden bağımsız yayınlanır |

## 3. Mimari

```
desktop/
  settings.gradle.kts, build.gradle.kts   (bağımsız; JVM 17+, org.jetbrains.compose)
  docs/spec.md, docs/plan.md, SYNC.md     (SYNC.md: kopyalanan dosyalar + kaynak commit)
  src/main/kotlin/com/macerce/switchguard/
    core/      ← telefondan kopya (paket adları aynı kalır, karşılaştırması kolay olsun)
    api/       ← telefondan kopya
    desktop/
      Main.kt            application{}: pencere, tepsi, tek örnek, argümanlar (--tray)
      engine/            MonitorEngine (izleme motoru), Scheduler
      data/              Store (settings.json), EventLog, MetricsLog (SQLite), Secrets (DPAPI)
      platform/          Tray, AlarmWindow, Sounds, Toast, Autostart, KeepAwake, SingleInstance
      login/             eWeLink tarayıcı girişi
      transfer/          JmDNS ile cihaz bulma (Transfer protokolü core'dan)
      i18n/              Strings (strings.xml okuyucu), dil seçimi
      ui/                ekranlar (bkz. §8)
  src/main/resources/
    strings/strings.xml, strings-tr.xml   (telefondan kopya + Windows'a özgü metinler)
    sounds/*.wav, icons/
  src/test/kotlin/...    ← telefondaki 102 birim testi (15 dosya) + motor testleri
```

### 3.1 Kopyalanan dosyalar ve uyarlamalar

| Telefondaki dosya | Uyarlama |
|---|---|
| `core/*` (Automation, ChangeDetector, Demo, DeviceLayout, DeviceParser, DeviceSnapshot, DeviceTimers, EnergyParser, Entitlement, EventText, ExpectedChanges, Metrics, OfflineGrace, Rules, Signer, Tuya, WsMessages) | Değişmeden (yalnızca `org.json` bağımlılığı eklenir) |
| `core/Transfer.kt` | `android.util.Base64` → `java.util.Base64`; cihaz adı `Build.MODEL` → bilgisayar adı |
| `core/TransferNet.kt` | Android NSD → JmDNS (`_switchguard._tcp`, aynı protokol) |
| `core/AppLanguage.kt` | Kopyalanmaz; masaüstü `i18n/` yazılır |
| `api/EwelinkClient.kt` | `android.net.Uri` → OkHttp `HttpUrl` |
| `api/TuyaSocket.kt` | `android.util.Log` → küçük günlük arayüzü (`desktop/Log`) |
| `api/EwelinkSocket.kt`, `api/TuyaClient.kt` | Değişmeden |
| `data/Store.kt` | Yeniden yazılır: aynı özellik adları, saklama `settings.json` (bkz. §7) |
| `data/EventLog.kt`, `data/MetricsLog.kt` | Aynı tablo şeması, Android SQLite → `sqlite-jdbc` |
| `data/LiveState.kt` | Değişmeden |
| `service/MonitorService.kt` | Kopyalanmaz; mantığı örnek alınarak `MonitorEngine` yazılır (§4) |

Bağımlılıklar: Compose Desktop (Material 3, ikonlar), OkHttp 4.12, `org.json`, kotlinx-coroutines, `sqlite-jdbc`, JNA + jna-platform (DPAPI, uyanık tutma, kayıt defteri), JmDNS. Hepsi Apache/MIT benzeri izinli lisanslar.

## 4. İzleme motoru (`MonitorEngine`)

`MonitorService`'in Android'e bağlı olmayan mantığının masaüstü karşılığı.

- **İş parçacığı modeli:** Tüm izleme işleri tek iş parçacıklı `ScheduledExecutorService`'te çalışır (telefondaki `HandlerThread` ile aynı garanti: `store.snapshots` okuma/yazma sırası korunur). Arayüz motoru yalnızca `post {}` ile çağırır.
- **Döngü:** 15 sn'de bir `tick()`; zaman sabitleri telefonla aynı: eWeLink yeniden senkron 5 dk (WebSocket canlıyken), yedek sorgulama `pollIntervalSec` (vars. 30 sn), yeniden bağlanma 5/15/30/60/120 sn; Tuya senkron 15 dk (canlı) / 5 dk (sorgulama), engelli 30 dk; uygulamadan yapılan aç/kapa sonrası doğrulama 5 sn; kanal gücü isteği 90 sn, "veri yok" uyarısı 5 dk.
- **Akış (telefonla aynı):** kaynak (WebSocket / HTTP / Tuya Pulsar / demo) → `process()` → `ChangeDetector` → kural (`RuleEngine`, sessiz saatler, çevrimdışı tolerans, beklenen değişiklikler) → `EventLog` + `AlertAction` (ALARM / NOTIFY / IGNORE); otomasyonlar `AutomationEngine` ile.
- **Platform kancaları (`MonitorHost` arayüzü):** `showAlarm(lines, sound)`, `stopAlarm()`, `notify(text, sound)`, `sessionLost(...)`, `clearWarning()`, `statusChanged(state, text)`, `texts` (yerelleştirilmiş metinler). Masaüstünde tepsi + alarm penceresi + ses tarafından uygulanır; motor testlerinde sahte uygulama kullanılır.
- **Uyku algılama (yeni):** İki tick arasında duvar saati 60 sn'den fazla atladıysa bilgisayar uyumuş sayılır: WebSocket ve Tuya bağlantıları baştan kurulur, iki bulut hemen senkronlanır, 2 dk'dan uzun uykularda geçmişe "Bilgisayar N dk uykudaydı — bu sürede kısa olaylar kaçmış olabilir" kaydı düşer.
- **Ağ değişimi:** Android'deki ağ geri geldi bildirimi yok; döngü ve yeniden bağlanma takvimi bağlantıyı en geç ~15 sn içinde yeniden kurar.
- **Bekleyen alarm:** `pendingAlarm` kalıcıdır; uygulama/bilgisayar yeniden başlarsa kapatılmamış alarm geri gelir (telefondaki gibi).
- **Durum:** Kaynakların en kötüsü gösterilir (`publishState` mantığı aynı); tepsi simgesi ve ipucu metni buna göre güncellenir.

## 5. Windows entegrasyonu

- **Pencere ve tepsi:** Ana pencere kapatılınca uygulama tepsiye iner, izleme sürer (ilk seferde bir kez bilgi bildirimi). Tepsi simgesi durum rengi: yeşil canlı, sarı bağlanıyor/sorgulama, kırmızı hata/oturum düştü, gri durdu. Sağ tık menüsü: Aç · İzlemeyi durdur/başlat · Test alarmı · Çıkış (izleme açıkken onay ister). Çift tık pencereyi açar.
- **Alarm penceresi:** Çerçevesiz, ana monitörde ekranı kaplayan, her zaman üstte, kırmızı pencere; olay satırları (saat + metin, en fazla 20) ve büyük "Kapat" düğmesi. Ses kapatılana kadar döngüde çalar. Pencere arkada kalırsa her tick'te öne getirilir (TV'deki `ensureShown` gibi). Uygulama içinden zaten bakılıyorsa da gösterilir (masaüstünde "uygulama önde" bastırması yok).
  - Sınırlar: Windows kilitliyken pencere kilit ekranının üstüne çıkamaz, ses çalar; kilit açılınca pencere görünür. "Rahatsız etmeyin / Odak" açıkken Windows bildirimleri gizlenebilir, alarm penceresi ve ses etkilenmez.
- **Bildirim (NOTIFY):** Tepsi üzerinden Windows bildirimi + cihazın sesi bir kez (en fazla 6 sn, telefondaki gibi).
- **Sesler:** Uygulamayla gelen, bizim ürettiğimiz (telif sorunu olmayan) 4–5 WAV alarm/bildirim sesi; cihaz başına ve genel ses seçimi; bilgisayardan `.wav` dosyası seçilebilir; ses düzeyi ayarı (`alarmVolume`). Çalma: `javax.sound.sampled`.
- **Windows ile başlat:** Ayar (varsayılan açık) → `HKCU\Software\Microsoft\Windows\CurrentVersion\Run\SwitchGuard = "<kurulum>\SwitchGuard.exe" --tray`. `--tray` ile pencere açılmadan tepside başlar; `monitoringEnabled` ise izleme kendiliğinden başlar.
- **Tek örnek:** `%APPDATA%\SwitchGuard\instance.lock` dosya kilidi; ikinci açılış, ilk örneğe yerel bir soket üzerinden "pencereyi göster" der ve kapanır.
- **Bilgisayarı uyanık tut:** Ayar (varsayılan açık; telefondaki "her zaman uyanık" karşılığı). İzleme sürerken `SetThreadExecutionState(ES_CONTINUOUS | ES_SYSTEM_REQUIRED)`; boşta uykuyu engeller, ekran yine kapanabilir. Kapak kapatma ve elle uyku engellenemez → §4 uyku algılama.

## 6. Hesaplar ve aktarma

- **Kural:** Bilgisayar ayrı bir cihazdır. eWeLink için kendi eWeLink hesabı + dev.ewelink.cc geliştirici onayı + o hesabın kendi App ID / App Secret'ı gerekir (telefonda/TV'de/tablette olduğu gibi). Uygulama bu uyarıyı kurulumda ve Ayarlar'da gösterir.
- **eWeLink girişi:** Giriş sayfası varsayılan tarayıcıda açılır.
  - **Deneme A (yedek eWeLink hesabıyla; hesabın geliştirici onayı gelince — 2026-10-06 itibarıyla bekleniyor, bu yüzden 2. aşamanın başında):** eWeLink geliştirici konsolu `http://127.0.0.1:<port>/` biçiminde bir dönüş adresini kabul ediyor mu? Ediyorsa uygulama bu portta küçük bir yerel sunucu açar, dönüşteki `code` + `region`'u yakalar ve oturum açar (Windows rehberi bu adresi söyler).
  - **Yedek yol:** Dönüş adresi telefondaki gibi `https://127.0.0.1` kalır; girişten sonra tarayıcı "bağlanılamıyor" sayfasında kalır; kullanıcı adres çubuğundaki adresi kopyalar; uygulama giriş penceresi açıkken panoyu izler ve `https://127.0.0.1/?code=…` görünce kendiliğinden alır (elle yapıştırma alanı da var).
- **Tuya:** Aynı Tuya anahtarları kullanılabilir (aktarma ile gelir ya da elle girilir).
  - **Deneme B (1. aşamada yapılır):** Tuya mesaj servisi (Pulsar, `Failover` aboneliği) aynı projeye bağlı iki cihaza da her mesajı veriyor mu? `tools/tuya_pulsar_probe.py` ile iki eşzamanlı tüketici denenir (aynı ve farklı abonelik adıyla).
  - Vermiyorsa: Ayarlar'a "Tuya canlı bildirimleri bu cihazda" anahtarı eklenir; masaüstünde **varsayılan kapalı** (telefonun canlı bağlantısını çalmasın); kapalıyken 5 dk'da bir sorgulanır.
- **Aktarma:** Telefondaki "Başka cihaza aktar / Başka cihazdan al" protokolü aynen (6 haneli kod, PBKDF2 + AES-GCM, izin listesi; eWeLink anahtarları ve oturumu aktarılmaz). Cihaz bulma JmDNS ile `_switchguard._tcp`; telefon bilgisayarı, bilgisayar telefonu bulur. Windows Güvenlik Duvarı ilk seferde izin sorabilir (kurulum sırasında kural eklemiyoruz; kullanıcı "İzin ver" der).
- **Demo:** Telefondaki demo cihazları ve simülasyon aynen (hesapsız deneme ve testler için).

## 7. Veri ve güvenlik

- Konum: `%APPDATA%\SwitchGuard\`.
  - `settings.json`: `Store` alanları; geçici dosyaya yazılıp atomik taşıma ile değiştirilir; yazmalar tek kilit altında; değişince `version` akışı artar (arayüz yeniden çizilir).
  - `events.db`, `metrics.db`: telefondakiyle aynı tablo şeması ve saklama süreleri.
- **Gizli alanlar** (`appSecret`, `at`, `rt`, `apikey`, `tuyaSecret`, `tuyaToken`): Windows DPAPI (`CryptProtectData`, kullanıcı kapsamı) ile şifrelenip `settings.json`'a `dpapi:` önekiyle yazılır. Yalnızca aynı Windows kullanıcısı açabilir; başka bilgisayara kopyalanan ayarlarda bu alanlar okunamaz → çıkış yapılmış sayılır.
- Günlük: `%APPDATA%\SwitchGuard\logs\switchguard.log` (dönen, en fazla ~5 MB; gizli bilgi yazılmaz).

## 8. Ekranlar (telefondakilerin hepsi)

Sol menü: **Cihazlar · Otomasyonlar · Geçmiş · Ayarlar**; pencere en az ~900×600, yeniden boyutlanabilir; açık/koyu tema (varsayılan Windows temasını izler, elle seçilebilir).

- **Kurulum sihirbazı:** Hoş geldin → Cihazlarınız nerede (eWeLink / Tuya / başka cihazdan al / hesapsız demo) → eWeLink geliştirici adımı + API anahtarları + giriş (atlanabilir) → Tuya → Windows ayarları (Windows ile başlat, uyanık tut) → bitti. Rehber ve video bağlantıları telefondaki gibi.
- **Cihazlar:** Izgara (kart: ad, çevrim içi durumu, kanallar ve aç/kapa, güç); kartın yanında **ayrıntı paneli**: kurallar (izle, olay başına ALARM/BİLDİRİM/YOK sesi), enerji grafikleri, eWeLink zamanlayıcıları, cihaz verisini kopyala, demo simülasyonu. Düzen düzenleme (sıra, öncelikli, küçük kart, kategori) fareyle sürükle-bırak.
- **Otomasyonlar:** Liste + düzenleyici (telefondaki tetik/eylem türlerinin hepsi).
- **Geçmiş:** Olay listesi, filtreler, görülmemiş rozeti.
- **Ayarlar:** Hesaplar (eWeLink, Tuya, demo) · İzleme (sorgulama aralığı, çevrimdışı tolerans, uyanık tut, Windows ile başlat, Tuya canlı — yalnızca Deneme B gerektirirse) · Alarm (ses, ses düzeyi, sessiz saatler, test alarmı) · Günlük özet · Dil (Sistem / Türkçe / English) · Tema · Aktar / Al · Hakkında (sürüm, rehberler, videolar, iletişim, lisans, Pro durumu).
- **Telefona özgü olup alınmayanlar:** pil optimizasyonu, üretici arka plan ayarları, bildirim/üstte gösterme izinleri, titreşim, Play satın alma ekranı.
- Telefondaki Compose bileşenleri (kartlar, grafik, düzen düzenleyici vb.) gerektiğinde kopyalanıp fare/klavye kullanımına uyarlanır.

## 9. Metinler ve dil

- Telefonun `values/strings.xml` (401 metin) ve `values-tr/strings.xml` dosyaları kopyalanır; Windows'a özgü metinler ayrı `strings-desktop*.xml` dosyalarında.
- Küçük bir okuyucu (`i18n/Strings`) XML'i Android kurallarıyla açar (`\'`, `\"`, `\n`, `%1$s`/`%1$d`); çoğul/dizi kullanılmıyor (kontrol edildi: 0). Dil anında değişir (yeniden başlatma yok).
- Günlük özet ve geçmiş metinleri telefondaki gibi `EventArgs` üzerinden o anki dilde çizilir.

## 10. Paketleme

- Compose Desktop `nativeDistributions` → **`SwitchGuard-Setup-<sürüm>.exe`** (jpackage).
  - Kullanıcı başına kurulum (yönetici izni yok), Başlat menüsü kısayolu, masaüstü kısayolu, "Program ekle/kaldır"dan kaldırma, sabit `upgradeUuid` (yeni sürüm eskisinin üstüne kurulur, ayarlar `%APPDATA%`'da kalır).
  - Java çalışma ortamı exe'nin içinde (kullanıcı ayrıca bir şey kurmaz). Boyut hedefi < 120 MB (ölçülecek).
  - Simge: `docs/brand` logosundan `.ico`.
- Derleme ortamı: `jpackage` içeren JDK 17+ gerekir (Android Studio'nun JBR'sinde yok) → Eclipse Temurin 21 JDK kurulur. WiX Toolset gerekiyorsa (Compose eklentisi kendisi indirebiliyor; 1. aşamada doğrulanacak) kurulur.
- **İmzasız:** İlk kurulumda SmartScreen "Windows bilgisayarınızı korudu" → "Ek bilgi" → "Yine de çalıştır". İmza kararı herkese açılış aşamasında.
- Yayın: İlk sürüm yerel; kullanıcı isteyince GitHub Releases'e `desktop-v1.0.0` etiketiyle (telefonun `v2.x` etiketlerinden ayrı).

## 11. Pro ve lisans anahtarı (herkese açılış aşaması)

- Ücretsiz/Pro kuralı telefondakiyle aynı (`Entitlement`: ücretsiz = 1 gerçek cihaz, demo sınıra sayılmaz). İlk (kişisel) sürümde `isPro = true` sabit.
- Herkese açılışta: Lemon Squeezy ürünü (tek seferlik, aktivasyon sınırı ör. 3 bilgisayar).
  - Ayarlar → Pro → anahtar gir → `POST https://api.lemonsqueezy.com/v1/licenses/activate` (`license_key`, `instance_name` = bilgisayar adı). Yanıttaki mağaza/ürün kimliği bizimkiyle eşleşmeli.
  - Sonuç (`key`, `instance_id`, geçerlilik, son kontrol) DPAPI ile saklanır; açılışta ve 7 günde bir `validate`; internet yokken 30 gün Pro kalır; "Bu bilgisayardan kaldır" → `deactivate` (başka bilgisayara taşımak için).
  - Uygulamaya hiçbir satıcı API anahtarı gömülmez (lisans uç noktaları anahtarsız çalışır).
- Telefonda Play'den alınan Pro, Windows Pro'yu açmaz (Play satın alımını doğrulamak sunucu gerektirir).

## 12. Test

- **Birim testleri:** Telefondaki 102 test (15 dosya) kopyalanan `core` ile birlikte masaüstünde de koşar.
- **Motor testleri (yeni, sahte saat + sahte bulut + sahte `MonitorHost`):** dışarıdan kapatma → ALARM; uygulamadan yapılan değişiklik → alarm yok; sessiz saat → BİLDİRİM; çevrimdışı tolerans içinde geri gelme → kısa kopma kaydı; süreli otomasyon zamanında tetiklenir; uyku boşluğu → yeniden bağlanma + senkron + geçmiş kaydı; WebSocket kalıcı hata → sorgulamaya geçiş; ücretsiz sınır.
- **Elle (bu bilgisayarda, Windows 11):** exe kurulumu/kaldırma/üstüne kurma; tepsi menüsü; Windows ile başlat (yeniden başlatma); demo ile alarm penceresi ve ses; kilitli ekranda alarm; uyku → dönüş; Tuya prizi ile canlı alarm; telefondan aktarma; açık/koyu tema; TR/EN.
- **eWeLink canlı testi:** İkinci bir eWeLink hesabı (cihazlar paylaşılmış, geliştirici onaylı) gerektirir — kullanıcının yedek hesabı var, geliştirici onayı bekleniyor. **Kullanıcının ana eWeLink hesabıyla bilgisayarda giriş yapılmaz** — telefondaki oturumu düşürür.

## 13. Aşamalar

1. **Temel:** Proje iskeleti, kopyalanan `core`/`api` + testleri yeşil, `Store`/`EventLog`/`MetricsLog`/DPAPI, `MonitorEngine` + motor testleri, tepsi, alarm penceresi, sesler, bildirim, tek örnek, Windows ile başlat, uyanık tut, demo modu, basit Cihazlar ekranı, **exe** (kurulup çalışan ilk sürüm). Deneme B bu aşamada yapılır; Deneme A yedek hesabın geliştirici onayını bekler (2. aşamanın başı).
   - Bitti sayılır: exe bu bilgisayara kuruluyor; yeniden başlatmadan sonra tepside açılıyor; demo cihazı simülasyonla kapatılınca alarm penceresi + ses geliyor.
2. **Hesaplar ve cihazlar:** Tuya bağlantısı + canlı, eWeLink tarayıcı girişi, Cihazlar ızgarası + ayrıntı paneli + kurallar + uzaktan aç/kapa.
3. **Otomasyonlar, geçmiş, enerji, zamanlayıcılar, düzen.**
4. **Aktarma, günlük özet, kurulum sihirbazı, dil/tema, Hakkında; son rötuşlar** → ilk tam sürüm `1.0.0`.
5. **Herkese açılış (ayrıca karar verilir):** Lemon Squeezy lisansı, Windows rehberi (TR/EN), GitHub yayını, imza kararı, sürüm denetimi.

**Senkron disiplini:** `SYNC.md` kopyalanan her dosyayı ve kaynak commit'i listeler. Telefonda `core/` veya `api/` değiştiğinde aynı değişiklik masaüstüne taşınır ve `SYNC.md` güncellenir.

## 14. Riskler

| Risk | Önlem |
|---|---|
| eWeLink yerel dönüş adresini kabul etmez | Pano izleyen yedek giriş yolu (§6) |
| Tuya canlı mesajları tek cihaza gider | Deneme B; "bu cihazda canlı" anahtarı, masaüstünde varsayılan kapalı |
| Bilgisayar uyur / kapak kapanır | Uyanık tut + uyku algılama + geçmiş kaydı; kullanıcıya açıkça söylenir |
| Kilit ekranında alarm görünmez | Ses çalar; kilit açılınca pencere görünür (Windows sınırı) |
| İmzasız exe uyarısı | İlk sürümde kabul; herkese açılışta imza değerlendirilir |
| Telefon ve masaüstü mantığı zamanla ayrışır | Aynı paket/dosya adları, `SYNC.md`, kopyalanan testler |
| Ana eWeLink hesabıyla PC'de giriş telefonu düşürür | Uygulamada ve rehberde açık uyarı; testlerde ana hesap kullanılmaz |

## 15. Kapsam dışı (ilk sürüm)

Otomatik güncelleme, kod imzalama, Microsoft Store, macOS/Linux, telefon–Windows ortak Pro, birden fazla monitörde alarm, Windows hizmeti olarak (oturum açmadan) çalışma.
