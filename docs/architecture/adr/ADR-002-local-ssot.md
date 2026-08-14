# ADR 002: Lokale Datenbank als Single Source of Truth (SSoT)

## Status
Akzeptiert

## Kontext
Die Monatsblitz App muss auch unter instabilen Netzwerkbedingungen (z. B. in Turnierräumen ohne WLAN) zuverlässig funktionieren. Gleichzeitig müssen Daten mit einer WordPress-Website synchronisiert werden. Es muss klar definiert sein, welche Komponente den maßgeblichen Zustand für die Benutzeroberfläche hält.

## Entscheidung
Wir nutzen die lokale Room-Datenbank als primäre und einzige "Single Source of Truth" (SSoT) für alle arbeitsrelevanten Daten (Spieler, Turniere, Ergebnisse).
*   Die UI beobachtet ausschließlich Datenströme (`Flow`), die aus der lokalen Datenbank kommen.
*   Schreibzugriffe der UI erfolgen erst in die lokale Datenbank.
*   Die Synchronisation mit der WordPress-API erfolgt entkoppelt: Daten werden von der API geladen, in der lokalen DB gespeichert und von dort automatisch an die UI emittiert.

## Konsequenzen
*   **Vorteile:** Volle Offline-Fähigkeit; konsistenter Datenzustand in der UI; einfache Reaktivität durch Room-Flows.
*   **Nachteile:** Zusätzlicher Aufwand für Mapping zwischen API-DTOs und DB-Entities; Komplexität bei der Konfliktlösung während der Synchronisation.
