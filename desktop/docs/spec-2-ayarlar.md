# SwitchGuard for Windows — 2. iş: Ayarlar, hesaplar, yedek, sürükle-bırak (spec)

Tarih: 2026-10-07 · Dal: `windows` (Android'den ayrı; birleştirilmez, hiçbir yere yüklenmez) · Ana tasarım: `spec.md`

## 1. Amaç

Bilgisayardaki SwitchGuard gerçek cihazlarla kullanılabilsin: Ayarlar'dan Tuya ve eWeLink hesabı eklenip
kaldırılabilsin, alarm sesi seçilip dosyadan yüklenebilsin, tüm ayarlar şifreli bir dosyaya yedeklenip format
sonrası ya da başka bir bilgisayarda geri yüklenebilsin; Cihazlar ekranında kartlar basılı tutulup sürüklenerek
akıcı biçimde yer değiştirsin ve cihaz başına kural ayarlanabilsin.

**Kullanıcının söyledikleri:** Android'deki gibi Ayarlar bölümü; dosyaya yedekle / dosyadan yükle (şifreli, başka
bilgisayarda da çözülebilsin; format sonrası ya da başka PC'ye); ses dosyası da yedeğe girsin, geri yüklemede tüm
ayarlar gelsin; alarm sesi yükle/değiştir (WAV ve MP3); eWeLink hesabı ekle/kaldır (yedek hesap, geliştirici onayı
bekleniyor); Tuya hesabı ekle/kaldır; cihaz kartları uzun basınca yer değiştirsin, akıcılık çok önemli; kartta basit
kural ayarı; kurulum talimatları ("nasıl kurulur" anlatımları). "Söylediklerin mantıklı, yap bakalım, gerekirse
düzenleriz."

**Başarı ölçütleri**
- Tuya Access ID/Secret/veri merkezi girilince bulunan cihaz sayısı gösterilir; cihazlar Cihazlar ekranında görünür
  ve izlenir. "Bağlantıyı kaldır" ile cihazlar ekrandan kalkar, izlenmez.
- eWeLink App ID/Secret girilip "Giriş yap" ile tarayıcıdan giriş yapılır; girişten sonra kopyalanan adres panodan
  kendiliğinden alınır (elle yapıştırma da var). "Çıkış yap" oturumu, "Hesabı kaldır" anahtarları da siler.
  (Gerçek eWeLink girişi yedek hesabın onayı gelince doğrulanır; o zamana kadar birim testleri.)
- Alarm sesi yerleşik seslerden seçilir ya da WAV/MP3 dosyasından yüklenir; dinleme düğmesiyle denenir; test
  alarmı seçilen sesi çalar. Asıl dosya silinse de ses çalmaya devam eder.
- "Dosyaya yedekle" şifre ister (iki kez), `.sgbackup` dosyası yazar. "Dosyadan geri yükle" şifreyi ister, içeriği
  özetler, onaylanınca tüm ayarları yedektekiyle değiştirir; başka bilgisayarda da aynı şifreyle açılır.
- Kart basılı tutulunca kalkar, sürüklenir, öbür kartlar animasyonla kayar, bırakınca sıra kaydedilir; takılma yok.
- Kartta "İzle" anahtarı ve "Kurallar" penceresi (olay başına ALARM / BİLDİRİM / YOK).
- Tuya ve eWeLink pencerelerinde adım adım "Nasıl kurulur?" anlatımı; Ayarlar'da "Yardım ve kurulum"; masaüstünde
  exe'nin yanında kurulum talimatları dosyası.

## 2. Kapsam

**Dahil:** Sol menü (Cihazlar · Ayarlar) · Ayarlar sayfası (aşağıdaki bölümler) · Tuya ve eWeLink hesap akışları ·
ses yükleme (WAV/MP3) · şifreli yedek/geri yükleme · Cihazlar ızgarasında sürükle-bırak sıralama · kartta İzle +
Kurallar · kurulum talimatları (§7a) · 2. aşama giriş kapısı maddeleri (§9) · sürüm 0.2.0, kurulum, masaüstüne exe.

**Dahil değil (sonraki işler):** Otomasyonlar ve Geçmiş ekranları, cihaz ayrıntı paneli (enerji grafikleri,
zamanlayıcılar, cihaza özel ses seçimi), kategoriler / öncelikli / küçük kart, günlük özet, Wi-Fi aktarma, kurulum
sihirbazı, eWeLink kendiliğinden dönüş adresi (Deneme A: onay gelince).

## 3. Arayüz yapısı

- Ana pencerenin solunda Material 3 `NavigationRail`: **Cihazlar · Ayarlar**. Seçili sekme pencere yeniden
  kurulsa da korunur (`AppState`). Otomasyonlar ve Geçmiş ileride aynı menüye eklenir.
- **Ayarlar:** tek kaydırılabilir sayfa, telefondaki gibi başlıklı bölümler:

| Bölüm | Satırlar |
|---|---|
| Hesaplar | eWeLink · Tuya / Smart Life · Tuya canlı bildirimleri bu bilgisayarda (varsayılan kapalı, açarken uyarı) · Demo cihazları |
| İzleme | Sorgulama aralığı · Çevrimdışı tolerans · Bilgisayarı uyanık tut · Windows ile başlat |
| Alarm | Alarm sesi · Bildirim sesi · Ses düzeyi · Sessiz saatler (aç/kapa, başlangıç, bitiş) · Test alarmı |
| Genel | Dil (Sistem / Türkçe / English) · Tema (Sistem / Açık / Koyu) |
| Yedekleme | Dosyaya yedekle… · Dosyadan geri yükle… |
| Yardım ve kurulum | Windows'a kurulum · Tuya kurulumu · eWeLink kurulumu (her biri uygulama içi adım adım anlatım + resimli rehber ve video bağlantısı) |
| Hakkında | Sürüm · Günlük klasörünü aç |

- Metinler: telefonda karşılığı olan satırlar telefonun anahtarlarını kullanır (`section_*`, `poll_interval`,
  `quiet_hours`, `tuya_*` …); yalnızca masaüstüne özgüler `desktop.xml`'e eklenir (EN + TR).
- Ayar değişikliği `Store`'a hemen yazılır; motoru ilgilendirenler (`pollIntervalSec`, `offlineGraceSec`,
  `tuyaLive`, hesaplar, `alwaysAwake`) `engine.restart()` ile uygulanır (izleme açıksa); `autostart` kayıt
  defteriyle hemen eşitlenir; dil ve tema anında değişir (`store.version`).

