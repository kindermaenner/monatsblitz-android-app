# 9. Architekturentscheidungen (ADR)

Im Projekt werden wichtige Architekturentscheidungen als ADR (Architecture Decision Records) dokumentiert.

*   **[ADR 001: Manueller Navigations-Ansatz via RootViewModel](adr/ADR-001-navigation-approach.md):** (Veraltet) Initialer zustandsbasierter Hosting-Ansatz.
*   **[ADR 002: Lokale Datenbank als Single Source of Truth (SSoT)](adr/ADR-002-local-ssot.md):** Die lokale Room-Datenbank ist der maßgebliche Datenhalter für die UI.
*   **[ADR 003: Einführung Jetpack Navigation Component & Splash Screen API](adr/ADR-003-jetpack-navigation.md):** Umstellung auf standardkonforme Navigation und flackerfreien Start.
*   **[ADR 004: Konsequente Nutzung von Material 3 als Design-Standard](adr/ADR-004-material3-design-system.md):** Verbindliche Festlegung auf M3 für alle UI-Komponenten.
*   **ADR 005: Manuelle Dependency Injection:** Entscheidung gegen Hilt, um volle Kontrolle über den Objektgraph zu behalten.
*   **ADR 006: Room für Persistenz:** Nutzung von Room als typsichere Abstraktion über SQLite.
