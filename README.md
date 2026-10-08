# Wans — Native Android

Wans is a native Jetpack Compose Android social app with an online Supabase backend.

## Implemented modules

- Email/password authentication with username metadata
- Persistent Supabase session detection
- Home dashboard and live-room discovery
- Online voice-room membership state
- Seat management and microphone state
- Owner room moderation: mute, kick, mute-all, room closing
- Room events: chat, reactions, raised hand, PK signal events
- Friend search, friend requests and acceptance
- Direct conversations and text messages
- Notifications
- Wallet and coin history
- Gifts and server-side gift transactions
- Daily quests, challenges and achievements data
- User profile editing and statistics
- Report submission and admin report history
- Role-gated admin dashboard
- Android microphone control
- GitHub Actions debug APK build

## Backend

Supabase project: `txpkrctvdtjcptotfqer`

The mobile app uses only the public publishable key. Privileged server credentials are not included in the APK.

Room presence/membership is synchronized through Supabase database state and heartbeat cleanup. Realtime replication is enabled for room state tables, while polling remains as a compatibility fallback.

## Build

The repository uses:

- Android Gradle Plugin 8.13.2
- Gradle 8.13
- Kotlin 2.2.21
- Jetpack Compose
- Supabase Kotlin 3.8.0
- Java 17

GitHub Actions builds `app/build/outputs/apk/debug/app-debug.apk`.

## Important limitation

The original APK is compiled code. This repository reconstructs its verified feature surface natively, but exact source-level parity with the original APK requires the original project source/ZIP. Network voice transport is intentionally not replaced with an unrelated third-party RTC provider; the original APK inspection did not identify a third-party RTC SDK.
