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
*   **Modus-Konfiguration:**
    *   Wahl des Zeitmodus (z. B. "5+0", "3+2", "Handicap").
    *   Wahl des Formats: Einrundig oder Doppelrundig (Round Robin).
*   **Turnierstart:** Mit dem Start werden alle notwendigen Paarungen (jeder gegen jeden) initial ohne Ergebnis erzeugt.

### Phase 2: Durchführung (Aktiv)
*   **Ergebniserfassung (Kreuztabelle):**
    *   Anzeige aller Paarungen in einer Matrix-Ansicht (Crosstable).
    *   Eingabe von Ergebnissen (1:0, 0:1, ½:½, +:-, -:+).
    *   Ergebnisse werden sofort in der lokalen Datenbank persistiert (Single Source of Truth).
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

## 1.3 Startverhalten und Sitzungsmanagement
*   **Status-Prüfung:** Beim Starten der App wird der Zustand des letzten Turniers aus dem lokalen Speicher (DataStore) ermittelt und mit dem Status auf dem WordPress-Server abgeglichen.
*   **Automatisches Fortsetzen:**
    *   Existiert ein lokales, nicht finalisiertes Turnier, das auch auf dem Server noch als "offen" markiert ist, startet die App direkt in **Phase 2 (Durchführung)**.
    *   Ist kein Turnier vorhanden oder wurde das Turnier bereits (lokal oder remote) finalisiert, startet die App in **Phase 1 (Setup)**.
*   **Initialer Datenabgleich:** Bei aktiver Internetverbindung wird beim App-Start geprüft, ob auf dem Server neuere Daten vorliegen, und gemäß dem Read-before-Write-Prinzip synchronisiert.

## 1.4 Synchronisation und Live-Status (Version 1)
*   **Hintergrund-Synchronisation:** Nach jeder Ergebniseingabe werden geänderte Partien automatisch im Hintergrund übertragen. Die Benutzeroberfläche bleibt jederzeit bedienbar.
*   **Status-Visualisierung:**
    *   Ein Symbol in der Kopfzeile (`TopAppBar`) visualisiert jederzeit den aktuellen Zustand: `SYNCED` (Wolke mit Haken), `SYNCING` (Ladekreis), `FAILED` (Warnung) und `FINALIZED` (Schloss).
    *   Tippen auf das Symbol löst bei Bedarf (z. B. nach Verbindungsabbruch oder bei Fehlern) einen manuellen Synchronisationsversuch aus.
    *   Optionale Statuspunkte an einzelnen Zellen markieren ungesyncte Partien (in den Einstellungen deaktivierbar).
*   **Korrekturfreundlichkeit:** Vor der Finalisierung können Ergebnisse jederzeit korrigiert oder zurückgesetzt werden. Das Backend führt nahtlose Aktualisierungen ohne Fehler durch.
*   **Multi-Device & Peer-to-Peer Merge (Geplant für V2+):**
    *   Für Version 1 führt ein einzelnes Gerät das Turnier. Erweiterte Konfliktlösungsdialoge und P2P-Merge mehrerer gleichzeitig schreibender Geräte sind für ein späteres Release vorgesehen.

## 1.5 Datenintegrität und Offline-Betrieb
*   **Offline-First:** Alle Aktionen während Phase 1 und Phase 2 müssen vollständig ohne aktive Internetverbindung möglich sein. Alle Eingaben werden lokal persistiert.
*   **Synchronisation bei Verbindungsverfügbarkeit:** Eine Internetverbindung ist lediglich für den initialen Spieler-Sync (vor dem Turnier), für zwischenzeitliche Synchronisationen und die Finalisierung (nach dem Turnier) erforderlich. War ein Gerät längere Zeit offline, holt der automatische Sync alle ungespeicherten Ergebnisse gebündelt nach, sobald wieder eine Verbindung besteht oder ein neues Ergebnis eingegeben wird.
