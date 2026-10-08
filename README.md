# Wans

Wans 3.1 recovery project.

The source repository was empty, so this project is being reconstructed from the supplied Wanas-v3.1 APK. The APK contains native Jetpack Compose code and a WanasViewModel with voice-room, seat, microphone and moderation logic.

## Recovery plan
- Restore the native Android foundation.
- Recreate the existing Wans UI/features from the APK.
- Move room membership/presence to server-backed realtime state.
- Preserve room seats, microphone state, raised-hand requests and moderation.
- Build and test an installable APK before publishing a release.
