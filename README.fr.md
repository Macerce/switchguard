[English](README.md) · [Türkçe](README.tr.md) · [Deutsch](README.de.md) · [Español](README.es.md) · [Português (Brasil)](README.pt-BR.md) · **Français** · [Italiano](README.it.md) · [Nederlands](README.nl.md) · [Polski](README.pl.md) · [Русский](README.ru.md) · [Українська](README.uk.md) · [العربية](README.ar.md) · [हिन्दी](README.hi.md) · [Bahasa Indonesia](README.id.md) · [Tiếng Việt](README.vi.md) · [ไทย](README.th.md) · [简体中文](README.zh-CN.md) · [繁體中文](README.zh-TW.md) · [日本語](README.ja.md) · [한국어](README.ko.md)

# SwitchGuard

SwitchGuard surveille les prises et interrupteurs connectés de vos propres comptes **eWeLink / SONOFF** et **Tuya / Smart Life** et déclenche une vraie alarme dès que quelque chose change : une pompe arrêtée, un congélateur débranché, un appareil qui a perdu la connexion. Il fonctionne sur téléphones et tablettes Android, Android TV et montres Wear OS, et il existe aussi une application Windows.

<p>
  <img src="docs/screenshots/play/en/v215/01-devices.png" width="200" alt="Appareils">
  <img src="docs/screenshots/play/en/v215/02-energy.png" width="200" alt="Énergie">
  <img src="docs/screenshots/play/en/v215/05-automations.png" width="200" alt="Automatisations">
  <img src="docs/screenshots/play/en/v215/08-alarm.png" width="200" alt="Alarme d'appareil">
</p>

> **Langues :** l’app Android (2.26+) et l’app Windows (0.13+) sont disponibles en 20 langues — Paramètres → Langue.

## Télécharger

