# Przypominajka

Aplikacja na Androida z przypomnieniami (Kotlin, Jetpack Compose / Material 3, MVVM, Room, AlarmManager).

## Wygląd 3.1 „Złoty dzwon”

Całość kręci się wokół złotego dzwonu (jak emoji 🔔) – ten sam kształt jest w ikonie
(`res/drawable/ic_launcher_foreground.xml`) i w aplikacji (`ui/components/Bell.kt`).

- **Kolory** (`ui/theme/Theme.kt`) – jedna spójna rodzina: kremowe tło, espresso (tekst, przyciski), złoto dzwonu
  i przygaszone, ziemiste barwy o podobnej jasności (`Pastel`: złoto, terakota, szałwia, zmierzch, śliwka, piasek).
  Kolory folderów też są przygaszone (migracja bazy 6→7 zamienia stare, jaskrawe). Płasko, bez gradientów.
- **Strona główna** (`ui/home/HubScreen.kt`) – duży dzwon z liczbą dzisiejszych powiadomień, wokół niego jak aureola
  sześć działów: Przypomnienia, Plan, Trening (po lewej), Quizy, Konto, Ustawienia (po prawej). Pod spodem „Dziś”.
- **Przejścia** – przy każdej zmianie ekranu dzwon bije (kołysanie, fale, dźwięk), a nowy ekran rozchodzi się kołem
  od dzwonu (`BellReveal`); przy powrocie zwija się z powrotem.
