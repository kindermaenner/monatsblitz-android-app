# 4. Lösungsstrategie

### 4.1 Zentrale Prinzipien
*   **Offline-First / Local Single Source of Truth:** Die lokale Room-Datenbank ist die primäre und einzige Quelle der Wahrheit für die UI. Sämtliche Daten (Spieler, Turniere, Ergebnisse) werden erst in der lokalen Datenbank persistiert. Die REST-API dient lediglich zur Initialbefüllung (Sync) oder Archivierung, wird aber nie direkt von der UI für die Anzeige von Arbeitsdaten abgefragt.
*   **Zustands-Persistenz:** Flüchtige Steuerungsdaten (wie die ID des aktuell aktiven Turniers) werden im **Jetpack DataStore** persistiert, um den App-Flow über Neustarts hinweg konsistent zu halten.
*   **Schichtenarchitektur:** Trennung in UI, Domain und Infrastructure zur Entkopplung von Frameworks (Android/Room/Retrofit).
*   **Unidirektionaler Datenfluss:** ViewModels halten den State via StateFlow, die UI beobachtet diesen reaktiv.