### Android (téléphone, tablette, TV)
- **Google Play**: https://play.google.com/store/apps/details?id=com.macerce.switchguard
- **APK**: [dernière version Android](https://github.com/Macerce/switchguard/releases/latest) — téléchargez `SwitchGuard-x.y.z.apk` et ouvrez-le sur le téléphone. Si SwitchGuard est déjà installé depuis Google Play, désinstallez-le d'abord (signature différente). La version GitHub vous prévient quand une nouvelle version sort.

### Windows 10 / 11
- **Programme d'installation**: [versions Windows](https://github.com/Macerce/switchguard/releases?q=Windows&expanded=true) — téléchargez `SwitchGuard-x.y.z-Kurulum.exe` de la plus récente et lancez-le. L'application cherche elle-même les nouvelles versions et se met à jour en un clic ; les réglages sont conservés.

### Gratuit
- Entièrement gratuit sur Android et Windows — toutes les fonctions, tous les appareils surveillés, sans achat ni clé de licence. Les appareils de démo permettent de tout essayer sans matériel.

## Configuration requise

- Android 8.0+ (API 26) ; Wear OS pour l'application montre
- eWeLink : une application développeur eWeLink gratuite (App ID + App Secret) — le guide intégré vous accompagne pas à pas. eWeLink n'autorise qu'une session par compte et par App ID : chaque téléphone, tablette, TV ou PC a donc besoin de son propre compte eWeLink avec les appareils partagés ([dev.ewelink.cc](https://dev.ewelink.cc))
- Tuya / Smart Life : l'Access ID et l'Access Secret de votre propre projet Tuya Cloud gratuit, lié à votre compte de l'application Smart Life / Tuya ([platform.tuya.com](https://platform.tuya.com))
- Windows 10 / 11 (64 bits) pour l'application Windows

## Guides de configuration

- eWeLink (SONOFF): [English](docs/guide/ewelink.en.md) · [Türkçe](docs/guide/ewelink.tr.md)
- Tuya / Smart Life: [English](docs/guide/tuya.en.md) · [Türkçe](docs/guide/tuya.tr.md)

### Vidéos

- **New in 2.17 – alert categories, critical alerts, groups (EN):** https://youtube.com/shorts/iVM3zEiBt8A
- **2.17 yenilikleri – uyarı kategorileri, kritik uyarılar, gruplar (TR):** https://youtube.com/shorts/Qha83ziwylI
- New in 2.15 – smart alerts, scenes, energy reports (EN): https://youtube.com/shorts/bOtFBjHoefE
- 2.15 yenilikleri – akıllı uyarılar, sahneler, enerji raporu (TR): https://youtube.com/shorts/y3QRZvVoeK4
- New in 2.12 – a tour of the app (EN): https://youtube.com/shorts/jvm0hx0gATk
- 2.12 yenilikleri – uygulama turu (TR): https://youtube.com/shorts/2KeUgJrCCEU
- First setup in one minute / İlk kurulum (1 dk): https://youtube.com/shorts/jarBmMg2kYA
- **Getting eWeLink API keys / eWeLink API anahtarlarını alma (TR, sesli):** https://youtu.be/G5vQ_7CXTcM
- **Getting Tuya API keys / Tuya API anahtarlarını alma (TR, sesli):** https://youtu.be/8vPjJMoMaMU

## Fonctions

### Surveillance et alarme

- Mises à jour en direct via le WebSocket eWeLink et le service de messages Tuya, avec resynchronisation HTTP et vérifications régulières en secours.
- L'alarme sonne jusqu'à ce que vous l'arrêtiez — même en mode silencieux — et s'ouvre en plein écran sur l'écran de verrouillage ; chaque appareil peut avoir son propre son.
- Règles par appareil : alarme, notification ou rien quand un appareil s'éteint, s'allume, passe hors ligne ou revient en ligne.
- Alertes conditionnelles : sonne quand un appareil reste allumé ou éteint un certain temps, ou quand sa puissance dépasse / passe sous une limite (p. ex. une pompe sous 50 W).
- Ignorer les coupures brèves, avertissement quand la surveillance elle-même s'arrête (pas d'internet / de cloud), alerte sonore quand la session eWeLink est perdue (p. ex. même compte connecté sur un autre téléphone).
- Une carte de préparation affiche et corrige les réglages du téléphone qui pourraient empêcher l'alarme (restriction de batterie, autorisation plein écran, Ne pas déranger, volume d'alarme…).

### Appareils

- Tuiles façon maison connectée avec boutons rapides, un bouton par canal sur les appareils multicanaux, état et puissance en direct.
- Page de l'appareil : règles, son d'alarme propre, catégorie d'alerte, historique d'énergie et coût, minuteries eWeLink, signal Wi-Fi et diagnostic des coupures, consommation en veille et événements récents.
- **Organiser les appareils**: Étoile = garder en haut · flèches = déplacer · taille de la carte = petite ou grande · étiquette = catégorie. Appuyez longuement sur une carte pour revenir ici.

### Capteurs (eWeLink)

- Température / humidité (TH16, THR316/320, SNZB-02), porte (DW2, SNZB-04), mouvement / présence (SNZB-03, SNZB-06P), fuite d’eau (SNZB-05) et batterie sur les tuiles d’appareil, sur l’écran de veille et dans le Chat.
- Déclencheurs de capteur dans les automatisations (valeur au-dessus / en dessous, porte ouverte, mouvement, fuite d’eau) et alertes de capteur en un geste sur la page de l’appareil.
- Historique des capteurs : température, humidité, luminosité et batterie sont enregistrées pendant la surveillance ; la page de l’appareil affiche un graphique sur 24 heures / 7 jours / 30 jours avec minimum, maximum et moyenne.

### Alertes

- Alertes d'appareil et alertes conditionnelles au même endroit, avec filtre par catégorie d'alerte (Critique / Important / Normal).
- Réglages des alertes : son et volume de l'alarme, heures calmes, catégories d'alerte, Telegram (« Prévenir quelqu'un d'autre ») et alertes intelligentes — voir *Tous les réglages* plus bas.
- Alertes intelligentes : une seule alerte en cas de coupure de courant, consommation inhabituelle, budget mensuel, rapport hebdomadaire, mode vacances.

### Automatisations

- **Règles** — actions automatiques : *Si* quelque chose arrive, *alors* faire quelque chose, avec délai facultatif.
- Déclencheurs: s'allume, s'éteint, reste allumé pendant, reste éteint pendant, passe hors ligne, revient en ligne, la puissance dépasse, la puissance tombe sous, au lever du soleil, au coucher du soleil, quand la température extérieure dépasse, quand la température extérieure passe sous, à l'arrivée, au départ. Déclencheurs de capteur : valeur au-dessus / en dessous, porte ouverte / fermée, mouvement, fuite d’eau.
- Actions: Allumer, Éteindre, Déclencher l'alarme, Envoyer une notification, Lancer une scène, Démarrer un cycle, Arrêter un cycle.
- **Seulement quand**: Seulement à ces heures, Et seulement si un autre appareil…, Seulement à la maison / absent. Les conditions peuvent aussi vérifier une valeur de capteur, un état de capteur (porte ouverte, mouvement, fuite), la puissance de l’appareil ou la température extérieure ; ajoutez-en plusieurs et choisissez « Toutes les conditions » (et) ou « Une seule suffit » (ou).
- **Scènes** — Choisissez ce qui arrive à chaque appareil quand la scène s'exécute. Les appareils laissés sur « Ignorer » ne sont pas touchés.
- **Groupes** — Choisissez les appareils du groupe. Le groupe apparaît sur l'écran Appareils comme un appareil : son bouton les allume ou les éteint tous ensemble, et vous pouvez l'utiliser dans les automatisations.
- **Cycles et minuteries** — Compte à rebours, Allumé, puis éteint, Cycle allumé/éteint, Un par un, Étapes personnalisées. Démarrage par bouton, à une heure fixe les jours choisis ou pendant une plage horaire ; variation aléatoire facultative (maison qui semble habitée) et « arrêter si modifié à la main ».
- Les automatisations s'exécutent sur ce téléphone tant que la surveillance est active. Pour des programmations qui doivent fonctionner même téléphone éteint, utilisez les minuteries de la page de l'appareil.

### Énergie

- Vue par jour / mois / année avec total, coût estimé, puissance moyenne et de pointe, heure/jour/mois le plus chargé et résumé des points marquants.
- Tarif unique ou à trois plages (jour / pointe / nuit), prix par appareil ou prix par défaut.
- Consommation en veille (la puissance sous laquelle un appareil ne descend jamais) et son coût mensuel.
- Rapport d'énergie en PDF (à imprimer ou partager) ou CSV (Excel, Google Sheets).
- La consommation est calculée par SwitchGuard à partir des mesures de puissance prises pendant que la surveillance est active ; elle peut donc différer légèrement de l'application du fabricant (eWeLink, Smart Life).

### Historique

- Chaque événement avec date et heure, et qui ou quoi l'a provoqué (application, automatisation) ; filtre par appareil.
- Export CSV (dernières 24 heures, 7 jours, 30 jours ou tout) et effacement de l'historique.

### Chat

- Bonjour. Vous pouvez gérer vos appareils en écrivant : les allumer ou les éteindre, demander leur état, leur consommation ou la température extérieure (commandes en anglais ou en turc). Touchez le microphone pour le dire à la place (“Turn off kitchen”) ; la réponse est lue à voix haute. Des modèles prêts à l'emploi sont ci-dessous ; touchez-en un pour l'envoyer, maintenez-le pour le modifier.
- Les commandes en texte libre fonctionnent hors ligne et comprennent l'anglais et le turc ; le micro utilise la reconnaissance vocale du téléphone et les réponses peuvent être lues à voix haute.
- Modèles : messages en un geste qui peuvent aussi s'exécuter seuls chaque jour à une heure donnée ou quand un événement survient.

### Écran de veille

- Un tableau plein écran pour un téléphone ou une tablette en charge : horloge, météo, état critique, puissance en direct, activité récente et prochaines tâches. Il s'affiche aussi par-dessus l'écran de verrouillage.
- Ouvre les réglages Android : choisissez SwitchGuard sous Économiseur d'écran et réglez-le pour démarrer pendant la charge. Le tableau remplace alors l'écran de verrouillage sur le chargeur.

### Widgets, vignette des réglages rapides, raccourcis

- **SwitchGuard** — Indique si la surveillance est active et toute alarme en cours
- **Interrupteur SwitchGuard** — Allume et éteint un appareil ou une voie d'une simple touche. Il peut aussi être réglé sur À la maison / Absent ou lancer une scène.
- **Groupe SwitchGuard** — Grande tuile : allumez ou éteignez chaque appareil d'un groupe séparément
- **Commande vocale SwitchGuard** — Une seule tuile : touchez, dites-le (« Éteins la cuisine »), et c'est exécuté
- **Vignette des réglages rapides** — affiche l'état de la surveillance et la démarre ou l'arrête (le téléphone doit d'abord être déverrouillé) ; pendant une alarme, elle ouvre l'application.
- **Raccourcis de l'application** — appui long sur l'icône pour À la maison / Absent et vos scènes.

### À la maison / Absent

- **À la maison / Absent** — Les règles marquées « seulement si absent » ne s'exécutent pas. Passez sur Absent quand vous partez (aussi depuis le widget, la montre ou la commande Telegram /away).
- **Mode vacances** — Vous comptez comme absent, les heures calmes sont désactivées et un rapport arrive chaque matin à 09:00. Il se termine tout seul.
- **Zones** — Choisissez une zone sur la carte ; les automatisations s'exécutent quand votre téléphone y entre ou en sort (p. ex. allumer la lumière du jardin en rentrant, tout éteindre en partant). Les images de la carte sont téléchargées depuis OpenStreetMap ; votre position n'est utilisée que sur ce téléphone.

### Wear OS

- L'application montre SwitchGuard affiche les alarmes (à arrêter depuis le poignet), les appareils (allumer/éteindre), les scènes et À la maison / Absent ; elle se connecte à l'application du téléphone.

### Android TV

- Fonctionne sur Android TV avec la télécommande ; l'alarme s'affiche par-dessus ce qui est en cours de lecture (« Afficher l'alarme par-dessus les autres applications »).
- « Saisir depuis le téléphone » : la TV affiche un QR code ; vous saisissez les clés sur votre téléphone et elles arrivent chiffrées à la TV via le Wi-Fi de la maison.
- « Recevoir d'un autre appareil » : copie les clés Tuya, les règles et les réglages depuis votre téléphone par Wi-Fi.

### Plusieurs appareils

Téléphone, tablette, Android TV et PC : **chaque appareil a besoin de son propre compte eWeLink avec la fonction développeur approuvée, et des clés API de ce compte (App ID / Secret)** — eWeLink n'autorise qu'un appareil connecté par compte. Partagez vos appareils avec le compte supplémentaire depuis l'application eWeLink. Les clés Tuya peuvent servir sur plusieurs appareils. Détails : [English](docs/guide/ewelink.en.md#using-several-devices-phone-tablet-android-tv) · [Türkçe](docs/guide/ewelink.tr.md#birden-fazla-cihazda-kullanım-telefon-tablet-android-tv)

### Démo

- **Appareils de démo** — Appareils virtuels pour essayer toutes les fonctions. Ces boutons agissent comme si quelque chose s'était produit sur l'appareil lui-même (quelqu'un l'a actionné, le courant ou le Wi-Fi a été coupé) : vos règles s'appliquent et l'alarme peut sonner. Actionner les boutons ci-dessus compte comme vous et ne déclenche pas l'alarme.

## Tous les réglages

Tout est dans l'ordre de l'application. L'onglet **Réglages** regroupe les connexions et les options de l'application ; alarme, heures calmes, Telegram et alertes intelligentes sont dans l'onglet **Alertes** → Réglages des alertes ; les options propres à chaque appareil sont sur sa page.

### Réglages → Comptes

| Réglage | À quoi il sert |
|---|---|
| eWeLink — Se connecter avec eWeLink | Ouvre la page de connexion d'eWeLink elle-même ; l'application ne voit jamais votre mot de passe. |
| Identifiants API | App ID, App Secret et région de votre application développeur eWeLink. |
| Se déconnecter | La surveillance s'arrêtera. Vos réglages et votre historique sont conservés. |
| Tuya / Smart Life | Saisissez l'Access ID et l'Access Secret de votre projet Tuya Cloud (platform.tuya.com → Cloud → votre projet → Overview). Le compte de votre application Smart Life / Tuya doit être lié au projet. Centre de données: Europe centrale, Ouest des États-Unis, Chine, Inde |
| Déconnecter | Supprime la connexion Tuya ; les appareils Tuya ne sont plus surveillés. |
| Appareils de démo | Appareils virtuels pour essayer toutes les fonctions |

### Réglages → Surveillance

Comment SwitchGuard surveille vos appareils en arrière-plan.

| Réglage | À quoi il sert |
|---|---|
| Fiabilité maximale | Garde le processeur éveillé pour qu'aucun événement ne soit retardé. Consomme plus de batterie. |
| Ignorer les brèves déconnexions | Attend ce délai avant de signaler qu'un appareil est hors ligne. S'il revient à temps, aucune alerte n'est déclenchée. |
| Avertir si la surveillance s'arrête | Sans internet ou sans connexion au cloud pendant cette durée : vous recevez un avertissement |
| Intervalle de vérification de secours | Utilisé uniquement quand les mises à jour instantanées ne sont pas disponibles. |

### Réglages → Autorisations

Sans elles, Android peut empêcher l'alarme de vous parvenir.

| Réglage | À quoi il sert |
|---|---|
| Notifications | Les alarmes et avertissements ne peuvent pas être affichés. |
| Activité en arrière-plan | Le téléphone peut mettre la surveillance en veille ; les alarmes peuvent être tardives ou manquées. |
| Alarme en plein écran | Téléphone verrouillé, l'alarme ne peut pas ouvrir son grand écran. |
| Alarme sur l'écran de verrouillage (Xiaomi) | Téléphone verrouillé, l'écran d'alarme ne peut pas s'ouvrir. Touchez Corriger, ouvrez Autres autorisations et activez « Afficher sur l'écran de verrouillage » et « Ouvrir de nouvelles fenêtres en arrière-plan ». |
| Afficher l'alarme par-dessus les autres applications | Nécessaire sur Android TV pour que l'alarme apparaisse par-dessus ce qui est en cours de lecture. Touchez et autorisez SwitchGuard. |
| Démarrage en arrière-plan (Xiaomi, Huawei, OPPO, vivo…) | Affiché sur les téléphones qui bloquent le démarrage en arrière-plan (Xiaomi, Huawei, OPPO, vivo…) : permet à la surveillance de redémarrer après un redémarrage ou une mise à jour. Voir Dépannage. |

### Réglages → Localisation et zones

| Réglage | À quoi il sert |
|---|---|
| Position | Nécessaire pour les règles de lever/coucher du soleil et de température extérieure. Elle reste sur cet appareil ; pour la température, elle est envoyée à Open-Meteo arrondie à environ 100 m. |
| Zones | Choisissez une zone sur la carte ; les automatisations s'exécutent quand votre téléphone y entre ou en sort (p. ex. allumer la lumière du jardin en rentrant, tout éteindre en partant). Les images de la carte sont téléchargées depuis OpenStreetMap ; votre position n'est utilisée que sur ce téléphone. |

### Réglages → Écran de veille

| Réglage | À quoi il sert |
|---|---|
| Ouvrir l'écran de veille | Touchez l'écran pour « Ouvrir l'application » et « Fermer » |
| Utiliser comme économiseur d'écran | Ouvre les réglages Android : choisissez SwitchGuard sous Économiseur d'écran et réglez-le pour démarrer pendant la charge. Le tableau remplace alors l'écran de verrouillage sur le chargeur. |
| Atténuer la nuit | Le texte s'estompe entre 23:00 et 07:00 |

### Réglages → Résumé quotidien

| Réglage | À quoi il sert |
|---|---|
| Notification de résumé quotidien | Combien de fois chaque appareil s'est allumé, combien de temps il est resté allumé et combien d'énergie il a consommé aujourd'hui |
| Envoyer à | Heure d'envoi du résumé. |
| Afficher le résumé d'aujourd'hui maintenant | L'affiche tout de suite. |

### Réglages → Sauvegarde et transfert

| Réglage | À quoi il sert |
|---|---|
| Sauvegarder dans un fichier | Les règles, automatisations, réglages et clés Tuya et eWeLink sont enregistrés dans un fichier protégé par mot de passe |
| Restaurer depuis un fichier | Vous changez de téléphone ? Choisissez le fichier de sauvegarde et saisissez son mot de passe |
| Envoyer vers un autre appareil | Copier les clés Tuya, les règles et les réglages vers une TV ou une tablette sur le même Wi-Fi |
| Recevoir d'un autre appareil | Récupérer la configuration de votre téléphone via le Wi-Fi |

Le transfert Wi-Fi n'envoie jamais les clés API eWeLink ni la session eWeLink : sur l'autre appareil, saisissez ses propres clés API et connectez-vous avec son propre compte eWeLink. Une sauvegarde dans un fichier contient en revanche les clés eWeLink et Tuya, protégées par votre mot de passe.

### Réglages → Langue

| Réglage | À quoi il sert |
|---|---|
| Langue | Choisissez la langue de l'application, des notifications et des alarmes. Langue du téléphone ou l'une des 20 langues (à partir de la 2.26). |

### Réglages → Aide et à propos

| Réglage | À quoi il sert |
|---|---|
| Guide de configuration | Afficher à nouveau la configuration du premier lancement |
| Guide de configuration eWeLink | Pas à pas avec images (GitHub) |
| Guide de configuration Tuya / Smart Life | Pas à pas avec images (GitHub) |
| Envoyer un rapport de problème | Un problème ? Prépare un e-mail avec des informations de diagnostic. Rien n'est partagé tant que vous ne l'envoyez pas. |
| Supprimer les rapports de plantage enregistrés | Supprime les rapports de plantage enregistrés sur cet appareil. |
| Contact et commentaires | Écrit un e-mail au développeur (questions, bugs, idées). |
| Politique de confidentialité | Ouvre la politique de confidentialité. |
| À propos | Version, mention légale. La version GitHub vérifie sur GitHub s'il en existe une plus récente. |

### Alertes

| Réglage | À quoi il sert |
|---|---|
| Alertes des appareils | Pour quoi chaque appareil sonne ou notifie |
| Alertes conditionnelles | Sonner quand un appareil reste allumé ou éteint un certain temps, ou que sa puissance change |
| L'alarme est prête / Les alarmes risquent de ne pas vous parvenir | La carte de préparation en haut : liste les réglages du téléphone qui peuvent empêcher l'alarme et ouvre le bon écran pour les corriger ; avec aussi un bouton « Tester l'alarme ». |

### Alertes → Alarme

| Réglage | À quoi il sert |
|---|---|
| Son de l'alarme | Son d'alarme général. Chaque appareil peut avoir le sien (page de l'appareil). |
| Volume de l'alarme | Volume propre de l'alarme SwitchGuard. |
| Vibrer | Vibrer pendant que l'alarme sonne. |
| Sonner à plein volume | Monte le volume d'alarme du téléphone au maximum pendant que l'alarme sonne et met la musique en pause ; rétabli quand vous l'arrêtez |
| Sonner à nouveau si non résolu | Après l'arrêt d'une alarme, elle sonne de nouveau à cet intervalle tant que l'appareil reste dans l'état qui l'a déclenchée (toujours éteint, toujours allumé ou hors ligne). S'arrête quand l'appareil redevient normal. |
| Prévenir quelqu'un d'autre | Configuration de Telegram (voir plus bas). |
| Tester l'alarme | Essayez l'alarme maintenant : avec ou sans son, tout de suite ou après 10 secondes |

### Alertes → Catégories d'alerte

Chaque appareil a une catégorie d'alerte. Sauf si vous en choisissez une, elle est définie automatiquement : un appareil surveillé dont un événement est réglé sur Alarme est Critique, celui qui n'envoie que des notifications est Important, les autres sont Normaux. Les alertes conditionnelles et les automatisations peuvent utiliser une catégorie comme déclencheur (p. ex. « sonner l'alarme quand un appareil critique passe hors ligne »).

| Réglage | À quoi il sert |
|---|---|
| Critique · Important · Normal · Auto | En automatique : Critique si un événement déclenche une alarme, Important si les événements ne font que notifier |
| Sonner aussi pendant les heures calmes | Les alarmes sonnent aussi pendant les heures calmes / Pendant les heures calmes, les alarmes deviennent des notifications |

### Alertes → Heures calmes

| Réglage | À quoi il sert |
|---|---|
| Heures calmes | Pendant ces heures, les alarmes deviennent des notifications (les appareils critiques sonnent quand même) |
| Début / Fin | Heure de début et de fin des heures calmes. |

Les appareils des catégories d'alerte réglées pour sonner pendant les heures calmes (Critique par défaut) sonnent quand même.

### Alertes → Prévenir quelqu'un d'autre

Si une alarme n'est pas arrêtée à temps, les personnes que vous choisissez reçoivent un message Telegram, puis un autre une fois l'alarme arrêtée. Même si votre téléphone est dans une autre pièce la nuit, quelqu'un à la maison ou un voisin est prévenu. Les messages sont envoyés par un bot Telegram que vous créez ; aucun autre serveur n'intervient.

| Réglage | À quoi il sert |
|---|---|
| 1. Bot Telegram | Dans Telegram, ouvrez @BotFather, envoyez /newbot et nommez votre bot. Collez ci-dessous le jeton que BotFather vous donne. |
| 2. Personnes à prévenir | Demandez à chaque personne d'ouvrir @your_bot dans Telegram et de toucher Start (vous pouvez aussi ajouter le bot à un groupe familial). Touchez ensuite Trouver les personnes. |
| 3. Quand | Durée pendant laquelle une alarme peut sonner avant de prévenir les personnes (ou tout de suite). |
| Envoyer un message de test | Envoie un message de test à toutes les personnes de la liste. |
| Désactiver | Désactive la notification Telegram. |

### Alertes → Alertes intelligentes

Des alertes qui regardent tous vos appareils ensemble : coupures de courant, consommation inhabituelle et résumé hebdomadaire. Avec Telegram, vous pouvez aussi consulter l'état, prendre en charge une alarme et commander les appareils à distance.

| Réglage | À quoi il sert |
|---|---|
| Coupure de courant: Détecter les coupures de courant | Si tous les appareils surveillés passent hors ligne en même temps, vous recevez une seule alerte « coupure de courant » au lieu d'une par appareil, et un message avec la durée au retour du courant. |
| Chargeur comme capteur de courant | Si ce téléphone/cette tablette reste branché(e) sur le site, être débranché(e) pendant une minute compte comme une coupure de courant ; cela fonctionne aussi sans internet. Le message part par Telegram via les données mobiles. |
| À la maison / Absent: Alertes plus douces à la maison | À la maison, les alarmes des appareils non critiques deviennent des notifications (comme les heures calmes). Les appareils critiques sonnent toujours. |
| Mode vacances | Vous comptez comme absent, les heures calmes sont désactivées et un rapport arrive chaque matin à 09:00. Il se termine tout seul. |
| Consommation: Consommation inhabituelle | Chaque matin, compare hier aux deux semaines précédentes. Une consommation beaucoup plus élevée peut indiquer une panne ; beaucoup plus basse (un congélateur qui a à peine tourné) peut signifier qu'il a cessé de fonctionner. |
| Tarif de l'électricité | Le prix par défaut pour les appareils sans prix propre. Avec un tarif à trois niveaux, chaque heure est facturée dans sa propre tranche : jour 06–17, pointe 17–22, nuit 22–06. |
| Budget mensuel | Vous êtes prévenu une fois par mois quand le coût estimé des appareils avec mesure d'énergie atteint ce montant. |
| Rapport hebdomadaire | Chaque lundi à 09:00 : coupures, alarmes et temps de réaction, déconnexions et consommation de la semaine écoulée. Également envoyé sur Telegram s'il est configuré. |
| Afficher le rapport de cette semaine | L'affiche tout de suite. |
| Telegram: Commandes et bouton « Bien reçu » | Les personnes de votre liste peuvent envoyer /status et prendre en charge une alarme qui sonne. Le message d'alarme reçoit un bouton « Bien reçu » : l'appuyer coupe le son du téléphone. |
| Autoriser l'allumage et l'extinction des appareils | Commandes /on et /off. Seules les personnes de votre liste peuvent les utiliser. |
| Notification par étapes | Chaque personne reçoit un message après son propre délai. Par exemple un voisin après 5 minutes, un proche après 15, le groupe familial après 30. Si quelqu'un touche « Bien reçu », les personnes suivantes ne sont pas prévenues. |
| Appel en cas d'alarme critique | Quand un appareil de la catégorie Critique déclenche l'alarme, la personne est aussi appelée depuis la ligne de ce téléphone, une fois son délai écoulé dans les alertes par étapes. L'appel ne sert qu'à la réveiller : ce qui s'est passé figure dans le message Telegram envoyé en même temps (Android ne permet pas aux applications de jouer du son dans un appel). Demandez-lui d'enregistrer ce numéro sous « Alarme SwitchGuard ». Les appels se répètent toutes les 2 minutes, jusqu'à 3 fois, jusqu'à ce que l'alarme soit arrêtée ou que quelqu'un touche « Bien reçu » dans Telegram. Les groupes ne peuvent pas être appelés. |
| Envoyer les alertes intelligentes sur Telegram | Les coupures de courant, la consommation inhabituelle, l'alerte de budget et le rapport hebdomadaire sont aussi écrits sur Telegram. |
| Chien de garde | Deux appareils avec le même bot se surveillent mutuellement (p. ex. le téléphone et un boîtier TV). L'appareil émetteur tient à jour un message épinglé dans la conversation Telegram ; l'appareil qui surveille déclenche une alarme et prévient vos contacts si cela s'arrête. Ne désépinglez pas ce message. |

### Page de l'appareil (touchez un appareil)

| Réglage | À quoi il sert |
|---|---|
| Surveiller cet appareil | Les changements sont enregistrés et signalés selon les règles ci-dessous |
| Quand cela se produit | S'éteint, S'allume, Passe hors ligne, Passe en ligne → Alarme / Notifier / Aucune. L'alarme sonne jusqu'à ce que vous l'arrêtiez. Notifier affiche une notification normale. Aucune enregistre seulement l'événement dans l'historique. |
| Son pour cet appareil | Son d'alarme propre ; sinon le son général. |
| Catégorie d'alerte | En automatique : Critique si un événement déclenche une alarme, Important si les événements ne font que notifier |
| Énergie | Puissance, tension, courant ; aujourd'hui et ce mois-ci ; historique par jour / mois / année avec coût ; rapport (PDF / CSV). |
| Prix du kWh | Prix du kWh pour cet appareil ou prix par défaut ; peut aussi devenir le prix par défaut. |
| Minuteries (sur l'appareil) | Aucune minuterie. Les minuteries s'exécutent sur l'appareil lui-même, même téléphone éteint. (Appareils eWeLink : ajoutez ou modifiez des minuteries marche/arrêt à une heure fixe les jours choisis.) |
| Connexion | Le dBm mesure la force du signal : plus proche de 0, mieux c'est. Au-dessus de -55 excellent, jusqu'à -67 bon, jusqu'à -75 correct, en dessous faible. |
| Consommation en veille | La moyenne horaire la plus basse des 7 derniers jours : la puissance sous laquelle cet appareil (et tout ce qui y est branché) ne descend jamais, même en « ne faisant rien ». Les téléviseurs, modems et chargeurs consomment souvent cela en veille. |
| Alertes de capteur | Pour les capteurs : alertes en un geste, par ex. trop chaud / trop froid, porte ouverte, mouvement ou fuite d’eau. |
| Historique du capteur | Graphique sur 24 heures / 7 jours / 30 jours des mesures du capteur avec minimum, maximum et moyenne. |
| Copier les données de l'appareil | Copie les données brutes de l'appareil dans le presse-papiers, utile pour signaler un modèle non pris en charge |

## Commandes Telegram

Les personnes de votre liste peuvent envoyer /status et prendre en charge une alarme qui sonne. Le message d'alarme reçoit un bouton « Bien reçu » : l'appuyer coupe le son du téléphone.

```
Commandes SwitchGuard :
/status – surveillance, alarme et appareils
/ack – prendre en charge l'alarme qui sonne
/on nom [voie] – allumer un appareil ou un groupe (plusieurs : /on kitchen, garden)
/off nom [voie] – éteindre un appareil ou un groupe
/watt [nom] – puissance actuelle, du jour et habituelle
/temp – température extérieure
/scene nom – lancer une scène
/home · /away – régler à la maison / absent

Vous pouvez aussi écrire librement (en anglais ou en turc) : “turn off kitchen and garden”, “heater watts”, “is the pump on”.

L'allumage et l'extinction des appareils et les scènes doivent être autorisés dans l'application (Alertes → Alertes intelligentes).
```

Les noms de commande anglais et turcs fonctionnent tous les deux (p. ex. /status = /durum, /on = /ac). Les commandes ne sont acceptées que depuis les discussions choisies.

## Autorisations et raisons

| Autorisation | Pourquoi |
|---|---|
| `POST_NOTIFICATIONS` | Afficher les alarmes, les notifications et l'état de la surveillance. |
| `FOREGROUND_SERVICE` | Faire tourner la surveillance en arrière-plan (la notification d'état permanente). |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Demander à être exclu de l'optimisation de la batterie pour que les alarmes ne soient pas retardées. |
| `USE_FULL_SCREEN_INTENT` | Ouvrir l'alarme en plein écran sur l'écran de verrouillage. |
| `SYSTEM_ALERT_WINDOW` | Afficher l'alarme par-dessus les autres applications (Android TV, Xiaomi). |
| `WAKE_LOCK` | Garder le processeur éveillé pendant le traitement des événements (« Fiabilité maximale »). |
| `VIBRATE` | Vibrer pendant l'alarme. |
| `RECEIVE_BOOT_COMPLETED` | Relancer la surveillance après un redémarrage du téléphone. |
| `INTERNET` | Communiquer avec le cloud eWeLink / Tuya (et Telegram / Open-Meteo / OpenStreetMap si vous les utilisez). |
| `ACCESS_COARSE_LOCATION` | Uniquement quand vous touchez « Utiliser la position de cet appareil » pour les règles lever/coucher du soleil et température. |
| `ACCESS_FINE_LOCATION` | Zones : une zone précise pour les règles d'arrivée / de départ. |
| `ACCESS_BACKGROUND_LOCATION` | Zones : détecter arrivées et départs application fermée (demandé seulement à la création d'une zone). |
| `CALL_PHONE` | « Appel en cas d'alarme critique » : appeler le numéro saisi depuis la ligne de ce téléphone. |

## Confidentialité

- Pas de serveur propre, pas d'analyse, pas de publicité, pas de pistage. Rien n'est vendu ni partagé. Les mesures des capteurs et leur historique restent aussi sur votre appareil et ne sont jamais envoyés ailleurs.
- Clés, jetons, appareils, règles, réglages et historique (2 000 événements max.) sont stockés uniquement sur votre appareil.
- L'application ne communique qu'avec les clouds que vous connectez (eWeLink `*.coolkit.cc` / `*.coolkit.cn`, Tuya `openapi.tuya*.com`) et, seulement si vous les utilisez, avec Telegram, Open-Meteo (température et météo ; position arrondie à environ 100 m) et les tuiles de carte OpenStreetMap.
- Votre mot de passe eWeLink se saisit sur la page d'eWeLink elle-même ; l'application ne le voit jamais.
- Les données de l'application sont exclues de la sauvegarde cloud d'Android ; les sauvegardes en fichier sont protégées par mot de passe ; le transfert Wi-Fi est chiffré par un code à usage unique et reste dans votre réseau domestique.

Politique de confidentialité complète: [Google Sites](https://sites.google.com/view/switchguard-privacy) · [docs/privacy-policy.md](docs/privacy-policy.md)

## Dépannage et FAQ

### L'alarme arrive en retard ou ne sonne pas

| Problème | Que faire |
|---|---|
| L'activité en arrière-plan est restreinte | Le téléphone peut mettre la surveillance en veille ; les alarmes peuvent être tardives ou manquées. |
| L'alarme en plein écran n'est pas autorisée | Téléphone verrouillé, l'alarme ne peut pas ouvrir son grand écran. |
| Xiaomi bloque l'écran d'alarme | Téléphone verrouillé, l'écran d'alarme ne peut pas s'ouvrir. Touchez Corriger, ouvrez Autres autorisations et activez « Afficher sur l'écran de verrouillage » et « Ouvrir de nouvelles fenêtres en arrière-plan ». |
| Les notifications d'alarme sont atténuées | La catégorie de notifications « Alarmes » n'est pas en priorité élevée, donc l'alarme n'apparaîtra pas et n'occupera pas l'écran. |
| Ne pas déranger : silence total | Dans ce mode, même les sons d'alarme sont coupés. |
| Le volume de l'alarme est très bas | Le volume d'alarme du téléphone est à zéro ou presque, vous risquez de ne pas entendre SwitchGuard. « Corriger » active « Sonner à plein volume ». |
| Les notifications sont désactivées | Les alarmes et avertissements ne peuvent pas être affichés. |
| Il y a un second SwitchGuard sur ce téléphone | Une autre copie de SwitchGuard a été créée avec la fonction Applications doubles (clone d'application) du téléphone. Deux copies en parallèle font sonner chaque alarme deux fois et se déconnectent mutuellement quand elles utilisent le même compte eWeLink ; sur les téléphones Xiaomi, les autorisations de cette copie peuvent aussi sembler désactivées. Supprimez la seconde copie : maintenez son icône (celle avec le petit badge) et touchez Désinstaller. |

### Fabricants qui arrêtent les applications en arrière-plan

| Téléphone | Que faire |
|---|---|
| Xiaomi | Nécessaire pour que la surveillance redémarre après un redémarrage ou une mise à jour. Touchez et activez le démarrage automatique pour SwitchGuard. |
| Samsung | Samsung peut mettre l'application en veille. Dans Batterie, choisissez "Sans restriction" et ajoutez SwitchGuard aux "Applications jamais en veille" (Batterie → Limites d'utilisation en arrière-plan). |
| Huawei / Honor | Dans Lancement d'applications, réglez SwitchGuard sur "Gérer manuellement" et activez Lancement auto, Lancement secondaire et Exécution en arrière-plan. |
| OPPO / realme / OnePlus | Activez le lancement automatique pour SwitchGuard, puis, dans ses réglages de batterie, autorisez l'activité en arrière-plan. |
| vivo / iQOO | Autorisez SwitchGuard à démarrer en arrière-plan et autorisez une forte consommation en arrière-plan. |
| Tecno / Infinix | Activez le démarrage automatique pour SwitchGuard dans Phone Master afin que la surveillance survive aux redémarrages et aux nettoyages. |
| ASUS | Autorisez SwitchGuard dans le Gestionnaire de démarrage automatique pour que la surveillance redémarre après un redémarrage. |

### Autres questions

**La surveillance s'est arrêtée et indique que j'ai été déconnecté.**  
Astuce : eWeLink n'autorise qu'une session par compte. Donnez à chaque téléphone son propre compte eWeLink et partagez les appareils depuis l'application eWeLink.

**Un appareil Tuya partagé avec moi n'apparaît pas.**  
Un appareil partagé avec vous est absent ? Tuya ne liste que les appareils de vos foyers. Demandez au propriétaire de vous ajouter comme membre du foyer dans Smart Life (Moi → Gestion du foyer → Ajouter un membre).

**Les appareils Tuya ne se mettent plus à jour.**  
L'essai Tuya Cloud a expiré, donc les appareils Tuya ne sont plus surveillés. Prolongez-le sur platform.tuya.com → Cloud → IoT Core → Extend trial period.

**Les changements arrivent en retard / pas de mises à jour instantanées.**  
Mises à jour instantanées indisponibles pour cet App ID ; vérifications périodiques utilisées (les changements plus courts que l'intervalle peuvent être manqués)

**Puis-je passer de la version Google Play à la version GitHub ?**  
Oui, mais désinstallez d'abord l'autre : elles sont signées différemment. Sauvegardez dans un fichier avant de désinstaller, puis restaurez.

**Comment tester l'alarme sans réveiller tout le monde ?**  
Essayez l'alarme maintenant : avec ou sans son, tout de suite ou après 10 secondes

**Pourquoi la consommation diffère-t-elle de l'application eWeLink / Smart Life ?**  
La consommation est calculée par SwitchGuard à partir des mesures de puissance prises pendant que la surveillance est active ; elle peut donc différer légèrement de l'application du fabricant (eWeLink, Smart Life).

## Application Windows

SwitchGuard pour Windows (10 / 11) assure la même surveillance sur un PC : il reste dans la zone de notification, déclenche une alarme plein écran sur l'ordinateur et partage les mêmes règles, automatisations et fonctions Telegram. Zones, widgets, montre et appels téléphoniques sont réservés au téléphone.

### Fonctions

- Les mêmes onglets : Appareils, Alertes, Automatisations, Historique, Chat, Réglages ; les commandes vocales du chat utilisent la saisie vocale de Windows.
- Icône de la zone de notification : ouvrir, démarrer / arrêter la surveillance, arrêter l'alarme, quitter ; fermer la fenêtre n'arrête pas la surveillance.
- Fenêtre d'alarme plein écran, sons intégrés (Sirène, Bip, Pulsation, Carillon) ou votre propre WAV / MP3.
- Écran de veille SwitchGuard et économiseur d'écran Windows : le tableau s'ouvre quand le PC est inactif.
- Un portable laissé sur secteur sert de détecteur de coupure de courant.
- Capteurs eWeLink, conditions de capteur et historique des capteurs, comme sur le téléphone.
- Mises à jour GitHub en un clic ; les paramètres sont conservés. Sauvegarde dans un fichier protégé par mot de passe (non restaurable sur un téléphone).

### Réglages propres à Windows

| Réglage | À quoi il sert |
|---|---|
| Windows | Pour que SwitchGuard surveille sans interruption sur cet ordinateur. Si l'un des deux est désactivé, des alarmes peuvent être manquées après un redémarrage ou pendant la veille. |
| Démarrer avec Windows | S'ouvre dans la zone de notification à la connexion à Windows |
| Garder l'ordinateur éveillé | Pendant la surveillance, l'ordinateur ne se met pas en veille |
| Utiliser comme écran de veille Windows | Quand l'ordinateur est inactif, l'écran de veille s'ouvre ; bouger la souris ou appuyer sur une touche le ferme |
| Délai | Durée d'inactivité de l'ordinateur avant l'ouverture de l'écran de veille |
| Paramètres de l'écran de veille Windows | Par ex. « À la reprise, afficher l'écran de connexion » |
| Ouvrir l'écran de veille | Plein écran ; Échap ferme, cliquez pour « Ouvrir l'application ». L'écran reste allumé tant qu'il est ouvert. |
| Langue et apparence: Thème | Clair, sombre ou comme Windows. |
| Langue | Choisissez la langue de l'application, des notifications et des alarmes. |
| Mises à jour Tuya en direct sur cet ordinateur | Désactivé : les appareils Tuya sont vérifiés toutes les 5 minutes |
| Coupure de courant quand cet ordinateur passe sur batterie | Pour un portable laissé branché à la maison : s'il fonctionne sur batterie pendant une minute, cela compte comme une coupure de courant, même sans Internet (le message Telegram part dès le retour de la connexion). |
| Cet ordinateur est sur la même alimentation que les appareils | Tant qu'il est sur secteur, il n'y a pas de coupure de courant : un appareil qui décroche est donc signalé immédiatement au lieu d'après jusqu'à 90 secondes. |
| Charger depuis un fichier… | Utilisez votre propre WAV ou MP3 pour l'alarme ou la notification (60 premières secondes, 20 Mo max.). |
| Sauvegarder dans un fichier… | Tous les réglages, clés de compte et sons dans un fichier protégé par mot de passe |

### Installation sous Windows

1. Double-cliquez sur le fichier d'installation (SwitchGuard-…-Kurulum.exe). Il ne demande pas de droits d'administrateur et s'installe en quelques secondes.
2. Si Windows affiche « Windows a protégé votre ordinateur » (SmartScreen), appuyez sur Informations complémentaires → Exécuter quand même. L'application n'est pas encore signée, d'où cet avertissement.
3. SwitchGuard s'installe dans votre dossier utilisateur (AppData › Local › SwitchGuard) et ajoute des raccourcis sur le bureau et dans le menu Démarrer.
4. Il démarre tout seul dans la zone de notification (près de l'horloge) à la connexion à Windows. Fermer la fenêtre ne l'arrête pas ; pour quitter, icône de la zone de notification → Quitter.
5. Ajoutez vos comptes dans Réglages → Comptes (les étapes Tuya et eWeLink sont plus bas).
6. Passer à un autre ordinateur : sur l'ancien, Réglages → Sauvegarde et transfert → Sauvegarder dans un fichier ; installez sur le nouveau, Restaurer depuis un fichier, puis reconnectez-vous à eWeLink.
7. Pour désinstaller : Paramètres Windows → Applications → Applications installées → SwitchGuard → Désinstaller.
8. Installer une nouvelle version : désinstallez d'abord l'ancienne (étape 7), puis lancez le nouveau fichier d'installation. Vos réglages, comptes et sons sont conservés. (Le bouton Mettre à jour de l'app s'en charge pour vous.)

## Compilation

```
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest      # unit tests
```
Les versions de publication nécessitent `keystore.properties` + keystore à la racine du projet (absents du dépôt).

## Historique des versions

[English](CHANGELOG.md) · [Türkçe](CHANGELOG.tr.md) · [Releases](https://github.com/Macerce/switchguard/releases)

## Contact

Questions, rapports de bug et retours (ou ouvrez une issue ici) : **switchguardapp@gmail.com**

## Licence

SwitchGuard est **source-available** (code source consultable), pas open source. Code source : [PolyForm Noncommercial 1.0.0](LICENSE). L'usage personnel et tout autre usage non commercial sont libres ; vendre le code ou l'utiliser commercialement est interdit. Les versions jusqu'à la 2.8.3 incluse étaient publiées sous GPL-3.0.

Les versions officielles (Google Play, [Releases](https://github.com/Macerce/switchguard/releases)) peuvent être utilisées par tous, entreprises comprises. Les versions modifiées ne doivent pas utiliser le nom ni le logo SwitchGuard. Licence commerciale : **switchguardapp@gmail.com**

Non affilié, approuvé ni sponsorisé par eWeLink, CoolKit ou ITEAD. Sonoff et eWeLink sont des marques déposées de leurs propriétaires respectifs. Aucun lien non plus avec Tuya.
