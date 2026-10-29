# 1. Funktionale Anforderungen - Monatsblitz Android App

Dieses Dokument beschreibt die funktionalen Anforderungen an die Monatsblitz Android App zur Durchführung von Schachturnieren.

## 1.1 Übersicht
Die App dient als mobiles Werkzeug für Turnierleiter, um monatliche Blitzschachturniere vor Ort durchzuführen, Ergebnisse zu erfassen und diese final an eine WordPress-Instanz zu übertragen. Ein Fokus liegt auf der vollen Offline-Fähigkeit während der Turnierdauer sowie der flexiblen Synchronisation zwischen mehreren Geräten über das Backend.

## 1.2 Turnier-Lebenszyklus

### Phase 1: Turniervorbereitung (Setup)
*   **Spielerauswahl:** Der Turnierleiter wählt Teilnehmer aus einer Liste bestehender Spieler aus.
*   **Spieler-Synchronisation:** Die Liste der bestehenden Spieler wird automatisch mit dem WordPress-Backend synchronisiert.
*   **Spielerneuanlage:**
    *   Turnierleiter können neue Spieler manuell in der App anlegen. Diese werden lokal gespeichert und beim nächsten Sync an WordPress übertragen.
    *   **Namenseindeutigkeit:** Es darf keine zwei Spieler mit gleichem Namen geben. Vorname und Nachname bilden zusammen das eindeutige Kriterium. Die App validiert Eingaben (getrimmt, case-insensitive) gegen alle lokal und remote vorhandenen Spieler. Die Neuanlage eines bereits existierenden Namens wird mit einer Fehlermeldung unterbunden.
*   **Modus-Konfiguration & Dynamische Modi:**
    *   **Auswahlliste:** Dropdown mit allen bekannten Modi (z. B. "3+2", "5+0", "10+0").
    *   **Neuen Modus hinzufügen:** Über "+ Neuer Modus" im Dropdown können direkt neue Modi angelegt werden:
        *   **Dauerhaft speichern (Standard):** Nach Bestätigungssicherheitsabfrage wird der Modus in der lokalen DB gesichert und via `POST /mode` mit dem WordPress-Server synchronisiert.
        *   **Einmalig (One-Shot):** Ohne Serverübertragung oder dauerhafte Speicherung direkt für das aktuelle Turnier verwendbar (z. B. "Jubiläumsblitz", "Weihnachtsturnier").
    *   **Automatische Vorabauswahl (in den Einstellungen konfigurierbar):**
        *   *Keine:* Dropdown startet unselektiert.
        *   *Fester Modus:* Immer derselbe Modus ist standardmäßig vorausgewählt.
        *   *Rotation:* Wechselt automatisch zyklisch durch eine Liste konfigurierter Modi (z. B. alternierend 3+2 und 5+0). Die Ermittlung des letzten Modus erfolgt anhand der jüngsten Turniere auf dem WordPress-Server (Single Source of Truth), wodurch auch Gerätewechsel fehlerfrei unterstützt werden. One-Shot-Turniere werden bei der Rotationsermittlung automatisch übersprungen.
    *   **Wahl des Formats:** Einrundig oder Doppelrundig (Round Robin).
*   **Turnierstart:** Mit dem Start werden alle notwendigen Paarungen (jeder gegen jeden) initial ohne Ergebnis erzeugt.

### Phase 2: Durchführung (Aktiv)
*   **Ergebniserfassung (Kreuztabelle):**
    *   Anzeige aller Paarungen in einer Matrix-Ansicht (Crosstable).
    *   Eingabe von Ergebnissen (1:0, 0:1, ½:½, +:-, -:+).
    *   Ergebnisse werden sofort in der lokalen Datenbank persistiert (Single Source of Truth).
*   **Vorzeitiger Rücktritt (Player Withdrawal):**
    *   Turnierleiter können vorzeitig ausscheidende Spieler (z. B. Jugendliche) durch Klick auf den Spielernamen markieren.
    *   Automatische Wertung offener Partien nach Vereinsregeln:
        *   **Kulanzregel (Jugend):** Ungespieltes Remis (`=:=`, 0.5 Punkte für beide Spieler).
        *   **Reguläre Wertung:** Kampflose Niederlage (`-:+`, 0 Punkte für den Aussteigenden, 1 Punkt für Gegner).
    *   Kennzeichnung ausgetretener Spieler mit `(z)` (*zurückgetreten*) in Kreuztabelle, Rangliste und Veröffentlichungen.
*   **Live-Ranking:**
    *   Aufruf einer aktuellen Rangliste während des laufenden Turniers.
    *   Berechnung nach Punkten (Sieg = 1.0, Remis = 0.5, Niederlage = 0.0).
    *   Verwendung von **Standard Competition Ranking** (mit Platz-Lücken bei Punktgleichheit, z. B. 1, 1, 3, 4).
    *   Keine Verwendung von Feinwertungen (wie Sonneborn-Berger).
*   **Synchronisation während der Durchführung:**
    *   Die Synchronisation mit dem Backend kann jederzeit durch den Benutzer über eine geeignete Bedienung (z. B. Sync-Icon in der TopBar / Aktionsleiste oder Pull-to-Refresh) manuell ausgelöst werden.
    *   Visuelle Rückmeldung über den aktuellen Synchronisationsstatus (z. B. "Synchronisiert", "Synchronisiere...", "Konflikt erkannt", "Offline").

### Phase 3: Abschluss (Finalisierung)
*   **Manueller Abschluss:** Der Turnierleiter löst den Abschluss des Turniers explizit per Button-Klick aus.
*   **Datenübertragung:** Die Gesamtergebnisse werden per REST-API an das WordPress-Plugin gesendet, um dort Berichte zu generieren und das Turnier zu archivieren.
*   **Schreibschutz:** Nach der erfolgreichen Übertragung ist das Turnier in der App und auf dem Server abgeschlossen. Es sind keine weiteren Änderungen an Ergebnissen mehr möglich (Read-only).

