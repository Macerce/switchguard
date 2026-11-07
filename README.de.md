[English](README.md) · [Türkçe](README.tr.md) · **Deutsch** · [Español](README.es.md) · [Português (Brasil)](README.pt-BR.md) · [Français](README.fr.md) · [Italiano](README.it.md) · [Nederlands](README.nl.md) · [Polski](README.pl.md) · [Русский](README.ru.md) · [Українська](README.uk.md) · [العربية](README.ar.md) · [हिन्दी](README.hi.md) · [Bahasa Indonesia](README.id.md) · [Tiếng Việt](README.vi.md) · [ไทย](README.th.md) · [简体中文](README.zh-CN.md) · [繁體中文](README.zh-TW.md) · [日本語](README.ja.md) · [한국어](README.ko.md)

# SwitchGuard

SwitchGuard überwacht die smarten Steckdosen und Schalter in deinen eigenen **eWeLink / SONOFF**- und **Tuya / Smart Life**-Konten und löst einen echten Alarm aus, sobald sich etwas ändert — eine Pumpe, die stehen bleibt, eine abgeschaltete Gefriertruhe, ein Gerät ohne Verbindung. Läuft auf Android-Handys, Tablets, Android TV und Wear-OS-Uhren; außerdem gibt es eine Windows-App.

<p>
  <img src="docs/screenshots/play/en/v215/01-devices.png" width="200" alt="Geräte">
  <img src="docs/screenshots/play/en/v215/02-energy.png" width="200" alt="Energie">
  <img src="docs/screenshots/play/en/v215/05-automations.png" width="200" alt="Automationen">
  <img src="docs/screenshots/play/en/v215/08-alarm.png" width="200" alt="Geräte-Alarm">
</p>

> **Sprachen:** Die Windows-App gibt es ab 0.13.0 in 20 Sprachen. Unter Android kommt die Sprachauswahl in der App (20 Sprachen) mit **2.26**; die neueste Android-Version ist 2.25.0.

## Download

