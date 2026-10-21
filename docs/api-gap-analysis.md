# API Gap-Analyse: WordPress Plugin vs. Android App Anforderungen

Dieses Dokument fasst den Schnittstellenabgleich zwischen der REST-API des **Monatsblitz WordPress Plugins** ([GitHub Repository](https://github.com/kindermaenner/monatsblitz-wp-plugin)) und den Anforderungen der **Monatsblitz Android App** ([`01-FunctionalRequirements.md`](./specs/01-FunctionalRequirements.md) & [`02-TechnicalRequirements.md`](./specs/02-TechnicalRequirements.md)) zusammen.

> **Status:** Alle für **Version 1** notwendigen Anpassungen an der REST-API und dem WordPress-Plugin wurden vollständig implementiert und per Unit-Tests verifiziert.

---

## 1. Management Summary

Die im WordPress-Plugin (`monatsblitz-wp-plugin`) bereitgestellte REST API stellt grundlegende CRUD-Funktionen für Spieler, Turniere, Spiele und Ergebnisse sowie die Finalisierung per Template (`/finalize`) bereit.

Auf Basis der Abstimmung für Release 1 ergibt sich folgendes Bild:

1. **Multi-Device & Peer-to-Peer Synchronisation:**
   * **Entscheidung:** Multi-Device-Support und Konflikt-Merging sind für ein **zukünftiges Release (V2+)** geplant.
   * **Version 1:** Die App arbeitet mit einem einzelnen führenden Gerät (Turnierleiter vor Ort). Es sind serverseitig vorerst keine erweiterten Konflikt- oder Merge-Schnittstellen erforderlich.
2. **Turnier-Lifecycle, Status & Schreibschutz (Umgesetzt im Plugin):**
   * **Status-Rückgabe:** `GET /tournaments` und `GET /tournament/{id}` liefern das Statusfeld `status` (`"open"` bzw. `"finalized"`) sowie das ISO-Format `date` (`YYYY-MM-DD`).
   * **Finalisierung:** `POST /finalize` persistiert den Statuswechsel des Turniers in der Datenbank (`status = 'finalized'`).
   * **Serverseitiger Schreibschutz:** Nach der Finalisierung werden weitere Schreibversuche über `POST /game` oder `POST /result` serverseitig mit `HTTP 403 Forbidden` (`tournament_finalized`) abgewiesen.
   * **`updated_at`:** Wurde evaluiert und **ersatzlos gestrichen**, da die App Daten auf Spiel-Ebene abgleicht und kein Zeitstempel erforderlich ist.
3. **Spielerverwaltung & Namenseindeutigkeit (Kein Handlungsbedarf):**
   * Vor- und Nachname bilden zusammen das eindeutige Kriterium.
   * Das Tabellenschema `wp_monatsblitz_players` besitzt bereits den Unique-Constraint `UNIQUE KEY unique_player_name (forename, surname)` und weist Duplikate ab.
4. **Jahreswertung (Grand Prix):**
   * Die Jahresseite wird serverseitig über das existierende `POST /buildYearPage` (bzw. automatisch bei `/finalize`) generiert. Es ist kein eigener Lese-Endpunkt für die App erforderlich.

---

## 2. Status der Kernbereiche

### 2.1 Synchronisation, Live-Status & Upsert
* **Referenz App-Spezifikation:** [01-FunctionalRequirements.md §1.4](./specs/01-FunctionalRequirements.md#14-synchronisation-und-live-status-version-1), [02-TechnicalRequirements.md §2.5](./specs/02-TechnicalRequirements.md#25-datenfluss-dirty-flags--live-synchronisation)
* **Status für Release 1:** **Vollständig umgesetzt**.
  * **Hintergrund-Synchronisation:** Die App synchronisiert nach jeder Ergebniseingabe im Hintergrund ohne Blockierung der UI (`TournamentSyncManager`).
  * **Batching & Retry:** Zuvor aufgelaufene ungesyncte Partien (`dirty = 1`) werden bei erneuter Verbindung automatisch mitgesendet.
  * **Serverseitiges Upsert (`POST /game`):** Das WordPress-Plugin (`CreateGameHandler.php`) prüft vor Insert, ob eine Partie bereits existiert, und wechselt transparent auf `updateSingle`. Dies erlaubt problemlose Korrekturen bei Falscheingaben.
  * **Live-Status & Manueller Sync:** Das Status-Icon in der TopBar visualisiert den Zustand (`SYNCED`, `SYNCING`, `FAILED`, `FINALIZED`). Bei `FAILED` löst ein Klick gezielt einen manuellen Neuversuch aus.
  * **Zell-Indikatoren:** Ungesyncte Partien werden durch einen dezenten Punkt markiert (in den App-Einstellungen konfigurierbar).
* **Multi-Device & P2P-Merge:** Geplant für ein zukünftiges Release (V2+).

---

### 2.2 Jahreswertung (Grand Prix)

* **Referenz App-Spezifikation:** Nicht Bestandteil der mobilen App.
* **Status im WP-Plugin:** `POST /buildYearPage` existiert zur statischen Generierung und wird beim Finalisieren von Blitzturnieren automatisch getriggert.
* **Handlungsbedarf:** Keiner. Status: **OK**.

---

### 2.3 Turnier-Status & Lebenszyklus

* **Referenz App-Spezifikation:** [01-FunctionalRequirements.md §1.3](./specs/01-FunctionalRequirements.md#13-startverhalten-und-sitzungsmanagement), [02-TechnicalRequirements.md §2.2](./specs/02-TechnicalRequirements.md#22-zustandsmodell-der-app)
* **Status im WP-Plugin:** **Vollständig umgesetzt**.

#### Umgesetzte Anpassungen:
* **Datenbankschema:** `wp_monatsblitz_tournaments` definiert `status ENUM('open','finalized') NOT NULL DEFAULT 'open'`.
* **Erweiterung von `GET /tournament/{id}` und `GET /tournaments`:**
  * Rückgabe von `status` (`"open"` | `"finalized"`).
  * Rückgabe von `date` im ISO-Format `YYYY-MM-DD` (für direktes Mapping auf `TournamentDto.date`).
* **Initialer Status (`POST /tournament`):**
  * Turniere werden mit initialem Status `'open'` in der Datenbank angelegt.
* **Finalisierung (`POST /finalize`):**
  * Beim Veröffentlichen des Turnier-Posts wird das Turnier in der Datenbank über `markFinalized()` auf `status = 'finalized'` aktualisiert.
* **Schreibschutz nach Finalisierung:**
  * Schreibversuche über `POST /game` (Single & Batch), `PUT /game` und `POST /result` (Single & Batch) auf ein bereits finalisiertes Turnier werden mit `HTTP 403 Forbidden` (`tournament_finalized`) blockiert.

---

### 2.4 Turniermodi & Dynamischer Modus-Katalog

* **Referenz App-Spezifikation:** [01-FunctionalRequirements.md §1.2](./specs/01-FunctionalRequirements.md), [02-TechnicalRequirements.md §2.1b & §2.6](./specs/02-TechnicalRequirements.md)
* **Status im WP-Plugin:** **Vollständig umgesetzt**.

#### Umgesetzte Anpassungen:
* **Datenbankschema:** Neue Tabelle `wp_monatsblitz_modes (id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50) NOT NULL UNIQUE)`. Initiales Seeding mit `'3+2'`, `'5+0'` sowie bestehenden Turniermodi.
* **REST-Endpunkte:**
  * `GET /monatsblitz/v1/modes`: Liefert alle im Katalog hinterlegten Modi als Array `{ id, name }`.
  * `POST /monatsblitz/v1/mode`: Legt neue Modi an (mit Validierung und Duplikatsprüfung).
* **Snapshot-Prinzip:** `wp_monatsblitz_tournaments.mode` bleibt unverändert `VARCHAR(30) NOT NULL`, um historische Berichte stabil zu halten und One-Shot-Turniere ohne Katalogeintrag zu ermöglichen.
* **Admin-Bereich:** Bearbeitungs- und Löschmöglichkeit unter *Monatsblitz → Einstellungen*.

---

### 2.4 Spielerverwaltung & Namenseindeutigkeit

* **Referenz App-Spezifikation:** [01-FunctionalRequirements.md §1.2 Phase 1](./specs/01-FunctionalRequirements.md#phase-1-turniervorbereitung-setup), [02-TechnicalRequirements.md §2.1](./specs/02-TechnicalRequirements.md#21-domain-modell-player)
* **Status im WP-Plugin:** Tabellenschema besitzt `UNIQUE KEY unique_player_name (forename, surname)`.
* **Handlungsbedarf:** Keiner. Namensdopplungen werden serverseitig abgewiesen. Status: **OK**.

---

## 3. Schnittstellen-Matrix (Ist vs. Soll)

| Endpunkt | Methode | WP-Plugin (Ist) | Android-App Anforderung (V1) | Umsetzungsstatus (V1) |
| :--- | :---: | :--- | :--- | :---: |
| `/players` | `GET` | Liefert alle Spieler (`id`, `forename`, `surname`) | Spielerliste für Teilnehmerauswahl | **Erledigt / OK** |
| `/player` | `POST` | Erstellt Spieler (`forename`, `surname`), weist Duplikate ab | Spieler-Neuanlage mit Namenseindeutigkeit | **Erledigt / OK** |
| `/tournaments` | `GET` | Liefert alle Turniere inkl. `status`, ISO-`date` und `date_formatted` | Liste inkl. Status zur Feststellung offener Turniere | **Erledigt** |
| `/tournament/{id}` | `GET` | Liefert Turnierdetails inkl. `status`, ISO-`date` und `date_formatted` | Details zur Prüfung, ob Turnier `open` oder `finalized` ist | **Erledigt** |
| `/tournament` | `POST` | Erstellt Turnier (`date`, `mode`, `round_count`) mit Status `open` | Startet Turnier-Setup | **Erledigt** |
| `/game` | `POST`/`PUT` | Speichert/Aktualisiert Spiele; blockiert wenn Status `finalized` (HTTP 403) | Speichert Spiele; schreibgeschützt nach Abschluss | **Erledigt** |
| `/games/{tournament_id}` | `GET` | Liefert alle Spiele eines Turniers | Liefert Spiele zur Ansicht / Abgleich | **Erledigt / OK** |
| `/result` | `POST` | Speichert Ergebnisse; blockiert wenn Status `finalized` (HTTP 403) | Speichert Gesamtergebnisse; schreibgeschützt nach Abschluss | **Erledigt** |
| `/results/{tournament_id}` | `GET` | Liefert Ergebnisse eines Turniers | Liefert Ranking/Ergebnisse | **Erledigt / OK** |
| `/finalize` | `POST` | Erstellt WP-Post und setzt Status auf `finalized` | Finalisiert Turnier und sperrt weitere Änderungen | **Erledigt** |
| `/buildYearPage` | `POST` | Baut statische Jahresseite | Serverseitig (automatisch bei `/finalize`) | **Erledigt / OK** |
| `/recreatePosts` | `POST` | Baut alle Posts neu auf | Wartungs-Endpunkt im Backend | **Erledigt / OK** |

---

## 4. Datenbank & Migrationen (WordPress Plugin)

### 4.1 Schema der Turniertabelle (`wp_monatsblitz_tournaments`)

Das Schema im Plugin (`DatabaseSchema.php`) umfasst:

```sql
CREATE TABLE wp_monatsblitz_tournaments (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    year SMALLINT NOT NULL,
    month TINYINT NOT NULL,
    day TINYINT NOT NULL,
    mode VARCHAR(30) NOT NULL,
    round_count TINYINT(1) NOT NULL DEFAULT 1,
    status ENUM('open','finalized') NOT NULL DEFAULT 'open',

    PRIMARY KEY (id),
    UNIQUE KEY unique_tournament_date (year, month, day)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 4.2 Einmalige Datenmigration für bestehende Live-Daten

Beim Deployment auf Produktiv-Instanzen mit bereits bestehenden historischen Turnieren sollten alte Turniere einmalig als `finalized` markiert werden, damit für sie der serverseitige Schreibschutz greift:

```sql
-- Alle Turniere mit erfassten Ergebnissen oder vergangener Austragung finalisieren
UPDATE wp_monatsblitz_tournaments 
SET status = 'finalized' 
WHERE STR_TO_DATE(CONCAT(year, '-', LPAD(month, 2, '0'), '-', LPAD(day, 2, '0')), '%Y-%m-%d') < CURDATE()
  AND status = 'open';
```
