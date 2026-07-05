# Grundsatz

- Spieler werden in der Datenbank remote gespeichert
- Turnierdaten werden ebenfalls remote gespeichert
- Turnier-ID wird lokal gespeichert, um den Status zwischen App-Starts zu erhalten

# Workflow

1. App startet
   - Prüft: Existiert gespeicherte Turnier-ID?
      * JA  und nicht finalized → TournamentScreen laden
      * NEIN oder finalized → HomeScreen anzeigen

2. HomeScreen: User erstellt Turnier
   * saveTournamentState(id, finalized=false)
   * Navigation zu TournamentScreen

3. TournamentScreen: User drückt Back-Taste
   * BackHandler abfangen
   * Dialog: "Turnier abbrechen?"
     - "Ja, abbrechen" → clearTournamentState() und zeige HomeScreen
     - "Nein, weitermachen" → Dialog schließen und bleibe auf TournamentScreen

   4. Turnier finalisieren (API-Call)
      - finalizeTournament() → saveTournamentState(id, finalized=true)
