# SwitchGuard – eWeLink (SONOFF) kurulum rehberi

[English](ewelink.en.md) · [Tuya / Smart Life rehberi](tuya.tr.md)

Bu rehber, **eWeLink** uygulamasıyla kontrol ettiğiniz SONOFF ve diğer eWeLink uyumlu cihazları SwitchGuard'a bağlamayı adım adım anlatır. Yaklaşık **2–5 dakika** sürer ve ücretsizdir.

SwitchGuard cihazlarınıza eWeLink'in resmi API'si üzerinden bağlanır. Bunun için eWeLink geliştirici sitesinde **size ait ücretsiz bir uygulama** (App ID + App Secret) oluşturacaksınız. Girişinizi eWeLink'in kendi sayfasında yaparsınız; SwitchGuard şifrenizi hiçbir zaman görmez.

## Videolu anlatım (3 dk)

Tüm adımları sesli anlatımlı bir videoda izleyin: [YouTube'da aç](https://youtu.be/G5vQ_7CXTcM)

[![SwitchGuard – eWeLink hesabını bağlama](https://img.youtube.com/vi/G5vQ_7CXTcM/hqdefault.jpg)](https://youtu.be/G5vQ_7CXTcM)

> **İhtiyacınız olanlar:** cihazlarınızın bulunduğu eWeLink hesabı, SwitchGuard. Geliştirici sitesi için bilgisayar daha rahattır ama telefonla da yapılabilir.

---

## 1. eWeLink geliştirici sitesine girin

1. **https://dev.ewelink.cc** adresini açın.
2. Sağ üstteki **Login/Register**'a basın ve **cihazlarınızın bulunduğu eWeLink hesabıyla** giriş yapın (eWeLink uygulamasındaki e-posta/telefon ve şifre).

![eWeLink Developer Center ana sayfa](img/ewelink-01-home.jpg)

3. Giriş yapınca üst menüde **Console** belirir; ona basın.

![Üst menüde Console](img/ewelink-02-console-link.jpg)

### Hesabınız henüz geliştirici değilse (yeni hesaplar)

Yeni açılan eWeLink hesapları **geliştirici olarak kayıtlı gelmez**. Üst menüdeki **My center → Account Settings** sayfasında *Account Identity: Ordinary users* ve *Certification Status: Unverified* görürsünüz; bu durumda Console'da uygulama oluşturamazsınız.

![Geliştirici olmayan hesap](img/ewelink-00-not-developer.jpg)

1. **Not a developer yet? Apply now!** bağlantısına basın.
2. **Individual developers** (bireysel geliştirici) seçeneğini seçin.

![Hesap türü seçimi](img/ewelink-00-account-type.jpg)

3. Formu doldurun: **Name** (ad soyad), **E-mail**, **Occupation** (meslek) → **Submit**.

![Bireysel geliştirici başvuru formu](img/ewelink-00-apply-form.jpg)

4. **Başvurunuzu eWeLink'in onaylaması gerekir.** Onay gelene kadar 2. adıma geçemezsiniz; onaylanınca hesabınız geliştirici olur ve Console'da uygulama oluşturabilirsiniz.

> **Ek cihazlar (tablet, TV) için açtığınız her eWeLink hesabı da bu başvuruyu yapmalı ve onay almalıdır.** API anahtarları (App ID ve App Secret) o hesabın geliştirici sitesinden alınır; her cihaz kendi hesabının anahtarlarını kullanır (bkz. [Birden fazla cihazda kullanım](#birden-fazla-cihazda-kullanım-telefon-tablet-android-tv)).

## 2. Uygulama oluşturun

1. **App List** sayfasında sağ üstteki **Create** düğmesine basın.

![App List](img/ewelink-03-app-list.jpg)

2. Açılan formu şöyle doldurun:

| Alan | Ne yazılacak |
|---|---|
| **App Name** | `SwitchGuard` (istediğiniz bir ad) |
| **App Profile** | kısa bir açıklama, ör. `SwitchGuard alarm` |
| **App Type** | **OAuth2.0** |
| **App Role** | **Standard Role** (ücretsiz) |
| **Redirect URL** | `https://127.0.0.1` — **birebir aynı** olmalı (sonunda `/` yok, `https`) |

![Uygulama formu](img/ewelink-04-app-form.jpg)

3. **OK**'e basın. Uygulama **App List**'te görünür.

> **Redirect URL çok önemli:** SwitchGuard'daki Redirect URL ile buradaki birebir aynı olmazsa giriş sayfası hata verir. SwitchGuard'daki varsayılan değer `https://127.0.0.1`'dir; kurulum ekranındaki **Redirect URL'yi kopyala** düğmesiyle kopyalayabilirsiniz.
>
> **Create'e basınca bir şey olmuyorsa:** ücretsiz standart rolde tek uygulama oluşturulabiliyor görünüyor. Listede zaten bir uygulamanız varsa onu kullanın; **Edit** ile Redirect URL'sini `https://127.0.0.1` yapın.

## 3. App ID ve App Secret'ı alın

1. **App List**'te uygulamanızın satırındaki **View**'a basın.
2. **Authorization Key** bölümünde **APPID**'yi kopyalayın.
3. **APP SECRET** yanındaki **göz** simgesine basıp görünür yapın ve kopyalayın.

![View App – APPID ve APP SECRET](img/ewelink-05-view-keys.jpg)

> Bu iki bilgi bir şifre gibidir; kimseyle paylaşmayın.
>
> **Uygulamanın süresi 1 yıldır.** **Expiration Date** tarihinden önce geliştirici sitesinden yenileyin ya da yeni bir uygulama oluşturup SwitchGuard'daki App ID/Secret'ı güncelleyin (Ayarlar → Hesaplar → API bilgileri).

## 4. SwitchGuard'a girin

**İlk kurulumda:** **Cihazlarınız nerede?** adımında **eWeLink**'i seçin → **İleri**.

| eWeLink'i seçin | Geliştirici uygulaması adımı |
|---|---|
| ![](img/app-tr-platform-ewelink.jpg) | ![](img/app-tr-ew-developer.jpg) |

Bu adımdaki **dev.ewelink.cc'yi aç** düğmesi siteyi açar, **Redirect URL'yi kopyala** da `https://127.0.0.1`'i panoya kopyalar.

**İleri** → **API bilgilerinizi girin** (altındaki **Her cihaz için ayrı bir eWeLink hesabı gereklidir** uyarısına dikkat edin, bkz. [Birden fazla cihazda kullanım](#birden-fazla-cihazda-kullanım-telefon-tablet-android-tv)):

1. **App ID:** 3. adımda kopyaladığınız APPID'yi yapıştırın.
2. **App Secret:** App Secret'ı yapıştırın.
3. **Redirect URL:** geliştirici sitesine yazdığınızla birebir aynı olmalı (varsayılan `https://127.0.0.1`).

**İleri** → **eWeLink'e giriş yapın** → **eWeLink ile giriş yap**'a basın. eWeLink'in kendi giriş sayfası açılır; e-posta/telefon ve şifrenizle girin. Başarılı olunca **"Giriş yapıldı"** yazar.

| API bilgileri | Giriş |
|---|---|
| ![](img/app-tr-ew-credentials.jpg) | ![](img/app-tr-ew-login.jpg) |

**İleri** → izinleri verin (bildirim, arka planda çalışma) → **İzlemeyi başlat**.

**Sonradan değiştirmek için:** **Ayarlar → Hesaplar → API bilgileri** (App ID/Secret) ve **Yeniden giriş yap**.

![Ayarlar – hesaplar](img/app-tr-settings-accounts.jpg)

---

## Birden fazla cihazda kullanım (telefon, tablet, Android TV)

- **Her cihaz için ayrı bir eWeLink hesabı gerekir. O hesapta geliştirici özelliği açılmalı (eWeLink onayı) ve o cihaza, o hesabın kendi API anahtarları (App ID ve App Secret) girilmelidir.** Cihaza girilen API anahtarları, o cihazda giriş yapılan eWeLink hesabına ait olmalıdır.
- **Aynı API bilgileriyle (App ID / App Secret) yalnızca bir cihazdan giriş yapılabilir.** eWeLink her hesap için tek oturuma izin verir: **en son hangi cihaza API bilgileri girilip giriş yapıldıysa oturum o cihazda aktif kalır**, diğer cihazın oturumu kapanır (SwitchGuard bunu yüksek sesli bir uyarıyla bildirir).
- Her ek cihaz için: 1) **eWeLink uygulamasından** yeni bir hesap açın (eWeLink'in giriş sayfasında kayıt seçeneği yoktur), 2) cihazlarınızı eWeLink uygulamasında **cihaz → Paylaş** ile o hesapla paylaşın, 3) o hesapla dev.ewelink.cc'de [geliştirici başvurusu](#hesabınız-henüz-geliştirici-değilse-yeni-hesaplar) yapıp onay bekleyin, 4) onaydan sonra o hesapta uygulama oluşturup App ID ve App Secret'ı alın ve o cihaza girin.
- **Kurulumu aktarma:** Telefonda **Ayarlar → Yedekleme ve aktarım → Başka cihaza aktar**, TV/tablette kurulum sırasında **Başka bir cihazdan aktar** ya da **Ayarlar → Yedekleme ve aktarım → Başka cihazdan al**. İki cihaz aynı Wi-Fi'da olmalı; TV'de görünen 6 haneli kodu telefona girersiniz. Tuya bilgileri, kurallar, otomasyonlar ve ayarlar ev ağınız içinde, kodla şifrelenerek aktarılır. eWeLink API anahtarları ve oturumu aktarılmaz — her cihaz kendi hesabının anahtarlarını kullanır.
- **TV'de giriş:** en kolayı, eWeLink giriş sayfasının altındaki **Login by QR code** seçeneğidir — QR kodu, o cihaz için açtığınız hesapla giriş yapılmış eWeLink uygulamasıyla okutun. Kumandayla da giriş yapılabilir; telefonunuzdaki Google TV uygulamasının klavyesiyle yazmak daha kolaydır.
- **SwitchGuard Pro** bir kez satın alınır ve aynı Google hesabıyla tüm cihazlarınızda (telefon, tablet, TV) geçerlidir.
- **TV'de uzun anahtarları yazmak:** API bilgileri penceresinde **Telefondan gir**'e basın, QR kodu telefonla okutun ve anahtarları telefonda yazın; ev Wi-Fi'ınız üzerinden şifreli olarak TV'ye gider.
- **Android TV'de alarm:** alarm, o an açık olan her şeyin (YouTube, kanal…) üstüne tam ekran çıkar ve kumandanın OK tuşuyla susturulur.

## Sorun giderme

| Belirti | Çözüm |
|---|---|
| Giriş sayfası "redirect" / "invalid" hatası veriyor | Redirect URL iki tarafta **birebir** aynı mı? (`https://127.0.0.1`, sonda `/` yok, `http` değil `https`.) |
| Giriş başarılı ama cihaz yok | Geliştirici sitesine ve SwitchGuard'a **cihazların bulunduğu hesapla** mı girdiniz? Bölge doğru mu? |
| Başka bir telefonda aynı hesapla giriş yapınca ilk telefon çıkış yapıyor ("İzleme durdu — oturum kapandı" uyarısı) | eWeLink her hesap için tek oturuma izin verir. İkinci telefon için **ayrı bir eWeLink hesabı** açın ve cihazları eWeLink uygulamasından o hesapla **paylaşın**. |
| Durum kartı **"İzleniyor (periyodik)"** kalıyor | Bazı App ID'lerde anlık bağlantı kapalı olabilir; SwitchGuard bu durumda düzenli kontrolle izlemeye devam eder. **Ayarlar → İzleme → Yedek kontrol aralığı** ile sıklığı ayarlayabilirsiniz. |

---

Sorunuz mu var? **switchguardapp@gmail.com** · [GitHub Issues](https://github.com/Macerce/switchguard/issues)
