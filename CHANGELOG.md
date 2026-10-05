# Changelog

[Türkçe](CHANGELOG.tr.md)

Newest first. Versions are **major.minor.patch**: patch = fixes, minor = new features.

---

## Unreleased
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
