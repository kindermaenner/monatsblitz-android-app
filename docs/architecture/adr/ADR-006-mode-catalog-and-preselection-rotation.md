# ADR 006: Modus-Katalog, Snapshot-Prinzip und automatisierte Vorabauswahl (Rotation)

## Status
Akzeptiert

## Kontext
Im Vereinsbetrieb wechseln Schachturniere typischerweise zwischen festen Bedenkzeitmodi (z. B. alternierend `3+2` und `5+0`). Zudem werden gelegentlich Sonderturniere mit abweichender Bedenkzeit oder speziellem Anlass ausgetragen (z. B. *"Schnellschach 10+0"*, *"Jubiläumsblitz 65 Jahre"* oder *"Weihnachtsturnier"*).

Bisher war `GameMode` als statisches Kotlin-Enum (`BLITZ_3_2`, `BLITZ_5_0`, `HANDICAP`) fest einprogrammiert. Das Anlegen neuer Modi war nicht dynamisch möglich. Gleichzeitig sollte der Setup-Prozess für den Turnierleiter beschleunigt werden (automatische Vorabauswahl des nächsten Modus), ohne dass bei Gerätewechseln die Rotation aus dem Takt gerät oder Sonderturniere die Historie verfälschen.

## Entscheidung
Wir etablieren ein Katalog- und Snapshot-Modell in Kombination mit einer serverseitig gestützten Rotationsermittlung:

1. **Katalog vs. Snapshot:**
   * **Modus-Katalog (`modes` in SQLite / `wp_monatsblitz_modes` in MySQL):** Dient ausschließlich als verwaltbare Auswahlliste für dauerhaft verfügbare Modi.
   * **Snapshot im Turnier (`tournaments.mode`):** Das Turnier speichert den tatsächlichen Modusnamen als Zeichenkette (`VARCHAR(30)`). Dies garantiert die Unveränderlichkeit bestehender Berichte und Turnierhistorien und vermeidet Fremdschlüsselzwänge.

2. **Dynamische Neuanlage direkt aus der App:**
   * Über `+ Neuer Modus...` im Dropdown des `TournamentSetupScreen` können neue Modi direkt am Smartphone angelegt werden.
   * **Dauerhafte Speicherung:** Wird nach einer Bestätigungsabfrage lokal in Room gesichert und über `POST /monatsblitz/v1/mode` an das WordPress-Backend übertragen.
   * **One-Shot-Option:** Erlaubt einmalige Turniermodi, ohne den Katalog dauerhaft zu verschmutzen.

3. **Multi-Device-feste Rotation (Server als SSOT):**
   * Die Vorabauswahl ist über `SettingsScreen` konfigurierbar (`Keine`, `Fester Modus`, `Rotation`).
   * Bei der Rotation ermittelt die App den Zyklus nicht aus lokalem App-Zustand, sondern fragt die jüngsten Turniere vom WordPress-Server ab (`GET /tournaments`, mit Fallback auf lokale SQLite-Daten).
   * **One-Shot-Resistenz:** Der Algorithmus sucht das jüngste Turnier, dessen Modus in der konfigurierten Rotationsliste enthalten ist. Einmalige Sonderturniere werden übersprungen, sodass der Rotationsrhythmus exakt erhalten bleibt.

## Konsequenzen
* **Vorteile:**
  * Maximale Flexibilität vor Ort: Neue Modi können sofort ohne WP-Admin-Zugriff angelegt werden.
  * Nahtlose Multi-Device-Fähigkeit: Bei Gerätewechsel ermittelt jedes Gerät denselben nächsten Rotationsmodus.
  * Historische Datenintegrität durch das Snapshot-Prinzip.
  * Hohe Offline-Resilienz (Room-Synchronisation mit Dirty-Flag).
* **Nachteile:**
  * Zusätzliche Room-Tabelle (`modes`) mit Migration von Version 5 auf 6.
  * Asynchrones Nachladen der Vorabauswahl beim Öffnen des Setup-Screens.
