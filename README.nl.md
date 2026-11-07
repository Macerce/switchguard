[English](README.md) · [Türkçe](README.tr.md) · [Deutsch](README.de.md) · [Español](README.es.md) · [Português (Brasil)](README.pt-BR.md) · [Français](README.fr.md) · [Italiano](README.it.md) · **Nederlands** · [Polski](README.pl.md) · [Русский](README.ru.md) · [Українська](README.uk.md) · [العربية](README.ar.md) · [हिन्दी](README.hi.md) · [Bahasa Indonesia](README.id.md) · [Tiếng Việt](README.vi.md) · [ไทย](README.th.md) · [简体中文](README.zh-CN.md) · [繁體中文](README.zh-TW.md) · [日本語](README.ja.md) · [한국어](README.ko.md)

# SwitchGuard

SwitchGuard bewaakt de slimme stekkers en schakelaars in je eigen **eWeLink / SONOFF**- en **Tuya / Smart Life**-accounts en laat een echt alarm afgaan zodra er iets verandert — een pomp die stilvalt, een vriezer die uitgeschakeld is, een apparaat zonder verbinding. Werkt op Android-telefoons, tablets, Android TV en Wear OS-horloges, en er is ook een Windows-app.

<p>
  <img src="docs/screenshots/play/en/v215/01-devices.png" width="200" alt="Apparaten">
  <img src="docs/screenshots/play/en/v215/02-energy.png" width="200" alt="Energie">
  <img src="docs/screenshots/play/en/v215/05-automations.png" width="200" alt="Automatiseringen">
  <img src="docs/screenshots/play/en/v215/08-alarm.png" width="200" alt="Apparaatalarm">
</p>

> **Talen:** de Windows-app is vanaf 0.13.0 beschikbaar in 20 talen. Op Android komt de taalkeuze in de app (20 talen) met **2.26**; de nieuwste Android-versie is 2.25.0.

## Downloaden

