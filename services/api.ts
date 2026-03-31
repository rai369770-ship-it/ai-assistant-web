import { ChatMessage, GroundingChunk, AttachedFile } from '../types';
import { GoogleGenAI, Type } from "@google/genai";

const KEY_POOL_URL = "https://mainsite-kcvz.onrender.com/tafb/key_pool.json?etag=1&n=2&client=key&maxage=1200";

const SYSTEM_INSTRUCTION = `You are an AI assistant developed by stech-vision team.
Your goal is to help the users in their everyday tasks.
your key capabilities:
You can search and browse the web in realtime.
You can browse any url in realtime and extract informations from that like from a web page.
You can remember the current chat context and continue the context with the user.
You can analyze multiple files "Videos, Audios, Images, documents and codes.".
You can generate code and build realtime and multi modal apps.
You are optimized for chat.
You are based on the artificial intelligence.
You are trained with a large datasets by stech-vision team.
the leader / director:
Sujan Rai from Nepal Asia.
the startup company in AI and agentic workflow.
You are a text processing AI model so you can only work with text and attachments. You can not directly generate visuals "images / videos".
Chat with the users in friendly tone and assistant tone.
help the users for everything they need.
Use markdown to respond. Make headings and paragraphs and links. Use contextual emojies. Use casual tone.`;

let cachedKeys: string[] = [];

export async function getApiKeys(): Promise<string[]> {
  if (cachedKeys.length > 0) return cachedKeys;
  try {
    const response = await fetch(KEY_POOL_URL);
    if (!response.ok) throw new Error("Failed to fetch keys");
    const keys = await response.json();
    if (Array.isArray(keys) && keys.length > 0 && typeof keys[0] === 'string') {
      cachedKeys = keys;
      return keys;
    }
    throw new Error("Invalid key format received");
  } catch (error) {
    console.error("Error fetching API keys:", error);
    return [];
  }
}

async function uploadFileToGemini(apiKey: string, file: File): Promise<string> {
  const metaData = {
    file: { display_name: file.name }
  };

  const startUploadRes = await fetch(
    `https://generativelanguage.googleapis.com/upload/v1beta/files?key=${apiKey}`,
    {
      method: 'POST',
      headers: {
        'X-Goog-Upload-Protocol': 'resumable',
        'X-Goog-Upload-Command': 'start',
        'X-Goog-Upload-Header-Content-Length': file.size.toString(),
        'X-Goog-Upload-Header-Content-Type': file.type,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(metaData)
    }
  );

  const uploadUrl = startUploadRes.headers.get('x-goog-upload-url');
  if (!startUploadRes.ok || !uploadUrl) {
    throw new Error(`Failed to initiate upload: ${startUploadRes.statusText}`);
  }

  const uploadRes = await fetch(uploadUrl, {
    method: 'POST',
    headers: {
      'Content-Length': file.size.toString(),
      'X-Goog-Upload-Offset': '0',
      'X-Goog-Upload-Command': 'upload, finalize'
    },
    body: file
  });

  if (!uploadRes.ok) {
    throw new Error(`Failed to upload file bytes: ${uploadRes.statusText}`);
  }

  const fileInfo = await uploadRes.json();
  const fileUri = fileInfo.file.uri;

  let state = fileInfo.file.state;
  while (state === 'PROCESSING') {
    await new Promise(r => setTimeout(r, 1000));
    const checkRes = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/files/${fileInfo.file.name.split('/').pop()}?key=${apiKey}`
    );
    const checkData = await checkRes.json();
    state = checkData.state;
    if (state === 'FAILED') throw new Error("File processing failed on Gemini server.");
  }

  return fileUri;
}

export const transcribeAudio = async (audioFile: File): Promise<string> => {
  const keys = await getApiKeys();
  if (keys.length === 0) throw new Error("No API keys available");

  for (const apiKey of keys) {
    try {
      const fileUri = await uploadFileToGemini(apiKey, audioFile);
      const ai = new GoogleGenAI({ apiKey });
      
      const response = await ai.models.generateContent({
        model: 'gemini-2.5-flash',
        contents: {
          parts: [
            {
              fileData: {
                mimeType: audioFile.type,
                fileUri: fileUri
              }
            },
            {
              text: "Transcribe the uploaded audio in the original audio language accurately. Never type anything except the transcribed text."
            }
          ]
        }
      });
      
      return response.text || "";
    } catch (error) {
      console.warn("Transcription failed with key", apiKey, error);
      continue;
    }
  }
  throw new Error("Transcription failed with all available keys.");
};

export const streamGeminiResponse = async (
  messages: ChatMessage[],
  activeFile: AttachedFile | null,
  youtubeLinks: string[],
  isUrlUnderstanding: boolean,
  modelName: string,
  onUpdate: (content: string, groundingChunks?: GroundingChunk[]) => void,
  onComplete: () => void,
  onError: (error: Error) => void
) => {
  const keys = await getApiKeys();
  if (keys.length === 0) {
    onError(new Error("Unable to retrieve API keys."));
    return;
  }

  let success = false;
  let lastError: Error | null = null;

  for (const apiKey of keys) {
    try {
      const ai = new GoogleGenAI({ apiKey: apiKey });
      
      const contents = messages
        .filter(m => m.role !== 'system')
        .map(m => ({
          role: m.role === 'user' ? 'user' : 'model',
          parts: [{ text: m.content }] as any[]
        }));

      // Find the last user message to attach multimodal data
      const lastUserMsgIndex = contents.map(c => c.role).lastIndexOf('user');
      if (lastUserMsgIndex !== -1) {
        // Attach File if present
        if (activeFile) {
          const fileUri = await uploadFileToGemini(apiKey, activeFile.file);
          contents[lastUserMsgIndex].parts.push({
            fileData: {
              mimeType: activeFile.mimeType,
              fileUri: fileUri
            }
          });
        }
        
        // Attach YouTube Links as fileData
        for (const ytLink of youtubeLinks) {
          contents[lastUserMsgIndex].parts.push({
            fileData: {
              mimeType: 'video/mp4', // YouTube links are treated as video/mp4 URIs in Gemini
              fileUri: ytLink
            }
          });
        }
      }

      const tools: any[] = [{ googleSearch: {} }];
      if (isUrlUnderstanding) {
        tools.push({ url_context: {} });
      }

      const responseStream = await ai.models.generateContentStream({
        model: modelName,
        contents: contents,
        config: {
          systemInstruction: SYSTEM_INSTRUCTION,
          tools: tools,
          maxOutputTokens: 8192,
          temperature: 0.7,
        }
      });

      let fullContent = "";
      let finalGroundingChunks: GroundingChunk[] | undefined;

      for await (const chunk of responseStream) {
        if (chunk.candidates?.[0]?.content?.parts) {
          for (const part of chunk.candidates[0].content.parts) {
            if (part.text) fullContent += part.text;
          }
        }
        if (chunk.candidates?.[0]?.groundingMetadata?.groundingChunks) {
          finalGroundingChunks = chunk.candidates[0].groundingMetadata.groundingChunks as GroundingChunk[];
        }
        onUpdate(fullContent, finalGroundingChunks);
      }

      onComplete();
      success = true;
      break;

    } catch (error: any) {
      console.warn("Key failure:", error.message);
      lastError = error;
      continue;
    }
  }

  if (!success) {
    onError(lastError || new Error("Service unavailable."));
  }
};