# 1. Funktionale Anforderungen - Monatsblitz Android App

Dieses Dokument beschreibt die funktionalen Anforderungen an die Monatsblitz Android App zur Durchführung von Schachturnieren.

## 1.1 Übersicht
Die App dient als mobiles Werkzeug für Turnierleiter, um monatliche Blitzschachturniere vor Ort durchzuführen, Ergebnisse zu erfassen und diese final an eine WordPress-Instanz zu übertragen. Ein Fokus liegt auf der vollen Offline-Fähigkeit während der Turnierdauer.

## 1.2 Turnier-Lebenszyklus

### Phase 1: Turniervorbereitung (Setup)
*   **Spielerauswahl:** Der Turnierleiter wählt Teilnehmer aus einer Liste bestehender Spieler aus.
*   **Spieler-Synchronisation:** Die Liste der bestehenden Spieler wird automatisch mit dem WordPress-Backend synchronisiert.
*   **Spielerneuanlage:**
    *   Turnierleiter können neue Spieler manuell in der App anlegen. Diese werden lokal gespeichert und beim nächsten Sync an WordPress übertragen.
    *   **Eindeutigkeit vor Ort:** Falls ein Spieler mit identischem Vor- und Nachnamen bereits lokal existiert, bietet die App die Anlage eines zweiten Spielers mit automatischem Suffix an (z. B. "Max Mustermann (1)", "Max Mustermann (2)").
*   **Spieler-Synchronisation:**
    *   Die Liste der bestehenden Spieler wird automatisch mit dem WordPress-Backend synchronisiert.
    *   **Merge-Prozess:** Besitzt ein lokaler Spieler noch keine Verknüpfung zum Backend (`remoteKey`), existiert dort aber bereits ein Spieler mit identischem Namen, wird der Nutzer gefragt, ob die Einträge verknüpft werden sollen oder ein neuer Backend-Eintrag erstellt werden soll.
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

### Phase 3: Abschluss (Finalisierung)
*   **Manueller Abschluss:** Der Turnierleiter löst den Abschluss des Turniers explizit per Button-Klick aus.
*   **Datenübertragung:** Die Gesamtergebnisse werden per REST-API an das WordPress-Plugin gesendet, um dort Berichte zu generieren und das Turnier zu archivieren.
*   **Jahreswertung (Grand Prix):**
    *   Nach der Finalisierung berechnet die App die aktuelle Jahreswertung.
    *   **Punktevergabe:** Basierend auf dem Turnier-Ranking (Dense Ranking) erhalten die Plätze 1 bis 5 jeweils 5, 4, 3, 2 oder 1 Punkt(e). Spieler auf demselben Platz erhalten die gleiche Punktzahl.
    *   **Berechnungslogik:** Die App addiert die Punkte der besten 5 Turnierplatzierungen eines Kalenderjahres für jeden Spieler.
    *   **Datenquelle:** Die Ergebnisse vergangener Turniere des Jahres werden über die WordPress REST-API bezogen.
    *   **Anzeige:** Das aktualisierte Gesamtranking wird zusammen mit dem aktuellen Turnierergebnis und den neu vergebenen Punkten in einer finalen Ansicht dargestellt.
*   **Schreibschutz:** Nach der erfolgreichen Übertragung ist das Turnier in der App abgeschlossen. Es sind keine weiteren Änderungen an Ergebnissen mehr möglich (Read-only).

## 1.3 Startverhalten und Sitzungsmanagement
*   **Status-Prüfung:** Beim Starten der App wird der Zustand des letzten Turniers aus dem lokalen Speicher (DataStore) ermittelt und mit dem Status auf dem WordPress-Server abgeglichen.
*   **Automatisches Fortsetzen:**
    *   Existiert ein lokales, nicht finalisiertes Turnier, das auch auf dem Server noch als "offen" markiert ist, startet die App direkt in **Phase 2 (Durchführung)**.
    *   Ist kein Turnier vorhanden oder wurde das Turnier bereits (lokal oder remote) finalisiert, startet die App in **Phase 1 (Setup)**.
*   **Konflikterkennung beim Start:** Falls das lokale Gerät ungesyncte Daten besitzt, aber nicht mehr den Master-Status (siehe 1.4) innehat, wird der Nutzer vor dem Betreten der Kreuztabelle zur Konfliktlösung aufgefordert.

## 1.4 Multi-Device-Support und Synchronisation
*   **Master-Rolle (Schreibhoheit):**
    *   Nur ein Gerät darf gleichzeitig Schreibzugriff (Master) auf ein Turnier haben.
    *   Ein neues Gerät kann den Master-Status jederzeit anfordern (z. B. bei Übernahme der Turnierleitung). Nach einer Sicherheitsabfrage wird dieses Gerät zum neuen Master auf dem Server erklärt.
*   **Lazy Role Switch (Umschaltung bei Bedarf):**
    *   Ein Gerät, das seinen Master-Status an ein anderes Gerät verloren hat, wird nicht aktiv benachrichtigt (kein Polling).
    *   Die Umschaltung in den **Read-Only-Modus** erfolgt "lazy" beim nächsten Synchronisationsversuch. Verweigert der Server die Annahme der Daten aufgrund des verlorenen Master-Status, sperrt die App die lokalen Eingabefelder.
*   **Konfliktmanagement (Verwaiste Daten):**
    *   Besitzt ein Gerät lokale Änderungen, die noch nicht synchronisiert wurden, während ein anderes Gerät den Master-Status übernommen hat, erscheint ein Dialog.
    *   **Option A (Verwerfen):** Die lokalen Änderungen werden gelöscht, der aktuelle Stand vom Server geladen und das Gerät wechselt in den Read-Only-Modus.
    *   **Option B (Erzwingen):** Das Gerät fordert den Master-Status zurück und versucht, die lokalen Änderungen nachträglich zu synchronisieren (nur nach ausdrücklicher Warnung).
*   **Remote-Finalisierung:**
    *   Wird ein Turnier auf Gerät B finalisiert (Phase 3), gilt dieser Status als absolut.
    *   Sobald Gerät A diesen Status vom Server erfährt, werden alle lokalen ungesyncten Änderungen verworfen und die App wechselt sofort in die schreibgeschützte Ergebnisansicht (Phase 3).
*   **Synchronisationsstatus:**
    *   In der UI muss jederzeit ersichtlich sein, ob das Gerät aktuell "Master" oder "Read-Only" ist und wann der letzte erfolgreiche Datenabgleich stattfand.

## 1.5 Datenintegrität und Offline-Betrieb
*   **Offline-First:** Alle Aktionen während Phase 1 und 2 müssen ohne aktive Internetverbindung möglich sein.
*   **Synchronisation:** Eine Internetverbindung ist lediglich für den initialen Spieler-Sync (vor dem Turnier) und die Finalisierung (nach dem Turnier) zwingend erforderlich.
