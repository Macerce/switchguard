[English](README.md) · [Türkçe](README.tr.md) · [Deutsch](README.de.md) · [Español](README.es.md) · [Português (Brasil)](README.pt-BR.md) · [Français](README.fr.md) · [Italiano](README.it.md) · [Nederlands](README.nl.md) · [Polski](README.pl.md) · [Русский](README.ru.md) · [Українська](README.uk.md) · [العربية](README.ar.md) · [हिन्दी](README.hi.md) · [Bahasa Indonesia](README.id.md) · **Tiếng Việt** · [ไทย](README.th.md) · [简体中文](README.zh-CN.md) · [繁體中文](README.zh-TW.md) · [日本語](README.ja.md) · [한국어](README.ko.md)

# SwitchGuard

SwitchGuard theo dõi ổ cắm và công tắc thông minh trong chính tài khoản **eWeLink / SONOFF** và **Tuya / Smart Life** của bạn, và phát báo động thật ngay khi có thay đổi — máy bơm ngừng chạy, tủ đông bị ngắt, thiết bị mất kết nối. Ứng dụng chạy trên điện thoại, máy tính bảng Android, Android TV và đồng hồ Wear OS; ngoài ra còn có ứng dụng Windows.

<p>
  <img src="docs/screenshots/play/en/v215/01-devices.png" width="200" alt="Thiết bị">
  <img src="docs/screenshots/play/en/v215/02-energy.png" width="200" alt="Năng lượng">
  <img src="docs/screenshots/play/en/v215/05-automations.png" width="200" alt="Tự động hóa">
  <img src="docs/screenshots/play/en/v215/08-alarm.png" width="200" alt="Báo động thiết bị">
</p>

> **Ngôn ngữ:** ứng dụng Windows có 20 ngôn ngữ từ bản 0.13.0. Trên Android, chọn ngôn ngữ trong ứng dụng (20 ngôn ngữ) sẽ có ở bản **2.26**; bản Android mới nhất là 2.25.0.

## Tải về

