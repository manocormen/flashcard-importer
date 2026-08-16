# Flashcard Importer

Open-source Android app for importing flashcards from the [Flashcard Generator](https://github.com/manocormen/flashcard-generator) into [AnkiDroid](https://github.com/ankidroid/anki-android), developed as part of [GSoC 2026](https://summerofcode.withgoogle.com/programs/2026/projects/XGTDRLdf), with AnkiDroid as mentoring organization.

## Features

- Scans card-sharing QR codes shown by the Flashcard Generator.
- Fetches generated flashcards over the local network.
- Displays the fetched cards: renders Markdown and supports swipe-to-discard.
- Lets users choose an AnkiDroid deck to push the cards to.
- Adds the cards through the AnkiDroid API, preserving their formatting.

## Setup

For now, the app must be built and installed from source.

1. Clone the project.
2. Open it in [Android Studio](https://developer.android.com/studio).
3. Run it on an Android device with [AnkiDroid](https://github.com/ankidroid/anki-android) installed and set up.
4. Set up the [Flashcard Generator](https://github.com/manocormen/flashcard-generator) on a computer on the same local network.

## Usage

1. Generate cards with the Flashcard Generator and click **Share**.
2. Open the Flashcard Importer and scan the QR code.
3. Review the imported cards and swipe to discard any you don't want.
4. Tap **Add to AnkiDroid**, pick a deck, and push the cards to AnkiDroid.

## Demo

TODO: Add video demo.

## Constraints

For now, the app has the following constraints:

- Requires Android 7.0 or newer with AnkiDroid and Google Play services.
- Requires the phone and Flashcard Generator to be on the same local network.
- Fetches cards over HTTP, so sharing should be used only on trusted networks.
- Supports only flashcards in Q&A format, i.e. [basic notes](https://docs.ankiweb.net/getting-started.html#note-types).

## Development

The Gradle wrapper provides useful development commands. Here's a selection:

```bash
./gradlew ktlintFormat               # Format the code
./gradlew ktlintCheck                # Check Kotlin formatting and linting
./gradlew check                      # Run all non-mutable QA: signal issues
./gradlew :app:assembleDebug         # Build the app
./gradlew :app:installDebug          # Install the app on a connected device
./gradlew :app:testDebugUnitTest     # Run unit tests
```

## Pipeline

The app is structured as a pipeline with the following components:

TODO: Add pipeline image.

## Structure

The project is broken down into the following files, which roughly map to each step in our pipeline.

```bash
.
├── LICENSE
├── README.md
├── app
│   └── src
│       ├── main
│       │   └── java/com/manocormen/flashcardimporter
│       │       ├── MainActivity.kt       # App flow and permissions
│       │       ├── ScanScreen.kt         # QR scanning entry screen
│       │       ├── ImportCards.kt        # Card fetching
│       │       ├── ImportViewModel.kt    # Card import state
│       │       ├── ImportScreen.kt       # Card preview and discarding
│       │       ├── ExportCards.kt        # AnkiDroid API integration
│       │       ├── ExportViewModel.kt    # Card export state
│       │       └── ExportScreen.kt       # Deck selection
│       └── test
│           └── ...                       # Unit tests
```

## License

This software is distributed with an AGPL licence. Refer to LICENSE for more details.
