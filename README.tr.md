[English](README.md) · **Türkçe** · [Deutsch](README.de.md) · [Español](README.es.md) · [Português (Brasil)](README.pt-BR.md) · [Français](README.fr.md) · [Italiano](README.it.md) · [Nederlands](README.nl.md) · [Polski](README.pl.md) · [Русский](README.ru.md) · [Українська](README.uk.md) · [العربية](README.ar.md) · [हिन्दी](README.hi.md) · [Bahasa Indonesia](README.id.md) · [Tiếng Việt](README.vi.md) · [ไทย](README.th.md) · [简体中文](README.zh-CN.md) · [繁體中文](README.zh-TW.md) · [日本語](README.ja.md) · [한국어](README.ko.md)

# SwitchGuard

SwitchGuard, size ait **eWeLink / SONOFF** ve **Tuya / Smart Life** hesaplarındaki akıllı priz ve anahtarları izler; bir şey değiştiği anda gerçek bir alarm çalar — duran bir pompa, kapanan bir dondurucu prizi, bağlantısı kopan bir cihaz. Android telefon, tablet, Android TV ve Wear OS saatlerde çalışır; Windows uygulaması da vardır.

<p>
  <img src="docs/screenshots/play/en/v215/01-devices.png" width="200" alt="Cihazlar">
  <img src="docs/screenshots/play/en/v215/02-energy.png" width="200" alt="Enerji">
  <img src="docs/screenshots/play/en/v215/05-automations.png" width="200" alt="Otomasyon">
  <img src="docs/screenshots/play/en/v215/08-alarm.png" width="200" alt="Cihaz alarmı">
</p>

> **Diller:** Windows uygulaması 0.13.0'dan itibaren 20 dilde. Android'de uygulama içi dil seçimi (20 dil) **2.26** ile geliyor; son Android sürümü 2.25.0.

## İndir

