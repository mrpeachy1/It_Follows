# Snail Game iOS MVP

This repository now includes a native iOS MVP skeleton at `ios/SnailGameIOS/`. It is intentionally separate from the working Android app and is not a direct Android conversion.

## What was ported

- A minimal SwiftUI app shell.
- A MapKit game screen that can show the user's location and a snail annotation.
- CoreLocation foreground location permission handling.
- Platform-independent geo/chase math translated from the Android shared math:
  - haversine distance in meters
  - bearing/direction in degrees
  - move-toward behavior for the snail chase
  - proximity checks for catch/game-over logic
- A simple visible game state:
  - active/paused chase state
  - score
  - elapsed seconds
  - distance to snail
  - one-second tick loop while active
- A small XCTest suite for the translated geo math.
- A few practical Android image assets copied into the iOS asset catalog:
  - classic snail
  - snail face
  - zombie snail
- A GitHub Actions workflow that builds the app and runs XCTest on a macOS runner for the iOS simulator when iOS files change.

## What was intentionally deferred

This MVP is a playable map-chase skeleton, not a full rewrite. The following Android behavior is not implemented yet:

- Background gameplay and foreground-service-equivalent behavior.
- Boot/startup receivers.
- Android Google Maps styling and `night_map_style.json` behavior.
- Google Maps iOS SDK integration.
- AdMob / consent / rewarded ads.
- Shop and inventory UI.
- All power-ups and special snail abilities.
- Achievements persistence.
- Snail coins and economy balancing.
- Minigames.
- Audio playback for `ambience01.wav`.
- Full settings screen.
- Full game-over screen.
- Android XML layout parity.
- App icon polish.
- Real-device tuning for GPS accuracy, permission prompts, battery behavior, and map camera behavior.

## Repository layout

```text
ios/SnailGameIOS/
  SnailGameIOS.xcodeproj/
  SnailGameIOS/
    SnailGameIOSApp.swift
    ContentView.swift
    Info.plist
    Models/
      Coordinate.swift
      GeoMath.swift
      GameState.swift
    Services/
      LocationManager.swift
    ViewModels/
      GameViewModel.swift
    Assets.xcassets/
  SnailGameIOSTests/
    GeoMathTests.swift
```

## Opening in Xcode

When you have access to a Mac:

1. Clone this repository.
2. Open `ios/SnailGameIOS/SnailGameIOS.xcodeproj` in Xcode.
3. Select the `SnailGameIOS` scheme.
4. Select an iPhone simulator first to confirm the app opens.
5. Then connect your iPhone 15 and select it as the run destination.

## Signing with a free Apple Account / Personal Team

This project does not assume a paid Apple Developer Program account.

1. In Xcode, open **Settings > Accounts**.
2. Add your Apple Account.
3. Select the `SnailGameIOS` project in the navigator.
4. Select the `SnailGameIOS` target.
5. Go to **Signing & Capabilities**.
6. Enable **Automatically manage signing**.
7. Choose your **Personal Team**.
8. Change the Bundle Identifier from `com.example.SnailGameIOS` to something unique, for example `com.yourname.SnailGameIOS`.
9. Build and run on your iPhone 15.

Notes for free Apple Accounts:

- You can usually install directly from Xcode to your own device.
- Provisioning may expire and require reinstalling from Xcode.
- Some capabilities may be limited compared with a paid developer account.
- This MVP does not rely on TestFlight or App Store Connect.

## Installing on a physical iPhone

1. Connect your iPhone 15 to the Mac.
2. Trust the computer if iOS prompts you.
3. In Xcode, select your iPhone as the run destination.
4. Press **Run**.
5. If iOS asks you to trust the developer profile, follow the prompt or go to **Settings > General > VPN & Device Management** and trust your Apple Account profile.
6. Launch the app and grant **While Using the App** location permission.

## What may not work until tested on a real iPhone

- The simulator does not provide realistic GPS movement unless a simulated location route is configured.
- Location permission prompts can differ between simulator and device.
- Map camera behavior and user-location accuracy need real outdoor testing.
- The current snail spawn offset is intentionally simple and may need tuning.
- The one-second timer is foreground-only and is paused/stopped by app lifecycle behavior.
- Background chase behavior is intentionally not implemented in this MVP.

## CI checks

The iOS workflow is defined in `.github/workflows/ios-build.yml`. It runs on a current macOS GitHub Actions runner and verifies simulator builds only; it does not sign for a physical device, upload to App Store Connect, or use TestFlight.

```sh
xcodebuild build \
  -project ios/SnailGameIOS/SnailGameIOS.xcodeproj \
  -scheme SnailGameIOS \
  -configuration Debug \
  -destination 'platform=iOS Simulator,id=$SIMULATOR_UDID' \
  CODE_SIGNING_ALLOWED=NO \
  CODE_SIGNING_REQUIRED=NO \
  CODE_SIGN_IDENTITY=""
```

When XCTest files are present, CI also runs:

```sh
xcodebuild test \
  -project ios/SnailGameIOS/SnailGameIOS.xcodeproj \
  -scheme SnailGameIOS \
  -configuration Debug \
  -destination 'platform=iOS Simulator,id=$SIMULATOR_UDID' \
  CODE_SIGNING_ALLOWED=NO \
  CODE_SIGNING_REQUIRED=NO \
  CODE_SIGN_IDENTITY=""
```

The workflow prints the selected Xcode version, lists available simulators, and chooses an available iPhone simulator from the runner before building. The simulator check is useful for catching Swift/Xcode compile errors before Mac access, but installing on your physical iPhone 15 still requires opening the project in Xcode on a Mac. Signing with a free Apple Account / Personal Team must be configured manually in Xcode under **Signing & Capabilities**.

Local Linux environments cannot run `xcodebuild`; use GitHub Actions or Xcode on macOS for the iOS compile/test check.

## Android verification note

The initial Codex/Linux Android verification failed before project configuration because Java 25.0.2 was too new for this Android Gradle/Kotlin environment. That is an environment issue, not an Android gameplay or source-code failure.

Android CI is defined in `.github/workflows/android-build.yml` and pins verification to JDK 17 with `actions/setup-java`. The expected Android verification runtime is JDK 17; the workflow prints `java -version`, `./gradlew --version`, and then runs `./gradlew :app:assembleDebug --stacktrace`.
