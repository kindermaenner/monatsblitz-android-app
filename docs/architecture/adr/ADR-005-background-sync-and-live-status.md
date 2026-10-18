# ADR 005: Hintergrund-Synchronisation und Live-Status für Turnierpartien

## Status
Akzeptiert

## Kontext
Bei Schachturnieren werden Ergebnisse zügig nacheinander durch den Turnierleiter in die Kreuztabelle eingetragen. Eine synchrone Netzwerkanfrage bei jedem Klick würde bei schlechter Netzabdeckung zu Latenzen und UI-Blockaden führen. Zudem können Fehlklicks vorkommen, die vor der Finalisierung des Turniers korrigiert werden müssen. Bei Netzwerkfehlern dürfen keine Daten verloren gehen, und der Benutzer benötigt jederzeit visuelles Feedback über den tatsächlichen Synchronisationszustand mit der Möglichkeit, bei Bedarf manuell einzugreifen.

## Entscheidung
Wir etablieren ein asynchrones, nicht-blockierendes Hintergrund-Synchronisationskonzept mit folgenden Säulen:

1. **Optimistic Local-First Persistence:**
   Ergebniseingaben werden sofort in der lokalen Room-Datenbank gespeichert (`dirty = 1`). Die Benutzeroberfläche reagiert verzögerungsfrei.

2. **Automatischer Hintergrund-Sync:**
   Unmittelbar nach der lokalen Persistenz startet das `CrosstableViewModel` asynchron im `viewModelScope` den `TournamentSyncManager`.
   * Parallele Sync-Aufrufe werden über eine `Mutex` serialisiert.
   * Der Sync löst Abhängigkeiten in fester Reihenfolge auf: Neue Spieler $\rightarrow$ Turnier $\rightarrow$ ausstehende Partien (`dirty = 1`).
   * Alle `dirty`-Partien werden gebatcht via `POST /game` übertragen.

3. **Serverseitiges Upsert:**
   Das WordPress-Plugin (`CreateGameHandler.php`) prüft vor dem Anlegen einer Partie, ob die Paarung bereits existiert, und wechselt bei Bedarf auf ein Update. Dies verhindert MySQL Duplicate Key Errors bei Ergebnisänderungen im Status `open`.

4. **Transparenter Live-Status (`SyncStatus`):**
   * Die Kopfzeile (`TopAppBar`) zeigt ein dynamisches Status-Icon:
     * `SYNCED`: Wolke mit Haken. Antippen zeigt "Bereits synchronisiert" (kein unnötiger Netzwerk-Traffic).
     * `SYNCING`: Unaufdringlicher Ladekreis.
     * `FAILED`: Warnsymbol. Antippen löst gezielt einen manuellen Neuversuch aus.
     * `FINALIZED`: Schloss-Symbol; schaltet die Kreuztabelle in den Schreibschutz.
   * Auf Zellebene markiert ein dezenter Punkt ungesyncte Partien (`dirty = 1`). Über `AppSettingsStorage` (DataStore) und einen Einstellungsbildschirm kann dieses Detail-Feature vom Benutzer gesteuert werden.

## Konsequenzen
* **Vorteile:**
  * Keine UI-Verzögerung bei der Ergebniseingabe.
  * Hohe Offline-Resilienz: Partien sammeln sich bei Verbindungsabbruch lokal an und werden beim nächsten Sync gebündelt nachgesendet.
  * Korrekturfreundlich für den Turnierleiter vor Ort.
  * Klare visuelle Rückmeldung ohne überladene UI.
* **Nachteile:**
  * Zusätzliche Zustandsverwaltung im `TournamentSyncManager` und Reaktivitätskoordination im ViewModel.
