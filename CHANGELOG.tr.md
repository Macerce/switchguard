# Sürüm geçmişi

[English](CHANGELOG.md)

En yeni sürüm en üsttedir. Sürüm numaraları: **büyük.orta.küçük**; küçük numara düzeltme, orta numara yeni özellik demektir.

---

## 2.10.0 — 12 Ekim 2026
**Takip eden alarm**
- Düzelmezse yeniden çal: alarmı kapattıktan sonra cihaz alarmı çaldıran durumda kalırsa (hâlâ kapalı, hâlâ açık ya da kopuk) alarm 10, 30 veya 60 dakikada bir yeniden çalar (Ayarlar → Alarm). Cihaz normale dönünce, izlenmeyince ya da o olay artık alarm vermeyince hatırlatma kendiliğinden biter.

**Yedek ve geçmiş**
- Şifreli yedek: kuralları, otomasyonları, ayarları ve Tuya ile eWeLink anahtarlarınızı şifreli bir dosyaya kaydedin, yeni telefonda geri yükleyin (Ayarlar → Yedekleme). Giriş oturumları dosyaya hiçbir zaman yazılmaz. Geri yüklemeden önce dosyanın içeriğinin özeti gösterilir.
- Geçmişi Excel ya da Google E-Tablolar'da doğrudan açılan bir CSV dosyası olarak dışa aktarın: son 24 saat, 7 gün, 30 gün ya da tümü; tüm cihazlar ya da seçili cihaz (Geçmiş → Dışa aktar).

**Bir bakışta**
- Ana ekran widget'ı: izlemenin çalışıp çalışmadığını gösterir; alarm çalarken kırmızı, sorun varken turuncu olur.
- Hızlı Ayarlar kutucuğu: izlemeyi bildirim panelinden açıp kapatın.

**Kalite ve gizlilik**
- Uygulama anahtarları ve giriş token'ları artık telefonun güvenli donanımında tutulan bir anahtarla (Android Keystore) şifreleniyor. Mevcut değerler kendiliğinden şifrelenir.
- Uygulama çökerse ayrıntılar telefonda saklanır. "Hata raporu gönder" (Ayarlar → Hakkında) bunları ve tanılama bilgilerini içeren bir e-posta hazırlar; siz göndermeden hiçbir şey paylaşılmaz.
- İngilizce metinlerde tekil/çoğul doğru kullanılıyor ("1 device", "2 devices").
- Çok cihazla daha hızlı: ayarlar her canlı güncellemede yeniden okunmuyor.
- Otomatik denetimler (birim testleri, lint, derleme) ve şifreleme, alarm bildirimi ile ses yükseltme için cihaz üstü testler.

