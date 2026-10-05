# SwitchGuard

Android app that watches the smart plugs and switches in your own **eWeLink / SONOFF** account and rings a real alarm the moment something changes — a pump that stopped, a freezer plug switched off, a device that lost its connection.

*Türkçe: eWeLink hesabınızdaki SONOFF priz ve anahtarları izler; kapandığında, açıldığında ya da bağlantısı koptuğunda siz kapatana kadar çalan bir alarm verir.*

<p>
  <img src="docs/screenshots/05-devices.png" width="220" alt="Devices">
  <img src="docs/screenshots/07-detail.png" width="220" alt="Device detail">
  <img src="docs/screenshots/12-notification.png" width="220" alt="Alarm">
</p>

## Features
- Alarm that keeps ringing until dismissed, even in silent mode; a different sound per device
- Live updates over the eWeLink WebSocket, with HTTP resync and polling fallback
- Per-device rules (alarm / notification / ignore) for off, on, offline and online
- Offline grace period, quiet hours, remote on/off, eWeLink device timers, automations
- Energy readings for power-metering devices, daily summary, full event history
- Loud warning when the eWeLink session is lost (e.g. same account signed in on another phone)
- Free for 1 device; a one-time Pro purchase on Google Play monitors all devices
- No server, no analytics, no ads — everything stays on the phone ([privacy policy](https://sites.google.com/view/switchguard-privacy))

## Several devices / Birden fazla cihaz
Phone, tablet and (coming soon) Android TV: **each device needs its own eWeLink account with the developer feature approved, and that account's own API keys (App ID / Secret)** — eWeLink allows one signed-in device per account, and the keys must belong to the signed-in account. Share your devices to the extra account in the eWeLink app. Pro is bought once and works on all devices with the same Google account. Details: [English](docs/guide/ewelink.en.md#using-several-devices-phone-tablet-android-tv) · [Türkçe](docs/guide/ewelink.tr.md#birden-fazla-cihazda-kullanım-telefon-tablet-android-tv)

## Requirements
- Android 8.0+ (API 26)
- A free eWeLink developer app (App ID + App Secret) from [dev.ewelink.cc](https://dev.ewelink.cc) — the in-app guide walks you through it
- eWeLink allows one session per account and App ID: give each phone its own eWeLink account and share the devices from the eWeLink app

## Build
```
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest      # unit tests
```
Release builds need a `keystore.properties` + keystore in the project root (not in the repo).

## Setup guides / Kurulum rehberleri
- eWeLink (SONOFF): [English](docs/guide/ewelink.en.md) · [Türkçe](docs/guide/ewelink.tr.md)
- Tuya / Smart Life: [English](docs/guide/tuya.en.md) · [Türkçe](docs/guide/tuya.tr.md)

## Videos / Videolar
- First setup in one minute / İlk kurulum (1 dk): https://youtube.com/shorts/jarBmMg2kYA
- Connecting eWeLink / eWeLink'i bağlama: https://youtu.be/jK_KcG2OPao
- Connecting Tuya / Smart Life / Tuya'yı bağlama: https://youtu.be/sCdvMs7CSic

## Changelog / Sürüm geçmişi
[English](CHANGELOG.md) · [Türkçe](CHANGELOG.tr.md) · [Releases](https://github.com/Macerce/switchguard/releases)

## Contact
Questions, bug reports and feedback: **switchguardapp@gmail.com** (or open an issue here).

## License / Lisans
Source code: [PolyForm Noncommercial 1.0.0](LICENSE). Personal and other noncommercial use is free; selling the code or using it commercially is not permitted. Versions up to and including 2.8.3 were released under GPL-3.0.
Official builds (Google Play, [Releases](https://github.com/Macerce/switchguard/releases)) may be used by anyone, including businesses. For commercial licensing contact **switchguardapp@gmail.com**. Modified versions must not use the SwitchGuard name or logo.

Kaynak kod: [PolyForm Noncommercial 1.0.0](LICENSE). Kişisel ve ticari olmayan kullanım serbesttir; kodu satmak veya ticari amaçla kullanmak yasaktır. 2.8.3 ve önceki sürümler GPL-3.0 ile yayımlanmıştı.
Resmi sürümleri (Google Play, Releases) işletmeler dahil herkes kullanabilir. Ticari lisans için: **switchguardapp@gmail.com**. Değiştirilmiş sürümler SwitchGuard adını ve logosunu kullanamaz.

SwitchGuard is an independent project and is not affiliated with eWeLink, CoolKit or ITEAD.