## 4. Hesaplar

### 4.1 Tuya
- Satır durumu: "Bağlı değil" / "Bağlı · <veri merkezi> · N cihaz" (telefon metinleri).
- "Bağla" penceresi: Access ID, Access Secret (gizli alan), veri merkezi (Orta Avrupa / Batı Amerika / Çin /
  Hindistan), açıklama ve paylaşılan cihaz ipucu (telefon metinleri). "Bağlan" arka planda bağlantıyı dener
  (token + cihaz listesi). Başarılıysa bilgiler kaydedilir, "N cihaz bulundu" gösterilir, `engine.restart()`.
  Başarısızsa önceki bilgiler geri konur ve hata metni gösterilir (`tuya_auth_error`, internet yok, deneme süresi
  doldu). Deneme sırasında düğmeler kilitli, "Tuya'ya bağlanılıyor…".
- "Bağlantıyı kaldır": onay → `store.disconnectTuya()` → `engine.restart()`; Tuya cihazları ekrandan kalkar.

### 4.2 eWeLink
- Satır durumu: anahtar yok / anahtar var, giriş yok / "Giriş yapıldı · <maskeli e-posta> · bölge".
- Uyarı (her zaman görünür): bu bilgisayar ayrı bir cihazdır; kendi eWeLink hesabı, o hesapta dev.ewelink.cc
  geliştirici onayı ve o hesabın App ID / App Secret'ı gerekir.
