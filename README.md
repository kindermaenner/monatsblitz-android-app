# monatsblitz-android-app

![Coverage](https://raw.githubusercontent.com/kindermaenner/sonar-badges/main/badges/monatsblitz-android/coverage.svg)
![Bugs](https://raw.githubusercontent.com/kindermaenner/sonar-badges/main/badges/monatsblitz-android/bugs.svg)
![Code Smells](https://raw.githubusercontent.com/kindermaenner/sonar-badges/main/badges/monatsblitz-android/code_smells.svg)
![Security Hotspots](https://raw.githubusercontent.com/kindermaenner/sonar-badges/main/badges/monatsblitz-android/security_hotspots.svg)

The android app for blitz chess tournaments which are stored in the  WordPress database and uses the plugins REST api to store the results and create a post for them.

## Motivation
The motivation and overall project context are documented in the [Monatsblitz WordPress plugin](https://github.com/kindermaenner/monatsblitz-wp-plugin) repository. This README focuses solely on the Android application and its technical implementation.

## Project Overview
**Monatsblitz** is a specialized Android application designed for chess clubs (specifically SG Königslutter) to manage their monthly blitz chess tournaments. It serves as a mobile client that operates primarily offline during tournaments and synchronizes data with a central WordPress-based backend.

## Key Features
*   **Player Management:** Browse and select participants from a player list synchronized via the WordPress REST API.
*   **Flexible Tournament Setup & Dynamic Modes:**
    *   Dynamic game mode catalog synchronized with WordPress (`wp_monatsblitz_modes`).
    *   Add new modes directly from the app, either permanently with server synchronization or as a one-shot tournament (e.g., club anniversary blitz).
    *   Automated mode pre-selection: None, Fixed mode, or cyclic Rotation (e.g., alternating 3+2 and 5+0) backed by recent server tournaments as the Single Source of Truth.
    *   Support for single and double round-robin formats.
*   **Interactive Crosstable:** A central matrix view for entering game results (Win, Loss, Draw) with immediate local persistence.
*   **Early Tournament Exit (Player Withdrawal):** Tournament directors can mark players who leave the tournament early (e.g., youth players having school the next morning) by clicking their name in the crosstable. Open games are automatically scored according to club rules:
    *   **Youth / Goodwill rule (Kulanzregel):** Remaining unplayed games are scored as unplayed draw (`=`, 0.5 points for both players).
    *   **Regular rule:** Remaining unplayed games are scored as forfeit loss (`-`, 0 points for the leaving player, `+` / 1 point for opponents).
    *   Withdrawn players are marked with `(z)` (*zurückgetreten*) across the crosstable, rankings, and published WordPress results.
*   **Real-time Rankings:** Automatic calculation of tournament standings based on entered results.
*   **Offline-First Approach & Standalone Offline Mode:**
    *   Full offline capability ensures tournaments can be conducted without reliable internet access. The local Room database serves as the Single Source of Truth (SSoT).
    *   **Standalone Offline Mode ("Dauerhafter Offline-Betrieb"):** Complete operation without any server or WordPress configuration (ideal for plush toy tournaments, home tournaments, or chess events without website publication).
    *   **First-Time Routing:** If neither server nor offline mode is configured, the app opens the Settings screen directly, allowing the user to configure the backend or enable offline mode with a single tap. Once configured or set to offline mode, the app starts directly into Tournament Setup.
    *   **Per-Tournament Local Mode:** Even with a configured WordPress server, individual tournaments can be run locally ("Nur lokal spielen") without website publication or background sync.
    *   **Local Player Management & Merge:** Local players created during offline tournaments persist in the Room DB and remain reusable. If a local player is later used in an online tournament, they are automatically merged and promoted to the server without duplicate entries.
*   **PDF Export & System Share / Print:**
    *   Export tournament results directly from the ranking screen as clean A4 PDF documents.
    *   Includes tournament metadata, final ranking standings, and the complete crosstable matrix (with result indicators, tie-breaks, and withdrawal markers).
    *   Integrated with the Android system share sheet for one-tap sharing via messengers, email, or direct printing.
*   **QR Code Setup:** Instant configuration of server URL, API key, and club-wide default tournament mode preselection (fixed or rotation) by scanning the QR code in WordPress Admin.
*   **Automatic Live Sync & Status:** Non-blocking background synchronization after every result change with batched retry, upsert support for score corrections, an interactive TopAppBar status indicator, and configurable cell-level indicators.
*   **WordPress Integration:** Automatic synchronization of players from the club website and publishing final results back to the WordPress plugin to generate news posts.

## Tech Stack
*   **UI:** Jetpack Compose with **Material 3** Design System.
*   **Navigation:** Type-safe Jetpack Navigation Component.
*   **Architecture:** Clean Architecture with a strict separation of UI, Domain, and Infrastructure layers.
*   **Persistence:** **Room** for structured data and **Jetpack DataStore** for session management.
*   **Networking:** Retrofit for REST API communication.
