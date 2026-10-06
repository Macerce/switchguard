# SwitchGuard for Windows — Denemeler (spikes)

Tarih: 2026-10-06 · Dal: `windows`

Bu belge, tasarımın (spec §6) dayandığı iki denemenin sonucunu tutar. Gizli bilgi yazılmaz: accessId `nj3***jj` biçiminde maskelidir, secret hiç yazılmaz, cihaz kimlikleri maskelidir.

## Deneme B — Tuya çift tüketici (1. aşama, yapıldı)

### Soru

Tuya mesaj servisi (Pulsar WebSocket, `subscriptionType=Failover`) aynı bulut projesinin anahtarlarıyla bağlanan iki tüketiciye (telefon + masaüstü) her cihaz mesajını veriyor mu? Masaüstü ayrı bir abonelik adıyla (`{accessId}-sub-desktop`) bağlanıp akışın kendi tam kopyasını alabilir mi?

### Yöntem

- Araç: `desktop/tools/pulsar_consumers.py` (`tools/tuya_pulsar_probe.py` bağlantı ve çözme mantığı; `--label`, `--sub`, `--seconds`, isteğe bağlı `--ping`). Yalnızca dinler ve mesajları onaylar (ack); cihaza komut göndermez, Tuya yazma API'si çağırmaz. Her mesajda etiket, maskeli cihaz kimliği, veri noktası kodları ve zamanı yazar; sonunda toplam yazar. El sıkışma reddinde durum satırını ve HTML'den ayıklanmış hata metnini (kırpmadan, maskeli) yazar; 3 reddten sonra yeniden denemeyi bırakır.
- Bağlantı: `wss://mqe.tuyaeu.com:8285/ws/v2/consumer/persistent/{accessId}/out/event/{abonelik}?ackTimeoutMillis=3000&subscriptionType=Failover`.
- Telefon, denemeler sürerken aynı projede ve `{accessId}-sub` aboneliğinde üçüncü tüketiciydi (eWeLink etkilenmez).
- Tuya tarafında tek cihaz: Bilicra akıllı priz (çevrimiçi ve yükteyken `cur_power` / `cur_current` / `cur_voltage` bildirir). Prize dokunulmadı.
- Her çalıştırma 5 dk (300 sn).

### Ham sonuç

Kayıtlar (çalışma alanında, depoda değil): çalıştırma 1 ve 2 günlükleri (`run1_A/B.log`, `run2_A/B.log`) ve aşağıdaki tekrar (`repro_B.log`). Çalıştırma 1 ve 2'de aracın daha önceki sürümleri kullanıldı (çalıştırma 1: ping ve çerçeve türü yazımı yok; ikisinde de reddi yalnızca 300 karakterlik ham HTML olarak yazıyordu); reddin tam metni ancak tekrarda alındı.

| Çalıştırma | Saat | Tüketici | Abonelik | Bağlantı | Mesaj sayısı | Not |
|---|---|---|---|---|---|---|
| 1 | 09:23:21–09:28:21 | A | `nj3***jj-sub` | kabul (HTTP 101) | **0** | ping yok; bkz. "300 sn olayı" |
| 1 | 09:23:21–09:28:21 | B | `nj3***jj-sub` | kabul (HTTP 101) | **0** | ping yok; bkz. "300 sn olayı" |
| 2 | 09:29:42–09:34:44 | A | `nj3***jj-sub` | kabul (HTTP 101) | **0** | 30 sn'de bir WebSocket ping; kopma yazılmadı |
| 2 | 09:29:42–09:29:54 | B | `nj3***jj-sub-desktop` | **reddedildi, HTTP 500** | — | 3 deneme (09:29:42.9, 09:29:48.4, 09:29:53.9; 5 sn arayla), üçü de HTTP 500; sonra elle durduruldu |
| Tekrar | 09:42:02–09:42:13 | B | `nj3***jj-sub-desktop` | **reddedildi, HTTP 500** | — | 3 deneme, üçü de HTTP 500; araç 3. reddden sonra durdu |

