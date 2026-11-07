[English](README.md) · [Türkçe](README.tr.md) · [Deutsch](README.de.md) · [Español](README.es.md) · [Português (Brasil)](README.pt-BR.md) · [Français](README.fr.md) · [Italiano](README.it.md) · [Nederlands](README.nl.md) · **Polski** · [Русский](README.ru.md) · [Українська](README.uk.md) · [العربية](README.ar.md) · [हिन्दी](README.hi.md) · [Bahasa Indonesia](README.id.md) · [Tiếng Việt](README.vi.md) · [ไทย](README.th.md) · [简体中文](README.zh-CN.md) · [繁體中文](README.zh-TW.md) · [日本語](README.ja.md) · [한국어](README.ko.md)

# SwitchGuard

SwitchGuard pilnuje inteligentnych gniazdek i przełączników na Twoich własnych kontach **eWeLink / SONOFF** i **Tuya / Smart Life** i włącza prawdziwy alarm, gdy tylko coś się zmieni — pompa stanęła, zamrażarka została wyłączona, urządzenie straciło połączenie. Działa na telefonach i tabletach z Androidem, Android TV i zegarkach Wear OS; jest też aplikacja na Windows.

<p>
  <img src="docs/screenshots/play/en/v215/01-devices.png" width="200" alt="Urządzenia">
  <img src="docs/screenshots/play/en/v215/02-energy.png" width="200" alt="Energia">
  <img src="docs/screenshots/play/en/v215/05-automations.png" width="200" alt="Automatyzacje">
  <img src="docs/screenshots/play/en/v215/08-alarm.png" width="200" alt="Alarm urządzenia">
</p>

> **Języki:** aplikacja na Windows jest dostępna w 20 językach od wersji 0.13.0. Na Androidzie wybór języka w aplikacji (20 języków) pojawi się w wersji **2.26**; najnowsza wersja na Androida to 2.25.0.

## Pobierz

