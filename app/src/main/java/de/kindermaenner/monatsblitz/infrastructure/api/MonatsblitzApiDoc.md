Base path: /wp-json/monatsblitz/v1
Players

    * POST /player

        * Creates a new player or returns the ID if the player already exists.

        * Alternatively accepts a batch payload via players or a top-level array.

        * Body (JSON):
            forename: first name
            surname: last name

        * Batch format:
            players: array of objects with forename, surname
            or a top-level array with the same objects

        * Behavior:
            Single JSON requests are still supported.
            Batch requests are validated and stored per player.
            Invalid batch data returns WP_Error with HTTP 400.

        * Batch response:
            success: true
            count: number of processed players
            items: list of per-player responses

    * GET /players
        * Returns all players.

Tournaments


    *POST /tournament

        * Creates a new tournament.
        
        * Body (JSON):
            date: date in format YYYY-MM-DD
            mode: tournament mode (for example 3+5, 5+0)
            round_count (optional): number of rounds, default 1

    * GET /tournaments
        * Returns all tournaments.

    * GET /tournament/{id}
        * Returns tournament details for a given ID.

Games

    * POST /game

        * Creates a game.

        * Alternatively accepts a batch payload via games.

        * Body (JSON):
            tournament_id
            player1_id
            player2_id
            leg_type (optional): round/leg number, default 1
            result (1-0, 0-1, 0.5-0.5)

        * Batch format:
            tournament_id
            games: array of objects with player1_id, player2_id, result, leg_type

        * Behavior:
            Single JSON requests are still supported.
            Batch requests are validated and stored per game.
            Invalid batch data returns WP_Error with HTTP 400.

        * Batch response:
            success: true
            count: number of processed games
            items: list of per-game responses

    * GET /games/{tournament_id}

        * Returns all games for a tournament.

Results
    * POST /result

    * Creates or updates a player's result in a tournament.

    * Alternatively accepts a batch payload via results.

    * Body (JSON):
        tournament_id
        player_id
        points
        rank

    * Batch format:
        tournament_id
        results: array of objects with player_id, points, rank

    * Behavior:
        Single JSON requests are still supported.
        Batch requests are validated and stored per result.
        Invalid batch data returns WP_Error with HTTP 400.

    * Batch response:
        success: true
        count: number of processed results
        items: list of per-result responses

    * GET /results/{tournament_id}
        * Returns all results for a tournament.

Finalization

    * POST /finalize
        *Creates or updates a published post from the configured template post.
        * Body (JSON):
            tournament_id

    * POST /buildYearPage
        * Creates or updates the yearly static page for a given year.
        * Body (JSON):
            year

    * POST /recreatePosts
        * Recreates all tournament posts by iterating over all stored tournaments.
        * For each tournament, it triggers the same finalize logic as POST /finalize.
        * Body (JSON): none
        * Response (JSON):
            processed: number of tournaments found
            succeeded: number of successfully recreated posts
            failed: number of tournaments that failed during finalize
            errors: per-tournament error details

    * Important:
        The post is published immediately.
        Existing monthly posts are updated if a post with meta key blitz-YYYY-MM-DD already exists.
        The title is created in the format Monatsblitz YYYY-MM-DD.
        Template placeholders are replaced.
        If round_count = 1, {{table}} contains the classic cross table including points and rank.
        If round_count > 1, {{table}} contains one cross table per round (without points/rank), followed by an overall table with player name, total points, and rank.
