# Sürüm geçmişi

[English](CHANGELOG.md)

En yeni sürüm en üsttedir. Sürüm numaraları: **büyük.orta.küçük**; küçük numara düzeltme, orta numara yeni özellik demektir.

---

## 2.17.0 — 27 Ekim 2026
**Yenilikler**
- Uyarı kategorileri: her cihaz Kritik, Önemli ya da Normal'dir. Cihaz sayfasından seçebilir ya da Otomatik bırakabilirsiniz — bir olayında Alarm seçili cihaz Kritik, yalnız bildirim gönderen Önemli olur. Uyarılar sekmesi cihazları kategoriye göre süzer ve rozet gösterir; Uyarılar → Uyarı ayarları → Uyarı kategorileri'nden her kategori için alarmın sessiz saatlerde de çalıp çalmayacağı seçilir (önceden kritik işaretlenen cihazlar çalmaya devam eder).
- Otomasyonlar ve koşullu uyarılar bir kategoriyle tetiklenebilir: "kritik cihazlardan biri çevrimdışı olunca alarm çal".
- Gruplar: tek düğmeyle birden çok cihazı ya da kanalı açıp kapatın. Gruplar cihazların arasında görünür, otomasyonlarda kullanılabilir; widget, saat ve Telegram'da da (/on, /off, /status) çalışır.
- Cihaz kartlarını basılı tutup sürükleyerek sıralayın; TV'de OK'a basılı tutup yön tuşlarıyla taşıyın.
- GitHub'dan kurulan kopyalar yeni sürüm çıkınca haber verir; Google Play kopyaları Play'in uygulama içi güncellemesini kullanır.

**Düzeltmeler**
- SONOFF POWR316 (ve benzeri cihazlar) yeniden açılıp kapatılabiliyor.

## 2.16.0 — 23 Ekim 2026
**Yenilikler**
- Uygulama kısayolları: SwitchGuard simgesine basılı tutarak uygulamayı açmadan Evdeyim / Dışarıdayım arasında geçebilir ya da ilk üç sahnenizden birini çalıştırabilirsiniz. Sonuç kısa bir mesajla gösterilir ve geçmişe "Uygulama kısayolu" olarak yazılır.
- Bir haftalık kullanımdan sonra SwitchGuard bir kez Google Play puanı isteyebilir (yalnızca izleme açıkken ve alarm çalmıyorken; TV'de ve GitHub'dan kurulan kopyalarda hiç sorulmaz).

**Düzeltmeler**
- Uyarılar sekmesinde Evdeyim / Dışarıdayım'a basınca seçim kaydediliyor ama düğmeler ekranda değişmiyordu.

## 2.15.2 — 23 Ekim 2026
**Düzeltmeler**
- SONOFF MINIR3 gibi tek röleli cihazlar (ve bazı Micro, M5, T5 modelleri) artık 4 kanallı görünmüyor: bulut bunlar için 4 kanal bildiriyor, artık yalnızca gerçek kanallar gösteriliyor. Açıp kapatma eskisi gibi çalışıyor.

