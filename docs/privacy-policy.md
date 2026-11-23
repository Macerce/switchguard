# SwitchGuard – Privacy Policy / Gizlilik Politikası

_Last updated / Son güncelleme: 18 November 2026 / 18 Kasım 2026_

## English

SwitchGuard ("the app") monitors smart devices in **your own** eWeLink and/or Tuya (Smart Life) account and alerts you when they change. It is developed by an independent developer and is not affiliated with eWeLink, CoolKit, ITEAD or Tuya.

**What the app stores (only on your device):**
- The eWeLink developer App ID and App Secret you enter.
- The access/refresh tokens eWeLink issues after you sign in, your eWeLink user key and email address.
- If you connect Tuya: the Access ID and Access Secret of your own Tuya Cloud project and the temporary access token Tuya issues.
- The names and on/off/online states of your devices, energy readings of power-metering devices, readings of your eWeLink sensors (temperature, humidity, light, door, motion, water leak, battery) and their history, your alert rules, automations and settings (including the alarm sounds you pick).
- A history of device events (at most 2,000 entries) and power/sensor readings (kept for up to 400 days).

**Who the app talks to:** only the official servers of the platforms you connect — eWeLink (`*.coolkit.cc` / `*.coolkit.cn`) and Tuya (`openapi.tuya*.com`, message service `mqe.tuya*.com`) — to sign you in, read your devices' states, receive live updates and switch devices on or off when you ask it to. If you turn on "Tell someone else", it also talks to Telegram (`api.telegram.org`); if you use temperature rules, it asks Open-Meteo (`api.open-meteo.com`) for the outside temperature; zones use OpenStreetMap map tiles and Open-Meteo place search (see below). Copies installed outside Google Play check GitHub (`api.github.com`) for new versions; this request contains nothing about you or your devices. The app has **no server of its own**, and contains **no analytics, advertising or tracking SDKs**. No data is sold or shared with third parties.

**No purchases:** the app is free with every feature, on Android and Windows. It has no in-app purchases, subscriptions, license keys or payment code, and collects no payment information.

**Your password** is entered on eWeLink's own sign-in page; the app never sees or stores it.

**Transfer to your other devices:** if you use "Send to another device", the app sends your Tuya keys, rules, automations and settings directly to your own TV or tablet on the same home Wi-Fi network, encrypted with the one-time 12-character code shown on that device. Nothing passes through any server; eWeLink API keys and the eWeLink session (sign-in) are never transferred.

**Local network control:** the app finds your eWeLink devices on your Wi-Fi (mDNS) and sends on/off commands to them directly, encrypted with each device's own key from your eWeLink device list. This traffic stays inside your network; the keys are stored encrypted on the device and are not backed up. Devices you add yourself from the local network (Shelly, and SONOFF devices in DIY mode) are found the same way (mDNS or the IP address you type) and controlled over plain HTTP inside your network, with no account or cloud. A Shelly login password you enter is stored encrypted on the device only and is not backed up or transferred.

**Heartbeat URL, ntfy and webhook:** off unless you enter an address. With a heartbeat URL (e.g. healthchecks.io, Uptime Kuma) the app calls that address every few minutes while monitoring is healthy; the call carries no data. With an ntfy topic or a webhook, when a real alarm starts the app sends the alarm text, the time and this device's model name to the address you entered, and a short message when the alarm is dismissed. Nothing goes anywhere else, and anyone who knows a public ntfy topic can read its messages.

**Telling someone else (Telegram):** this is off unless you turn it on. If an alarm isn't dismissed within the time you choose, the app sends the alarm's text (time, device name and what happened) and your phone's name to the people or groups you picked, through a Telegram bot that you create and own; it sends one more message when the alarm is dismissed. If you turn them on, the same bot also carries power-outage messages, the daily summary and weekly report (device names, events and energy figures), and a pinned "watchdog" message that shows the app is running. The bot only accepts commands (status, take over an alarm, switch a device, run a scene, home/away) from the chats you picked; other chats are ignored. The bot token and the chosen chats are stored only on your device. Telegram's own privacy policy applies to these messages.

