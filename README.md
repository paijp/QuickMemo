# Quick Memo - Android Lock Screen Note App

A sample Android app that provides a memo input feature accessible from the lock screen.

## Features

- **Persistent notification** with custom layout on lock screen
- **Memo input activity** displayed over lock screen (no unlock required)
- **Action buttons**: New, List, Remind, Photo
- **Quick Settings tile** for fast access
- Dark theme UI

## Requirements

- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 34
- Min SDK: 27 (Android 8.1)
- Kotlin 1.9+

## How to Use

1. Open in Android Studio
2. Build and run on a device or emulator (API 27+)
3. Tap "Enable lock screen memo" to start the foreground service
4. Lock the device - the memo notification appears on the lock screen
5. Tap the notification or action buttons to open the memo input

## Architecture

- `MainActivity` - Launcher screen with service toggle
- `MemoActivity` - Full-screen memo input shown over lock screen
- `MemoNotificationService` - Foreground service with custom notification layout
- `NotificationActionReceiver` - Handles notification button actions
- `MemoTileService` - Quick Settings tile

## Notes

- This is a sample/demo app. Memo data is not persisted.
- On some manufacturer ROMs (Xiaomi, Samsung, etc.), additional permissions may be needed.
- The `foregroundServiceType="specialUse"` requires a declaration in Play Console for production release.
