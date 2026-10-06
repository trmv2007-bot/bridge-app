# Bridge App

A simple Android app that gives Termux full UI control over your phone.

## How it works

1. Install the APK
2. Enable Accessibility Service for "Bridge App"
3. Use the `bridge` script in Termux to send commands

## Commands

| Command | Description |
|---------|-------------|
| `tap <x> <y>` | Tap at coordinates |
| `swipe <x1> <y1> <x2> <y2> [duration]` | Swipe gesture |
| `text <text>` | Type text |
| `home` | Press home button |
| `back` | Press back button |
| `recent` | Press recents button |
| `screenshot` | Take screenshot |
| `screen` | Read screen UI tree |
| `currentapp` | Get current app package |
| `find <text>` | Find text on screen |
| `taptext <text>` | Tap on text |
| `scroll <up/down/left/right>` | Scroll |
| `clipboard` | Get clipboard |
| `clipboard set <text>` | Set clipboard |

## Building

### Option 1: GitHub Actions (recommended)

1. Push this repo to GitHub
2. Go to Actions tab
3. Click "Build Bridge App" workflow
4. Click "Run workflow"
5. Download the APK from artifacts

### Option 2: Android Studio

1. Open project in Android Studio
2. Build > Build Bundle(s) / APK(s) > Build APK(s)
3. Install on phone

## Installation

1. Install the APK
2. Open Bridge App
3. Tap "Enable Accessibility Service"
4. Find "Bridge App" in the list
5. Toggle it ON
6. Go back to Termux and test:
   ```
   bridge tap 500 500
   ```

## Communication

The app communicates via files:
- Command: `/sdcard/bridge/cmd.txt`
- Result: `/sdcard/bridge/result.txt`

Or via broadcast:
```
adb shell am broadcast -a com.bridge.app.EXECUTE -e command "tap 500 500"
```

## Security

This app has full control of your phone UI. Only install if you trust the source code.