## 1.3 Startverhalten, Sitzungsmanagement & Turnierübernahme
*   **Status-Prüfung:** Beim Starten der App wird der Zustand des letzten Turniers aus dem lokalen Speicher (DataStore) ermittelt und mit dem Status auf dem WordPress-Server abgeglichen.
*   **Automatisches Fortsetzen lokaler Turniere:**
    *   Existiert ein lokales, nicht finalisiertes Turnier, das auch auf dem Server noch als "offen" markiert ist, startet die App direkt in **Phase 2 (Durchführung)**.
    *   Ist kein Turnier vorhanden oder wurde das Turnier bereits (lokal oder remote) finalisiert, startet die App in **Phase 1 (Setup)**.
*   **Übernahme offener Server-Turniere ("Turnier fortsetzen"):**
    *   Liegt auf dem WordPress-Server ein offenes Turnier vor, das lokal noch nicht geführt wird:
        *   **2-Phasen-Start (Setup-Screen):** Der Setup-Screen prüft initial kurz den Serverzustand (*„Prüfe auf laufendes Turnier…“*). Erst nach negativem Befund wird die Eingabemaske freigeschaltet.
        *   **Übernahme-Dialog:** Liegt ein offenes Turnier vor, wird ein Info-Dialog mit Datum, Modus und tatsächlicher Teilnehmeranzahl angezeigt. Der Nutzer kann das Turnier mit allen bisherigen Ergebnissen auf das Gerät übernehmen oder stattdessen ein neues Turnier starten.
        *   **Automatisches Übernehmen (Einstellung):** Über `autoAdoptOpenTournament` kann in den Einstellungen festgelegt werden, dass offene Turniere beim Start ohne Rückfrage direkt in die Kreuztabelle übernommen werden.
        *   **Lade-Sperre:** Während des Übernahme- und Vorbereitungsvorgangs sperrt ein modales Vollbild-Overlay alle Touches und Zurück-Eingaben.
*   **Initialer Datenabgleich:** Bei aktiver Internetverbindung wird beim App-Start geprüft, ob auf dem Server neuere Daten vorliegen, und gemäß dem Read-before-Write-Prinzip synchronisiert.

## 1.4 Synchronisation und Live-Status (Version 1)
*   **Hintergrund-Synchronisation:** Nach jeder Ergebniseingabe werden geänderte Partien automatisch im Hintergrund übertragen. Die Benutzeroberfläche bleibt jederzeit bedienbar.
*   **Status-Visualisierung:**
    *   Ein Symbol in der Kopfzeile (`TopAppBar`) visualisiert jederzeit den aktuellen Zustand: `SYNCED` (Wolke mit Haken), `SYNCING` (Ladekreis), `FAILED` (Warnung) und `FINALIZED` (Schloss).
    *   Tippen auf das Symbol löst bei Bedarf (z. B. nach Verbindungsabbruch oder bei Fehlern) einen manuellen Synchronisationsversuch aus.
    *   **Detaillierte Zellen-Indikatoren:** Jede Zelle mit erfasstem Ergebnis zeigt einen farbigen Statuspunkt: **Grün** signalisiert eine erfolgreich synchronisierte Partie (`!dirty`), **Orange / Terziär** eine lokale, noch ausstehende Änderung (`dirty`). Diese Anzeige ist in den Einstellungen konfigurierbar (`showDetailedSyncIndicators`).
*   **Korrekturfreundlichkeit:** Vor der Finalisierung können Ergebnisse jederzeit korrigiert oder zurückgesetzt werden. Das Backend führt nahtlose Aktualisierungen ohne Fehler durch.
*   **Multi-Device & Peer-to-Peer Merge (Geplant für V2+):**
    *   Für Version 1 führt ein einzelnes Gerät das Turnier. Erweiterte Konfliktlösungsdialoge und P2P-Merge mehrerer gleichzeitig schreibender Geräte sind für ein späteres Release vorgesehen.

## 1.5 Datenintegrität und Offline-Betrieb
*   **Offline-First:** Alle Aktionen während Phase 1 und Phase 2 müssen vollständig ohne aktive Internetverbindung möglich sein. Alle Eingaben werden lokal persistiert.
*   **Standalone Offline-Modus:** Die App kann vollständig ohne Serveranbindung betrieben werden.
*   **Synchronisation bei Verbindungsverfügbarkeit:** Eine Internetverbindung ist lediglich für den initialen Spieler-Sync (vor dem Turnier), für zwischenzeitliche Synchronisationen und die Finalisierung (nach dem Turnier) erforderlich. War ein Gerät längere Zeit offline, holt der automatische Sync alle ungespeicherten Ergebnisse gebündelt nach, sobald wieder eine Verbindung besteht oder ein neues Ergebnis eingegeben wird.

## 1.6 Fehlerbehandlung und Fehlerberichte
*   **Transparente Fehlerinformation:** Fehlermeldungen bei Serverinteraktionen kombinieren technische Exceptions mit den genauen Fehlermeldungen des WordPress-Plugins (`WP_Error`-Body).
*   **Fehler an Entwickler melden:** In allen relevanten Fehlerdialogen (Finalisierung, Turnierübernahme) sowie im Einstellungsbereich (*Hilfe & Feedback*) steht eine Schaltfläche bereit, um vorkonfigurierte Fehlerberichte direkt per E-Mail an `monatsblitz@kindermaenner.de` zu übermitteln (inklusive Fallback über die Android-Zwischenablage).
