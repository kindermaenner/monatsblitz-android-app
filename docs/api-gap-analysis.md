# API Gap-Analyse: WordPress Plugin vs. Android App Anforderungen

Dieses Dokument fasst die Abweichungen und fehlenden Schnittstellen zwischen der aktuellen REST-API des **Monatsblitz WordPress Plugins** ([GitHub Repository](https://github.com/kindermaenner/monatsblitz-wp-plugin)) und den Anforderungen der **Monatsblitz Android App** ([`01-FunctionalRequirements.md`](./specs/01-FunctionalRequirements.md) & [`02-TechnicalRequirements.md`](./specs/02-TechnicalRequirements.md)) zusammen.

---

## 1. Management Summary

Die im WordPress-Plugin (`monatsblitz-wp-plugin`) dokumentierte REST API stellt grundlegende CRUD-Funktionen für Spieler, Turniere, Spiele und Ergebnisse sowie die Finalisierung per Template (`/finalize`) bereit. 

Für den vollständigen Betrieb der Android-App gemäß Spezifikation fehlen jedoch zentrale Schnittstellen und Datenstrukturen in vier Schlüsselbereichen:
1. **Multi-Device & Concurrency-Management (Master-Rolle):** Zuweisung und Validierung der Schreibhoheit inkl. `HTTP 409 Conflict`-Handling.
2. **Jahreswertung (Grand Prix):** Abruf historischer Ergebnisse eines Kalenderjahres für die clientseitige Top-5-Berechnung.
3. **Turnier-Lifecycle & Status:** Nachverfolgbarkeit des Turnierstatus (`open` vs. `finalized`) und Schreibschutz nach Abschluss.
4. **Spielerverwaltung & Namensgleichheit:** Unterstützung für Unterscheidungs-Suffixe (z. B. "Max Mustermann (1)").

---

## 2. Detaillierte Lückenanalyse (API Gaps)

### 2.1 Multi-Device-Support & Master-Management

* **Referenz App-Spezifikation:** [01-FunctionalRequirements.md §1.4](./specs/01-FunctionalRequirements.md#14-multi-device-support-und-synchronisation), [02-TechnicalRequirements.md §2.4.1](./specs/02-TechnicalRequirements.md#241-master-management--concurrency)
* **Status im WP-Plugin:** Nicht vorhanden.

#### Fehlende Endpunkte & Logik:
* **`POST /wp-json/monatsblitz/v1/tournament/{id}/claim-master` (NEU)**
  * **Zweck:** Ermöglicht einem Gerät, die Schreibhoheit für ein Turnier zu übernehmen.
  * **Payload:** `{ "device_id": "string" }`
  * **Verhalten:** Speichert die `device_id` als aktiven Master in der Datenbank (`wp_monatsblitz_tournaments`).
* **Concurrency-Prüfung bei Schreiboperationen (`POST /game`, `PUT /game`, `POST /result`, `POST /finalize`) (ÄNDERUNG)**
  * **Anforderung:** Alle schreibenden Aufrufe müssen die `device_id` übermitteln.
  * **Verhalten bei Konflikt:** Ist die anfragende `device_id` nicht der aktuelle Master, muss die API mit **`HTTP 409 Conflict`** antworten.
  * **Konflikt-Response:** Der Response-Body bei 409 muss den aktuellen Stand des Turniers (inkl. aller Spiele/Ergebnisse auf dem Server) enthalten, um den Konflikt-Dialog in der App zu unterstützen.

---

### 2.2 Jahreswertung (Grand Prix / Historische Daten)

* **Referenz App-Spezifikation:** [01-FunctionalRequirements.md §1.2 Phase 3](./specs/01-FunctionalRequirements.md#phase-3-abschluss-finalisierung), [02-TechnicalRequirements.md §2.4.2](./specs/02-TechnicalRequirements.md#242-historische-grand-prix-daten)
* **Status im WP-Plugin:** Es existiert lediglich `POST /buildYearPage` (zur statischen HTML-Seitengenerierung mit Best-7-Regel), aber kein strukturierter Lese-Endpunkt für die App.

#### Fehlende Endpunkte & Logik:
* **`GET /wp-json/monatsblitz/v1/results/yearly/{year}` (NEU)**
  * **Zweck:** Liefert alle bisherigen Turnierplatzierungen und erzielten Punkte aller Spieler für ein Kalenderjahr in strukturierter Form (JSON).
  * **Verwendung in der App:** Die App berechnet lokal die Jahreswertung (Punktevergabe Plätze 1–5 nach Dense Ranking, Summe der besten 5 Turniere) und visualisiert das Jahresgesamtranking direkt nach der Finalisierung.

---

### 2.3 Turnier-Status & Lebenszyklus

* **Referenz App-Spezifikation:** [01-FunctionalRequirements.md §1.3 & §1.4](./specs/01-FunctionalRequirements.md#13-startverhalten-und-sitzungsmanagement)
* **Status im WP-Plugin:** Das Tabellenschema `wp_monatsblitz_tournaments` speichert bisher nur `(id, year, month, day, mode, round_count)`. Ein Status (`open` / `finalized`) oder Master-Gerät wird nicht geführt.

#### Notwendige Anpassungen:
* **Erweiterung von `GET /tournament/{id}` und `GET /tournaments`:**
  * Rückgabe von `status` (`"open"` | `"finalized"`).
  * Rückgabe von `master_device_id` (Gerät mit Schreibrecht).
  * Rückgabe von `updated_at` (Zeitstempel zur Synchronisationsprüfung).
* **Schreibschutz nach Finalisierung:**
  * Sobald ein Turnier finalisiert wurde (`POST /finalize`), müssen weitere Schreibzugriffe (`POST /game`, `POST /result`) für dieses Turnier serverseitig abgewiesen werden (`HTTP 403 Forbidden` oder `HTTP 409 Conflict`).

---

### 2.4 Spielerverwaltung & Namensgleichheit

* **Referenz App-Spezifikation:** [01-FunctionalRequirements.md §1.2 Phase 1](./specs/01-FunctionalRequirements.md#phase-1-turniervorbereitung-setup), [02-TechnicalRequirements.md §2.1](./specs/02-TechnicalRequirements.md#21-domain-modell-player)
* **Status im WP-Plugin:** Das Tabellenschema `wp_monatsblitz_players` besitzt einen Constraint `UNIQUE(forename, surname)`.

#### Notwendige Anpassungen:
* **Unterstützung von Namenszusätzen:**
  * Wenn zwei Spieler denselben Vor- und Nachnamen tragen, muss die Datenbank ein optionales Feld `display_suffix` unterstützen (z. B. `"(1)"`, `"(2)"`).
  * Unique-Constraint in WordPress anpassen: `UNIQUE(forename, surname, display_suffix)`.
* **Erweiterung von `POST /player` und `GET /players`:**
  * Übergabe und Rückgabe des Feldes `display_suffix`.

---

## 3. Schnittstellen-Matrix (Ist vs. Soll)

| Endpunkt | Methode | WP-Plugin (Ist) | Android-App Anforderung (Soll) | Status / Handlungsbedarf |
| :--- | :---: | :--- | :--- | :--- |
| `/players` | `GET` | Liefert alle Spieler (`id`, `forename`, `surname`) | Spielerliste inkl. `display_suffix` | **Erweitern** (`display_suffix` ergänzen) |
| `/player` | `POST` | Erstellt/Gibt Spieler zurück (Single & Batch) | Erstellung mit optionalem `display_suffix` | **Erweitern** (`display_suffix` unterstützen) |
| `/tournaments` | `GET` | Liefert alle Turniere | Liste inkl. `status`, `master_device_id` | **Erweitern** (Metadaten ergänzen) |
| `/tournament/{id}` | `GET` | Liefert Turnierdetails | Details inkl. `status`, `master_device_id` | **Erweitern** (Metadaten ergänzen) |
| `/tournament` | `POST` | Erstellt Turnier (`date`, `mode`, `round_count`) | Erstellt Turnier mit initialem Status `open` & `device_id` | **Erweitern** (Master zuweisen) |
| `/tournament/{id}/claim-master` | `POST` | *Nicht vorhanden* | Übernahme der Master-Rolle mit `device_id` | **NEU erstellen** |
| `/game` | `POST`/`PUT` | Speichert/Aktualisiert Spiele (Single & Batch) | Prüfung auf `device_id`; wirft `409 Conflict` bei Fremd-Master | **Erweitern** (Master-Prüfung + 409-Handling) |
| `/games/{tournament_id}` | `GET` | Liefert alle Spiele eines Turniers | Liefert Spiele eines Turniers | **OK** |
| `/result` | `POST` | Speichert Ergebnisse (Single & Batch) | Prüfung auf `device_id` und Finalisierungsstatus | **Erweitern** (Schreibschutz / Master-Check) |
| `/results/{tournament_id}` | `GET` | Liefert Ergebnisse eines Turniers | Liefert Ergebnisse eines Turniers | **OK** |
| `/results/yearly/{year}` | `GET` | *Nicht vorhanden* | Liefert Jahresergebnisse aller Spieler (JSON) | **NEU erstellen** |
| `/finalize` | `POST` | Erstellt WordPress-Post aus Template | Setzt Turnier-Status auf `finalized`, sperrt weitere Edits | **Erweitern** (Statuswechsel + Schreibschutz) |
| `/buildYearPage` | `POST` | Baut statische Jahresseite | Baut statische Jahresseite | **OK** |
| `/recreatePosts` | `POST` | Baut alle Posts neu auf | Baut alle Posts neu auf | **OK** |

---

## 4. Empfohlene Datenbank-Migrationen & Datenbereinigung (WordPress Plugin)

Bei einer bereits bestehenden, produktiven Datenbank müssen neben den DDL-Schemaanpassungen auch DML-Datenmigrationen durchgeführt werden, um Datenkonsistenz zu gewährleisten.

### 4.1 Schema-Anpassungen (DDL)

```sql
-- 1. Erweiterung der Turniertabelle
ALTER TABLE wp_monatsblitz_tournaments 
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'open',
    ADD COLUMN master_device_id VARCHAR(100) NULL,
    ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD COLUMN post_id BIGINT UNSIGNED NULL;

-- 2. Erweiterung der Spielertabelle für Namensgleichheit
ALTER TABLE wp_monatsblitz_players 
    ADD COLUMN display_suffix VARCHAR(50) NOT NULL DEFAULT '',
    DROP INDEX forename_surname, -- Bisheriger UNIQUE-Index
    ADD UNIQUE KEY forename_surname_suffix (forename, surname, display_suffix);
```

> **Hinweis zum Unique-Index:** In MySQL/MariaDB schützt ein `UNIQUE`-Index nicht vor mehrfachen `NULL`-Werten. Daher sollte `display_suffix` als `NOT NULL DEFAULT ''` (leerer String) definiert werden, damit Spieler ohne Namenszusatz weiterhin strikt eindeutig bleiben.

---

### 4.2 Datenmigrationen für bestehende Daten (DML)

#### 4.2.1 Historische Turniere als `finalized` markieren
Bestehende Turniere dürfen nach dem Schema-Upgrade nicht standardmäßig auf `open` verbleiben, da die App sonst versuchen würde, diese fortzusetzen, und kein Schreibschutz bestünde.

```sql
-- Turniere mit bereits erfassten Ergebnissen auf 'finalized' setzen
UPDATE wp_monatsblitz_tournaments t
SET t.status = 'finalized'
WHERE EXISTS (
    SELECT 1 
    FROM wp_monatsblitz_results r 
    WHERE r.tournament_id = t.id
);

-- Alle Turniere, deren Datum vor dem heutigen Tag liegt, ebenfalls finalisieren
UPDATE wp_monatsblitz_tournaments 
SET status = 'finalized' 
WHERE STR_TO_DATE(CONCAT(year, '-', LPAD(month, 2, '0'), '-', LPAD(day, 2, '0')), '%Y-%m-%d') < CURDATE()
  AND status = 'open';
```

#### 4.2.2 Spieler-Initialisierung
```sql
-- Sicherstellen, dass alle bestehenden Spieler einen leeren String als display_suffix haben
UPDATE wp_monatsblitz_players 
SET display_suffix = '' 
WHERE display_suffix IS NULL;
```

#### 4.2.3 Optionale Verknüpfung zu WordPress-Posts (`post_id`)
Bestehende Turnierberichte können anhand des bisher genutzten Metadaten-Keys `blitz-YYYY-MM-DD` direkt in `wp_monatsblitz_tournaments` verknüpft werden:

```sql
UPDATE wp_monatsblitz_tournaments t
JOIN wp_postmeta pm 
  ON pm.meta_key = CONCAT('blitz-', t.year, '-', LPAD(t.month, 2, '0'), '-', LPAD(t.day, 2, '0'))
SET t.post_id = pm.post_id;
```

---

### 4.3 Umsetzung im WordPress-Plugin (`dbDelta` & Versions-Hook)

WordPress führt über `dbDelta()` nur strukturelle Tabellenänderungen durch, jedoch keine Datenaktualisierungen (`UPDATE`-Statements).

* **Empfohlenes Vorgehen im Plugin:**
  1. Eine DB-Versionskonstante im Plugin pflegen (z. B. `define('MONATSBLITZ_DB_VERSION', '1.1.0');`).
  2. Beim Plugin-Laden / `plugins_loaded` die gespeicherte Option `monatsblitz_db_version` prüfen.
  3. Bei Versionsunterschied:
     - `dbDelta()` für Schema-Updates ausführen.
     - Datenmigrations-Queries (Abschnitt 4.2) einmalig ausführen.
     - `update_option('monatsblitz_db_version', MONATSBLITZ_DB_VERSION);` setzen.