### Android (Handy, Tablet, TV)
- **Google Play** (geschlossener Test): https://play.google.com/apps/testing/com.macerce.switchguard
- **APK**: [neueste Android-Version](https://github.com/Macerce/switchguard/releases/latest) — `SwitchGuard-x.y.z.apk` herunterladen und auf dem Handy öffnen. Ist SwitchGuard bereits aus Google Play installiert, deinstalliere es zuerst (andere Signatur). Die GitHub-Version meldet sich, wenn es eine neuere gibt.

### Windows 10 / 11
- **Installationsprogramm**: [Windows-Versionen](https://github.com/Macerce/switchguard/releases?q=Windows&expanded=true) — lade `SwitchGuard-x.y.z-Kurulum.exe` der neuesten Version herunter und starte sie. Die App sucht selbst nach neuen Versionen und aktualisiert sich mit einem Klick; die Einstellungen bleiben erhalten.

### Kostenlos und Pro
- Kostenlos: alle Funktionen für 1 überwachtes Gerät. Mit Demo-Geräten kannst du alles ohne Hardware ausprobieren.
- Android: Pro ist ein einmaliger Kauf in Google Play (kein Abo) und überwacht alle Geräte; es zieht mit deinem Google-Konto auf ein neues Handy um.
- Windows: Ein Pro-Lizenzschlüssel (Einstellungen → Tarifkarte) überwacht alle Geräte. Schlüssel gibt es über **switchguardapp@gmail.com**

## Voraussetzungen

- Android 8.0+ (API 26); Wear OS für die Uhren-App
- eWeLink: eine kostenlose eWeLink-Entwickler-App (App ID + App Secret) — die Anleitung in der App führt Schritt für Schritt. eWeLink erlaubt nur eine Sitzung pro Konto und App ID, daher braucht jedes Handy, Tablet, jeder TV oder PC ein eigenes eWeLink-Konto, für das die Geräte freigegeben sind ([dev.ewelink.cc](https://dev.ewelink.cc))
- Tuya / Smart Life: Access ID und Access Secret deines eigenen kostenlosen Tuya-Cloud-Projekts, verknüpft mit deinem Smart-Life- / Tuya-App-Konto ([platform.tuya.com](https://platform.tuya.com))
- Windows 10 / 11 (64 Bit) für die Windows-App

## Einrichtungsanleitungen

- eWeLink (SONOFF): [English](docs/guide/ewelink.en.md) · [Türkçe](docs/guide/ewelink.tr.md)
- Tuya / Smart Life: [English](docs/guide/tuya.en.md) · [Türkçe](docs/guide/tuya.tr.md)

### Videos

- **New in 2.17 – alert categories, critical alerts, groups (EN):** https://youtube.com/shorts/iVM3zEiBt8A
- **2.17 yenilikleri – uyarı kategorileri, kritik uyarılar, gruplar (TR):** https://youtube.com/shorts/Qha83ziwylI
- New in 2.15 – smart alerts, scenes, energy reports (EN): https://youtube.com/shorts/bOtFBjHoefE
- 2.15 yenilikleri – akıllı uyarılar, sahneler, enerji raporu (TR): https://youtube.com/shorts/y3QRZvVoeK4
- New in 2.12 – a tour of the app (EN): https://youtube.com/shorts/jvm0hx0gATk
- 2.12 yenilikleri – uygulama turu (TR): https://youtube.com/shorts/2KeUgJrCCEU
- First setup in one minute / İlk kurulum (1 dk): https://youtube.com/shorts/jarBmMg2kYA
- **Getting eWeLink API keys / eWeLink API anahtarlarını alma (TR, sesli):** https://youtu.be/G5vQ_7CXTcM
- **Getting Tuya API keys / Tuya API anahtarlarını alma (TR, sesli):** https://youtu.be/8vPjJMoMaMU

## Funktionen

### Überwachung und Alarm

- Live-Updates über den eWeLink-WebSocket und den Tuya-Nachrichtendienst, mit HTTP-Abgleich und regelmäßiger Prüfung als Rückfallebene.
- Der Alarm klingelt, bis du ihn beendest — auch im Lautlos-Modus — und öffnet sich im Vollbild über dem Sperrbildschirm; jedes Gerät kann einen eigenen Ton haben.
- Regeln pro Gerät: Alarm, Benachrichtigung oder nichts, wenn ein Gerät ausgeht, angeht, offline geht oder wieder online kommt.
- Bedingte Warnungen: Alarm, wenn ein Gerät eine Weile an oder aus bleibt oder seine Leistung über / unter eine Grenze geht (z. B. Pumpe unter 50 W).
- Kurze Verbindungsabbrüche ignorieren, Warnung, wenn die Überwachung selbst ausfällt (kein Internet / keine Cloud), lauter Hinweis, wenn die eWeLink-Sitzung verloren geht (z. B. gleiches Konto auf einem anderen Handy angemeldet).
- Eine Bereitschaftskarte zeigt und behebt Handy-Einstellungen, die den Alarm verhindern könnten (Akku-Einschränkung, Vollbild-Berechtigung, Nicht stören, Alarmlautstärke…).

### Geräte

- Kacheln im Smart-Home-Stil mit Schnell-Schaltern, einem Knopf pro Kanal bei Mehrkanal-Geräten, Live-Status und Leistung.
- Geräteseite: Regeln, eigener Alarmton, Warnkategorie, Energieverlauf und Kosten, eWeLink-Gerätetimer, WLAN-Signal und Abbruch-Diagnose, Standby-Verbrauch und letzte Ereignisse.
- **Geräte anordnen**: Stern = oben halten · Pfeile = verschieben · Kartengröße = klein oder groß · Tag = Kategorie. Halte eine Karte gedrückt, um hierher zurückzukehren.

### Warnungen

- Gerätewarnungen und bedingte Warnungen an einem Ort, Filter nach Warnkategorie (Kritisch / Wichtig / Normal).
- Warnungseinstellungen: Alarmton und Lautstärke, Ruhezeiten, Warnkategorien, Telegram („Jemand anderen informieren“) und intelligente Warnungen — siehe *Alle Einstellungen* unten.
- Intelligente Warnungen: eine Warnung bei Stromausfall, ungewöhnlicher Verbrauch, Monatsbudget, Wochenbericht, Urlaubsmodus.

### Automationen

- **Regeln** — automatische Aktionen: *Wenn* etwas passiert, *dann* tue etwas, optional mit Verzögerung.
- Auslöser: einschaltet, ausschaltet, an bleibt für, aus bleibt für, offline geht, wieder online kommt, die Leistung steigt über, die Leistung fällt unter, bei Sonnenaufgang, bei Sonnenuntergang, wenn die Außentemperatur steigt über, wenn die Außentemperatur fällt unter, bei Ankunft, beim Verlassen.
- Aktionen: Einschalten, Ausschalten, Alarm läuten, Benachrichtigung senden, Szene starten, Zyklus starten, Zyklus stoppen.
- **Nur wenn**: Nur in diesen Stunden, Und nur wenn ein anderes Gerät …, Nur bei Zuhause / Abwesend.
- **Szenen** — Lege fest, was mit jedem Gerät passiert, wenn die Szene läuft. Geräte auf „Überspringen“ bleiben unberührt.
- **Gruppen** — Wähle die Geräte der Gruppe. Die Gruppe erscheint auf dem Bildschirm „Geräte“ wie ein Gerät: Ihre Taste schaltet alle gemeinsam ein oder aus, und du kannst sie in Automationen verwenden.
- **Zyklen & Timer** — Countdown, An, dann aus, An/Aus-Zyklus, Nacheinander, Eigene Schritte. Start per Knopf, zu einer festen Uhrzeit an gewählten Tagen oder in einem Zeitfenster; optional zufällige Abweichung (bewohnt wirken) und „bei Handbedienung stoppen“.
- Automationen laufen auf diesem Handy, solange die Überwachung an ist. Für Zeitpläne, die auch bei ausgeschaltetem Handy funktionieren müssen, nutze die Timer auf der Geräteseite.

### Energie

- Ansicht nach Tag / Monat / Jahr mit Summe, geschätzten Kosten, mittlerer und Spitzenleistung, stärkster Stunde/Tag/Monat und Highlights.
- Einfach- oder Dreifachtarif (Tag / Spitze / Nacht), Preis pro Gerät oder Standardpreis.
- Standby-Verbrauch (die Leistung, unter die ein Gerät nie fällt) und seine Monatskosten.
- Energiebericht als PDF (drucken oder teilen) oder CSV (Excel, Google Tabellen).
- Der Verbrauch wird von SwitchGuard aus Leistungsmesswerten berechnet, die bei eingeschalteter Überwachung erfasst werden, und kann daher leicht von der App des Geräteherstellers (eWeLink, Smart Life) abweichen.

### Verlauf

- Jedes Ereignis mit Datum und Uhrzeit und wer oder was es ausgelöst hat (App, Automatisierung); Filter nach Gerät.
- Export als CSV (letzte 24 Stunden, 7 Tage, 30 Tage oder alles) und Verlauf löschen.

### Chat

- Hallo. Du kannst deine Geräte per Text verwalten: ein- oder ausschalten, nach Status, Stromverbrauch oder Außentemperatur fragen (Befehle auf Englisch oder Türkisch). Tippe auf das Mikrofon, um stattdessen zu sprechen („Turn off kitchen“); die Antwort wird vorgelesen. Fertige Vorlagen stehen unten; tippe auf eine, um sie zu senden, halte sie gedrückt, um sie zu bearbeiten.
- Freitext-Befehle funktionieren offline und verstehen Englisch und Türkisch; das Mikrofon nutzt die Spracherkennung des Handys, Antworten können vorgelesen werden.
- Vorlagen: Nachrichten mit einem Tipp, die auch automatisch täglich zu einer Uhrzeit oder bei einem Ereignis laufen können.

### Standby-Bildschirm

- Eine Vollbild-Tafel für ein ladendes Handy oder Tablet: Uhr, Wetter, kritischer Status, aktuelle Leistung, letzte Aktivität und was als Nächstes läuft. Sie erscheint auch über dem Sperrbildschirm.
- Öffnet die Android-Einstellungen: Wähle unter Bildschirmschoner SwitchGuard und stelle ein, dass er beim Laden startet. Die Tafel ersetzt dann am Ladegerät den Sperrbildschirm.

### Widgets, Schnelleinstellungs-Kachel, Verknüpfungen

- **SwitchGuard** — Zeigt, ob die Überwachung läuft, und einen läutenden Alarm
- **SwitchGuard-Schalter** — Schaltet ein Gerät oder einen Kanal mit einem Tipp ein und aus. Es lässt sich auch auf Zuhause / Abwesend oder eine Szene einstellen.
- **SwitchGuard-Gruppe** — Große Kachel: jedes Gerät einer Gruppe einzeln ein- oder ausschalten
- **SwitchGuard-Sprachbefehl** — Eine Kachel: tippen, sagen („Turn off kitchen“), und es wird ausgeführt
- **Schnelleinstellungs-Kachel** — zeigt den Überwachungsstatus und startet oder stoppt die Überwachung (das Handy muss zuerst entsperrt werden); während ein Alarm klingelt, öffnet sie die App.
- **App-Verknüpfungen** — lange auf das App-Symbol drücken für Zuhause / Abwesend und deine Szenen.

### Zuhause / Abwesend

- **Zuhause / Abwesend** — Regeln mit „nur bei Abwesenheit“ laufen nicht. Wechsle auf Abwesend, wenn du gehst (auch über das Widget, die Uhr oder Telegram /away).
- **Urlaubsmodus** — Du giltst als abwesend, Ruhezeiten sind aus und jeden Morgen um 09:00 kommt ein Bericht. Er endet von selbst.
- **Zonen** — Wähle einen Bereich auf der Karte; Automationen laufen, wenn dein Handy ihn betritt oder verlässt (z. B. das Gartenlicht einschalten, wenn du nach Hause kommst, alles ausschalten, wenn du gehst). Kartenbilder werden von OpenStreetMap geladen; dein Standort wird nur auf diesem Handy verwendet.

### Wear OS

- Die SwitchGuard-Uhren-App zeigt Alarme (am Handgelenk beenden), Geräte (schalten), Szenen und Zuhause / Abwesend; sie verbindet sich mit der Handy-App.

### Android TV

- Läuft auf Android TV mit Fernbedienung; der Alarm erscheint über allem, was gerade läuft („Alarm über anderen Apps anzeigen“).
- „Am Handy eingeben“: Der TV zeigt einen QR-Code; du tippst die Schlüssel am Handy ein, sie gehen verschlüsselt über dein Heim-WLAN an den TV.
- „Von anderem Gerät empfangen“: Tuya-Schlüssel, Regeln und Einstellungen per WLAN vom Handy übernehmen.

### Mehrere Geräte

Handy, Tablet, Android TV und PC: **Jedes Gerät braucht ein eigenes eWeLink-Konto mit freigeschalteter Entwicklerfunktion und den eigenen API-Schlüsseln dieses Kontos (App ID / Secret)** — eWeLink erlaubt pro Konto nur ein angemeldetes Gerät. Gib deine Geräte in der eWeLink-App für das zusätzliche Konto frei. Tuya-Schlüssel können auf mehreren Geräten genutzt werden. Pro wird einmal gekauft und gilt auf allen Android-Geräten mit demselben Google-Konto. Details: [English](docs/guide/ewelink.en.md#using-several-devices-phone-tablet-android-tv) · [Türkçe](docs/guide/ewelink.tr.md#birden-fazla-cihazda-kullanım-telefon-tablet-android-tv)

### Demo & Pro

- **Demo-Geräte** — Virtuelle Geräte, um alle Funktionen auszuprobieren. Diese verhalten sich, als wäre am Gerät selbst etwas passiert (jemand hat es geschaltet, Strom oder WLAN ist ausgefallen), daher gelten deine Regeln und der Alarm kann läuten. Schalten mit den Tasten oben zählt als deine Aktion und löst keinen Alarm aus.
- Die kostenlose Version überwacht 1 Gerät. Pro überwacht alle deine Geräte, mit allen Funktionen. Einmalige Zahlung, kein Abo. An dein Google-Konto gebunden, daher zieht es mit dir auf ein neues Handy um.

## Alle Einstellungen

Alles in der Reihenfolge der App. Der Tab **Einstellungen** enthält Verbindungen und App-Optionen; Alarm, Ruhezeiten, Telegram und intelligente Warnungen liegen unter Tab **Warnungen** → Warneinstellungen; gerätespezifische Optionen auf der Seite des jeweiligen Geräts.

### Einstellungen → Konten

| Einstellung | Wozu |
|---|---|
| Pro / Pro holen | Die kostenlose Version überwacht 1 Gerät. Pro überwacht alle deine Geräte, mit allen Funktionen. Einmalige Zahlung, kein Abo. An dein Google-Konto gebunden, daher zieht es mit dir auf ein neues Handy um. |
| Wiederherstellen | Prüft deine Google-Play-Käufe erneut (neues Handy, Neuinstallation). |
| eWeLink — Mit eWeLink anmelden | Öffnet die eigene Anmeldeseite von eWeLink; die App sieht dein Passwort nie. |
| API-Zugangsdaten | App ID, App Secret und Region deiner eWeLink-Entwickler-App. |
| Abmelden | Die Überwachung wird beendet. Deine Einstellungen und der Verlauf bleiben erhalten. |
| Tuya / Smart Life | Gib Access ID und Access Secret deines Tuya-Cloud-Projekts ein (platform.tuya.com → Cloud → dein Projekt → Overview). Das Konto deiner Smart-Life- / Tuya-App muss mit dem Projekt verknüpft sein. Data Center: Mitteleuropa, Westamerika, China, Indien |
| Trennen | Entfernt die Tuya-Verbindung; Tuya-Geräte werden nicht mehr überwacht. |
| Demo-Geräte | Virtuelle Geräte, um alle Funktionen auszuprobieren |

### Einstellungen → Überwachung

Wie SwitchGuard deine Geräte im Hintergrund überwacht.

| Einstellung | Wozu |
|---|---|
| Maximale Zuverlässigkeit | Hält den Prozessor wach, damit kein Ereignis verzögert wird. Verbraucht mehr Akku. |
| Kurze Verbindungsabbrüche ignorieren | So lange warten, bevor gemeldet wird, dass ein Gerät offline ist. Kommt es rechtzeitig zurück, wird kein Alarm ausgelöst. |
| Warnen, wenn die Überwachung ausfällt | So lange kein Internet oder keine Cloud-Verbindung: du erhältst eine Warnung |
| Intervall der Ersatzprüfung | Wird nur verwendet, wenn Sofort-Updates nicht verfügbar sind. |

### Einstellungen → Berechtigungen

Ohne diese kann Android verhindern, dass der Alarm dich erreicht.

| Einstellung | Wozu |
|---|---|
| Benachrichtigungen | Alarme und Warnungen können nicht angezeigt werden. |
| Hintergrundaktivität | Das Handy kann die Überwachung in den Ruhezustand versetzen; Alarme können sich verspäten oder ausbleiben. |
| Alarm im Vollbild | Bei gesperrtem Handy kann der Alarm seinen großen Bildschirm nicht öffnen. |
| Alarm auf dem Sperrbildschirm (Xiaomi) | Bei gesperrtem Handy kann sich der Alarmbildschirm nicht öffnen. Tippe auf „Beheben“, öffne „Weitere Berechtigungen“ und aktiviere „Auf dem Sperrbildschirm anzeigen“ und „Neue Fenster im Hintergrund öffnen“. |
| Alarm über anderen Apps anzeigen | Auf Android TV nötig, damit der Alarm über der laufenden Wiedergabe erscheint. Tippen und SwitchGuard erlauben. |
| Start im Hintergrund (Xiaomi, Huawei, OPPO, vivo…) | Erscheint auf Handys, die den Hintergrundstart blockieren (Xiaomi, Huawei, OPPO, vivo…): damit startet die Überwachung nach Neustart oder Update wieder. Siehe Fehlerbehebung. |

### Einstellungen → Standort und Zonen

| Einstellung | Wozu |
|---|---|
| Standort | Erforderlich für Regeln zu Sonnenaufgang/-untergang und Außentemperatur. Er bleibt auf diesem Gerät; für die Temperatur wird er auf etwa 100 m gerundet an Open-Meteo gesendet. |
| Zonen | Wähle einen Bereich auf der Karte; Automationen laufen, wenn dein Handy ihn betritt oder verlässt (z. B. das Gartenlicht einschalten, wenn du nach Hause kommst, alles ausschalten, wenn du gehst). Kartenbilder werden von OpenStreetMap geladen; dein Standort wird nur auf diesem Handy verwendet. |

### Einstellungen → Standby-Bildschirm

| Einstellung | Wozu |
|---|---|
| Standby-Bildschirm öffnen | Tippe auf den Bildschirm für „App öffnen“ und „Schließen“ |
| Als Bildschirmschoner verwenden | Öffnet die Android-Einstellungen: Wähle unter Bildschirmschoner SwitchGuard und stelle ein, dass er beim Laden startet. Die Tafel ersetzt dann am Ladegerät den Sperrbildschirm. |
| Nachts abdunkeln | Der Text blendet zwischen 23:00 und 07:00 ab |

### Einstellungen → Tageszusammenfassung

| Einstellung | Wozu |
|---|---|
| Benachrichtigung mit Tageszusammenfassung | Wie oft sich jedes Gerät heute eingeschaltet hat, wie lange es an war und wie viel Energie es verbraucht hat |
| Senden um | Uhrzeit, zu der die Zusammenfassung kommt. |
| Heutige Zusammenfassung jetzt anzeigen | Zeigt sie sofort. |

### Einstellungen → Sicherung & Übertragung

| Einstellung | Wozu |
|---|---|
| In Datei sichern | Regeln, Automationen, Einstellungen sowie die Tuya- und eWeLink-Schlüssel kommen in eine passwortgeschützte Datei |
| Aus Datei wiederherstellen | Neues Handy? Wähle die Sicherungsdatei und gib ihr Passwort ein |
| An anderes Gerät senden | Tuya-Schlüssel, Regeln und Einstellungen auf einen Fernseher oder ein Tablet im selben WLAN kopieren |
| Von anderem Gerät empfangen | Die Einrichtung per WLAN vom Handy holen |

Die WLAN-Übertragung sendet nie die eWeLink-API-Schlüssel oder die eWeLink-Sitzung: Gib auf dem anderen Gerät dessen eigene API-Schlüssel ein und melde dich mit dessen eigenem eWeLink-Konto an. Eine Datei-Sicherung enthält dagegen die eWeLink- und Tuya-Schlüssel, geschützt durch dein Passwort.

### Einstellungen → Sprache

| Einstellung | Wozu |
|---|---|
| Sprache | Wähle die Sprache der App, der Benachrichtigungen und der Alarme. Handysprache oder eine von 20 Sprachen (ab 2.26). |

### Einstellungen → Hilfe & Info

| Einstellung | Wozu |
|---|---|
| Einrichtungsassistent | Die Ersteinrichtung erneut anzeigen |
| eWeLink-Einrichtungsanleitung | Schritt für Schritt mit Bildern (GitHub) |
| Tuya- / Smart-Life-Einrichtungsanleitung | Schritt für Schritt mit Bildern (GitHub) |
| Problembericht senden | Etwas stimmt nicht? Erstellt eine E-Mail mit Diagnosedaten. Es wird nichts geteilt, solange du sie nicht sendest. |
| Gespeicherte Absturzberichte löschen | Löscht die auf diesem Gerät gespeicherten Absturzberichte. |
| Kontakt & Feedback | Schreibt eine E-Mail an den Entwickler (Fragen, Fehler, Ideen). |
| Datenschutzerklärung | Öffnet die Datenschutzerklärung. |
| Info | Version, rechtlicher Hinweis. Die GitHub-Version prüft auf GitHub, ob es eine neuere gibt. |

### Warnungen

| Einstellung | Wozu |
|---|---|
| Gerätewarnungen | Wofür jedes Gerät läutet oder benachrichtigt |
| Bedingte Warnungen | Läuten, wenn ein Gerät eine Weile an oder aus bleibt oder sich seine Leistung ändert |
| Alarm ist bereit / Alarme erreichen dich vielleicht nicht | Die Bereitschaftskarte oben: listet Handy-Einstellungen, die den Alarm verhindern können, und öffnet den passenden Bildschirm zum Beheben; dazu ein Knopf „Testalarm“. |

### Warnungen → Alarm

| Einstellung | Wozu |
|---|---|
| Alarmton | Allgemeiner Alarmton. Jedes Gerät kann einen eigenen haben (Geräteseite). |
| Alarmlautstärke | Eigene Alarmlautstärke von SwitchGuard. |
| Vibration | Vibrieren, während der Alarm klingelt. |
| Mit voller Lautstärke läuten | Stellt die Alarmlautstärke des Handys auf Maximum, solange der Alarm läutet, und pausiert Musik; wird beim Beenden wiederhergestellt |
| Erneut läuten, wenn nicht behoben | Nachdem du einen Alarm beendet hast, läutet er in diesem Intervall erneut, solange das Gerät in dem Zustand bleibt, der ihn ausgelöst hat (weiterhin aus, weiterhin an oder offline). Endet, sobald das Gerät wieder normal ist. |
| Jemand anderen informieren | Telegram-Einrichtung (siehe unten). |
| Testalarm | Alarm jetzt ausprobieren: mit Ton oder lautlos, sofort oder nach 10 Sekunden |

### Warnungen → Warnkategorien

Jedes Gerät hat eine Warnkategorie. Solange du keine wählst, wird sie automatisch festgelegt: Ein überwachtes Gerät mit einem auf Alarm gesetzten Ereignis ist Kritisch, eines, das nur benachrichtigt, ist Wichtig, der Rest ist Normal. Bedingte Warnungen und Automationen können eine Kategorie als Auslöser verwenden (z. B. „Alarm läuten, wenn ein kritisches Gerät offline geht“).

| Einstellung | Wozu |
|---|---|
| Kritisch · Wichtig · Normal · Auto | Bei Automatik: Kritisch, wenn ein Ereignis einen Alarm auslöst, Wichtig, wenn Ereignisse nur benachrichtigen |
| Auch in den Ruhezeiten läuten | Alarme läuten auch in den Ruhezeiten / In den Ruhezeiten werden Alarme zu Benachrichtigungen |

### Warnungen → Ruhezeiten

| Einstellung | Wozu |
|---|---|
| Ruhezeiten | In diesen Stunden werden Alarme zu Benachrichtigungen (kritische Geräte läuten weiterhin) |
| Beginn / Ende | Beginn und Ende der Ruhezeiten. |

Geräte in Warnkategorien, die in den Ruhezeiten läuten sollen (standardmäßig Kritisch), läuten weiterhin.

### Warnungen → Jemand anderen informieren

Wird ein Alarm nicht rechtzeitig beendet, erhalten die von dir gewählten Personen eine Telegram-Nachricht und eine weitere, sobald er beendet wurde. Auch wenn dein Handy nachts in einem anderen Zimmer liegt, erfährt es jemand im Haus oder ein Nachbar. Die Nachrichten sendet ein Telegram-Bot, den du erstellst; es ist kein weiterer Server beteiligt.

| Einstellung | Wozu |
|---|---|
| 1. Telegram-Bot | Öffne in Telegram @BotFather, sende /newbot und gib deinem Bot einen Namen. Füge das Token, das BotFather dir gibt, unten ein. |
| 2. Zu informierende Personen | Bitte jede Person, in Telegram @your_bot zu öffnen und auf Start zu tippen (du kannst den Bot auch zu einer Familiengruppe hinzufügen). Tippe dann auf Personen suchen. |
| 3. Wann | Wie lange ein Alarm klingeln darf, bevor die Personen benachrichtigt werden (oder sofort). |
| Testnachricht senden | Sendet allen in der Liste eine Testnachricht. |
| Ausschalten | Schaltet die Telegram-Benachrichtigung aus. |

### Warnungen → Intelligente Warnungen

Warnungen, die alle deine Geräte gemeinsam betrachten: Stromausfälle, ungewöhnlicher Verbrauch und eine Wochenzusammenfassung. Mit Telegram kannst du außerdem den Status prüfen, einen Alarm übernehmen und Geräte aus der Ferne schalten.

| Einstellung | Wozu |
|---|---|
| Stromausfall: Stromausfälle erkennen | Gehen alle überwachten Geräte gleichzeitig offline, erhältst du eine einzige Warnung „Stromausfall“ statt einer pro Gerät und eine Nachricht mit der Dauer, wenn der Strom zurückkehrt. |
| Ladegerät als Stromsensor | Bleibt dieses Handy/Tablet am Standort angesteckt, zählt eine Minute ohne Ladegerät als Stromausfall; das funktioniert auch ohne Internet. Die Nachricht geht per Telegram über mobile Daten raus. |
| Zuhause / Abwesend: Sanftere Warnungen zu Hause | Zu Hause werden Alarme nicht kritischer Geräte zu Benachrichtigungen (wie in den Ruhezeiten). Kritische Geräte läuten immer. |
| Urlaubsmodus | Du giltst als abwesend, Ruhezeiten sind aus und jeden Morgen um 09:00 kommt ein Bericht. Er endet von selbst. |
| Verbrauch: Ungewöhnlicher Verbrauch | Vergleicht jeden Morgen gestern mit den vorherigen zwei Wochen. Viel höherer Verbrauch kann auf einen Defekt hindeuten; viel niedrigerer (ein Gefrierschrank, der kaum lief) darauf, dass es nicht mehr funktioniert. |
| Stromtarif | Der Standardpreis für Geräte ohne eigenen Preis. Beim Dreifachtarif wird jede Stunde in ihrem eigenen Band berechnet: Tag 06–17, Spitze 17–22, Nacht 22–06. |
| Monatsbudget | Du wirst einmal im Monat benachrichtigt, wenn die geschätzten Kosten der Geräte mit Verbrauchsmessung diesen Betrag erreichen. |
| Wochenbericht | Jeden Montag um 09:00: Ausfälle, Alarme und Reaktionszeit, Verbindungsabbrüche und Verbrauch der vergangenen Woche. Wird auch an Telegram gesendet, falls eingerichtet. |
| Bericht dieser Woche anzeigen | Zeigt sie sofort. |
| Telegram: Befehle und Schaltfläche „Verstanden“ | Personen aus deiner Liste können /status senden und einen läutenden Alarm übernehmen. Die Alarmnachricht erhält eine Schaltfläche „Verstanden“: Ein Druck darauf schaltet das Handy stumm. |
| Ein- und Ausschalten von Geräten erlauben | Befehle /on und /off. Nur Personen aus deiner Liste können sie nutzen. |
| Gestufte Benachrichtigung | Jede Person erhält nach ihrer eigenen Wartezeit eine Nachricht. Zum Beispiel ein Nachbar nach 5 Minuten, ein Verwandter nach 15, die Familiengruppe nach 30. Tippt jemand auf „Verstanden“, werden die nächsten Personen nicht benachrichtigt. |
| Anruf bei kritischem Alarm | Läutet ein Gerät der Kategorie Kritisch, wird die Person zusätzlich von der Leitung dieses Handys angerufen, sobald ihre Wartezeit in den gestuften Warnungen abgelaufen ist. Der Anruf weckt sie nur auf: Was passiert ist, steht in der gleichzeitig gesendeten Telegram-Nachricht (Android erlaubt Apps nicht, Ton in ein Telefonat einzuspielen). Bitte sie, diese Nummer als „SwitchGuard-Alarm“ zu speichern. Anrufe wiederholen sich alle 2 Minuten, bis zu 3 Mal, bis der Alarm beendet wird oder jemand in Telegram auf „Verstanden“ tippt. Gruppen können nicht angerufen werden. |
| Intelligente Warnungen an Telegram senden | Stromausfälle, ungewöhnlicher Verbrauch, die Budgetwarnung und der Wochenbericht werden auch in Telegram geschrieben. |
| Watchdog | Zwei Geräte mit demselben Bot überwachen sich gegenseitig (z. B. das Handy und eine TV-Box). Das sendende Gerät hält eine angeheftete Nachricht im Telegram-Chat aktuell; das überwachende Gerät löst einen Alarm aus und benachrichtigt deine Kontakte, wenn sie ausbleibt. Löse diese Nachricht nicht von der Anheftung. |

### Geräteseite (auf ein Gerät tippen)

| Einstellung | Wozu |
|---|---|
| Dieses Gerät überwachen | Änderungen werden aufgezeichnet und gemäß den Regeln unten gemeldet |
| Wenn dies passiert | Schaltet aus, Schaltet ein, Geht offline, Geht online → Alarm / Benachrichtigen / Keine. Alarm läutet, bis du ihn beendest. Benachrichtigen zeigt eine normale Benachrichtigung. Keine speichert das Ereignis nur im Verlauf. |
| Ton für dieses Gerät | Eigener Alarmton; sonst der allgemeine Ton. |
| Warnkategorie | Bei Automatik: Kritisch, wenn ein Ereignis einen Alarm auslöst, Wichtig, wenn Ereignisse nur benachrichtigen |
| Energie | Leistung, Spannung, Strom; heute und diesen Monat; Verlauf nach Tag / Monat / Jahr mit Kosten; Bericht (PDF / CSV). |
| Preis pro kWh | kWh-Preis für dieses Gerät oder der Standardpreis; kann auch als Standard gesetzt werden. |
| Timer (auf dem Gerät) | Keine Timer. Timer laufen auf dem Gerät selbst, auch bei ausgeschaltetem Handy. (eWeLink-Geräte: Ein/Aus-Timer zu einer Uhrzeit an gewählten Tagen hinzufügen oder bearbeiten.) |
| Verbindung | dBm ist die Signalstärke: je näher an 0, desto besser. Über -55 ausgezeichnet, bis -67 gut, bis -75 mittelmäßig, darunter schwach. |
| Standby-Verbrauch | Der niedrigste Stundendurchschnitt der letzten 7 Tage: die Leistung, die dieses Gerät (und alles, was daran angeschlossen ist) nie unterschreitet, auch wenn es „nichts tut“. Fernseher, Router und Ladegeräte ziehen im Standby oft so viel. |
| Gerätedaten kopieren | Kopiert die Rohdaten des Geräts in die Zwischenablage, nützlich, um ein nicht unterstütztes Modell zu melden |

## Telegram-Befehle

Personen aus deiner Liste können /status senden und einen läutenden Alarm übernehmen. Die Alarmnachricht erhält eine Schaltfläche „Verstanden“: Ein Druck darauf schaltet das Handy stumm.

```
SwitchGuard-Befehle:
/status – Überwachung, Alarm und Geräte
/ack – den läutenden Alarm übernehmen
/on Name [Kanal] – ein Gerät oder eine Gruppe einschalten (mehrere: /on kitchen, garden)
/off Name [Kanal] – ein Gerät oder eine Gruppe ausschalten
/watt [Name] – aktuelle und heutige Leistung sowie üblicher Verbrauch
/temp – Außentemperatur
/scene Name – eine Szene starten
/home · /away – Zuhause / Abwesend setzen

Du kannst auch frei schreiben (auf Englisch oder Türkisch): „turn off kitchen and garden“, „heater watts“, „is the pump on“.

Das Ein- und Ausschalten von Geräten und Szenen muss in der App erlaubt sein (Warnungen → Intelligente Warnungen).
```

Englische und türkische Befehlsnamen funktionieren beide (z. B. /status = /durum, /on = /ac). Befehle werden nur aus den ausgewählten Chats angenommen.

## Berechtigungen und warum

| Berechtigung | Warum |
|---|---|
| `POST_NOTIFICATIONS` | Alarme, Benachrichtigungen und den Überwachungsstatus anzeigen. |
| `FOREGROUND_SERVICE` | Die Überwachung im Hintergrund laufen lassen (dauerhafte Statusbenachrichtigung). |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Ausnahme von der Akku-Optimierung anfragen, damit Alarme nicht verzögert werden. |
| `USE_FULL_SCREEN_INTENT` | Den Alarm im Vollbild über dem Sperrbildschirm öffnen. |
| `SYSTEM_ALERT_WINDOW` | Den Alarm über anderen Apps zeigen (Android TV, Xiaomi). |
| `WAKE_LOCK` | Den Prozessor wach halten, während Ereignisse verarbeitet werden („Maximale Zuverlässigkeit“). |
| `VIBRATE` | Vibration beim Alarm. |
| `RECEIVE_BOOT_COMPLETED` | Die Überwachung nach einem Neustart des Handys wieder starten. |
| `INTERNET` | Mit der eWeLink- / Tuya-Cloud sprechen (und mit Telegram / Open-Meteo / OpenStreetMap, falls genutzt). |
| `ACCESS_COARSE_LOCATION` | Nur wenn du „Standort dieses Geräts verwenden“ für Sonnenaufgang/-untergang- und Temperaturregeln tippst. |
| `ACCESS_FINE_LOCATION` | Zonen: ein genauer Bereich für Ankunfts- / Verlassen-Regeln. |
| `ACCESS_BACKGROUND_LOCATION` | Zonen: Ankunft und Verlassen erkennen, während die App geschlossen ist (nur gefragt, wenn du eine Zone anlegst). |
| `CALL_PHONE` | „Anruf bei kritischem Alarm“: die eingegebene Nummer über die Leitung dieses Handys anrufen. |

## Datenschutz

- Kein eigener Server, keine Analyse, keine Werbung, kein Tracking. Nichts wird verkauft oder weitergegeben.
- Schlüssel, Tokens, Geräte, Regeln, Einstellungen und Verlauf (max. 2.000 Ereignisse) werden nur auf deinem Gerät gespeichert.
- Die App spricht nur mit den verbundenen Clouds (eWeLink `*.coolkit.cc` / `*.coolkit.cn`, Tuya `openapi.tuya*.com`) und, nur wenn du sie nutzt, mit Telegram, Open-Meteo (Temperatur und Wetter, Standort auf ca. 100 m gerundet) und OpenStreetMap-Kartenkacheln.
- Dein eWeLink-Passwort gibst du auf der eigenen Seite von eWeLink ein; die App sieht es nie.
- App-Daten sind von der Android-Cloud-Sicherung ausgeschlossen; Datei-Sicherungen sind passwortgeschützt; die WLAN-Übertragung ist mit einem Einmalcode verschlüsselt und bleibt im Heimnetz.
- Pro-Käufe wickelt Google Play ab.

Vollständige Datenschutzerklärung: [Google Sites](https://sites.google.com/view/switchguard-privacy) · [docs/privacy-policy.md](docs/privacy-policy.md)

## Fehlerbehebung und FAQ

### Der Alarm kommt zu spät oder gar nicht

| Problem | Was tun |
|---|---|
| Hintergrundaktivität ist eingeschränkt | Das Handy kann die Überwachung in den Ruhezustand versetzen; Alarme können sich verspäten oder ausbleiben. |
| Alarm im Vollbild ist nicht erlaubt | Bei gesperrtem Handy kann der Alarm seinen großen Bildschirm nicht öffnen. |
| Xiaomi blockiert den Alarmbildschirm | Bei gesperrtem Handy kann sich der Alarmbildschirm nicht öffnen. Tippe auf „Beheben“, öffne „Weitere Berechtigungen“ und aktiviere „Auf dem Sperrbildschirm anzeigen“ und „Neue Fenster im Hintergrund öffnen“. |
| Alarm-Benachrichtigungen sind heruntergeregelt | Die Benachrichtigungskategorie „Alarme“ hat keine hohe Priorität, daher erscheint der Alarm nicht als Pop-up und füllt den Bildschirm nicht aus. |
| Bitte nicht stören: völlige Stille | In diesem Modus werden sogar Alarmtöne stummgeschaltet. |
| Alarmlautstärke ist sehr niedrig | Die Alarmlautstärke des Handys ist null oder fast null, daher hörst du SwitchGuard eventuell nicht. „Beheben“ aktiviert „Mit voller Lautstärke läuten“. |
| Benachrichtigungen sind aus | Alarme und Warnungen können nicht angezeigt werden. |
| Auf diesem Handy gibt es ein zweites SwitchGuard | Eine weitere Kopie von SwitchGuard wurde mit der Funktion „Dual Apps“ (App-Klon) des Handys erstellt. Zwei nebeneinander laufende Kopien läuten jeden Alarm doppelt und melden sich gegenseitig ab, wenn sie dasselbe eWeLink-Konto verwenden; auf Xiaomi-Handys können die Berechtigungen dieser Kopie außerdem als ausgeschaltet erscheinen. Entferne die zweite Kopie: Halte ihr Symbol (das mit dem kleinen Abzeichen) gedrückt und tippe auf „Deinstallieren“. |

### Hersteller, die Hintergrund-Apps stoppen

| Handy | Was tun |
|---|---|
| Xiaomi | Nötig, damit die Überwachung nach einem Neustart oder Update wieder startet. Tippe und aktiviere den Autostart für SwitchGuard. |
| Samsung | Samsung kann die App in den Ruhezustand versetzen. Wähle unter Akku "Uneingeschränkt" und füge SwitchGuard zu "Apps, die nie in den Ruhezustand wechseln" hinzu (Akku → Hintergrundnutzungsgrenzen). |
| Huawei / Honor | Stelle unter App-Start SwitchGuard auf "Manuell verwalten" und aktiviere Autostart, Sekundärstart und Ausführung im Hintergrund. |
| OPPO / realme / OnePlus | Aktiviere den Autostart für SwitchGuard und erlaube dann in den Akkueinstellungen der App die Hintergrundaktivität. |
| vivo / iQOO | Erlaube SwitchGuard den Start im Hintergrund und einen hohen Stromverbrauch im Hintergrund. |
| Tecno / Infinix | Aktiviere in Phone Master den Autostart für SwitchGuard, damit die Überwachung Neustarts und Bereinigungen übersteht. |
| ASUS | Erlaube SwitchGuard im Autostart-Manager, damit die Überwachung nach einem Neustart wieder startet. |

### Weitere Fragen

**Die Überwachung ist gestoppt und meldet, ich sei abgemeldet.**  
Tipp: eWeLink erlaubt nur eine Sitzung pro Konto. Gib jedem Handy ein eigenes eWeLink-Konto und teile die Geräte über die eWeLink-App.

**Ein mit mir geteiltes Tuya-Gerät fehlt.**  
Ein mit dir geteiltes Gerät fehlt? Tuya listet nur die Geräte in deinen Homes auf. Bitte den Besitzer, dich in Smart Life als Mitglied hinzuzufügen (Ich → Home-Verwaltung → Mitglied hinzufügen).

**Tuya-Geräte aktualisieren sich nicht mehr.**  
Die Tuya-Cloud-Testphase ist abgelaufen, daher werden Tuya-Geräte nicht überwacht. Verlängere sie unter platform.tuya.com → Cloud → IoT Core → Extend trial period.

**Änderungen kommen verspätet / Sofort-Updates sind nicht verfügbar.**  
Sofort-Updates sind für diese App ID nicht verfügbar; es werden periodische Prüfungen verwendet (Änderungen, die kürzer als das Intervall dauern, können verpasst werden)

**Kann ich zwischen der Google-Play- und der GitHub-Version wechseln?**  
Ja, aber deinstalliere vorher die andere: Sie sind unterschiedlich signiert. Vor dem Deinstallieren in eine Datei sichern und danach wiederherstellen.

**Wie teste ich den Alarm, ohne alle zu wecken?**  
Alarm jetzt ausprobieren: mit Ton oder lautlos, sofort oder nach 10 Sekunden

**Warum weicht der Verbrauch von der eWeLink- / Smart-Life-App ab?**  
Der Verbrauch wird von SwitchGuard aus Leistungsmesswerten berechnet, die bei eingeschalteter Überwachung erfasst werden, und kann daher leicht von der App des Geräteherstellers (eWeLink, Smart Life) abweichen.

## Windows-App

SwitchGuard für Windows (10 / 11) überwacht genauso auf dem PC: Es sitzt im Infobereich, löst einen Vollbild-Alarm am Computer aus und teilt dieselben Regeln, Automatisierungen und Telegram-Funktionen. Zonen, Widgets, die Uhr und Telefonanrufe gibt es nur am Handy.

### Funktionen

- Dieselben Tabs: Geräte, Warnungen, Automationen, Verlauf, Chat, Einstellungen; Sprachbefehle im Chat nutzen die Windows-Spracheingabe.
- Infobereich-Symbol: öffnen, Überwachung starten / stoppen, Alarm beenden, beenden; das Schließen des Fensters stoppt die Überwachung nicht.
- Vollbild-Alarmfenster, eingebaute Töne (Sirene, Piepton, Puls, Glockenspiel) oder eigene WAV / MP3.
- Standby-Bildschirm und Windows-Bildschirmschoner: Das Board öffnet sich, wenn der PC im Leerlauf ist.
- Ein am Netzteil gelassener Laptop dient als Stromausfall-Sensor.
- Updates von GitHub mit einem Klick; Einstellungen und Lizenz bleiben. Sicherung in eine passwortgeschützte Datei (nicht auf einem Handy wiederherstellbar).

### Nur unter Windows

| Einstellung | Wozu |
|---|---|
| Windows | Damit SwitchGuard auf diesem Computer ohne Unterbrechung wacht. Ist eines davon aus, können Alarme nach einem Neustart oder im Ruhezustand verpasst werden. |
| Mit Windows starten | Startet im Infobereich, wenn du dich bei Windows anmeldest |
| Computer wach halten | Während der Überwachung geht der Computer nicht in den Ruhezustand |
| Als Windows-Bildschirmschoner verwenden | Ist der Computer inaktiv, öffnet sich der Standby-Bildschirm; Mausbewegung oder Tastendruck schließt ihn |
| Wartezeit | Wie lange der Computer inaktiv sein muss, bevor sich der Standby-Bildschirm öffnet |
| Windows-Bildschirmschoner-Einstellungen | Z. B. „Anmeldeseite bei Reaktivierung“ |
| Standby-Bildschirm öffnen | Vollbild; Esc schließt, Klick für „App öffnen“. Der Bildschirm bleibt an, solange es offen ist. |
| Sprache & Darstellung: Design | Hell, dunkel oder wie Windows. |
| Sprache | Wähle die Sprache der App, der Benachrichtigungen und der Alarme. |
| Tuya-Live-Updates auf diesem Computer | Aus: Tuya-Geräte werden alle 5 Minuten geprüft |
| Stromausfall, wenn dieser Computer auf Akku läuft | Für einen zu Hause eingesteckten Laptop: Läuft er eine Minute auf Akku, gilt das als Stromausfall, auch ohne Internet (die Telegram-Nachricht geht raus, sobald die Verbindung zurück ist). |
| Dieser Computer hängt am selben Strom wie die Geräte | Solange er am Netz läuft, gibt es keinen Stromausfall; ein ausfallendes Gerät wird daher sofort gemeldet statt nach bis zu 90 Sekunden. |
| Aus Datei laden… | Eigene WAV- oder MP3-Datei für Alarm oder Benachrichtigung (erste 60 Sekunden, max. 20 MB). |
| In Datei sichern… | Alle Einstellungen, Kontoschlüssel und Töne in einer passwortgeschützten Datei |
| Pro aktivieren | Ohne Lizenz überwacht die App 1 Gerät. Gib deinen Lizenzschlüssel ein, um alle Geräte zu überwachen. |

### Installation unter Windows

1. Doppelklicke auf die Setup-Datei (SwitchGuard-…-Kurulum.exe). Sie fragt nicht nach Administratorrechten und installiert in wenigen Sekunden.
2. Meldet Windows „Der Computer wurde durch Windows geschützt“ (SmartScreen), drücke Weitere Informationen → Trotzdem ausführen. Die App ist noch nicht signiert; deshalb die Warnung.
3. SwitchGuard wird in deinem Benutzerordner installiert (AppData › Local › SwitchGuard) und legt Verknüpfungen auf dem Desktop und im Startmenü an.
4. Es startet beim Anmelden bei Windows von selbst im Infobereich (neben der Uhr). Das Schließen des Fensters beendet es nicht; zum Beenden Infobereich-Symbol → Beenden.
5. Füge deine Konten unter Einstellungen → Konten hinzu (die Schritte für Tuya und eWeLink stehen unten).
6. Umzug auf einen anderen Computer: auf dem alten Einstellungen → Sicherung & Übertragung → In Datei sichern; auf dem neuen installieren, Aus Datei wiederherstellen, dann erneut bei eWeLink anmelden.
7. Deinstallieren: Windows-Einstellungen → Apps → Installierte Apps → SwitchGuard → Deinstallieren.
8. Neue Version installieren: erst die alte deinstallieren (Schritt 7), dann die neue Setup-Datei ausführen. Einstellungen, Konten und Töne bleiben erhalten.

## Bauen

```
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest      # unit tests
```
Release-Builds brauchen `keystore.properties` + Keystore im Projektordner (nicht im Repository).

## Änderungsprotokoll

[English](CHANGELOG.md) · [Türkçe](CHANGELOG.tr.md) · [Releases](https://github.com/Macerce/switchguard/releases)

## Kontakt

Fragen, Fehlermeldungen und Feedback (oder hier ein Issue öffnen): **switchguardapp@gmail.com**

## Lizenz

SwitchGuard ist **source-available** (Quellcode einsehbar), nicht Open Source. Quellcode: [PolyForm Noncommercial 1.0.0](LICENSE). Private und andere nicht-kommerzielle Nutzung ist frei; den Code zu verkaufen oder kommerziell zu nutzen ist nicht erlaubt. Versionen bis einschließlich 2.8.3 erschienen unter GPL-3.0.

Offizielle Builds (Google Play, [Releases](https://github.com/Macerce/switchguard/releases)) darf jeder nutzen, auch Unternehmen. Veränderte Versionen dürfen Name und Logo von SwitchGuard nicht verwenden. Kommerzielle Lizenzen: **switchguardapp@gmail.com**

Steht in keiner Verbindung zu eWeLink, CoolKit oder ITEAD und wird von diesen weder unterstützt noch gesponsert. Sonoff und eWeLink sind Marken ihrer jeweiligen Inhaber. Auch keine Verbindung zu Tuya.
