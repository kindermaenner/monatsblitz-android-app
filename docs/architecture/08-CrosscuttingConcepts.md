# 8. Querschnittliche Konzepte

### 8.1 Single Source of Truth (SSoT)
Die Architektur erzwingt, dass die Datenbank der SSoT ist. Ein typischer Ablauf für externe Daten sieht so aus:
1.  **Sync Trigger:** Ein Use Case ruft die REST-API auf.
2.  **Persistenz:** Die erhaltenen Daten werden in Room-Entities umgewandelt und gespeichert/aktualisiert.
3.  **Reaktivität:** Repositories bieten `Flow<T>`-Schnittstellen an, die direkt an die DAO-Abfragen gekoppelt sind.
4.  **UI-Update:** Sobald Room die Änderung in der DB registriert, emittiert der Flow neue Daten, die über das ViewModel an die UI fließen.

### 8.2 Dependency Injection
Das Projekt nutzt kein DI-Framework wie Hilt oder Dagger. Stattdessen wird ein manueller `AppContainer` verwendet, der in der `Application`-Klasse initialisiert wird. ViewModels werden über spezifische `ViewModelFactory`-Klassen erzeugt, die ihre Abhängigkeiten aus dem `AppContainer` beziehen.

### 8.3 Persistenz-Konzept
Die App nutzt zwei unterschiedliche Persistenz-Technologien je nach Anwendungsfall:
*   **Room (SQLite):** Wird für strukturierte, relationale Daten verwendet (Spieler, Turniere, Games, Rankings). Dies bildet die fachliche SSoT.
*   **Jetpack DataStore (Preferences):** Wird für einfache Schlüssel-Wert-Paare zur Steuerung des App-Zustands verwendet (z. B. `tournament_id` des aktiven Turniers). Dies ermöglicht die nahtlose Fortführung einer Sitzung nach App-Neustarts.

### 8.4 User Interface & Design System
Die visuelle Gestaltung folgt dem **Material 3 (M3)** Standard. 
*   **Theming:** Zentral definiert in `ui/theme/Theme.kt`. Es unterstützt sowohl Light- als auch Dark-Mode sowie Dynamic Color (ab Android 12).
*   **Farben:** Die Farbpalette ist in `ui/theme/Color.kt` definiert und nutzt M3-spezifische Rollen (Primary, Secondary, Tertiary, Surface etc.).
*   **Typografie:** Die Textstile sind in `ui/theme/Type.kt` nach dem M3-Schema (Headline, Title, Body, Label) strukturiert.
*   **Komponenten:** Es werden ausschließlich M3-Komponenten (`androidx.compose.material3`) verwendet.
