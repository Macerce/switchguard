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
> • Ignore brief disconnections, quiet hours, custom alarm sound, volume and vibration
> • Switch devices on and off from the app
> • Full event history
> • No ads, no tracking, no account of ours — your data stays on your phone
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
> • Kısa kopmaları yok sayma, sessiz saatler, alarm sesi, ses seviyesi ve titreşim ayarı
> • Cihazları uygulamadan açıp kapatma
> • Tüm olayların geçmişi
> • Reklam yok, izleme yok, bize ait hesap yok — verileriniz telefonunuzda kalır
>
> Ücretsiz bir eWeLink geliştirici App ID'si gerekir (uygulama içi rehber iki dakikada adım adım anlatır). SONOFF markalı cihazlarla çalışır.
>
> SwitchGuard bağımsız bir uygulamadır; eWeLink, CoolKit veya ITEAD ile bağlantılı değildir.

**Kategori:** Araçlar (Tools) · **İçerik derecelendirmesi:** Herkes

## 4. Play Console formları için cevaplar

**Veri güvenliği (Data safety):**
- Veri toplanıyor mu? → Kullanıcı verisi yalnızca cihazda işlenir ve kullanıcının kendi eWeLink hesabına iletilir. Geliştiriciye hiçbir veri gönderilmez.
- Beyan: *"Personal info → Email address"* ve *"App activity → Other actions"* **cihazda işlenir, paylaşılmaz**; aktarım şifreli (HTTPS/WSS); kullanıcı verilerini silebilir (çıkış/kaldırma).

**Ön plan hizmeti beyanı (Foreground service – specialUse):**
> The app keeps a live connection to the user's own eWeLink smart plugs/switches and must ring a user-dismissible alarm within seconds when their power or connection state changes (e.g. a freezer or pump switching off). This can't be deferred or done with WorkManager/FCM because the events come from a third-party cloud the app does not control. The service starts only when the user taps "Start monitoring" and shows a persistent status notification.

**Pil optimizasyonu izni (REQUEST_IGNORE_BATTERY_OPTIMIZATIONS):** Google bu izni kısıtlı kabul eder. Gerekçe: *"Core function is real-time safety alerts for user-owned devices; delayed alerts defeat the purpose."* Reddedilirse izni manifestten kaldırın; uygulama otomatik olarak sistem ayarları listesini açan yedek yola geçer (`Actions.requestBatteryExemption`).

**Hedef kitle:** 18+ · **Reklam:** Yok

## 5. Görseller
- **Uygulama simgesi (512×512):** `docs/brand/play-icon-512.png`
- **Tanıtım görseli (1024×500):** `docs/brand/feature-graphic-1024x500.png` (EN), `docs/brand/feature-graphic-tr-1024x500.png` (TR)
- **Logo kaynağı:** `docs/brand/logo.svg` (her boyutta kullanılabilir)
- **Ekran görüntüleri:** `docs/screenshots/` içinde örnek verilerle çekilmiş görüntüler var. Play en az 2 telefon görüntüsü ister; kendi cihazlarınızla çekilenler daha inandırıcı olur.

## 6. Sürüm güncellemek
`app/build.gradle.kts` içinde `versionCode`'u 1 artırıp `versionName`'i değiştirin, sonra:
```
./gradlew bundleRelease   →  app/build/outputs/bundle/release/app-release.aab
```