**Location and weather (optional):** sunrise/sunset and outside-temperature rules need a location. You type it in, or tap "Use this device's location" (approximate location permission, read only when you tap). Before Android's permission request, the app shows its own window explaining why location is needed and what it is used for; you can decline and type the place instead. It is stored only on your device. For temperature rules the app asks Open-Meteo (`api.open-meteo.com`, no account) for the current temperature, sending the location rounded to about 100 m and nothing else.

**Zones (optional):** if you create a zone (an area on a map, e.g. your home) and use it in an automation, the app asks for precise location "all the time" (background location), after explaining why in its own window shown before Android's permission request. Android's geofencing (Google Play services) then tells the app when you enter or leave your zones, even while the app is closed; the app does not track or record your route. The map's "My current location" button asks the same way. Zones and your location stay only on your device and are never sent to us. To draw the map, the app downloads map tiles of the area you are looking at from OpenStreetMap (`tile.openstreetmap.org`); place names you search for are sent to Open-Meteo's place search (`geocoding-api.open-meteo.com`). You can delete zones or revoke the permission at any time.

**Phone calls on a critical alarm (optional):** if you set a phone number for a person under "Call on critical alarm", the app calls that number from this phone's own line (phone call permission) when a critical alarm isn't dismissed. The number is stored only on your device. The call is placed by your phone carrier like any normal call; nothing else is sent.

**Sensor readings:** values from your eWeLink sensors (temperature, humidity, light, door, motion, water leak, battery) come from your own eWeLink account like any other device state. The app keeps their history to draw charts and evaluate your rules; it is stored only on your device and is never sent anywhere.

**Charger as a power sensor (optional):** the app reads whether this phone or tablet is charging, only on the device.

**Entering keys from your phone (TV):** on a TV, "Enter from phone" shows a QR code that opens a page served by the TV itself on your home network. What you type there is encrypted in your phone's browser with a one-time key from the QR code and goes straight to the TV; it never leaves your home network.

**Voice commands (optional):** when you tap the microphone, speech is recognised by your device's built-in speech service (for example Google's on Android); the app receives only the recognised text and handles it on the device. Spoken answers use the device's text-to-speech.

**Backups:** app data is excluded from Android cloud backup and device transfer.

**Deleting your data:** sign out in Settings, clear the history, or uninstall the app — all data is removed from the device. You can also revoke access at any time from your eWeLink developer account or by deleting your Tuya Cloud project / unlinking your app account from it.

**Children:** the app is not directed at children.

**Contact:** switchguardapp@gmail.com

## Türkçe

SwitchGuard ("uygulama"), **size ait** eWeLink ve/veya Tuya (Smart Life) hesabındaki akıllı cihazları izler ve değiştiklerinde sizi uyarır. Bağımsız bir geliştirici tarafından geliştirilmiştir; eWeLink, CoolKit, ITEAD veya Tuya ile bağlantısı yoktur.

**Uygulamanın sakladıkları (yalnızca cihazınızda):**
- Girdiğiniz eWeLink geliştirici App ID ve App Secret bilgileri.
- Girişten sonra eWeLink'in verdiği erişim/yenileme token'ları, eWeLink kullanıcı anahtarınız ve e-posta adresiniz.
- Tuya bağlarsanız: size ait Tuya Cloud projesinin Access ID ve Access Secret bilgileri ile Tuya'nın verdiği geçici erişim token'ı.
- Cihazlarınızın adları, açık/kapalı/çevrimiçi durumları, enerji ölçen cihazların ölçümleri, eWeLink sensörlerinizin değerleri (sıcaklık, nem, ışık, kapı, hareket, su kaçağı, pil) ve geçmişleri, alarm kurallarınız, otomasyonlarınız ve ayarlarınız (seçtiğiniz alarm sesleri dahil).
- Cihaz olaylarının geçmişi (en fazla 2.000 kayıt) ve güç/sensör ölçümleri (en fazla 400 gün).

