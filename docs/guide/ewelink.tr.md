# SwitchGuard – eWeLink (SONOFF) kurulum rehberi

[English](ewelink.en.md) · [Tuya / Smart Life rehberi](tuya.tr.md)

Bu rehber, **eWeLink** uygulamasıyla kontrol ettiğiniz SONOFF ve diğer eWeLink uyumlu cihazları SwitchGuard'a bağlamayı adım adım anlatır. Yaklaşık **2–5 dakika** sürer ve ücretsizdir.

SwitchGuard cihazlarınıza eWeLink'in resmi API'si üzerinden bağlanır. Bunun için eWeLink geliştirici sitesinde **size ait ücretsiz bir uygulama** (App ID + App Secret) oluşturacaksınız. Girişinizi eWeLink'in kendi sayfasında yaparsınız; SwitchGuard şifrenizi hiçbir zaman görmez.

> **İhtiyacınız olanlar:** cihazlarınızın bulunduğu eWeLink hesabı, SwitchGuard. Geliştirici sitesi için bilgisayar daha rahattır ama telefonla da yapılabilir.

---

## 1. eWeLink geliştirici sitesine girin

1. **https://dev.ewelink.cc** adresini açın.
2. Sağ üstteki **Login/Register**'a basın ve **cihazlarınızın bulunduğu eWeLink hesabıyla** giriş yapın (eWeLink uygulamasındaki e-posta/telefon ve şifre).

![eWeLink Developer Center ana sayfa](img/ewelink-01-home.jpg)

## 2. Geliştirici olarak kaydolun

İlk girişte geliştirici rolü sorulur. **Ücretsiz standart rol** yeterlidir; ücretli bir plan seçmeniz gerekmez.

## 3. Uygulama oluşturun

1. Konsolda yeni bir uygulama oluşturun (**Create / Add App**).
2. Alanları şöyle doldurun:

| Alan | Ne yazılacak |
|---|---|
| **App name (Uygulama adı)** | `SwitchGuard` (istediğiniz bir ad) |
| **Redirect URL** | `https://127.0.0.1` — **birebir aynı** olmalı (sonunda `/` yok) |

3. Kaydedin. Uygulamanızın **App ID** ve **App Secret** bilgileri görünür.

> **Redirect URL çok önemli:** SwitchGuard'daki Redirect URL ile geliştirici sitesindeki birebir aynı olmazsa giriş sayfası hata verir. SwitchGuard'daki varsayılan değer `https://127.0.0.1`'dir; kurulum ekranındaki **Redirect URL'yi kopyala** düğmesiyle de kopyalayabilirsiniz.

## 4. SwitchGuard'a girin

**İlk kurulumda:** **Cihazlarınız nerede?** adımında **eWeLink**'i seçin → **İleri**.

| eWeLink'i seçin | Geliştirici uygulaması adımı |
|---|---|
| ![](img/app-tr-platform-ewelink.jpg) | ![](img/app-tr-ew-developer.jpg) |

Bu adımdaki **dev.ewelink.cc'yi aç** düğmesi siteyi açar, **Redirect URL'yi kopyala** da `https://127.0.0.1`'i panoya kopyalar.

**İleri** → **API bilgilerinizi girin**:

1. **App ID:** 3. adımda aldığınız App ID'yi yapıştırın.
2. **App Secret:** App Secret'ı yapıştırın.
3. **Redirect URL:** geliştirici sitesine yazdığınızla birebir aynı olmalı (varsayılan `https://127.0.0.1`).

**İleri** → **eWeLink'e giriş yapın** → **eWeLink ile giriş yap**'a basın. eWeLink'in kendi giriş sayfası açılır; e-posta/telefon ve şifrenizle girin. Başarılı olunca **"Giriş yapıldı"** yazar.

| API bilgileri | Giriş |
|---|---|
| ![](img/app-tr-ew-credentials.jpg) | ![](img/app-tr-ew-login.jpg) |

**İleri** → izinleri verin (bildirim, arka planda çalışma) → **İzlemeyi başlat**.

**Sonradan değiştirmek için:** **Ayarlar → Hesap → API bilgileri** (App ID/Secret) ve **Yeniden giriş yap**.

![Ayarlar – hesaplar](img/app-tr-settings-accounts.jpg)

---

## Sorun giderme

| Belirti | Çözüm |
|---|---|
| Giriş sayfası "redirect" / "invalid" hatası veriyor | Redirect URL iki tarafta **birebir** aynı mı? (`https://127.0.0.1`, sonda `/` yok, `http` değil `https`.) |
| Giriş başarılı ama cihaz yok | Geliştirici sitesine ve SwitchGuard'a **cihazların bulunduğu hesapla** mı girdiniz? Bölge doğru mu? |
| Başka bir telefonda aynı hesapla giriş yapınca ilk telefon çıkış yapıyor ("İzleme durdu — oturum kapandı" uyarısı) | eWeLink her hesap için tek oturuma izin verir. İkinci telefon için **ayrı bir eWeLink hesabı** açın ve cihazları eWeLink uygulamasından o hesapla **paylaşın**. |
| Durum kartı **"İzleniyor (periyodik)"** kalıyor | Bazı App ID'lerde anlık bağlantı kapalı olabilir; SwitchGuard bu durumda düzenli kontrolle izlemeye devam eder. Ayarlar'daki **Yedek kontrol aralığı** ile sıklığı ayarlayabilirsiniz. |

---

Sorunuz mu var? **switchguardapp@gmail.com** · [GitHub Issues](https://github.com/Macerce/switchguard/issues)