### Android (điện thoại, máy tính bảng, TV)
- **Google Play**: https://play.google.com/store/apps/details?id=com.macerce.switchguard
- **APK**: [bản Android mới nhất](https://github.com/Macerce/switchguard/releases/latest) — tải `SwitchGuard-x.y.z.apk` và mở trên điện thoại. Nếu SwitchGuard đã được cài từ Google Play, hãy gỡ nó trước (chữ ký khác nhau). Bản GitHub sẽ báo cho bạn khi có bản mới hơn.

### Windows 10 / 11
- **Trình cài đặt**: [các bản Windows](https://github.com/Macerce/switchguard/releases?q=Windows&expanded=true) — tải `SwitchGuard-x.y.z-Kurulum.exe` của bản mới nhất rồi chạy. Ứng dụng tự tìm bản mới và cập nhật chỉ với một cú nhấp; cài đặt được giữ nguyên.

### Miễn phí và Pro
- Android: hoàn toàn miễn phí — mọi tính năng, theo dõi mọi thiết bị, không cần mua. Thiết bị demo cho phép thử mọi thứ mà không cần phần cứng.
- Windows miễn phí: mọi tính năng, cho 1 thiết bị được theo dõi.
- Windows: khóa giấy phép Pro (Cài đặt → thẻ gói) theo dõi mọi thiết bị. Để nhận khóa, liên hệ **switchguardapp@gmail.com**

## Yêu cầu

- Android 8.0+ (API 26); Wear OS cho ứng dụng đồng hồ
- eWeLink: một ứng dụng nhà phát triển eWeLink miễn phí (App ID + App Secret) — hướng dẫn trong ứng dụng chỉ từng bước. eWeLink chỉ cho một phiên trên mỗi tài khoản và App ID, nên mỗi điện thoại, máy tính bảng, TV hoặc PC cần tài khoản eWeLink riêng được chia sẻ thiết bị ([dev.ewelink.cc](https://dev.ewelink.cc))
- Tuya / Smart Life: Access ID và Access Secret của dự án Tuya Cloud miễn phí của riêng bạn, đã liên kết với tài khoản ứng dụng Smart Life / Tuya ([platform.tuya.com](https://platform.tuya.com))
- Windows 10 / 11 (64-bit) cho ứng dụng Windows

## Hướng dẫn cài đặt

- eWeLink (SONOFF): [English](docs/guide/ewelink.en.md) · [Türkçe](docs/guide/ewelink.tr.md)
- Tuya / Smart Life: [English](docs/guide/tuya.en.md) · [Türkçe](docs/guide/tuya.tr.md)

### Video

- **New in 2.17 – alert categories, critical alerts, groups (EN):** https://youtube.com/shorts/iVM3zEiBt8A
- **2.17 yenilikleri – uyarı kategorileri, kritik uyarılar, gruplar (TR):** https://youtube.com/shorts/Qha83ziwylI
- New in 2.15 – smart alerts, scenes, energy reports (EN): https://youtube.com/shorts/bOtFBjHoefE
- 2.15 yenilikleri – akıllı uyarılar, sahneler, enerji raporu (TR): https://youtube.com/shorts/y3QRZvVoeK4
- New in 2.12 – a tour of the app (EN): https://youtube.com/shorts/jvm0hx0gATk
- 2.12 yenilikleri – uygulama turu (TR): https://youtube.com/shorts/2KeUgJrCCEU
- First setup in one minute / İlk kurulum (1 dk): https://youtube.com/shorts/jarBmMg2kYA
- **Getting eWeLink API keys / eWeLink API anahtarlarını alma (TR, sesli):** https://youtu.be/G5vQ_7CXTcM
- **Getting Tuya API keys / Tuya API anahtarlarını alma (TR, sesli):** https://youtu.be/8vPjJMoMaMU

## Tính năng

### Theo dõi và báo động

- Cập nhật trực tiếp qua WebSocket của eWeLink và dịch vụ tin nhắn Tuya, có đồng bộ lại HTTP và kiểm tra định kỳ làm dự phòng.
- Báo động kêu cho đến khi bạn tắt — kể cả ở chế độ im lặng — và mở toàn màn hình trên màn hình khóa; mỗi thiết bị có thể có âm thanh riêng.
- Quy tắc theo thiết bị: báo động, thông báo hoặc không gì cả khi thiết bị tắt, bật, ngoại tuyến hoặc trực tuyến trở lại.
- Cảnh báo có điều kiện: kêu khi thiết bị bật hoặc tắt trong một lúc, hoặc công suất vượt / tụt dưới một ngưỡng (vd. máy bơm dưới 50 W).
- Bỏ qua mất kết nối ngắn, cảnh báo khi chính việc theo dõi dừng (mất internet / đám mây), cảnh báo lớn khi mất phiên eWeLink (vd. cùng tài khoản đăng nhập trên điện thoại khác).
- Thẻ sẵn sàng hiển thị và sửa các cài đặt điện thoại có thể chặn báo động (hạn chế pin, quyền toàn màn hình, Không làm phiền, âm lượng báo thức…).

### Thiết bị

- Ô kiểu nhà thông minh với nút bật/tắt nhanh, mỗi kênh một nút trên thiết bị nhiều kênh, trạng thái và công suất trực tiếp.
- Trang thiết bị: quy tắc, âm báo động riêng, danh mục cảnh báo, lịch sử năng lượng và chi phí, hẹn giờ eWeLink, tín hiệu Wi-Fi và chẩn đoán mất kết nối, điện năng chờ và sự kiện gần đây.
- **Sắp xếp thiết bị**: Ngôi sao = giữ ở trên cùng · mũi tên = di chuyển · kích thước thẻ = nhỏ hoặc lớn · nhãn = danh mục. Nhấn giữ một thẻ để quay lại đây.

### Cảnh báo

- Cảnh báo thiết bị và cảnh báo có điều kiện ở một nơi, lọc theo danh mục cảnh báo (Rất quan trọng / Quan trọng / Bình thường).
- Cài đặt cảnh báo: âm và âm lượng báo động, giờ yên tĩnh, danh mục cảnh báo, Telegram ("Báo cho người khác") và cảnh báo thông minh — xem *Tất cả cài đặt* bên dưới.
- Cảnh báo thông minh: một cảnh báo duy nhất khi mất điện, tiêu thụ bất thường, ngân sách tháng, báo cáo tuần, chế độ kỳ nghỉ.

### Tự động hóa

- **Quy tắc** — hành động tự động: *Nếu* có chuyện xảy ra, *thì* làm gì đó, có độ trễ tùy chọn.
- Điều kiện kích hoạt: bật, tắt, vẫn bật trong, vẫn tắt trong, chuyển sang ngoại tuyến, trực tuyến trở lại, công suất vượt quá, công suất giảm xuống dưới, lúc mặt trời mọc, lúc mặt trời lặn, khi nhiệt độ ngoài trời tăng lên trên, khi nhiệt độ ngoài trời giảm xuống dưới, khi đến, khi rời đi.
- Hành động: Bật, Tắt, Phát báo động, Gửi thông báo, Chạy kịch bản, Bắt đầu chu kỳ, Dừng chu kỳ.
- **Chỉ khi**: Chỉ trong những giờ này, Và chỉ khi một thiết bị khác…, Chỉ khi ở nhà / vắng nhà.
- **Kịch bản** — Chọn điều gì xảy ra với từng thiết bị khi kịch bản chạy. Các thiết bị để ở “Bỏ qua” sẽ không bị tác động.
- **Nhóm** — Chọn các thiết bị trong nhóm. Nhóm hiện trên màn hình Thiết bị như một thiết bị: nút của nó bật hoặc tắt tất cả cùng lúc, và bạn có thể dùng nó trong tự động hóa.
- **Chu kỳ & hẹn giờ** — Đếm ngược, Bật rồi tắt, Chu kỳ bật/tắt, Lần lượt từng cái, Các bước tùy chỉnh. Bắt đầu bằng nút, vào giờ cố định ngày đã chọn hoặc trong một khung giờ; tùy chọn dao động ngẫu nhiên (như có người ở nhà) và "dừng khi đổi bằng tay".
- Tự động hóa chạy trên điện thoại này khi giám sát đang bật. Với lịch phải hoạt động cả khi điện thoại tắt, hãy dùng hẹn giờ trên trang thiết bị.

### Năng lượng

- Xem theo ngày / tháng / năm: tổng, chi phí ước tính, công suất trung bình và đỉnh, giờ/ngày/tháng dùng nhiều nhất và tóm tắt điểm nổi bật.
- Biểu giá một mức hoặc ba mức (ngày / cao điểm / đêm), giá cho từng thiết bị hoặc giá mặc định.
- Điện năng chờ (công suất mà thiết bị không bao giờ xuống thấp hơn) và chi phí hằng tháng.
- Báo cáo năng lượng dạng PDF (in hoặc chia sẻ) hoặc CSV (Excel, Google Trang tính).
- Mức tiêu thụ do SwitchGuard tính từ các số đo công suất được lấy khi giám sát đang bật, nên có thể hơi khác so với ứng dụng của nhà sản xuất thiết bị (eWeLink, Smart Life).

### Lịch sử

- Mọi sự kiện kèm ngày giờ và ai hay cái gì gây ra (ứng dụng, tự động hóa); lọc theo thiết bị.
- Xuất CSV (24 giờ, 7 ngày, 30 ngày gần nhất hoặc tất cả) và xóa lịch sử.

### Trò chuyện

- Xin chào. Bạn có thể quản lý thiết bị bằng cách viết: bật hoặc tắt chúng, hỏi trạng thái, mức dùng điện hoặc nhiệt độ ngoài trời (bằng tiếng Anh hoặc tiếng Thổ Nhĩ Kỳ). Chạm vào micro để nói thay vì viết (“Turn off kitchen”); câu trả lời sẽ được đọc to. Các mẫu làm sẵn nằm bên dưới; chạm vào một mẫu để gửi, giữ để sửa.
- Lệnh văn bản tự do hoạt động không cần internet và hiểu tiếng Anh và tiếng Thổ Nhĩ Kỳ; micro dùng nhận dạng giọng nói của điện thoại và câu trả lời có thể được đọc to.
- Mẫu: tin nhắn một chạm, cũng có thể tự chạy mỗi ngày vào một giờ hoặc khi có chuyện xảy ra.

### Màn hình chờ

- Bảng toàn màn hình cho điện thoại hoặc máy tính bảng đang sạc: đồng hồ, thời tiết, trạng thái quan trọng, công suất trực tiếp, hoạt động gần đây và những gì sắp chạy. Nó cũng hiển thị trên màn hình khóa.
- Mở cài đặt Android: chọn SwitchGuard trong Trình bảo vệ màn hình và đặt để bắt đầu khi đang sạc. Khi đó bảng này thay thế màn hình khóa lúc cắm sạc.

### Tiện ích, ô cài đặt nhanh, lối tắt

- **SwitchGuard** — Hiển thị việc giám sát có đang chạy không và mọi báo động đang kêu
- **Công tắc SwitchGuard** — Bật và tắt một thiết bị hoặc kênh chỉ bằng một lần chạm. Cũng có thể đặt thành Ở nhà / Vắng nhà hoặc chạy một kịch bản.
- **Nhóm SwitchGuard** — Ô lớn: bật hoặc tắt riêng từng thiết bị trong một nhóm
- **Lệnh giọng nói SwitchGuard** — Một ô duy nhất: chạm, nói (“Turn off kitchen”) và lệnh sẽ chạy
- **Ô cài đặt nhanh** — hiển thị trạng thái theo dõi, bật hoặc dừng theo dõi (phải mở khóa điện thoại trước); khi báo động đang kêu thì mở ứng dụng.
- **Lối tắt ứng dụng** — nhấn giữ biểu tượng ứng dụng để chọn Ở nhà / Vắng nhà và các kịch bản.

### Ở nhà / Vắng nhà

- **Ở nhà / Vắng nhà** — Các quy tắc đánh dấu “chỉ khi vắng nhà” sẽ không chạy. Hãy chuyển sang Vắng nhà khi bạn ra ngoài (cũng có thể từ widget, đồng hồ hoặc lệnh /away trên Telegram).
- **Chế độ kỳ nghỉ** — Bạn được tính là vắng nhà, giờ yên tĩnh tắt và mỗi sáng lúc 09:00 có một báo cáo. Chế độ này tự kết thúc.
- **Khu vực** — Chọn một vùng trên bản đồ; tự động hóa chạy khi điện thoại của bạn vào hoặc rời vùng đó (ví dụ: bật đèn sân vườn khi bạn về nhà, tắt mọi thứ khi bạn rời đi). Hình ảnh bản đồ được tải từ OpenStreetMap; vị trí của bạn chỉ được dùng trên điện thoại này.

### Wear OS

- Ứng dụng đồng hồ SwitchGuard hiển thị báo động (tắt ngay trên cổ tay), thiết bị (bật/tắt), kịch bản và Ở nhà / Vắng nhà; kết nối với ứng dụng trên điện thoại.

### Android TV

- Chạy trên Android TV bằng điều khiển từ xa; báo động hiện đè lên mọi nội dung đang phát ("Hiện báo động trên các ứng dụng khác").
- "Nhập từ điện thoại": TV hiển thị mã QR; bạn nhập khóa trên điện thoại và chúng được mã hóa gửi tới TV qua Wi-Fi nhà.
- "Nhận từ thiết bị khác": sao chép khóa Tuya, quy tắc và cài đặt từ điện thoại qua Wi-Fi.

### Nhiều thiết bị

Điện thoại, máy tính bảng, Android TV và PC: **mỗi thiết bị cần một tài khoản eWeLink riêng đã được duyệt tính năng nhà phát triển, và khóa API riêng của tài khoản đó (App ID / Secret)** — eWeLink chỉ cho một thiết bị đăng nhập trên mỗi tài khoản. Hãy chia sẻ thiết bị cho tài khoản bổ sung trong ứng dụng eWeLink. Khóa Tuya có thể dùng trên nhiều thiết bị. Chi tiết: [English](docs/guide/ewelink.en.md#using-several-devices-phone-tablet-android-tv) · [Türkçe](docs/guide/ewelink.tr.md#birden-fazla-cihazda-kullanım-telefon-tablet-android-tv)

### Demo

- **Thiết bị demo** — Thiết bị ảo để thử mọi tính năng. Các nút này hoạt động như thể có điều gì đó xảy ra ngay tại thiết bị (ai đó bật/tắt, mất điện hoặc mất Wi-Fi), nên các quy tắc của bạn được áp dụng và báo động có thể kêu. Bật/tắt bằng các nút phía trên được tính là do bạn thực hiện và không làm báo động kêu.

## Tất cả cài đặt

Mọi thứ theo đúng thứ tự trong ứng dụng. Thẻ **Cài đặt** chứa kết nối và tùy chọn ứng dụng; báo động, giờ yên tĩnh, Telegram và cảnh báo thông minh nằm ở thẻ **Cảnh báo** → Cài đặt cảnh báo; tùy chọn riêng của từng thiết bị nằm trên trang của nó.

### Cài đặt → Tài khoản

| Cài đặt | Tác dụng |
|---|---|
| eWeLink — Đăng nhập bằng eWeLink | Mở trang đăng nhập riêng của eWeLink; ứng dụng không bao giờ thấy mật khẩu của bạn. |
| Thông tin xác thực API | App ID, App Secret và khu vực của ứng dụng nhà phát triển eWeLink. |
| Đăng xuất | Giám sát sẽ dừng. Cài đặt và lịch sử của bạn được giữ lại. |
| Tuya / Smart Life | Nhập Access ID và Access Secret của dự án Tuya Cloud (platform.tuya.com → Cloud → dự án của bạn → Overview). Tài khoản ứng dụng Smart Life / Tuya của bạn phải được liên kết với dự án. Trung tâm dữ liệu: Trung Âu, Tây Mỹ, Trung Quốc, Ấn Độ |
| Ngắt kết nối | Gỡ kết nối Tuya; thiết bị Tuya không còn được theo dõi. |
| Thiết bị demo | Thiết bị ảo để thử mọi tính năng |

### Cài đặt → Giám sát

Cách SwitchGuard theo dõi thiết bị của bạn trong nền.

| Cài đặt | Tác dụng |
|---|---|
| Độ tin cậy tối đa | Giữ bộ xử lý luôn thức để không sự kiện nào bị trễ. Tốn pin hơn. |
| Bỏ qua mất kết nối ngắn | Chờ khoảng thời gian này trước khi cảnh báo thiết bị ngoại tuyến. Nếu thiết bị kết nối lại kịp thời, sẽ không có cảnh báo. |
| Cảnh báo nếu giám sát dừng | Không có internet hoặc không kết nối được đám mây trong khoảng thời gian này: bạn sẽ nhận cảnh báo |
| Khoảng thời gian kiểm tra dự phòng | Chỉ dùng khi không có cập nhật tức thời. |

### Cài đặt → Quyền

Nếu thiếu các quyền này, Android có thể ngăn báo động đến được với bạn.

| Cài đặt | Tác dụng |
|---|---|
| Thông báo | Không thể hiển thị báo động và cảnh báo. |
| Hoạt động nền | Điện thoại có thể cho giám sát vào chế độ ngủ; báo động có thể trễ hoặc bị bỏ lỡ. |
| Báo động toàn màn hình | Khi điện thoại bị khóa, báo động không thể mở màn hình lớn của nó. |
| Báo động trên màn hình khóa (Xiaomi) | Khi điện thoại bị khóa, màn hình báo động không thể mở. Chạm Khắc phục, mở Quyền khác và bật “Hiển thị trên màn hình khóa” và “Mở cửa sổ mới khi chạy trong nền”. |
| Hiện báo động trên các ứng dụng khác | Cần thiết trên Android TV để báo động hiện lên trên mọi nội dung đang phát. Chạm và cho phép SwitchGuard. |
| Khởi động nền (Xiaomi, Huawei, OPPO, vivo…) | Hiện trên điện thoại chặn khởi chạy nền (Xiaomi, Huawei, OPPO, vivo…): giúp việc theo dõi chạy lại sau khi khởi động lại hoặc cập nhật. Xem Khắc phục sự cố. |

### Cài đặt → Vị trí và khu vực

| Cài đặt | Tác dụng |
|---|---|
| Vị trí | Cần cho các quy tắc mặt trời mọc/lặn và nhiệt độ ngoài trời. Vị trí ở lại trên thiết bị này; với nhiệt độ, vị trí được gửi đến Open-Meteo đã làm tròn khoảng 100 m. |
| Khu vực | Chọn một vùng trên bản đồ; tự động hóa chạy khi điện thoại của bạn vào hoặc rời vùng đó (ví dụ: bật đèn sân vườn khi bạn về nhà, tắt mọi thứ khi bạn rời đi). Hình ảnh bản đồ được tải từ OpenStreetMap; vị trí của bạn chỉ được dùng trên điện thoại này. |

### Cài đặt → Màn hình chờ

| Cài đặt | Tác dụng |
|---|---|
| Mở màn hình chờ | Chạm vào màn hình để có “Mở ứng dụng” và “Đóng” |
| Dùng làm trình bảo vệ màn hình | Mở cài đặt Android: chọn SwitchGuard trong Trình bảo vệ màn hình và đặt để bắt đầu khi đang sạc. Khi đó bảng này thay thế màn hình khóa lúc cắm sạc. |
| Làm mờ vào ban đêm | Chữ mờ dần từ 23:00 đến 07:00 |

### Cài đặt → Tóm tắt hằng ngày

| Cài đặt | Tác dụng |
|---|---|
| Thông báo tóm tắt hằng ngày | Mỗi thiết bị đã bật bao nhiêu lần, bật trong bao lâu và đã dùng bao nhiêu điện năng hôm nay |
| Gửi lúc | Giờ gửi bản tóm tắt. |
| Hiện tóm tắt hôm nay ngay | Hiển thị ngay. |

### Cài đặt → Sao lưu & chuyển

| Cài đặt | Tác dụng |
|---|---|
| Sao lưu ra tệp | Quy tắc, tự động hóa, cài đặt và khóa Tuya, eWeLink được lưu vào một tệp có mật khẩu bảo vệ |
| Khôi phục từ tệp | Chuyển sang điện thoại mới? Chọn tệp sao lưu và nhập mật khẩu của tệp |
| Gửi sang thiết bị khác | Sao chép khóa Tuya, quy tắc và cài đặt sang TV hoặc máy tính bảng trong cùng Wi-Fi |
| Nhận từ thiết bị khác | Nhận thiết lập từ điện thoại của bạn qua Wi-Fi |

Chuyển qua Wi-Fi không bao giờ gửi khóa API eWeLink hay phiên eWeLink: trên thiết bị kia, hãy nhập khóa API riêng của nó và đăng nhập bằng tài khoản eWeLink riêng của nó. Còn bản sao lưu ra tệp thì có kèm khóa eWeLink và Tuya, được bảo vệ bằng mật khẩu của bạn.

### Cài đặt → Ngôn ngữ

| Cài đặt | Tác dụng |
|---|---|
| Ngôn ngữ | Chọn ngôn ngữ của ứng dụng, thông báo và báo động. Ngôn ngữ điện thoại hoặc một trong 20 ngôn ngữ (từ 2.26). |

### Cài đặt → Trợ giúp & giới thiệu

| Cài đặt | Tác dụng |
|---|---|
| Hướng dẫn thiết lập | Hiện lại phần thiết lập lần đầu |
| Hướng dẫn thiết lập eWeLink | Từng bước có hình ảnh (GitHub) |
| Hướng dẫn thiết lập Tuya / Smart Life | Từng bước có hình ảnh (GitHub) |
| Gửi báo cáo sự cố | Có gì đó không ổn? Soạn sẵn một email kèm thông tin chẩn đoán. Sẽ không có gì được chia sẻ nếu bạn không gửi. |
| Xóa các báo cáo sự cố đã lưu | Xóa các báo cáo sự cố đã lưu trên thiết bị này. |
| Liên hệ & phản hồi | Viết email cho nhà phát triển (câu hỏi, lỗi, ý tưởng). |
| Chính sách quyền riêng tư | Mở chính sách quyền riêng tư. |
| Giới thiệu | Phiên bản, thông tin pháp lý. Bản GitHub kiểm tra trên GitHub xem có bản mới hơn không. |

### Cảnh báo

| Cài đặt | Tác dụng |
|---|---|
| Cảnh báo thiết bị | Mỗi thiết bị kêu hoặc thông báo vì điều gì |
| Cảnh báo có điều kiện | Kêu khi thiết bị vẫn bật hoặc tắt trong một thời gian, hoặc khi công suất thay đổi |
| Báo động đã sẵn sàng / Báo động có thể không đến được với bạn | Thẻ sẵn sàng ở trên cùng: liệt kê các cài đặt điện thoại có thể chặn báo động và mở đúng màn hình để sửa; kèm nút "Báo động thử". |

### Cảnh báo → Báo động

| Cài đặt | Tác dụng |
|---|---|
| Âm báo động | Âm báo động chung. Mỗi thiết bị có thể có âm riêng (trang thiết bị). |
| Âm lượng báo động | Âm lượng báo động riêng của SwitchGuard. |
| Rung | Rung khi báo động kêu. |
| Kêu hết âm lượng | Tăng âm lượng báo thức của điện thoại lên tối đa khi báo động kêu và tạm dừng nhạc; được khôi phục khi bạn tắt báo động |
| Kêu lại nếu chưa xử lý | Sau khi bạn tắt báo động, báo động sẽ kêu lại theo khoảng thời gian này nếu thiết bị vẫn ở trạng thái gây ra báo động (vẫn tắt, vẫn bật hoặc ngoại tuyến). Dừng khi thiết bị trở lại bình thường. |
| Báo cho người khác | Cài đặt Telegram (xem bên dưới). |
| Báo động thử | Thử báo động ngay: có âm thanh hoặc im lặng, ngay lập tức hoặc sau 10 giây |

### Cảnh báo → Danh mục cảnh báo

Mỗi thiết bị có một danh mục cảnh báo. Nếu bạn không chọn, nó được đặt tự động: thiết bị được giám sát có bất kỳ sự kiện nào đặt là Báo động thì thuộc loại Rất quan trọng, thiết bị chỉ gửi thông báo thì thuộc loại Quan trọng, còn lại là Bình thường. Cảnh báo có điều kiện và tự động hóa có thể dùng danh mục làm điều kiện kích hoạt (ví dụ: “kêu báo động khi bất kỳ thiết bị rất quan trọng nào ngoại tuyến”).

| Cài đặt | Tác dụng |
|---|---|
| Rất quan trọng · Quan trọng · Bình thường · Tự động | Khi tự động: Rất quan trọng nếu có sự kiện nào kêu báo động, Quan trọng nếu các sự kiện chỉ thông báo |
| Kêu cả trong giờ yên tĩnh | Báo động kêu cả trong giờ yên tĩnh / Trong giờ yên tĩnh, báo động chuyển thành thông báo |

### Cảnh báo → Giờ yên tĩnh

| Cài đặt | Tác dụng |
|---|---|
| Giờ yên tĩnh | Trong khoảng giờ này, báo động chuyển thành thông báo (thiết bị quan trọng vẫn kêu) |
| Bắt đầu / Kết thúc | Giờ bắt đầu và kết thúc giờ yên tĩnh. |

Các thiết bị thuộc danh mục cảnh báo được đặt để kêu trong giờ yên tĩnh (mặc định là Quan trọng) vẫn kêu.

### Cảnh báo → Báo cho người khác

Nếu báo động không được tắt kịp thời, những người bạn chọn sẽ nhận được tin nhắn Telegram, và thêm một tin nữa khi báo động được tắt. Dù điện thoại của bạn ở phòng khác vào ban đêm, người nhà hoặc hàng xóm vẫn biết được. Tin nhắn được gửi bởi bot Telegram do bạn tạo; không có máy chủ nào khác tham gia.

| Cài đặt | Tác dụng |
|---|---|
| 1. Bot Telegram | Trong Telegram, mở @BotFather, gửi /newbot và đặt tên cho bot của bạn. Dán token mà BotFather cung cấp vào bên dưới. |
| 2. Người cần báo | Nhờ mỗi người mở @your_bot trong Telegram và nhấn Start (bạn cũng có thể thêm bot vào nhóm gia đình). Sau đó nhấn Tìm người. |
| 3. Khi nào | Báo động được kêu bao lâu trước khi báo cho mọi người (hoặc ngay lập tức). |
| Gửi tin nhắn thử | Gửi tin nhắn thử tới mọi người trong danh sách. |
| Tắt | Tắt thông báo qua Telegram. |

### Cảnh báo → Cảnh báo thông minh

Các cảnh báo xem xét tất cả thiết bị của bạn cùng lúc: mất điện, tiêu thụ bất thường và tóm tắt hằng tuần. Với Telegram, bạn còn có thể kiểm tra trạng thái, nhận xử lý báo động và bật/tắt thiết bị từ xa.

| Cài đặt | Tác dụng |
|---|---|
| Mất điện: Phát hiện mất điện | Nếu tất cả thiết bị được giám sát cùng chuyển sang ngoại tuyến, bạn nhận một cảnh báo “mất điện” thay vì một cảnh báo cho mỗi thiết bị, và một tin nhắn kèm thời lượng khi có điện trở lại. |
| Bộ sạc làm cảm biến điện | Nếu điện thoại/máy tính bảng này luôn được cắm sạc tại địa điểm, việc bị rút sạc trong một phút được coi là mất điện; cách này cũng hoạt động khi mất internet. Tin nhắn được gửi qua Telegram bằng dữ liệu di động. |
| Ở nhà / Vắng nhà: Cảnh báo nhẹ hơn khi ở nhà | Khi ở nhà, báo động của các thiết bị không quan trọng chuyển thành thông báo (như giờ yên tĩnh). Thiết bị quan trọng luôn kêu. |
| Chế độ kỳ nghỉ | Bạn được tính là vắng nhà, giờ yên tĩnh tắt và mỗi sáng lúc 09:00 có một báo cáo. Chế độ này tự kết thúc. |
| Tiêu thụ: Tiêu thụ bất thường | Mỗi sáng, so sánh hôm qua với hai tuần trước đó. Mức dùng cao hơn nhiều có thể là do sự cố; thấp hơn nhiều (tủ đông hầu như không chạy) có thể có nghĩa là nó đã ngừng hoạt động. |
| Biểu giá điện | Giá mặc định cho các thiết bị không có giá riêng. Với biểu giá ba mức, mỗi giờ được tính theo khung riêng: ban ngày 06–17, cao điểm 17–22, ban đêm 22–06. |
| Ngân sách hằng tháng | Bạn được thông báo một lần mỗi tháng khi chi phí ước tính của các thiết bị đo năng lượng đạt mức này. |
| Báo cáo hằng tuần | Mỗi thứ Hai lúc 09:00: mất điện, báo động và thời gian phản hồi, mất kết nối và tiêu thụ của tuần qua. Cũng được gửi đến Telegram nếu đã thiết lập. |
| Hiện báo cáo tuần này | Hiển thị ngay. |
| Telegram: Lệnh và nút “Đã nhận” | Những người trong danh sách của bạn có thể gửi /status và nhận xử lý báo động đang kêu. Tin nhắn báo động có nút “Đã nhận”: nhấn vào sẽ tắt tiếng điện thoại. |
| Cho phép bật và tắt thiết bị | Lệnh /on và /off. Chỉ những người trong danh sách của bạn mới dùng được. |
| Thông báo theo giai đoạn | Mỗi người được nhắn sau khoảng chờ riêng. Ví dụ hàng xóm sau 5 phút, người thân sau 15 phút, nhóm gia đình sau 30 phút. Nếu ai đó nhấn “Đã nhận”, những người tiếp theo sẽ không được nhắn. |
| Gọi điện khi có báo động quan trọng | Khi một thiết bị thuộc danh mục Quan trọng báo động, người đó cũng được gọi từ đường dây của điện thoại này khi hết thời gian chờ trong cảnh báo theo giai đoạn. Cuộc gọi chỉ để đánh thức họ: chuyện gì đã xảy ra nằm trong tin nhắn Telegram gửi cùng lúc (Android không cho ứng dụng phát âm thanh vào cuộc gọi). Hãy nhờ họ lưu số này là “SwitchGuard alarm”. Cuộc gọi lặp lại mỗi 2 phút, tối đa 3 lần, cho đến khi báo động được tắt hoặc ai đó nhấn “Đã nhận” trong Telegram. Không thể gọi cho nhóm. |
| Gửi cảnh báo thông minh đến Telegram | Mất điện, tiêu thụ bất thường, cảnh báo ngân sách và báo cáo hằng tuần cũng được ghi vào Telegram. |
| Watchdog | Hai thiết bị dùng cùng một bot sẽ theo dõi lẫn nhau (ví dụ điện thoại và TV box). Thiết bị gửi luôn cập nhật một tin nhắn được ghim trong cuộc trò chuyện Telegram; thiết bị theo dõi sẽ báo động và nhắn tin cho danh bạ của bạn nếu nó ngừng. Đừng bỏ ghim tin nhắn đó. |

### Trang thiết bị (chạm vào một thiết bị)

| Cài đặt | Tác dụng |
|---|---|
| Giám sát thiết bị này | Các thay đổi được ghi lại và cảnh báo theo các quy tắc bên dưới |
| Khi điều này xảy ra | Tắt, Bật, Chuyển sang ngoại tuyến, Trực tuyến → Báo động / Thông báo / Không. Báo động kêu cho đến khi bạn tắt. Thông báo hiện một thông báo thường. Không chỉ ghi sự kiện vào lịch sử. |
| Âm thanh cho thiết bị này | Âm báo động riêng; nếu không thì dùng âm chung. |
| Danh mục cảnh báo | Khi tự động: Rất quan trọng nếu có sự kiện nào kêu báo động, Quan trọng nếu các sự kiện chỉ thông báo |
| Năng lượng | Công suất, điện áp, dòng điện; hôm nay và tháng này; lịch sử theo ngày / tháng / năm kèm chi phí; xuất báo cáo (PDF / CSV). |
| Giá mỗi kWh | Giá mỗi kWh cho thiết bị này hoặc giá mặc định; cũng có thể đặt làm mặc định. |
| Hẹn giờ (trên thiết bị) | Chưa có hẹn giờ. Hẹn giờ chạy ngay trên thiết bị, kể cả khi điện thoại của bạn tắt. (Thiết bị eWeLink: thêm hoặc sửa hẹn giờ bật/tắt vào giờ cố định ngày đã chọn.) |
| Kết nối | dBm là cường độ tín hiệu: càng gần 0 càng tốt. Trên -55 là rất tốt, đến -67 là tốt, đến -75 là khá, thấp hơn là yếu. |
| Tiêu thụ ở chế độ chờ | Mức trung bình theo giờ thấp nhất trong 7 ngày qua: công suất mà thiết bị này (và mọi thứ cắm vào nó) không bao giờ xuống dưới, kể cả khi “không làm gì”. TV, modem và bộ sạc thường tiêu thụ mức này khi ở chế độ chờ. |
| Sao chép dữ liệu thiết bị | Sao chép dữ liệu thô của thiết bị vào bộ nhớ tạm, hữu ích để báo cáo model chưa được hỗ trợ |

## Lệnh Telegram

Những người trong danh sách của bạn có thể gửi /status và nhận xử lý báo động đang kêu. Tin nhắn báo động có nút “Đã nhận”: nhấn vào sẽ tắt tiếng điện thoại.

```
Lệnh SwitchGuard:
/status – giám sát, báo động và thiết bị
/ack – nhận xử lý báo động đang kêu
/on tên [kênh] – bật một thiết bị hoặc nhóm (nhiều: /on kitchen, garden)
/off tên [kênh] – tắt một thiết bị hoặc nhóm
/watt [tên] – công suất hiện tại, hôm nay và mức dùng thông thường
/temp – nhiệt độ ngoài trời
/scene tên – chạy một kịch bản
/home · /away – đặt ở nhà / vắng nhà

Bạn cũng có thể viết tự do: “turn off kitchen and garden”, “heater watts”, “is the pump on”.

Việc bật/tắt thiết bị và kịch bản phải được cho phép trong ứng dụng (Cảnh báo → Cảnh báo thông minh).
```

Cả tên lệnh tiếng Anh và tiếng Thổ Nhĩ Kỳ đều dùng được (vd. /status = /durum, /on = /ac). Lệnh chỉ được nhận từ các cuộc trò chuyện bạn đã chọn.

## Quyền và lý do

| Quyền | Lý do |
|---|---|
| `POST_NOTIFICATIONS` | Hiển thị báo động, thông báo và trạng thái theo dõi. |
| `FOREGROUND_SERVICE` | Giữ việc theo dõi chạy nền (thông báo trạng thái cố định). |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Xin miễn tối ưu hóa pin để báo động không bị trễ. |
| `USE_FULL_SCREEN_INTENT` | Mở báo động toàn màn hình trên màn hình khóa. |
| `SYSTEM_ALERT_WINDOW` | Hiện báo động trên các ứng dụng khác (Android TV, Xiaomi). |
| `WAKE_LOCK` | Giữ bộ xử lý thức khi xử lý sự kiện ("Độ tin cậy tối đa"). |
| `VIBRATE` | Rung khi báo động. |
| `RECEIVE_BOOT_COMPLETED` | Chạy lại việc theo dõi sau khi điện thoại khởi động lại. |
| `INTERNET` | Kết nối với đám mây eWeLink / Tuya (và Telegram / Open-Meteo / OpenStreetMap nếu bạn dùng). |
| `ACCESS_COARSE_LOCATION` | Chỉ khi bạn chạm "Dùng vị trí của thiết bị này" cho quy tắc bình minh/hoàng hôn và nhiệt độ. |
| `ACCESS_FINE_LOCATION` | Khu vực: một khu vực chính xác cho quy tắc đến / rời đi. |
| `ACCESS_BACKGROUND_LOCATION` | Khu vực: nhận biết lúc đến và rời đi khi ứng dụng đóng (chỉ hỏi khi bạn tạo khu vực). |
| `CALL_PHONE` | "Gọi điện khi có báo động quan trọng": gọi số bạn đã nhập bằng đường dây của điện thoại này. |

## Quyền riêng tư

- Không có máy chủ riêng, không phân tích, không quảng cáo, không theo dõi. Không bán hay chia sẻ dữ liệu.
- Khóa, token, thiết bị, quy tắc, cài đặt và lịch sử (tối đa 2.000 sự kiện) chỉ lưu trên thiết bị của bạn.
- Ứng dụng chỉ kết nối với các đám mây bạn liên kết (eWeLink `*.coolkit.cc` / `*.coolkit.cn`, Tuya `openapi.tuya*.com`) và, chỉ khi bạn dùng, với Telegram, Open-Meteo (nhiệt độ và thời tiết; vị trí làm tròn khoảng 100 m) và ô bản đồ OpenStreetMap.
- Mật khẩu eWeLink được nhập trên trang của chính eWeLink; ứng dụng không bao giờ thấy nó.
- Dữ liệu ứng dụng không nằm trong sao lưu đám mây của Android; bản sao lưu ra tệp có mật khẩu; chuyển qua Wi-Fi được mã hóa bằng mã dùng một lần và không ra khỏi mạng gia đình.

Chính sách quyền riêng tư đầy đủ: [Google Sites](https://sites.google.com/view/switchguard-privacy) · [docs/privacy-policy.md](docs/privacy-policy.md)

## Khắc phục sự cố và câu hỏi thường gặp

### Báo động đến trễ hoặc không kêu

| Vấn đề | Cách xử lý |
|---|---|
| Hoạt động nền bị hạn chế | Điện thoại có thể cho giám sát vào chế độ ngủ; báo động có thể trễ hoặc bị bỏ lỡ. |
| Chưa cho phép báo động toàn màn hình | Khi điện thoại bị khóa, báo động không thể mở màn hình lớn của nó. |
| Xiaomi chặn màn hình báo động | Khi điện thoại bị khóa, màn hình báo động không thể mở. Chạm Khắc phục, mở Quyền khác và bật “Hiển thị trên màn hình khóa” và “Mở cửa sổ mới khi chạy trong nền”. |
| Thông báo báo động bị hạ mức | Danh mục thông báo “Báo động” chưa được đặt ở mức ưu tiên cao, nên báo động sẽ không bật lên hoặc phủ kín màn hình. |
| Không làm phiền: im lặng hoàn toàn | Ở chế độ này, ngay cả âm báo động cũng bị tắt tiếng. |
| Âm lượng báo động rất thấp | Âm lượng báo thức của điện thoại bằng hoặc gần bằng không, nên bạn có thể không nghe thấy SwitchGuard. Khắc phục sẽ bật “Kêu hết âm lượng”. |
| Thông báo đang tắt | Không thể hiển thị báo động và cảnh báo. |
| Có một SwitchGuard thứ hai trên điện thoại này | Một bản sao khác của SwitchGuard đã được tạo bằng tính năng Ứng dụng kép (nhân bản ứng dụng) của điện thoại. Hai bản chạy song song sẽ làm mỗi báo động kêu hai lần và đăng xuất lẫn nhau khi dùng cùng một tài khoản eWeLink; trên điện thoại Xiaomi, quyền của bản này cũng có thể hiện là đã tắt. Hãy gỡ bản sao thứ hai: chạm và giữ biểu tượng của nó (biểu tượng có huy hiệu nhỏ) rồi chạm Gỡ cài đặt. |

### Hãng điện thoại hay dừng ứng dụng chạy nền

| Điện thoại | Cách xử lý |
|---|---|
| Xiaomi | Cần thiết để giám sát khởi động lại sau khi khởi động lại máy hoặc cập nhật. Chạm và bật Tự khởi động cho SwitchGuard. |
| Samsung | Samsung có thể đưa ứng dụng vào chế độ ngủ. Trong Pin, chọn "Không hạn chế" và thêm SwitchGuard vào "Ứng dụng không bao giờ ngủ" (Pin → Giới hạn sử dụng nền). |
| Huawei / Honor | Trong Khởi chạy ứng dụng, đặt SwitchGuard thành "Quản lý thủ công" và bật Tự khởi chạy, Khởi chạy phụ và Chạy trong nền. |
| OPPO / realme / OnePlus | Bật Tự khởi chạy cho SwitchGuard, sau đó trong cài đặt Pin của ứng dụng, cho phép hoạt động nền. |
| vivo / iQOO | Cho phép SwitchGuard khởi động trong nền và cho phép tiêu thụ điện năng nền cao. |
| Tecno / Infinix | Bật Tự khởi động cho SwitchGuard trong Phone Master để giám sát không bị gián đoạn sau khi khởi động lại hoặc dọn dẹp. |
| ASUS | Cho phép SwitchGuard trong Auto-start Manager để giám sát khởi động lại sau khi khởi động lại máy. |

### Câu hỏi khác

**Việc theo dõi đã dừng và báo tôi đã bị đăng xuất.**  
Mẹo: eWeLink chỉ cho phép một phiên cho mỗi tài khoản. Hãy dùng tài khoản eWeLink riêng cho từng điện thoại và chia sẻ thiết bị từ ứng dụng eWeLink.

**Thiếu một thiết bị Tuya được chia sẻ cho tôi.**  
Thiếu thiết bị được chia sẻ với bạn? Tuya chỉ liệt kê các thiết bị trong nhà của bạn. Hãy nhờ chủ sở hữu thêm bạn làm thành viên nhà trong Smart Life (Me → Home Management → Add Member).

**Thiết bị Tuya ngừng cập nhật.**  
Bản dùng thử Tuya Cloud đã hết hạn nên các thiết bị Tuya không được giám sát. Hãy gia hạn tại platform.tuya.com → Cloud → IoT Core → Extend trial period.

**Thay đổi đến trễ / không có cập nhật tức thì.**  
App ID này không hỗ trợ cập nhật tức thời; đang kiểm tra định kỳ (các thay đổi ngắn hơn khoảng thời gian kiểm tra có thể bị bỏ sót)

**Tôi có thể chuyển giữa bản Google Play và bản GitHub không?**  
Có, nhưng hãy gỡ bản kia trước: chữ ký khác nhau. Sao lưu ra tệp trước khi gỡ rồi khôi phục sau đó.

**Làm sao thử báo động mà không đánh thức mọi người?**  
Thử báo động ngay: có âm thanh hoặc im lặng, ngay lập tức hoặc sau 10 giây

**Vì sao điện năng khác với ứng dụng eWeLink / Smart Life?**  
Mức tiêu thụ do SwitchGuard tính từ các số đo công suất được lấy khi giám sát đang bật, nên có thể hơi khác so với ứng dụng của nhà sản xuất thiết bị (eWeLink, Smart Life).

## Ứng dụng Windows

SwitchGuard cho Windows (10 / 11) theo dõi tương tự trên PC: nằm ở khay hệ thống, phát báo động toàn màn hình trên máy tính và dùng chung quy tắc, tự động hóa và tính năng Telegram. Khu vực, tiện ích, đồng hồ và cuộc gọi điện thoại chỉ có trên điện thoại.

### Tính năng

- Các thẻ giống nhau: Thiết bị, Cảnh báo, Tự động hóa, Lịch sử, Trò chuyện, Cài đặt; lệnh thoại trong trò chuyện dùng tính năng nhập bằng giọng nói của Windows.
- Biểu tượng khay: mở, bắt đầu / dừng theo dõi, tắt báo động, thoát; đóng cửa sổ không dừng việc theo dõi.
- Cửa sổ báo động toàn màn hình, âm có sẵn (Còi hú, Bíp, Nhịp, Chuông) hoặc WAV / MP3 của riêng bạn.
- Màn hình chờ và trình bảo vệ màn hình Windows: bảng hiện ra khi PC rảnh.
- Laptop cắm sạc có thể làm cảm biến mất điện.
- Cập nhật từ GitHub bằng một cú nhấp; giữ nguyên cài đặt và giấy phép. Sao lưu ra tệp có mật khẩu (không khôi phục được trên điện thoại).

### Cài đặt chỉ có trên Windows

| Cài đặt | Tác dụng |
|---|---|
| Windows | Để SwitchGuard canh chừng trên máy tính này không gián đoạn. Nếu một trong hai bị tắt, báo động có thể bị bỏ lỡ sau khi khởi động lại hoặc khi máy đang ngủ. |
| Khởi động cùng Windows | Mở ở khay hệ thống khi bạn đăng nhập Windows |
| Giữ máy tính luôn thức | Trong khi giám sát, máy tính không chuyển sang chế độ ngủ |
| Dùng làm trình bảo vệ màn hình Windows | Khi máy tính không hoạt động, màn hình chờ sẽ mở; di chuột hoặc nhấn phím để đóng |
| Thời gian chờ | Máy tính phải không hoạt động bao lâu trước khi màn hình chờ mở |
| Cài đặt trình bảo vệ màn hình Windows | Ví dụ "Khi tiếp tục, hiển thị màn hình đăng nhập" |
| Mở màn hình chờ | Toàn màn hình; Esc để đóng, nhấp để "Mở ứng dụng". Màn hình luôn sáng khi đang mở. |
| Ngôn ngữ & giao diện: Giao diện | Sáng, tối hoặc giống Windows. |
| Ngôn ngữ | Chọn ngôn ngữ của ứng dụng, thông báo và báo động. |
| Cập nhật trực tiếp từ Tuya trên máy tính này | Tắt: thiết bị Tuya được kiểm tra mỗi 5 phút |
| Mất điện khi máy tính này chuyển sang pin | Dành cho laptop cắm điện ở nhà: nếu chạy bằng pin một phút, được tính là mất điện, kể cả khi không có internet (tin nhắn Telegram được gửi khi có kết nối lại). |
| Máy tính này dùng chung nguồn điện với các thiết bị | Khi máy đang chạy bằng điện lưới thì không mất điện, nên thiết bị bị rớt sẽ được báo ngay thay vì sau tối đa 90 giây. |
| Tải từ tệp… | Dùng WAV hoặc MP3 của bạn cho báo động hoặc thông báo (60 giây đầu, tối đa 20 MB). |
| Sao lưu ra tệp… | Toàn bộ cài đặt, khóa tài khoản và âm thanh trong một tệp được bảo vệ bằng mật khẩu |
| Kích hoạt Pro | Khi chưa có giấy phép, ứng dụng giám sát 1 thiết bị. Nhập khóa giấy phép để giám sát tất cả thiết bị. |

### Cài đặt trên Windows

1. Nhấp đúp vào tệp cài đặt (SwitchGuard-…-Kurulum.exe). Tệp không yêu cầu quyền quản trị và cài xong trong vài giây.
2. Nếu Windows báo "Windows đã bảo vệ PC của bạn" (SmartScreen), nhấn Thông tin thêm → Vẫn chạy. Ứng dụng chưa được ký; vì vậy mới có cảnh báo.
3. SwitchGuard được cài vào thư mục người dùng (AppData › Local › SwitchGuard) và thêm lối tắt vào màn hình nền và menu Start.
4. Ứng dụng tự khởi động ở khay hệ thống (cạnh đồng hồ) khi bạn đăng nhập Windows. Đóng cửa sổ không dừng ứng dụng; để thoát, dùng biểu tượng ở khay → Thoát.
5. Thêm tài khoản trong Cài đặt → Tài khoản (các bước cho Tuya và eWeLink ở bên dưới).
6. Chuyển sang máy tính khác: trên máy cũ, Cài đặt → Sao lưu & chuyển → Sao lưu ra tệp; cài trên máy mới, Khôi phục từ tệp, rồi đăng nhập lại eWeLink.
7. Để gỡ cài đặt: Cài đặt Windows → Ứng dụng → Ứng dụng đã cài → SwitchGuard → Gỡ cài đặt.
8. Cài phiên bản mới: trước tiên gỡ phiên bản cũ (bước 7), rồi chạy tệp cài đặt mới. Cài đặt, tài khoản và âm thanh của bạn được giữ nguyên. (Nút Cập nhật trong ứng dụng sẽ tự làm việc này.)

## Biên dịch

```
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest      # unit tests
```
Bản phát hành cần `keystore.properties` + keystore ở thư mục gốc dự án (không có trong kho mã).

## Lịch sử thay đổi

[English](CHANGELOG.md) · [Türkçe](CHANGELOG.tr.md) · [Releases](https://github.com/Macerce/switchguard/releases)

## Liên hệ

Câu hỏi, báo lỗi và góp ý (hoặc mở issue tại đây): **switchguardapp@gmail.com**

## Giấy phép

SwitchGuard là **source-available** (mã nguồn công khai để xem), không phải mã nguồn mở. Mã nguồn: [PolyForm Noncommercial 1.0.0](LICENSE). Sử dụng cá nhân và phi thương mại khác là miễn phí; bán mã hoặc dùng cho mục đích thương mại bị cấm. Các bản đến hết 2.8.3 được phát hành theo GPL-3.0.

Bản chính thức (Google Play, [Releases](https://github.com/Macerce/switchguard/releases)) ai cũng có thể dùng, kể cả doanh nghiệp. Bản sửa đổi không được dùng tên hay logo SwitchGuard. Giấy phép thương mại: **switchguardapp@gmail.com**

Không liên kết, không được bảo trợ hay tài trợ bởi eWeLink, CoolKit hoặc ITEAD. Sonoff và eWeLink là nhãn hiệu của các chủ sở hữu tương ứng. Cũng không liên kết với Tuya.