**Uygulamanın iletişim kurduğu yerler:** yalnızca bağladığınız platformların resmi sunucuları: eWeLink (`*.coolkit.cc` / `*.coolkit.cn`) ve Tuya (`openapi.tuya*.com`, mesaj servisi `mqe.tuya*.com`). Bu bağlantı giriş yapmak, cihaz durumlarını okumak, anlık güncellemeleri almak ve istediğinizde cihazları açıp kapatmak için kullanılır. "İkinci kişiye haber ver" özelliğini açarsanız Telegram'a (`api.telegram.org`), sıcaklık kuralı kullanırsanız dış sıcaklık için Open-Meteo'ya (`api.open-meteo.com`) da bağlanır; bölgeler OpenStreetMap harita karolarını ve Open-Meteo yer aramasını kullanır (aşağıya bakın). Google Play dışından kurulan kopyalar yeni sürüm için GitHub'a (`api.github.com`) bakar; bu istek sizinle veya cihazlarınızla ilgili hiçbir bilgi içermez. Uygulamanın **kendi sunucusu yoktur**; **analiz, reklam veya izleme kütüphanesi içermez**. Hiçbir veri satılmaz veya üçüncü taraflarla paylaşılmaz.

**Satın alma yok:** uygulama Android'de ve Windows'ta tüm özellikleriyle ücretsizdir. Uygulama içi satın alma, abonelik, lisans anahtarı veya ödeme kodu içermez; hiçbir ödeme bilgisi toplamaz.

**Şifreniz** eWeLink'in kendi giriş sayfasına girilir; uygulama şifrenizi hiçbir zaman görmez ve saklamaz.

**Diğer cihazlarınıza aktarma:** "Başka cihaza aktar" özelliğini kullanırsanız uygulama Tuya bilgilerinizi, kurallarınızı, otomasyonlarınızı ve ayarlarınızı aynı ev Wi-Fi ağındaki kendi TV'nize ya da tabletinize, o cihazda görünen tek seferlik 12 karakterlik kodla şifreleyerek doğrudan gönderir. Hiçbir sunucudan geçmez; eWeLink API anahtarları ve eWeLink oturumu (girişi) hiçbir zaman aktarılmaz.

**Yerel ağ kontrolü:** uygulama Wi-Fi ağınızdaki eWeLink cihazlarını (mDNS ile) bulur ve aç/kapa komutlarını, eWeLink cihaz listenizdeki cihaza özel anahtarla şifreleyerek doğrudan cihaza gönderir. Bu trafik ağınızın dışına çıkmaz; anahtarlar cihazda şifreli saklanır ve yedeğe girmez. Yerel ağdan kendiniz eklediğiniz cihazlar (Shelly ve DIY kipindeki SONOFF) aynı şekilde (mDNS ya da yazdığınız IP adresiyle) bulunur ve hesap ya da bulut olmadan ağınızın içinde HTTP ile kontrol edilir. Girdiğiniz Shelly giriş şifresi yalnızca cihazda şifreli saklanır; yedeğe ve aktarıma girmez.

**Canlılık adresi, ntfy ve webhook:** siz bir adres girmedikçe kapalıdır. Canlılık adresi (ör. healthchecks.io, Uptime Kuma) girerseniz uygulama, izleme sağlıklıyken birkaç dakikada bir bu adrese veri içermeyen bir istek gönderir. ntfy konusu ya da webhook girerseniz gerçek bir alarm başlayınca alarm metni, saati ve bu cihazın model adı girdiğiniz adrese, alarm kapatılınca da kısa bir mesaj gönderilir. Başka hiçbir yere gitmez; herkese açık bir ntfy konusunu bilen herkes mesajlarını okuyabilir.

**İkinci kişiye haber ver (Telegram):** siz açmadıkça kapalıdır. Alarm seçtiğiniz süre içinde kapatılmazsa uygulama, alarmın metnini (saat, cihaz adı ve ne olduğu) ve telefonunuzun adını seçtiğiniz kişi ya da gruplara, sizin oluşturduğunuz ve size ait bir Telegram botu üzerinden gönderir; alarm kapatılınca bir mesaj daha gönderir. Açarsanız aynı bot elektrik kesintisi mesajlarını, günlük özeti ve haftalık raporu (cihaz adları, olaylar ve enerji rakamları) ve uygulamanın çalıştığını gösteren sabitlenmiş bir "bekçi" mesajını da iletir. Bot komutları (durum, alarmı üstlenme, cihaz açıp kapatma, sahne çalıştırma, evde/dışarıda) yalnızca seçtiğiniz sohbetlerden kabul eder; diğer sohbetler yok sayılır. Bot anahtarı ve seçilen sohbetler yalnızca cihazınızda saklanır. Bu mesajlar için Telegram'ın kendi gizlilik politikası geçerlidir.

