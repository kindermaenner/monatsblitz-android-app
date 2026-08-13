# ADR 001: Manueller Navigations-Ansatz via RootViewModel

## Status
Akzeptiert (Ist-Zustand)

## Kontext
Im Projekt existieren zwei Ansätze zur Navigation:
1.  Ein zustandsbasierter Ansatz im `RootScreen.kt`, der über das `RootViewModel` und einen `RootUiState` zwischen den Haupt-Screens (`Home`, `Tournament`) umschaltet.
2.  Die Jetpack Navigation Component Implementierung in `AppNavHost.kt`.

Die `MainActivity` nutzt aktuell ausschließlich den zustandsbasierten Ansatz des `RootScreen`.

## Entscheidung
Wir dokumentieren den zustandsbasierten Ansatz als den aktuell maßgeblichen Navigationsmechanismus für das Hosting der App. Der `AppNavHost.kt` verbleibt als vorbereitete, aber ungenutzte Komponente im Code (technische Schuld).

## Konsequenzen
*   **Vorteile:** Einfache Kontrolle über den globalen App-Zustand (z.B. automatisches Wiederaufnehmen eines Turniers nach App-Neustart via DataStore).
*   **Nachteile:** Abweichung von Android-Standards; Schwierigkeiten bei der Skalierung auf viele Screens; Komplexere Backstack-Verwaltung.
*   **Maßnahme:** Es wird empfohlen, in einer zukünftigen Phase den `RootScreen` so umzubauen, dass er den `AppNavHost` als zentrale Hosting-Komponente nutzt, um den Dualismus aufzulösen.
