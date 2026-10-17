# API Gap-Analyse: WordPress Plugin vs. Android App Anforderungen

Dieses Dokument fasst die Abweichungen und Schnittstellen zwischen der aktuellen REST-API des **Monatsblitz WordPress Plugins** ([GitHub Repository](https://github.com/kindermaenner/monatsblitz-wp-plugin)) und den aktualisierten Anforderungen der **Monatsblitz Android App** ([`01-FunctionalRequirements.md`](./specs/01-FunctionalRequirements.md) & [`02-TechnicalRequirements.md`](./specs/02-TechnicalRequirements.md)) zusammen.

---

## 1. Management Summary

Die im WordPress-Plugin (`monatsblitz-wp-plugin`) dokumentierte REST API stellt grundlegende CRUD-Funktionen für Spieler, Turniere, Spiele und Ergebnisse sowie die Finalisierung per Template (`/finalize`) bereit.

Durch die Vereinfachung der Spezifikation (Entfall der Master/Slave-Rolle und des Suffix-Systems) hat sich der Anpassungsbedarf am WordPress-Plugin deutlich reduziert:

1. **Multi-Device & Synchronisation (Peer-to-Peer über Backend):**
   * **Read-before-Write:** Die Konflikterkennung und das Merging erfolgen clientseitig in der Android-App. Die API muss lediglich idempotente bzw. überschreibende Updates über `POST /game` / `PUT /game` und `POST /result` für bestehende Spiele unterstützen (zur Abwicklung regulärer Syncs und von "Force Write").
2. **Turnier-Lifecycle & Status:**
   * Nachverfolgbarkeit des Turnierstatus (`open` vs. `finalized`) in `GET /tournaments` und `GET /tournament/{id}`.
   * Serverseitiger Schreibschutz: Nach erfolgreicher Finalisierung (`/finalize`) müssen weitere Schreibzugriffe für das Turnier abgewiesen werden (`HTTP 403 Forbidden` oder `HTTP 409 Conflict`).
3. **Spielerverwaltung & Namenseindeutigkeit (Kein Gap!):**
   * Es darf keine zwei Spieler mit gleichem Namen (`Vorname` + `Nachname`) geben.
   * Das Tabellenschema `wp_monatsblitz_players` besitzt bereits den Unique-Constraint `UNIQUE KEY forename_surname (forename, surname)`.
   * **Kein Änderungsbedarf am WordPress-Plugin:** Das Suffix-Konzept (`display_suffix`) ist entfallen. WordPress weist Namensdopplungen bereits ab, und die Android-App verbietet Duplikate nun ebenfalls strikt.
4. **Jahreswertung (Grand Prix):**
   * Die Berechnung der Jahreswertung in der App ist entfallen. Die Jahresseite wird serverseitig über das existierende `POST /buildYearPage` generiert. Es ist kein neuer Lese-Endpunkt für historische Daten erforderlich.

---

## 2. Detaillierte Lückenanalyse (API Gaps)

### 2.1 Multi-Device-Support & Synchronisation

* **Referenz App-Spezifikation:** [01-FunctionalRequirements.md §1.4](./specs/01-FunctionalRequirements.md#14-multi-device-support-und-synchronisation), [02-TechnicalRequirements.md §2.4.1](./specs/02-TechnicalRequirements.md#241-ger%C3%A4teunabh%C3%A4ngige-synchronisation)
* **Status im WP-Plugin:** Grundlegende Endpunkte vorhanden.

#### Funktionsweise & Anforderungen:
* **Entfall von Master-Rollen:** Endpunkte wie `POST /tournament/{id}/claim-master` werden **nicht** benötigt. Schreibaufrufe müssen keine `device_id` mitsenden.
* **Unterstützung von Updates (Überschreiben):**
  * `POST /game`, `PUT /game` sowie `POST /result` müssen bestehende Spiele/Ergebnisse anhand der IDs bzw. der Paarung aktualisieren können.
  * Dies ist für das reguläre Hochladen lokaler Änderungen sowie für den Benutzer-Entscheid **"Force Write"** (lokale Daten überschreiben Backend) zwingend erforderlich.
* **Lese-Endpunkt für den Abgleich:**
  * `GET /games/{tournament_id}` und `GET /results/{tournament_id}` liefern den aktuellen Serverstand für das clientseitige Pre-Read und Merging vor Schreiboperationen (bereits vorhanden).

---

### 2.2 Jahreswertung (Grand Prix)

* **Referenz App-Spezifikation:** Nicht mehr Teil der App-Anforderungen.
* **Status im WP-Plugin:** `POST /buildYearPage` existiert zur statischen Generierung.
* **Handlungsbedarf:** Keiner. Ein strukturierter JSON-Endpunkt für Jahreswertungen in der App ist nicht erforderlich.

---

### 2.3 Turnier-Status & Lebenszyklus

* **Referenz App-Spezifikation:** [01-FunctionalRequirements.md §1.3](./specs/01-FunctionalRequirements.md#13-startverhalten-und-sitzungsmanagement), [02-TechnicalRequirements.md §2.2](./specs/02-TechnicalRequirements.md#22-zustandsmodell-der-app)
* **Status im WP-Plugin:** Das Schema `wp_monatsblitz_tournaments` speichert bisher nur `(id, year, month, day, mode, round_count)`. Ein Statusfeld fehlt.

#### Notwendige Anpassungen:
* **Erweiterung von `GET /tournament/{id}` und `GET /tournaments`:**
  * Rückgabe von `status` (`"open"` | `"finalized"`).
  * Rückgabe von `updated_at` (Zeitstempel zur Synchronisationsprüfung).
* **Schreibschutz nach Finalisierung:**
  * Sobald ein Turnier finalisiert wurde (`POST /finalize`), müssen weitere Schreibzugriffe (`POST /game`, `POST /result`) für dieses Turnier serverseitig abgewiesen werden (`HTTP 403 Forbidden` oder `HTTP 409 Conflict`).

---

### 2.4 Spielerverwaltung & Namenseindeutigkeit

* **Referenz App-Spezifikation:** [01-FunctionalRequirements.md §1.2 Phase 1](./specs/01-FunctionalRequirements.md#phase-1-turniervorbereitung-setup), [02-TechnicalRequirements.md §2.1](./specs/02-TechnicalRequirements.md#21-domain-modell-player)
* **Status im WP-Plugin:** Tabellenschema besitzt bereits `UNIQUE KEY forename_surname (forename, surname)`.

#### Handlungsbedarf:
* **Kein Änderungsbedarf im WordPress-Plugin.**
* Die API liefert bei Duplikaten bereits einen Fehler (HTTP 400/409 oder WP_Error).
* Die Android-App validiert Vor- und Nachname vor dem Senden und fängt Fehler ab.

---

## 3. Schnittstellen-Matrix (Ist vs. Soll)

| Endpunkt | Methode | WP-Plugin (Ist) | Android-App Anforderung (Soll) | Status / Handlungsbedarf |
| :--- | :---: | :--- | :--- | :--- |
| `/players` | `GET` | Liefert alle Spieler (`id`, `forename`, `surname`) | Spielerliste ohne Suffixe | **OK** |
| `/player` | `POST` | Erstellt Spieler (`forename`, `surname`), weist Duplikate ab | Erstellung mit Namenseindeutigkeit | **OK** |
| `/tournaments` | `GET` | Liefert alle Turniere | Liste inkl. `status` (`open`/`finalized`), `updated_at` | **Erweitern** (Statusfeld ergänzen) |
| `/tournament/{id}` | `GET` | Liefert Turnierdetails | Details inkl. `status`, `updated_at` | **Erweitern** (Statusfeld ergänzen) |
| `/tournament` | `POST` | Erstellt Turnier (`date`, `mode`, `round_count`) | Erstellt Turnier mit initialem Status `open` | **Erweitern** (Status initialisieren) |
| `/game` | `POST`/`PUT` | Speichert/Aktualisiert Spiele (Single & Batch) | Aktualisiert Spiele; abgewiesen wenn Status `finalized` | **Erweitern** (Schreibschutz bei Finalisierung) |
| `/games/{tournament_id}` | `GET` | Liefert alle Spiele eines Turniers | Liefert Spiele für Pre-Read & Merge | **OK** |
| `/result` | `POST` | Speichert Ergebnisse (Single & Batch) | Speichert/Überschreibt Ergebnisse; abgewiesen wenn Status `finalized` | **Erweitern** (Schreibschutz bei Finalisierung) |
| `/results/{tournament_id}` | `GET` | Liefert Ergebnisse eines Turniers | Liefert Ergebnisse für Pre-Read & Merge | **OK** |
| `/finalize` | `POST` | Erstellt WordPress-Post aus Template | Setzt Turnier-Status auf `finalized`, sperrt weitere Edits | **Erweitern** (Statuswechsel + Schreibschutz) |
| `/buildYearPage` | `POST` | Baut statische Jahresseite | Baut statische Jahresseite | **OK** |
| `/recreatePosts` | `POST` | Baut alle Posts neu auf | Baut alle Posts neu auf | **OK** |

---

## 4. Empfohlene Datenbank-Migrationen (WordPress Plugin)

Die notwendigen Änderungen in der WordPress-Datenbank beschränken sich auf die Turniertabelle.

### 4.1 Schema-Anpassungen (DDL)

```sql
-- Erweiterung der Turniertabelle um Status und Änderungszeitpunkt
ALTER TABLE wp_monatsblitz_tournaments 
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'open',
    ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD COLUMN post_id BIGINT UNSIGNED NULL;
```

> **Hinweis:** Die Spielertabelle `wp_monatsblitz_players` bleibt unverändert, da sie bereits `UNIQUE KEY forename_surname (forename, surname)` besitzt.

---

### 4.2 Datenmigrationen für bestehende Daten (DML)

#### 4.2.1 Historische Turniere als `finalized` markieren
Bestehende Turniere dürfen nach dem Schema-Upgrade nicht auf `open` verbleiben, damit für alte Turniere der Schreibschutz aktiv ist:

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

#### 4.2.2 Optionale Verknüpfung zu WordPress-Posts (`post_id`)
```sql
UPDATE wp_monatsblitz_tournaments t
JOIN wp_postmeta pm 
  ON pm.meta_key = CONCAT('blitz-', t.year, '-', LPAD(t.month, 2, '0'), '-', LPAD(t.day, 2, '0'))
SET t.post_id = pm.post_id;
```

---

### 4.3 Umsetzung im WordPress-Plugin (`dbDelta` & Versions-Hook)

* **Vorgehen im Plugin:**
  1. DB-Versionskonstante im Plugin pflegen (z. B. `define('MONATSBLITZ_DB_VERSION', '1.1.0');`).
  2. Beim Plugin-Laden (`plugins_loaded`) die gespeicherte Option `monatsblitz_db_version` prüfen.
  3. Bei Versionsunterschied:
     * `dbDelta()` für das `wp_monatsblitz_tournaments`-Schema ausführen.
     * Datenmigrationen (Abschnitt 4.2) einmalig ausführen.
     * `update_option('monatsblitz_db_version', MONATSBLITZ_DB_VERSION);` setzen.
