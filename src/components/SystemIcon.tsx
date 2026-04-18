import React from 'react';
import { Text, TextStyle } from 'react-native';

export type IconName =
  | 'brain'
  | 'lightbulb-on'
  | 'shield-alert'
  | 'youtube'
  | 'file-document'
  | 'sparkles'
  | 'message-plus-outline'
  | 'trash-can-outline'
  | 'account'
  | 'robot'
  | 'check'
  | 'content-copy'
  | 'pencil'
  | 'download'
  | 'web'
  | 'close'
  | 'microphone'
  | 'stop'
  | 'loading'
  | 'plus'
  | 'upload'
  | 'send'
  | 'stop-circle'
  | 'radio'
  | 'microphone-off';

const ICON_MAP: Record<IconName, string> = {
  brain: '🧠',
  'lightbulb-on': '💡',
  'shield-alert': '🛡️',
  youtube: '▶️',
  'file-document': '📄',
  sparkles: '✨',
  'message-plus-outline': '➕',
  'trash-can-outline': '🗑️',
  account: '👤',
  robot: '🤖',
  check: '✅',
  'content-copy': '📋',
  pencil: '✏️',
  download: '⬇️',
  web: '🌐',
  close: '✕',
  microphone: '🎤',
  stop: '⏹️',
  loading: '⏳',
  plus: '＋',
  upload: '⤴️',
  send: '📤',
  'stop-circle': '⏹️',
  radio: '📡',
  'microphone-off': '🔇',
};

interface SystemIconProps {
  name: IconName;
  size?: number;
  color?: string;
  style?: TextStyle;
}

export const SystemIcon: React.FC<SystemIconProps> = ({ name, size = 16, color = '#9CA3AF', style }) => {
  return (
    <Text
      accessibilityElementsHidden
      importantForAccessibility="no"
      style={[{ fontSize: size, color, lineHeight: size + 2 }, style]}
    >
      {ICON_MAP[name] ?? '•'}
    </Text>
  );
};
