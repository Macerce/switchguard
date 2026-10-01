# SwitchGuard – eWeLink (SONOFF) setup guide

[Türkçe](ewelink.tr.md) · [Tuya / Smart Life guide](tuya.en.md)

This guide connects the SONOFF and other eWeLink-compatible devices you control in the **eWeLink** app to SwitchGuard, step by step. It takes about **2–5 minutes** and is free.

SwitchGuard talks to your devices through eWeLink's official API. For that you create **your own free app** (App ID + App Secret) on the eWeLink developer site. You sign in on eWeLink's own page; SwitchGuard never sees your password.

> **You need:** the eWeLink account your devices are in, and SwitchGuard. A computer is easier for the developer site, but a phone works too.

---

## 1. Open the eWeLink developer site

1. Go to **https://dev.ewelink.cc**.
2. Click **Login/Register** (top right) and sign in with **the eWeLink account your devices are in** (the same email/phone and password as in the eWeLink app).

![eWeLink Developer Center home](img/ewelink-01-home.jpg)

## 2. Register as a developer

On first sign-in you are asked for a developer role. The **free standard role** is enough; no paid plan is needed.

## 3. Create an app

1. In the console create a new app (**Create / Add App**).
2. Fill in:

| Field | What to enter |
|---|---|
| **App name** | `SwitchGuard` (any name) |
| **Redirect URL** | `https://127.0.0.1` — must match **exactly** (no trailing `/`) |

3. Save. Your app's **App ID** and **App Secret** are shown.

> **The Redirect URL matters:** if the one in SwitchGuard and the one on the developer site are not identical, the sign-in page shows an error. SwitchGuard's default is `https://127.0.0.1`; the **Copy Redirect URL** button in setup copies it for you.

## 4. Enter them in SwitchGuard

**During first setup:** at **Where are your devices?** choose **eWeLink** → **Next**.

| Choose eWeLink | Developer app step |
|---|---|
| ![](img/app-en-platform-ewelink.jpg) | ![](img/app-en-ew-developer.jpg) |

**Open dev.ewelink.cc** opens the site; **Copy Redirect URL** copies `https://127.0.0.1`.

**Next** → **Enter your API credentials**:

1. **App ID:** paste the App ID from step 3.
2. **App Secret:** paste the App Secret.
3. **Redirect URL:** must be identical to the developer site (default `https://127.0.0.1`).

**Next** → **Sign in to eWeLink** → tap **Sign in with eWeLink**. eWeLink's own sign-in page opens; use your email/phone and password. When it works it says **"Signed in"**.

| API credentials | Sign in |
|---|---|
| ![](img/app-en-ew-credentials.jpg) | ![](img/app-en-ew-login.jpg) |

**Next** → grant permissions (notifications, background activity) → **Start monitoring**.

**To change later:** **Settings → Account → API credentials** (App ID/Secret) and **Sign in again**.

![Settings – accounts](img/app-en-settings-accounts.jpg)

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| The sign-in page shows a "redirect" / "invalid" error | Is the Redirect URL **exactly** the same on both sides? (`https://127.0.0.1`, no trailing `/`, `https` not `http`.) |
| Signed in but no devices | Did you sign in to the developer site and SwitchGuard with **the account your devices are in**? Is the region right? |
| Signing in on another phone signs the first one out ("Monitoring stopped — signed out" warning) | eWeLink allows one session per account. Give the second phone **its own eWeLink account** and **share** the devices to it from the eWeLink app. |
| Status card stays **"Monitoring (periodic)"** | Live connection may be unavailable for some App IDs; SwitchGuard keeps watching with regular checks. Adjust the frequency in Settings → **Backup check interval**. |

---

Questions? **switchguardapp@gmail.com** · [GitHub Issues](https://github.com/Macerce/switchguard/issues)
