# SwitchGuard for Windows — Denemeler (spikes)

Tarih: 2026-10-06 · Dal: `windows`

Bu belge, tasarımın (spec §6) dayandığı iki denemenin sonucunu tutar. Gizli bilgi yazılmaz: accessId `nj3***jj` biçiminde maskelidir, secret hiç yazılmaz, cihaz kimlikleri maskelidir.

## Deneme B — Tuya çift tüketici (1. aşama, yapıldı)

### Soru

Tuya mesaj servisi (Pulsar WebSocket, `subscriptionType=Failover`) aynı bulut projesinin anahtarlarıyla bağlanan iki tüketiciye (telefon + masaüstü) her cihaz mesajını veriyor mu? Masaüstü ayrı bir abonelik adıyla (`{accessId}-sub-desktop`) bağlanıp akışın kendi tam kopyasını alabilir mi?

### Yöntem

- Araç: `desktop/tools/pulsar_consumers.py` (`tools/tuya_pulsar_probe.py` bağlantı ve çözme mantığı; `--label`, `--sub`, `--seconds`, isteğe bağlı `--ping`). Yalnızca dinler ve mesajları onaylar (ack); cihaza komut göndermez, Tuya yazma API'si çağırmaz. Her mesajda etiket, maskeli cihaz kimliği, veri noktası kodları ve zamanı yazar; sonunda toplam yazar.
- Bağlantı: `wss://mqe.tuyaeu.com:8285/ws/v2/consumer/persistent/{accessId}/out/event/{abonelik}?ackTimeoutMillis=3000&subscriptionType=Failover`.
- Telefon, denemeler sürerken aynı projede ve `{accessId}-sub` aboneliğinde üçüncü tüketiciydi (eWeLink etkilenmez).
- Tuya tarafında tek cihaz: Bilicra akıllı priz (çevrimiçi ve yükteyken `cur_power` / `cur_current` / `cur_voltage` bildirir). Prize dokunulmadı.
- Her çalıştırma 5 dk (300 sn).

### Ham sonuç

| Çalıştırma | Saat | Tüketici | Abonelik | Bağlantı | Mesaj sayısı | Not |
|---|---|---|---|---|---|---|
| 1 | 09:23:21–09:28:21 | A | `nj3***jj-sub` | kabul (HTTP 101) | **0** | ping yok; bağlantı tam 300,0 sn sonra düştü (boş çerçeve) |
| 1 | 09:23:21–09:28:21 | B | `nj3***jj-sub` | kabul (HTTP 101) | **0** | ping yok; bağlantı tam 300,0 sn sonra düştü (boş çerçeve) |
| 2 | 09:29:42–09:34:44 | A | `nj3***jj-sub` | kabul (HTTP 101) | **0** | 30 sn'de bir WebSocket ping; 300 sn kopmadan sürdü |
| 2 | 09:29:42 | B | `nj3***jj-sub-desktop` | **reddedildi, HTTP 500** | — | 5 sn arayla 2 deneme, ikisi de aynı hata; sonra elle durduruldu |

Reddedilen bağlantının tam hata gövdesi (maskeli):

```
HTTP 500 Failed to create producer/consumer: ...PulsarClientException:
Failed to subscribe persistent://nj3***jj/out/event with 11 partitions
{"errorMsg":"Subscription does not exist", ...}
```

