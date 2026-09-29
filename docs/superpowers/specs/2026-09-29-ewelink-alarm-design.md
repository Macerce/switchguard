# eWeLink Alarm — Tasarım

## Amaç
eWeLink hesabına bağlı (Sonoff) röle/priz cihazlarının **açık/kapalı konumunu** ve
**online/offline bağlantısını** izleyen bir Android uygulaması. Bir değişiklik olunca
sesli + titreşimli alarm bildirimi gönderir; kullanıcı "Kapat" diyene kadar ses döngüde çalar.

## Kısıtlar
- Ücretsiz: sunucu yok, telefon eWeLink bulut API'sine (CoolKit v2) doğrudan bağlanır.
- Geliştirici merkezi (dev.ewelink.cc) uygulamaları e-posta/şifre girişine izinli değil →
  giriş **OAuth2.0** ile (uygulama içi WebView).
- Kontrol aralığı varsayılan 30 sn (ayarlanabilir, en az 15 sn).
- Android 8.0+ (minSdk 26), targetSdk 36.

## Bileşenler
| Bileşen | Sorumluluk |
|---|---|
| `core/DeviceSnapshot` | Cihazın anlık hali: id, ad, online, kanal→açık/kapalı |
| `core/ChangeDetector` | Önceki/şimdiki snapshot'ları karşılaştırıp `Change` listesi üretir (saf Kotlin) |
| `core/Signer` | HMAC-SHA256 + Base64 imza (saf Kotlin) |
| `core/DeviceParser` | `/v2/device/thing` yanıtını snapshot'lara çevirir |
| `api/EwelinkClient` | OAuth URL üretimi, code→token, token yenileme, cihaz listesi |
| `data/Store` | SharedPreferences: kimlik bilgileri, token, bölge, son durumlar, izlenen cihazlar, bekleyen alarm satırları |
| `service/MonitorService` | Foreground service (`specialUse`), partial wakelock, periyodik sorgu |
| `service/AlarmNotifier` | Alarm kanalı (USAGE_ALARM), `FLAG_INSISTENT`, "Kapat" aksiyonu |
| `service/BootReceiver` | Yeniden başlatmada izlemeyi geri açar |
| `ui/*` | Ana ekran (durumlar, izleme seçimi, butonlar), Ayarlar, Giriş (WebView) |

## Kurallar
1. İlk çalıştırmada (kayıtlı önceki durum yok) mevcut durum temel alınır, alarm çalmaz.
2. Telefonun internet/API hatası cihaz offline sayılmaz → sessiz durum bildirimi.
3. Sadece eWeLink'in `online=false` bildirmesi "offline" alarmıdır.
4. Token hatasında (401/402) refresh denenir; olmazsa "yeniden giriş gerekli" uyarısı.
5. Alarm çalarken yeni değişiklikler aynı bildirime eklenir (en fazla 20 satır).
6. Sadece kullanıcının işaretlediği cihazlar izlenir (varsayılan: hepsi).

## Test
- Birim: `ChangeDetector`, `Signer`, `DeviceParser`.
- Manuel: cihazı eWeLink'ten aç/kapat → ≤ ~30 sn içinde alarm, "Kapat"a kadar sürer.