### Android (telefoon, tablet, tv)
- **Google Play** (gesloten test): https://play.google.com/apps/testing/com.macerce.switchguard
- **APK**: [nieuwste Android-versie](https://github.com/Macerce/switchguard/releases/latest) — download `SwitchGuard-x.y.z.apk` en open het op de telefoon. Is SwitchGuard al via Google Play geïnstalleerd, verwijder het dan eerst (andere handtekening). De GitHub-versie meldt het als er een nieuwere is.

### Windows 10 / 11
- **Installatieprogramma**: [Windows-versies](https://github.com/Macerce/switchguard/releases?q=Windows&expanded=true) — download `SwitchGuard-x.y.z-Kurulum.exe` van de nieuwste en voer het uit. De app zoekt zelf naar nieuwe versies en werkt zich met één klik bij; instellingen blijven bewaard.

### Gratis en Pro
- Gratis: alle functies, voor 1 bewaakt apparaat. Met demo-apparaten probeer je alles zonder hardware.
- Android: Pro is een eenmalige aankoop in Google Play (geen abonnement) en bewaakt alle apparaten; het verhuist met je Google-account mee naar een nieuwe telefoon.
- Windows: een Pro-licentiesleutel (Instellingen → abonnementskaart) bewaakt alle apparaten. Voor een sleutel: **switchguardapp@gmail.com**

## Vereisten

- Android 8.0+ (API 26); Wear OS voor de horloge-app
- eWeLink: een gratis eWeLink-ontwikkelaarsapp (App ID + App Secret) — de handleiding in de app helpt stap voor stap. eWeLink staat één sessie per account en App ID toe, dus elke telefoon, tablet, tv of pc heeft een eigen eWeLink-account nodig waarmee de apparaten gedeeld zijn ([dev.ewelink.cc](https://dev.ewelink.cc))
- Tuya / Smart Life: de Access ID en Access Secret van je eigen gratis Tuya Cloud-project, gekoppeld aan je Smart Life- / Tuya-appaccount ([platform.tuya.com](https://platform.tuya.com))
- Windows 10 / 11 (64-bit) voor de Windows-app

## Installatiehandleidingen

- eWeLink (SONOFF): [English](docs/guide/ewelink.en.md) · [Türkçe](docs/guide/ewelink.tr.md)
- Tuya / Smart Life: [English](docs/guide/tuya.en.md) · [Türkçe](docs/guide/tuya.tr.md)

### Video's

- **New in 2.17 – alert categories, critical alerts, groups (EN):** https://youtube.com/shorts/iVM3zEiBt8A
- **2.17 yenilikleri – uyarı kategorileri, kritik uyarılar, gruplar (TR):** https://youtube.com/shorts/Qha83ziwylI
- New in 2.15 – smart alerts, scenes, energy reports (EN): https://youtube.com/shorts/bOtFBjHoefE
- 2.15 yenilikleri – akıllı uyarılar, sahneler, enerji raporu (TR): https://youtube.com/shorts/y3QRZvVoeK4
- New in 2.12 – a tour of the app (EN): https://youtube.com/shorts/jvm0hx0gATk
- 2.12 yenilikleri – uygulama turu (TR): https://youtube.com/shorts/2KeUgJrCCEU
- First setup in one minute / İlk kurulum (1 dk): https://youtube.com/shorts/jarBmMg2kYA
- **Getting eWeLink API keys / eWeLink API anahtarlarını alma (TR, sesli):** https://youtu.be/G5vQ_7CXTcM
- **Getting Tuya API keys / Tuya API anahtarlarını alma (TR, sesli):** https://youtu.be/8vPjJMoMaMU

## Functies

### Bewaking en alarm

- Live-updates via de eWeLink-WebSocket en de Tuya-berichtendienst, met HTTP-hersynchronisatie en periodieke controles als terugval.
- Het alarm blijft afgaan tot je het uitzet — ook in de stille modus — en opent schermvullend over het vergrendelscherm; elk apparaat kan een eigen geluid hebben.
- Regels per apparaat: alarm, melding of niets wanneer een apparaat uitgaat, aangaat, offline gaat of weer online komt.
- Voorwaardelijke waarschuwingen: alarm als een apparaat een tijd aan of uit blijft, of als het vermogen boven / onder een grens komt (bijv. een pomp onder 50 W).
- Korte verbindingsonderbrekingen negeren, waarschuwing als de bewaking zelf stopt (geen internet / cloud), luide waarschuwing als de eWeLink-sessie verloren gaat (bijv. hetzelfde account op een andere telefoon ingelogd).
- Een gereedheidskaart toont en herstelt telefooninstellingen die het alarm kunnen tegenhouden (batterijbeperking, schermvullende toestemming, Niet storen, alarmvolume…).

### Apparaten

- Tegels in smart-home-stijl met snelknoppen, één knop per kanaal op meerkanaalsapparaten, live status en vermogen.
- Apparaatpagina: regels, eigen alarmgeluid, waarschuwingscategorie, energiegeschiedenis en kosten, eWeLink-timers, wifi-signaal en diagnose van onderbrekingen, sluimerverbruik en recente gebeurtenissen.
- **Apparaten indelen**: Ster = bovenaan houden · pijlen = verplaatsen · kaartgrootte = klein of groot · label = categorie. Houd een kaart lang ingedrukt om hier terug te komen.

### Waarschuwingen

- Apparaatwaarschuwingen en voorwaardelijke waarschuwingen op één plek, met filter op waarschuwingscategorie (Kritiek / Belangrijk / Normaal).
- Waarschuwingsinstellingen: alarmgeluid en volume, stille uren, waarschuwingscategorieën, Telegram (‘Iemand anders waarschuwen’) en slimme waarschuwingen — zie *Alle instellingen* hieronder.
- Slimme waarschuwingen: één waarschuwing bij een stroomstoring, ongewoon verbruik, maandbudget, weekrapport, vakantiemodus.

### Automatiseringen

- **Regels** — automatische acties: *Als* er iets gebeurt, *dan* iets doen, met optionele vertraging.
- Triggers: aangaat, uitgaat, aan blijft gedurende, uit blijft gedurende, offline gaat, weer online komt, vermogen boven, vermogen daalt onder, bij zonsopgang, bij zonsondergang, wanneer de buitentemperatuur stijgt boven, wanneer de buitentemperatuur daalt onder, bij aankomst, bij vertrek.
- Acties: Aanzetten, Uitzetten, Alarm laten afgaan, Melding sturen, Scène uitvoeren, Cyclus starten, Cyclus stoppen.
- **Alleen wanneer**: Alleen tijdens deze uren, En alleen als een ander apparaat…, Alleen als ik thuis / weg ben.
- **Scènes** — Kies wat er met elk apparaat gebeurt als de scène wordt uitgevoerd. Apparaten die op “Overslaan” blijven staan, worden niet aangeraakt.
- **Groepen** — Kies de apparaten in de groep. De groep verschijnt op het scherm Apparaten als een apparaat: met de knop zet je ze allemaal tegelijk aan of uit, en je kunt hem in automatiseringen gebruiken.
- **Cycli & timers** — Aftellen, Aan, dan uit, Aan/uit-cyclus, Een voor een, Eigen stappen. Start met een knop, op een vast tijdstip op gekozen dagen of tijdens een tijdvenster; optionele willekeurige variatie (bewoonde indruk) en ‘stoppen bij handmatige wijziging’.
- Automatiseringen draaien op deze telefoon zolang de bewaking aan staat. Gebruik voor schema's die ook moeten werken als de telefoon uit staat de timers op de apparaatpagina.

### Energie

- Weergave per dag / maand / jaar met totaal, geschatte kosten, gemiddeld en piekvermogen, drukste uur/dag/maand en een overzicht van opvallende punten.
- Enkel- of drievoudig tarief (dag / piek / nacht), prijs per apparaat of standaardprijs.
- Sluimerverbruik (het vermogen waar een apparaat nooit onder komt) en de maandkosten ervan.
- Energierapport als PDF (om te printen of delen) of CSV (Excel, Google Spreadsheets).
- Het verbruik wordt door SwitchGuard berekend uit vermogensmetingen die zijn gedaan terwijl de bewaking aan stond, dus het kan licht afwijken van de app van de fabrikant (eWeLink, Smart Life).

### Geschiedenis

- Elke gebeurtenis met datum en tijd, en wie of wat het veroorzaakte (app, automatisering); filter per apparaat.
- Exporteren naar CSV (laatste 24 uur, 7 dagen, 30 dagen of alles) en geschiedenis wissen.

### Chat

- Hallo. Je kunt je apparaten beheren door te schrijven: aan- of uitzetten, hun status, vermogensgebruik of de buitentemperatuur opvragen. Tik op de microfoon om het in te spreken (“Zet keuken uit”); het antwoord wordt voorgelezen. Hieronder staan kant-en-klare sjablonen; tik op een sjabloon om het te versturen, houd het ingedrukt om het te bewerken.
- Opdrachten in vrije tekst werken offline en begrijpen Engels en Turks; de microfoon gebruikt de spraakherkenning van de telefoon en antwoorden kunnen worden voorgelezen.
- Sjablonen: berichten met één tik die ook automatisch kunnen draaien, elke dag op een tijdstip of als er iets gebeurt.

### Stand-byscherm

- Een bord op volledig scherm voor een telefoon of tablet aan de oplader: klok, weer, kritieke status, live vermogen, recente activiteit en wat er hierna draait. Het verschijnt ook over het vergrendelscherm.
- Opent de Android-instellingen: kies SwitchGuard onder Screensaver en stel in dat die start tijdens het opladen. Het bord vervangt dan het vergrendelscherm zolang het aan de oplader ligt.

### Widgets, snelinstellingstegel, snelkoppelingen

- **SwitchGuard** — Toont of de bewaking actief is en of er een alarm afgaat
- **SwitchGuard-schakelaar** — Zet één apparaat of kanaal met een tik aan en uit. Kan ook worden ingesteld op Thuis / Weg of om een scène te starten.
- **SwitchGuard-groep** — Grote tegel: zet elk apparaat in een groep afzonderlijk aan of uit
- **SwitchGuard-spraakcommando** — Eén tegel: tik, zeg het (“Zet de keuken uit”) en het wordt uitgevoerd
- **Snelinstellingstegel** — toont de bewakingsstatus en start of stopt de bewaking (de telefoon moet eerst ontgrendeld zijn); terwijl een alarm afgaat, opent hij de app.
- **App-snelkoppelingen** — houd het app-pictogram ingedrukt voor Thuis / Weg en je scènes.

### Thuis / Weg

- **Thuis / Weg** — Regels die zijn gemarkeerd als “alleen als ik weg ben” worden niet uitgevoerd. Schakel over naar Weg als je vertrekt (ook via de widget, het horloge of Telegram /away).
- **Vakantiemodus** — Je telt als weg, stille uren staan uit en elke ochtend om 09:00 komt er een rapport. Het eindigt vanzelf.
- **Zones** — Kies een gebied op de kaart; automatiseringen draaien wanneer je telefoon het binnengaat of verlaat (bijv. de tuinlamp aanzetten als je thuiskomt, alles uitzetten als je weggaat). Kaartafbeeldingen worden gedownload van OpenStreetMap; je locatie wordt alleen op deze telefoon gebruikt.

### Wear OS

- De SwitchGuard-horloge-app toont alarmen (uitzetten vanaf je pols), apparaten (schakelen), scènes en Thuis / Weg; hij verbindt met de telefoon-app.

### Android TV

- Werkt op Android TV met de afstandsbediening; het alarm verschijnt bovenop wat er speelt (‘Alarm over andere apps tonen’).
- ‘Invoeren vanaf telefoon’: de tv toont een QR-code; je typt de sleutels op je telefoon en ze gaan versleuteld via je thuiswifi naar de tv.
- ‘Van een ander apparaat ontvangen’: kopieer Tuya-sleutels, regels en instellingen via wifi van je telefoon.

### Meerdere apparaten

Telefoon, tablet, Android TV en pc: **elk apparaat heeft een eigen eWeLink-account nodig met goedgekeurde ontwikkelaarsfunctie, en de eigen API-sleutels van dat account (App ID / Secret)** — eWeLink staat één ingelogd apparaat per account toe. Deel je apparaten in de eWeLink-app met het extra account. Tuya-sleutels kun je op meerdere apparaten gebruiken. Pro koop je één keer en werkt op alle Android-apparaten met hetzelfde Google-account. Details: [English](docs/guide/ewelink.en.md#using-several-devices-phone-tablet-android-tv) · [Türkçe](docs/guide/ewelink.tr.md#birden-fazla-cihazda-kullanım-telefon-tablet-android-tv)

### Demo & Pro

- **Demo-apparaten** — Virtuele apparaten om alle functies uit te proberen. Deze werken alsof er iets met het apparaat zelf gebeurde (iemand schakelde het, de stroom of wifi viel uit), dus je regels gelden en het alarm kan afgaan. Schakelen met de knoppen hierboven telt als jijzelf en laat het alarm niet afgaan.
- De gratis versie bewaakt 1 apparaat. Pro bewaakt al je apparaten, met alle functies. Eenmalige betaling, geen abonnement. Gekoppeld aan je Google-account, dus het gaat mee naar een nieuwe telefoon.

## Alle instellingen

Alles in de volgorde van de app. Het tabblad **Instellingen** bevat verbindingen en app-opties; alarm, stille uren, Telegram en slimme waarschuwingen staan onder tabblad **Waarschuwingen** → Waarschuwingsinstellingen; opties per apparaat staan op de pagina van dat apparaat.

### Instellingen → Accounts

| Instelling | Waarvoor |
|---|---|
| Pro / Pro kopen | De gratis versie bewaakt 1 apparaat. Pro bewaakt al je apparaten, met alle functies. Eenmalige betaling, geen abonnement. Gekoppeld aan je Google-account, dus het gaat mee naar een nieuwe telefoon. |
| Herstellen | Controleert je Google Play-aankopen opnieuw (nieuwe telefoon, herinstallatie). |
| eWeLink — Inloggen met eWeLink | Opent de eigen inlogpagina van eWeLink; de app ziet je wachtwoord nooit. |
| API-gegevens | App ID, App Secret en regio van je eWeLink-ontwikkelaarsapp. |
| Uitloggen | De bewaking stopt. Je instellingen en geschiedenis blijven bewaard. |
| Tuya / Smart Life | Voer de Access ID en het Access Secret van je Tuya Cloud-project in (platform.tuya.com → Cloud → je project → Overview). Het account van je Smart Life / Tuya-app moet aan het project gekoppeld zijn. Data center: Centraal-Europa, West-Amerika, China, India |
| Verbinding verbreken | Verwijdert de Tuya-verbinding; Tuya-apparaten worden niet meer bewaakt. |
| Demo-apparaten | Virtuele apparaten om alle functies uit te proberen |

### Instellingen → Bewaking

Hoe SwitchGuard op de achtergrond je apparaten in de gaten houdt.

| Instelling | Waarvoor |
|---|---|
| Maximale betrouwbaarheid | Houdt de processor wakker zodat geen enkele gebeurtenis vertraagd wordt. Verbruikt meer batterij. |
| Korte verbindingsonderbrekingen negeren | Wacht zo lang voordat een offline apparaat wordt gemeld. Als het op tijd terugkomt, volgt er geen melding. |
| Waarschuwen als de bewaking stopt | Zo lang geen internet of geen cloudverbinding: je krijgt een waarschuwing |
| Interval voor reservecontrole | Alleen gebruikt wanneer directe updates niet beschikbaar zijn. |

### Instellingen → Rechten

Zonder deze kan Android voorkomen dat het alarm je bereikt.

| Instelling | Waarvoor |
|---|---|
| Meldingen | Alarmen en waarschuwingen kunnen niet worden getoond. |
| Activiteit op de achtergrond | De telefoon kan de bewaking in slaapstand zetten; alarmen kunnen te laat komen of gemist worden. |
| Alarm op volledig scherm | Als de telefoon vergrendeld is, kan het alarm zijn grote scherm niet openen. |
| Alarm op vergrendelscherm (Xiaomi) | Als de telefoon vergrendeld is, kan het alarmscherm niet openen. Tik op Oplossen, open Andere rechten en zet “Weergeven op vergrendelscherm” en “Nieuwe vensters openen terwijl op de achtergrond” aan. |
| Alarm over andere apps tonen | Nodig op Android TV zodat het alarm bovenop alles wat speelt verschijnt. Tik en sta SwitchGuard toe. |
| Starten op de achtergrond (Xiaomi, Huawei, OPPO, vivo…) | Verschijnt op telefoons die starten op de achtergrond blokkeren (Xiaomi, Huawei, OPPO, vivo…): laat de bewaking na een herstart of update weer starten. Zie Problemen oplossen. |

### Instellingen → Locatie en zones

| Instelling | Waarvoor |
|---|---|
| Locatie | Nodig voor regels met zonsopgang/zonsondergang en buitentemperatuur. Blijft op dit apparaat; voor de temperatuur wordt ze op ongeveer 100 m afgerond naar Open-Meteo gestuurd. |
| Zones | Kies een gebied op de kaart; automatiseringen draaien wanneer je telefoon het binnengaat of verlaat (bijv. de tuinlamp aanzetten als je thuiskomt, alles uitzetten als je weggaat). Kaartafbeeldingen worden gedownload van OpenStreetMap; je locatie wordt alleen op deze telefoon gebruikt. |

### Instellingen → Stand-byscherm

| Instelling | Waarvoor |
|---|---|
| Stand-byscherm openen | Tik op het scherm voor “App openen” en “Sluiten” |
| Als screensaver gebruiken | Opent de Android-instellingen: kies SwitchGuard onder Screensaver en stel in dat die start tijdens het opladen. Het bord vervangt dan het vergrendelscherm zolang het aan de oplader ligt. |
| 's Nachts dimmen | Tekst vervaagt tussen 23:00 en 07:00 |

### Instellingen → Dagelijks overzicht

| Instelling | Waarvoor |
|---|---|
| Melding met dagelijks overzicht | Hoe vaak elk apparaat aanging, hoe lang het aan bleef en hoeveel energie het vandaag verbruikte |
| Versturen om | Tijdstip waarop de samenvatting komt. |
| Overzicht van vandaag nu tonen | Toont hem meteen. |

### Instellingen → Back-up & overzetten

| Instelling | Waarvoor |
|---|---|
| Back-up naar een bestand | Regels, automatiseringen, instellingen en de Tuya- en eWeLink-sleutels komen in een met een wachtwoord beveiligd bestand |
| Herstellen uit een bestand | Naar een nieuwe telefoon? Kies het back-upbestand en voer het wachtwoord in |
| Naar een ander apparaat sturen | Kopieer Tuya-sleutels, regels en instellingen naar een tv of tablet op hetzelfde wifi-netwerk |
| Van een ander apparaat ontvangen | Haal de configuratie via wifi van je telefoon |

Overzetten via wifi verstuurt nooit de eWeLink-API-sleutels of de eWeLink-sessie: voer op het andere apparaat diens eigen API-sleutels in en log in met diens eigen eWeLink-account. Een back-up naar een bestand bevat wel de eWeLink- en Tuya-sleutels, beschermd met je wachtwoord.

### Instellingen → Taal

| Instelling | Waarvoor |
|---|---|
| Taal | Kies de taal van de app, meldingen en alarmen. Telefoontaal of een van 20 talen (vanaf 2.26). |

### Instellingen → Help & info

| Instelling | Waarvoor |
|---|---|
| Installatiegids | De eerste installatie opnieuw tonen |
| Installatiegids voor eWeLink | Stap voor stap met afbeeldingen (GitHub) |
| Installatiegids voor Tuya / Smart Life | Stap voor stap met afbeeldingen (GitHub) |
| Probleemrapport sturen | Werkt er iets niet? Bereidt een e-mail voor met diagnostische gegevens. Er wordt niets gedeeld tenzij je het verstuurt. |
| Opgeslagen crashrapporten verwijderen | Verwijdert de crashrapporten die op dit apparaat zijn opgeslagen. |
| Contact & feedback | Schrijft een e-mail aan de ontwikkelaar (vragen, fouten, ideeën). |
| Privacybeleid | Opent het privacybeleid. |
| Over | Versie, juridische vermelding. De GitHub-versie controleert op GitHub of er een nieuwere is. |

### Waarschuwingen

| Instelling | Waarvoor |
|---|---|
| Apparaatwaarschuwingen | Waarvoor elk apparaat afgaat of een melding geeft |
| Voorwaardelijke waarschuwingen | Laat afgaan als een apparaat een tijd aan of uit blijft, of als het vermogen verandert |
| Alarm is gereed / Alarmen bereiken je mogelijk niet | De gereedheidskaart bovenaan: toont telefooninstellingen die het alarm kunnen tegenhouden en opent het juiste scherm om ze te herstellen; met ook een knop ‘Testalarm’. |

### Waarschuwingen → Alarm

| Instelling | Waarvoor |
|---|---|
| Alarmgeluid | Algemeen alarmgeluid. Elk apparaat kan een eigen geluid hebben (apparaatpagina). |
| Alarmvolume | Eigen alarmvolume van SwitchGuard. |
| Trillen | Trillen terwijl het alarm afgaat. |
| Op vol volume laten klinken | Zet het alarmvolume van de telefoon op maximaal zolang het alarm afgaat en pauzeert muziek; wordt hersteld wanneer je het alarm uitzet |
| Opnieuw laten klinken als niet opgelost | Nadat je een alarm hebt uitgezet, gaat het op dit interval opnieuw af zolang het apparaat in de toestand blijft die het veroorzaakte (nog steeds uit, nog steeds aan of offline). Stopt zodra het apparaat weer normaal is. |
| Iemand anders waarschuwen | Telegram instellen (zie hieronder). |
| Testalarm | Probeer het alarm nu uit: met geluid of stil, direct of na 10 seconden |

### Waarschuwingen → Waarschuwingscategorieën

Elk apparaat heeft een waarschuwingscategorie. Tenzij je er een kiest, wordt die automatisch ingesteld: een bewaakt apparaat met een gebeurtenis die op Alarm staat is Kritiek, een apparaat dat alleen meldingen stuurt is Belangrijk, de rest is Normaal. Voorwaardelijke waarschuwingen en automatiseringen kunnen een categorie als trigger gebruiken (bijv. “laat het alarm afgaan als een kritiek apparaat offline gaat”).

| Instelling | Waarvoor |
|---|---|
| Kritiek · Belangrijk · Normaal · Auto | Bij automatisch: Kritiek als een gebeurtenis een alarm laat afgaan, Belangrijk als gebeurtenissen alleen een melding geven |
| Ook tijdens stille uren laten afgaan | Alarmen gaan ook tijdens stille uren af / Tijdens stille uren worden alarmen meldingen |

### Waarschuwingen → Stille uren

| Instelling | Waarvoor |
|---|---|
| Stille uren | Tijdens deze uren worden alarmen meldingen (kritieke apparaten gaan nog wel af) |
| Begint / Eindigt | Begin- en eindtijd van de stille uren. |

Apparaten in waarschuwingscategorieën die tijdens stille uren mogen afgaan (standaard Kritiek) gaan nog steeds af.

### Waarschuwingen → Iemand anders waarschuwen

Als een alarm niet op tijd wordt uitgezet, krijgen de mensen die je kiest een Telegram-bericht, en nog een zodra het is uitgezet. Zelfs als je telefoon 's nachts in een andere kamer ligt, merkt iemand thuis of een buur het. De berichten worden verstuurd door een Telegram-bot die je zelf maakt; er is geen andere server bij betrokken.

| Instelling | Waarvoor |
|---|---|
| 1. Telegram-bot | Open in Telegram @BotFather, stuur /newbot en geef je bot een naam. Plak hieronder de token die BotFather je geeft. |
| 2. Mensen om te waarschuwen | Vraag iedereen om @your_bot in Telegram te openen en op Start te tikken (je kunt de bot ook aan een familiegroep toevoegen). Tik daarna op Mensen zoeken. |
| 3. Wanneer | Hoe lang een alarm mag afgaan voordat mensen worden ingelicht (of meteen). |
| Testbericht sturen | Stuurt iedereen in de lijst een testbericht. |
| Uitzetten | Zet de Telegram-melding uit. |

### Waarschuwingen → Slimme waarschuwingen

Waarschuwingen die al je apparaten samen bekijken: stroomstoringen, ongebruikelijk verbruik en een weekoverzicht. Met Telegram kun je ook de status bekijken, een alarm overnemen en apparaten op afstand schakelen.

| Instelling | Waarvoor |
|---|---|
| Stroomstoring: Stroomstoringen detecteren | Als alle bewaakte apparaten tegelijk offline gaan, krijg je één waarschuwing “stroomstoring” in plaats van één per apparaat, en een bericht met de duur zodra de stroom terug is. |
| Oplader als stroomsensor | Als deze telefoon/tablet op locatie aangesloten blijft, telt een minuut losgekoppeld zijn als stroomstoring; dit werkt ook als het internet weg is. Het bericht gaat via Telegram over mobiele data. |
| Thuis / Weg: Zachtere waarschuwingen thuis | Terwijl je thuis bent, worden alarmen van niet-kritieke apparaten meldingen (zoals bij stille uren). Kritieke apparaten gaan altijd af. |
| Vakantiemodus | Je telt als weg, stille uren staan uit en elke ochtend om 09:00 komt er een rapport. Het eindigt vanzelf. |
| Verbruik: Ongebruikelijk verbruik | Vergelijkt elke ochtend gisteren met de twee weken ervoor. Veel hoger gebruik kan een defect betekenen; veel lager (een vriezer die amper draaide) kan betekenen dat die niet meer werkt. |
| Elektriciteitstarief | De standaardprijs voor apparaten zonder eigen prijs. Bij een driedelig tarief wordt elk uur in zijn eigen band geprijsd: dag 06–17, piek 17–22, nacht 22–06. |
| Maandbudget | Je krijgt één keer per maand een melding wanneer de geschatte kosten van de apparaten met energiemeting dit bedrag bereiken. |
| Weekrapport | Elke maandag om 09:00: storingen, alarmen en reactietijd, verbindingsonderbrekingen en verbruik van de afgelopen week. Ook naar Telegram gestuurd als dat is ingesteld. |
| Rapport van deze week tonen | Toont hem meteen. |
| Telegram: Commando's en knop “Begrepen” | Mensen in je lijst kunnen /status sturen en een afgaand alarm overnemen. Het alarmbericht krijgt een knop “Begrepen”: als je erop drukt, wordt de telefoon stil. |
| Aan- en uitzetten van apparaten toestaan | De commando's /on en /off. Alleen mensen in je lijst kunnen ze gebruiken. |
| Gefaseerde melding | Elke persoon krijgt een bericht na zijn eigen wachttijd. Bijvoorbeeld een buur na 5 minuten, een familielid na 15, de familiegroep na 30. Als iemand op “Begrepen” tikt, krijgen de volgende mensen geen bericht. |
| Bellen bij kritiek alarm | Wanneer een apparaat in de categorie Kritiek het alarm laat afgaan, wordt de persoon ook gebeld vanaf de lijn van deze telefoon zodra zijn wachttijd bij gefaseerde waarschuwingen om is. Het gesprek maakt de persoon alleen wakker: wat er is gebeurd staat in het Telegram-bericht dat tegelijk wordt gestuurd (Android staat niet toe dat apps geluid in een telefoongesprek afspelen). Vraag hem of haar dit nummer op te slaan als “SwitchGuard-alarm”. Gesprekken worden elke 2 minuten herhaald, maximaal 3 keer, tot het alarm is uitgezet of iemand in Telegram op “Begrepen” tikt. Groepen kunnen niet worden gebeld. |
| Slimme waarschuwingen naar Telegram sturen | Stroomstoringen, ongebruikelijk verbruik, de budgetwaarschuwing en het weekrapport worden ook naar Telegram geschreven. |
| Watchdog | Twee apparaten met dezelfde bot houden elkaar in de gaten (bijv. de telefoon en een tv-box). Het zendende apparaat houdt een vastgemaakt bericht in de Telegram-chat actueel; het bewakende apparaat laat een alarm afgaan en waarschuwt je contacten als dat stopt. Maak dat bericht niet los. |

### Apparaatpagina (tik op een apparaat)

| Instelling | Waarvoor |
|---|---|
| Dit apparaat bewaken | Wijzigingen worden vastgelegd en gemeld volgens de onderstaande regels |
| Wanneer dit gebeurt | Gaat uit, Gaat aan, Gaat offline, Komt online → Alarm / Melding / Geen. Alarm gaat af tot je het uitzet. Melding toont een gewone melding. Geen legt de gebeurtenis alleen vast in de geschiedenis. |
| Geluid voor dit apparaat | Eigen alarmgeluid; anders het algemene geluid. |
| Waarschuwingscategorie | Bij automatisch: Kritiek als een gebeurtenis een alarm laat afgaan, Belangrijk als gebeurtenissen alleen een melding geven |
| Energie | Vermogen, spanning, stroom; vandaag en deze maand; geschiedenis per dag / maand / jaar met kosten; rapport (PDF / CSV). |
| Prijs per kWh | Prijs per kWh voor dit apparaat of de standaardprijs; kan ook als standaard worden ingesteld. |
| Timers (op het apparaat) | Geen timers. Timers draaien op het apparaat zelf, ook als je telefoon uit staat. (eWeLink-apparaten: aan/uit-timers op een tijdstip op gekozen dagen toevoegen of bewerken.) |
| Verbinding | dBm is de signaalsterkte: dichter bij 0 is beter. Boven -55 uitstekend, tot -67 goed, tot -75 redelijk, daaronder zwak. |
| Verbruik in stand-by | Het laagste uurgemiddelde van de afgelopen 7 dagen: het vermogen waar dit apparaat (en alles wat erop is aangesloten) nooit onder komt, zelfs als het “niets doet”. Tv's, modems en opladers gebruiken dit vaak in stand-by. |
| Apparaatgegevens kopiëren | Kopieert de ruwe gegevens van het apparaat naar het klembord, handig om een niet-ondersteund model te melden |

## Telegram-opdrachten

Mensen in je lijst kunnen /status sturen en een afgaand alarm overnemen. Het alarmbericht krijgt een knop “Begrepen”: als je erop drukt, wordt de telefoon stil.

```
SwitchGuard-commando's:
/status – bewaking, alarm en apparaten
/ack – het afgaande alarm overnemen
/on naam [kanaal] – een apparaat of groep aanzetten (meerdere: /on keuken, tuin)
/off naam [kanaal] – een apparaat of groep uitzetten
/watt [naam] – vermogen nu, vandaag en gebruikelijk verbruik
/temp – buitentemperatuur
/scene naam – een scène uitvoeren
/home · /away – thuis / weg instellen

Je kunt ook vrij schrijven: “zet keuken en tuin uit”, “watt kachel”, “staat de pomp aan”.

Apparaten aan- en uitzetten en scènes moeten in de app zijn toegestaan (Waarschuwingen → Slimme waarschuwingen).
```

Zowel Engelse als Turkse opdrachtnamen werken (bijv. /status = /durum, /on = /ac). Opdrachten worden alleen aangenomen uit de gekozen chats.

## Machtigingen en waarom

| Machtiging | Waarom |
|---|---|
| `POST_NOTIFICATIONS` | Alarmen, meldingen en de bewakingsstatus tonen. |
| `FOREGROUND_SERVICE` | De bewaking op de achtergrond laten draaien (de vaste statusmelding). |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Vragen om uitzondering van batterijoptimalisatie zodat alarmen niet vertragen. |
| `USE_FULL_SCREEN_INTENT` | Het alarm schermvullend over het vergrendelscherm openen. |
| `SYSTEM_ALERT_WINDOW` | Het alarm boven andere apps tonen (Android TV, Xiaomi). |
| `WAKE_LOCK` | De processor wakker houden tijdens het verwerken van gebeurtenissen (‘Maximale betrouwbaarheid’). |
| `VIBRATE` | Trillen tijdens het alarm. |
| `RECEIVE_BOOT_COMPLETED` | De bewaking opnieuw starten nadat de telefoon herstart. |
| `INTERNET` | Praten met de eWeLink- / Tuya-cloud (en Telegram / Open-Meteo / OpenStreetMap als je die gebruikt). |
| `ACCESS_COARSE_LOCATION` | Alleen als je op ‘Locatie van dit apparaat gebruiken’ tikt voor regels met zonsopgang/-ondergang en temperatuur. |
| `ACCESS_FINE_LOCATION` | Zones: een precies gebied voor aankomst- / vertrekregels. |
| `ACCESS_BACKGROUND_LOCATION` | Zones: aankomst en vertrek opmerken terwijl de app gesloten is (alleen gevraagd als je een zone maakt). |
| `CALL_PHONE` | ‘Bellen bij kritiek alarm’: het ingevoerde nummer bellen via de lijn van deze telefoon. |

## Privacy

- Geen eigen server, geen analyse, geen advertenties, geen tracking. Er wordt niets verkocht of gedeeld.
- Sleutels, tokens, apparaten, regels, instellingen en geschiedenis (max. 2.000 gebeurtenissen) worden alleen op je apparaat bewaard.
- De app praat alleen met de clouds die je koppelt (eWeLink `*.coolkit.cc` / `*.coolkit.cn`, Tuya `openapi.tuya*.com`) en, alleen als je ze gebruikt, met Telegram, Open-Meteo (temperatuur en weer; locatie afgerond op ongeveer 100 m) en OpenStreetMap-kaarttegels.
- Je eWeLink-wachtwoord typ je op de eigen pagina van eWeLink; de app ziet het nooit.
- App-gegevens zijn uitgesloten van de Android-cloudback-up; bestandsback-ups zijn met een wachtwoord beveiligd; overzetten via wifi is versleuteld met een eenmalige code en blijft in je thuisnetwerk.
- Pro-aankopen worden door Google Play afgehandeld.

Volledig privacybeleid: [Google Sites](https://sites.google.com/view/switchguard-privacy) · [docs/privacy-policy.md](docs/privacy-policy.md)

## Problemen oplossen en veelgestelde vragen

### Het alarm komt te laat of gaat niet af

| Probleem | Wat te doen |
|---|---|
| Activiteit op de achtergrond is beperkt | De telefoon kan de bewaking in slaapstand zetten; alarmen kunnen te laat komen of gemist worden. |
| Alarm op volledig scherm is niet toegestaan | Als de telefoon vergrendeld is, kan het alarm zijn grote scherm niet openen. |
| Xiaomi blokkeert het alarmscherm | Als de telefoon vergrendeld is, kan het alarmscherm niet openen. Tik op Oplossen, open Andere rechten en zet “Weergeven op vergrendelscherm” en “Nieuwe vensters openen terwijl op de achtergrond” aan. |
| Alarmmeldingen zijn zachter gezet | De meldingscategorie “Alarmen” staat niet op hoge prioriteit, dus het alarm verschijnt niet als pop-up en vult het scherm niet. |
| Niet storen: totale stilte | In deze modus worden zelfs alarmgeluiden gedempt. |
| Alarmvolume is erg laag | Het alarmvolume van de telefoon staat op of bijna op nul, dus je hoort SwitchGuard mogelijk niet. Oplossen zet “Op vol volume laten klinken” aan. |
| Meldingen staan uit | Alarmen en waarschuwingen kunnen niet worden getoond. |
| Er staat een tweede SwitchGuard op deze telefoon | Er is een andere kopie van SwitchGuard gemaakt met de functie Dubbele apps (app-kloon) van de telefoon. Twee kopieën naast elkaar laten elk alarm twee keer afgaan en loggen elkaar uit als ze hetzelfde eWeLink-account gebruiken; op Xiaomi-telefoons kunnen de rechten van deze kopie ook uitgeschakeld lijken. Verwijder de tweede kopie: houd het pictogram ervan ingedrukt (die met het kleine label) en tik op Verwijderen. |

### Fabrikanten die achtergrond-apps stoppen

| Telefoon | Wat te doen |
|---|---|
| Xiaomi | Nodig zodat de bewaking na een herstart of update weer start. Tik en zet Automatisch starten aan voor SwitchGuard. |
| Samsung | Samsung kan de app in slaapstand zetten. Kies bij Batterij "Onbeperkt" en voeg SwitchGuard toe aan "Apps die nooit slapen" (Batterij → Limieten voor achtergrondgebruik). |
| Huawei / Honor | Stel bij App starten SwitchGuard in op "Handmatig beheren" en zet Automatisch starten, Secundair starten en Op achtergrond uitvoeren aan. |
| OPPO / realme / OnePlus | Zet Automatisch starten aan voor SwitchGuard en sta daarna in de batterij-instellingen activiteit op de achtergrond toe. |
| vivo / iQOO | Sta toe dat SwitchGuard op de achtergrond start en sta hoog stroomverbruik op de achtergrond toe. |
| Tecno / Infinix | Zet Automatisch starten aan voor SwitchGuard in Phone Master, zodat de bewaking herstarts en opschoonacties overleeft. |
| ASUS | Sta SwitchGuard toe in Auto-start Manager zodat de bewaking na een herstart weer start. |

### Andere vragen

**De bewaking is gestopt en zegt dat ik ben uitgelogd.**  
Tip: eWeLink staat één sessie per account toe. Geef elke telefoon een eigen eWeLink-account en deel de apparaten via de eWeLink-app.

**Een met mij gedeeld Tuya-apparaat ontbreekt.**  
Ontbreekt een apparaat dat met je is gedeeld? Tuya toont alleen de apparaten in jouw huizen. Vraag de eigenaar om je als huislid toe te voegen in Smart Life (Me → Home Management → Add Member).

**Tuya-apparaten worden niet meer bijgewerkt.**  
De proefperiode van Tuya Cloud is verlopen, dus Tuya-apparaten worden niet bewaakt. Verleng hem via platform.tuya.com → Cloud → IoT Core → Extend trial period.

**Wijzigingen komen te laat / geen directe updates beschikbaar.**  
Directe updates zijn niet beschikbaar voor deze App ID; er wordt periodiek gecontroleerd (wijzigingen korter dan het interval kunnen gemist worden)

**Kan ik wisselen tussen de Google Play- en de GitHub-versie?**  
Ja, maar verwijder eerst de andere: ze zijn anders ondertekend. Maak vóór het verwijderen een back-up naar een bestand en zet die daarna terug.

**Hoe test ik het alarm zonder iedereen wakker te maken?**  
Probeer het alarm nu uit: met geluid of stil, direct of na 10 seconden

**Waarom wijkt het verbruik af van de eWeLink- / Smart Life-app?**  
Het verbruik wordt door SwitchGuard berekend uit vermogensmetingen die zijn gedaan terwijl de bewaking aan stond, dus het kan licht afwijken van de app van de fabrikant (eWeLink, Smart Life).

## Windows-app

SwitchGuard voor Windows (10 / 11) bewaakt op dezelfde manier op een pc: hij zit in het systeemvak, laat een schermvullend alarm afgaan op de computer en deelt dezelfde regels, automatiseringen en Telegram-functies. Zones, widgets, het horloge en telefoongesprekken zijn alleen voor de telefoon.

### Functies

- Dezelfde tabbladen: Apparaten, Waarschuwingen, Automatiseringen, Geschiedenis, Chat, Instellingen; spraakopdrachten in de chat gebruiken spraakgestuurd typen van Windows.
- Systeemvakpictogram: openen, bewaking starten / stoppen, alarm uitzetten, afsluiten; het venster sluiten stopt de bewaking niet.
- Schermvullend alarmvenster, ingebouwde geluiden (Sirene, Piep, Puls, Klokkenspel) of je eigen WAV / MP3.
- Standby-scherm en Windows-schermbeveiliging: het bord opent als de pc inactief is.
- Een laptop die aan de lader blijft, werkt als stroomuitvalsensor.
- Updates van GitHub met één klik; instellingen en licentie blijven. Back-up naar een met wachtwoord beveiligd bestand (niet terug te zetten op een telefoon).

### Alleen in Windows

| Instelling | Waarvoor |
|---|---|
| Windows | Zodat SwitchGuard op deze computer zonder onderbreking blijft waken. Staat een van beide uit, dan kunnen alarmen na een herstart of tijdens slaapstand worden gemist. |
| Starten met Windows | Opent in het systeemvak als je je aanmeldt bij Windows |
| Computer wakker houden | Tijdens het bewaken gaat de computer niet in slaapstand |
| Gebruiken als Windows-schermbeveiliging | Als de computer inactief is, opent het stand-byscherm; de muis bewegen of een toets indrukken sluit het |
| Wachttijd | Hoelang de computer inactief moet zijn voordat het stand-byscherm opent |
| Instellingen voor Windows-schermbeveiliging | Bijv. ‘Aanmeldingsscherm weergeven bij hervatten’ |
| Stand-byscherm openen | Volledig scherm; Esc sluit, klik voor ‘App openen’. Het scherm blijft aan zolang het open is. |
| Taal & weergave: Thema | Licht, donker of hetzelfde als Windows. |
| Taal | Kies de taal van de app, meldingen en alarmen. |
| Live Tuya-updates op deze computer | Uit: Tuya-apparaten worden elke 5 minuten gecontroleerd |
| Stroomstoring als deze computer op de accu overgaat | Voor een laptop die thuis aan de lader hangt: draait hij een minuut op de accu, dan telt dat als stroomstoring, ook zonder internet (het Telegram-bericht gaat zodra de verbinding terug is). |
| Deze computer zit op dezelfde stroom als de apparaten | Zolang hij op netstroom draait is er geen stroomstoring, dus een apparaat dat wegvalt wordt meteen gemeld in plaats van na maximaal 90 seconden. |
| Laden uit bestand… | Gebruik je eigen WAV of MP3 voor het alarm of de melding (eerste 60 seconden, max. 20 MB). |
| Back-up naar bestand… | Alle instellingen, accountsleutels en geluiden in één met een wachtwoord beveiligd bestand |
| Pro activeren | Zonder licentie bewaakt de app 1 apparaat. Voer je licentiesleutel in om al je apparaten te bewaken. |

### Installeren op Windows

1. Dubbelklik op het installatiebestand (SwitchGuard-…-Kurulum.exe). Het vraagt geen beheerdersrechten en installeert in een paar seconden.
2. Als Windows ‘Uw pc is beschermd door Windows’ meldt (SmartScreen), druk op Meer informatie → Toch uitvoeren. De app is nog niet ondertekend; daarom de waarschuwing.
3. SwitchGuard wordt in je gebruikersmap geïnstalleerd (AppData › Local › SwitchGuard) en zet snelkoppelingen op het bureaublad en in het Startmenu.
4. Het start vanzelf in het systeemvak (naast de klok) als je je aanmeldt bij Windows. Het venster sluiten stopt het niet; afsluiten via het systeemvakpictogram → Afsluiten.
5. Voeg je accounts toe in Instellingen → Accounts (de stappen voor Tuya en eWeLink staan hieronder).
6. Naar een andere computer: op de oude Instellingen → Back-up & overzetten → Back-up naar bestand; installeer op de nieuwe, Herstellen uit bestand, en log daarna opnieuw in bij eWeLink.
7. Verwijderen: Windows-instellingen → Apps → Geïnstalleerde apps → SwitchGuard → Verwijderen.
8. Een nieuwe versie installeren: verwijder eerst de oude (stap 7) en voer dan het nieuwe installatiebestand uit. Je instellingen, accounts en geluiden blijven bewaard.

## Bouwen

```
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest      # unit tests
```
Release-builds hebben `keystore.properties` + keystore in de projectmap nodig (niet in de repository).

## Wijzigingslog

[English](CHANGELOG.md) · [Türkçe](CHANGELOG.tr.md) · [Releases](https://github.com/Macerce/switchguard/releases)

## Contact

Vragen, foutmeldingen en feedback (of open hier een issue): **switchguardapp@gmail.com**

## Licentie

SwitchGuard is **source-available** (broncode inzichtelijk), geen open source. Broncode: [PolyForm Noncommercial 1.0.0](LICENSE). Persoonlijk en ander niet-commercieel gebruik is vrij; de code verkopen of commercieel gebruiken is niet toegestaan. Versies tot en met 2.8.3 zijn onder GPL-3.0 uitgebracht.

Officiële builds (Google Play, [Releases](https://github.com/Macerce/switchguard/releases)) mag iedereen gebruiken, ook bedrijven. Gewijzigde versies mogen de naam en het logo van SwitchGuard niet gebruiken. Commerciële licentie: **switchguardapp@gmail.com**

Niet verbonden aan, goedgekeurd of gesponsord door eWeLink, CoolKit of ITEAD. Sonoff en eWeLink zijn handelsmerken van hun respectieve eigenaren. Ook niet verbonden met Tuya.
