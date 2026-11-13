[English](README.md) · [Türkçe](README.tr.md) · [Deutsch](README.de.md) · [Español](README.es.md) · [Português (Brasil)](README.pt-BR.md) · [Français](README.fr.md) · **Italiano** · [Nederlands](README.nl.md) · [Polski](README.pl.md) · [Русский](README.ru.md) · [Українська](README.uk.md) · [العربية](README.ar.md) · [हिन्दी](README.hi.md) · [Bahasa Indonesia](README.id.md) · [Tiếng Việt](README.vi.md) · [ไทย](README.th.md) · [简体中文](README.zh-CN.md) · [繁體中文](README.zh-TW.md) · [日本語](README.ja.md) · [한국어](README.ko.md)

# SwitchGuard

SwitchGuard controlla le prese e gli interruttori smart dei tuoi account **eWeLink / SONOFF** e **Tuya / Smart Life** e fa suonare un vero allarme appena qualcosa cambia: una pompa che si è fermata, un congelatore spento, un dispositivo che ha perso la connessione. Funziona su telefoni e tablet Android, Android TV e smartwatch Wear OS, e c'è anche un'app per Windows.

<p>
  <img src="docs/screenshots/play/en/v215/01-devices.png" width="200" alt="Dispositivi">
  <img src="docs/screenshots/play/en/v215/02-energy.png" width="200" alt="Energia">
  <img src="docs/screenshots/play/en/v215/05-automations.png" width="200" alt="Automazioni">
  <img src="docs/screenshots/play/en/v215/08-alarm.png" width="200" alt="Allarme dispositivo">
</p>

> **Lingue:** l’app Android (2.26+) e l’app Windows (0.13+) sono disponibili in 20 lingue — Impostazioni → Lingua.

## Download