### Android (telefon, tablet, TV)
- **Google Play**: https://play.google.com/store/apps/details?id=com.macerce.switchguard
- **APK**: [son Android sürümü](https://github.com/Macerce/switchguard/releases/latest) — `SwitchGuard-x.y.z.apk` dosyasını indirip telefonda açın. SwitchGuard Google Play'den kuruluysa önce onu kaldırın (imza farklı). GitHub sürümü yenisi çıkınca size haber verir.

### Windows 10 / 11
- **Kurulum**: [Windows sürümleri](https://github.com/Macerce/switchguard/releases?q=Windows&expanded=true) — en yenisinin `SwitchGuard-x.y.z-Kurulum.exe` dosyasını indirip çalıştırın. Uygulama yeni sürümleri kendisi bulur ve tek tıkla günceller; ayarlar korunur.

### Ücretsiz ve Pro
- Ücretsiz: tüm özellikler, 1 izlenen cihaz için. Demo cihazlarla donanım olmadan her şeyi deneyebilirsiniz.
- Android: Pro, Google Play'den tek seferlik satın alınır (abonelik yok) ve tüm cihazları izler; Google hesabınızla yeni telefona taşınır.
- Windows: Pro lisans anahtarı (Ayarlar → plan kartı) tüm cihazları izler. Anahtar için: **switchguardapp@gmail.com**

## Gereksinimler

- Android 8.0+ (API 26); saat uygulaması için Wear OS
- eWeLink: ücretsiz bir eWeLink geliştirici uygulaması (App ID + App Secret) — uygulama içi rehber adım adım anlatır. eWeLink her hesap ve App ID için tek oturuma izin verir; bu yüzden her telefon, tablet, TV veya bilgisayarın cihazların paylaşıldığı kendi eWeLink hesabı olmalı ([dev.ewelink.cc](https://dev.ewelink.cc))
- Tuya / Smart Life: Smart Life / Tuya uygulama hesabınıza bağlı, size ait ücretsiz Tuya Cloud projesinin Access ID ve Access Secret bilgileri ([platform.tuya.com](https://platform.tuya.com))
- Windows uygulaması için Windows 10 / 11 (64 bit)

## Kurulum rehberleri

- eWeLink (SONOFF): [English](docs/guide/ewelink.en.md) · [Türkçe](docs/guide/ewelink.tr.md)
- Tuya / Smart Life: [English](docs/guide/tuya.en.md) · [Türkçe](docs/guide/tuya.tr.md)

### Videolar

- **New in 2.17 – alert categories, critical alerts, groups (EN):** https://youtube.com/shorts/iVM3zEiBt8A
- **2.17 yenilikleri – uyarı kategorileri, kritik uyarılar, gruplar (TR):** https://youtube.com/shorts/Qha83ziwylI
- New in 2.15 – smart alerts, scenes, energy reports (EN): https://youtube.com/shorts/bOtFBjHoefE
- 2.15 yenilikleri – akıllı uyarılar, sahneler, enerji raporu (TR): https://youtube.com/shorts/y3QRZvVoeK4
- New in 2.12 – a tour of the app (EN): https://youtube.com/shorts/jvm0hx0gATk
- 2.12 yenilikleri – uygulama turu (TR): https://youtube.com/shorts/2KeUgJrCCEU
- First setup in one minute / İlk kurulum (1 dk): https://youtube.com/shorts/jarBmMg2kYA
- **Getting eWeLink API keys / eWeLink API anahtarlarını alma (TR, sesli):** https://youtu.be/G5vQ_7CXTcM
- **Getting Tuya API keys / Tuya API anahtarlarını alma (TR, sesli):** https://youtu.be/8vPjJMoMaMU

## Özellikler

### İzleme ve alarm

- eWeLink WebSocket ve Tuya mesaj servisi üzerinden anlık güncellemeler; yedek olarak HTTP eşitleme ve düzenli kontrol.
- Alarm siz kapatana kadar çalar — sessiz modda bile — ve kilit ekranında tam ekran açılır; her cihazın kendi sesi olabilir.
- Cihaz başına kurallar: cihaz kapanınca, açılınca, çevrimdışı olunca ya da geri gelince alarm, bildirim veya hiçbiri.
- Koşullu uyarılar: bir cihaz bir süre açık ya da kapalı kalınca veya gücü bir sınırın üstüne çıkınca / altına düşünce çalar (ör. 50 W altında çeken pompa).
- Kısa kopmaları yok sayma, izlemenin kendisi durunca uyarı (internet / bulut yok), eWeLink oturumu düşünce yüksek sesli uyarı (ör. aynı hesap başka telefonda açıldı).
- Hazırlık kartı alarmı engelleyebilecek telefon ayarlarını gösterir ve düzeltir (pil kısıtlaması, tam ekran izni, Rahatsız Etmeyin, alarm sesi…).

### Cihazlar

- Akıllı ev tarzı kutucuklar: hızlı aç/kapa düğmeleri, çok kanallı cihazlarda kanal başına düğme, canlı durum ve güç.
- Cihaz sayfası: kurallar, kendi alarm sesi, uyarı kategorisi, enerji geçmişi ve maliyet, eWeLink cihaz zamanlayıcıları, Wi-Fi sinyali ve kopma teşhisi, bekleme tüketimi ve son olaylar.
- **Cihazları düzenle**: Yıldız = en üstte tut · oklar = taşı · kart boyutu = küçük ya da büyük · etiket = kategori. Buraya dönmek için bir karta uzun basın.

### Uyarılar

- Cihaz uyarıları ve koşullu uyarılar tek yerde; uyarı kategorisine göre filtre (Kritik / Önemli / Normal).
- Uyarı ayarları: alarm sesi ve ses düzeyi, sessiz saatler, uyarı kategorileri, Telegram ("İkinci kişiye haber ver") ve akıllı uyarılar — aşağıdaki *Tüm ayarlar* bölümüne bakın.
- Akıllı uyarılar: elektrik kesintisi için tek uyarı, olağandışı tüketim, aylık bütçe, haftalık rapor, tatil modu.

### Otomasyon

- **Kurallar** — otomatik işlemler: *Eğer* bir şey olursa *o zaman* bir şey yap; isteğe bağlı gecikmeyle.
- Tetikleyiciler: açılınca, kapanınca, şu süre açık kalınca:, şu süre kapalı kalınca:, çevrimdışı olunca, tekrar çevrimiçi olunca, güç şunu aşınca:, güç şunun altına inince:, gün doğunca, gün batınca, dış sıcaklık şunu aşınca:, dış sıcaklık şunun altına inince:, bölgesine girince, bölgesinden çıkınca.
- İşlemler: Aç, Kapat, Alarm çal, Bildirim gönder, Sahneyi çalıştır, Döngüyü başlat, Döngüyü durdur.
- **Yalnızca**: Yalnızca şu saatlerde, Ve başka bir cihaz şu durumdaysa…, Yalnızca evdeyken / dışarıdayken.
- **Sahneler** — Sahne çalışınca her cihaza ne olacağını seçin. "Dokunma"da bırakılan cihazlar değişmez.
- **Gruplar** — Gruptaki cihazları seçin. Grup, Cihazlar ekranında bir cihaz gibi görünür: düğmesi hepsini birlikte açar ya da kapatır; otomasyonlarda da seçilebilir.
- **Döngü ve zamanlayıcılar** — Geri sayım, Aç, sonra kapat, Aç/kapa döngüsü, Sırayla, Özel adımlar. Düğmeyle, seçilen günlerde belirli saatte ya da bir saat aralığında başlar; isteğe bağlı rastgele değişim (evde biri var görünümü) ve "elle değiştirilince dur".
- Otomasyonlar izleme açıkken bu telefonda çalışır. Telefon kapalıyken de çalışması gereken saatli işler için cihaz sayfasındaki zamanlayıcıları kullanın.

### Enerji

- Gün / ay / yıl görünümü: toplam, tahmini maliyet, ortalama ve en yüksek güç, en yoğun saat/gün/ay ve öne çıkanlar özeti.
- Tek zamanlı ya da üç zamanlı tarife (gündüz / puant / gece), cihaz başına fiyat veya varsayılan fiyat.
- Bekleme tüketimi (cihazın hiç altına inmediği güç) ve aylık maliyeti.
- Enerji raporu PDF (yazdırmak ya da paylaşmak için) veya CSV (Excel, Google E-Tablolar) olarak.
- Tüketim, izleme açıkken alınan güç ölçümlerinden SwitchGuard tarafından hesaplanır; cihazın kendi uygulamasındaki (eWeLink, Smart Life) değerden biraz farklı olabilir.

### Geçmiş

- Her olay tarih ve saatiyle, kimin ya da neyin değiştirdiği (uygulama, otomasyon); cihaza göre filtre.
- CSV'ye aktarma (son 24 saat, 7 gün, 30 gün ya da tümü) ve geçmişi temizleme.

### Sohbet

- Merhaba. Cihazlarınızı yazarak yönetebilirsiniz: açıp kapatma, durum, güç tüketimi ve dış sıcaklık. Yazmak yerine mikrofona dokunup söyleyebilirsiniz (“Mutfağı kapat”); yanıt sesli okunur. Hazır şablonlar aşağıdadır; şablona dokunduğunuzda gönderilir, düzenlemek için basılı tutun.
- Serbest metin komutları internetsiz çalışır ve Türkçe ile İngilizceyi anlar; mikrofon telefonun konuşma tanımasını kullanır, yanıtlar sesli okunabilir.
- Şablonlar: tek dokunuşla gönderilen mesajlar; her gün belirli saatte ya da bir şey olunca kendiliğinden de çalışabilir.

### Bekleme ekranı

- Şarjdaki telefon ya da tablet için tam ekran pano: saat, hava durumu, kritik durum, anlık güç, son hareketler ve yakında çalışacak işler. Kilit ekranının üstünde de görünür.
- Android ayarları açılır: Ekran koruyucu’da SwitchGuard’ı seçip “Şarj olurken” başlamasını ayarlayın. Böylece şarjdayken kilit ekranının yerine pano görünür.

### Widget'lar, hızlı ayar kutucuğu, kısayollar

- **SwitchGuard** — İzlemenin çalışıp çalışmadığını ve çalan alarmı gösterir
- **SwitchGuard düğmesi** — Bir cihazı ya da kanalı tek dokunuşla açıp kapatır. Evdeyim / Dışarıdayım ya da sahne çalıştırma için de ayarlanabilir.
- **SwitchGuard grubu** — Büyük kutucuk: gruptaki her cihazı ayrı ayrı açıp kapatın
- **SwitchGuard sesli komut** — Tek kutucuk: dokunun, söyleyin (“Mutfağı kapat”), komut uygulansın
- **Hızlı ayar kutucuğu** — izleme durumunu gösterir, izlemeyi başlatır ya da durdurur (önce telefon kilidi açılmalı); alarm çalarken uygulamayı açar.
- **Uygulama kısayolları** — uygulama simgesine uzun basınca Evdeyim / Dışarıdayım ve sahneleriniz.

### Evde / Dışarıda

- **Evdeyim / Dışarıdayım** — "Yalnızca dışarıdayken" kuralları çalışmaz. Çıkarken Dışarıdayım'a geçin (widget, saat ya da Telegram /disarida ile de olur).
- **Tatil modu** — Dışarıda sayılıyorsunuz, sessiz saatler kapalı ve her sabah 09:00'da rapor geliyor. Kendiliğinden biter.
- **Bölgeler** — Haritada bir alan seçin; telefonunuz bu alana girince ya da çıkınca otomasyon çalışsın (ör. eve yaklaşınca bahçe ışığını aç, evden çıkınca tüm cihazları kapat). Harita görüntüleri OpenStreetMap'ten indirilir; konumunuz yalnızca bu telefonda kullanılır.

### Wear OS

- SwitchGuard saat uygulaması alarmları (bilekten kapatma), cihazları (aç/kapa), sahneleri ve Evdeyim / Dışarıdayım modunu gösterir; telefondaki uygulamaya bağlanır.

### Android TV

- Android TV'de kumandayla gezinerek çalışır; alarm oynayan her şeyin üstünde görünür ("Alarmı diğer uygulamaların üstünde göster").
- "Telefondan gir": TV bir QR kod gösterir; anahtarları telefonunuzda yazarsınız, ev Wi-Fi ağınız üzerinden şifreli olarak TV'ye gider.
- "Başka cihazdan al": Tuya anahtarlarını, kuralları ve ayarları Wi-Fi ile telefonunuzdan kopyalar.

### Birden fazla cihaz

Telefon, tablet, Android TV ve bilgisayar: **her cihazın geliştirici özelliği onaylanmış kendi eWeLink hesabı ve o hesabın kendi API anahtarları (App ID / Secret) olmalı** — eWeLink her hesapta tek oturum açık cihaza izin verir. Cihazlarınızı eWeLink uygulamasından ek hesaba paylaşın. Tuya anahtarları birden çok cihazda kullanılabilir. Pro bir kez alınır ve aynı Google hesabındaki tüm Android cihazlarda çalışır. Ayrıntılar: [English](docs/guide/ewelink.en.md#using-several-devices-phone-tablet-android-tv) · [Türkçe](docs/guide/ewelink.tr.md#birden-fazla-cihazda-kullanım-telefon-tablet-android-tv)

### Demo ve Pro

- **Demo cihazlar** — Tüm özellikleri denemek için sanal cihazlar. Bunlar cihazın kendisinde bir şey olmuş gibi davranır (biri cihazı açtı/kapattı, elektrik ya da Wi-Fi gitti); kurallarınız uygulanır ve alarm çalabilir. Yukarıdaki düğmelerle açıp kapatmak sizin işleminiz sayılır, alarm çaldırmaz.
- Ücretsiz sürüm 1 cihazı izler. Pro tüm cihazlarınızı, bütün özellikleriyle izler. Tek seferlik ödeme, abonelik yok. Google hesabınıza bağlıdır; telefon değiştirseniz de geçerli.

## Tüm ayarlar

Aşağıdakilerin hepsi uygulamadaki sırayla. **Ayarlar** sekmesinde bağlantılar ve uygulama seçenekleri; alarm, sessiz saatler, Telegram ve akıllı uyarılar **Uyarılar** sekmesi → Uyarı ayarları altında; cihaza özel seçenekler her cihazın sayfasında.

### Ayarlar → Hesaplar

| Ayar | Ne işe yarar |
|---|---|
| Pro / Pro'ya geç | Ücretsiz sürüm 1 cihazı izler. Pro tüm cihazlarınızı, bütün özellikleriyle izler. Tek seferlik ödeme, abonelik yok. Google hesabınıza bağlıdır; telefon değiştirseniz de geçerli. |
| Geri yükle | Google Play satın alımlarınızı yeniden kontrol eder (yeni telefon, yeniden kurulum). |
| eWeLink — eWeLink ile giriş yap | eWeLink'in kendi giriş sayfasını açar; uygulama şifrenizi hiç görmez. |
| API bilgileri | eWeLink geliştirici uygulamanızın App ID, App Secret ve bölgesi. |
| Çıkış yap | İzleme duracak. Ayarlarınız ve geçmişiniz korunur. |
| Tuya / Smart Life | Tuya Cloud projenizin Access ID ve Access Secret bilgilerini girin (platform.tuya.com → Cloud → projeniz → Overview). Smart Life / Tuya uygulama hesabınız projeye bağlı olmalı. Veri merkezi: Orta Avrupa, Batı Amerika, Çin, Hindistan |
| Bağlantıyı kaldır | Tuya bağlantısını kaldırır; Tuya cihazları artık izlenmez. |
| Demo cihazlar | Tüm özellikleri denemek için sanal cihazlar |

### Ayarlar → İzleme

SwitchGuard'ın cihazlarınızı arka planda nasıl izlediği.

| Ayar | Ne işe yarar |
|---|---|
| En yüksek güvenilirlik | İşlemciyi uyanık tutar, hiçbir olay gecikmez. Daha fazla pil harcar. |
| Kısa kopmaları yok say | Bir cihazın bağlantısı koptuğunda uyarmadan önce bu kadar bekle. Süre içinde geri gelirse uyarı verilmez. |
| İzleme durursa uyar | Bu kadar süre internet ya da bulut bağlantısı olmazsa uyarı verir |
| Yedek kontrol aralığı | Yalnızca anlık bildirim kullanılamadığında kullanılır. |

### Ayarlar → İzinler

Bunlar olmadan Android alarmın size ulaşmasını engelleyebilir.

| Ayar | Ne işe yarar |
|---|---|
| Bildirimler | Alarmlar ve uyarılar gösterilemez. |
| Arka plan çalışması | Telefon izlemeyi uyutabilir; alarmlar gecikebilir ya da kaçabilir. |
| Tam ekran alarm | Telefon kilitliyken alarm büyük ekranıyla açılamaz. |
| Kilit ekranında alarm (Xiaomi) | Telefon kilitliyken alarm ekranı açılamaz. Düzelt'e dokunun, Diğer izinler'i açın ve “Kilit ekranında görüntüle” ile “Arka planda çalışırken yeni pencereler açın” izinlerini açın. |
| Alarmı diğer uygulamaların üstünde göster | Android TV'de alarmın o an açık olan her şeyin üstüne çıkması için gerekli. Dokunun ve SwitchGuard'a izin verin. |
| Arka planda başlatma (Xiaomi, Huawei, OPPO, vivo…) | Arka planda başlamayı engelleyen telefonlarda görünür (Xiaomi, Huawei, OPPO, vivo…): yeniden başlatma ya da güncellemeden sonra izlemenin tekrar başlamasını sağlar. Sorun giderme bölümüne bakın. |

### Ayarlar → Konum ve bölgeler

| Ayar | Ne işe yarar |
|---|---|
| Konum | Gün doğumu/batımı ve dış sıcaklık kuralları için gerekir. Yalnızca bu cihazda tutulur; sıcaklık için Open-Meteo'ya ≈100 m'ye yuvarlanarak gönderilir. |
| Bölgeler | Haritada bir alan seçin; telefonunuz bu alana girince ya da çıkınca otomasyon çalışsın (ör. eve yaklaşınca bahçe ışığını aç, evden çıkınca tüm cihazları kapat). Harita görüntüleri OpenStreetMap'ten indirilir; konumunuz yalnızca bu telefonda kullanılır. |

### Ayarlar → Bekleme ekranı

| Ayar | Ne işe yarar |
|---|---|
| Bekleme ekranını aç | Ekrana dokununca “Uygulamayı aç” ve “Kapat” çıkar |
| Ekran koruyucu olarak kullan | Android ayarları açılır: Ekran koruyucu’da SwitchGuard’ı seçip “Şarj olurken” başlamasını ayarlayın. Böylece şarjdayken kilit ekranının yerine pano görünür. |
| Geceleri karart | 23:00–07:00 arası yazılar soluklaşır |

### Ayarlar → Günlük özet

| Ayar | Ne işe yarar |
|---|---|
| Günlük özet bildirimi | Bugün her cihaz kaç kez açıldı, ne kadar açık kaldı ve ne kadar enerji harcadı |
| Gönderim saati | Özetin gönderileceği saat. |
| Bugünün özetini şimdi göster | Hemen gösterir. |

### Ayarlar → Yedekleme ve aktarım

| Ayar | Ne işe yarar |
|---|---|
| Şifreli yedek al | Kurallar, otomasyonlar, ayarlar ile Tuya ve eWeLink anahtarları şifreli bir dosyaya kaydedilir |
| Yedekten geri yükle | Yeni telefona mı geçtiniz? Yedek dosyasını seçin ve şifresini girin |
| Başka cihaza aktar | Tuya bilgilerini, kuralları ve ayarları aynı Wi-Fi'daki TV ya da tablete kopyalayın |
| Başka cihazdan al | Kurulumu telefonunuzdan Wi-Fi üzerinden alın |

Wi-Fi ile aktarma eWeLink API anahtarlarını ve eWeLink oturumunu asla göndermez: diğer cihazda o cihazın kendi API anahtarlarını girip kendi eWeLink hesabıyla giriş yapın. Dosyaya yedek ise eWeLink ve Tuya anahtarlarını şifrenizle korunmuş olarak içerir.

### Ayarlar → Dil

| Ayar | Ne işe yarar |
|---|---|
| Dil | Uygulamanın, bildirimlerin ve alarmların dilini seçin. Telefonun dili ya da 20 dilden biri (2.26'dan itibaren). |

### Ayarlar → Yardım ve hakkında

| Ayar | Ne işe yarar |
|---|---|
| Kurulum rehberi | İlk kurulum adımlarını tekrar göster |
| eWeLink kurulum rehberi | Resimli, adım adım anlatım (GitHub) |
| Tuya / Smart Life kurulum rehberi | Resimli, adım adım anlatım (GitHub) |
| Hata raporu gönder | Bir sorun mu var? Tanılama bilgileriyle bir e-posta hazırlar. Siz göndermeden hiçbir şey paylaşılmaz. |
| Kayıtlı hataları sil | Bu cihazda kaydedilmiş çökme raporlarını siler. |
| İletişim ve geri bildirim | Geliştiriciye e-posta yazar (soru, hata, öneri). |
| Gizlilik politikası | Gizlilik politikasını açar. |
| Hakkında | Sürüm, yasal not. GitHub sürümü GitHub'da yeni sürüm olup olmadığını kontrol eder. |

### Uyarılar

| Ayar | Ne işe yarar |
|---|---|
| Cihaz uyarıları | Her cihazın hangi değişiklikte alarm ya da bildirim verdiği |
| Koşullu uyarılar | Cihaz bir süre açık ya da kapalı kalınca veya gücü değişince uyar |
| Alarm hazır / Alarm size ulaşmayabilir | Üstteki hazırlık kartı: alarmı engelleyebilecek telefon ayarlarını listeler ve düzeltmek için doğru ekranı açar; Test alarmı düğmesi de buradadır. |

### Uyarılar → Alarm

| Ayar | Ne işe yarar |
|---|---|
| Alarm sesi | Genel alarm sesi. Her cihazın kendi sesi olabilir (cihaz sayfası). |
| Alarm ses seviyesi | SwitchGuard'ın kendi alarm ses düzeyi. |
| Titreşim | Alarm çalarken titreşim. |
| Alarm tam seste çalsın | Alarm çalarken telefonun alarm sesini en yükseğe çıkarır ve müziği duraklatır; alarmı kapatınca eski haline döner |
| Düzelmezse yeniden çal | Alarmı kapattıktan sonra cihaz alarmı çaldıran durumda kaldıkça (hâlâ kapalı, hâlâ açık ya da kopuk) bu aralıkla yeniden çalar. Cihaz normale dönünce durur. |
| İkinci kişiye haber ver | Telegram kurulumu (aşağıya bakın). |
| Test alarmı | Alarmı şimdi dener: sesli ya da sessiz, hemen ya da 10 saniye sonra |

### Uyarılar → Uyarı kategorileri

Her cihazın bir uyarı kategorisi vardır. Siz seçmezseniz kendiliğinden belirlenir: bir olayında Alarm seçili olan izlenen cihaz Kritik, yalnız Bildirim seçili olan Önemli, diğerleri Normal. Koşullu uyarılarda ve otomasyonlarda tetikleyici olarak bir kategori seçebilirsiniz (ör. “Kritik cihazlardan biri çevrimdışı olunca alarm çal”).

| Ayar | Ne işe yarar |
|---|---|
| Kritik · Önemli · Normal · Otomatik | Otomatik seçimde alarm seçili olay varsa Kritik, yalnız bildirim varsa Önemli olur |
| Sessiz saatlerde de alarm çal | Sessiz saatlerde de alarm çalar / Sessiz saatlerde alarm bildirime düşer |

### Uyarılar → Sessiz saatler

| Ayar | Ne işe yarar |
|---|---|
| Sessiz saatler | Bu saatlerde alarmlar bildirime dönüşür (kritik cihazlar yine çalar) |
| Başlangıç / Bitiş | Sessiz saatlerin başlangıç ve bitiş saati. |

Sessiz saatlerde de çalmasını seçtiğiniz uyarı kategorilerindeki cihazlar (varsayılan: Kritik) sessiz saatlerde de çalar.

### Uyarılar → İkinci kişiye haber ver

Alarm belirli bir süre içinde kapatılmazsa seçtiğiniz kişilere Telegram mesajı gider; alarm kapatılınca da haber verilir. Gece telefon başka odada kalsa bile evdeki biri ya da komşunuz haberdar olur. Mesajları sizin oluşturduğunuz Telegram botu gönderir, arada başka bir sunucu yoktur.

| Ayar | Ne işe yarar |
|---|---|
| 1. Telegram botu | Telegram'da @BotFather'ı açın, /newbot yazın ve botunuza bir ad verin. BotFather'ın verdiği anahtarı (token) aşağıya yapıştırın. |
| 2. Haber verilecek kişiler | Haber verilecek kişi Telegram'da @your_bot bağlantısını açıp Başlat'a dokunsun (botu bir aile grubuna da ekleyebilirsiniz). Sonra Kişileri bul'a dokunun. |
| 3. Ne zaman | Kişilere haber verilmeden önce alarmın ne kadar çalabileceği (ya da hemen). |
| Deneme mesajı gönder | Listedeki herkese deneme mesajı gönderir. |
| Kapat | Telegram bildirimini kapatır. |

### Uyarılar → Akıllı uyarılar

Tüm cihazlarınıza birlikte bakan uyarılar: elektrik kesintisi, anormal tüketim ve haftalık özet. Telegram ile ayrıca durumu sorabilir, alarmı üstlenebilir ve cihazları uzaktan açıp kapatabilirsiniz.

| Ayar | Ne işe yarar |
|---|---|
| Elektrik kesintisi: Elektrik kesintisini algıla | İzlenen cihazların hepsi birlikte koparsa her biri için ayrı alarm yerine tek "elektrik kesildi" uyarısı gelir; elektrik gelince kesintinin süresi bildirilir. |
| Şarjı elektrik algılayıcısı olarak kullan | Bu telefon/tablet yerinde prize takılı duruyorsa, şarjın bir dakika kesik kalması elektrik kesintisi sayılır; internet gitse de çalışır. Haber Telegram'a mobil veriyle gider. |
| Evde / Dışarıda: Evdeyken alarmlar yumuşasın | Evdeyken kritik olmayan cihazların alarmı bildirime düşer (sessiz saatler gibi). Kritik cihazlar her zaman çalar. |
| Tatil modu | Dışarıda sayılıyorsunuz, sessiz saatler kapalı ve her sabah 09:00'da rapor geliyor. Kendiliğinden biter. |
| Tüketim: Anormal tüketim | Her sabah dünü önceki iki haftayla karşılaştırır. Çok fazla tüketim arıza, çok az tüketim (neredeyse hiç çalışmayan dondurucu) cihazın durduğu anlamına gelebilir. |
| Elektrik tarifesi | Kendi fiyatı olmayan cihazlar için varsayılan fiyat. Üç zamanlı tarifede her saat kendi diliminin fiyatıyla hesaplanır: gündüz 06–17, puant 17–22, gece 22–06. |
| Aylık bütçe | Enerji ölçen cihazların bu ayki tahmini maliyeti bu tutara ulaşınca ayda bir kez bildirilir. |
| Haftalık rapor | Her pazartesi 09:00'da geçen haftanın kesintileri, alarmları ve tepki süresi, kopmaları ve tüketimi. Telegram kuruluysa oraya da gider. |
| Bu haftanın raporunu göster | Hemen gösterir. |
| Telegram: Komutlar ve "Gördüm" düğmesi | Listenizdeki kişiler /durum yazabilir ve çalan alarmı üstlenebilir. Alarm mesajına "Gördüm" düğmesi eklenir: dokununca telefondaki alarm susar. |
| Aç/kapa izni | /ac ve /kapat komutları. Yalnızca listenizdeki kişiler kullanabilir. |
| Kademeli haber | Her kişiye kendi bekleme süresi dolunca yazılır. Örneğin komşuya 5, akrabaya 15, aile grubuna 30 dakika sonra. Biri "Gördüm" derse sıradakilere yazılmaz. |
| Kritik alarmda ara | Kritik kategorideki bir cihaz alarm çaldırınca kişi, kademeli haberdeki bekleme süresi dolduğunda bu telefonun hattından ayrıca aranır. Arama yalnız uyandırmak içindir: ne olduğu aynı anda giden Telegram mesajında yazar (Android, uygulamaların telefon görüşmesine ses vermesine izin vermez). Kişiden bu numarayı “SwitchGuard alarm” adıyla rehbere kaydetmesini isteyin. Alarm kapatılana ya da Telegram'dan "Gördüm" denene dek 2 dakika arayla en çok 3 kez aranır. Gruplar aranamaz. |
| Akıllı uyarıları Telegram'a da gönder | Elektrik kesintisi, anormal tüketim, bütçe uyarısı ve haftalık rapor Telegram'a da yazılır. |
| Bekçi | Aynı botu kullanan iki cihaz birbirini izler (ör. telefon ve TV kutusu). Sinyal veren cihaz Telegram sohbetindeki sabitlenmiş mesajı günceller; denetleyen cihaz mesaj güncellenmezse alarm çalar ve kişilerinize yazar. Sabit mesajı kaldırmayın. |

### Cihaz sayfası (bir cihaza dokunun)

| Ayar | Ne işe yarar |
|---|---|
| Bu cihazı izle | Değişiklikler kaydedilir ve aşağıdaki kurallara göre bildirilir |
| Şu olduğunda | Kapanınca, Açılınca, Çevrimdışı olunca, Çevrimiçi olunca → Alarm / Bildirim / Yok. Alarm, siz kapatana kadar çalar. Bildirim, normal bir bildirim gösterir. Yok ise olayı yalnızca geçmişe kaydeder. |
| Bu cihazın sesi | Kendi alarm sesi; yoksa genel ses. |
| Uyarı kategorisi | Otomatik seçimde alarm seçili olay varsa Kritik, yalnız bildirim varsa Önemli olur |
| Enerji | Güç, gerilim, akım; bugün ve bu ay; maliyetli gün / ay / yıl geçmişi; Rapor al (PDF / CSV). |
| kWh fiyatı | Bu cihazın kWh fiyatı ya da varsayılan fiyat; varsayılan olarak da ayarlanabilir. |
| Zamanlayıcılar (cihazda) | Zamanlayıcı yok. Zamanlayıcılar cihazın kendisinde çalışır, telefon kapalıyken bile. (eWeLink cihazları: seçilen günlerde belirli saatte aç/kapa zamanlayıcısı ekleyin ya da düzenleyin.) |
| Bağlantı | dBm sinyal gücüdür: 0'a yakın olan daha iyi. -55 üstü çok iyi, -67'ye kadar iyi, -75'e kadar orta, altı zayıf. |
| Bekleme tüketimi | Son 7 günün en düşük saatlik ortalaması: bu cihazın (ve ona takılı olanların) "hiçbir şey yapmıyorken" bile altına inmediği güç. TV, modem ve şarj aletleri beklemede genellikle bu kadar çeker. |
| Cihaz verisini kopyala | Cihazın ham verisini panoya kopyalar; desteklenmeyen bir modeli bildirmek için |

## Telegram komutları

Listenizdeki kişiler /durum yazabilir ve çalan alarmı üstlenebilir. Alarm mesajına "Gördüm" düğmesi eklenir: dokununca telefondaki alarm susar.

```
SwitchGuard komutları:
/durum – izleme, alarm ve cihazlar
/gordum – çalan alarmı üstlen
/ac ad [kanal] – cihazı ya da grubu aç (birden çok: /ac mutfak, bahçe)
/kapat ad [kanal] – cihazı ya da grubu kapat
/watt [ad] – şimdiki, bugünkü ve olağan güç
/sicaklik – dış sıcaklık
/sahne ad – sahneyi çalıştır
/evde · /disarida – evde / dışarıda

Serbest de yazabilirsiniz: “mutfak ve bahçeyi kapat”, “klima kaç watt”, “pompa açık mı”.

Aç/kapa ve sahneler uygulamada açılmalıdır (Uyarılar → Akıllı uyarılar).
```

Türkçe ve İngilizce komut adlarının ikisi de çalışır (ör. /durum = /status, /ac = /on). Komutlar yalnızca seçtiğiniz sohbetlerden kabul edilir.

## İzinler ve nedenleri

| İzin | Neden |
|---|---|
| `POST_NOTIFICATIONS` | Alarmları, bildirimleri ve izleme durumunu göstermek. |
| `FOREGROUND_SERVICE` | İzlemeyi arka planda çalışır tutmak (kalıcı durum bildirimi). |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Alarmlar gecikmesin diye pil optimizasyonundan muaf tutulmayı istemek. |
| `USE_FULL_SCREEN_INTENT` | Alarmı kilit ekranının üstünde tam ekran açmak. |
| `SYSTEM_ALERT_WINDOW` | Alarmı diğer uygulamaların üstünde göstermek (Android TV, Xiaomi). |
| `WAKE_LOCK` | Olayları işlerken işlemciyi uyanık tutmak ("En yüksek güvenilirlik"). |
| `VIBRATE` | Alarm sırasında titreşim. |
| `RECEIVE_BOOT_COMPLETED` | Telefon yeniden başlayınca izlemeyi yeniden başlatmak. |
| `INTERNET` | eWeLink / Tuya bulutuyla (kullanırsanız Telegram / Open-Meteo / OpenStreetMap ile) konuşmak. |
| `ACCESS_COARSE_LOCATION` | Yalnızca gün doğumu/batımı ve sıcaklık kuralları için "Bu cihazın konumunu kullan"a dokunduğunuzda. |
| `ACCESS_FINE_LOCATION` | Bölgeler: varış / ayrılış kuralları için kesin alan. |
| `ACCESS_BACKGROUND_LOCATION` | Bölgeler: uygulama kapalıyken varış ve ayrılışları fark etmek (yalnızca bölge oluşturunca istenir). |
| `CALL_PHONE` | "Kritik alarmda ara": girdiğiniz numarayı bu telefonun hattından aramak. |

## Gizlilik

- Kendi sunucusu yok; analiz, reklam ve izleme yok. Hiçbir veri satılmaz veya paylaşılmaz.
- Anahtarlar, token'lar, cihazlar, kurallar, ayarlar ve geçmiş (en fazla 2.000 olay) yalnızca cihazınızda saklanır.
- Uygulama yalnızca bağladığınız bulutlarla (eWeLink `*.coolkit.cc` / `*.coolkit.cn`, Tuya `openapi.tuya*.com`) ve yalnızca kullanırsanız Telegram, Open-Meteo (sıcaklık ve hava durumu; konum yaklaşık 100 m'ye yuvarlanır) ve OpenStreetMap harita karolarıyla konuşur.
- eWeLink şifreniz eWeLink'in kendi sayfasına yazılır; uygulama onu hiç görmez.
- Uygulama verileri Android bulut yedeğine dahil edilmez; dosya yedekleri şifreyle korunur; Wi-Fi aktarımı tek seferlik kodla şifrelenir ve ev ağınızdan çıkmaz.
- Pro satın alımlarını Google Play yönetir.

Gizlilik politikasının tamamı: [Google Sites](https://sites.google.com/view/switchguard-privacy) · [docs/privacy-policy.md](docs/privacy-policy.md)

## Sorun giderme ve SSS

### Alarm geç çalıyor ya da hiç çalmıyor

| Sorun | Ne yapmalı |
|---|---|
| Arka plan kısıtlı | Telefon izlemeyi uyutabilir; alarmlar gecikebilir ya da kaçabilir. |
| Tam ekran alarm izni yok | Telefon kilitliyken alarm büyük ekranıyla açılamaz. |
| Xiaomi alarm ekranını engelliyor | Telefon kilitliyken alarm ekranı açılamaz. Düzelt'e dokunun, Diğer izinler'i açın ve “Kilit ekranında görüntüle” ile “Arka planda çalışırken yeni pencereler açın” izinlerini açın. |
| Alarm bildirimleri kısılmış | “Alarmlar” bildirim kategorisi yüksek önemde değil; alarm üstte ya da tam ekran açılmaz. |
| Rahatsız Etmeyin: tamamen sessiz | Bu modda alarm sesleri de susar. |
| Alarm sesi çok kısık | Telefonun alarm ses seviyesi sıfır ya da sıfıra yakın; SwitchGuard'ı duymayabilirsiniz. Düzelt, “Alarm tam seste çalsın” ayarını açar. |
| Bildirimler kapalı | Alarmlar ve uyarılar gösterilemez. |
| Telefonda ikinci bir SwitchGuard var | Telefonun Çift uygulama (ikiz uygulama) özelliğiyle SwitchGuard'ın bir kopyası daha açılmış. Yan yana çalışan iki kopya her alarmı iki kez çalar, aynı eWeLink hesabını kullanınca birbirinin oturumunu kapatır; Xiaomi telefonlarda bu kopyanın izinleri de kapalı görünebilir. İkinci kopyayı kaldırın: simgesine (küçük işaretli olana) basılı tutun ve Kaldır'a dokunun. |

### Arka plan uygulamalarını durduran telefon markaları

| Telefon | Ne yapmalı |
|---|---|
| Xiaomi | Telefon yeniden başlayınca ya da güncellemeden sonra izlemenin kendiliğinden açılması için gerekli. Dokunun ve SwitchGuard için Otomatik başlatmayı açın. |
| Samsung | Samsung uygulamayı uyutabilir. Pil bölümünde "Kısıtlanmamış"ı seçin ve SwitchGuard'ı "Hiç uyutulmayan uygulamalar" listesine ekleyin (Pil → Arka plan kullanım sınırları). |
| Huawei / Honor | Uygulama başlatma bölümünde SwitchGuard'ı "Manuel olarak yönet" yapın; Otomatik başlatma, İkincil başlatma ve Arka planda çalıştırma anahtarlarını açın. |
| OPPO / realme / OnePlus | SwitchGuard için Otomatik başlatmayı açın, ardından uygulamanın Pil ayarlarında arka plan etkinliğine izin verin. |
| vivo / iQOO | SwitchGuard'ın arka planda başlamasına ve yüksek arka plan güç tüketimine izin verin. |
| Tecno / Infinix | Telefon yeniden başlayınca ya da temizlikten sonra izlemenin sürmesi için Phone Master'da SwitchGuard için Otomatik başlatmayı açın. |
| ASUS | Telefon yeniden başlayınca izlemenin açılması için Otomatik başlatma yöneticisinde SwitchGuard'a izin verin. |

### Diğer sorular

**İzleme durdu ve oturumun kapandığını söylüyor.**  
İpucu: eWeLink her hesaba tek oturum izin verir. Her telefona ayrı eWeLink hesabı açıp cihazları eWeLink uygulamasından paylaşın.

**Benimle paylaşılan bir Tuya cihazı görünmüyor.**  
Size paylaşılan bir cihaz görünmüyor mu? Tuya yalnızca evlerinizdeki cihazları listeler. Cihaz sahibinden sizi Smart Life'ta ev üyesi olarak eklemesini isteyin (Ben → Ev Yönetimi → Üye Ekle).

**Tuya cihazları güncellenmeyi bıraktı.**  
Tuya Cloud deneme süresi doldu; Tuya cihazları izlenmiyor. platform.tuya.com → Cloud → IoT Core → Extend trial period ile uzatın.

**Değişiklikler geç geliyor / "Anlık güncellemeler kullanılamıyor".**  
Bu App ID ile anlık bildirim kullanılamıyor; periyodik kontrol yapılıyor (aralıktan kısa değişiklikler kaçabilir)

**Google Play ve GitHub sürümleri arasında geçiş yapabilir miyim?**  
Evet, ama önce diğerini kaldırın: imzaları farklı. Kaldırmadan önce dosyaya yedek alın, sonra geri yükleyin.

**Kimseyi uyandırmadan alarmı nasıl denerim?**  
Alarmı şimdi dener: sesli ya da sessiz, hemen ya da 10 saniye sonra

**Tüketim neden eWeLink / Smart Life uygulamasından farklı?**  
Tüketim, izleme açıkken alınan güç ölçümlerinden SwitchGuard tarafından hesaplanır; cihazın kendi uygulamasındaki (eWeLink, Smart Life) değerden biraz farklı olabilir.

## Windows uygulaması

SwitchGuard for Windows (10 / 11) aynı izlemeyi bilgisayarda yapar: tepside durur, bilgisayarda tam ekran alarm çalar; aynı kuralları, otomasyonları ve Telegram özelliklerini paylaşır. Bölgeler, widget'lar, saat ve telefonla arama yalnızca telefonda.

### Özellikler

- Aynı sekmeler: Cihazlar, Uyarılar, Otomasyon, Geçmiş, Sohbet, Ayarlar; Sohbet'teki sesli komut Windows sesle yazmayı kullanır.
- Tepsi simgesi: aç, izlemeyi başlat / durdur, alarmı kapat, çıkış; pencereyi kapatmak izlemeyi durdurmaz.
- Tam ekran alarm penceresi, hazır sesler (Siren, Bip, Nabız, Çan) ya da kendi WAV / MP3 dosyanız.
- Bekleme ekranı ve Windows ekran koruyucusu: bilgisayar boştayken pano açılır.
- Şarja takılı bırakılan dizüstü bilgisayar elektrik kesintisi algılayıcısı olur.
- GitHub'dan tek tıkla güncelleme; ayarlar ve lisans korunur. Şifreli dosyaya yedekleme (telefona geri yüklenemez).

### Yalnızca Windows'taki ayarlar

| Ayar | Ne işe yarar |
|---|---|
| Windows | SwitchGuard'ın bu bilgisayarda kesintisiz izlemesi için. Biri kapalıysa yeniden başlatmadan sonra ya da bilgisayar uyurken alarmlar kaçabilir. |
| Windows ile başlat | Windows'ta oturum açınca tepside açılır |
| Bilgisayarı uyanık tut | İzleme açıkken bilgisayar uykuya geçmez |
| Windows ekran koruyucusu olarak kullan | Bilgisayar boşta kalınca bekleme ekranı açılır; fareyi oynatmak ya da bir tuşa basmak kapatır |
| Bekleme süresi | Bekleme ekranı açılmadan önce bilgisayarın ne kadar boşta kalacağı |
| Windows ekran koruyucu ayarları | Ör. “Devam edildiğinde oturum açma ekranını göster” |
| Bekleme ekranını aç | Tam ekran açılır; Esc kapatır, tıklayınca “Uygulamayı aç” çıkar. Açıkken ekran kapanmaz. |
| Dil ve görünüm: Tema | Açık, koyu ya da Windows'takiyle aynı. |
| Dil | Uygulamanın, bildirimlerin ve alarmların dilini seçin. |
| Tuya canlı bildirimleri bu bilgisayarda | Kapalı: Tuya cihazları 5 dakikada bir sorgulanır |
| Bilgisayar pile geçince elektrik kesildi say | Evde prize takılı bırakılan dizüstü için: bir dakika pille çalışırsa kesinti sayılır, internet olmasa da (Telegram mesajı bağlantı gelince gider). |
| Bu bilgisayar cihazlarla aynı elektrikte | Bilgisayar prizden çalıştıkça elektrik var demektir: kopan cihaz 90 saniyeye kadar beklemeden hemen bildirilir. |
| Dosyadan yükle… | Alarm ya da bildirim için kendi WAV veya MP3 dosyanızı kullanın (ilk 60 saniye, en fazla 20 MB). |
| Dosyaya yedekle… | Tüm ayarlar, hesap anahtarları ve sesler tek bir şifreli dosyada |
| Pro'yu etkinleştir | Lisans olmadan uygulama 1 cihazı izler. Tüm cihazlarınızı izlemek için lisans anahtarınızı girin. |

### Windows'a kurulum

1. Kurulum dosyasına (SwitchGuard-…-Kurulum.exe) çift tıklayın. Yönetici izni istemez, birkaç saniyede kurulur.
2. Windows "Bilgisayarınız Windows tarafından korundu" (SmartScreen) derse Ek bilgi → Yine de çalıştır'a basın. Uygulama henüz imzalı olmadığı için bu uyarı çıkar.
3. SwitchGuard kullanıcı klasörünüze (AppData › Local › SwitchGuard) kurulur, masaüstüne ve Başlat menüsüne kısayol ekler.
4. Windows'ta oturum açınca kendiliğinden tepside (saatin yanında) başlar. Pencereyi kapatmak uygulamayı kapatmaz; tamamen çıkmak için tepsi simgesi → Çıkış.
5. Hesaplarınızı Ayarlar → Hesaplar bölümünden ekleyin (Tuya ve eWeLink anlatımları aşağıda).
6. Başka bir bilgisayara geçerken: eskisinde Ayarlar → Yedekleme ve aktarım → Dosyaya yedekle; yenisine kurup Dosyadan geri yükle; ardından eWeLink'e yeniden giriş yapın.
7. Kaldırmak için: Windows Ayarlar → Uygulamalar → Yüklü uygulamalar → SwitchGuard → Kaldır.
8. Yeni sürüm kurarken önce eski sürümü kaldırın (7. adım), sonra yeni kurulum dosyasını çalıştırın. Ayarlarınız, hesaplarınız ve sesleriniz silinmez. (Uygulamadaki Güncelle düğmesi bunu kendisi yapar.)

## Derleme

```
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest      # unit tests
```
Yayın derlemeleri için proje kökünde `keystore.properties` + keystore gerekir (depoda yok).

## Sürüm geçmişi

[English](CHANGELOG.md) · [Türkçe](CHANGELOG.tr.md) · [Releases](https://github.com/Macerce/switchguard/releases)

## İletişim

Sorular, hata bildirimleri ve geri bildirim (ya da burada issue açın): **switchguardapp@gmail.com**

## Lisans

SwitchGuard açık kaynak değil, **kaynağı açık (source-available)** bir projedir. Kaynak kod: [PolyForm Noncommercial 1.0.0](LICENSE). Kişisel ve ticari olmayan kullanım serbesttir; kodu satmak veya ticari amaçla kullanmak yasaktır. 2.8.3 ve önceki sürümler GPL-3.0 ile yayımlanmıştı.

Resmi sürümleri (Google Play, [Releases](https://github.com/Macerce/switchguard/releases)) işletmeler dahil herkes kullanabilir. Değiştirilmiş sürümler SwitchGuard adını ve logosunu kullanamaz. Ticari lisans için: **switchguardapp@gmail.com**

eWeLink, CoolKit veya ITEAD ile bağlantılı değildir; onlar tarafından onaylanmamış veya desteklenmemiştir. Sonoff ve eWeLink ilgili sahiplerinin ticari markalarıdır. Tuya ile de bağlantısı yoktur.
