# Google Play yayın rehberi – SwitchGuard

## 1. Hazır dosyalar
| Dosya | Ne işe yarar |
|---|---|
| `SwitchGuard.aab` | Play Console'a yüklenecek paket |
| `SwitchGuard.apk` | Telefona doğrudan kurulum (test) |
| `release.jks` + `keystore.properties` | **Yükleme (upload) anahtarı. Kaybederseniz güncelleme yayınlayamazsınız.** Proje klasörü dışına (ör. şifreli USB + parola yöneticisi) yedekleyin. Git'e girmez. |
| `docs/privacy-policy.md` | Gizlilik politikası metni (herkese açık bir URL'de yayınlanmalı) |

## 2. Yayından önce yapılacaklar
1. **Play Console hesabı:** https://play.google.com/console (tek seferlik 25 $). Kişisel hesaplarda yayından önce **12 test kullanıcısıyla 14 gün kapalı test** zorunludur.
2. **Gizlilik politikası URL'si:** `docs/privacy-policy.md` içeriğini herkese açık bir sayfada yayınlayın (GitHub Pages, Google Sites vb.) ve bu adresi `Actions.PRIVACY_URL` sabitine yazıp yeniden derleyin.
3. **Play App Signing:** Play Console'da açık bırakın (varsayılan). `release.jks` yalnızca yükleme anahtarı olur.

## 3. Mağaza metinleri

**Uygulama adı:** SwitchGuard – Smart Plug Alarm

**Kısa açıklama (EN, ≤80):** Instant alarm when your Sonoff/eWeLink plugs switch or go offline.

**Kısa açıklama (TR, ≤80):** Sonoff/eWeLink prizleriniz kapanınca ya da koptuğunda anında alarm.

**Tam açıklama (EN):**
> SwitchGuard watches the smart plugs and switches in your eWeLink account and raises an alarm the moment something changes — a freezer plug switched off, a pump that stopped, a device that lost its connection.
>
> • Alarm that keeps ringing until you dismiss it, even in silent mode
> • Live updates within seconds, with automatic backup checks
> • Per-device rules: alarm, notification or nothing — for on, off, offline and online
> • Ignore brief disconnections, quiet hours, a different alarm sound for each device, volume and vibration
> • Automations: alarm when a switch stays on too long, or when a channel's power drops while it should be running (e.g. a pump that stalls — SONOFF SPM-4Relay / DUALR3)
> • Power, voltage and energy readings on power-metering devices
> • Switch devices on and off from the app and manage their eWeLink timers
> • Full event history and an optional daily summary
> • English and Turkish, switchable in the app
> • No ads, no tracking, no account of ours — your data stays on your phone
> • Free for 1 device; a one-time Pro purchase monitors all your devices
>
> Requires a free eWeLink developer App ID (the in-app guide takes you through it in two minutes). Works with SONOFF-branded devices.
>
> SwitchGuard is an independent app and is not affiliated with eWeLink, CoolKit or ITEAD.

**Tam açıklama (TR):**
> SwitchGuard, eWeLink hesabınızdaki akıllı priz ve anahtarları izler ve bir şey değiştiği anda alarm çalar: kapanan bir dondurucu prizi, duran bir pompa, bağlantısı kopan bir cihaz.
>
> • Siz kapatana kadar susmayan alarm; telefon sessizdeyken bile
> • Saniyeler içinde canlı güncelleme, otomatik yedek kontroller
> • Cihaz başına kurallar: açıldı, kapandı, koptu, geri geldi için alarm, bildirim veya hiçbiri
> • Kısa kopmaları yok sayma, sessiz saatler, her cihaz için ayrı alarm sesi, ses seviyesi ve titreşim ayarı
> • Otomasyonlar: bir anahtar fazla uzun açık kalınca ya da çalışması gerekirken bir kanalın gücü düşünce alarm (ör. duran bir pompa — SONOFF SPM-4Relay / DUALR3)
> • Güç ölçen cihazlarda güç, voltaj ve enerji değerleri
> • Cihazları uygulamadan açıp kapatma ve eWeLink zamanlayıcılarını yönetme
> • Tüm olayların geçmişi ve isteğe bağlı günlük özet
> • Türkçe ve İngilizce; uygulama içinden değiştirilebilir
> • Reklam yok, izleme yok, bize ait hesap yok — verileriniz telefonunuzda kalır
> • 1 cihaz için ücretsiz; tek seferlik Pro satın alımıyla tüm cihazlar izlenir
>
> Ücretsiz bir eWeLink geliştirici App ID'si gerekir (uygulama içi rehber iki dakikada adım adım anlatır). SONOFF markalı cihazlarla çalışır.
>
> SwitchGuard bağımsız bir uygulamadır; eWeLink, CoolKit veya ITEAD ile bağlantılı değildir.

**Kategori:** Araçlar (Tools) · **İçerik derecelendirmesi:** Herkes

## 4. Play Console formları için cevaplar

**Veri güvenliği (Data safety):**
- Veri toplanıyor mu? → Kullanıcı verisi yalnızca cihazda işlenir ve kullanıcının kendi eWeLink hesabına iletilir. Geliştiriciye hiçbir veri gönderilmez.
- Beyan: *"Personal info → Email address"* ve *"App activity → Other actions"* **cihazda işlenir, paylaşılmaz**; aktarım şifreli (HTTPS/WSS); kullanıcı verilerini silebilir (çıkış/kaldırma).
- Pro satın alma: ödemeyi Google Play yürütür; uygulama yalnızca "Pro alındı mı" bilgisini telefonda saklar, geliştiriciye gönderilmez → *"Financial info → Purchase history"* işaretlenmez (Google'ın kendi işlediği veri beyana girmez).
- Enerji/güç değerleri cihaz durumudur, kişisel veri sayılmaz; yine yalnızca telefonda tutulur.

**Ön plan hizmeti beyanı (Foreground service – specialUse):**
> The app keeps a live connection to the user's own eWeLink smart plugs/switches and must ring a user-dismissible alarm within seconds when their power or connection state changes (e.g. a freezer or pump switching off). This can't be deferred or done with WorkManager/FCM because the events come from a third-party cloud the app does not control. The service starts only when the user taps "Start monitoring" and shows a persistent status notification.

**Pil optimizasyonu izni (REQUEST_IGNORE_BATTERY_OPTIMIZATIONS):** Google bu izni kısıtlı kabul eder. Gerekçe: *"Core function is real-time safety alerts for user-owned devices; delayed alerts defeat the purpose."* Reddedilirse izni manifestten kaldırın; uygulama otomatik olarak sistem ayarları listesini açan yedek yola geçer (`Actions.requestBatteryExemption`).

**Hedef kitle:** 18+ · **Reklam:** Yok

**Uygulama erişimi (App access) – inceleme ekibi için:**
Uygulama giriş gerektirdiği için "Tüm işlevler veya bazı işlevler kısıtlı" seçilir ve aşağıdaki talimat eklenir.
Önce ayrı bir **inceleme eWeLink hesabı** açın ve eWeLink uygulamasından bir cihazı bu hesaba paylaşın
(inceleme ekibi gerçek bir cihaz görsün). Kendi ana hesabınızın şifresini vermeyin.

> 1. Open the app and go through the setup screens.
> 2. When asked for the eWeLink developer credentials, enter — App ID: `<APP_ID>` · App Secret: `<APP_SECRET>` (leave Redirect URL unchanged).
> 3. Tap "Sign in with eWeLink" and sign in with — Email: `<REVIEW_EMAIL>` · Password: `<REVIEW_PASSWORD>` · Region: Europe.
> 4. A shared smart plug appears on the Devices tab. Tap "Start monitoring".
> 5. Settings → "Test alarm" rings the alarm without touching any device; tap "DISMISS ALARM" to stop it.
> The plug is real hardware at our site; switching it from the app is allowed.
> The free version monitors one device, which is all the reviewer account has; no purchase is needed to review the app.

## 4b. Yayın kontrol listesi (sırayla)
- [x] Gizlilik politikası: https://sites.google.com/view/switchguard-privacy (Google Sites, `Actions.PRIVACY_URL`e yazıldı)
- [ ] Play Console hesabı (25 $) + kimlik doğrulama
- [ ] İnceleme eWeLink hesabı + bir cihaz paylaşımı (yukarıdaki talimat)
- [ ] Uygulama oluştur → formlar (bölüm 4) → mağaza girişi (bölüm 3, 5)
- [ ] Ön plan hizmeti videosu: telefonda ekran kaydı — izlemeyi başlat, cihazı kapat, alarm çalsın, kapat (30 sn yeter); YouTube'a "Liste dışı" yükleyip linki forma yaz
- [ ] Kapalı test kanalı → `SwitchGuard.aab` → 12+ testçi e-postası → 14 gün
- [ ] Üretim erişimi başvurusu → üretim sürümü

## 4c. Uygulama içi ürün (Pro)
- Play Console → **Para kazanma → Ürünler → Uygulama içi ürünler** → Ürün oluştur
  - Ürün kimliği: **`pro_unlimited`** (koddaki `Billing.PRO_ID` ile birebir aynı olmalı)
  - Ad: *SwitchGuard Pro* · Açıklama: *Monitor all your devices (free version monitors 1).*
  - Fiyat: **4,99 USD** → "Diğer ülkeler için fiyatları otomatik ayarla" (Türkiye yerel fiyata çevrilir)
  - Etkinleştir
- Ürün oluşturabilmek için önce **ödeme profili** (banka + vergi bilgisi) tamamlanmalı ve Play'e billing izni içeren bir AAB yüklenmiş olmalı (kapalı test yüklemesi yeterli).
- **Lisans testi:** Ayarlar → Lisans testi → kendi Gmail'in ve testçilerin → "RESPOND_NORMALLY". Bu hesaplar satın almayı gerçek ödeme olmadan dener.
- Fiyat istenildiğinde buradan değiştirilir, uygulama güncellemesi gerekmez.

## 4e. Mağaza dilleri
Play Console → **Mağaza varlığı → Ana mağaza girişi**: varsayılan dil **English (United States) – en-US**.
Sonra **Çeviriler → Kendi çevirilerinizi yönetin → Türkçe – tr-TR** ekleyin ve Türkçe başlık, açıklama, sürüm notu ve `tr/secim/` görüntülerini oraya yükleyin.
Play, telefonu Türkçe olan kullanıcıya Türkçe girişi, diğer herkese İngilizce girişi gösterir. Uygulamanın kendisi de telefonun dilini izler; kullanıcı Ayarlar → Dil'den değiştirebilir.

## 4d. Sürüm notları ("Bu sürümdeki yenilikler", ≤500 karakter)
İlk yayında Play bu alanı ister. Sonraki her güncellemede yalnızca yenilikleri yazın.

**EN:**
> First release: instant alarms for your SONOFF/eWeLink switches, per-device rules, automations (stayed-on and power-drop alarms), energy readings, device timers, history and daily summary.

**TR:**
> İlk sürüm: SONOFF/eWeLink anahtarlarınız için anında alarm, cihaz başına kurallar, otomasyonlar (fazla açık kalma ve güç düşüşü alarmı), enerji değerleri, cihaz zamanlayıcıları, geçmiş ve günlük özet.

## 5. Görseller
- **Uygulama simgesi (512×512):** `docs/brand/play-icon-512.png`
- **Tanıtım görseli (1024×500):** `docs/brand/feature-graphic-1024x500.png` (EN), `docs/brand/feature-graphic-tr-1024x500.png` (TR)
- **Logo kaynağı:** `docs/brand/logo.svg` (her boyutta kullanılabilir)
- **Ekran görüntüleri (v2.5.0, gerçek telefon 1080×2400):** `docs/screenshots/play/`
  - **Yüklenecek olanlar:** `tr/secim/` → Türkçe mağaza girişi, `en/secim/` → İngilizce (varsayılan) giriş. 8'er görüntü, sırayla bir açık bir koyu tema; 8.'si dil seçimi.
  - `tr|en/gece/` ve `tr|en/gunduz/`: aynı 7 ekranın tüm koyu/açık halleri (değiştirmek isterseniz).
  - Not: Geçmiş kayıtları oluştukları andaki dille saklanır; İngilizce görüntülerde eski olaylar Türkçe görünür.
  - `docs/screenshots/` kökündekiler v2.0 emülatör görüntüleridir, kullanmayın.

## 6. Sürüm güncellemek
`app/build.gradle.kts` içinde `versionCode`'u 1 artırıp `versionName`'i değiştirin, sonra:
```
./gradlew bundleRelease   →  app/build/outputs/bundle/release/app-release.aab
```
**Mağaza paketini asla `-PownerUnlock=true` ile derlemeyin** — o bayrak Pro'yu herkese ücretsiz açar. Yüklemeden önce `app/build/generated/source/buildConfig/release/.../BuildConfig.java` içinde `OWNER_UNLOCK = false` olduğunu kontrol edin.