- "API bilgileri" penceresi: App ID, App Secret (gizli), dönüş adresi (varsayılan `https://127.0.0.1`), bölge.
- "Giriş yap" penceresi (yedek yol, `spec.md` §6):
  1. Uygulama rastgele `state` üretir, `EwelinkClient.buildLoginUrl(state)` adresini varsayılan tarayıcıda açar.
  2. Pencere adımları anlatır: giriş yapın → tarayıcı "bağlanılamıyor" sayfasında kalır → adres çubuğundaki adresi
     kopyalayın.
  3. Pencere açıkken pano 500 ms'de bir okunur; dönüş adresiyle başlayan, `code` içeren ve `state`'i eşleşen bir
     adres görülünce kendiliğinden alınır. Elle yapıştırma alanı da vardır.
  4. `code` + `region` ile `exchangeCode` arka planda çalışır; başarılıysa pencere kapanır, `engine.restart()`.
     `state` eşleşmezse ya da kod reddedilirse açık hata metni, pencere açık kalır.
- "Çıkış yap": `store.logout()` (oturum silinir, anahtarlar kalır) → `engine.restart()`.
- "Hesabı kaldır": onay → çıkış + App ID/Secret silinir → `engine.restart()`; eWeLink cihazları ekrandan kalkar.

## 5. Sesler

- Kimlikler: `builtin:<ad>` (mevcut), `user:<dosya>` (yeni): `%APPDATA%\SwitchGuard\sounds\<dosya>` altındaki
  kopya. Mevcut mutlak yol kimliği (`SoundSource.File`) geriye dönük kalır.
- "Dosyadan yükle…" (Windows dosya seçicisi; `.wav`, `.mp3`): dosya en çok 20 MB. Yüklemede 16 bit PCM WAV'a
  çevrilir (MP3 çözümü `mp3spi` + `jlayer` ile, yalnızca yükleme anında), en çok 60 sn tutulur (uzunsa kesilir ve
  söylenir), içerik özetinden (SHA-256) adlandırılarak `sounds\` altına yazılır. Böylece çalma yolu (güvenlik açısından
  kritik `SoundPlayer`) değişmez; asıl dosya silinse de ses çalar; aynı dosya iki kez yüklenirse tek kopya olur.
  Bozuk / ses olmayan dosya: hata metni, ayar değişmez.
- Alarm sesi listesi: yerleşik sesler + yüklenmiş sesler; her biri için ▶ dinle (`once`, ses düzeyiyle). Seçim
  `alarmSoundUri`'ye yazılır.
- Bildirim sesi: yeni `notifySoundUri` (varsayılan `builtin:notify_chime`); `DesktopHost.notify` cihaza özel ses
  yoksa bunu çalar. Aynı seçici.
- Kullanılmayan yüklenmiş sesler silinmez (yedekten gelenler dahil); "Kaldır" düğmesi sonraki iş.

## 6. Yedek ve geri yükleme

### 6.1 Dosya biçimi (`.sgbackup`, UTF-8 JSON zarf)

```json
{ "format": "switchguard-backup", "version": 1, "created": "2026-10-07T12:00:00Z",
  "kdf": { "name": "PBKDF2WithHmacSHA256", "iterations": 600000, "salt": "<b64 16 bayt>" },
  "cipher": { "name": "AES/GCM/NoPadding", "iv": "<b64 12 bayt>" },
  "data": "<b64 şifreli içerik + 128 bit etiket>" }
