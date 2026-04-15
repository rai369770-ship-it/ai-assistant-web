# AI Assistant - React Native

A cross-platform AI assistant application built with React Native for Android (and iOS) development. This is a complete rewrite of the web-based AI assistant using React Native with accessibility-first principles.

## Features

- **Chat Interface**: Real-time chat with AI assistant powered by Google Gemini
- **Voice Input**: Speech-to-text functionality using native voice recognition
- **Live Mode**: Real-time voice conversation with AI (Gemini Live API)
- **File Upload**: Support for uploading and analyzing files (images, documents, audio, video)
- **YouTube Integration**: Summarize and transcribe YouTube videos
- **URL Understanding**: Browse and analyze web content
- **Grounding Sources**: View search sources used by AI for responses
- **Markdown Support**: Rich text rendering with code blocks
- **Accessibility First**: Full accessibility support with screen reader compatibility

## Tech Stack

- **React Native** 0.76.6
- **TypeScript** for type safety
- **@google/genai** for Gemini AI integration
- **react-native-vector-icons** for icons
- **react-native-markdown-display** for markdown rendering
- **@react-native-voice/voice** for speech recognition
- **react-native-document-picker** for file selection

## Prerequisites

- Node.js >= 18
- React Native CLI
- Android Studio (for Android development)
- Xcode (for iOS development, macOS only)

## Installation

1. Install dependencies:
```bash
npm install
```

2. For iOS (macOS only):
```bash
cd ios && pod install && cd ..
```

3. For Android, ensure you have Android Studio and SDK configured.

## Running the App

### Android
```bash
npm run android
```

### iOS
```bash
npm run ios
```

### Start Metro Bundler
```bash
npm start
```

## Project Structure

```
/workspace
├── src/
│   ├── App.tsx              # Main application component
│   ├── components/
│   │   ├── ChatInput.tsx    # Input component with voice/live mode
│   │   └── MessageBubble.tsx # Message display component
│   ├── services/
│   │   └── api.ts           # API integration with Gemini
│   └── hooks/               # Custom hooks (if needed)
├── types.ts                 # TypeScript type definitions
├── app.json                 # React Native app configuration
├── babel.config.js          # Babel configuration
├── metro.config.js          # Metro bundler configuration
├── tsconfig.json            # TypeScript configuration
└── package.json             # Dependencies and scripts
```

## Accessibility Features

- All interactive elements have `accessibilityLabel` props
- Status messages use `accessibilityRole="alert"`
- Proper focus management for screen readers
- High contrast colors for visibility
- Touch targets meet minimum size requirements (44x44 points)
- Semantic HTML equivalents using React Native accessibility props

## API Integration

The app uses Google Gemini API with:
- **gemini-2.5-flash** for standard chat
- **gemini-3.1-flash-live-preview** for live voice mode
- API keys are fetched from a remote key pool service

## License

MIT

## Credits

Developed by stech-vision team
Leader/Director: Sujan Rai from Nepal Asia
