import { Tool, CategoryGroup } from '../types';

export const toolsData: Tool[] = [
  // AI Tools
  {
    id: 'stoolkit-ai',
    name: 'SToolkit AI',
    description: 'Multi-model native AI for writing, coding, and planning anything.',
    category: 'AI Tools',
  },
  {
    id: 'auto-tts',
    name: 'Auto TTS',
    description: 'Multilingual text-to-speech with automatic language detection and synthesis.',
    category: 'AI Tools',
  },
  {
    id: 'audio-video-transcriber',
    name: 'Audio & Video Transcriber',
    description: 'Transcribe your audio and video files accurately using AI.',
    category: 'AI Tools',
  },
  {
    id: 'youtube-toolkit',
    name: 'YouTube Toolkit',
    description: 'Download, summarize, and transcribe YouTube videos effortlessly.',
    category: 'AI Tools',
  },
  {
    id: 'ai-image-generator',
    name: 'AI Image Generator & Editor',
    description: 'Generate and edit stunning images using advanced AI technology.',
    category: 'AI Tools',
  },
  {
    id: 'ai-image-analysis',
    name: 'AI Image Analysis',
    description: 'Analyze images with AI to extract insights and information.',
    category: 'AI Tools',
  },
  {
    id: 'ai-personal-assistant',
    name: 'AI Personal Assistant',
    description: 'Create custom AI assistants to chat, talk, and complete tasks.',
    category: 'AI Tools',
  },
  {
    id: 'ai-future-predictor',
    name: 'AI Future Predictor',
    description: 'Explore AI-powered predictions and insights about future trends.',
    category: 'AI Tools',
  },
  // Audio Tools
  {
    id: 'text-to-speech',
    name: 'Text to Speech Converter',
    description: 'Multilingual TTS synthesizer with local and online engine support.',
    category: 'Audio Tools',
  },
  {
    id: 'voice-recorder',
    name: 'Voice Recorder',
    description: 'Record your voice with customizable quality and format options.',
    category: 'Audio Tools',
  },
  {
    id: 'media-player',
    name: 'Media Player',
    description: 'Play and listen to your favorite audio and video files.',
    category: 'Audio Tools',
  },
  // Productivity Tools
  {
    id: 'pdf-toolkit',
    name: 'PDF Toolkit',
    description: 'Create, read, and organize PDF documents with ease.',
    category: 'Productivity Tools',
  },
  {
    id: 'notepad',
    name: 'Notepad',
    description: 'Write, edit, and manage your notes efficiently.',
    category: 'Productivity Tools',
  },
  {
    id: 'reminder',
    name: 'Reminder',
    description: 'Set and manage reminders to stay organized and on track.',
    category: 'Productivity Tools',
  },
  // Video Tools
  {
    id: 'screen-recorder',
    name: 'Screen Recorder',
    description: 'Record your screen with customizable settings and options.',
    category: 'Video Tools',
  },
  // Image Tools
  {
    id: 'camera',
    name: 'Camera',
    description: 'Capture photos and videos with advanced camera features.',
    category: 'Image Tools',
  },
  {
    id: 'image-toolkit',
    name: 'Image Toolkit',
    description: 'Edit, enhance, and transform your images with powerful tools.',
    category: 'Image Tools',
  },
  // Device Tools
  {
    id: 'device-info',
    name: 'Device Info',
    description: 'View detailed information about your device specifications.',
    category: 'Device Tools',
  },
];

export function getToolsByCategory(): CategoryGroup[] {
  const categories: Record<string, Tool[]> = {};
  
  toolsData.forEach((tool) => {
    if (!categories[tool.category]) {
      categories[tool.category] = [];
    }
    categories[tool.category].push(tool);
  });

  return Object.entries(categories).map(([name, tools]) => ({
    name: name as CategoryGroup['name'],
    tools,
  }));
}
