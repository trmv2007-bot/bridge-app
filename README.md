# Bridge App v2.0

A full companion app that gives Termux AI assistant complete control over your phone.

## Features

### 1. Phone Control (Bridge Service)
- Tap, swipe, type text
- Read screen content
- Press home/back/recent
- Take screenshots
- Control Wi-Fi, flashlight, volume, brightness
- Send SMS, make calls, read contacts
- Get GPS location

### 2. AI Chat Companion
- Chat interface in the app
- Connects to OpenAI API (or any compatible API)
- Local fallback responses
- Chat history saved
- Natural language commands

### 3. Voice Commands
- Always-on voice recognition
- Text-to-speech responses
- Voice commands: "battery", "time", "home", "back", "screen"
- Works in background

### 4. Automation Engine
- Create trigger-action automations
- Battery level triggers
- Time-based triggers
- Screen state triggers
- Background execution
- Start/stop engine

### 5. Settings
- AI API key configuration
- Voice service control
- Automation service control
- Service status monitoring

## How it works

1. Install the APK
2. Enable Accessibility Service for "Bridge App"
3. Open the app and configure:
   - Add AI API key (optional)
   - Start voice service (optional)
   - Start automation engine (optional)
4. Use the Chat screen to talk to AI
5. Use Automations screen to create rules
6. Use Settings to configure everything

## Commands

### Bridge Commands (via Termux or Chat)
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

### Chat Commands
| Command | Description |
|---------|-------------|
| `/tap 500 500` | Execute bridge command |
| `/screen` | Read screen |
| `/battery` | Get battery status |
| `automation list` | List automations |
| `automation add` | Add automation |

### Voice Commands
| Command | Description |
|---------|-------------|
| "battery" | Get battery status |
| "time" | Get current time |
| "home" | Go to home screen |
| "back" | Go back |
| "screen" | Read screen content |
| "open [app]" | Open an app |

## Building

### Option 1: GitHub Actions (recommended)

1. Push this repo to GitHub
2. Go to Actions tab
3. Click "Run workflow"
4. Download the APK from artifacts

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
6. Go back to the app
7. Configure settings (API key, services)
8. Start using!

## Communication

The app communicates via files:
- Command: `/sdcard/bridge/cmd.txt`
- Result: `/sdcard/bridge/result.txt`
- Chat history: `/sdcard/bridge/chat_history.json`
- Automations: `/sdcard/bridge/automations.json`

Or via broadcast:
```
adb shell am broadcast -a com.bridge.app.EXECUTE -e command "tap 500 500"
```

## Security

This app has full control of your phone UI. Only install if you trust the source code.
The Accessibility Service can read all screen content and simulate all touch events.

## API Key

To use the AI chat feature, you need an OpenAI API key:
1. Go to https://platform.openai.com/api-keys
2. Create a new API key
3. Open Bridge App > Settings
4. Paste your API key
5. Save

Without an API key, the app uses local responses (limited but functional).

## License

MIT License - Use at your own risk.