## 2.9.0 — 11 Ekim 2026
**Sessizce susmayan alarm**
- Kritik cihaz: cihaz sayfasındaki yeni anahtar, o cihazın alarmlarını sessiz saatlerde de çaldırır (ör. gece çalışan bir taşma sensörü).
- İzleme kesintiye uğradıysa (telefon SwitchGuard'ı durdurdu ya da telefon kapalıydı) aradaki süre Geçmiş'e yazılır ve bildirim gelir; çünkü o sürede olan değişiklikler kaçırılmış olabilir. Telefon izlemeyi kapatırsa bir bekçi onu yeniden başlatır.
- SwitchGuard cihazlarınızı uzun süre göremezse (internet yok, bulut hatası, oturum düştü) 10 dakika sonra uyarı verir (Ayarlar → İzleme'den değiştirilebilir); izleme düzelince de haber verir.
- Bekleyen "koptu" alarmları ve otomasyon süreleri ("N dakikadır açık", gecikmeli eylemler) artık izleme yeniden başlasa ya da uygulama güncellense de kaybolmuyor.
- eWeLink'ten nadiren, yanıltıcı bir "başka cihazda giriş yapıldı" mesajıyla çıkış yapılması düzeltildi: uygulama ile izleme oturumu artık aynı anda yenilemiyor ve eWeLink sunucusunun geçici hataları çıkış yaptırmıyor.

**Gerçek bir alarm ekranı**
- Telefon kilitliyken ya da ekran kapalıyken alarm, büyük "Alarmı kapat" düğmeli tam ekran bir alarm sayfasıyla açılır (Android 14 ve sonrasında bir kez izin istenir).
- Alarm tam seste çalar ve müziği/videoyu duraklatır; alarmı kapatınca alarm ses seviyeniz eski haline döner (Ayarlar → Alarm → Alarm tam seste çalsın).
- Geçmiş, alarmın ne zaman ve ne kadar sürede kapatıldığını (tepki süresi) gösterir.
- Ana ekrandaki yeni kart, alarmın size ulaşmasını engelleyebilecek telefon ayarlarını gösterir: bildirimler kapalı, alarm bildirimleri kısılmış, Rahatsız Etmeyin tamamen sessiz, alarm sesi sıfır, pil kısıtlaması, tam ekran ya da otomatik başlatma izni eksik. Her birinin tek dokunuşla düzeltmesi var. Kart yalnızca bir sorun varken görünür.

**Tablet ve Android TV**
- Tablet ve Chromebook: yan gezinme çubuğu, ızgara halinde cihazlar ve rahat okunan genişlik.
- Android TV: SwitchGuard TV başlatıcısında görünür, her yerde kumandayla kullanılır ve koyu tema kullanır. Alarm, o an açık olan her şeyin üstüne çıkar ve kapatılana kadar geri gelir.
- Kurulumunuzu telefondan TV'ye ya da tablete Wi-Fi üzerinden 6 haneli kodla aktarın (Ayarlar → Başka cihaza aktar). eWeLink girişleri kopyalanmaz: her cihazın kendi eWeLink hesabı olmalıdır.
- eWeLink girişi TV kumandasıyla yapılabilir; çevrimiçi/çevrimdışı etiketleri Türkçe.

**Lisans**
- Kaynak kodun lisansı GPL-3.0'dan PolyForm Noncommercial 1.0.0'a geçiyor: kişisel ve ticari olmayan kullanım serbest kalıyor, kodu satmak veya ticari amaçla kullanmak yasak. 2.8.3 ve önceki sürümler GPL-3.0 altında kalır. Resmi sürümleri işletmeler dahil herkes kullanmaya devam edebilir.

## 2.8.3 — 4 Ekim 2026
**Bakım**
- Google Play'in eski bulduğu bir iç kütüphane güncellendi (androidx.fragment 1.1.0 → 1.9.1, Google Play hizmetleri üzerinden geliyordu). Görünür bir değişiklik yok.

## 2.8.2 — 3 Ekim 2026
**Daha fazla telefon markası için arka planda başlatma yardımı**
- Xiaomi'nin yanı sıra Samsung, Huawei/Honor, Oppo/Realme/OnePlus, Vivo/iQOO, Tecno/Infinix ve Asus telefonlarda da Ayarlar → İzinler'de "Arka planda başlatma" satırı var: markaya özel talimat ve doğru ayar ekranına kısayol.
- Aynı kart kurulum sihirbazının izinler adımında da gösteriliyor; böylece izleme telefon yeniden başlayınca sürer ve telefon uygulamayı uyutmaz.
- Markanın ayar ekranı açılamazsa uygulama bilgi sayfası açılır.

## 2.8.1 — 3 Ekim 2026
**Paylaşılan Tuya cihazları için ipucu**
- Tuya bağlantı penceresinde artık şu not var: size yalnızca paylaşılmış cihazlar görünmeyebilir; cihaz sahibinin sizi Smart Life'ta ev üyesi olarak eklemesi gerekir (Ben → Ev Yönetimi → Üye Ekle).
- Tuya bağlanıp hiç cihaz bulunamazsa pencere kapanmaz; nedenleri ve ev üyeliği ipucu gösterilir.
- Cihaz listesi boşken Tuya bağlıysa aynı ipucu listede de görünür.

## 2.8.0 — 1 Ekim 2026
**Hesap olmadan deneme (demo)**
- Kurulumda yeni **"Hesap olmadan dene (demo)"** seçeneği; Ayarlar → Hesap'ta **Demo cihazlar** anahtarı.
- Tüm özellikleriyle üç sanal cihaz: güç ölçen dondurucu prizi, kanal başına güç ölçen 4 kanallı pompa paneli ve bahçe ışığı.
- Demo cihazın sayfasında **olay canlandırma**: dışarıdan aç/kapat, bağlantıyı kopar/geri getir, kanalın gücünü düşür. Kurallar, alarm, otomasyonlar, geçmiş ve enerji gerçek cihazdaki gibi çalışır.
- İki örnek otomasyon: "Pompa durdu" (alarm) ve "Bahçe ışığı açık kaldı" (bildirim).
- Demo cihazlar ücretsiz sürümün tek cihaz sınırına sayılmaz.
- Kurulumda **"Şimdilik atla"**: hesap bağlamadan uygulamaya göz atılabilir.
- **Test alarmı** artık izleme kapalıyken de çalışır.

## 2.7.0 — 1 Ekim 2026
**Cihazları düzenleme**
- Cihazlar ekranında **Cihazları düzenle** modu (üstteki ayar simgesi ya da bir karta uzun basarak).
- **Öncelikli cihazlar:** yıldızla işaretlenen cihazlar her zaman en üstte, "Öncelikli" başlığı altında.
- **Sıralama:** yukarı/aşağı oklarla cihazların yeri değiştirilebilir.
- **Küçük / büyük kart:** küçük kart tek satırda ad, durum, güç ve aç/kapa anahtarını gösterir.
- **Kategoriler:** "Rögarlar", "Aydınlatma" gibi kategoriler oluşturma, yeniden adlandırma, sıralama, silme; cihazları kategorilere atama; başlığa dokunarak grubu katlama.

## 2.6.2 — 1 Ekim 2026
- eWeLink ve Tuya için **resimli kurulum rehberleri** (Türkçe/İngilizce, GitHub'da).
- Ayarlar → Hakkında'ya rehber bağlantıları; kurulum sihirbazına "Resimli rehberi aç" düğmesi.
- Düzeltme: durum kartında ara sıra görünen "Güncellendi: … saniye **sonra**" yazısı.
- Enerji notu markadan bağımsız hale getirildi.

## 2.6.1 — 1 Ekim 2026
- İlk kurulumda **"Cihazlarınız nerede?"** adımı: eWeLink ya da Tuya / Smart Life yolu.
- Tuya için adım adım Cloud projesi rehberi ve bağlantı adımı.
- **Güç düşüşü alarmı** artık tek kanallı güç ölçerli prizlerde de çalışıyor (Tuya prizler, eWeLink POW).
- Gizlilik politikası Tuya'yı kapsayacak şekilde güncellendi.

## 2.6.0 — 1 Ekim 2026
**Tuya / Smart Life desteği**
- Smart Life ve Tuya Smart cihazları, kullanıcının kendi ücretsiz Tuya Cloud projesiyle izlenir.
- Tuya mesaj servisi (Pulsar) ile **1 saniyenin altında** anlık alarm.
- eWeLink ve Tuya cihazları aynı listede; alarm, kural, otomasyon ve geçmiş ikisinde de aynı.
- Tuya deneme süresi dolarsa ya da anahtarlar hatalıysa uyarı.
- Ayarlar → Hesap → Tuya / Smart Life bağlantı ekranı.

## 2.5.2 — 1 Ekim 2026
- Ayarlar → Hakkında → **İletişim ve geri bildirim** (switchguardapp@gmail.com).

## 2.5.1 — 1 Ekim 2026
- **Geçmiş seçili dilde** gösteriliyor; eski kayıtlar da otomatik çevrildi.
- Dil değişince yeni bildirimler de hemen yeni dilde.

## 2.5.0 — 1 Ekim 2026
- Uygulama içi **dil seçimi**: Telefonun dili / English / Türkçe.

## 2.4.0 — 1 Ekim 2026
- Yeni otomasyon: **"Açıkken gücü şunun altına düşerse"** (SONOFF SPM-4Relay / DUALR3 kanalları için); açılış payı ve süre ayarı.

## 2.3.1 — 30 Eylül 2026
- Düzeltme: bazı App ID'lerde anlık bağlantı kurulamıyor ve kısa açılıp kapanmalar kaçıyordu.
- Geçmiş sekmesinde görülmemiş olay rozeti.
- Xiaomi telefonlar için "Otomatik başlatma" ayarı; uygulama açılınca izleme servisi gerekirse yeniden başlar.

## 2.3.0 — 30 Eylül 2026
- **SwitchGuard Pro** (Google Play üzerinden tek seferlik satın alma): ücretsiz sürüm 1 cihazı, Pro tüm cihazları izler.
- Düzeltme: diğer sekmelerden geri tuşu Cihazlar'a döner.

## 2.2.0 — 30 Eylül 2026
- **Cihaza özel alarm sesi.**
- eWeLink oturumu başka telefonda açılınca **"İzleme durdu — oturum kapandı"** uyarısı.
- Açık kaynak lisans (GPL-3.0).

## 2.1.0 — 29 Eylül 2026
- **Enerji izleme:** güç, gerilim, akım; günlük/aylık tüketim ve 7 günlük grafik.
- **Otomasyonlar:** "şu süre açık/kapalı kalınca", güç eşikleri, cihaz aç/kapa, alarm/bildirim.
- eWeLink **cihaz zamanlayıcıları** ve **günlük özet** bildirimi.
- Çok kanallı cihazlarda kanallar açılır/kapanır özet satırında.
- Düzeltme: uygulamadan aç/kapa sonrası durumun eski değere dönmesi.

## 2.0.1 — 29 Eylül 2026
- Düzeltme: koyu temada okunamayan yazılar.

## 2.0.0 — 29 Eylül 2026
**SwitchGuard**
- Yeni ad ve Material 3 tasarım; Türkçe ve İngilizce.
- eWeLink WebSocket ile **anlık izleme**, kopukluğa karşı periyodik yedek kontrol.
- Cihaz başına kurallar (alarm / bildirim / yok), sessiz saatler, kısa kopmaları yok sayma.
- Uygulamadan aç/kapa, olay geçmişi, logo ve mağaza görselleri.

## 1.0 — 29 Eylül 2026
- İlk sürüm ("eWeLink Alarm"): eWeLink cihazlarının aç/kapa ve çevrimiçi durumunu izleyip alarm çalan basit uygulama.