### Android (telefono, tablet, TV)
- **Google Play**: https://play.google.com/store/apps/details?id=com.macerce.switchguard
- **APK**: [ultima versione Android](https://github.com/Macerce/switchguard/releases/latest) — scarica `SwitchGuard-x.y.z.apk` e aprilo sul telefono. Se SwitchGuard è già installato da Google Play, disinstallalo prima (firma diversa). La versione GitHub ti avvisa quando ne esce una nuova.

### Windows 10 / 11
- **Programma di installazione**: [versioni per Windows](https://github.com/Macerce/switchguard/releases?q=Windows&expanded=true) — scarica `SwitchGuard-x.y.z-Kurulum.exe` dalla più recente ed eseguilo. L'app cerca da sola le nuove versioni e si aggiorna con un clic; le impostazioni restano.

### Gratis
- Completamente gratis su Android e Windows: tutte le funzioni, tutti i dispositivi monitorati, nessun acquisto, nessuna chiave di licenza. Con i dispositivi demo puoi provare tutto senza hardware.

## Requisiti

- Android 8.0+ (API 26); Wear OS per l'app per orologio
- eWeLink: un'app sviluppatore eWeLink gratuita (App ID + App Secret) — la guida nell'app ti accompagna passo passo. eWeLink consente una sola sessione per account e App ID, quindi ogni telefono, tablet, TV o PC ha bisogno di un proprio account eWeLink con i dispositivi condivisi ([dev.ewelink.cc](https://dev.ewelink.cc))
- Tuya / Smart Life: Access ID e Access Secret del tuo progetto Tuya Cloud gratuito, collegato al tuo account dell'app Smart Life / Tuya ([platform.tuya.com](https://platform.tuya.com))
- Windows 10 / 11 (64 bit) per l'app Windows

## Guide di configurazione

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

## Funzioni

### Monitoraggio e allarme

- Aggiornamenti in tempo reale tramite il WebSocket di eWeLink e il servizio messaggi di Tuya, con risincronizzazione HTTP e controlli periodici di riserva.
- L'allarme suona finché non lo spegni — anche in modalità silenziosa — e si apre a schermo intero sopra la schermata di blocco; ogni dispositivo può avere il suo suono.
- Regole per dispositivo: allarme, notifica o niente quando un dispositivo si spegne, si accende, va offline o torna online.
- Avvisi condizionali: suona quando un dispositivo resta acceso o spento per un po', o la sua potenza supera / scende sotto un limite (es. una pompa sotto i 50 W).
- Ignora le brevi disconnessioni, avviso quando il monitoraggio stesso si ferma (niente internet / cloud), avviso sonoro quando la sessione eWeLink si perde (es. stesso account connesso su un altro telefono).
- Una scheda di prontezza mostra e corregge le impostazioni del telefono che potrebbero bloccare l'allarme (restrizione batteria, permesso schermo intero, Non disturbare, volume sveglia…).

### Dispositivi

- Riquadri stile casa smart con pulsanti rapidi, un pulsante per canale sui dispositivi multicanale, stato e potenza in tempo reale.
- Pagina del dispositivo: regole, suono d'allarme proprio, categoria di avviso, cronologia energia e costo, timer eWeLink, segnale Wi-Fi e diagnosi delle cadute, consumo in standby ed eventi recenti.
- **Disponi i dispositivi**: Stella = tieni in alto · frecce = sposta · dimensione scheda = piccola o grande · etichetta = categoria. Tieni premuta una scheda per tornare qui.

### Sensori (eWeLink)

- Temperatura / umidità (TH16, THR316/320, SNZB-02), porta (DW2, SNZB-04), movimento / presenza (SNZB-03, SNZB-06P), perdita d’acqua (SNZB-05) e batteria sulle schede dei dispositivi, nella schermata di attesa e nella Chat.
- Attivatori dei sensori nelle automazioni (valore sopra / sotto, porta aperta, movimento, perdita d’acqua) e avvisi dei sensori con un tocco nella pagina del dispositivo.
- Cronologia dei sensori: temperatura, umidità, luce e batteria vengono salvate mentre il monitoraggio è attivo; la pagina del dispositivo mostra un grafico di 24 ore / 7 giorni / 30 giorni con minimo, massimo e media.

### Avvisi

- Avvisi dei dispositivi e avvisi condizionali in un unico posto, con filtro per categoria (Critico / Importante / Normale).
- Impostazioni avvisi: suono e volume dell'allarme, ore di silenzio, categorie di avviso, Telegram («Avvisa qualcun altro») e avvisi intelligenti — vedi *Tutte le impostazioni* più sotto.
- Avvisi intelligenti: un solo avviso per un blackout, consumo insolito, budget mensile, report settimanale, modalità vacanza.

### Automazioni

- **Regole** — azioni automatiche: *Se* succede qualcosa, *allora* fai qualcosa, con ritardo facoltativo.
- Attivatori: si accende, si spegne, resta acceso per, resta spento per, va offline, torna online, la potenza supera, la potenza scende sotto, all'alba, al tramonto, quando la temperatura esterna sale sopra, quando la temperatura esterna scende sotto, all'arrivo, alla partenza. Attivatori dei sensori: valore sopra / sotto, porta aperta / chiusa, movimento, perdita d’acqua.
- Azioni: Accendi, Spegni, Fai suonare l'allarme, Invia notifica, Esegui scena, Avvia ciclo, Ferma ciclo.
- **Solo quando**: Solo in queste ore, E solo se un altro dispositivo…, Solo quando in casa / fuori casa. Le condizioni possono anche controllare un valore del sensore, uno stato del sensore (porta aperta, movimento, perdita), la potenza del dispositivo o la temperatura esterna; aggiungine più di una e scegli «Tutte le condizioni» (e) o «Ne basta una» (o).
- **Scene** — Scegli cosa succede a ogni dispositivo quando la scena viene eseguita. I dispositivi lasciati su “Salta” non vengono toccati.
- **Gruppi** — Scegli i dispositivi del gruppo. Il gruppo compare nella schermata Dispositivi come un dispositivo: il suo pulsante li accende o li spegne tutti insieme, e puoi usarlo nelle automazioni.
- **Cicli e timer** — Conto alla rovescia, Acceso, poi spento, Ciclo acceso/spento, Uno alla volta, Passaggi personalizzati. Avvio con un pulsante, a un orario nei giorni scelti o durante una fascia oraria; variazione casuale facoltativa (casa che sembra abitata) e «ferma se cambiato a mano».
- Le automazioni vengono eseguite su questo telefono mentre il monitoraggio è attivo. Per le pianificazioni che devono funzionare anche a telefono spento, usa i timer nella pagina del dispositivo.

### Energia

- Vista per giorno / mese / anno con totale, costo stimato, potenza media e di picco, ora/giorno/mese di maggior consumo e un riepilogo dei punti salienti.
- Tariffa unica o a tre fasce (giorno / punta / notte), prezzo per dispositivo o prezzo predefinito.
- Consumo in standby (la potenza sotto cui un dispositivo non scende mai) e il suo costo mensile.
- Report energetico in PDF (da stampare o condividere) o CSV (Excel, Fogli Google).
- Il consumo viene calcolato da SwitchGuard in base alle letture di potenza effettuate mentre il monitoraggio è attivo, quindi può differire leggermente dall'app del produttore del dispositivo (eWeLink, Smart Life).

### Cronologia

- Ogni evento con data e ora, e chi o cosa l'ha causato (app, automazione); filtro per dispositivo.
- Esporta in CSV (ultime 24 ore, 7 giorni, 30 giorni o tutto) e cancella la cronologia.

### Chat

- Ciao. Puoi gestire i tuoi dispositivi scrivendo (in inglese o turco): accenderli o spegnerli, chiedere il loro stato, il consumo o la temperatura esterna. Tocca il microfono per dirlo a voce (“Turn off kitchen”); la risposta viene letta ad alta voce. Qui sotto trovi modelli pronti; toccane uno per inviarlo, tienilo premuto per modificarlo.
- I comandi a testo libero funzionano offline e capiscono inglese e turco; il microfono usa il riconoscimento vocale del telefono e le risposte possono essere lette ad alta voce.
- Modelli: messaggi con un tocco che possono anche partire da soli ogni giorno a un orario o quando succede qualcosa.

### Schermata di standby

- Una bacheca a schermo intero per un telefono o tablet in carica: orologio, meteo, stato critico, potenza in tempo reale, attività recente e cosa viene eseguito dopo. Compare anche sopra la schermata di blocco.
- Apre le impostazioni di Android: scegli SwitchGuard in Salvaschermo e impostalo perché parta durante la ricarica. La bacheca sostituisce poi la schermata di blocco mentre è in carica.

### Widget, riquadro delle impostazioni rapide, scorciatoie

- **SwitchGuard** — Mostra se il monitoraggio è attivo e l'eventuale allarme in corso
- **Interruttore SwitchGuard** — Accende e spegne un dispositivo o un canale con un tocco. Può anche essere impostato su In casa / Fuori casa o per avviare una scena.
- **Gruppo SwitchGuard** — Riquadro grande: accendi o spegni ogni dispositivo di un gruppo singolarmente
- **Comando vocale SwitchGuard** — Un solo riquadro: tocca, dillo (“Spegni la cucina”) e viene eseguito
- **Riquadro impostazioni rapide** — mostra lo stato del monitoraggio e lo avvia o ferma (il telefono va prima sbloccato); mentre suona un allarme apre l'app.
- **Scorciatoie dell'app** — tieni premuta l'icona per In casa / Fuori casa e le tue scene.

### In casa / Fuori casa

- **In casa / Fuori casa** — Le regole contrassegnate “solo quando fuori casa” non vengono eseguite. Passa a Fuori casa quando esci (anche dal widget, dall'orologio o con /away su Telegram).
- **Modalità vacanza** — Risulti fuori casa, le ore di silenzio sono disattivate e ogni mattina alle 09:00 arriva un report. Termina da sola.
- **Zone** — Scegli un'area sulla mappa; le automazioni vengono eseguite quando il telefono vi entra o ne esce (ad es. accendi la luce del giardino quando torni a casa, spegni tutto quando esci). Le immagini della mappa vengono scaricate da OpenStreetMap; la tua posizione è usata solo su questo telefono.

### Wear OS

- L'app per orologio SwitchGuard mostra gli allarmi (spegnili dal polso), i dispositivi (accendi/spegni), le scene e In casa / Fuori casa; si collega all'app del telefono.

### Android TV

- Funziona su Android TV con il telecomando; l'allarme appare sopra qualsiasi contenuto in riproduzione («Mostra l'allarme sopra le altre app»).
- «Inserisci dal telefono»: la TV mostra un codice QR; scrivi le chiavi sul telefono e arrivano cifrate alla TV tramite il Wi-Fi di casa.
- «Ricevi da un altro dispositivo»: copia chiavi Tuya, regole e impostazioni dal telefono via Wi-Fi.

### Più dispositivi

Telefono, tablet, Android TV e PC: **ogni dispositivo ha bisogno di un proprio account eWeLink con la funzione sviluppatore approvata e delle chiavi API di quell'account (App ID / Secret)** — eWeLink consente un solo dispositivo connesso per account. Condividi i tuoi dispositivi con l'account aggiuntivo dall'app eWeLink. Le chiavi Tuya si possono usare su più dispositivi. Dettagli: [English](docs/guide/ewelink.en.md#using-several-devices-phone-tablet-android-tv) · [Türkçe](docs/guide/ewelink.tr.md#birden-fazla-cihazda-kullanım-telefon-tablet-android-tv)

### Demo

- **Dispositivi demo** — Dispositivi virtuali per provare ogni funzione. Questi agiscono come se qualcosa fosse successo sul dispositivo stesso (qualcuno lo ha azionato, è mancata la corrente o il Wi-Fi), quindi le tue regole si applicano e l'allarme può suonare. L'azionamento con i pulsanti qui sopra conta come fatto da te e non fa suonare l'allarme.

## Tutte le impostazioni

Tutto nell'ordine dell'app. La scheda **Impostazioni** contiene connessioni e opzioni dell'app; allarme, ore di silenzio, Telegram e avvisi intelligenti sono nella scheda **Avvisi** → Impostazioni degli avvisi; le opzioni di ogni dispositivo sono nella sua pagina.

### Impostazioni → Account

| Impostazione | A cosa serve |
|---|---|
| eWeLink — Accedi con eWeLink | Apre la pagina di accesso di eWeLink; l'app non vede mai la tua password. |
| Credenziali API | App ID, App Secret e regione della tua app sviluppatore eWeLink. |
| Esci | Il monitoraggio si fermerà. Impostazioni e cronologia vengono conservate. |
| Tuya / Smart Life | Inserisci Access ID e Access Secret del tuo progetto Tuya Cloud (platform.tuya.com → Cloud → il tuo progetto → Overview). L'account dell'app Smart Life / Tuya deve essere collegato al progetto. Data center: Europa centrale, America occidentale, Cina, India |
| Disconnetti | Rimuove la connessione Tuya; i dispositivi Tuya non vengono più monitorati. |
| Dispositivi demo | Dispositivi virtuali per provare ogni funzione |

### Impostazioni → Monitoraggio

Come SwitchGuard tiene sotto controllo i tuoi dispositivi in background.

| Impostazione | A cosa serve |
|---|---|
| Massima affidabilità | Mantiene il processore attivo così nessun evento viene mai ritardato. Consuma più batteria. |
| Ignora brevi disconnessioni | Attende questo tempo prima di segnalare che un dispositivo è andato offline. Se torna in tempo, non viene generato alcun avviso. |
| Avvisa se il monitoraggio si ferma | Se manca Internet o la connessione al cloud per questo tempo: ricevi un avviso |
| Intervallo di controllo di riserva | Usato solo quando gli aggiornamenti istantanei non sono disponibili. |

### Impostazioni → Autorizzazioni

Senza queste, Android potrebbe impedire all'allarme di raggiungerti.

| Impostazione | A cosa serve |
|---|---|
| Notifiche | Allarmi e avvisi non possono essere mostrati. |
| Attività in background | Il telefono potrebbe sospendere il monitoraggio; gli allarmi possono arrivare in ritardo o mancare. |
| Allarme a schermo intero | Con il telefono bloccato, l'allarme non può aprire la sua schermata grande. |
| Allarme sulla schermata di blocco (Xiaomi) | Con il telefono bloccato, la schermata dell'allarme non può aprirsi. Tocca Risolvi, apri Altre autorizzazioni e attiva “Mostra nella schermata di blocco” e “Apri nuove finestre durante l'esecuzione in background”. |
| Mostra l'allarme sopra le altre app | Necessario su Android TV perché l'allarme compaia sopra qualunque cosa sia in riproduzione. Tocca e consenti SwitchGuard. |
| Avvio in background (Xiaomi, Huawei, OPPO, vivo…) | Compare sui telefoni che bloccano l'avvio in background (Xiaomi, Huawei, OPPO, vivo…): permette al monitoraggio di ripartire dopo un riavvio o un aggiornamento. Vedi Risoluzione dei problemi. |

### Impostazioni → Posizione e zone

| Impostazione | A cosa serve |
|---|---|
| Posizione | Necessaria per le regole di alba/tramonto e temperatura esterna. Resta su questo dispositivo; per la temperatura viene inviata a Open-Meteo arrotondata a circa 100 m. |
| Zone | Scegli un'area sulla mappa; le automazioni vengono eseguite quando il telefono vi entra o ne esce (ad es. accendi la luce del giardino quando torni a casa, spegni tutto quando esci). Le immagini della mappa vengono scaricate da OpenStreetMap; la tua posizione è usata solo su questo telefono. |

### Impostazioni → Schermata di standby

| Impostazione | A cosa serve |
|---|---|
| Apri la schermata di standby | Tocca lo schermo per “Apri app” e “Chiudi” |
| Usa come salvaschermo | Apre le impostazioni di Android: scegli SwitchGuard in Salvaschermo e impostalo perché parta durante la ricarica. La bacheca sostituisce poi la schermata di blocco mentre è in carica. |
| Attenua di notte | Il testo si attenua tra le 23:00 e le 07:00 |

### Impostazioni → Riepilogo giornaliero

| Impostazione | A cosa serve |
|---|---|
| Notifica di riepilogo giornaliero | Quante volte ogni dispositivo si è acceso, per quanto è rimasto acceso e quanta energia ha usato oggi |
| Invia alle | Ora in cui viene inviato il riepilogo. |
| Mostra ora il riepilogo di oggi | Lo mostra subito. |

### Impostazioni → Backup e trasferimento

| Impostazione | A cosa serve |
|---|---|
| Esegui il backup in un file | Regole, automazioni, impostazioni e chiavi Tuya ed eWeLink vengono salvate in un file protetto da password |
| Ripristina da un file | Stai passando a un nuovo telefono? Scegli il file di backup e inserisci la sua password |
| Invia a un altro dispositivo | Copia chiavi Tuya, regole e impostazioni su una TV o un tablet nello stesso Wi-Fi |
| Ricevi da un altro dispositivo | Ricevi la configurazione dal telefono tramite Wi-Fi |

Il trasferimento via Wi-Fi non invia mai le chiavi API eWeLink né la sessione eWeLink: sull'altro dispositivo inserisci le sue chiavi API e accedi con il suo account eWeLink. Il backup su file invece include le chiavi eWeLink e Tuya, protette dalla tua password.

### Impostazioni → Lingua

| Impostazione | A cosa serve |
|---|---|
| Lingua | Scegli la lingua dell'app, delle notifiche e degli allarmi. Lingua del telefono o una di 20 lingue (dalla 2.26). |

### Impostazioni → Aiuto e informazioni

| Impostazione | A cosa serve |
|---|---|
| Guida alla configurazione | Mostra di nuovo la configurazione iniziale |
| Guida alla configurazione di eWeLink | Passo dopo passo con immagini (GitHub) |
| Guida alla configurazione di Tuya / Smart Life | Passo dopo passo con immagini (GitHub) |
| Invia una segnalazione del problema | Qualcosa non va? Prepara un'email con i dettagli diagnostici. Non viene condiviso nulla se non la invii. |
| Elimina le segnalazioni di arresto salvate | Elimina i report di arresto salvati su questo dispositivo. |
| Contatti e feedback | Scrive un'e-mail allo sviluppatore (domande, bug, idee). |
| Informativa sulla privacy | Apre l'informativa sulla privacy. |
| Informazioni | Versione e nota legale. La versione GitHub controlla su GitHub se ce n'è una più recente. |

### Avvisi

| Impostazione | A cosa serve |
|---|---|
| Avvisi dei dispositivi | Per cosa suona o notifica ogni dispositivo |
| Avvisi condizionali | Suona quando un dispositivo resta acceso o spento per un certo tempo, o ne cambia la potenza |
| L'allarme è pronto / Gli allarmi potrebbero non raggiungerti | La scheda di prontezza in alto: elenca le impostazioni del telefono che possono bloccare l'allarme e apre la schermata giusta per correggerle; c'è anche un pulsante «Prova allarme». |

### Avvisi → Allarme

| Impostazione | A cosa serve |
|---|---|
| Suoneria dell'allarme | Suono generale dell'allarme. Ogni dispositivo può avere il suo (pagina del dispositivo). |
| Volume dell'allarme | Volume proprio dell'allarme SwitchGuard. |
| Vibrazione | Vibra mentre suona l'allarme. |
| Suona al volume massimo | Porta al massimo il volume della sveglia del telefono mentre l'allarme suona e mette in pausa la musica; viene ripristinato quando lo silenzi |
| Suona di nuovo se non risolto | Dopo aver silenziato un allarme, suona di nuovo a questo intervallo finché il dispositivo resta nello stato che l'ha causato (ancora spento, ancora acceso od offline). Si ferma quando il dispositivo torna alla normalità. |
| Avvisa qualcun altro | Configurazione di Telegram (vedi sotto). |
| Prova allarme | Prova l'allarme ora: con suono o in silenzio, subito o dopo 10 secondi |

### Avvisi → Categorie di avviso

Ogni dispositivo ha una categoria di avviso. Se non ne scegli una, viene impostata automaticamente: un dispositivo monitorato con un evento impostato su Allarme è Critico, uno che invia solo notifiche è Importante, gli altri sono Normali. Gli avvisi condizionali e le automazioni possono usare una categoria come innesco (ad es. “fai suonare l'allarme quando un dispositivo critico va offline”).

| Impostazione | A cosa serve |
|---|---|
| Critico · Importante · Normale · Auto | In automatico: Critico se un evento fa suonare un allarme, Importante se gli eventi inviano solo notifiche |
| Suona anche durante le ore di silenzio | Gli allarmi suonano anche durante le ore di silenzio / Durante le ore di silenzio gli allarmi diventano notifiche |

### Avvisi → Ore di silenzio

| Impostazione | A cosa serve |
|---|---|
| Ore di silenzio | In queste ore gli allarmi diventano notifiche (i dispositivi critici suonano comunque) |
| Inizio / Fine | Ora di inizio e di fine delle ore di silenzio. |

I dispositivi nelle categorie di avviso impostate per suonare durante le ore di silenzio (Critico per impostazione predefinita) suonano comunque.

### Avvisi → Avvisa qualcun altro

Se un allarme non viene silenziato in tempo, le persone che scegli ricevono un messaggio Telegram, e un altro quando viene silenziato. Anche se di notte il telefono è in un'altra stanza, qualcuno in casa o un vicino lo viene a sapere. I messaggi sono inviati da un bot Telegram che crei tu; nessun altro server è coinvolto.

| Impostazione | A cosa serve |
|---|---|
| 1. Bot Telegram | In Telegram apri @BotFather, invia /newbot e dai un nome al tuo bot. Incolla qui sotto il token che BotFather ti fornisce. |
| 2. Persone da avvisare | Chiedi a ogni persona di aprire @your_bot in Telegram e toccare Start (puoi anche aggiungere il bot a un gruppo di famiglia). Poi tocca Trova persone. |
| 3. Quando | Per quanto può suonare un allarme prima di avvisare le persone (o subito). |
| Invia un messaggio di prova | Invia un messaggio di prova a tutti nell'elenco. |
| Disattiva | Disattiva l'avviso su Telegram. |

### Avvisi → Avvisi intelligenti

Avvisi che guardano tutti i tuoi dispositivi insieme: interruzioni di corrente, consumi insoliti e un riepilogo settimanale. Con Telegram puoi anche controllare lo stato, prendere in carico un allarme e azionare i dispositivi da remoto.

| Impostazione | A cosa serve |
|---|---|
| Interruzione di corrente: Rileva le interruzioni di corrente | Se tutti i dispositivi monitorati vanno offline insieme, ricevi un solo avviso di “interruzione di corrente” invece di uno per dispositivo, e un messaggio con la durata quando la corrente torna. |
| Caricabatterie come sensore di corrente | Se questo telefono/tablet resta collegato alla corrente nel luogo, essere scollegato per un minuto conta come interruzione di corrente; funziona anche quando manca Internet. Il messaggio parte tramite Telegram con i dati mobili. |
| In casa / Fuori casa: Avvisi più leggeri in casa | Quando sei in casa, gli allarmi dei dispositivi non critici diventano notifiche (come nelle ore di silenzio). I dispositivi critici suonano sempre. |
| Modalità vacanza | Risulti fuori casa, le ore di silenzio sono disattivate e ogni mattina alle 09:00 arriva un report. Termina da sola. |
| Consumo: Consumo insolito | Ogni mattina confronta ieri con le due settimane precedenti. Un uso molto più alto può indicare un guasto; molto più basso (un congelatore che ha lavorato appena) può indicare che ha smesso di funzionare. |
| Tariffa elettrica | Il prezzo predefinito per i dispositivi senza un prezzo proprio. Con una tariffa a tre fasce ogni ora è calcolata nella sua fascia: giorno 06–17, punta 17–22, notte 22–06. |
| Budget mensile | Ricevi una notifica una volta al mese quando il costo stimato dei dispositivi con misura dell'energia raggiunge questo importo. |
| Report settimanale | Ogni lunedì alle 09:00: interruzioni, allarmi e tempo di risposta, disconnessioni e consumo dell'ultima settimana. Inviato anche a Telegram, se configurato. |
| Mostra il report di questa settimana | Lo mostra subito. |
| Telegram: Comandi e pulsante “Ricevuto” | Le persone nel tuo elenco possono inviare /status e prendere in carico un allarme in corso. Il messaggio di allarme ha un pulsante “Ricevuto”: premendolo si silenzia il telefono. |
| Consenti di accendere e spegnere i dispositivi | Comandi /on e /off. Solo le persone nel tuo elenco possono usarli. |
| Avviso a fasi | Ogni persona riceve il messaggio dopo la propria attesa. Ad esempio un vicino dopo 5 minuti, un parente dopo 15, il gruppo di famiglia dopo 30. Se qualcuno tocca “Ricevuto”, le persone successive non vengono avvisate. |
| Chiamata per allarme critico | Quando un dispositivo della categoria Critico fa suonare l'allarme, la persona viene chiamata anche dalla linea di questo telefono allo scadere della sua attesa negli avvisi a fasi. La chiamata serve solo a svegliarla: cosa è successo è nel messaggio Telegram inviato nello stesso momento (Android non permette alle app di riprodurre audio in una chiamata). Chiedile di salvare questo numero come “Allarme SwitchGuard”. Le chiamate si ripetono ogni 2 minuti, fino a 3 volte, finché l'allarme non viene silenziato o qualcuno tocca “Ricevuto” su Telegram. I gruppi non possono essere chiamati. |
| Invia gli avvisi intelligenti a Telegram | Interruzioni di corrente, consumi insoliti, l'avviso di budget e il report settimanale vengono scritti anche su Telegram. |
| Watchdog | Due dispositivi con lo stesso bot si controllano a vicenda (ad es. il telefono e un box TV). Il dispositivo che invia tiene aggiornato un messaggio fissato nella chat di Telegram; quello che controlla fa scattare un allarme e avvisa i tuoi contatti se si ferma. Non togliere quel messaggio dai fissati. |

### Pagina del dispositivo (tocca un dispositivo)

| Impostazione | A cosa serve |
|---|---|
| Monitora questo dispositivo | Le variazioni vengono registrate e segnalate secondo le regole qui sotto |
| Quando succede questo | Si spegne, Si accende, Va offline, Torna online → Allarme / Notifica / Nessuna. L'allarme suona finché non lo silenzi. Notifica mostra una normale notifica. Nessuna registra solo l'evento nella cronologia. |
| Suono per questo dispositivo | Il suo suono d'allarme; altrimenti quello generale. |
| Categoria di avviso | In automatico: Critico se un evento fa suonare un allarme, Importante se gli eventi inviano solo notifiche |
| Energia | Potenza, tensione, corrente; oggi e questo mese; cronologia per giorno / mese / anno con costo; genera report (PDF / CSV). |
| Prezzo per kWh | Prezzo al kWh per questo dispositivo o prezzo predefinito; può anche diventare il predefinito. |
| Timer (sul dispositivo) | Nessun timer. I timer vengono eseguiti sul dispositivo stesso, anche a telefono spento. (Dispositivi eWeLink: aggiungi o modifica timer di accensione/spegnimento a un orario nei giorni scelti.) |
| Connessione | dBm è l'intensità del segnale: più vicino a 0 è meglio. Sopra -55 eccellente, fino a -67 buono, fino a -75 discreto, sotto debole. |
| Consumo in standby | La media oraria più bassa degli ultimi 7 giorni: la potenza sotto la quale questo dispositivo (e qualunque cosa vi sia collegata) non scende mai, anche quando “non fa nulla”. TV, modem e caricabatterie spesso assorbono questo in standby. |
| Avvisi dei sensori | Per i sensori: avvisi con un tocco, ad es. troppo caldo / troppo freddo, porta aperta, movimento o perdita d’acqua. |
| Cronologia del sensore | Grafico di 24 ore / 7 giorni / 30 giorni delle letture del sensore con minimo, massimo e media. |
| Copia dati del dispositivo | Copia negli appunti i dati grezzi del dispositivo, utile per segnalare un modello non supportato |

## Comandi Telegram

Le persone nel tuo elenco possono inviare /status e prendere in carico un allarme in corso. Il messaggio di allarme ha un pulsante “Ricevuto”: premendolo si silenzia il telefono.

```
Comandi SwitchGuard:
/status – monitoraggio, allarme e dispositivi
/ack – prendi in carico l'allarme che suona
/on nome [canale] – accende un dispositivo o un gruppo (più d'uno: /on kitchen, garden)
/off nome [canale] – spegne un dispositivo o un gruppo
/watt [nome] – potenza ora, oggi e consumo abituale
/temp – temperatura esterna
/scene nome – esegue una scena
/home · /away – imposta in casa / fuori casa

Puoi anche scrivere liberamente (in inglese o turco): “turn off kitchen and garden”, “heater watts”, “is the pump on”.

L'accensione e lo spegnimento dei dispositivi e le scene devono essere consentiti nell'app (Avvisi → Avvisi intelligenti).
```

Funzionano sia i nomi dei comandi in inglese sia quelli in turco (es. /status = /durum, /on = /ac). I comandi sono accettati solo dalle chat scelte.

## Autorizzazioni e perché

| Autorizzazione | Perché |
|---|---|
| `POST_NOTIFICATIONS` | Mostrare allarmi, notifiche e lo stato del monitoraggio. |
| `FOREGROUND_SERVICE` | Mantenere il monitoraggio in background (la notifica di stato permanente). |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Chiedere l'esclusione dall'ottimizzazione batteria, così gli allarmi non arrivano in ritardo. |
| `USE_FULL_SCREEN_INTENT` | Aprire l'allarme a schermo intero sopra la schermata di blocco. |
| `SYSTEM_ALERT_WINDOW` | Mostrare l'allarme sopra le altre app (Android TV, Xiaomi). |
| `WAKE_LOCK` | Tenere sveglio il processore mentre gestisce gli eventi («Massima affidabilità»). |
| `VIBRATE` | Vibrare durante l'allarme. |
| `RECEIVE_BOOT_COMPLETED` | Riavviare il monitoraggio dopo il riavvio del telefono. |
| `INTERNET` | Comunicare con il cloud eWeLink / Tuya (e con Telegram / Open-Meteo / OpenStreetMap se li usi). |
| `ACCESS_COARSE_LOCATION` | Solo quando tocchi «Usa la posizione di questo dispositivo» per le regole su alba/tramonto e temperatura. |
| `ACCESS_FINE_LOCATION` | Zone: un'area precisa per le regole di arrivo / uscita. |
| `ACCESS_BACKGROUND_LOCATION` | Zone: rilevare arrivi e uscite con l'app chiusa (chiesto solo quando crei una zona). |
| `CALL_PHONE` | «Chiamata per allarme critico»: chiamare il numero inserito dalla linea di questo telefono. |

## Privacy

- Nessun server proprio, nessuna analisi, nessuna pubblicità, nessun tracciamento. Niente viene venduto o condiviso. Anche le letture dei sensori e la loro cronologia restano sul tuo dispositivo e non vengono mai inviate altrove.
- Chiavi, token, dispositivi, regole, impostazioni e cronologia (max. 2.000 eventi) restano solo sul tuo dispositivo.
- L'app parla solo con i cloud che colleghi (eWeLink `*.coolkit.cc` / `*.coolkit.cn`, Tuya `openapi.tuya*.com`) e, solo se li usi, con Telegram, Open-Meteo (temperatura e meteo; posizione arrotondata a circa 100 m) e le tessere mappa di OpenStreetMap.
- La password eWeLink si inserisce nella pagina di eWeLink; l'app non la vede mai.
- I dati dell'app sono esclusi dal backup cloud di Android; i backup su file sono protetti da password; il trasferimento Wi-Fi è cifrato con un codice monouso e resta nella rete di casa.

Informativa sulla privacy completa: [Google Sites](https://sites.google.com/view/switchguard-privacy) · [docs/privacy-policy.md](docs/privacy-policy.md)

## Risoluzione dei problemi e FAQ

### L'allarme arriva in ritardo o non suona

| Problema | Cosa fare |
|---|---|
| L'attività in background è limitata | Il telefono potrebbe sospendere il monitoraggio; gli allarmi possono arrivare in ritardo o mancare. |
| L'allarme a schermo intero non è consentito | Con il telefono bloccato, l'allarme non può aprire la sua schermata grande. |
| Xiaomi blocca la schermata dell'allarme | Con il telefono bloccato, la schermata dell'allarme non può aprirsi. Tocca Risolvi, apri Altre autorizzazioni e attiva “Mostra nella schermata di blocco” e “Apri nuove finestre durante l'esecuzione in background”. |
| Le notifiche di allarme sono ridotte | La categoria di notifica “Allarmi” non è impostata su priorità alta, quindi l'allarme non comparirà in primo piano né occuperà lo schermo. |
| Non disturbare: silenzio totale | In questa modalità anche i suoni degli allarmi sono disattivati. |
| Il volume dell'allarme è molto basso | Il volume della sveglia del telefono è a zero o quasi, quindi potresti non sentire SwitchGuard. Risolvi attiva “Suona al volume massimo”. |
| Le notifiche sono disattivate | Allarmi e avvisi non possono essere mostrati. |
| Sul telefono c'è un secondo SwitchGuard | Un'altra copia di SwitchGuard è stata creata con la funzione App doppie (clonazione app) del telefono. Due copie in esecuzione insieme fanno suonare ogni allarme due volte e si disconnettono a vicenda se usano lo stesso account eWeLink; sui telefoni Xiaomi anche le autorizzazioni di questa copia possono risultare disattivate. Rimuovi la seconda copia: tieni premuta la sua icona (quella con il piccolo badge) e tocca Disinstalla. |

### Produttori che fermano le app in background

| Telefono | Cosa fare |
|---|---|
| Xiaomi | Necessario affinché il monitoraggio riparta dopo un riavvio o un aggiornamento. Tocca e attiva Avvio automatico per SwitchGuard. |
| Samsung | Samsung potrebbe mettere l'app in sospensione. In Batteria scegli "Senza limitazioni" e aggiungi SwitchGuard a "App mai in sospensione" (Batteria → Limiti di utilizzo in background). |
| Huawei / Honor | In Avvio app, imposta SwitchGuard su "Gestisci manualmente" e attiva Avvio automatico, Avvio secondario ed Esecuzione in background. |
| OPPO / realme / OnePlus | Attiva l'avvio automatico per SwitchGuard, poi nelle sue impostazioni della batteria consenti l'attività in background. |
| vivo / iQOO | Consenti a SwitchGuard di avviarsi in background e di consumare molta energia in background. |
| Tecno / Infinix | Attiva l'avvio automatico per SwitchGuard in Phone Master, così il monitoraggio sopravvive a riavvii e pulizie. |
| ASUS | Consenti SwitchGuard in Gestione avvio automatico, così il monitoraggio riparte dopo un riavvio. |

### Altre domande

**Il monitoraggio si è fermato e dice che sono stato disconnesso.**  
Suggerimento: eWeLink consente una sola sessione per account. Dai a ogni telefono il suo account eWeLink e condividi i dispositivi dall'app eWeLink.

**Manca un dispositivo Tuya condiviso con me.**  
Manca un dispositivo condiviso con te? Tuya elenca solo i dispositivi delle tue case. Chiedi al proprietario di aggiungerti come membro della casa in Smart Life (Io → Gestione casa → Aggiungi membro).

**I dispositivi Tuya non si aggiornano più.**  
La prova di Tuya Cloud è scaduta, quindi i dispositivi Tuya non vengono monitorati. Prolungala su platform.tuya.com → Cloud → IoT Core → Extend trial period.

**Le modifiche arrivano in ritardo / aggiornamenti istantanei non disponibili.**  
Aggiornamenti istantanei non disponibili per questo App ID; vengono usati controlli periodici (le variazioni più brevi dell'intervallo potrebbero sfuggire)

**Posso passare dalla versione Google Play a quella GitHub?**  
Sì, ma prima disinstalla l'altra: hanno firme diverse. Fai un backup su file prima di disinstallare e ripristinalo dopo.

**Come provo l'allarme senza svegliare nessuno?**  
Prova l'allarme ora: con suono o in silenzio, subito o dopo 10 secondi

**Perché il consumo è diverso da quello dell'app eWeLink / Smart Life?**  
Il consumo viene calcolato da SwitchGuard in base alle letture di potenza effettuate mentre il monitoraggio è attivo, quindi può differire leggermente dall'app del produttore del dispositivo (eWeLink, Smart Life).

## App per Windows

SwitchGuard per Windows (10 / 11) fa lo stesso monitoraggio su un PC: resta nell'area di notifica, fa suonare un allarme a schermo intero sul computer e condivide le stesse regole, automazioni e funzioni Telegram. Zone, widget, orologio e chiamate telefoniche sono solo sul telefono.

### Funzioni

- Le stesse schede: Dispositivi, Avvisi, Automazioni, Cronologia, Chat, Impostazioni; i comandi vocali della chat usano la digitazione vocale di Windows.
- Icona nell'area di notifica: apri, avvia / ferma il monitoraggio, spegni l'allarme, esci; chiudere la finestra non ferma il monitoraggio.
- Finestra d'allarme a schermo intero, suoni integrati (Sirena, Bip, Impulso, Carillon) o un tuo WAV / MP3.
- Schermata di standby e salvaschermo di Windows: il pannello si apre quando il PC è inattivo.
- Un portatile lasciato in carica funziona da sensore di blackout.
- Sensori eWeLink, condizioni dei sensori e cronologia dei sensori, come sul telefono.
- Aggiornamenti da GitHub con un clic; le impostazioni vengono mantenute. Backup in un file protetto da password (non ripristinabile su un telefono).

### Impostazioni solo di Windows

| Impostazione | A cosa serve |
|---|---|
| Windows | Perché SwitchGuard vigili su questo computer senza interruzioni. Se uno dei due è disattivato, gli allarmi possono sfuggire dopo un riavvio o mentre il computer è in sospensione. |
| Avvia con Windows | Si apre nell'area di notifica quando accedi a Windows |
| Tieni sveglio il computer | Durante il monitoraggio il computer non va in sospensione |
| Usa come screensaver di Windows | Quando il computer è inattivo si apre la schermata di standby; muovere il mouse o premere un tasto la chiude |
| Attesa | Per quanto tempo il computer deve restare inattivo prima che si apra la schermata di standby |
| Impostazioni screensaver di Windows | Ad es. «Al ripristino, torna alla schermata di accesso» |
| Apri la schermata di standby | Schermo intero; Esc chiude, clic per «Apri app». Lo schermo resta acceso finché è aperto. |
| Lingua e aspetto: Tema | Chiaro, scuro o come Windows. |
| Lingua | Scegli la lingua dell'app, delle notifiche e degli allarmi. |
| Aggiornamenti Tuya in tempo reale su questo computer | Disattivato: i dispositivi Tuya vengono controllati ogni 5 minuti |
| Interruzione di corrente quando questo computer passa a batteria | Per un portatile lasciato collegato a casa: se va a batteria per un minuto conta come interruzione di corrente, anche senza Internet (il messaggio Telegram parte quando torna la connessione). |
| Questo computer è sulla stessa alimentazione dei dispositivi | Finché è collegato alla rete non c'è interruzione di corrente, quindi un dispositivo che cade viene segnalato subito invece che dopo fino a 90 secondi. |
| Carica da file… | Usa un tuo WAV o MP3 per l'allarme o la notifica (primi 60 secondi, max. 20 MB). |
| Backup su file… | Tutte le impostazioni, le chiavi degli account e i suoni in un file protetto da password |

### Installazione su Windows

1. Fai doppio clic sul file di installazione (SwitchGuard-…-Kurulum.exe). Non chiede diritti di amministratore e si installa in pochi secondi.
2. Se Windows dice «Windows ha protetto il PC» (SmartScreen), premi Ulteriori informazioni → Esegui comunque. L'app non è ancora firmata; per questo avvisa.
3. SwitchGuard si installa nella tua cartella utente (AppData › Local › SwitchGuard) e aggiunge collegamenti al desktop e al menu Start.
4. Si avvia da solo nell'area di notifica (accanto all'orologio) quando accedi a Windows. Chiudere la finestra non lo ferma; per uscire usa l'icona nell'area di notifica → Esci.
5. Aggiungi i tuoi account in Impostazioni → Account (i passaggi per Tuya ed eWeLink sono qui sotto).
6. Passare a un altro computer: su quello vecchio, Impostazioni → Backup e trasferimento → Backup su file; installa sul nuovo, Ripristina da file, poi accedi di nuovo a eWeLink.
7. Per disinstallare: Impostazioni di Windows → App → App installate → SwitchGuard → Disinstalla.
8. Installare una nuova versione: prima disinstalla quella vecchia (passaggio 7), poi esegui il nuovo file di installazione. Impostazioni, account e suoni vengono mantenuti. (Il pulsante Aggiorna dell'app lo fa per te.)

## Compilazione

```
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest      # unit tests
```
Le build di rilascio richiedono `keystore.properties` + keystore nella cartella del progetto (non nel repository).

## Cronologia delle versioni

[English](CHANGELOG.md) · [Türkçe](CHANGELOG.tr.md) · [Releases](https://github.com/Macerce/switchguard/releases)

## Contatti

Domande, segnalazioni di bug e feedback (o apri una issue qui): **switchguardapp@gmail.com**

## Licenza

SwitchGuard è **source-available** (codice sorgente disponibile), non open source. Codice sorgente: [PolyForm Noncommercial 1.0.0](LICENSE). L'uso personale e gli altri usi non commerciali sono liberi; vendere il codice o usarlo a fini commerciali non è consentito. Le versioni fino alla 2.8.3 inclusa sono state pubblicate con licenza GPL-3.0.

Le build ufficiali (Google Play, [Releases](https://github.com/Macerce/switchguard/releases)) possono essere usate da chiunque, aziende comprese. Le versioni modificate non possono usare il nome né il logo di SwitchGuard. Licenze commerciali: **switchguardapp@gmail.com**

Non affiliata, approvata né sponsorizzata da eWeLink, CoolKit o ITEAD. Sonoff ed eWeLink sono marchi dei rispettivi proprietari. Nessun legame neanche con Tuya.