```

- Anahtar: PBKDF2-HMAC-SHA256(şifre, salt, 600 000) → 256 bit; AES-256-GCM. Zarfın `format/version/created/kdf`
  alanları GCM ek doğrulama verisi (AAD) olarak bağlanır: biri değiştirilirse çözme başarısız olur.
- Şifre en az 8 karakter, iki kez girilir; unutulursa yedek açılamaz (pencerede yazar). Şifre hiçbir yerde saklanmaz.
- Telefon aktarmasıyla aynı yapı taşları (PBKDF2 + AES-GCM); yineleme sayısı kullanıcı şifresi için yükseltildi.

### 6.2 İçerik (çözülmüş JSON)

`{ "app": "0.2.0", "settings": { … }, "sounds": { "user:<dosya>": "<b64>" } }`

- **Girenler (izin listesi):** eWeLink anahtarları `appId, appSecret, redirectUrl, region` · Tuya `tuyaId,
  tuyaSecret, tuyaRegion, tuyaScales, tuyaLive` · `rules, automations, layout, freeDevice` · ses `alarmSound,
  notifySound, alarmVolume` · sessiz saatler `quietEnabled, quietStart, quietEnd` · izleme `monitoring,
  pollIntervalSec, offlineGraceSec, alwaysAwake` · `autostart, language, theme, demo, onboardingDone,
  summaryEnabled, summaryMinute`. Gizli alanlar şifreli dosyanın içinde düz metindir; diske DPAPI ile ancak geri
  yüklemede yeniden kodlanır.
- **Sesler:** `alarmSound`, `notifySound` ve kurallardaki `soundUri` alanlarından başvurulan her `user:` dosyası
  gömülür.
- **Girmeyenler:** eWeLink oturumu (`at, rt, apikey, email, sessionLost`), Tuya token'ı (`tuyaToken,
  tuyaTokenExp`; yeniden alınır), `snapshots`, `pendingAlarm`, `historySeen`, `transferReceived, transferSource`,
  olay geçmişi ve enerji veritabanları.

### 6.3 Geri yükleme
1. Dosya seçilir, şifre sorulur. Yanlış şifre ya da bozuk dosya: "Şifre yanlış ya da dosya bozuk" (GCM doğrulaması).
   `format` tanınmıyorsa ya da `version` bu sürümden yeniyse açık hata.
2. Özet gösterilir: oluşturulma tarihi, uygulama sürümü, Tuya var/yok, eWeLink anahtarları var/yok, kural ve
   otomasyon sayısı, gömülü ses sayısı. Uyarı: "Bu bilgisayardaki tüm ayarlar yedektekiyle değiştirilecek."
3. Onaylanınca: izleme durdurulur → mevcut `settings.json` `settings.json.before-restore-<ms>` olarak kopyalanır →
   sesler `sounds\`'a yazılır → `Store` tek kilit altında yedekteki izin listesiyle **değiştirilir** (listede olup
   yedekte olmayan anahtarlar silinir; oturum, anlık görüntüler ve bekleyen alarm temizlenir) → dil/tema/Windows ile
   başlat uygulanır → `monitoring` açıksa izleme başlar. eWeLink anahtarı varsa "Giriş yapın" uyarısı gösterilir.
4. Herhangi bir adım başarısız olursa `Store`'a dokunulmamışsa hiçbir şey değişmez; değişmişse kopyadan geri alınır
   ve hata gösterilir.

## 7. Cihazlar ekranı

- Izgara `LazyVerticalGrid`; sıralama `sh.calvin.reorderable:reorderable` 3.1.0 (Apache-2.0, Compose Multiplatform,
  `reorderable-jvm`; Compose ≥ 1.7 ister, bizde 1.9.3).
- Kartın gövdesine ~500 ms basılı tutunca kart kalkar (hafif büyüme + gölge), imleci/parmağı izler; öbür kartlar
  `animateItem` ile kayar; pencere kenarında otomatik kaydırma. Kısa tıklama ve kart üstündeki anahtar/düğmeler
  sürüklemeyi başlatmaz.
- Akıcılık kuralları: sürükleme sırasında sıra yalnızca bellekteki listede değişir; `store.deviceLayout` **bırakınca
  bir kez** yazılır. Kartın konumu çizim katmanında güncellenir (her karede bileşim kurulmaz). Kart içerikleri
  `key = device.id` ile kararlı. Motorun anlık görüntü güncellemeleri sürükleme sırasında listeyi yeniden
  sıralamaz (sürükleme bitince birleştirilir).
- Sıra `DeviceLayout.order`'da (telefonla aynı model); yeni cihazlar sona ada göre eklenir (mevcut `sorted`).
- Kartta "İzle" anahtarı (`DeviceRules.monitored`) ve ⋮ menüsünde "Kurallar…": olay başına (kapandı, açıldı,
  çevrimdışı oldu, çevrimiçi oldu) ALARM / BİLDİRİM / YOK. Yazma motor üzerinden (`spec.md` §4: arayüz yalnızca
  motora gönderir); mevcut demo aç/kapa yarışı (M5) da bu yola alınır.

## 7a. Kurulum talimatları

- **Kaynak:** depodaki resimli rehberler `docs/guide/{tuya,ewelink}.{tr,en}.md` (telefonun bağlandığı
  `https://github.com/Macerce/switchguard/blob/master/docs/guide/<bulut>.<dil>.md`) ve videolar (Tuya
  `https://youtu.be/sCdvMs7CSic`, eWeLink `https://youtu.be/jK_KcG2OPao`). Uygulama içi metinler bu rehberlerin
  kısaltılmış adımlarıdır; internet olmadan okunur (`desktop.xml`, EN + TR).
