# NotifyLens

NotifyLens is an Android notification and UI-event history center. It records notifications in normal mode and can optionally capture Toast/Dialog/Popup/Snackbar events with Accessibility and LSPosed enhancements.

## Current features

- Notification history through `NotificationListenerService`
- Full notification parsing: title, text, BigText, TextLines, MessagingStyle messages, actions and raw extras
- Best-effort text extraction from `contentView`, `bigContentView` and `headsUpContentView`
- Notification lifecycle: posted/updated/removed timestamps
- Notification subtypes: standard, message, call, alarm, media, progress, bubble, full-screen, foreground-service, ongoing, silent and system
- Ranking metadata: importance, conversation, bubble capability, ambient and suspended state
- Timeline filtering by event type and full-text search
- Per-app history view and per-app filtering
- Per-app privacy controls: pause recording or redact content while keeping metadata
- Accessibility capture for Toast and best-effort UI prompts
- LSPosed API 102 enhancement hooks for Toast, Dialog, PopupWindow and Material Snackbar in selected scoped apps
- Encrypted Room database using SQLCipher; database passphrase is protected by Android Keystore
- Configurable retention: 7/30/90 days or forever
- Detailed event viewer including source app/package/channel/flags/progress/messages/actions/raw extras
- Local-only storage; Android backup is disabled

## Architecture

```text
NotificationListenerService ─┐
AccessibilityService ────────┼─> EventRecord -> Room/SQLCipher -> Timeline / Apps / Details
LSPosed collectors ──────────┘
```

The normal notification collector works without root or LSPosed. LSPosed is an optional enhancement for UI events that Android does not expose through the notification-listener API.

## Build

Requirements:

- JDK 17
- Gradle 9.4.1
- Android SDK 37

```bash
gradle :app:assembleDebug
gradle :app:assembleRelease
```

GitHub Actions builds both debug and release APKs on pushes and pull requests to `main`.

## Permissions / setup

1. Open NotifyLens and grant notification access.
2. Optionally enable the accessibility service for Toast and UI-event capture.
3. Optionally enable NotifyLens in LSPosed and scope the module to apps for which enhanced Toast/Dialog/Popup/Snackbar capture is wanted.

## Capture limitations

- If an app never places its full content into the Android notification object, normal notification access cannot reconstruct that missing content.
- RemoteViews extraction is best-effort because custom notification layouts can execute app-specific rendering logic.
- Accessibility-based Toast/Dialog/Snackbar/Popup classification is heuristic and varies by app/UI framework.
- LSPosed enhancement only operates inside selected module scopes and is intentionally optional.
- Custom canvas/OpenGL/WebView-only prompts may expose little or no semantic text.

## Privacy

Notification history can contain messages, OTPs and other private data. NotifyLens therefore encrypts its local database, disables Android backup, and supports per-app ignore/redaction policies.
