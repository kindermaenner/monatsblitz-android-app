# 9. Architekturentscheidungen (ADR)

Im Projekt werden wichtige Architekturentscheidungen als ADR (Architecture Decision Records) dokumentiert.

*   **[ADR 001: Manueller Navigations-Ansatz via RootViewModel](adr/ADR-001-navigation-approach.md):** Entscheidung für einen zustandsbasierten Hosting-Ansatz anstelle der Navigation Component.
*   **[ADR 002: Lokale Datenbank als Single Source of Truth (SSoT)](adr/ADR-002-local-ssot.md):** Die lokale Room-Datenbank ist der maßgebliche Datenhalter für die UI, um Offline-Fähigkeit und Konsistenz zu garantieren.
*   **ADR 3: Manuelle DI:** Entscheidung gegen Hilt, um die Komplexität gering zu halten und volle Kontrolle über den Objektgraph zu behalten.
*   **ADR 4: Room für Persistenz:** Nutzung von Room als Abstraktion über SQLite für typsichere Abfragen.