**Konum ve hava durumu (isteğe bağlı):** gün doğumu/batımı ve dış sıcaklık kuralları bir konuma ihtiyaç duyar. Konumu siz yazarsınız ya da "Bu cihazın konumunu kullan"a dokunursunuz (yaklaşık konum izni; yalnızca dokunduğunuzda okunur). Android'in izin isteğinden önce uygulama, konumun neden gerektiğini ve ne için kullanıldığını anlatan kendi penceresini gösterir; reddedip yeri elle yazabilirsiniz. Yalnızca cihazınızda saklanır. Sıcaklık kuralları için uygulama Open-Meteo'ya (`api.open-meteo.com`, hesap gerekmez) yalnızca yaklaşık 100 m'ye yuvarlanmış konumu göndererek o anki sıcaklığı sorar.

**Bölgeler (isteğe bağlı):** bir bölge oluşturup (haritada bir alan, ör. eviniz) bir otomasyonda kullanırsanız uygulama, Android'in izin isteğinden önce kendi penceresinde nedenini açıkladıktan sonra "her zaman" kesin konum izni (arka planda konum) ister. Android'in bölge izlemesi (Google Play hizmetleri) bölgelerinize girip çıktığınızda, uygulama kapalıyken de uygulamaya haber verir; uygulama yolunuzu izlemez ve kaydetmez. Haritadaki "Şu anki konumum" düğmesi de aynı şekilde sorar. Bölgeler ve konumunuz yalnızca cihazınızda kalır, bize hiç gönderilmez. Haritayı çizmek için uygulama baktığınız alanın harita karolarını OpenStreetMap'ten (`tile.openstreetmap.org`) indirir; aradığınız yer adları Open-Meteo'nun yer aramasına (`geocoding-api.open-meteo.com`) gönderilir. Bölgeleri silebilir ya da izni dilediğiniz zaman geri alabilirsiniz.

**Kritik alarmda telefonla arama (isteğe bağlı):** "Kritik alarmda ara" bölümünde bir kişiye telefon numarası girerseniz, kritik alarm kapatılmadığında uygulama bu telefonun kendi hattından o numarayı arar (telefon araması izni). Numara yalnızca cihazınızda saklanır. Arama, her normal arama gibi operatörünüz üzerinden yapılır; başka hiçbir şey gönderilmez.

**Sensör değerleri:** eWeLink sensörlerinizin değerleri (sıcaklık, nem, ışık, kapı, hareket, su kaçağı, pil) diğer cihaz durumları gibi kendi eWeLink hesabınızdan gelir. Uygulama grafik çizmek ve kurallarınızı değerlendirmek için geçmişlerini tutar; bunlar yalnızca cihazınızda saklanır ve hiçbir yere gönderilmez.

**Şarjı elektrik algılayıcısı olarak kullanma (isteğe bağlı):** uygulama bu telefonun ya da tabletin şarj olup olmadığını yalnızca cihazda okur.

**Anahtarları telefondan girme (TV):** TV'de "Telefondan gir", TV'nin ev ağınızda kendisinin sunduğu bir sayfayı açan QR kodu gösterir. Oraya yazdıklarınız telefonunuzun tarayıcısında QR koddaki tek seferlik anahtarla şifrelenir ve doğrudan TV'ye gider; ev ağınızdan çıkmaz.

**Sesli komut (isteğe bağlı):** mikrofona dokunduğunuzda konuşma, cihazınızın yerleşik konuşma tanıma servisiyle (ör. Android'de Google'ınki) tanınır; uygulamaya yalnızca tanınan metin gelir ve cihazda işlenir. Sesli yanıtlar cihazın metin okuma özelliğiyle verilir.

**Yedekleme:** Uygulama verileri Android bulut yedeğine ve cihaz aktarımına dahil edilmez.

**Verilerinizi silme:** Ayarlar'dan çıkış yapabilir, geçmişi temizleyebilir veya uygulamayı kaldırabilirsiniz; tüm veriler cihazdan silinir. Erişimi dilediğiniz zaman eWeLink geliştirici hesabınızdan ya da Tuya Cloud projenizi silerek / uygulama hesabınızın projeyle bağlantısını kaldırarak da iptal edebilirsiniz.

**Çocuklar:** Uygulama çocuklara yönelik değildir.

**İletişim:** switchguardapp@gmail.com
