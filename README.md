# AI Assistant - Expo Project

A modern AI chat assistant built with Expo and React Native, featuring Google Gemini AI integration.

## Features

- **AI Chat**: Interactive conversations with Google Gemini AI
- **File Analysis**: Upload and analyze documents, images, and videos
- **YouTube Integration**: Summarize and transcribe YouTube videos
- **URL Understanding**: Browse and extract information from web pages
- **Live Voice Mode**: Real-time voice conversations with AI
- **Modern UI**: Dark theme with smooth animations

## Tech Stack

- **Framework**: Expo SDK 53
- **Language**: TypeScript
- **Navigation**: Expo Router
- **AI**: Google Generative AI (Gemini)
- **UI Components**: React Native with custom styling

## Prerequisites

- Node.js >= 18
- npm or yarn
- Expo CLI (`npm install -g expo-cli`)
- iOS Simulator (for Mac) or Android Emulator

## Installation

1. Install dependencies:
```bash
npm install
```

2. Start the development server:
```bash
npm start
```

3. Run on your device:
- Press `i` for iOS simulator
- Press `a` for Android emulator
- Scan QR code with Expo Go app on physical device

## Project Structure

```
/workspace
├── app/                    # Expo Router pages
│   ├── _layout.tsx        # Root layout with navigation
│   └── index.tsx          # Main chat screen
├── assets/                 # Images and icons
├── src/
│   ├── components/        # Reusable UI components
│   │   ├── ChatInput.tsx
│   │   ├── MessageBubble.tsx
│   │   └── SystemIcon.tsx
│   └── services/          # API and business logic
│       └── api.ts
├── types.ts               # TypeScript type definitions
├── app.json              # Expo configuration
├── babel.config.js       # Babel configuration
├── package.json          # Dependencies
└── tsconfig.json         # TypeScript configuration
```

## Configuration

### App Settings (app.json)

- App name and slug
- Bundle identifiers for iOS/Android
- Permissions (camera, microphone, storage)
- Splash screen and icon configuration

### API Keys

The app fetches API keys dynamically from a remote key pool service. No local configuration needed.

## Available Scripts

- `npm start` - Start Expo development server
- `npm run android` - Run on Android device/emulator
- `npm run ios` - Run on iOS simulator
- `npm run web` - Run in web browser
- `npm run lint` - Run ESLint
- `npm run typecheck` - Run TypeScript type checking

## Permissions

The app requests the following permissions:

- **Microphone**: For voice input and live conversations
- **Camera**: For document scanning and image capture
- **Photo Library**: For selecting images and files
- **Storage**: For file access and caching

## Troubleshooting

### Build Issues

Clear cache and reinstall:
```bash
rm -rf node_modules
npm install
npx expo start -c
```

### Permission Issues

Ensure you've granted all necessary permissions in your device settings.

### API Errors

Check your internet connection and verify the API key pool service is accessible.

## License

MIT License - See LICENSE file for details.

## Credits

Developed by stech-vision team
Lead: Sujan Rai
