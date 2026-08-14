# ADR 001: Manueller Navigations-Ansatz via RootViewModel

## Status
Veraltet (Abgelöst durch ADR 003)

## Kontext
Im Projekt existierten zwei Ansätze zur Navigation:
1.  Ein zustandsbasierter Ansatz im `RootScreen.kt`.
2.  Die Jetpack Navigation Component Implementierung in `AppNavHost.kt`.

## Entscheidung (Historisch)
Der zustandsbasierte Ansatz wurde initial als maßgeblich dokumentiert, da er in der `MainActivity` genutzt wurde.

## Nachfolger
Dieser Ansatz wurde im Rahmen des Refactorings im August 2026 vollständig durch die **Jetpack Navigation Component** (siehe [ADR 003](ADR-003-jetpack-navigation.md)) abgelöst. Der ungenutzte Dualismus wurde aufgelöst.