- **Dźwięki** (`res/raw`, `util/BellTone.kt`, `util/BellSound.kt`) – syntezowane dzwony (bez cudzych próbek):
  „Dzwon” (masywny dzwon kościelny z tercją małą, mocnym uderzeniem i długim pogłosem), „Dzwon – melodia”
  (C#6–E6–C#6–G#5), Dzwonek, Kryształ,
  Rowerowy, Gong. Dźwięk przejść gra jako dźwięk aplikacji (strumień multimediów, nie powiadomienie).
- **Ustawienia** (`ui/settings/`) – motyw, dzwon w aplikacji (włącz/wyłącz, dźwięk, głośność, cisza w trybie cichym),
  dźwięk powiadomień (lista z odsłuchem; każdy dźwięk ma własny kanał `reminders_<dźwięk>`), kiedy przypominać,
  koniec lekcji, foldery. Konto zawiera już tylko Librusa i klucz AI.

## Działy

- **Przypomnienia** (`ui/home/HomeScreen.kt` → `RemindersScreen`) – filtr folderów i lista kart „najpierw godzina”
  w sekcjach Aktywne / Wyłączone / Zakończone. Nowe przypomnienie domyślnie trwa **1 dzień**; przyciski
  „1 dzień / Tydzień / 2 tyg. / Miesiąc” zmieniają długość.
- **Plan** – plan lekcji z Librusa (tydzień/dzień, odwołania, zastępstwa, sprawdziany danego dnia) na osi czasu;
  lekcja, która trwa teraz, ma koralową ramkę i plakietkę „teraz”.
- **Trening** – plan treningowy jako checklista (bez przypomnień): foldery (Siłownia, Bieganie, własne) → plany
  (Push / Pull / Legs, Upper / Lower, Full body albo własne) → ćwiczenia z checkboxami i opisem serii. Postęp
  planu i folderu, „Wyczyść zaznaczenia” przed kolejnym treningiem (`ui/workout/`, tabele `workout_*`).
  **Przerwa między seriami**: 1:00 / 1:30 / 2:00 / 3:00, „+30 s”, „Stop”; koniec przerwy to dokładny alarm
  i powiadomienie z wibracją – działa też przy zablokowanym ekranie (`alarm/RestTimer.kt`).
  **Przypomnienie o treningu** (od 3.4): przełącznik i wybór dni PN–ND; powiadomienie „Czas na trening” przychodzi
  dokładnie o końcu lekcji danego dnia (plan z Librusa, a gdy go brak – godziny z Ustawień). Sobota i niedziela
  mają własne godziny („Zmień” w karcie, domyślnie 15:00, od 3.5); inny dzień bez lekcji – domyślna godzina 15:00. Jeden dokładny alarm na najbliższy termin, przeliczany po synchronizacji planu,
  zmianie godzin lekcji i restarcie telefonu (`alarm/TrainingReminder.kt`).
- **Quizy** – zestawy pytań do nauki: fiszki (obracana karta) i test ABCD (błędne odpowiedzi z innych
  pytań zestawu), wynik, „Powtórz trudne”, podpowiedzi „Przygotuj się” dla nadchodzących sprawdzianów.
  Pytania można wkleić listą: jedna linia = „pytanie - odpowiedź”.
- **Konto** – Librus, „kiedy przypominać” (suwaki: sprawdzian 3 dni, kartkówka 2, zadanie 1, wyjście 1),
  koniec lekcji w każdy dzień tygodnia (gdy brak planu z Librusa), **Wygląd** (motyw: jak w telefonie /
  jasny / ciemny, dźwięk dzwonu; `util/AppearancePrefs.kt`), foldery.

### Poza aplikacją
- **Widżet „Dziś”** na ekran główny telefonu (Glance, `widget/TodayWidget.kt`): ile powiadomień zostało, najbliższe
  godziny, najbliższy sprawdzian i przycisk „+” do nowego przypomnienia. Odświeża się przy każdej zmianie i co 30 min.
- **Skróty z ikony** (przytrzymaj ikonę): Nowe przypomnienie, Plan lekcji, Trening (`res/xml/shortcuts.xml`,
  adresy `przypominajka://open/...`, obsługa w `StartRequest`).
- **Powiadomienie** ma trzy przyciski: „Zrobione”, „Za 10 min”, „Za godzinę”.

## Instalacja z pliku APK
Gotowy plik: `Przypominajka.apk` w GitHub Releases (https://github.com/tymoteusz-hash/przypominajka/releases)
albo w katalogu projektu. Skopiuj go na telefon i otwórz – Android zapyta o zgodę na instalację z nieznanego źródła.
Nowa wersja instaluje się jako aktualizacja (dane zostają). Budowanie z wiersza poleceń:
`gradle :app:assembleRelease` → `app/build/outputs/apk/release/app-release.apk`.

### Aktualizacje (od 3.3)
Aplikacja sama sprawdza `https://api.github.com/repos/tymoteusz-hash/przypominajka/releases/latest`
(przy starcie, najwyżej co 6 h; przełącznik i „Sprawdź aktualizacje” w Ustawieniach). Gdy tag wydania
(`vX.Y`) jest nowszy od zainstalowanej wersji, pokazuje okno „Nowa wersja” z opisem wydania, pobiera
`Przypominajka.apk`, sprawdza, czy to ta sama aplikacja w wyższej wersji, i otwiera systemowy instalator
(`update/UpdateManager.kt`, `ui/update/UpdateDialog.kt`, FileProvider `res/xml/update_paths.xml`).

**Wydanie nowej wersji:** podbij `versionCode` i `versionName` w `app/build.gradle.kts`, zbuduj release,
utwórz na GitHubie Release z tagiem `vX.Y` i dołącz plik `Przypominajka.apk`.

**Podpis:** release podpisuje `przypominajka.jks` (dane w `keystore.properties`). Oba pliki są tylko na tym
komputerze i w kopii `E:\Przypominajka-klucz` – **nigdy w repozytorium**. Bez nich nie da się wydać aktualizacji,
która zainstaluje się na starą wersję. Bez `keystore.properties` build podpisuje kluczem debug.

### Quizy z AI
W Koncie wpisz własny klucz API z console.anthropic.com (zaszyfrowany, tylko na telefonie). W quizie sekcja
„Pytania z AI” generuje 5–20 pytań z nazwy i tematu zestawu (`ai/QuizGenerator.kt`, model `claude-opus-5-5`,
odpowiedź wymuszona schematem JSON). Koszt płaci właściciel klucza.

### Wydajność
Wersja **debug** (domyślna w Android Studio) nie jest optymalizowana i Compose działa w niej wolniej.
Do codziennego używania instaluj **release**: Build → Select Build Variant → `release`, potem Run.
Release ma włączone R8 (`isMinifyEnabled`, `isShrinkResources`) i jest podpisany kluczem `przypominajka.jks`.

### Przypomnienia „po lekcjach”
Wpis w folderze Szkoła z rodzajem ma **termin** (dzień sprawdzianu) i liczbę dni przed. Powiadomienie przychodzi
każdego dnia **godzinę po ostatniej lekcji tego dnia** – z planu Librusa (`timetable_lessons`), a gdy go brak,
z ustawień dnia tygodnia; w dni bez lekcji godzinę po domyślnym końcu lekcji (`SchoolTimes`, `ReminderSchedule`).
Po synchronizacji planu lub zmianie godzin alarmy przeliczają się same (`rescheduleAfterSchool`).

## Struktura projektu

```
przypominajka/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml              # wersje bibliotek (version catalog)
│   └── wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/
        │   ├── drawable/ic_notification.xml, ic_launcher_foreground.xml
        │   ├── mipmap-anydpi-v26/ic_launcher.xml, ic_launcher_round.xml
        │   ├── values/strings.xml, colors.xml, themes.xml
        │   └── values-night/themes.xml
        └── java/pl/przypominajka/
            ├── PrzypominajkaApp.kt          # Application: kontener zależności + kanał powiadomień
            ├── AppContainer.kt              # ręczne DI (baza, scheduler, repozytorium)
            ├── MainActivity.kt
            ├── data/
            │   ├── Folder.kt                # encja folderu + paleta kolorów
            │   ├── Reminder.kt              # encja przypomnienia
            │   ├── Converters.kt            # LocalDate / List<LocalTime> <-> SQLite
            │   ├── FolderDao.kt
            │   ├── ReminderDao.kt
            │   ├── AppDatabase.kt           # Room + domyślne foldery (Szkoła, Dom, Praca, Inne)
            │   └── ReminderRepository.kt    # cała logika zapisu + (re)planowania alarmów
            ├── alarm/
            │   ├── ReminderSchedule.kt      # obliczanie najbliższego terminu
            │   ├── ReminderScheduler.kt     # AlarmManager.setExactAndAllowWhileIdle
            │   ├── NotificationHelper.kt    # kanał + powiadomienie z przyciskami
            │   ├── AlarmReceiver.kt         # alarm -> powiadomienie -> kolejny alarm
            │   ├── NotificationActionReceiver.kt  # "Zrobione" / "Przypomnij za 10 min"
            │   ├── BootReceiver.kt          # BOOT_COMPLETED i in. -> ponowne planowanie
            │   └── ReceiverExtensions.kt    # goAsync + korutyny
            ├── util/Format.kt               # formatowanie dat, polska odmiana liczebników
            └── ui/
                ├── AppViewModelProvider.kt
                ├── navigation/AppNavHost.kt
                ├── theme/Theme.kt           # własna paleta jasna/ciemna, typografia, "Większy tekst"
                ├── components/
                │   ├── CommonComponents.kt  # FolderDot, swipe-to-delete, TimePicker
                │   └── PermissionsSection.kt
                ├── home/HomeScreen.kt, HomeViewModel.kt
                ├── edit/EditReminderScreen.kt, EditReminderViewModel.kt
                └── folders/FoldersScreen.kt, FoldersViewModel.kt
```

## Jak działa planowanie

- Dla każdego przypomnienia w `AlarmManager` istnieje **najwyżej jeden alarm** – najbliższy termin
  (albo odłożenie „za 10 min”, jeśli wypada wcześniej).
- `AlarmReceiver` pokazuje powiadomienie i od razu planuje kolejny termin. Gdy kolejnego nie ma
  (minął ostatni dzień), przypomnienie dostaje flagę `finished` i przestaje działać.
- Każda zmiana (edycja, wyłączenie, usunięcie, usunięcie folderu) przechodzi przez
  `ReminderRepository`, który anuluje stary alarm i planuje nowy.
- `BootReceiver` po restarcie telefonu, aktualizacji aplikacji, zmianie czasu/strefy oraz
  po przyznaniu zgody na dokładne alarmy planuje wszystkie aktywne przypomnienia od nowa.
  Dodatkowo robi to `MainActivity` przy uruchomieniu (np. po „Wymuś zatrzymanie”).

## Librus Synergia (nieoficjalnie)

Menu ⋮ → **Librus Synergia** (albo karta „Masz Librusa?” na ekranie głównym).

- Logowanie tym samym loginem i hasłem co na synergia.librus.pl. Aplikacja przechodzi tę samą ścieżkę co
  przeglądarka: `synergia.librus.pl/loguj/portalRodzina` → formularz `api.librus.pl/OAuth/Authorization?client_id=46`
  → `synergia.librus.pl/refreshToken` (ciasteczko `oauth_token`) → JSON z `synergia.librus.pl/gateway/api/2.0`.
- Pobierane dane: `HomeWorks` (terminarz: sprawdziany, kartkówki, wycieczki), `HomeWorks/Categories`, `Subjects`,
  `Timetables?weekStart=` (koniec ostatniej lekcji danego dnia).
- Dla każdego nadchodzącego sprawdzianu/kartkówki (i opcjonalnie wyjścia) powstaje przypomnienie w folderze
  Szkoła: od 1–3 dni przed terminem, **godzinę po ostatniej lekcji** dnia poprzedzającego (gdy planu brak –
  według ustawionego końca lekcji). Zadania domowe wpisuje się ręcznie.
- Synchronizacja: przy połączeniu, przycisk „Synchronizuj teraz” i w tle co ok. 6 h (WorkManager).
  Wpisy usunięte ręcznie nie wracają; wpisy usunięte w Librusie znikają.
- Login i hasło: AES-256/GCM z kluczem w AndroidKeyStore, tylko na telefonie (kopia zapasowa wyłączona).
  Klient łączy się wyłącznie z `https://*.librus.pl`.
- „Szczegóły ostatniej synchronizacji” pokazują kody HTTP kolejnych kroków (bez hasła i ciasteczek) – przydatne,
  gdy Librus coś zmieni.

Pliki: `librus/LibrusClient.kt` (HTTP + logowanie), `LibrusEvent.kt` (parsowanie), `LibrusSyncer.kt` (logika),
`LibrusSettings.kt` (ustawienia + szyfrowanie), `LibrusSyncWorker.kt` (praca w tle), `ui/librus/` (ekran).

## Uruchomienie w Android Studio

1. Zainstaluj aktualne Android Studio (z SDK Platform 36 – Studio zaproponuje pobranie, jeśli go brakuje).
2. **File → Open…** i wskaż katalog `przypominajka` (ten z `settings.gradle.kts`).
3. Poczekaj na **Gradle Sync** – Studio pobierze Gradle 8.11.1 i wszystkie biblioteki.
   - W repozytorium nie ma binarnego `gradle-wrapper.jar` ani skryptów `gradlew`. Android Studio
     korzysta z `gradle-wrapper.properties` i ich nie potrzebuje. Jeśli chcesz budować z wiersza
     poleceń, wygeneruj je raz poleceniem `gradle wrapper` (wymaga zainstalowanego Gradle).
   - Jeśli sync zgłosi niezgodną wersję Javy: **Settings → Build, Execution, Deployment →
     Build Tools → Gradle → Gradle JDK** i wybierz JDK 17 lub 21 (np. wbudowane „jbr-21”).
   - Studio może zaproponować aktualizację AGP/Kotlina (Upgrade Assistant) – nie jest to wymagane.
4. Uruchom emulator (najlepiej API 33+, żeby sprawdzić prośbę o powiadomienia) lub podłącz telefon
   z włączonym debugowaniem USB.
5. Kliknij **Run ▶** (konfiguracja `app`).

### Szybki test

- Dodaj przypomnienie z godziną za 1–2 minuty i zablokuj ekran – powinno przyjść powiadomienie
  z tytułem i nazwą folderu oraz przyciskami „Zrobione” i „Przypomnij za 10 min”.
- Test restartu na emulatorze: `adb reboot`, a po starcie sprawdź zaplanowane alarmy:
  `adb shell dumpsys alarm | findstr przypominajka`.