**Reddin tam metni.** Tekrar, yalnızca dinleyen araçla tek seferde yapıldı:

```
python desktop/tools/pulsar_consumers.py --label B --seconds 30 --sub '{CID}-sub-desktop'
```

Araç durum satırını `Handshake status 500 Server Error` olarak yazdı. HTML'den ayıklanmış hata metni (accessId aracın kendi maskelemesiyle `nj3***jj`; 1. denemenin çıktısı, birebir; metin HTML'de başlıkta, gövde başlığında ve `MESSAGE:` satırında üç kez tekrar eder, burada bir kez ve ardından URI/SERVLET satırı verilir):

```
Error 500 Failed to create producer/consumer: java.util.concurrent.ExecutionException: org.apache.pulsar.client.api.PulsarClientException: Failed to subscribe persistent://nj3***jj/out/event with 11 partitions {"errorMsg":"Subscription does not exist","reqId":4044833348924281227, "remote":"172.21.10.55/172.21.10.55:6650", "local":"/172.21.10.13:32440"}
...
URI: /ws/v2/consumer/persistent/nj3***jj/out/event/nj3***jj-sub-desktop STATUS: 500 MESSAGE: (yukarıdaki metin) SERVLET: org.apache.pulsar.websocket.WebSocketConsumerServlet-39c85c1a Powered by Jetty:// 9.4.54.v20240208
```

2. ve 3. denemede aynı metin geldi; yalnızca `reqId` ve yerel bağlantı noktası değişti. Çalıştırma 1–2'de satırlar kırpıldığından (`...Failed to subscribe persistent://nj3***jj/out/even`) bu metin onlarda görünmez; `-sub-desktop` için aynı HTTP 500 durumu üç kayıtlı denemede de alındı.

Tek seferlik, kaydı tutulmayan ek tanılar (ayrı satır içi betikle, yalnızca bağlanma): `-sub-desktop` için bir denemede 5 sn el sıkışma zaman aşımı, tekrarında HTTP 500; düz `desktop` adı HTTP 500 (aynı "Subscription does not exist"); `{accessId}-desktop` bir kez zaman aşımına uğradı, tekrar denenmedi. Bunlar belge dayanağı değildir, yalnızca bilgi içindir.

Çalıştırma 1 ve 2'de A/B hiçbir mesaj almadı (toplam 0). Aynı abonelikte "iki tüketiciye de aynı mesajlar geliyor mu" sorusu bu yüzden ölçülemedi.

