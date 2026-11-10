# ORBIT Music

Native Android local music player built with Kotlin, Jetpack Compose and AndroidX Media3.

## Features
- Material 3 dark UI with translucent glass-inspired surfaces
- Local audio discovery through MediaStore
- Search, play/pause, previous/next, seek, shuffle and repeat
- Media3 ExoPlayer with MediaSessionService
- Android media notification and lock-screen transport controls
- Adaptive launcher icon with monochrome layer
- Android 13+ notification permission request

## Build
Open in Android Studio with JDK 17, sync Gradle and run the app. Minimum SDK 26; target SDK 35.

## OriginOS / iQOO
ORBIT publishes a standard Android MediaSession and media-style notification. OriginOS controls its own lock-screen and dynamic-area presentation, so the exact appearance must be validated on the target iQOO firmware. Enable notification and lock-screen notification permissions for the app.

## Status
Initial implementation. Device QA, release signing and vendor-specific OriginOS behavior remain to be validated.
