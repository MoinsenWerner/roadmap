# DriveMap

DriveMap ist eine Android-App, die während der Fahrt kontinuierlich GPS-Daten sammelt, daraus eine vollständig offline nutzbare Straßenkarte generiert und automatisch Geschwindigkeitsbegrenzungen für 500-Meter-Abschnitte aus den gemessenen Durchschnittsgeschwindigkeiten ableitet. Straßennamen und Ortsinformationen werden, sofern eine Datenverbindung besteht, per Geocoder nachgeladen. Für jeden Abschnitt lässt sich die Geschwindigkeitsbegrenzung im Stillstand direkt in der Karte manuell korrigieren.

## Merkmale

- Offline-Kartendarstellung auf Basis der aufgezeichneten GPS-Tracks inklusive Live-Ausrichtung.
- Automatische Segmentierung der Strecke in 500-Meter-Abschnitte und Ermittlung der gemittelten Geschwindigkeit als Speed-Limit.
- Lokale Speicherung der Abschnitte via Room-Datenbank.
- Vordergrunddienst zur kontinuierlichen Standortverfolgung im Hintergrund.
- Manuelle Bearbeitung von Geschwindigkeitsbegrenzungen per Tipp auf einen Straßenabschnitt (nur im Stillstand).
- Optionale Online-Abfrage von Straßennamen und Ortsangaben über den Android-Geocoder (nur bei verfügbarer Verbindung).
- Keine externen Kartendaten – alle Straßenverläufe stammen ausschließlich aus lokal aufgezeichneten GPS-Daten.

## Voraussetzungen

- Android Studio oder das Android SDK 34
- Java 17
- Bash, curl, unzip, sha256sum (für das Setup-Skript)

## Projekt einrichten und bauen

Das Repository enthält keine Binärdateien wie den Gradle-Wrapper-JAR. Das Skript `setup.sh` lädt alle benötigten Komponenten herunter, richtet den Gradle-Wrapper ein, installiert die notwendigen Android-SDK-Komponenten und startet anschließend einen Debug-Build.

```bash
./setup.sh
```

Nach erfolgreichem Durchlauf liegt das Build-Artefakt unter `app/build/outputs/apk/debug/app-debug.apk`.

## Entwicklungshinweise

- Das Modul `app` enthält sämtliche Android-spezifischen Quellen.
- Die Standortaufzeichnung erfolgt über `LocationRecorderService`, der als Vordergrunddienst läuft.
- Die Datenpersistenz geschieht in `RoadRepository`, das eingehende Location-Samples in 500-Meter-Segmente umwandelt und in Room speichert.
- Die Compose-Oberfläche wird in `MainScreen` gezeichnet und nutzt eine eigene Projektion zur Darstellung der GPS-Punkte.

## Tests

Nach der Installation des SDKs können Unit-Tests mit Gradle ausgeführt werden:

```bash
./gradlew test
```

Instrumentierte Tests können mit `./gradlew connectedAndroidTest` ausgeführt werden, sobald ein Emulator oder Gerät verfügbar ist.
