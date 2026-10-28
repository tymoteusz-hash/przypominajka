# Przypominajka

Aplikacja na Androida z przypomnieniami (Kotlin, Jetpack Compose / Material 3, MVVM, Room, AlarmManager).

## Wygląd 4.0

Trzy wybieralne wyglądy mają własne układy ekranów i ikony:

- **Kartka** (`ui/kartka`) – spokojna lista, cienkie linie i jeden kolor akcentu.
- **Dzwon** (`ui/dzwon`) – złoty dzwon na środku i działy na okręgu wokół niego.
- **Tablica** (`ui/tablica`) – kolorowe karteczki przypięte do tablicy.

Przy pierwszym uruchomieniu dwukrokowy przewodnik (`ui/onboarding`) pozwala najpierw wybrać wygląd,
a potem motyw: **Jasny / Ciemny / Jak w telefonie**. Oba wybory można zmienić w **Ustawienia > Wygląd**.

- **Wspólny motyw** (`ui/look`) – tokeny kolorów oraz `LookTheme` zapewniają spójne kolory, typografię
  i kształty także we wspólnych oknach Material 3. `LookFonts` korzysta z dołączonych fontów OFL w `res/font`:
  Inter Tight (Kartka), Fraunces i Manrope (Dzwon), Caveat i Nunito (Tablica). Licencje są w `assets/licenses`.
- **Ikona aplikacji** zmienia się razem z wyglądem przez `activity-alias` w manifeście
  (`util/AppIconSwitcher.kt`). Na Samsungach zmiana może usunąć ikonę z ekranu głównego – pozostaje ona
  w szufladzie aplikacji i trzeba przeciągnąć ją ponownie na pulpit.
- **Dźwięki powiadomień** pozostają dostępne w Ustawieniach, z odsłuchem; każdy dźwięk ma własny kanał
  `reminders_<dźwięk>` (`res/raw`, `util/BellTone.kt`, `util/TonePreview.kt`).

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
- **Konto** – Librus i klucz AI. Wybór wyglądu i motywu jest w **Ustawienia > Wygląd**
  (`util/AppearancePrefs.kt`); Ustawienia zawierają też „kiedy przypominać”, koniec lekcji i foldery.

## Checklista

Jedna lista punktów do odhaczenia, wspólna dla trzech wyglądów. Kolejność można zmieniać przeciąganiem.
**Usuń zrobione** usuwa odhaczone punkty, a **Cofnij** przywraca je po usunięciu. Każdy punkt może
opcjonalnie mieć datę i godzinę przypomnienia – wtedy otrzymuje powiadomienie z akcją **Zrobione**.
Odhaczenie anuluje alarm i powiadomienie.

Dane i zapis: `data/Checklist.kt` (`ChecklistItem`, `ChecklistDao`), `data/ChecklistRepository.kt`.
Alarmy: `alarm/ChecklistAlarms.kt` i `alarm/ChecklistAlarmReceiver.kt`. Baza Room ma wersję **8**;
`MIGRATION_7_8` w `data/AppDatabase.kt` dodaje tabelę `checklist_items`.

### Poza aplikacją

- **Widżet „Dziś”** na ekran główny telefonu (Glance, `widget/TodayWidget.kt`): ile powiadomień zostało, najbliższe
  godziny, najbliższy sprawdzian i przycisk „+” do nowego przypomnienia. Odświeża się przy każdej zmianie i co 30 min.
- **Nowy widżet „Checklista”** (`widget/ChecklistWidget.kt`) pozwala odhaczać punkty bezpośrednio z ekranu głównego.
  Jego „+” otwiera `widget/QuickAddActivity.kt` – małe okno do szybkiego dopisania punktu.
  Oba widżety dopasowują styl i motyw do wybranego wyglądu (Kartka / Dzwon / Tablica; `widget/WidgetLook.kt`).
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

**Wydanie nowej wersji:** podbij `versionCode` i `versionName` w `app/build.gradle.kts` i wypchnij na `main`.
GitHub Actions (`.github/workflows/build.yml`) zbuduje APK i utworzy Release `vX.Y` z `Przypominajka.apk`
(tylko gdy są sekrety z kluczem podpisu – patrz komentarz w pliku workflow). Opis wydania = opis commita.

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
        ├── assets/licenses/             # licencje OFL dołączonych fontów
        ├── res/
        │   ├── drawable/, mipmap-anydpi-v26/ # ikony trzech wyglądów i grafiki widżetów
        │   ├── font/                    # Inter Tight, Fraunces, Manrope, Caveat, Nunito
        │   └── values/, values-night/, xml/, raw/, layout/
        └── java/pl/przypominajka/
            ├── PrzypominajkaApp.kt, AppContainer.kt, MainActivity.kt
            ├── data/                    # Room v8, przypomnienia, Checklist.kt, ChecklistRepository.kt
            ├── alarm/                   # przypomnienia, ChecklistAlarms.kt, ChecklistAlarmReceiver.kt
            ├── widget/                  # TodayWidget, ChecklistWidget, WidgetLook, QuickAddActivity
            ├── util/                    # AppearancePrefs, AppIconSwitcher, formatowanie i dźwięki
            └── ui/
                ├── AppRoot.kt, AppViewModelProvider.kt
                ├── navigation/Routes.kt
                ├── look/                # tokeny, LookTheme i LookFonts
                ├── onboarding/          # wybór wyglądu, potem motywu
                ├── kartka/, dzwon/, tablica/ # ekrany i komponenty trzech wyglądów
                └── checklist/, home/, edit/, folders/, settings/, account/, plan/, workout/, quiz/, librus/, update/
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
