# 9. Architekturentscheidungen (ADR)

Im Projekt werden wichtige Architekturentscheidungen als ADR (Architecture Decision Records) dokumentiert.

*   **[ADR 001: Manueller Navigations-Ansatz via RootViewModel](adr/ADR-001-navigation-approach.md):** (Veraltet) Initialer zustandsbasierter Hosting-Ansatz.
*   **[ADR 002: Lokale Datenbank als Single Source of Truth (SSoT)](adr/ADR-002-local-ssot.md):** Die lokale Room-Datenbank ist der maßgebliche Datenhalter für die UI.
*   **[ADR 003: Einführung Jetpack Navigation Component & Splash Screen API](adr/ADR-003-jetpack-navigation.md):** Umstellung auf standardkonforme Navigation und flackerfreien Start.
*   **ADR 4: Manuelle DI:** Entscheidung gegen Hilt.
*   **ADR 5: Room für Persistenz:** Nutzung von Room als Abstraktion über SQLite.