- **Tuya penceresi → "Nasıl kurulur?" (açılır bölüm):** 1) platform.tuya.com'da ücretsiz hesap · 2) Cloud →
  Development → Create Cloud Project (veri merkezi: Türkiye/Avrupa için Orta Avrupa) → Authorize · 3) Devices → Link
  App Account → Smart Life'ta QR kodu okut · 4) Message Service'i aç, kural oluştur, **kuralın anahtarını aç** ·
  5) Overview'dan Access ID ve Access Secret'ı kopyala · 6) buraya yapıştır, veri merkezini seç, Bağlan. Düğmeler:
  "platform.tuya.com'u aç", "Resimli rehberi aç", "Videoyu izle". Telefondakiyle aynı anahtarlar kullanılabilir notu.
- **eWeLink penceresi → "Nasıl kurulur?":** 1) Bu bilgisayar için ayrı bir eWeLink hesabı (eWeLink uygulamasından
  açılır, cihazlar o hesapla paylaşılır) · 2) dev.ewelink.cc'de o hesapla giriş → Console → bireysel geliştirici
  başvurusu → **eWeLink'in onayını bekle** · 3) App List → Create, Redirect URL `https://127.0.0.1` (birebir) · 4)
  View → APPID ve APP SECRET'ı kopyala · 5) "API bilgileri"ne yapıştır · 6) "Giriş yap" → tarayıcıda giriş →
  "bağlanılamıyor" sayfası normaldir → adres çubuğundaki adresi kopyala, SwitchGuard kendiliğinden alır. Düğmeler:
  "dev.ewelink.cc'yi aç", "Redirect URL'yi kopyala", "Resimli rehberi aç", "Videoyu izle".
- **Ayarlar → Yardım ve kurulum → Windows'a kurulum:** kurulum exe'sini çalıştırma; imzasız olduğu için SmartScreen'de
  "Ek bilgi → Yine de çalıştır"; yönetici izni istemez, `%LOCALAPPDATA%\SwitchGuard`'a kurulur; masaüstü ve Başlat
  kısayolu; Windows ile başlar ve tepside durur (kapatınca tepside çalışmaya devam eder, tamamen çıkmak için tepsi →
  Çıkış); başka bilgisayara geçiş: eski bilgisayarda yedek al → yenisinde kur → Dosyadan geri yükle → eWeLink'e
  yeniden giriş; kaldırma: Windows Ayarlar → Uygulamalar → SwitchGuard → Kaldır.
- **Masaüstündeki talimat dosyası:** `SwitchGuard Kurulum Talimatları.txt` (Türkçe, UTF-8 BOM'lu, Not Defteri'nde
  doğru açılır), aynı Windows kurulumu metni + Tuya/eWeLink rehber bağlantıları; kaynağı `desktop/docs/kurulum.tr.txt`.
- Bağlantılar varsayılan tarayıcıda açılır (`Desktop.browse`); açılamazsa adres panoya kopyalanır ve söylenir.

## 8. Mimari birimler (yeni / değişen)

| Birim | Görev |
|---|---|
| `ui/AppNav.kt` | Sol menü + sekme durumu |
| `ui/settings/SettingsScreen.kt` (+ bölüm dosyaları) | Ayarlar sayfası ve pencereleri |
| `accounts/TuyaConnect.kt` | Bilgileri dener, başarıda kaydeder, başarısızlıkta geri alır (arayüzsüz, test edilir) |
| `accounts/EwelinkLogin.kt` | `state` üretimi, dönüş adresi ayrıştırma/doğrulama, kod değişimi (arayüzsüz) |
| `platform/ClipboardWatcher.kt` | Panoyu aralıklı okur, eşleşen adresi bildirir |
| `sound/SoundImport.kt` | WAV/MP3 → 60 sn'lik PCM WAV, `user:` kimliği |
| `backup/BackupCrypto.kt` | Zarf, PBKDF2 + AES-GCM, AAD |
| `backup/BackupContent.kt` | İzin listesi, sesleri gömme, özet |
| `backup/Restore.kt` | Güvenli kopya, değiştirme, geri alma |
| `data/Store.kt` | `notifySoundUri`; izin listesi dışa/içe aktarma (`exportSettings`, `replaceSettings`) |
| `ui/DevicesScreen.kt` | Reorderable ızgara, İzle, Kurallar |
| `engine/MonitorEngine.kt` | `setRules`, `setMonitored` (motor üzerinden), §9 düzeltmeleri |
| `ui/settings/HelpSection.kt`, `ui/settings/SetupSteps.kt` | Yardım ve kurulum bölümü, açılır adım listeleri, bağlantı düğmeleri |
| `platform/Links.kt` | Tarayıcıda aç / olmazsa panoya kopyala; rehber ve video adresleri (dil seçimine göre) |

