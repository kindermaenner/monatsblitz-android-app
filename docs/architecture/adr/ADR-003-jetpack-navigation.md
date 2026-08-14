# ADR 003: Einführung Jetpack Navigation Component & Splash Screen API

## Status
Akzeptiert

## Kontext
Um die Navigation innerhalb der App zu standardisieren und das Nutzererlebnis beim Starten der App zu verbessern (Vermeidung von Screen-Flackern), wurde der bisherige manuelle Navigationsansatz überarbeitet.

## Entscheidung
Wir führen die **Jetpack Navigation Component** als zentralen Hosting-Mechanismus ein.
*   **Typsicherheit:** Nutzung von `@Serializable` Routen-Definitionen.
*   **Splash Screen:** Nutzung der offiziellen `androidx.core:core-splashscreen` Bibliothek, um den Startvorgang (Ermittlung der Startseite aus dem DataStore) zu maskieren.
*   **Zentrales Hosting:** `AppNavHost` übernimmt die Kontrolle über alle Screens. `MainActivity` dient nur noch als technischer Einstiegspunkt.
*   **Feature-orientiertes Naming:** Umstellung aller Komponenten auf Namen, die das Feature beschreiben (`TournamentSetup`, `Crosstable`, `Ranking`).

## Konsequenzen
*   **Vorteile:** Standardkonformes Backstack-Management; einfache Skalierbarkeit; kein visuelles Flackern beim Kaltstart.
*   **Nachteile:** Erhöhte Abhängigkeit von Jetpack Bibliotheken.
