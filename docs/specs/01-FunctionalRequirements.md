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
    *   Verwendung von **Dense Ranking** (keine Platz-Lücken bei Punktgleichheit, z. B. 1, 1, 2, 3).
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

## 1.4 Multi-Device-Support und Synchronisation
*   **Gleichberechtigte Geräte:**
    *   Jedes Gerät darf mit dem Backend synchronisieren. Es gibt keine Master/Slave-Rollen und keine Zuweisung einer exklusiven Schreibhoheit an ein bestimmtes Gerät.
*   **Manuelle Synchronisation:**
    *   Der Benutzer kann jederzeit über eine geeignete Bedienung (z. B. Sync-Button oder Pull-to-Refresh) eine Synchronisation anstoßen.
*   **Read-before-Write & Merge:**
    *   Grundsätzlich wird vor jedem Schreibvorgang (Write / Upload lokaler Änderungen ins Backend) zuerst ein Lesezugriff (Read / Download des aktuellen Remote-Stands) durchgeführt.
    *   **Automatischer Merge:** Auf dem Server vorhandene Änderungen oder neue Ergebnisse, die lokal noch nicht erfasst wurden und zu keinen lokalen Änderungen im Widerspruch stehen, werden automatisch in die lokale Datenbank übernommen (Merge fehlender Daten).
    *   Lokale Änderungen für Spiele, die auf dem Server unverändert geblieben sind, werden anschließend an das Backend übertragen.
*   **Konflikterkennung & Benutzerentscheidung:**
    *   Ein Konflikt entsteht, wenn für dasselbe Spiel sowohl lokal als auch auf dem Server ein Ergebnis geändert wurde und diese Werte voneinander abweichen.
    *   Bei einem solchen Konflikt wird der Synchronisationsvorgang unterbrochen und der Benutzer über einen Dialog zur Entscheidung aufgefordert:
        1.  **Force Read (Remote überschreibt Lokal):** Die Daten aus dem Backend überschreiben die lokalen Daten. Lokale Änderungen werden verworfen und der Stand des Servers übernommen.
        2.  **Force Write (Lokal überschreibt Backend):** Die lokalen Daten überschreiben die Daten im Backend. Der Stand auf dem Server wird mit den lokalen Änderungen aktualisiert.
        3.  **Keine Aktion:** Es findet keine Synchronisation statt. Die Daten bleiben unverändert (lokale Daten und Backend-Daten bleiben so wie sie sind), und der Konflikt bleibt bestehen (wird beim nächsten Sync erneut erkannt).

## 1.5 Datenintegrität und Offline-Betrieb
*   **Offline-First:** Alle Aktionen während Phase 1 und Phase 2 müssen vollständig ohne aktive Internetverbindung möglich sein. Alle Eingaben werden lokal persistiert.
*   **Synchronisation bei Verbindungsverfügbarkeit:** Eine Internetverbindung ist lediglich für den initialen Spieler-Sync (vor dem Turnier), für zwischenzeitliche Synchronisationen und die Finalisierung (nach dem Turnier) erforderlich. War ein Gerät längere Zeit offline, kann der Datenabgleich jederzeit nachgeholt werden, sobald wieder eine Verbindung besteht.
