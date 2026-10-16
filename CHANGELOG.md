# Changelog

[Türkçe](CHANGELOG.tr.md)

Newest first. Versions are **major.minor.patch**: patch = fixes, minor = new features.

---

## 2.12.1 — 2026-10-16
**Fixes**
- TV: choosing a time (quiet hours start and end, daily summary time, device timers) now works with the remote. The system's clock face couldn't be used with the remote; on a TV the app now shows its own time picker: up and down change the hour or minutes, OK on the hour moves to minutes, OK on the minutes saves, Back cancels. Phones and tablets keep the usual clock.
- TV: the steps for receiving from a phone now point to Settings → Backup & transfer → Send to another device.

## 2.12.0 — 2026-10-15
**New look**
- The Devices screen now looks like a smart-home app: devices are tiles in a grid (two per row on a phone). Each tile has an icon that matches what the device controls (light, water or pump, fridge, heating, fan, door, plug — guessed from its name), its state and power, and a round power button. A device that's on lights up in SwitchGuard green; an offline one is greyed out with a red dot.
- Multi-channel devices (4CH, SPM, DualR3) get a wide tile with one button per channel, so you can switch any channel without opening the device. Channels that measure power show their watts.
- At the top, a protection card shows whether monitoring is live, how many devices are on or offline, and the total power.
- Device page: a large power button in the middle (large channel buttons on multi-channel devices), with connection, monitoring and power above it.
- Automations read as "If … / Then …", with a coloured icon for the action.
- The automation editor uses device cards, tiles for events and actions, numbered channel buttons and − / + for minutes instead of drop-down lists, with an "If / Then" summary above Save.
- History shows each event's icon on a coloured circle: red for alarms, teal for notifications.
- The full-screen alarm now has rings spreading out from the bell.
- The app always uses SwitchGuard's own colours (in light and dark) instead of taking them from the wallpaper, so it looks the same on every phone.

**Simpler settings**
- Settings is now a short list of categories: Accounts, Alarm, Quiet hours, Tell someone else, Daily summary, Monitoring, Permissions, Backup & transfer, Language, Help & about. Tap one to open its page. Each row shows a one-line summary, e.g. "2 permissions missing" in orange.
- Tapping the Settings tab again takes you back to the list. On a TV, the remote returns to the row you opened.

**Arrange devices**
- Card size is now "Small card" (half a row) or "Large card" (full row). Multi-channel devices start large, the others small.

## 2.11.0 — 2026-10-14
**Tell someone else (Telegram)**
- If an alarm isn't dismissed within the time you choose (2, 5, 10, 15 or 30 minutes), the people or family group you picked get a Telegram message, and another one once it's dismissed. Even if your phone is in another room at night, someone at home or a neighbour finds out (Settings → Alarm → Tell someone else).
- The messages are sent by a Telegram bot you create: free, and no other server is involved. The setup window walks you through it: create a bot with BotFather, check the token, have people tap Start in the bot, "Find people", send a test message.
- Test alarms never message anyone. Without internet it retries every minute; every attempt and error shows up in History.

**TV: enter keys from your phone**
- On a TV, the App ID/Secret, Tuya Access ID/Secret and Telegram bot token fields have an "Enter from phone" button. The TV shows a QR code; scan it with your phone's camera and paste the keys into the page that opens — no more typing long keys with the remote. SwitchGuard doesn't need to be installed on the phone.
- What you type is encrypted in the phone's browser with a one-time key from the QR code and goes straight to the TV; it never leaves your home network.

## 2.10.2 — 2026-10-14
**Dual apps warning**
- A second copy of SwitchGuard made with the phone's Dual apps (app clone) feature is now detected. Two copies running side by side ring every alarm twice and sign each other out when they use the same eWeLink account; on Xiaomi phones permissions also appear switched off. The clone shows "This is a second copy of SwitchGuard", the main copy shows "There's a second SwitchGuard on this phone", and both explain how to remove it. If you use both copies on purpose, you can turn the warning off.
- Detected: dual apps on Xiaomi, OPPO, realme, OnePlus, Samsung and vivo, plus the Android 14+ clone profile and private space.

**Test alarm**
- The test alarm can now ring with sound or silently (screen only). The silent test has no sound or vibration, so you can check the alarm screen and notification at night without waking anyone.
- "Ring in 10 seconds": lock the phone meanwhile to see the alarm screen open on the lock screen. On a TV, switch to another app to see the alarm come up over it.
- If a real alarm arrives during a silent test, it still rings.

## 2.10.1 — 2026-10-13
**Xiaomi phones**
- Xiaomi (MIUI / HyperOS) keeps the alarm screen off the lock screen while "Show on Lock screen" and "Open new windows while running in the background" are off, even when full-screen alarms are allowed. The app now reads both and shows them on the "Alarm may not reach you" card, in Settings and during setup; "Fix" opens Xiaomi's "Other permissions" page directly.
- Autostart is now detected on Xiaomi; you no longer confirm it with "I did it".