### Android (telefon, tablet, TV)
- **Google Play**: https://play.google.com/store/apps/details?id=com.macerce.switchguard
- **APK**: [najnowsza wersja na Androida](https://github.com/Macerce/switchguard/releases/latest) — pobierz `SwitchGuard-x.y.z.apk` i otwórz na telefonie. Jeśli SwitchGuard jest już zainstalowany z Google Play, najpierw go odinstaluj (inny podpis). Wersja z GitHuba powiadomi Cię o nowszej.

### Windows 10 / 11
- **Instalator**: [wersje na Windows](https://github.com/Macerce/switchguard/releases?q=Windows&expanded=true) — pobierz `SwitchGuard-x.y.z-Kurulum.exe` z najnowszej i uruchom. Aplikacja sama szuka nowych wersji i aktualizuje się jednym kliknięciem; ustawienia zostają.

### Za darmo i Pro
- Za darmo: wszystkie funkcje dla 1 monitorowanego urządzenia. Urządzenia demo pozwalają wypróbować wszystko bez sprzętu.
- Android: Pro to jednorazowy zakup w Google Play (bez subskrypcji) i monitoruje wszystkie urządzenia; przechodzi z Twoim kontem Google na nowy telefon.
- Windows: klucz licencyjny Pro (Ustawienia → karta planu) monitoruje wszystkie urządzenia. Po klucz napisz na **switchguardapp@gmail.com**

## Wymagania

- Android 8.0+ (API 26); Wear OS dla aplikacji na zegarek
- eWeLink: darmowa aplikacja deweloperska eWeLink (App ID + App Secret) — przewodnik w aplikacji prowadzi krok po kroku. eWeLink pozwala na jedną sesję na konto i App ID, więc każdy telefon, tablet, TV lub komputer potrzebuje własnego konta eWeLink z udostępnionymi urządzeniami ([dev.ewelink.cc](https://dev.ewelink.cc))
- Tuya / Smart Life: Access ID i Access Secret Twojego własnego darmowego projektu Tuya Cloud, powiązanego z kontem aplikacji Smart Life / Tuya ([platform.tuya.com](https://platform.tuya.com))
- Windows 10 / 11 (64-bit) dla aplikacji na Windows

## Przewodniki konfiguracji

- eWeLink (SONOFF): [English](docs/guide/ewelink.en.md) · [Türkçe](docs/guide/ewelink.tr.md)
- Tuya / Smart Life: [English](docs/guide/tuya.en.md) · [Türkçe](docs/guide/tuya.tr.md)

### Filmy

- **New in 2.17 – alert categories, critical alerts, groups (EN):** https://youtube.com/shorts/iVM3zEiBt8A
- **2.17 yenilikleri – uyarı kategorileri, kritik uyarılar, gruplar (TR):** https://youtube.com/shorts/Qha83ziwylI
- New in 2.15 – smart alerts, scenes, energy reports (EN): https://youtube.com/shorts/bOtFBjHoefE
- 2.15 yenilikleri – akıllı uyarılar, sahneler, enerji raporu (TR): https://youtube.com/shorts/y3QRZvVoeK4
- New in 2.12 – a tour of the app (EN): https://youtube.com/shorts/jvm0hx0gATk
- 2.12 yenilikleri – uygulama turu (TR): https://youtube.com/shorts/2KeUgJrCCEU
- First setup in one minute / İlk kurulum (1 dk): https://youtube.com/shorts/jarBmMg2kYA
- **Getting eWeLink API keys / eWeLink API anahtarlarını alma (TR, sesli):** https://youtu.be/G5vQ_7CXTcM
- **Getting Tuya API keys / Tuya API anahtarlarını alma (TR, sesli):** https://youtu.be/8vPjJMoMaMU

## Funkcje

### Monitorowanie i alarm

- Aktualizacje na żywo przez WebSocket eWeLink i usługę wiadomości Tuya, z ponowną synchronizacją HTTP i okresowym sprawdzaniem jako zapas.
- Alarm dzwoni, dopóki go nie wyłączysz — nawet w trybie cichym — i otwiera się na pełnym ekranie nad ekranem blokady; każde urządzenie może mieć własny dźwięk.
- Reguły dla urządzenia: alarm, powiadomienie albo nic, gdy urządzenie się wyłączy, włączy, przejdzie w tryb offline lub wróci online.
- Alerty warunkowe: dzwoni, gdy urządzenie przez jakiś czas pozostaje włączone lub wyłączone albo jego moc przekroczy / spadnie poniżej progu (np. pompa poniżej 50 W).
- Ignorowanie krótkich rozłączeń, ostrzeżenie, gdy samo monitorowanie stanie (brak internetu / chmury), głośne ostrzeżenie po utracie sesji eWeLink (np. to samo konto zalogowane na innym telefonie).
- Karta gotowości pokazuje i naprawia ustawienia telefonu, które mogą zablokować alarm (ograniczenie baterii, uprawnienie pełnego ekranu, Nie przeszkadzać, głośność alarmu…).

### Urządzenia

- Kafelki w stylu smart home z szybkimi przyciskami, jeden przycisk na kanał w urządzeniach wielokanałowych, stan i moc na żywo.
- Strona urządzenia: reguły, własny dźwięk alarmu, kategoria alertu, historia energii i koszt, timery eWeLink, sygnał Wi-Fi i diagnoza zerwań, zużycie w czuwaniu i ostatnie zdarzenia.
- **Rozmieść urządzenia**: Gwiazdka = zawsze na górze · strzałki = przesuń · rozmiar karty = mała lub duża · etykieta = kategoria. Przytrzymaj kartę, aby tu wrócić.

### Alerty

- Alerty urządzeń i alerty warunkowe w jednym miejscu, z filtrem kategorii alertu (Krytyczne / Ważne / Zwykłe).
- Ustawienia alertów: dźwięk i głośność alarmu, godziny ciszy, kategorie alertów, Telegram („Powiadom kogoś jeszcze”) i inteligentne alerty — zobacz *Wszystkie ustawienia* niżej.
- Inteligentne alerty: jeden alert przy awarii prądu, nietypowe zużycie, budżet miesięczny, raport tygodniowy, tryb wakacyjny.

### Automatyzacje

- **Reguły** — automatyczne działania: *Jeśli* coś się stanie, *to* zrób coś, z opcjonalnym opóźnieniem.
- Wyzwalacze: włączy się, wyłączy się, pozostaje włączone przez, pozostaje wyłączone przez, przejdzie w tryb offline, wróci online, moc przekroczy, moc spadnie poniżej, o wschodzie słońca, o zachodzie słońca, gdy temperatura na zewnątrz wzrośnie powyżej, gdy temperatura na zewnątrz spadnie poniżej, po przybyciu, po wyjściu.
- Działania: Włącz, Wyłącz, Uruchom alarm, Wyślij powiadomienie, Uruchom scenę, Uruchom cykl, Zatrzymaj cykl.
- **Tylko gdy**: Tylko w tych godzinach, I tylko jeśli inne urządzenie…, Tylko w domu / poza domem.
- **Sceny** — Wybierz, co ma się stać z każdym urządzeniem po uruchomieniu sceny. Urządzenia ustawione na „Pomiń” nie są ruszane.
- **Grupy** — Wybierz urządzenia w grupie. Grupa pojawia się na ekranie Urządzenia jak urządzenie: jej przycisk włącza lub wyłącza wszystkie naraz, a możesz jej używać w automatyzacjach.
- **Cykle i timery** — Odliczanie, Włącz, potem wyłącz, Cykl wł./wył., Po kolei, Własne kroki. Start przyciskiem, o ustalonej godzinie w wybrane dni lub w przedziale godzin; opcjonalna losowa zmienność (dom wygląda na zamieszkany) i „zatrzymaj po ręcznej zmianie”.
- Automatyzacje działają na tym telefonie, gdy monitorowanie jest włączone. W przypadku harmonogramów, które muszą działać także przy wyłączonym telefonie, użyj timerów na stronie urządzenia.

### Energia

- Widok dnia / miesiąca / roku: suma, szacowany koszt, średnia i szczytowa moc, najbardziej obciążona godzina/dzień/miesiąc i podsumowanie najważniejszych punktów.
- Taryfa jednostrefowa lub trzystrefowa (dzień / szczyt / noc), cena dla urządzenia lub cena domyślna.
- Zużycie w czuwaniu (moc, poniżej której urządzenie nigdy nie spada) i jego miesięczny koszt.
- Raport energii jako PDF (do druku lub udostępnienia) albo CSV (Excel, Arkusze Google).
- Zużycie jest obliczane przez SwitchGuard na podstawie odczytów mocy wykonywanych przy włączonym monitorowaniu, więc może nieznacznie różnić się od aplikacji producenta urządzenia (eWeLink, Smart Life).

### Historia

- Każde zdarzenie z datą i godziną oraz tym, kto lub co je spowodowało (aplikacja, automatyzacja); filtr według urządzenia.
- Eksport do CSV (ostatnie 24 godziny, 7 dni, 30 dni lub wszystko) i czyszczenie historii.

### Czat

- Cześć. Możesz zarządzać urządzeniami, pisząc: włączaj je lub wyłączaj, pytaj o stan, pobór mocy albo temperaturę na zewnątrz (polecenia po angielsku lub turecku). Dotknij mikrofonu, aby powiedzieć polecenie („Turn off kitchen”); odpowiedź zostanie odczytana na głos. Gotowe szablony są poniżej; dotknij jednego, aby go wysłać, przytrzymaj, aby edytować.
- Polecenia pisane swobodnie działają offline i rozumieją angielski oraz turecki; mikrofon korzysta z rozpoznawania mowy telefonu, a odpowiedzi mogą być czytane na głos.
- Szablony: wiadomości jednym dotknięciem, które mogą też uruchamiać się same codziennie o danej godzinie lub gdy coś się wydarzy.

### Ekran gotowości

- Pełnoekranowa tablica dla ładującego się telefonu lub tabletu: zegar, pogoda, stan krytyczny, moc na żywo, ostatnia aktywność i to, co uruchomi się następne. Pokazuje się także nad ekranem blokady.
- Otwiera ustawienia Androida: w Wygaszaczu ekranu wybierz SwitchGuard i ustaw uruchamianie podczas ładowania. Tablica zastąpi wtedy ekran blokady na ładowarce.

### Widżety, kafelek szybkich ustawień, skróty

- **SwitchGuard** — Pokazuje, czy monitorowanie działa, oraz dzwoniący alarm
- **Przełącznik SwitchGuard** — Jednym dotknięciem włącza i wyłącza urządzenie lub kanał. Można go też ustawić na W domu / Poza domem lub uruchamianie sceny.
- **Grupa SwitchGuard** — Duży kafelek: włączaj lub wyłączaj każde urządzenie w grupie osobno
- **Polecenie głosowe SwitchGuard** — Jeden kafelek: dotknij, powiedz („Turn off kitchen”), a polecenie zostanie wykonane
- **Kafelek szybkich ustawień** — pokazuje stan monitorowania i uruchamia je lub zatrzymuje (najpierw trzeba odblokować telefon); gdy dzwoni alarm, otwiera aplikację.
- **Skróty aplikacji** — przytrzymaj ikonę aplikacji, aby wybrać W domu / Poza domem i swoje sceny.

### W domu / Poza domem

- **W domu / Poza domem** — Reguły oznaczone „tylko poza domem” nie działają. Przełącz na Poza domem, gdy wychodzisz (także z widżetu, zegarka lub przez Telegram /away).
- **Tryb wakacyjny** — Jesteś traktowany jako poza domem, godziny ciszy są wyłączone, a każdego ranka o 09:00 przychodzi raport. Kończy się sam.
- **Strefy** — Wybierz obszar na mapie; automatyzacje uruchamiają się, gdy telefon wejdzie do niego lub go opuści (np. włącz lampę w ogrodzie, gdy wrócisz do domu, wyłącz wszystko, gdy wyjdziesz). Obrazy map są pobierane z OpenStreetMap; Twoja lokalizacja jest używana tylko na tym telefonie.

### Wear OS

- Aplikacja SwitchGuard na zegarek pokazuje alarmy (wyłączanie z nadgarstka), urządzenia (przełączanie), sceny oraz W domu / Poza domem; łączy się z aplikacją na telefonie.

### Android TV

- Działa na Android TV z pilotem; alarm pojawia się nad wszystkim, co jest odtwarzane („Pokazuj alarm nad innymi aplikacjami”).
- „Wpisz z telefonu”: TV pokazuje kod QR; wpisujesz klucze na telefonie, a one trafiają do TV zaszyfrowane przez domowe Wi-Fi.
- „Odbierz z innego urządzenia”: kopiuje klucze Tuya, reguły i ustawienia z telefonu przez Wi-Fi.

### Kilka urządzeń

Telefon, tablet, Android TV i komputer: **każde urządzenie potrzebuje własnego konta eWeLink z zatwierdzoną funkcją deweloperską oraz kluczy API tego konta (App ID / Secret)** — eWeLink pozwala na jedno zalogowane urządzenie na konto. Udostępnij swoje urządzenia dodatkowemu kontu w aplikacji eWeLink. Klucze Tuya można używać na kilku urządzeniach. Pro kupuje się raz i działa na wszystkich urządzeniach z Androidem z tym samym kontem Google. Szczegóły: [English](docs/guide/ewelink.en.md#using-several-devices-phone-tablet-android-tv) · [Türkçe](docs/guide/ewelink.tr.md#birden-fazla-cihazda-kullanım-telefon-tablet-android-tv)

### Demo i Pro

- **Urządzenia demo** — Wirtualne urządzenia do wypróbowania wszystkich funkcji. Działają tak, jakby coś wydarzyło się na samym urządzeniu (ktoś je przełączył, zabrakło prądu lub Wi-Fi), więc obowiązują Twoje reguły i może zadzwonić alarm. Przełączenie przyciskami powyżej liczy się jako Twoje działanie i nie uruchamia alarmu.
- Wersja bezpłatna monitoruje urządzenia: 1. Pro monitoruje wszystkie Twoje urządzenia, ze wszystkimi funkcjami. Jednorazowa płatność, bez subskrypcji. Powiązana z Twoim kontem Google, więc przenosi się z Tobą na nowy telefon.

## Wszystkie ustawienia

Wszystko w kolejności z aplikacji. Karta **Ustawienia** zawiera połączenia i opcje aplikacji; alarm, godziny ciszy, Telegram i inteligentne alerty są w karcie **Alerty** → Ustawienia alertów; opcje poszczególnych urządzeń są na ich stronach.

### Ustawienia → Konta

| Ustawienie | Do czego służy |
|---|---|
| Pro / Kup Pro | Wersja bezpłatna monitoruje urządzenia: 1. Pro monitoruje wszystkie Twoje urządzenia, ze wszystkimi funkcjami. Jednorazowa płatność, bez subskrypcji. Powiązana z Twoim kontem Google, więc przenosi się z Tobą na nowy telefon. |
| Przywróć | Ponownie sprawdza Twoje zakupy w Google Play (nowy telefon, ponowna instalacja). |
| eWeLink — Zaloguj się przez eWeLink | Otwiera własną stronę logowania eWeLink; aplikacja nigdy nie widzi Twojego hasła. |
| Dane uwierzytelniające API | App ID, App Secret i region Twojej aplikacji deweloperskiej eWeLink. |
| Wyloguj się | Monitorowanie zostanie zatrzymane. Twoje ustawienia i historia zostaną zachowane. |
| Tuya / Smart Life | Wpisz Access ID i Access Secret swojego projektu Tuya Cloud (platform.tuya.com → Cloud → Twój projekt → Overview). Konto aplikacji Smart Life / Tuya musi być połączone z projektem. Centrum danych: Europa Środkowa, Zachodnia Ameryka, Chiny, Indie |
| Rozłącz | Usuwa połączenie z Tuya; urządzenia Tuya przestają być monitorowane. |
| Urządzenia demo | Wirtualne urządzenia do wypróbowania wszystkich funkcji |

### Ustawienia → Monitorowanie

Jak SwitchGuard czuwa nad Twoimi urządzeniami w tle.

| Ustawienie | Do czego służy |
|---|---|
| Maksymalna niezawodność | Utrzymuje procesor w stanie czuwania, aby żadne zdarzenie nie było opóźnione. Zużywa więcej baterii. |
| Ignoruj krótkie rozłączenia | Tyle czasu aplikacja czeka, zanim zgłosi, że urządzenie przeszło w tryb offline. Jeśli wróci na czas, alert nie zostanie wysłany. |
| Ostrzegaj, gdy monitorowanie się zatrzyma | Brak internetu lub połączenia z chmurą przez tak długi czas: dostaniesz ostrzeżenie |
| Interwał kontroli zapasowej | Używany tylko wtedy, gdy natychmiastowe aktualizacje są niedostępne. |

### Ustawienia → Uprawnienia

Bez nich Android może uniemożliwić dotarcie alarmu do Ciebie.

| Ustawienie | Do czego służy |
|---|---|
| Powiadomienia | Nie można wyświetlać alarmów ani ostrzeżeń. |
| Aktywność w tle | Telefon może usypiać monitorowanie; alarmy mogą się spóźniać lub nie dotrzeć. |
| Alarm na pełnym ekranie | Gdy telefon jest zablokowany, alarm nie może otworzyć dużego ekranu. |
| Alarm na ekranie blokady (Xiaomi) | Gdy telefon jest zablokowany, ekran alarmu nie może się otworzyć. Dotknij Napraw, otwórz Inne uprawnienia i włącz „Wyświetlaj na ekranie blokady” oraz „Otwieraj nowe okna podczas działania w tle”. |
| Pokazuj alarm nad innymi aplikacjami | Potrzebne na Android TV, aby alarm pojawiał się na wierzchu tego, co jest odtwarzane. Dotknij i zezwól SwitchGuard. |
| Uruchamianie w tle (Xiaomi, Huawei, OPPO, vivo…) | Widoczne na telefonach blokujących start w tle (Xiaomi, Huawei, OPPO, vivo…): pozwala monitorowaniu wystartować po ponownym uruchomieniu lub aktualizacji. Zobacz Rozwiązywanie problemów. |

### Ustawienia → Lokalizacja i strefy

| Ustawienie | Do czego służy |
|---|---|
| Lokalizacja | Potrzebna do reguł wschodu/zachodu słońca i temperatury na zewnątrz. Pozostaje na tym urządzeniu; w przypadku temperatury jest wysyłana do Open-Meteo, zaokrąglona do około 100 m. |
| Strefy | Wybierz obszar na mapie; automatyzacje uruchamiają się, gdy telefon wejdzie do niego lub go opuści (np. włącz lampę w ogrodzie, gdy wrócisz do domu, wyłącz wszystko, gdy wyjdziesz). Obrazy map są pobierane z OpenStreetMap; Twoja lokalizacja jest używana tylko na tym telefonie. |

### Ustawienia → Ekran gotowości

| Ustawienie | Do czego służy |
|---|---|
| Otwórz ekran gotowości | Dotknij ekranu, aby zobaczyć „Otwórz aplikację” i „Zamknij” |
| Użyj jako wygaszacza ekranu | Otwiera ustawienia Androida: w Wygaszaczu ekranu wybierz SwitchGuard i ustaw uruchamianie podczas ładowania. Tablica zastąpi wtedy ekran blokady na ładowarce. |
| Przyciemniaj w nocy | Tekst przygasa między 23:00 a 07:00 |

### Ustawienia → Podsumowanie dzienne

| Ustawienie | Do czego służy |
|---|---|
| Powiadomienie z podsumowaniem dziennym | Ile razy każde urządzenie się włączyło, jak długo było włączone i ile energii zużyło dzisiaj |
| Wyślij o | Godzina wysłania podsumowania. |
| Pokaż teraz dzisiejsze podsumowanie | Pokazuje je od razu. |

### Ustawienia → Kopia zapasowa i przenoszenie

| Ustawienie | Do czego służy |
|---|---|
| Utwórz kopię w pliku | Reguły, automatyzacje, ustawienia oraz klucze Tuya i eWeLink trafią do pliku chronionego hasłem |
| Przywróć z pliku | Przechodzisz na nowy telefon? Wybierz plik kopii i podaj jego hasło |
| Wyślij na inne urządzenie | Skopiuj klucze Tuya, reguły i ustawienia na telewizor lub tablet w tej samej sieci Wi-Fi |
| Odbierz z innego urządzenia | Pobierz konfigurację z telefonu przez Wi-Fi |

Przesyłanie przez Wi-Fi nigdy nie wysyła kluczy API eWeLink ani sesji eWeLink: na drugim urządzeniu wpisz jego własne klucze API i zaloguj się jego własnym kontem eWeLink. Kopia zapasowa w pliku zawiera natomiast klucze eWeLink i Tuya, chronione Twoim hasłem.

### Ustawienia → Język

| Ustawienie | Do czego służy |
|---|---|
| Język | Wybierz język aplikacji, powiadomień i alarmów. Język telefonu lub jeden z 20 języków (od 2.26). |

### Ustawienia → Pomoc i informacje

| Ustawienie | Do czego służy |
|---|---|
| Kreator konfiguracji | Pokaż ponownie konfigurację przy pierwszym uruchomieniu |
| Poradnik konfiguracji eWeLink | Krok po kroku z obrazkami (GitHub) |
| Poradnik konfiguracji Tuya / Smart Life | Krok po kroku z obrazkami (GitHub) |
| Wyślij zgłoszenie problemu | Coś nie działa? Przygotuje e-mail ze szczegółami diagnostycznymi. Nic nie zostanie udostępnione, dopóki go nie wyślesz. |
| Usuń zapisane raporty awarii | Usuwa raporty awarii zapisane na tym urządzeniu. |
| Kontakt i opinie | Pisze e-mail do twórcy (pytania, błędy, pomysły). |
| Polityka prywatności | Otwiera politykę prywatności. |
| Informacje | Wersja, nota prawna. Wersja z GitHuba sprawdza na GitHubie, czy jest nowsza. |

### Alerty

| Ustawienie | Do czego służy |
|---|---|
| Alerty urządzeń | Z jakiego powodu każde urządzenie dzwoni lub powiadamia |
| Alerty warunkowe | Dzwoń, gdy urządzenie przez jakiś czas pozostaje włączone lub wyłączone albo zmienia się jego moc |
| Alarm jest gotowy / Alarmy mogą do Ciebie nie dotrzeć | Karta gotowości u góry: wymienia ustawienia telefonu, które mogą zablokować alarm, i otwiera właściwy ekran do naprawy; jest tu też przycisk „Alarm testowy”. |

### Alerty → Alarm

| Ustawienie | Do czego służy |
|---|---|
| Dźwięk alarmu | Ogólny dźwięk alarmu. Każde urządzenie może mieć własny (strona urządzenia). |
| Głośność alarmu | Własna głośność alarmu SwitchGuard. |
| Wibracje | Wibracje podczas alarmu. |
| Dzwoń z pełną głośnością | Podnosi głośność alarmu w telefonie do maksimum, gdy alarm dzwoni, i wstrzymuje muzykę; po wyłączeniu alarmu przywraca poprzednią głośność |
| Dzwoń ponownie, jeśli nie rozwiązano | Po wyłączeniu alarmu dzwoni on ponownie w tym odstępie, dopóki urządzenie pozostaje w stanie, który go wywołał (nadal wyłączone, nadal włączone lub offline). Kończy się, gdy urządzenie wróci do normy. |
| Powiadom kogoś jeszcze | Konfiguracja Telegrama (zobacz niżej). |
| Alarm testowy | Wypróbuj alarm teraz: z dźwiękiem lub bezgłośnie, od razu lub po 10 sekundach |

### Alerty → Kategorie alertów

Każde urządzenie ma kategorię alertu. Jeśli jej nie wybierzesz, jest ustawiana automatycznie: monitorowane urządzenie z jakimkolwiek zdarzeniem ustawionym na Alarm jest Krytyczne, to, które tylko wysyła powiadomienia, jest Ważne, pozostałe są Zwykłe. Alerty warunkowe i automatyzacje mogą używać kategorii jako wyzwalacza (np. „uruchom alarm, gdy dowolne urządzenie krytyczne przejdzie w tryb offline”).

| Ustawienie | Do czego służy |
|---|---|
| Krytyczne · Ważne · Zwykłe · Auto | Gdy automatyczna: Krytyczne, jeśli jakiekolwiek zdarzenie uruchamia alarm, Ważne, jeśli zdarzenia tylko powiadamiają |
| Dzwoń także w godzinach ciszy | Alarmy dzwonią także w godzinach ciszy / W godzinach ciszy alarmy zamieniają się w powiadomienia |

### Alerty → Godziny ciszy

| Ustawienie | Do czego służy |
|---|---|
| Godziny ciszy | W tych godzinach alarmy zamieniają się w powiadomienia (urządzenia krytyczne nadal dzwonią) |
| Początek / Koniec | Godzina początku i końca ciszy. |

Urządzenia w kategoriach alertów ustawionych tak, by dzwoniły w godzinach ciszy (domyślnie Krytyczne), nadal dzwonią.

### Alerty → Powiadom kogoś jeszcze

Jeśli alarm nie zostanie wyłączony na czas, wybrane przez Ciebie osoby dostaną wiadomość na Telegramie, a kolejną po jego wyłączeniu. Nawet jeśli w nocy telefon jest w innym pokoju, ktoś z domowników lub sąsiad się dowie. Wiadomości wysyła bot Telegrama, którego sam tworzysz; żaden inny serwer nie bierze w tym udziału.

| Ustawienie | Do czego służy |
|---|---|
| 1. Bot Telegrama | W Telegramie otwórz @BotFather, wyślij /newbot i nadaj nazwę swojemu botowi. Wklej poniżej token, który poda BotFather. |
| 2. Osoby do powiadomienia | Poproś każdą osobę, aby otworzyła @your_bot w Telegramie i dotknęła Start (możesz też dodać bota do rodzinnej grupy). Następnie dotknij Znajdź osoby. |
| 3. Kiedy | Jak długo alarm może dzwonić, zanim ludzie zostaną powiadomieni (lub od razu). |
| Wyślij wiadomość testową | Wysyła wiadomość testową do wszystkich z listy. |
| Wyłącz | Wyłącza powiadamianie przez Telegram. |

### Alerty → Inteligentne alerty

Alerty, które patrzą na wszystkie urządzenia razem: awarie zasilania, nietypowe zużycie i podsumowanie tygodnia. Przez Telegram możesz też sprawdzić stan, przejąć alarm i przełączać urządzenia zdalnie.

| Ustawienie | Do czego służy |
|---|---|
| Awaria zasilania: Wykrywaj awarie zasilania | Jeśli wszystkie monitorowane urządzenia przejdą w tryb offline jednocześnie, dostaniesz jeden alert „awaria zasilania” zamiast jednego na urządzenie oraz wiadomość z czasem trwania po powrocie zasilania. |
| Ładowarka jako czujnik zasilania | Jeśli ten telefon/tablet pozostaje podłączony w miejscu monitorowania, odłączenie na minutę liczy się jako awaria zasilania; działa to także bez internetu. Wiadomość jest wysyłana przez Telegram za pomocą danych komórkowych. |
| W domu / Poza domem: Łagodniejsze alerty w domu | Gdy jesteś w domu, alarmy urządzeń niekrytycznych zamieniają się w powiadomienia (jak w godzinach ciszy). Urządzenia krytyczne zawsze dzwonią. |
| Tryb wakacyjny | Jesteś traktowany jako poza domem, godziny ciszy są wyłączone, a każdego ranka o 09:00 przychodzi raport. Kończy się sam. |
| Zużycie: Nietypowe zużycie | Każdego ranka porównuje wczorajszy dzień z poprzednimi dwoma tygodniami. Znacznie wyższe zużycie może oznaczać usterkę; znacznie niższe (zamrażarka, która ledwo pracowała) może oznaczać, że przestała działać. |
| Taryfa energii elektrycznej | Cena domyślna dla urządzeń bez własnej ceny. Przy taryfie trójstrefowej każda godzina jest wyceniana w swoim paśmie: dzień 06–17, szczyt 17–22, noc 22–06. |
| Budżet miesięczny | Raz w miesiącu dostaniesz powiadomienie, gdy szacowany koszt urządzeń z pomiarem energii osiągnie tę kwotę. |
| Raport tygodniowy | W każdy poniedziałek o 09:00: awarie zasilania, alarmy i czas reakcji, rozłączenia oraz zużycie z ostatniego tygodnia. Wysyłany też na Telegram, jeśli skonfigurowano. |
| Pokaż raport z tego tygodnia | Pokazuje je od razu. |
| Telegram: Polecenia i przycisk „Rozumiem” | Osoby z Twojej listy mogą wysłać /status i przejąć dzwoniący alarm. Wiadomość o alarmie ma przycisk „Rozumiem”: jego naciśnięcie wycisza telefon. |
| Zezwól na włączanie i wyłączanie urządzeń | Polecenia /on i /off. Mogą ich używać tylko osoby z Twojej listy. |
| Powiadamianie etapami | Każda osoba dostaje wiadomość po własnym czasie oczekiwania. Na przykład sąsiad po 5 minutach, krewny po 15, grupa rodzinna po 30. Jeśli ktoś dotknie „Rozumiem”, kolejne osoby nie dostaną wiadomości. |
| Dzwoń przy alarmie krytycznym | Gdy urządzenie z kategorii Krytyczne uruchomi alarm, osoba jest też wywoływana z linii tego telefonu, gdy minie jej czas oczekiwania w alertach etapowych. Połączenie tylko ją budzi: co się stało, jest w wiadomości na Telegramie wysłanej w tym samym czasie (Android nie pozwala aplikacjom odtwarzać dźwięku w rozmowie). Poproś, aby zapisała ten numer jako „Alarm SwitchGuard”. Połączenia powtarzają się co 2 minuty, do 3 razy, dopóki alarm nie zostanie wyłączony lub ktoś nie dotknie „Rozumiem” w Telegramie. Do grup nie można dzwonić. |
| Wysyłaj inteligentne alerty na Telegram | Awarie zasilania, nietypowe zużycie, alert budżetowy i raport tygodniowy są też zapisywane na Telegramie. |
| Watchdog | Dwa urządzenia z tym samym botem obserwują się nawzajem (np. telefon i przystawka TV). Urządzenie wysyłające aktualizuje przypiętą wiadomość na czacie Telegrama; urządzenie obserwujące uruchamia alarm i wysyła wiadomość do Twoich kontaktów, jeśli sygnał ustanie. Nie odpinaj tej wiadomości. |

### Strona urządzenia (dotknij urządzenia)

| Ustawienie | Do czego służy |
|---|---|
| Monitoruj to urządzenie | Zmiany są zapisywane, a alerty wysyłane zgodnie z regułami poniżej |
| Gdy to nastąpi | Wyłączy się, Włączy się, Przejdzie w tryb offline, Wróci online → Alarm / Powiadomienie / Brak. Alarm dzwoni, dopóki go nie wyłączysz. Powiadomienie pokazuje zwykłe powiadomienie. Brak tylko zapisuje zdarzenie w historii. |
| Dźwięk dla tego urządzenia | Własny dźwięk alarmu; w przeciwnym razie dźwięk ogólny. |
| Kategoria alertu | Gdy automatyczna: Krytyczne, jeśli jakiekolwiek zdarzenie uruchamia alarm, Ważne, jeśli zdarzenia tylko powiadamiają |
| Energia | Moc, napięcie, prąd; dziś i w tym miesiącu; historia dnia / miesiąca / roku z kosztem; raport (PDF / CSV). |
| Cena za kWh | Cena za kWh dla tego urządzenia lub cena domyślna; można ją też ustawić jako domyślną. |
| Timery (w urządzeniu) | Brak timerów. Timery działają w samym urządzeniu, nawet gdy telefon jest wyłączony. (Urządzenia eWeLink: dodawaj i edytuj timery włącz/wyłącz o danej godzinie w wybrane dni.) |
| Połączenie | dBm to siła sygnału: im bliżej 0, tym lepiej. Powyżej -55 doskonały, do -67 dobry, do -75 średni, poniżej słaby. |
| Zużycie w trybie czuwania | Najniższa średnia godzinowa z ostatnich 7 dni: moc, poniżej której to urządzenie (i wszystko, co do niego podłączono) nigdy nie schodzi, nawet gdy „nic nie robi”. Telewizory, routery i ładowarki często pobierają tyle w trybie czuwania. |
| Kopiuj dane urządzenia | Kopiuje surowe dane urządzenia do schowka, co przydaje się przy zgłaszaniu nieobsługiwanego modelu |

## Polecenia Telegrama

Osoby z Twojej listy mogą wysłać /status i przejąć dzwoniący alarm. Wiadomość o alarmie ma przycisk „Rozumiem”: jego naciśnięcie wycisza telefon.

```
Polecenia SwitchGuard:
/status – monitorowanie, alarm i urządzenia
/ack – przejmij dzwoniący alarm
/on nazwa [kanał] – włącz urządzenie lub grupę (kilka: /on kitchen, garden)
/off nazwa [kanał] – wyłącz urządzenie lub grupę
/watt [nazwa] – moc teraz, dzisiaj i zwykły pobór
/temp – temperatura na zewnątrz
/scene nazwa – uruchom scenę
/home · /away – ustaw w domu / poza domem

Możesz też pisać swobodnie: „turn off kitchen and garden”, „heater watts”, „is the pump on” (po angielsku lub turecku).

Włączanie i wyłączanie urządzeń oraz sceny muszą być dozwolone w aplikacji (Alerty → Inteligentne alerty).
```

Działają zarówno angielskie, jak i tureckie nazwy poleceń (np. /status = /durum, /on = /ac). Polecenia są przyjmowane tylko z wybranych czatów.

## Uprawnienia i powody

| Uprawnienie | Po co |
|---|---|
| `POST_NOTIFICATIONS` | Pokazywanie alarmów, powiadomień i stanu monitorowania. |
| `FOREGROUND_SERVICE` | Utrzymanie monitorowania w tle (stałe powiadomienie o stanie). |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prośba o wyłączenie z optymalizacji baterii, aby alarmy się nie opóźniały. |
| `USE_FULL_SCREEN_INTENT` | Otwieranie alarmu na pełnym ekranie nad ekranem blokady. |
| `SYSTEM_ALERT_WINDOW` | Pokazywanie alarmu nad innymi aplikacjami (Android TV, Xiaomi). |
| `WAKE_LOCK` | Utrzymanie procesora w gotowości podczas obsługi zdarzeń („Maksymalna niezawodność”). |
| `VIBRATE` | Wibracje podczas alarmu. |
| `RECEIVE_BOOT_COMPLETED` | Ponowne uruchomienie monitorowania po restarcie telefonu. |
| `INTERNET` | Komunikacja z chmurą eWeLink / Tuya (oraz Telegramem / Open-Meteo / OpenStreetMap, jeśli ich używasz). |
| `ACCESS_COARSE_LOCATION` | Tylko gdy dotkniesz „Użyj lokalizacji tego urządzenia” dla reguł wschodu/zachodu słońca i temperatury. |
| `ACCESS_FINE_LOCATION` | Strefy: dokładny obszar dla reguł przyjazdu / wyjazdu. |
| `ACCESS_BACKGROUND_LOCATION` | Strefy: wykrywanie przyjazdów i wyjazdów przy zamkniętej aplikacji (pytanie tylko przy tworzeniu strefy). |
| `CALL_PHONE` | „Dzwoń przy alarmie krytycznym”: dzwonienie pod wpisany numer z linii tego telefonu. |

## Prywatność

- Brak własnego serwera, analityki, reklam i śledzenia. Nic nie jest sprzedawane ani udostępniane.
- Klucze, tokeny, urządzenia, reguły, ustawienia i historia (maks. 2000 zdarzeń) są przechowywane tylko na Twoim urządzeniu.
- Aplikacja łączy się tylko z podłączonymi chmurami (eWeLink `*.coolkit.cc` / `*.coolkit.cn`, Tuya `openapi.tuya*.com`) oraz — tylko jeśli ich używasz — z Telegramem, Open-Meteo (temperatura i pogoda; lokalizacja zaokrąglona do ok. 100 m) i kafelkami map OpenStreetMap.
- Hasło eWeLink wpisujesz na własnej stronie eWeLink; aplikacja nigdy go nie widzi.
- Dane aplikacji są wyłączone z kopii zapasowej Androida w chmurze; kopie w pliku są chronione hasłem; przesyłanie przez Wi-Fi jest szyfrowane jednorazowym kodem i nie opuszcza sieci domowej.
- Zakupy Pro obsługuje Google Play.

Pełna polityka prywatności: [Google Sites](https://sites.google.com/view/switchguard-privacy) · [docs/privacy-policy.md](docs/privacy-policy.md)

## Rozwiązywanie problemów i FAQ

### Alarm się spóźnia albo nie dzwoni

| Problem | Co zrobić |
|---|---|
| Aktywność w tle jest ograniczona | Telefon może usypiać monitorowanie; alarmy mogą się spóźniać lub nie dotrzeć. |
| Alarm na pełnym ekranie jest niedozwolony | Gdy telefon jest zablokowany, alarm nie może otworzyć dużego ekranu. |
| Xiaomi blokuje ekran alarmu | Gdy telefon jest zablokowany, ekran alarmu nie może się otworzyć. Dotknij Napraw, otwórz Inne uprawnienia i włącz „Wyświetlaj na ekranie blokady” oraz „Otwieraj nowe okna podczas działania w tle”. |
| Powiadomienia alarmowe są wyciszone | Kategoria powiadomień „Alarmy” nie ma wysokiego priorytetu, więc alarm się nie wyświetli ani nie wypełni ekranu. |
| Nie przeszkadzać: całkowita cisza | W tym trybie wyciszone są nawet dźwięki alarmów. |
| Głośność alarmu jest bardzo niska | Głośność alarmu w telefonie wynosi zero lub prawie zero, więc możesz nie usłyszeć SwitchGuard. Naprawa włącza „Dzwoń z pełną głośnością”. |
| Powiadomienia są wyłączone | Nie można wyświetlać alarmów ani ostrzeżeń. |
| Na tym telefonie jest druga kopia SwitchGuard | Inna kopia SwitchGuard powstała dzięki funkcji telefonu Podwójne aplikacje (klonowanie aplikacji). Dwie kopie działające obok siebie dzwonią przy każdym alarmie dwa razy i wzajemnie się wylogowują, gdy używają tego samego konta eWeLink; w telefonach Xiaomi uprawnienia tej kopii również mogą wyglądać na wyłączone. Usuń drugą kopię: przytrzymaj jej ikonę (tę z małą plakietką) i dotknij Odinstaluj. |

### Producenci, którzy zatrzymują aplikacje w tle

| Telefon | Co zrobić |
|---|---|
| Xiaomi | Potrzebne, aby monitorowanie wznawiało się po restarcie lub aktualizacji. Dotknij i włącz Autostart dla SwitchGuard. |
| Samsung | Samsung może usypiać aplikację. W Baterii wybierz "Bez ograniczeń" i dodaj SwitchGuard do "Aplikacji, które nigdy nie zasypiają" (Bateria → Limity użycia w tle). |
| Huawei / Honor | W Uruchamianiu aplikacji ustaw SwitchGuard na "Zarządzaj ręcznie" i włącz Autouruchamianie, Uruchamianie dodatkowe oraz Działanie w tle. |
| OPPO / realme / OnePlus | Włącz Autouruchamianie dla SwitchGuard, a następnie w ustawieniach jego baterii zezwól na aktywność w tle. |
| vivo / iQOO | Zezwól SwitchGuard na uruchamianie w tle oraz na wysokie zużycie energii w tle. |
| Tecno / Infinix | Włącz Autostart dla SwitchGuard w Phone Master, aby monitorowanie przetrwało restarty i czyszczenie. |
| ASUS | Zezwól SwitchGuard w Menedżerze autostartu, aby monitorowanie wznawiało się po restarcie. |

### Inne pytania

**Monitorowanie stanęło i pisze, że zostałem wylogowany.**  
Wskazówka: eWeLink dopuszcza jedną sesję na konto. Daj każdemu telefonowi osobne konto eWeLink i udostępnij urządzenia w aplikacji eWeLink.

**Brakuje urządzenia Tuya, które mi udostępniono.**  
Brakuje urządzenia, które Ci udostępniono? Tuya wyświetla tylko urządzenia z Twoich domów. Poproś właściciela, aby dodał Cię jako członka domu w Smart Life (Ja → Zarządzanie domem → Dodaj członka).

**Urządzenia Tuya przestały się aktualizować.**  
Okres próbny Tuya Cloud wygasł, więc urządzenia Tuya nie są monitorowane. Przedłuż go na platform.tuya.com → Cloud → IoT Core → Extend trial period.

**Zmiany docierają z opóźnieniem / brak natychmiastowych aktualizacji.**  
Natychmiastowe aktualizacje są niedostępne dla tego App ID; używane są sprawdzenia okresowe (zmiany krótsze niż interwał mogą zostać pominięte)

**Czy mogę przejść między wersją z Google Play i z GitHuba?**  
Tak, ale najpierw odinstaluj drugą: mają różne podpisy. Przed odinstalowaniem zrób kopię zapasową do pliku, a potem ją przywróć.

**Jak sprawdzić alarm, nie budząc wszystkich?**  
Wypróbuj alarm teraz: z dźwiękiem lub bezgłośnie, od razu lub po 10 sekundach

**Dlaczego zużycie różni się od aplikacji eWeLink / Smart Life?**  
Zużycie jest obliczane przez SwitchGuard na podstawie odczytów mocy wykonywanych przy włączonym monitorowaniu, więc może nieznacznie różnić się od aplikacji producenta urządzenia (eWeLink, Smart Life).

## Aplikacja na Windows

SwitchGuard dla Windows (10 / 11) monitoruje tak samo na komputerze: siedzi w zasobniku systemowym, włącza alarm na pełnym ekranie komputera i współdzieli te same reguły, automatyzacje i funkcje Telegrama. Strefy, widżety, zegarek i połączenia telefoniczne są tylko w telefonie.

### Funkcje

- Te same karty: Urządzenia, Alerty, Automatyzacje, Historia, Czat, Ustawienia; polecenia głosowe w czacie używają pisania głosowego Windows.
- Ikona w zasobniku: otwórz, uruchom / zatrzymaj monitorowanie, wyłącz alarm, zakończ; zamknięcie okna nie zatrzymuje monitorowania.
- Okno alarmu na pełnym ekranie, wbudowane dźwięki (Syrena, Pisk, Puls, Kurant) lub własny WAV / MP3.
- Ekran czuwania i wygaszacz ekranu Windows: tablica otwiera się, gdy komputer jest bezczynny.
- Laptop pozostawiony na ładowarce działa jako czujnik zaniku prądu.
- Aktualizacje z GitHuba jednym kliknięciem; ustawienia i licencja zostają. Kopia w pliku chronionym hasłem (nie da się jej przywrócić na telefonie).

### Ustawienia tylko w Windows

| Ustawienie | Do czego służy |
|---|---|
| Windows | Aby SwitchGuard czuwał na tym komputerze bez przerw. Jeśli któreś jest wyłączone, alarmy mogą zostać pominięte po ponownym uruchomieniu lub podczas uśpienia. |
| Uruchamiaj z Windows | Otwiera się w zasobniku po zalogowaniu do Windows |
| Nie usypiaj komputera | Podczas monitorowania komputer nie przechodzi w stan uśpienia |
| Używaj jako wygaszacza ekranu Windows | Gdy komputer jest bezczynny, otwiera się ekran gotowości; ruch myszy lub naciśnięcie klawisza go zamyka |
| Czas oczekiwania | Jak długo komputer musi być bezczynny, zanim otworzy się ekran gotowości |
| Ustawienia wygaszacza ekranu Windows | Np. „Po wznowieniu wyświetl ekran logowania” |
| Otwórz ekran gotowości | Pełny ekran; Esc zamyka, kliknięcie = „Otwórz aplikację”. Ekran pozostaje włączony, dopóki jest otwarty. |
| Język i wygląd: Motyw | Jasny, ciemny lub jak w Windows. |
| Język | Wybierz język aplikacji, powiadomień i alarmów. |
| Aktualizacje Tuya na żywo na tym komputerze | Wyłączone: urządzenia Tuya są sprawdzane co 5 minut |
| Brak prądu, gdy ten komputer przechodzi na baterię | Dla laptopa podłączonego w domu: jeśli przez minutę działa na baterii, liczy się to jako brak prądu, nawet bez internetu (wiadomość Telegram wyjdzie po powrocie połączenia). |
| Ten komputer jest na tym samym zasilaniu co urządzenia | Dopóki działa z sieci, nie ma przerwy w dostawie prądu, więc urządzenie, które się rozłączy, jest zgłaszane od razu, a nie po nawet 90 sekundach. |
| Wczytaj z pliku… | Użyj własnego pliku WAV lub MP3 dla alarmu lub powiadomienia (pierwsze 60 sekund, maks. 20 MB). |
| Kopia do pliku… | Wszystkie ustawienia, klucze kont i dźwięki w jednym pliku chronionym hasłem |
| Aktywuj Pro | Bez licencji aplikacja monitoruje urządzenia: 1. Wpisz klucz licencyjny, aby monitorować wszystkie urządzenia. |

### Instalacja w Windows

1. Kliknij dwukrotnie plik instalatora (SwitchGuard-…-Kurulum.exe). Nie prosi o uprawnienia administratora i instaluje się w kilka sekund.
2. Jeśli Windows wyświetli „System Windows ochronił Twój komputer” (SmartScreen), naciśnij Więcej informacji → Uruchom mimo to. Aplikacja nie jest jeszcze podpisana; dlatego ostrzeżenie.
3. SwitchGuard instaluje się w folderze użytkownika (AppData › Local › SwitchGuard) i dodaje skróty na pulpicie i w menu Start.
4. Uruchamia się sam w zasobniku (obok zegara) po zalogowaniu do Windows. Zamknięcie okna go nie zatrzymuje; aby zakończyć, użyj ikony w zasobniku → Zakończ.
5. Dodaj konta w Ustawienia → Konta (kroki dla Tuya i eWeLink są poniżej).
6. Przenosiny na inny komputer: na starym Ustawienia → Kopia zapasowa i przenoszenie → Kopia do pliku; zainstaluj na nowym, Przywróć z pliku, a potem ponownie zaloguj się do eWeLink.
7. Odinstalowanie: Ustawienia Windows → Aplikacje → Zainstalowane aplikacje → SwitchGuard → Odinstaluj.
8. Instalacja nowej wersji: najpierw odinstaluj starą (krok 7), potem uruchom nowy instalator. Ustawienia, konta i dźwięki zostają.

## Budowanie

```
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest      # unit tests
```
Wersje release wymagają `keystore.properties` + keystore w katalogu projektu (nie ma ich w repozytorium).

## Historia zmian

[English](CHANGELOG.md) · [Türkçe](CHANGELOG.tr.md) · [Releases](https://github.com/Macerce/switchguard/releases)

## Kontakt

Pytania, zgłoszenia błędów i opinie (lub otwórz tu issue): **switchguardapp@gmail.com**

## Licencja

SwitchGuard jest **source-available** (kod źródłowy dostępny), a nie open source. Kod źródłowy: [PolyForm Noncommercial 1.0.0](LICENSE). Użytek osobisty i inny niekomercyjny jest wolny; sprzedawanie kodu lub używanie go komercyjnie jest zabronione. Wersje do 2.8.3 włącznie były wydane na licencji GPL-3.0.

Oficjalnych wersji (Google Play, [Releases](https://github.com/Macerce/switchguard/releases)) może używać każdy, także firmy. Zmodyfikowane wersje nie mogą używać nazwy ani logo SwitchGuard. Licencja komercyjna: **switchguardapp@gmail.com**

Brak powiązań z eWeLink, CoolKit ani ITEAD, ich poparcia ani sponsoringu. Sonoff i eWeLink są znakami towarowymi ich właścicieli. Nie jest też powiązany z Tuya.
