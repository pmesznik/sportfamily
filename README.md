# SportFamily

Rodzinny kalendarz rodzica młodego sportowca na Androida. Jedno miejsce na treningi, szkołę, zajęcia dodatkowe, dojazdy i rzeczy do spakowania.

- Ekran **Dzisiaj**: najbliższe wydarzenie, godzina wyjazdu, kto wiezie, co zabrać, oś dnia.
- **Kalendarz** tygodniowy i miesięczny z filtrami dzieci i kategorii, wykrywanie konfliktów.
- Wydarzenia powtarzalne i kilkudniowe, listy rzeczy z szablonami, zadania.
- Dane zapisane tylko na telefonie.

## Instalacja

Pobierz `SportFamily.apk` z [najnowszego wydania](../../releases/latest) i zainstaluj. Kolejne wersje aplikacja pobiera sama.

## Budowanie

Kotlin, Jetpack Compose (Material 3), Room. JDK 17+, Android SDK 36.

```
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Wydanie: zwiększ `versionCode` i `versionName` w `app/build.gradle.kts`, potem `scripts/wydaj.ps1 -Notes "Co nowego"`. Wymaga lokalnego `keystore.properties` z kluczem podpisu (nie jest w repozytorium).