## 9. 2. aşama giriş kapısı (gerçek hesaplar gelmeden önce zorunlu)

- **Yavaş kapatma (I6):** "Kapat" (alarm penceresi ve tepsi) sesi ve pencereyi **arayüz tarafında hemen** kapatır
  (`DesktopHost.silenceNow`), `pendingAlarm` temizliği motora gönderilir. Bulut çağrısı tıkalıyken bile siren
  düğmeye basınca susar. Motor temizliği yaptığında arada yeni alarm geldiyse pencere yeniden açılır.
- **Yanlış uyku kaydı (I6):** uyku = iki tick arası duvar saati boşluğu − bilgisayarın uyanık geçirdiği süre
  (`QueryUnbiasedInterruptTime`; `nanoTime`/QPC uykuda da saydığı için kullanılamaz). Uzun süren bir bulut çağrısı
  "bilgisayar uyudu" sayılmaz; uyku bir çağrının ortasında olsa da ölçülür.
- **DPAPI geçici hatası (M6):** çözülemeyen gizli alan "çıkış yapılmış" sayılmadan önce bir kez yeniden denenir ve
  günlüğe yazılır.

## 10. Hata durumları

- Ağ çağrıları (Tuya deneme, eWeLink kod değişimi) arayüz iş parçacığında asla çalışmaz; zaman aşımı mevcut
  istemcilerdeki gibi; hata metinleri telefonla aynı.
- Dosya işlemleri (yedek yazma/okuma, ses kopyalama) hata verirse ayar değişmez, açık hata metni.
- Yedek dosyası en çok 100 MB okunur (sesler en çok 60 sn); daha büyüğü reddedilir.
- Gizli bilgi (şifre, Secret, token, kod) günlüğe yazılmaz.

## 11. Test

- Birim: yedek gidiş-dönüş (ayarlar + sesler), yanlış şifre, zarf/AAD kurcalama, yeni sürüm reddi, izin listesi
  (oturum ve token dışarıda), geri yüklemenin değiştirmesi ve hata durumunda geri alma; ses yükleme (WAV, MP3, 60 sn
  kesme, ses olmayan dosya, aynı dosya tek kopya); `user:` çözümleme; Tuya bağlantısı (sahte istemci: başarı,
  yetki hatası → geri alma); eWeLink dönüş adresi ayrıştırma (`code`, `region`, `state` eşleşmesi, yanlış adres);
  pano izleyicisi; motor: `setRules`/`setMonitored`, hesap kaldırınca cihazların düşmesi, §9 düzeltmeleri; sıralama
  mantığı (taşı → sıra; bırakınca tek yazma).
- Elle (kurulu uygulamada): Ayarlar'ın tüm satırları; MP3 yükleyip test alarmı; yedekle → ayarları değiştir → geri
  yükle; sürükle-bırak akıcılığı (çok kartla, hızlı sürükleme, kenar kaydırma); kural değiştirip demo simülasyonu.
  Gerçek Tuya: kullanıcının anahtarlarıyla (kullanıcıyla birlikte). Gerçek eWeLink: yedek hesabın onayı gelince.

## 12. Paketleme

Sürüm 0.2.0; yeni bağımlılıklar `reorderable` (Apache-2.0), `mp3spi` + `jlayer` + `tritonus-share` (LGPL, ayrı
jar olarak paketlenir). Kurulum yönetici olmadan (`explorer.exe`), MSI eski sürümü kendisi kaldırır; kurulum exe'si
`Desktop\SwitchGuard-0.2.0-Kurulum.exe`.