**300 sn olayı (gözlem).** Çalıştırma 1'de A ve B'nin ikisinde de, bağlantıdan tam 300,0 sn sonra (09:28:21.98) `ws.recv()` boş dize döndürdü ve `json.loads` `JSONDecodeError: Expecting value: line 1 column 1 (char 0)` verdi; betik soketi kendisi kapatıp 5 sn bekledi, sonra bitti. Çalıştırma 1'de araç, gelen çerçevenin türünü (kapatma çerçevesi mi, boş veri çerçevesi mi) yazmıyordu; bu nedenle ayırt edilemiyor. Çalıştırma 2'de A (30 sn'de bir ping) 300 sn'lik işareti ~1,5 sn geçene kadar açık kaldı ve kopma yazılmadı.

### Sonuç

1. **Ayrı abonelik adı çalışmıyor (kesin; gözlem).** `{accessId}-sub-desktop` altı denemede (çalıştırma 2'de 3, tekrarda 3) HTTP 500 aldı; tekrarın tam metni yukarıda: `Failed to subscribe persistent://nj3***jj/out/event with 11 partitions {"errorMsg":"Subscription does not exist"...}`. Masaüstünün kendi aboneliğiyle akışın ayrı bir kopyasını alması bu projede mümkün değil. (Mekanizma yorumu: büyük olasılıkla mesaj servisi yalnızca önceden oluşturulmuş `{accessId}-sub` aboneliğine izin veriyor, yeni abonelik açmıyor; sunucu yapılandırması doğrulanmadı.)
2. **Aynı abonelikte iki tüketici: sonuçsuz (ölçülemedi).** İki tüketici aynı abonelikte sorunsuz bağlanıyor (Failover bunu kabul ediyor), ancak 10 dakika boyunca hiç mesaj gelmedi. Bunun nedeni ölçümden ayırt edilemiyor: priz o sırada rapor yollamıyor olabilir ya da ilgili bölümün etkin tüketicisi telefon olabilir. Tasarım açısından bunun ölçülmesi gerekmiyor; aşağıdaki gerekçeye bakın.
3. **Beklenen davranış (Pulsar tasarım bilgisi; denemede gözlenmedi):** hata metni konunun 11 bölümlü olduğunu gösteriyor (`with 11 partitions`); `Failover` aboneliğinde her bölümün aynı anda tek etkin tüketicisi vardır, mesaj o tüketiciye gider, diğerleri beklemede kalır. Yani aynı abonelikte iki cihazın her mesajı birlikte alması beklenmez; masaüstü bağlanırsa bazı bölümlerin (dolayısıyla bazı cihazların) canlı mesajlarını telefondan alabilir.
4. **Yan bulgu (yorum, doğrulanmadı): boşta bağlantı ≈300 sn'de düşebilir.** Ping göndermeyen iki bağlantıda da 300,0 sn'de boş çerçeve geldi (yukarıdaki gözlem); bu, sunucunun boşta zaman aşımıyla uyumlu (Pulsar WebSocket servisinin varsayılan boşta zaman aşımı 300 sn olarak bilinir, burada doğrulanmadı). 30 sn'de bir ping atan bağlantı 300 sn'i yalnızca ~1,5 sn aştı; bu zayıf bir kanıt. Telefonun `TuyaSocket` (OkHttp `pingInterval(30 sn)`) zaten 30 sn'de bir ping atıyor; masaüstü aynı sınıfı değişmeden kullandığı için ek iş öngörülmüyor.
5. `Connection-Id` başlığı denemede tüketici başına farklıydı (`probe-A`, `probe-B`); telefon `switchguard` yolluyor. Aynı `Connection-Id` ile iki bağlantının birbirini düşürüp düşürmediği denenmedi.

### Tasarıma etkisi

Ayrı abonelik çalışmadığı için spec §6'daki geri dönüş kuralı geçerli: Tuya canlı bildirimi masaüstünde **varsayılan kapalı** (`tuyaLive = false`, telefonun canlı bağlantısını çalmasın); kapalıyken Tuya her **5 dk'da bir sorgulanır**. Ayarlar'da "Tuya canlı bildirimleri bu cihazda" anahtarı olur; kullanıcı açarsa bunun telefonun canlı mesajlarının bir kısmını (veya tamamını) alabileceği uyarısı gösterilir. "Masaüstü kendi aboneliğiyle canlı, varsayılan açık" yolu **uygulanmaz**. Spec §6, §8 (Ayarlar) ve §14 buna göre güncellendi.

### Tekrar (isteğe bağlı, 2. aşama)

Aynı abonelikte iki tüketicinin mesaj paylaşımını ölçmek istenirse: priz yükteyken (ör. kullanıcı prize bağlı cihazı açıp kapatarak rapor üretirken) iki süreç `python desktop/tools/pulsar_consumers.py --label A --seconds 300` ve `--label B` ile birlikte çalıştırılır; hangi etiketin hangi mesajı aldığı `dataId` ile karşılaştırılır. Tasarım kararını değiştirmesi beklenmez.

## Deneme A — beklemede (yedek eWeLink hesabının geliştirici onayı bekleniyor)

eWeLink geliştirici konsolunun `http://127.0.0.1:<port>/` biçiminde bir dönüş adresini kabul edip etmediği, yedek eWeLink hesabının geliştirici onayı (ve kendi App ID / App Secret'ı) gelince denenecek. 2026-10-06 itibarıyla onay bekleniyor; bu yüzden deneme 2. aşamanın başına ertelendi (Task 13). Sonuç gelene kadar spec §6'daki yedek yol (dönüş adresi telefondaki gibi `https://127.0.0.1`, pano izleyen giriş) geçerlidir.
