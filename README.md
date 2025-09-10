# 🐌 It Follows: Snail Edition

A GPS-based horror game for Android where a relentless snail slowly chases you in real life. Inspired by the concept of *It Follows*—but slower, slimier, and way more stressful.

> “It’s always coming. Very slowly. And now it has a shell.”

---

## 📱 Features

- 🔥 Real-time GPS snail chasing logic  
- 🗺️ Google Maps integration with zoom/pan/fit features  
- 🎮 In-game minigames like *Don’t Look Away*  
- 💥 Power-ups like Salt Bomb, Goo Trap, Snail Repel, and more  
- 🛒 Shop system and inventory UI  
- 🌙 Night Mode: faster snail, darker map, whispers intensify  
- 🧟 Multiple snail variants (classic, zombie, nightmare, etc.)  
- 💾 Game state auto-resume and continue functionality
---
## 🧙 Credits
- Concept inspired by It Follows
- Code & Design: [mrpeachy1]
- Sound FX: [Horror Ambient Pack by Puritune]
- Map SDK: Google Maps Platform

---
## 🚀 Getting Started

### Prerequisites

- Android Studio (latest version recommended)  
- A physical Android device with GPS enabled  
- Google Maps API Key ([setup guide here](https://developers.google.com/maps/documentation/android-sdk/start))  
- Permissions: Location, Internet

### Setup Instructions

```bash
git clone https://github.com/mrpeachy1/it-follows-snail.git
cd it-follows-snail
```

## 📄 Java Files

- `MainMenuActivity.java`: Simple activity that launches `GameActivity`.
- `app/src/main/java/com/example/itfollows/AdConsentActivity.java`: Launcher activity that gathers ad consent and preloads a rewarded ad.
- `app/src/main/java/com/example/itfollows/AdManager.java`: Singleton managing preloaded rewarded ads.
- `app/src/main/java/com/example/itfollows/App.java`: Application class creating the notification channel for the game service.
- `app/src/main/java/com/example/itfollows/BootReceiver.java`: Broadcast receiver that starts the game service after boot or app update.
- `app/src/main/java/com/example/itfollows/CoinFlipMinigameActivity.java`: Coin flip mini-game where players guess heads or tails.
- `app/src/main/java/com/example/itfollows/CreditsActivity.java`: Displays the credits screen with ambient music.
- `app/src/main/java/com/example/itfollows/GameActivity.java`: Core gameplay with map, power-ups, and snail chasing logic.
- `app/src/main/java/com/example/itfollows/GameManager.java`: Static holder indicating if the session is a new game.
- `app/src/main/java/com/example/itfollows/GameOverActivity.java`: Shows game over stats and optional rewarded ad revive.
- `app/src/main/java/com/example/itfollows/GameService.java`: Foreground service requesting location updates and maintaining game state.
- `app/src/main/java/com/example/itfollows/GameStateRepo.java`: SharedPreferences repository for player and snail data.
- `app/src/main/java/com/example/itfollows/GeoMath.java`: Geographic utilities for distance and movement calculations.
- `app/src/main/java/com/example/itfollows/GoogleMobileAdsConsentManager.java`: Helper managing User Messaging Platform consent flow.
- `app/src/main/java/com/example/itfollows/HoldToSurviveMinigameActivity.java`: Hold-the-screen mini-game with a random survival timer.
- `app/src/main/java/com/example/itfollows/HowToPlayActivity.java`: Instructional screen explaining gameplay basics.
- `app/src/main/java/com/example/itfollows/LocationUpdatesReceiver.java`: Processes location updates and advances the snail.
- `app/src/main/java/com/example/itfollows/MainActivity.java`: Legacy activity that displays the snail's distance from the player.
- `app/src/main/java/com/example/itfollows/MainMenuActivity.java`: Main menu for starting or resuming the game and navigating to other screens.
- `app/src/main/java/com/example/itfollows/MapsActivity.java`: Sample Google Maps activity scaffold.
- `app/src/main/java/com/example/itfollows/MusicManager.java`: Static manager controlling looping ambient music.
- `app/src/main/java/com/example/itfollows/ReconcileWorker.java`: WorkManager task to reconcile snail movement in the background.
- `app/src/main/java/com/example/itfollows/SettingsActivity.java`: Settings screen for volume, snail speed/distance, units, and sprite selection.
- `app/src/main/java/com/example/itfollows/SlimeTapMinigameActivity.java`: Tap-to-burst slime bubbles mini-game under a time limit.
- `app/src/main/java/com/example/itfollows/SnailPhysics.java`: Calculates snail movement toward the player and triggers game over notifications.
- `app/src/main/java/com/example/itfollows/SnailSpriteAdapter.java`: RecyclerView adapter displaying available snail sprites.
- `app/src/main/java/com/example/itfollows/models/SnailSprite.java`: Model describing a snail sprite's metadata and resources.