Ek bağlantı denemeleri (tek seferlik, yalnızca bağlanma): `nj3***jj-sub-desktop` yine HTTP 500 (aynı "Subscription does not exist"; bir tanı denemesinde bağlantı 5 sn'de zaman aşımına uğradı, tekrarında 500); düz `desktop` adı da aynı 500 ile reddedildi.

Çalıştırma 1 ve 2'de A/B hiçbir mesaj almadı (toplam 0). Aynı abonelikte "iki tüketiciye de aynı mesajlar geliyor mu" sorusu bu yüzden ölçülemedi.

### Sonuç

1. **Ayrı abonelik adı çalışmıyor (kesin).** Tuya mesaj servisi `{accessId}-sub` dışında yeni bir abonelik oluşturmaya izin vermiyor: "Subscription does not exist" (HTTP 500). Masaüstünün kendi aboneliğiyle akışın ayrı bir kopyasını alması mümkün değil.
2. **Aynı abonelikte iki tüketici: sonuçsuz (ölçülemedi).** İki tüketici aynı abonelikte sorunsuz bağlanıyor (Failover bunu kabul ediyor), ancak 10 dakika boyunca hiç mesaj gelmedi. Bunun nedeni ölçümden ayırt edilemiyor: priz o sırada rapor yollamıyor olabilir ya da ilgili bölümün etkin tüketicisi telefon olabilir. Tasarım açısından bunun ölçülmesi gerekmiyor; aşağıdaki gerekçeye bakın.
3. **Beklenen davranış (belge/tasarım bilgisi, bu denemede gözlenmedi):** konu 11 bölümlü (partition); `Failover` aboneliğinde her bölümün aynı anda tek etkin tüketicisi vardır, mesaj o tüketiciye gider, diğerleri beklemede kalır. Yani aynı abonelikte iki cihaz her mesajı birlikte almaz; masaüstü bağlanırsa bazı bölümlerin (dolayısıyla bazı cihazların) canlı mesajlarını telefondan alabilir.
4. **Yan bulgu: boşta bağlantı 5 dk'da kapanıyor.** Ping göndermeyen iki bağlantı da tam 300,0 sn'de düştü (istemciye boş çerçeve geldi; büyük olasılıkla sunucunun boşta zaman aşımı); 30 sn'de bir ping atan bağlantı 300 sn boyunca açık kaldı. Telefonun `TuyaSocket` (OkHttp `pingInterval(30 sn)`) bunu zaten yapıyor; masaüstü aynı sınıfı değişmeden kullandığı için ek iş yok.
5. `Connection-Id` başlığı denemede tüketici başına farklıydı (`probe-A`, `probe-B`); telefon `switchguard` yolluyor. Aynı `Connection-Id` ile iki bağlantının birbirini düşürüp düşürmediği denenmedi.

### Tasarıma etkisi

Ayrı abonelik çalışmadığı için spec §6'daki geri dönüş kuralı geçerli: Tuya canlı bildirimi masaüstünde **varsayılan kapalı** (`tuyaLive = false`, telefonun canlı bağlantısını çalmasın); kapalıyken Tuya her **5 dk'da bir sorgulanır**. Ayarlar'da "Tuya canlı bildirimleri bu cihazda" anahtarı olur; kullanıcı açarsa bunun telefonun canlı mesajlarının bir kısmını (veya tamamını) alabileceği uyarısı gösterilir. "Masaüstü kendi aboneliğiyle canlı, varsayılan açık" yolu **uygulanmaz**. Spec §6, §8 (Ayarlar) ve §14 buna göre güncellendi.

### Tekrar (isteğe bağlı, 2. aşama)

Aynı abonelikte iki tüketicinin mesaj paylaşımını ölçmek istenirse: priz yükteyken (ör. kullanıcı prize bağlı cihazı açıp kapatarak rapor üretirken) iki süreç `python desktop/tools/pulsar_consumers.py --label A --seconds 300` ve `--label B` ile birlikte çalıştırılır; hangi etiketin hangi mesajı aldığı `dataId` ile karşılaştırılır. Tasarım kararını değiştirmesi beklenmez.

## Deneme A — beklemede (yedek eWeLink hesabının geliştirici onayı bekleniyor)

eWeLink geliştirici konsolunun `http://127.0.0.1:<port>/` biçiminde bir dönüş adresini kabul edip etmediği, yedek eWeLink hesabının geliştirici onayı (ve kendi App ID / App Secret'ı) gelince denenecek. 2026-10-06 itibarıyla onay bekleniyor; bu yüzden deneme 2. aşamanın başına ertelendi (Task 13). Sonuç gelene kadar spec §6'daki yedek yol (dönüş adresi telefondaki gibi `https://127.0.0.1`, pano izleyen giriş) geçerlidir.
