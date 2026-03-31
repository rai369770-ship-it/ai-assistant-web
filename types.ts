export type Role = 'user' | 'assistant' | 'system';

export interface GroundingChunk {
  web?: {
    uri: string;
    title: string;
  };
}

export interface ChatMessage {
  role: Role;
  content: string;
  reasoning_details?: string;
  groundingChunks?: GroundingChunk[];
  isComplete?: boolean;
}

export interface AttachedFile {
  file: File;
  uri?: string;
  mimeType: string;
  name: string;
}

export interface ChatState {
  messages: ChatMessage[];
  isLoading: boolean;
  error: string | null;
  apiKey: string;
}