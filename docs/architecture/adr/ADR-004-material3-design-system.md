# ADR 004: Konsequente Nutzung von Material 3 als Design-Standard

## Status
Akzeptiert

## Kontext
Für eine moderne Android-Anwendung ist ein konsistentes, ansprechendes und barrierefreies Design-System essenziell. Es muss sichergestellt werden, dass neue UI-Komponenten einem einheitlichen Standard folgen, um die Wartbarkeit und das Nutzererlebnis zu optimieren.

## Entscheidung
Wir legen **Material 3 (M3)** als den verbindlichen Design-Standard für die Monatsblitz App fest.
*   Es werden ausschließlich Bibliotheken aus dem Package `androidx.compose.material3` verwendet.
*   Die Verwendung von Material 2 (M2) oder älteren UI-Standards ist nicht gestattet.
*   Theming (Farben, Typografie, Shapes) muss über das zentrale `MaterialTheme` der App gesteuert werden, anstatt hartkodierte Werte in den Screens zu verwenden.

## Konsequenzen
*   **Vorteile:** Einheitliches Look-and-Feel; Unterstützung moderner Android-Features wie Dynamic Color; vereinfachtes UI-Testing; klare Richtlinien für neue Screens.
*   **Nachteile:** Bindung an das Material-Design-Vokabular von Google.
