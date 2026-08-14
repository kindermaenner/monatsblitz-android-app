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
*   **Flexible Tournament Setup:** Support for various blitz modes (e.g., 3+2 minutes, 5+0 minutes) and tournament formats (single or double round-robin).
*   **Interactive Crosstable:** A central matrix view for entering game results (Win, Loss, Draw) with immediate local persistence.
*   **Real-time Rankings:** Automatic calculation of tournament standings based on entered results.
*   **Offline-First Approach:** Full offline capability ensures tournaments can be conducted without reliable internet access. The local Room database serves as the Single Source of Truth (SSoT).
*   **WordPress Integration:** Automatic synchronization of players from the club website and publishing final results back to the WordPress plugin to generate news posts.

## Tech Stack
*   **UI:** Jetpack Compose with **Material 3** Design System.
*   **Navigation:** Type-safe Jetpack Navigation Component.
*   **Architecture:** Clean Architecture with a strict separation of UI, Domain, and Infrastructure layers.
*   **Persistence:** **Room** for structured data and **Jetpack DataStore** for session management.
*   **Networking:** Retrofit for REST API communication.