## 2.15.1 — 22 Ekim 2026
**Düzeltmeler**
- Konum donanımı olmayan cihazlara (bazı Android TV'ler ve tabletler) SwitchGuard yeniden kurulabiliyor. Konum yalnızca, isteğe bağlı olarak, gün doğumu/batımı ve sıcaklık kuralları için kullanılır.

## 2.15.0 — 22 Ekim 2026
**Akıllı uyarılar** (Uyarılar → Uyarı ayarları → Akıllı uyarılar)
- Elektrik kesintisi: izlenen cihazların hepsi aynı anda çevrimdışı olursa cihaz başına ayrı ayrı değil, tek bir "elektrik kesildi" uyarısı gelir; elektrik gelince süresiyle birlikte haber verilir. Yerinde prize takılı bırakılan bir telefon ya da tablet de elektrik algılayıcısı olabilir: bir dakika şarjdan çıkarsa kesinti sayılır, internet yokken bile (mesaj Telegram ile mobil veriden gider).
- Evde / Dışarıda ve tatil modu: evdeyken kritik olmayan cihazların alarmı bildirime döner; kurallar yalnızca dışarıdayken çalışabilir; tatilde her alarm çalar ve her sabah rapor gelir. Uyarılar sekmesinden, widget'tan, saatten ya da Telegram'dan değiştirilir.
- Anormal tüketim: her sabah dün, önceki iki haftayla karşılaştırılır; çok yüksek (arıza) ya da çok düşük (neredeyse hiç çalışmamış dondurucu) tüketim bildirilir. İsteğe bağlı aylık bütçe.
- Her pazartesi 09:00'da haftalık rapor: kesintiler, alarmlar ve tepki süresi, bağlantı kopmaları ve tüketim, önceki haftayla karşılaştırmalı.

**Telegram**
- Komutlar: /durum, /gordum, /ac ad, /kapat ad, /sahne ad, /evde, /disarida. Alarm mesajındaki "Gördüm" düğmesi alarmı üstlenir.
- Kademeli haber: N dakika sonra bir kişiye, hâlâ kimse ilgilenmediyse M dakika sonra başka birine.
- Bekçi: sabitlenmiş bir mesaj SwitchGuard'ın çalıştığını gösterir; telefon durursa karşı taraf bunu anlar.

**Otomasyon**
- Yeni tetikleyiciler: gün doğumu ve gün batımı (kaydırmalı), şundan sıcak / şundan soğuk (dış sıcaklık Open-Meteo'dan) ve gücün en az N dakika eşiğin üstünde/altında kalması.
- Sınırlar: yalnızca belirli saat ve günlerde, yalnızca evde ya da dışarıdayken ve "başka bir cihaz açık/kapalıysa".
- Sahneler: tek dokunuşla birden çok cihazı aç/kapat (widget, saat ve Telegram'dan da).

**Enerji**
- Cihaz başına kWh fiyatı; tek zamanlı ya da üç zamanlı (gündüz / puant / gece) tarife.
- Bekleme tüketimi: cihazın hiç altına inmediği güç ve aylık maliyeti.
- Raporlar yenilendi: günlük özet, haftalık rapor ve enerji raporu uygulamada, PDF'te ve Telegram'da aynı düzende — özet kartları, grafik, sade dille öne çıkanlar, "ne yapmalı", ayrıntılar ve terimler.
- Haftalık raporda her cihazın Wi-Fi sinyali ve neden koptuğu.

**Yeni**
- Cihaz widget'ı: bir cihazı ya da kanalı tek dokunuşla açıp kapatır.
- Wear OS saat uygulaması: alarm bilekte, kapatma, cihaz açıp kapatma ve sahne çalıştırma.

## 2.14.0 — 21 Ekim 2026
**Enerji geçmişi ve rapor**
- Enerji ölçen cihazların sayfasında Gün · Ay · Yıl seçilerek geçmiş tüketim görülür; oklarla önceki günlere, aylara, yıllara gidilir. Grafik günde saat saat, ayda gün gün, yılda ay ay gösterir; bir çubuğa dokununca o dilimin tüketimi, en yüksek gücü ve maliyeti yazılır.
- Her dönem için toplam, ortalama güç, en yüksek güç (zamanıyla), günlük/aylık ortalama ve en çok tüketen saat/gün/ay.
- kWh fiyatı girilirse tahmini maliyet gösterilir.
- Rapor al: seçili dönem PDF (yazdırmak, paylaşmak) ya da CSV (Excel) olarak kaydedilir. Raporda özet, grafik, her saat/gün/ay için tüketim, pay, ortalama ve en yüksek güç, zirve anı, ölçülen süre, maliyet ve anahtarın kaç kez açılıp ne kadar açık kaldığı bulunur.

**Güvenlik**
- eWeLink bölgesi yalnızca bilinen sunuculardan (eu, us, as, cn) biri olabilir; giriş bilgileri başka bir sunucuya gönderilemez.
- Wi-Fi ile başka cihaza aktarmada gelen veri boyutu sınırlandı; bozuk ya da kötü niyetli bir gönderici uygulamanın belleğini dolduramaz.

**Altyapı**
- Android 17 (API 37) hedefleniyor; derleme araçları ve kütüphaneler güncellendi (AGP 9.4, Kotlin 2.4, Compose 2026.09).

## 2.13.0 — 17 Ekim 2026
**Uyarıların kendi sekmesi var**
- Menü artık Cihazlar · Uyarılar · Otomasyon · Geçmiş · Ayarlar. SwitchGuard'ın asıl işi uyarmak; uyarıyla ilgili her şey tek yerde:
  - durum kartı: "Alarm hazır", izleme kapalı ya da alarmı engelleyebilecek ayarlar; Test alarmı düğmesi;
  - cihaz uyarıları: her cihazın hangi değişiklikte alarm ya da bildirim verdiği (kritik cihazlar işaretli); dokununca o cihazın uyarı kuralları açılır;
  - koşullu uyarılar: cihaz bir süre açık ya da kapalı kalınca veya gücü değişince uyar;
  - uyarı ayarları: Alarm, Sessiz saatler ve İkinci kişiye haber ver, Ayarlar'dan buraya taşındı.
- Otomasyon sekmesinde artık yalnızca cihazı açan ya da kapatan kurallar var. Yeni koşullu uyarı varsayılan olarak alarm çalar.

**İzleme**
- Anlık bağlantı çalışırken tek bir başarısız bulut kontrolü (ör. ağ değişiminde tek bir zaman aşımı) artık "İnternet yok" göstermiyor; kontrol 15 sn, 30 sn, 1 dk ve 2 dk sonra yeniden deneniyor. Önceden koruma kartı, alarmlar gelmeye devam etse de 5 dakikaya kadar sorun gösteriyordu.
- Yalnızca kendisiyle paylaşılmış cihazları olan hesaplar (ör. ikinci cihazda kullanılan hesap) artık 30 saniyede bir kontrol yerine anlık bağlantıyla izleniyor.

## 2.12.3 — 17 Ekim 2026
**Düzeltmeler**
- Günlük özet: izleme kapalıyken uygulamadan açılıp kapatılan cihaz (ya da demo kapatılıp yeniden açılınca demo cihazı) bütün gün açık sayılabiliyordu. Bu değişiklikler de artık kaydediliyor.

## 2.12.2 — 17 Ekim 2026
**Düzeltmeler**
- Ayarlar → Günlük özet → "Bugünün özetini şimdi göster" özeti küçük bir pencerede açıyor. Önceden bildirim gönderiyordu; telefonda durum çubuğunda yalnızca simge görünüyor, TV'de hiç görünmüyordu.
- Otomasyonlar: "açıkken gücü düşerse" otomasyonunda "açıldıktan sonra bekle" 0 dakikaysa, düzenleyici açılınca 2 dakika görünüyor ve Kaydet'e basılınca 2 olarak kaydediliyordu. Kayıtlı 0 artık korunuyor.

## 2.12.1 — 16 Ekim 2026
**Düzeltmeler**
- TV: saat seçimi (sessiz saatlerin başlangıcı ve bitişi, günlük özet saati, cihaz zamanlayıcıları) artık kumandayla yapılabiliyor. Sistemin saat kadranı kumandayla kullanılamıyordu; TV'de uygulama artık kendi saat seçicisini gösteriyor: yukarı ve aşağı saati ya da dakikayı değiştirir, saatteyken OK dakikaya geçer, dakikadayken OK kaydeder, Geri tuşu vazgeçer. Telefon ve tablette alışılmış saat penceresi kalıyor.
- TV: telefondan alma adımları artık Ayarlar → Yedekleme ve aktarım → Başka cihaza aktar yolunu gösteriyor.

## 2.12.0 — 15 Ekim 2026
**Yeni görünüm**
- Cihazlar ekranı artık bir akıllı ev uygulaması gibi: cihazlar ızgarada kutucuklar halinde (telefonda satırda iki tane). Her kutucukta cihazın neyi kontrol ettiğine uyan bir simge (ışık, su ya da pompa, dondurucu, ısıtma, fan, kapı, priz; adından tahmin ediliyor), durumu, gücü ve yuvarlak bir güç düğmesi var. Açık cihazın kutucuğu SwitchGuard yeşiliyle parlıyor; çevrimdışı cihaz soluk ve kırmızı noktalı görünüyor.
- Çok kanallı cihazlar (4CH, SPM, DualR3) her kanal için ayrı düğmesi olan geniş bir kutucukla gösteriliyor; cihazı açmadan istediğiniz kanalı açıp kapatabilirsiniz. Güç ölçen kanallar watt değerini de gösteriyor.
- En üstteki koruma kartı izlemenin canlı olup olmadığını, kaç cihazın açık ya da çevrimdışı olduğunu ve toplam gücü gösteriyor.
- Cihaz sayfası: ortada büyük bir güç düğmesi (çok kanallı cihazlarda büyük kanal düğmeleri), üstünde bağlantı, izleme ve güç bilgisi.
- Otomasyonlar "Eğer … / O zaman …" diye okunuyor; eylemin renkli simgesi yanında.
- Otomasyon düzenleyici açılır listeler yerine cihaz kartları, olay ve işlem kutucukları, numaralı kanal düğmeleri ve dakikalar için − / + kullanıyor; Kaydet'in üstünde "Eğer / O zaman" özeti var.
- Geçmiş'te her olayın simgesi renkli bir daire içinde: alarmlar kırmızı, bildirimler turkuaz.
- Tam ekran alarmda zilin çevresinden dışa yayılan halkalar var.
- Uygulama renklerini artık duvar kâğıdından almıyor, açık ve koyu temada SwitchGuard'ın kendi renklerini kullanıyor; her telefonda aynı görünüyor.

**Daha sade ayarlar**
- Ayarlar artık kısa bir kategori listesi: Hesaplar, Alarm, Sessiz saatler, İkinci kişiye haber ver, Günlük özet, İzleme, İzinler, Yedekleme ve aktarım, Dil, Yardım ve hakkında. Birine dokununca kendi sayfası açılıyor. Her satırda tek satırlık bir özet var, örneğin turuncu renkte "2 izin eksik".
- Ayarlar sekmesine yeniden dokununca listeye dönülüyor. TV'de kumanda, açtığınız satıra geri geliyor.

**Cihazları düzenle**
- Kart boyutu artık "Küçük kart" (yarım satır) ya da "Büyük kart" (tam satır). Çok kanallı cihazlar büyük, diğerleri küçük başlıyor.

## 2.11.0 — 14 Ekim 2026
**İkinci kişiye haber ver (Telegram)**
- Alarm seçtiğiniz süre (2, 5, 10, 15 ya da 30 dakika) içinde kapatılmazsa seçtiğiniz kişilere ya da aile grubunuza Telegram mesajı gidiyor; alarm kapatılınca da "kapatıldı" mesajı gidiyor. Gece telefon başka odada kalsa bile evdeki biri ya da komşunuz haberdar oluyor (Ayarlar → Alarm → İkinci kişiye haber ver).
- Mesajları sizin oluşturduğunuz Telegram botu gönderiyor; arada başka sunucu yok ve ücretsiz. Kurulum pencereniz adım adım götürüyor: BotFather'dan bot oluşturma, anahtarı deneme, kişilerin bota Başlat'a dokunması, "Kişileri bul", deneme mesajı.
- Test alarmları kimseye mesaj göndermiyor. İnternet yoksa dakikada bir yeniden deneniyor; her deneme ve hata Geçmiş'te görünüyor.

**TV: anahtarları telefondan girin**
- TV'de App ID/Secret, Tuya Access ID/Secret ve Telegram bot anahtarı alanlarının yanında "Telefondan gir" düğmesi var. TV bir QR kodu gösteriyor; telefonun kamerasıyla okutup açılan sayfaya anahtarları yapıştırıyorsunuz, kumandayla uzun anahtar yazmanız gerekmiyor. Telefonda SwitchGuard kurulu olması gerekmiyor.
- Bilgiler telefonun tarayıcısında QR koddaki tek seferlik anahtarla şifreleniyor ve doğrudan TV'ye gidiyor; ev ağınızdan çıkmıyor.

## 2.10.2 — 14 Ekim 2026
**İkiz uygulama uyarısı**
- Telefonun Çift uygulama (ikiz uygulama) özelliğiyle açılmış ikinci bir SwitchGuard kopyası artık fark ediliyor. Yan yana çalışan iki kopya her alarmı iki kez çalar, aynı eWeLink hesabını kullanınca birbirinin oturumunu kapatır; Xiaomi telefonlarda izinler de kapalı görünür. İkiz kopyada "Bu, SwitchGuard'ın ikinci kopyası", ana kopyada "Telefonda ikinci bir SwitchGuard var" uyarısı çıkıyor ve nasıl kaldırılacağını anlatıyor. İki kopyayı bilerek kullananlar uyarıyı kapatabilir.
- Xiaomi, OPPO, realme, OnePlus, Samsung ve vivo ikiz uygulamaları ile Android 14+ klon profili ve özel alan tanınıyor.

**Test alarmı**
- Test alarmı artık sesli ya da sessiz (yalnızca ekran) çalabiliyor. Sessiz testte ses ve titreşim olmuyor; gece kimseyi uyandırmadan alarm ekranını ve bildirimi görebilirsiniz.
- "10 saniye sonra çal" seçeneği: bu sürede telefonu kilitleyip alarm ekranının kilit ekranında açıldığını görebilirsiniz. TV'de başka bir uygulamaya geçip alarmın onun üstüne geldiğini görebilirsiniz.
- Sessiz test sürerken gerçek bir alarm gelirse ses yine çalıyor.

## 2.10.1 — 13 Ekim 2026
**Xiaomi telefonlar**
- Xiaomi (MIUI / HyperOS), tam ekran izni açık olsa bile "Kilit ekranında görüntüle" ve "Arka planda çalışırken yeni pencereler açın" izinleri kapalıyken alarm ekranının kilit ekranında açılmasını engelliyor. Uygulama artık bu iki izni okuyor, "Alarm size ulaşmayabilir" kartında, Ayarlar'da ve ilk kurulumda gösteriyor; "Düzelt" düğmesi doğrudan Xiaomi'nin "Diğer izinler" sayfasını açıyor.
- Otomatik başlatmanın açık olup olmadığı Xiaomi'de artık okunuyor; "Yaptım" diye onaylamanız gerekmiyor.

**Alarm sesi**
- Alarm çalarken telefonun ses tuşları artık alarmın sesini ayarlıyor (SwitchGuard ekranı açıkken); önceden medya sesini değiştiriyordu.
- "Alarm tam seste çalsın" açıkken, alarm çalarken sesi kısarsanız alarmı kapatınca alarm sesi eski seviyesine dönüyor. Önceden ses zaten en yüksekteyse kısık kalıyor, telefonun saat alarmı da kısık çalıyordu.

**TV**
- Ana ekranda kumandayla kartın içindeki aç/kapa anahtarına ve "4 kanal" satırına gidilebiliyor; önceden odak bunları atlıyor, Tamam tuşu her zaman cihazın sayfasını açıyordu. Cihaz sayfası artık kartın başlığından açılıyor.
- Anahtara basınca odak yerinde kalıyor (önceden yan menüye atlıyordu).
- Cihaz sayfasından dönünce odak aynı karta dönüyor.

**Telefon ve tablet**
- Cihaz sayfasından dönünce liste en başa atlamıyor; kaydırma konumu ve açık kanal grupları korunuyor.

## 2.10.0 — 12 Ekim 2026
**Takip eden alarm**
- Düzelmezse yeniden çal: alarmı kapattıktan sonra cihaz alarmı çaldıran durumda kalırsa (hâlâ kapalı, hâlâ açık ya da kopuk) alarm 10, 30 veya 60 dakikada bir yeniden çalar (Ayarlar → Alarm). Cihaz normale dönünce, izlenmeyince ya da o olay artık alarm vermeyince hatırlatma kendiliğinden biter.

**Yedek ve geçmiş**
- Şifreli yedek: kuralları, otomasyonları, ayarları ve Tuya ile eWeLink anahtarlarınızı şifreli bir dosyaya kaydedin, yeni telefonda geri yükleyin (Ayarlar → Yedekleme). Giriş oturumları dosyaya hiçbir zaman yazılmaz. Geri yüklemeden önce dosyanın içeriğinin özeti gösterilir.
- Geçmişi Excel ya da Google E-Tablolar'da doğrudan açılan bir CSV dosyası olarak dışa aktarın: son 24 saat, 7 gün, 30 gün ya da tümü; tüm cihazlar ya da seçili cihaz (Geçmiş → Dışa aktar).

**Bir bakışta**
- Ana ekran widget'ı: izlemenin çalışıp çalışmadığını gösterir; alarm çalarken kırmızı, sorun varken turuncu olur.
- Hızlı Ayarlar kutucuğu: izlemeyi bildirim panelinden açıp kapatın (telefon kilitliyse önce kilidi açmanız istenir).

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