**Alarm volume**
- While an alarm rings, the phone's volume keys now adjust the alarm (while a SwitchGuard screen is open); before, they changed the media volume.
- With "Ring at full volume" on, if you turn the volume down while an alarm rings, the alarm volume returns to its previous level when you dismiss it. Before, it stayed low if it had already been at maximum, so the phone's clock alarm rang quietly too.

**TV**
- On the home screen the remote now reaches the on/off switch and the "4 channels" row inside a card; before, focus skipped them and OK always opened the device. The device page now opens from the card's title.
- Focus stays on a switch after you press it (it used to jump to the side menu).
- Coming back from a device page returns focus to the same card.

**Phones and tablets**
- Coming back from a device page no longer jumps to the top of the list; the scroll position and open channel groups are kept.

## 2.10.0 — 2026-10-12
**Alarms that follow up**
- Ring again if not resolved: after you dismiss an alarm, it rings again every 10, 30 or 60 minutes while the device stays in the state that caused it — still off, still on or offline (Settings → Alarm). Reminders stop by themselves when the device is back to normal, is no longer monitored or that event no longer alarms.

**Backup and history**
- Encrypted backup: save rules, automations, settings and your Tuya and eWeLink keys to a password-protected file, and restore it on a new phone (Settings → Backup). Sign-in sessions are never stored in the file. Before restoring you see a summary of what is inside.
- Export history as a CSV file that opens directly in Excel or Google Sheets — last 24 hours, 7 days, 30 days or everything, for all devices or the selected one (History → Export).

**At a glance**
- Home-screen widget: shows whether monitoring is running; turns red while an alarm rings and amber when there is a problem.
- Quick Settings tile: turn monitoring on or off from the notification shade (on a locked phone it asks to unlock first).

**Quality and privacy**
- App secrets and sign-in tokens are now encrypted with a key kept in the phone's secure hardware (Android Keystore). Existing values are encrypted automatically.
- If the app ever crashes, the details are saved on the phone. "Send a problem report" (Settings → About) prepares an email with them and with diagnostic details; nothing is shared unless you send it.
- English texts use proper singular and plural forms ("1 device", "2 devices").
- Faster with many devices: settings are no longer re-read on every live update.
- Automated checks (unit tests, lint and build) and on-device tests for encryption, the alarm notification and the volume boost.

## 2.9.0 — 2026-10-11
**Alarms that never fail silently**
- Critical devices: a new switch on the device page makes its alarms ring even during quiet hours (for example a flood sensor at night).
- If monitoring was interrupted (the phone stopped SwitchGuard, or the phone was off), the gap is recorded in History and you get a notification, because changes during that time may have been missed. A watchdog now restarts monitoring when the phone kills it.
- If SwitchGuard can't see your devices for too long (no internet, cloud errors, signed out), you get a warning after 10 minutes — adjustable in Settings → Monitoring — and a note when monitoring works again.
- Pending "went offline" alarms and automation timers (stayed on for N minutes, delayed actions) now survive a restart of the monitor or an app update instead of being lost.
- Fixed a rare sign-out from eWeLink with a misleading "signed in on another device" message: the app and the monitor no longer refresh the session at the same time, and temporary eWeLink server errors no longer sign you out.

**A real alarm screen**
- With the phone locked or the screen off, an alarm now opens a full-screen alarm page with a large Dismiss button (Android 14 and later ask for permission once).
- The alarm rings at full volume and pauses music and videos; your alarm volume goes back to what it was when you dismiss it (Settings → Alarm → Ring at full volume).
- History shows when an alarm was dismissed and how long it took (response time).
- A new card on the home screen warns about phone settings that can stop alarms from reaching you — notifications off, alarm notifications turned down, Do Not Disturb set to total silence, alarm volume at zero, battery restrictions, missing full-screen or autostart permission — each with a one-tap fix. It only appears when something is wrong.

**Tablets and Android TV**
- Tablets and Chromebooks: side navigation rail, devices in a grid and a comfortable reading width.
- Android TV: SwitchGuard appears in the TV launcher, works with the remote everywhere and uses a dark theme. An alarm appears on top of whatever is playing and keeps coming back until it is dismissed.
- Move your setup from your phone to a TV or tablet over Wi-Fi with a 6-digit code (Settings → Send to another device). eWeLink sign-ins aren't copied: each device needs its own eWeLink account.
- eWeLink sign-in works with the TV remote.

**License**
- The source code license changes from GPL-3.0 to PolyForm Noncommercial 1.0.0: personal and noncommercial use stays free, selling the code or using it commercially is not permitted. Versions up to and including 2.8.3 remain under GPL-3.0. Official builds may still be used by anyone, including businesses.

## 2.8.3 — 2026-10-04
**Maintenance**
- Updated an outdated internal library (androidx.fragment 1.1.0 → 1.9.1, pulled in by Google Play services) that Google Play flagged. No visible changes.

## 2.8.2 — 2026-10-03
**Background start help for more phone brands**
- Besides Xiaomi, phones from Samsung, Huawei/Honor, Oppo/Realme/OnePlus, Vivo/iQOO, Tecno/Infinix and Asus now get a "Background start" row in Settings → Permissions, with brand-specific instructions and a shortcut to the right settings screen.
- The same card now appears in the setup wizard's permissions step, so monitoring keeps running after reboots and isn't put to sleep by the phone.
- If a brand's settings screen can't be opened, the app info page opens instead.

