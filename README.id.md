[English](README.md) · [Türkçe](README.tr.md) · [Deutsch](README.de.md) · [Español](README.es.md) · [Português (Brasil)](README.pt-BR.md) · [Français](README.fr.md) · [Italiano](README.it.md) · [Nederlands](README.nl.md) · [Polski](README.pl.md) · [Русский](README.ru.md) · [Українська](README.uk.md) · [العربية](README.ar.md) · [हिन्दी](README.hi.md) · **Bahasa Indonesia** · [Tiếng Việt](README.vi.md) · [ไทย](README.th.md) · [简体中文](README.zh-CN.md) · [繁體中文](README.zh-TW.md) · [日本語](README.ja.md) · [한국어](README.ko.md)

# SwitchGuard

SwitchGuard memantau colokan dan sakelar pintar di akun **eWeLink / SONOFF** dan **Tuya / Smart Life** milik Anda sendiri dan membunyikan alarm sungguhan begitu ada yang berubah — pompa yang berhenti, colokan freezer yang mati, perangkat yang kehilangan koneksi. Berjalan di ponsel dan tablet Android, Android TV, dan jam tangan Wear OS, serta tersedia juga aplikasi Windows.

<p>
  <img src="docs/screenshots/play/en/v215/01-devices.png" width="200" alt="Perangkat">
  <img src="docs/screenshots/play/en/v215/02-energy.png" width="200" alt="Energi">
  <img src="docs/screenshots/play/en/v215/05-automations.png" width="200" alt="Otomatisasi">
  <img src="docs/screenshots/play/en/v215/08-alarm.png" width="200" alt="Alarm perangkat">
</p>

> **Bahasa:** aplikasi Windows tersedia dalam 20 bahasa sejak 0.13.0. Di Android, pilihan bahasa di dalam aplikasi (20 bahasa) hadir di **2.26**; rilis Android terbaru adalah 2.25.0.

## Unduh

