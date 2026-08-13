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