## 2.8.1 — 2026-10-03
**Hint for shared Tuya devices**
- The Tuya connect dialog now explains that devices only shared with you may not appear; the owner must add you as a home member in Smart Life (Me → Home Management → Add Member).
- If Tuya connects but finds no devices, the dialog stays open and shows the likely causes and the home-member hint.
- When the device list is empty and Tuya is connected, the same hint appears in the list.

## 2.8.0 — 2026-10-01
**Try it without an account (demo)**
- New **"Try without an account (demo)"** option in setup, and a **Demo devices** switch in Settings → Account.
- Three virtual devices with every feature: a power-metering freezer plug, a 4-channel pump panel with per-channel power, and a garden light.
- **Simulate events** on a demo device's page: switch it from outside, lose/restore the connection, drop a channel's power. Rules, alarm, automations, history and energy all work as with real devices.
- Two sample automations: "Pump stalled" (alarm) and "Garden light left on" (notification).
- Demo devices don't count toward the free version's one-device limit.
- **"Skip for now"** in setup lets you look around without connecting an account.
- **Test alarm** now works even when monitoring is off.

## 2.7.0 — 2026-10-01
**Arrange your devices**
- **Arrange devices** mode on the Devices screen (tune icon at the top, or long-press a card).
- **Priority devices:** starred devices always stay at the top under "Priority".
- **Ordering:** move devices up/down with arrows.
- **Small / large cards:** a small card shows name, status, power and the switch on one line.
- **Categories:** create, rename, reorder and delete categories such as "Manholes" or "Lighting"; assign devices; tap a header to collapse the group.

## 2.6.2 — 2026-10-01
- **Illustrated setup guides** for eWeLink and Tuya (English/Turkish, on GitHub).
- Guide links in Settings → About; "Open the illustrated guide" button in the setup wizard.
- Fix: the status card sometimes said "Updated in … seconds".
- Energy note is now brand-neutral.

## 2.6.1 — 2026-10-01
- First-run **"Where are your devices?"** step: eWeLink or Tuya / Smart Life path.
- Step-by-step Tuya Cloud project guide and connect step.
- **Power-drop alarm** now also works on single-channel power-metering plugs (Tuya plugs, eWeLink POW).
- Privacy policy updated to cover Tuya.

## 2.6.0 — 2026-10-01
**Tuya / Smart Life support**
- Smart Life and Tuya Smart devices are monitored through the user's own free Tuya Cloud project.
- Live alarms **under one second** via Tuya's message service (Pulsar).
- eWeLink and Tuya devices in one list; alarms, rules, automations and history work the same.
- Warning when the Tuya trial expires or the keys are wrong.
- Settings → Account → Tuya / Smart Life connect dialog.

## 2.5.2 — 2026-10-01
- Settings → About → **Contact & feedback** (switchguardapp@gmail.com).

## 2.5.1 — 2026-10-01
- **History is shown in the selected language**; existing entries are translated automatically.
- New notifications switch language immediately.

## 2.5.0 — 2026-10-01
- In-app **language setting**: phone language / English / Türkçe.

## 2.4.0 — 2026-10-01
- New automation: **"is on but its power drops below"** (SONOFF SPM-4Relay / DUALR3 channels), with start-up grace and duration.

## 2.3.1 — 2026-09-30
- Fix: with some App IDs the live connection failed and short on/off pulses were missed.
- Unseen-events badge on the History tab.
- "Autostart" setting for Xiaomi phones; the monitoring service restarts when the app is opened if needed.

## 2.3.0 — 2026-09-30
- **SwitchGuard Pro** (one-time Google Play purchase): the free version monitors 1 device, Pro monitors all.
- Fix: Back from other tabs returns to Devices.

## 2.2.0 — 2026-09-30
- **Per-device alarm sound.**
- **"Monitoring stopped — signed out"** warning when the eWeLink session is opened on another phone.
- Open-source license (GPL-3.0).

## 2.1.0 — 2026-09-29
- **Energy monitoring:** power, voltage, current; daily/monthly consumption and a 7-day chart.
- **Automations:** "stays on/off for", power thresholds, switch devices, alarm/notification.
- eWeLink **device timers** and a **daily summary** notification.
- Multi-channel devices fold their channels behind a summary row.
- Fix: state reverting to the old value after switching from the app.

## 2.0.1 — 2026-09-29
- Fix: unreadable text in dark theme.

## 2.0.0 — 2026-09-29
**SwitchGuard**
- New name and Material 3 design; English and Turkish.
- **Live monitoring** over eWeLink WebSocket with periodic backup checks.
- Per-device rules (alarm / notification / none), quiet hours, ignore brief disconnections.
- Switch devices from the app, event history, logo and store graphics.

## 1.0 — 2026-09-29
- First release ("eWeLink Alarm"): a simple app that monitors eWeLink devices' on/off and online state and rings an alarm.