### Android (ponsel, tablet, TV)
- **Google Play**: https://play.google.com/store/apps/details?id=com.macerce.switchguard
- **APK**: [rilis Android terbaru](https://github.com/Macerce/switchguard/releases/latest) — unduh `SwitchGuard-x.y.z.apk` lalu buka di ponsel. Jika SwitchGuard sudah terpasang dari Google Play, copot dulu (tanda tangan berbeda). Versi GitHub memberi tahu Anda saat ada versi lebih baru.

### Windows 10 / 11
- **Penginstal**: [rilis Windows](https://github.com/Macerce/switchguard/releases?q=Windows&expanded=true) — unduh `SwitchGuard-x.y.z-Kurulum.exe` dari rilis terbaru lalu jalankan. Aplikasi mencari versi baru sendiri dan memperbarui diri dengan satu klik; pengaturan tetap tersimpan.

### Gratis dan Pro
- Android: sepenuhnya gratis — semua fitur, semua perangkat dipantau, tanpa pembelian. Perangkat demo memungkinkan Anda mencoba semuanya tanpa perangkat keras.
- Windows gratis: semua fitur, untuk 1 perangkat yang dipantau.
- Windows: kunci lisensi Pro (Setelan → kartu paket) memantau semua perangkat. Untuk mendapatkan kunci, hubungi **switchguardapp@gmail.com**

## Persyaratan

- Android 8.0+ (API 26); Wear OS untuk aplikasi jam
- eWeLink: aplikasi developer eWeLink gratis (App ID + App Secret) — panduan di aplikasi menuntun langkah demi langkah. eWeLink hanya mengizinkan satu sesi per akun dan App ID, jadi setiap ponsel, tablet, TV, atau PC memerlukan akun eWeLink sendiri yang dibagikan perangkatnya ([dev.ewelink.cc](https://dev.ewelink.cc))
- Tuya / Smart Life: Access ID dan Access Secret dari proyek Tuya Cloud gratis milik Anda, yang ditautkan ke akun aplikasi Smart Life / Tuya Anda ([platform.tuya.com](https://platform.tuya.com))
- Windows 10 / 11 (64-bit) untuk aplikasi Windows

## Panduan penyiapan

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

## Fitur

### Pemantauan dan alarm

- Pembaruan langsung melalui WebSocket eWeLink dan layanan pesan Tuya, dengan sinkron ulang HTTP dan pemeriksaan berkala sebagai cadangan.
- Alarm terus berbunyi sampai Anda matikan — bahkan dalam mode senyap — dan terbuka layar penuh di atas layar kunci; setiap perangkat bisa punya suara sendiri.
- Aturan per perangkat: alarm, notifikasi, atau tidak ada saat perangkat mati, menyala, offline, atau kembali online.
- Peringatan bersyarat: berbunyi saat perangkat tetap menyala atau mati beberapa waktu, atau dayanya naik di atas / turun di bawah batas (mis. pompa di bawah 50 W).
- Abaikan putus koneksi singkat, peringatan saat pemantauan itu sendiri berhenti (tanpa internet / cloud), peringatan keras saat sesi eWeLink hilang (mis. akun yang sama masuk di ponsel lain).
- Kartu kesiapan menampilkan dan memperbaiki setelan ponsel yang bisa menghalangi alarm (pembatasan baterai, izin layar penuh, Jangan Ganggu, volume alarm…).

### Perangkat

- Ubin bergaya rumah pintar dengan tombol cepat, satu tombol per kanal pada perangkat multi-kanal, status dan daya langsung.
- Halaman perangkat: aturan, suara alarm sendiri, kategori peringatan, riwayat energi dan biaya, timer eWeLink, sinyal Wi-Fi dan diagnosis putus koneksi, konsumsi siaga, dan peristiwa terbaru.
- **Atur perangkat**: Bintang = tetap di atas · panah = pindahkan · ukuran kartu = kecil atau besar · tag = kategori. Tekan lama kartu untuk kembali ke sini.

### Peringatan

- Peringatan perangkat dan peringatan bersyarat di satu tempat, dengan filter kategori peringatan (Kritis / Penting / Normal).
- Setelan peringatan: suara dan volume alarm, jam tenang, kategori peringatan, Telegram ("Beri tahu orang lain") dan peringatan pintar — lihat *Semua setelan* di bawah.
- Peringatan pintar: satu peringatan saat listrik padam, konsumsi tidak biasa, anggaran bulanan, laporan mingguan, mode liburan.

### Otomatisasi

- **Aturan** — tindakan otomatis: *Jika* sesuatu terjadi, *maka* lakukan sesuatu, dengan jeda opsional.
- Pemicu: menyala, mati, tetap menyala selama, tetap mati selama, menjadi offline, kembali online, daya melebihi, daya turun di bawah, saat matahari terbit, saat matahari terbenam, saat suhu luar naik di atas, saat suhu luar turun di bawah, saat tiba, saat pergi.
- Tindakan: Nyalakan, Matikan, Bunyikan alarm, Kirim notifikasi, Jalankan skenario, Mulai siklus, Hentikan siklus.
- **Hanya jika**: Hanya pada jam ini, Dan hanya jika perangkat lain…, Hanya saat di rumah / bepergian.
- **Skenario** — Pilih apa yang terjadi pada setiap perangkat saat skenario dijalankan. Perangkat yang dibiarkan di “Lewati” tidak disentuh.
- **Grup** — Pilih perangkat dalam grup. Grup muncul di layar Perangkat seperti perangkat: tombolnya menyalakan atau mematikan semuanya sekaligus, dan Anda dapat memakainya di otomatisasi.
- **Siklus & timer** — Hitung mundur, Nyala, lalu mati, Siklus nyala/mati, Satu per satu, Langkah kustom. Mulai dengan tombol, pada jam tertentu di hari yang dipilih, atau selama rentang jam; variasi acak opsional (rumah terlihat berpenghuni) dan "berhenti jika diubah manual".
- Otomatisasi berjalan di ponsel ini selama pemantauan aktif. Untuk jadwal yang harus berjalan meski ponsel mati, gunakan timer di halaman perangkat.

### Energi

- Tampilan hari / bulan / tahun dengan total, perkiraan biaya, daya rata-rata dan puncak, jam/hari/bulan tersibuk, serta ringkasan sorotan.
- Tarif tunggal atau tiga tarif (siang / puncak / malam), harga per perangkat atau harga bawaan.
- Konsumsi siaga (daya yang tidak pernah dilewati ke bawah oleh perangkat) dan biaya bulanannya.
- Laporan energi dalam PDF (untuk dicetak atau dibagikan) atau CSV (Excel, Google Spreadsheet).
- Konsumsi dihitung oleh SwitchGuard dari pembacaan daya selama pemantauan aktif, sehingga bisa sedikit berbeda dari aplikasi pembuat perangkat (eWeLink, Smart Life).

### Riwayat

- Setiap peristiwa dengan tanggal dan jam, serta siapa atau apa yang mengubahnya (aplikasi, otomatisasi); filter per perangkat.
- Ekspor ke CSV (24 jam terakhir, 7 hari, 30 hari, atau semua) dan hapus riwayat.

### Chat

- Halo. Anda dapat mengelola perangkat dengan menulis (dalam bahasa Inggris atau Turki): menyalakan atau mematikannya, menanyakan status, pemakaian daya, atau suhu luar. Ketuk mikrofon untuk mengucapkannya (“Turn off kitchen”); balasan dibacakan. Templat siap pakai ada di bawah; ketuk untuk mengirim, tahan untuk mengubah.
- Perintah teks bebas bekerja tanpa internet dan memahami bahasa Inggris dan Turki; mikrofon memakai pengenalan suara ponsel dan balasan bisa dibacakan.
- Template: pesan sekali ketuk yang juga bisa berjalan otomatis setiap hari pada jam tertentu atau saat sesuatu terjadi.

### Layar siaga

- Papan layar penuh untuk ponsel atau tablet yang sedang diisi daya: jam, cuaca, status kritis, daya langsung, aktivitas terbaru, dan apa yang berjalan berikutnya. Juga tampil di atas layar kunci.
- Membuka pengaturan Android: pilih SwitchGuard di Screensaver dan atur agar mulai saat mengisi daya. Papan ini kemudian menggantikan layar kunci saat tercolok pengisi daya.

### Widget, ubin setelan cepat, pintasan

- **SwitchGuard** — Menampilkan apakah pemantauan berjalan dan alarm yang sedang berbunyi
- **Sakelar SwitchGuard** — Menyalakan dan mematikan satu perangkat atau kanal dengan sekali ketuk. Bisa juga diatur ke Di rumah / Bepergian atau untuk menjalankan skenario.
- **Grup SwitchGuard** — Ubin besar: nyalakan atau matikan setiap perangkat dalam grup secara terpisah
- **Perintah suara SwitchGuard** — Satu ubin: ketuk, ucapkan (“Turn off kitchen”), dan langsung dijalankan
- **Ubin setelan cepat** — menampilkan status pemantauan dan memulai atau menghentikannya (ponsel harus dibuka kuncinya dulu); saat alarm berbunyi, membuka aplikasi.
- **Pintasan aplikasi** — tekan lama ikon aplikasi untuk Di rumah / Bepergian dan skenario Anda.

### Di rumah / Bepergian

- **Di rumah / Bepergian** — Aturan yang ditandai "hanya saat bepergian" tidak berjalan. Beralih ke Bepergian saat Anda pergi (juga dari widget, jam tangan, atau /away di Telegram).
- **Mode liburan** — Anda dianggap bepergian, jam tenang nonaktif, dan laporan tiba setiap pagi pukul 09:00. Berakhir dengan sendirinya.
- **Zona** — Pilih area di peta; otomatisasi berjalan saat ponsel Anda memasuki atau meninggalkannya (mis. nyalakan lampu taman saat Anda tiba di rumah, matikan semuanya saat Anda pergi). Gambar peta diunduh dari OpenStreetMap; lokasi Anda hanya dipakai di ponsel ini.

### Wear OS

- Aplikasi jam SwitchGuard menampilkan alarm (matikan dari pergelangan), perangkat (nyalakan/matikan), skenario, dan Di rumah / Bepergian; terhubung ke aplikasi ponsel.

### Android TV

- Berjalan di Android TV dengan remote; alarm muncul di atas apa pun yang sedang diputar ("Tampilkan alarm di atas aplikasi lain").
- "Masukkan dari ponsel": TV menampilkan kode QR; Anda mengetik kunci di ponsel dan kunci dikirim terenkripsi ke TV lewat Wi-Fi rumah.
- "Terima dari perangkat lain": menyalin kunci Tuya, aturan, dan setelan dari ponsel Anda lewat Wi-Fi.

### Beberapa perangkat

Ponsel, tablet, Android TV, dan PC: **setiap perangkat memerlukan akun eWeLink sendiri dengan fitur developer yang disetujui, dan kunci API milik akun itu (App ID / Secret)** — eWeLink hanya mengizinkan satu perangkat masuk per akun. Bagikan perangkat Anda ke akun tambahan dari aplikasi eWeLink. Kunci Tuya bisa dipakai di beberapa perangkat. Detail: [English](docs/guide/ewelink.en.md#using-several-devices-phone-tablet-android-tv) · [Türkçe](docs/guide/ewelink.tr.md#birden-fazla-cihazda-kullanım-telefon-tablet-android-tv)

### Demo

- **Perangkat demo** — Perangkat virtual untuk mencoba semua fitur. Ini bertindak seolah-olah sesuatu terjadi pada perangkat itu sendiri (seseorang menyalakannya, listrik atau Wi-Fi padam), sehingga aturan Anda berlaku dan alarm dapat berbunyi. Menyalakan lewat tombol di atas dianggap tindakan Anda dan tidak membunyikan alarm.

## Semua setelan

Semuanya sesuai urutan di aplikasi. Tab **Pengaturan** berisi koneksi dan opsi aplikasi; alarm, jam tenang, Telegram, dan peringatan pintar ada di tab **Peringatan** → Pengaturan peringatan; opsi tiap perangkat ada di halamannya.

### Pengaturan → Akun

| Setelan | Fungsinya |
|---|---|
| eWeLink — Masuk dengan eWeLink | Membuka halaman masuk eWeLink sendiri; aplikasi tidak pernah melihat kata sandi Anda. |
| Kredensial API | App ID, App Secret, dan wilayah aplikasi developer eWeLink Anda. |
| Keluar | Pemantauan akan berhenti. Pengaturan dan riwayat Anda tetap tersimpan. |
| Tuya / Smart Life | Masukkan Access ID dan Access Secret proyek Tuya Cloud Anda (platform.tuya.com → Cloud → proyek Anda → Overview). Akun aplikasi Smart Life / Tuya Anda harus ditautkan ke proyek. Pusat data: Eropa Tengah, Amerika Barat, Tiongkok, India |
| Putuskan | Menghapus koneksi Tuya; perangkat Tuya tidak lagi dipantau. |
| Perangkat demo | Perangkat virtual untuk mencoba semua fitur |

### Pengaturan → Pemantauan

Cara SwitchGuard menjaga perangkat Anda di latar belakang.

| Setelan | Fungsinya |
|---|---|
| Keandalan maksimum | Menjaga prosesor tetap aktif agar tidak ada kejadian yang tertunda. Memakai lebih banyak baterai. |
| Abaikan pemutusan singkat | Tunggu selama ini sebelum memberi peringatan bahwa perangkat offline. Jika tersambung kembali tepat waktu, tidak ada peringatan. |
| Peringatkan jika pemantauan berhenti | Jika tidak ada internet atau koneksi cloud selama waktu ini: Anda mendapat peringatan |
| Interval pemeriksaan cadangan | Hanya dipakai bila pembaruan instan tidak tersedia. |

### Pengaturan → Izin

Tanpa ini, Android dapat menghalangi alarm sampai ke Anda.

| Setelan | Fungsinya |
|---|---|
| Notifikasi | Alarm dan peringatan tidak dapat ditampilkan. |
| Aktivitas latar belakang | Ponsel dapat menidurkan pemantauan; alarm bisa terlambat atau terlewat. |
| Alarm layar penuh | Saat ponsel terkunci, alarm tidak dapat membuka layar besarnya. |
| Alarm di layar kunci (Xiaomi) | Saat ponsel terkunci, layar alarm tidak dapat terbuka. Ketuk Perbaiki, buka Izin lainnya dan aktifkan “Tampilkan di layar Kunci” dan “Buka jendela baru saat berjalan di latar belakang”. |
| Tampilkan alarm di atas aplikasi lain | Diperlukan di Android TV agar alarm muncul di atas apa pun yang sedang diputar. Ketuk dan izinkan SwitchGuard. |
| Mulai di latar belakang (Xiaomi, Huawei, OPPO, vivo…) | Muncul di ponsel yang memblokir mulai di latar belakang (Xiaomi, Huawei, OPPO, vivo…): agar pemantauan berjalan lagi setelah mulai ulang atau pembaruan. Lihat Pemecahan masalah. |

### Pengaturan → Lokasi dan zona

| Setelan | Fungsinya |
|---|---|
| Lokasi | Diperlukan untuk aturan matahari terbit/terbenam dan suhu luar. Tetap di perangkat ini; untuk suhu, dikirim ke Open-Meteo dengan pembulatan sekitar 100 m. |
| Zona | Pilih area di peta; otomatisasi berjalan saat ponsel Anda memasuki atau meninggalkannya (mis. nyalakan lampu taman saat Anda tiba di rumah, matikan semuanya saat Anda pergi). Gambar peta diunduh dari OpenStreetMap; lokasi Anda hanya dipakai di ponsel ini. |

### Pengaturan → Layar siaga

| Setelan | Fungsinya |
|---|---|
| Buka layar siaga | Ketuk layar untuk “Buka aplikasi” dan “Tutup” |
| Gunakan sebagai screensaver | Membuka pengaturan Android: pilih SwitchGuard di Screensaver dan atur agar mulai saat mengisi daya. Papan ini kemudian menggantikan layar kunci saat tercolok pengisi daya. |
| Redupkan di malam hari | Teks memudar antara 23:00 dan 07:00 |

### Pengaturan → Ringkasan harian

| Setelan | Fungsinya |
|---|---|
| Notifikasi ringkasan harian | Berapa kali setiap perangkat menyala, berapa lama menyala, dan berapa energi yang dipakai hari ini |
| Kirim pukul | Jam pengiriman ringkasan. |
| Tampilkan ringkasan hari ini sekarang | Langsung menampilkannya. |

### Pengaturan → Cadangan & transfer

| Setelan | Fungsinya |
|---|---|
| Cadangkan ke file | Aturan, otomatisasi, pengaturan, serta kunci Tuya dan eWeLink disimpan dalam file yang dilindungi kata sandi |
| Pulihkan dari file | Pindah ke ponsel baru? Pilih file cadangan dan masukkan kata sandinya |
| Kirim ke perangkat lain | Salin kunci Tuya, aturan, dan pengaturan ke TV atau tablet di Wi-Fi yang sama |
| Terima dari perangkat lain | Ambil penyiapan dari ponsel Anda lewat Wi-Fi |

Transfer lewat Wi-Fi tidak pernah mengirim kunci API eWeLink atau sesi eWeLink: di perangkat lain, masukkan kunci API miliknya sendiri dan masuk dengan akun eWeLink miliknya sendiri. Cadangan ke file justru menyertakan kunci eWeLink dan Tuya, dilindungi kata sandi Anda.

### Pengaturan → Bahasa

| Setelan | Fungsinya |
|---|---|
| Bahasa | Pilih bahasa untuk aplikasi, notifikasi, dan alarm. Bahasa ponsel atau salah satu dari 20 bahasa (mulai 2.26). |

### Pengaturan → Bantuan & tentang

| Setelan | Fungsinya |
|---|---|
| Panduan penyiapan | Tampilkan lagi penyiapan awal |
| Panduan penyiapan eWeLink | Langkah demi langkah dengan gambar (GitHub) |
| Panduan penyiapan Tuya / Smart Life | Langkah demi langkah dengan gambar (GitHub) |
| Kirim laporan masalah | Ada yang salah? Menyiapkan email berisi detail diagnostik. Tidak ada yang dibagikan kecuali Anda mengirimnya. |
| Hapus laporan error tersimpan | Menghapus laporan error yang tersimpan di perangkat ini. |
| Kontak & masukan | Menulis email ke developer (pertanyaan, bug, ide). |
| Kebijakan privasi | Membuka kebijakan privasi. |
| Tentang | Versi dan catatan hukum. Versi GitHub memeriksa GitHub untuk versi yang lebih baru. |

### Peringatan

| Setelan | Fungsinya |
|---|---|
| Peringatan perangkat | Untuk apa setiap perangkat berbunyi atau memberi notifikasi |
| Peringatan bersyarat | Berbunyi saat perangkat tetap menyala atau mati selama beberapa waktu, atau dayanya berubah |
| Alarm siap / Alarm mungkin tidak sampai ke Anda | Kartu kesiapan di atas: mendaftar setelan ponsel yang bisa menghalangi alarm dan membuka layar yang tepat untuk memperbaikinya; ada juga tombol "Uji alarm". |

### Peringatan → Alarm

| Setelan | Fungsinya |
|---|---|
| Suara alarm | Suara alarm umum. Setiap perangkat bisa punya suara sendiri (halaman perangkat). |
| Volume alarm | Volume alarm SwitchGuard sendiri. |
| Getar | Bergetar saat alarm berbunyi. |
| Bunyi dengan volume penuh | Menaikkan volume alarm ponsel ke maksimum selama alarm berbunyi dan menjeda musik; dikembalikan saat Anda mematikannya |
| Bunyi lagi jika belum teratasi | Setelah Anda mematikan alarm, alarm berbunyi lagi pada interval ini selama perangkat tetap dalam kondisi penyebabnya (masih mati, masih nyala, atau offline). Berhenti saat perangkat kembali normal. |
| Beri tahu orang lain | Penyiapan Telegram (lihat di bawah). |
| Uji alarm | Coba alarm sekarang: dengan suara atau senyap, langsung atau setelah 10 detik |

### Peringatan → Kategori peringatan

Setiap perangkat memiliki kategori peringatan. Jika tidak Anda pilih, kategori diatur otomatis: perangkat yang dipantau dengan kejadian apa pun yang diatur ke Alarm adalah Kritis, yang hanya mengirim notifikasi adalah Penting, sisanya Normal. Peringatan bersyarat dan otomatisasi dapat memakai kategori sebagai pemicu (mis. “bunyikan alarm saat perangkat kritis mana pun offline”).

| Setelan | Fungsinya |
|---|---|
| Kritis · Penting · Normal · Otomatis | Jika otomatis: Kritis bila ada kejadian yang membunyikan alarm, Penting bila kejadian hanya memberi notifikasi |
| Berbunyi juga saat jam tenang | Alarm berbunyi juga saat jam tenang / Saat jam tenang, alarm menjadi notifikasi |

### Peringatan → Jam tenang

| Setelan | Fungsinya |
|---|---|
| Jam tenang | Pada jam ini alarm menjadi notifikasi (perangkat kritis tetap berbunyi) |
| Mulai / Selesai | Jam mulai dan selesai jam tenang. |

Perangkat dalam kategori peringatan yang diatur berbunyi saat jam tenang (Kritis secara default) tetap berbunyi.

### Peringatan → Beri tahu orang lain

Jika alarm tidak dimatikan tepat waktu, orang yang Anda pilih mendapat pesan Telegram, dan satu lagi setelah alarm dimatikan. Meski ponsel Anda di ruangan lain pada malam hari, seseorang di rumah atau tetangga tetap tahu. Pesan dikirim oleh bot Telegram yang Anda buat; tidak ada server lain yang terlibat.

| Setelan | Fungsinya |
|---|---|
| 1. Bot Telegram | Di Telegram, buka @BotFather, kirim /newbot dan beri nama bot Anda. Tempel token dari BotFather di bawah. |
| 2. Orang yang diberi tahu | Minta setiap orang membuka @your_bot di Telegram dan mengetuk Start (Anda juga dapat menambahkan bot ke grup keluarga). Lalu ketuk Cari orang. |
| 3. Kapan | Berapa lama alarm boleh berbunyi sebelum orang diberi tahu (atau langsung). |
| Kirim pesan uji | Mengirim pesan uji ke semua orang di daftar. |
| Matikan | Mematikan pemberitahuan Telegram. |

### Peringatan → Peringatan pintar

Peringatan yang melihat semua perangkat Anda sekaligus: pemadaman listrik, konsumsi tidak wajar, dan ringkasan mingguan. Dengan Telegram Anda juga dapat memeriksa status, mengambil alih alarm, dan menyalakan/mematikan perangkat dari jauh.

| Setelan | Fungsinya |
|---|---|
| Pemadaman listrik: Deteksi pemadaman listrik | Jika semua perangkat yang dipantau menjadi offline bersamaan, Anda mendapat satu peringatan “pemadaman listrik” alih-alih satu per perangkat, dan pesan berisi durasinya saat listrik kembali. |
| Pengisi daya sebagai sensor listrik | Jika ponsel/tablet ini tetap tercolok di lokasi, dicabut selama semenit dianggap pemadaman listrik; ini juga berfungsi saat internet mati. Pesan dikirim lewat Telegram melalui data seluler. |
| Di rumah / Bepergian: Peringatan lebih lembut saat di rumah | Saat di rumah, alarm perangkat non-kritis menjadi notifikasi (seperti jam tenang). Perangkat kritis selalu berbunyi. |
| Mode liburan | Anda dianggap bepergian, jam tenang nonaktif, dan laporan tiba setiap pagi pukul 09:00. Berakhir dengan sendirinya. |
| Konsumsi: Konsumsi tidak wajar | Setiap pagi, membandingkan kemarin dengan dua minggu sebelumnya. Pemakaian yang jauh lebih tinggi bisa berarti ada gangguan; yang jauh lebih rendah (freezer yang hampir tidak bekerja) bisa berarti perangkat berhenti berfungsi. |
| Tarif listrik | Harga default untuk perangkat tanpa harga sendiri. Dengan tarif tiga tingkat, setiap jam dihargai pada bandnya sendiri: siang 06–17, puncak 17–22, malam 22–06. |
| Anggaran bulanan | Anda diberi tahu sebulan sekali saat perkiraan biaya perangkat pengukur energi mencapai jumlah ini. |
| Laporan mingguan | Setiap Senin pukul 09:00: pemadaman, alarm dan waktu respons, pemutusan koneksi, dan konsumsi minggu lalu. Juga dikirim ke Telegram jika sudah diatur. |
| Tampilkan laporan minggu ini | Langsung menampilkannya. |
| Telegram: Perintah dan tombol “Siap” | Orang dalam daftar Anda dapat mengirim /status dan mengambil alih alarm yang berbunyi. Pesan alarm mendapat tombol “Siap”: menekannya membisukan ponsel. |
| Izinkan menyalakan dan mematikan perangkat | Perintah /on dan /off. Hanya orang dalam daftar Anda yang dapat memakainya. |
| Pemberitahuan bertahap | Setiap orang dikirimi pesan setelah waktu tunggunya sendiri. Misalnya tetangga setelah 5 menit, kerabat setelah 15, grup keluarga setelah 30. Jika seseorang mengetuk “Siap”, orang berikutnya tidak dikirimi pesan. |
| Telepon saat alarm kritis | Saat perangkat dalam kategori Kritis membunyikan alarm, orang tersebut juga ditelepon dari saluran ponsel ini ketika waktu tunggunya di peringatan bertahap habis. Telepon hanya untuk membangunkan: apa yang terjadi ada di pesan Telegram yang dikirim bersamaan (Android tidak mengizinkan aplikasi memutar suara ke panggilan telepon). Minta mereka menyimpan nomor ini sebagai “Alarm SwitchGuard”. Panggilan diulang setiap 2 menit, hingga 3 kali, sampai alarm dimatikan atau seseorang mengetuk “Siap” di Telegram. Grup tidak dapat ditelepon. |
| Kirim peringatan pintar ke Telegram | Pemadaman listrik, konsumsi tidak wajar, peringatan anggaran, dan laporan mingguan juga ditulis ke Telegram. |
| Watchdog | Dua perangkat dengan bot yang sama saling mengawasi (mis. ponsel dan TV box). Perangkat pengirim menjaga pesan yang disematkan di obrolan Telegram tetap mutakhir; perangkat pengawas membunyikan alarm dan mengirim pesan ke kontak Anda jika berhenti. Jangan lepas sematan pesan itu. |

### Halaman perangkat (ketuk perangkat)

| Setelan | Fungsinya |
|---|---|
| Pantau perangkat ini | Perubahan dicatat dan diberi peringatan sesuai aturan di bawah |
| Saat hal ini terjadi | Mati, Menyala, Menjadi offline, Menjadi online → Alarm / Notifikasi / Tidak ada. Alarm berbunyi sampai Anda mematikannya. Notifikasi menampilkan pemberitahuan biasa. Tidak ada hanya mencatat kejadian di riwayat. |
| Suara untuk perangkat ini | Suara alarm sendiri; jika tidak, suara umum. |
| Kategori peringatan | Jika otomatis: Kritis bila ada kejadian yang membunyikan alarm, Penting bila kejadian hanya memberi notifikasi |
| Energi | Daya, tegangan, arus; hari ini dan bulan ini; riwayat hari / bulan / tahun dengan biaya; buat laporan (PDF / CSV). |
| Harga per kWh | Harga per kWh untuk perangkat ini atau harga bawaan; bisa juga dijadikan bawaan. |
| Timer (di perangkat) | Tidak ada timer. Timer berjalan di perangkat itu sendiri, bahkan saat ponsel Anda mati. (Perangkat eWeLink: tambah atau ubah timer nyala/mati pada jam tertentu di hari yang dipilih.) |
| Koneksi | dBm adalah kekuatan sinyal: makin mendekati 0 makin baik. Di atas -55 sangat baik, hingga -67 baik, hingga -75 cukup, di bawahnya lemah. |
| Konsumsi siaga | Rata-rata per jam terendah dalam 7 hari terakhir: daya yang tidak pernah turun di bawahnya pada perangkat ini (dan apa pun yang tercolok padanya), bahkan saat "tidak melakukan apa-apa". TV, modem, dan pengisi daya sering menarik daya sebesar ini saat siaga. |
| Salin data perangkat | Menyalin data mentah perangkat ke papan klip, berguna untuk melaporkan model yang tidak didukung |

## Perintah Telegram

Orang dalam daftar Anda dapat mengirim /status dan mengambil alih alarm yang berbunyi. Pesan alarm mendapat tombol “Siap”: menekannya membisukan ponsel.

```
Perintah SwitchGuard:
/status – pemantauan, alarm, dan perangkat
/ack – ambil alih alarm yang berbunyi
/on nama [kanal] – nyalakan perangkat atau grup (beberapa: /on kitchen, garden)
/off nama [kanal] – matikan perangkat atau grup
/watt [nama] – daya saat ini, hari ini, dan pemakaian biasa
/temp – suhu luar ruangan
/scene nama – jalankan skenario
/home · /away – atur di rumah / bepergian

Anda juga dapat menulis bebas dalam bahasa Inggris atau Turki: “turn off kitchen and garden”, “heater watts”, “is the pump on”.

Menyalakan dan mematikan perangkat serta skenario harus diizinkan di aplikasi (Peringatan → Peringatan pintar).
```

Nama perintah bahasa Inggris dan Turki sama-sama berfungsi (mis. /status = /durum, /on = /ac). Perintah hanya diterima dari obrolan yang Anda pilih.

## Izin dan alasannya

| Izin | Alasan |
|---|---|
| `POST_NOTIFICATIONS` | Menampilkan alarm, notifikasi, dan status pemantauan. |
| `FOREGROUND_SERVICE` | Menjaga pemantauan tetap berjalan di latar belakang (notifikasi status permanen). |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Meminta pengecualian dari pengoptimalan baterai agar alarm tidak terlambat. |
| `USE_FULL_SCREEN_INTENT` | Membuka alarm layar penuh di atas layar kunci. |
| `SYSTEM_ALERT_WINDOW` | Menampilkan alarm di atas aplikasi lain (Android TV, Xiaomi). |
| `WAKE_LOCK` | Menjaga prosesor tetap aktif saat menangani peristiwa ("Keandalan maksimum"). |
| `VIBRATE` | Bergetar saat alarm. |
| `RECEIVE_BOOT_COMPLETED` | Memulai ulang pemantauan setelah ponsel dinyalakan ulang. |
| `INTERNET` | Berkomunikasi dengan cloud eWeLink / Tuya (dan Telegram / Open-Meteo / OpenStreetMap jika Anda memakainya). |
| `ACCESS_COARSE_LOCATION` | Hanya saat Anda mengetuk "Gunakan lokasi perangkat ini" untuk aturan matahari terbit/terbenam dan suhu. |
| `ACCESS_FINE_LOCATION` | Zona: area tepat untuk aturan tiba / pergi. |
| `ACCESS_BACKGROUND_LOCATION` | Zona: mendeteksi kedatangan dan kepergian saat aplikasi tertutup (hanya diminta saat membuat zona). |
| `CALL_PHONE` | "Telepon saat alarm kritis": menelepon nomor yang Anda masukkan dari jalur ponsel ini. |

## Privasi

- Tanpa server sendiri, tanpa analitik, iklan, atau pelacakan. Tidak ada yang dijual atau dibagikan.
- Kunci, token, perangkat, aturan, setelan, dan riwayat (maks. 2.000 peristiwa) hanya disimpan di perangkat Anda.
- Aplikasi hanya berkomunikasi dengan cloud yang Anda hubungkan (eWeLink `*.coolkit.cc` / `*.coolkit.cn`, Tuya `openapi.tuya*.com`) dan, hanya jika Anda memakainya, dengan Telegram, Open-Meteo (suhu dan cuaca; lokasi dibulatkan sekitar 100 m), dan ubin peta OpenStreetMap.
- Kata sandi eWeLink Anda diketik di halaman eWeLink sendiri; aplikasi tidak pernah melihatnya.
- Data aplikasi dikecualikan dari cadangan cloud Android; cadangan file dilindungi kata sandi; transfer Wi-Fi dienkripsi dengan kode sekali pakai dan tetap di jaringan rumah Anda.

Kebijakan privasi lengkap: [Google Sites](https://sites.google.com/view/switchguard-privacy) · [docs/privacy-policy.md](docs/privacy-policy.md)

## Pemecahan masalah dan FAQ

### Alarm terlambat atau tidak berbunyi

| Masalah | Yang harus dilakukan |
|---|---|
| Aktivitas latar belakang dibatasi | Ponsel dapat menidurkan pemantauan; alarm bisa terlambat atau terlewat. |
| Alarm layar penuh tidak diizinkan | Saat ponsel terkunci, alarm tidak dapat membuka layar besarnya. |
| Xiaomi memblokir layar alarm | Saat ponsel terkunci, layar alarm tidak dapat terbuka. Ketuk Perbaiki, buka Izin lainnya dan aktifkan “Tampilkan di layar Kunci” dan “Buka jendela baru saat berjalan di latar belakang”. |
| Notifikasi alarm diturunkan | Kategori notifikasi “Alarm” tidak diatur ke prioritas tinggi, sehingga alarm tidak akan muncul atau memenuhi layar. |
| Jangan Ganggu: senyap total | Dalam mode ini bahkan suara alarm dibisukan. |
| Volume alarm sangat rendah | Volume alarm ponsel nol atau hampir nol, sehingga Anda mungkin tidak mendengar SwitchGuard. Perbaiki akan mengaktifkan “Bunyi dengan volume penuh”. |
| Notifikasi nonaktif | Alarm dan peringatan tidak dapat ditampilkan. |
| Ada SwitchGuard kedua di ponsel ini | Salinan lain SwitchGuard dibuat dengan fitur Aplikasi ganda (klon aplikasi) di ponsel. Dua salinan yang berjalan bersamaan membunyikan setiap alarm dua kali dan saling mengeluarkan akun bila memakai akun eWeLink yang sama; di ponsel Xiaomi, izin salinan ini juga bisa tampak nonaktif. Hapus salinan kedua: sentuh dan tahan ikonnya (yang punya lencana kecil) lalu ketuk Copot pemasangan. |

### Merek ponsel yang menghentikan aplikasi latar belakang

| Ponsel | Yang harus dilakukan |
|---|---|
| Xiaomi | Diperlukan agar pemantauan berjalan lagi setelah restart atau pembaruan. Ketuk lalu aktifkan Autostart untuk SwitchGuard. |
| Samsung | Samsung dapat menidurkan aplikasi. Di Baterai, pilih "Tidak dibatasi" dan tambahkan SwitchGuard ke "Aplikasi yang tidak pernah tidur" (Baterai → Batas penggunaan di latar belakang). |
| Huawei / Honor | Di Peluncuran aplikasi, atur SwitchGuard ke "Kelola manual" dan aktifkan Peluncuran otomatis, Peluncuran sekunder, dan Jalankan di latar belakang. |
| OPPO / realme / OnePlus | Aktifkan Peluncuran otomatis untuk SwitchGuard, lalu di pengaturan Baterainya izinkan aktivitas latar belakang. |
| vivo / iQOO | Izinkan SwitchGuard berjalan di latar belakang dan izinkan konsumsi daya latar belakang yang tinggi. |
| Tecno / Infinix | Aktifkan Auto-start untuk SwitchGuard di Phone Master agar pemantauan tetap berjalan setelah restart dan pembersihan. |
| ASUS | Izinkan SwitchGuard di Auto-start Manager agar pemantauan berjalan lagi setelah restart. |

### Pertanyaan lain

**Pemantauan berhenti dan mengatakan saya sudah keluar.**  
Tips: eWeLink hanya mengizinkan satu sesi per akun. Berikan setiap ponsel akun eWeLink sendiri dan bagikan perangkat dari aplikasi eWeLink.

**Perangkat Tuya yang dibagikan kepada saya tidak muncul.**  
Perangkat yang dibagikan kepada Anda tidak muncul? Tuya hanya mencantumkan perangkat di rumah Anda. Minta pemiliknya menambahkan Anda sebagai anggota rumah di Smart Life (Me → Home Management → Add Member).

**Perangkat Tuya berhenti diperbarui.**  
Uji coba Tuya Cloud telah berakhir, sehingga perangkat Tuya tidak dipantau. Perpanjang di platform.tuya.com → Cloud → IoT Core → Extend trial period.

**Perubahan datang terlambat / pembaruan instan tidak tersedia.**  
Pembaruan instan tidak tersedia untuk App ID ini; memakai pemeriksaan berkala (perubahan yang lebih singkat dari interval mungkin terlewat)

**Bisakah saya beralih antara versi Google Play dan GitHub?**  
Bisa, tetapi copot dulu yang lain: tanda tangannya berbeda. Cadangkan ke file sebelum mencopot, lalu pulihkan setelahnya.

**Bagaimana menguji alarm tanpa membangunkan semua orang?**  
Coba alarm sekarang: dengan suara atau senyap, langsung atau setelah 10 detik

**Mengapa konsumsi berbeda dari aplikasi eWeLink / Smart Life?**  
Konsumsi dihitung oleh SwitchGuard dari pembacaan daya selama pemantauan aktif, sehingga bisa sedikit berbeda dari aplikasi pembuat perangkat (eWeLink, Smart Life).

## Aplikasi Windows

SwitchGuard untuk Windows (10 / 11) melakukan pemantauan yang sama di PC: berada di baki sistem, membunyikan alarm layar penuh di komputer, dan berbagi aturan, otomatisasi, serta fitur Telegram yang sama. Zona, widget, jam tangan, dan panggilan telepon hanya ada di ponsel.

### Fitur

- Tab yang sama: Perangkat, Peringatan, Otomatisasi, Riwayat, Chat, Pengaturan; perintah suara di obrolan memakai pengetikan suara Windows.
- Ikon baki: buka, mulai / hentikan pemantauan, matikan alarm, keluar; menutup jendela tidak menghentikan pemantauan.
- Jendela alarm layar penuh, suara bawaan (Sirene, Bip, Denyut, Lonceng) atau WAV / MP3 milik Anda.
- Layar siaga dan screen saver Windows: papan terbuka saat PC menganggur.
- Laptop yang dibiarkan tercolok berfungsi sebagai sensor pemadaman listrik.
- Pembaruan dari GitHub dengan satu klik; setelan dan lisensi tetap. Cadangan ke file berkata sandi (tidak bisa dipulihkan di ponsel).

### Setelan khusus Windows

| Setelan | Fungsinya |
|---|---|
| Windows | Agar SwitchGuard berjaga di komputer ini tanpa henti. Jika salah satunya nonaktif, alarm bisa terlewat setelah restart atau saat komputer tidur. |
| Mulai bersama Windows | Terbuka di baki sistem saat Anda masuk ke Windows |
| Jaga komputer tetap aktif | Selama pemantauan, komputer tidak masuk mode tidur |
| Gunakan sebagai screensaver Windows | Saat komputer menganggur, layar siaga terbuka; menggerakkan mouse atau menekan tombol akan menutupnya |
| Waktu tunggu | Berapa lama komputer harus menganggur sebelum layar siaga terbuka |
| Pengaturan screensaver Windows | Misalnya "Saat melanjutkan, tampilkan layar masuk" |
| Buka layar siaga | Layar penuh; Esc menutup, klik untuk "Buka aplikasi". Layar tetap menyala selama terbuka. |
| Bahasa & tampilan: Tema | Terang, gelap, atau sama seperti Windows. |
| Bahasa | Pilih bahasa untuk aplikasi, notifikasi, dan alarm. |
| Pembaruan langsung Tuya di komputer ini | Nonaktif: perangkat Tuya diperiksa setiap 5 menit |
| Listrik padam saat komputer ini beralih ke baterai | Untuk laptop yang dibiarkan tercolok di rumah: jika berjalan dengan baterai selama satu menit, dianggap listrik padam, bahkan tanpa internet (pesan Telegram terkirim setelah koneksi kembali). |
| Komputer ini memakai listrik yang sama dengan perangkat | Selama berjalan dengan listrik dari stopkontak tidak ada pemadaman, jadi perangkat yang terputus langsung dilaporkan, bukan setelah hingga 90 detik. |
| Muat dari file… | Pakai WAV atau MP3 Anda sendiri untuk alarm atau notifikasi (60 detik pertama, maks. 20 MB). |
| Cadangkan ke file… | Semua pengaturan, kunci akun, dan suara dalam satu file yang dilindungi kata sandi |
| Aktifkan Pro | Tanpa lisensi, aplikasi memantau 1 perangkat. Masukkan kunci lisensi untuk memantau semua perangkat Anda. |

### Memasang di Windows

1. Klik dua kali file penyiapan (SwitchGuard-…-Kurulum.exe). File ini tidak meminta hak administrator dan terpasang dalam beberapa detik.
2. Jika Windows menampilkan "Windows melindungi PC Anda" (SmartScreen), tekan Info selengkapnya → Tetap jalankan. Aplikasi belum ditandatangani; karena itu ada peringatan.
3. SwitchGuard dipasang di folder pengguna Anda (AppData › Local › SwitchGuard) dan menambahkan pintasan ke desktop dan menu Start.
4. Aplikasi mulai sendiri di baki sistem (di samping jam) saat Anda masuk ke Windows. Menutup jendela tidak menghentikannya; untuk keluar, gunakan ikon baki → Keluar.
5. Tambahkan akun Anda di Pengaturan → Akun (langkah Tuya dan eWeLink ada di bawah).
6. Pindah ke komputer lain: di komputer lama, Pengaturan → Cadangan & transfer → Cadangkan ke file; pasang di komputer baru, Pulihkan dari file, lalu masuk lagi ke eWeLink.
7. Untuk menghapus: Pengaturan Windows → Aplikasi → Aplikasi terinstal → SwitchGuard → Hapus instalan.
8. Memasang versi baru: hapus dulu versi lama (langkah 7), lalu jalankan file penyiapan yang baru. Pengaturan, akun, dan suara Anda tetap tersimpan. (Tombol Perbarui di aplikasi melakukannya untuk Anda.)

## Build

```
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest      # unit tests
```
Build rilis memerlukan `keystore.properties` + keystore di root proyek (tidak ada di repositori).

## Riwayat perubahan

[English](CHANGELOG.md) · [Türkçe](CHANGELOG.tr.md) · [Releases](https://github.com/Macerce/switchguard/releases)

## Kontak

Pertanyaan, laporan bug, dan masukan (atau buka issue di sini): **switchguardapp@gmail.com**

## Lisensi

SwitchGuard bersifat **source-available** (kode sumber tersedia untuk dilihat), bukan open source. Kode sumber: [PolyForm Noncommercial 1.0.0](LICENSE). Penggunaan pribadi dan nonkomersial lainnya bebas; menjual kode atau memakainya secara komersial tidak diizinkan. Versi hingga dan termasuk 2.8.3 dirilis dengan GPL-3.0.

Build resmi (Google Play, [Releases](https://github.com/Macerce/switchguard/releases)) boleh dipakai siapa saja, termasuk bisnis. Versi yang dimodifikasi tidak boleh memakai nama atau logo SwitchGuard. Lisensi komersial: **switchguardapp@gmail.com**

Tidak berafiliasi dengan, didukung, atau disponsori oleh eWeLink, CoolKit, atau ITEAD. Sonoff dan eWeLink adalah merek dagang dari pemiliknya masing-masing. Juga tidak berafiliasi dengan Tuya.
