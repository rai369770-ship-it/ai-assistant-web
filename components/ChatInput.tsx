import React, { useState, useRef, useEffect, useImperativeHandle, forwardRef } from 'react';
import { Send, Loader2, X, Youtube, Globe, Mic, MicOff, Square, Plus, FileUp, Check, Image as ImageIcon, Radio, StopCircle } from 'lucide-react';
import { YouTubeModal } from './YouTubeModal';
import { transcribeAudio, getApiKeys } from '../services/api';
import { motion, AnimatePresence } from 'motion/react';
import { GoogleGenAI, Modality, LiveServerMessage } from "@google/genai";

interface ChatInputProps {
  onSendMessage: (content: string, file: File | null, youtubeLinks: string[], isUrlUnderstanding: boolean) => void;
  isLoading: boolean;
  prefilledText?: string;
  onClearPrefill?: () => void;
  statusMessage?: string;
  onLiveTranscription?: (text: string, role: 'user' | 'assistant', isComplete?: boolean) => void;
  onLiveComplete?: () => void;
  onLiveStateChange?: (isActive: boolean) => void;
}

export interface ChatInputHandle {
  focus: () => void;
}

export const ChatInput = forwardRef<ChatInputHandle, ChatInputProps>(({ 
  onSendMessage, 
  isLoading, 
  prefilledText, 
  onClearPrefill,
  statusMessage,
  onLiveTranscription,
  onLiveComplete,
  onLiveStateChange
}, ref) => {
  const [input, setInput] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [ytLinks, setYtLinks] = useState<string[]>([]);
  const [isUrlUnderstanding, setIsUrlUnderstanding] = useState(false);
  const [isYtModalOpen, setIsYtModalOpen] = useState(false);
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  
  const selectedFileRef = useRef<File | null>(null);
  const ytLinksRef = useRef<string[]>([]);
  const isUrlUnderstandingRef = useRef<boolean>(false);

  useEffect(() => {
    selectedFileRef.current = selectedFile;
    ytLinksRef.current = ytLinks;
    isUrlUnderstandingRef.current = isUrlUnderstanding;
  }, [selectedFile, ytLinks, isUrlUnderstanding]);
  
  // Voice Input State (Standard Transcription)
  const [isRecording, setIsRecording] = useState(false);
  const [isTranscribing, setIsTranscribing] = useState(false);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  
  // Live Mode State
  const [isLiveActive, setIsLiveActive] = useState(false);
  const [isMuted, setIsMuted] = useState(false);
  const [liveStatus, setLiveStatus] = useState<string>('');
  const liveSessionRef = useRef<any>(null);
  const audioContextRef = useRef<AudioContext | null>(null);
  const processorRef = useRef<ScriptProcessorNode | null>(null);
  const sourceRef = useRef<MediaStreamAudioSourceNode | null>(null);
  const audioStreamRef = useRef<MediaStream | null>(null);
  const nextPlayTimeRef = useRef<number>(0);
  const scheduledSourcesRef = useRef<AudioBufferSourceNode[]>([]);
  
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);
  const menuItemsRef = useRef<(HTMLButtonElement | null)[]>([]);

  useImperativeHandle(ref, () => ({
    focus: () => {
      textareaRef.current?.focus();
    }
  }));

  useEffect(() => {
    if (prefilledText) {
      setInput(prefilledText);
      textareaRef.current?.focus();
      if (onClearPrefill) onClearPrefill();
    }
  }, [prefilledText, onClearPrefill]);

  // Live Mode Logic
  const startLiveMode = async () => {
    if (isLiveActive) return;
    
    setIsLiveActive(true);
    onLiveStateChange?.(true);
    setLiveStatus('Requesting microphone...');
    
    let stream: MediaStream;
    let audioContext: AudioContext;
    try {
      stream = await navigator.mediaDevices.getUserMedia({ 
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true
        } 
      });
      audioStreamRef.current = stream;
      
      // Create AudioContext immediately after user gesture
      audioContext = new AudioContext({ sampleRate: 16000 });
      audioContextRef.current = audioContext;
    } catch (err: any) {
      console.error("Microphone access error:", err);
      if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
        alert("Microphone access denied. Please allow microphone access in your browser settings to use live mode.");
      } else {
        alert("Failed to access microphone: " + err.message);
      }
      setIsLiveActive(false);
      onLiveStateChange?.(false);
      setLiveStatus('');
      return;
    }
    
    setLiveStatus('Connecting...');
    
    const keys = await getApiKeys();
    if (keys.length === 0) {
      console.error("No API keys available for live mode.");
      stopAudioStreaming();
      setIsLiveActive(false);
      onLiveStateChange?.(false);
      setLiveStatus('');
      return;
    }

    let connected = false;
    for (const apiKey of keys) {
      try {
        const ai = new GoogleGenAI({ apiKey });
        
        const session = await ai.live.connect({
          model: "gemini-3.1-flash-live-preview",
          config: {
            responseModalities: [Modality.AUDIO],
            speechConfig: {
              voiceConfig: { prebuiltVoiceConfig: { voiceName: "Kore" } },
            },
            systemInstruction: "You're Mero AI assistant web version. You're developed by sujan Rai under builgit project. Your aim is to chat and interact with the users. You can interact naturally like human. You can express emotions like humans. You are Maya AI voice of Mero ai assistant. Behave like humans. Play roles given by users as anything. You will use female gender to interact first. User can ask you to change your gender. Then change. Your primary language is Hindi.\nUse markdown to respond. Make headings and paragraphs and links. Use contextual emojies. Use casual tone.",
            tools: [{ googleSearch: {} }],
            outputAudioTranscription: {},
            inputAudioTranscription: {},
            temperature: 0.7,
            maxOutputTokens: 8192,
          },
          callbacks: {
            onopen: () => {
              setLiveStatus('Live');
              startAudioStreamingWithStream(stream, audioContext);
            },
            onmessage: async (message: any) => {
              // Handle transcriptions
              if (message.serverContent?.outputTranscription) {
                const text = message.serverContent.outputTranscription.text || '';
                const finished = message.serverContent.outputTranscription.finished;
                if (text || finished) {
                  onLiveTranscription(text, 'assistant', finished);
                }
              }
              
              // Handle user transcription
              if (message.serverContent?.inputTranscription) {
                 const text = message.serverContent.inputTranscription.text || '';
                 const finished = message.serverContent.inputTranscription.finished;
                 
                 // Aggressively stop playback when user starts speaking
                 if (text) {
                   stopAudioPlayback();
                 }
                 
                 if (text || finished) {
                   onLiveTranscription(text, 'user', finished);
                 }
              }

              // Handle interruption
              if (message.serverContent?.interrupted) {
                stopAudioPlayback();
              }

              // Handle audio output
              const audioData = message.serverContent?.modelTurn?.parts?.find((p: any) => p.inlineData)?.inlineData?.data;
              if (audioData) {
                playAudioResponse(audioData);
              }
              
              if (message.serverContent?.turnComplete) {
                if (onLiveComplete) onLiveComplete();
              }
            },
            onclose: () => {
              stopLiveMode();
            },
            onerror: (err) => {
              console.error("Live API Error:", err);
              stopLiveMode();
            }
          }
        });
        
        liveSessionRef.current = session;
        connected = true;
        break;
      } catch (err) {
        console.warn(`Failed to connect with key ${apiKey.substring(0, 5)}...`, err);
        continue;
      }
    }

    if (!connected) {
      console.error("Failed to start live mode with all available keys.");
      setIsLiveActive(false);
      setLiveStatus('');
    }
  };

  const stopLiveMode = () => {
    if (liveSessionRef.current) {
      liveSessionRef.current.close();
      liveSessionRef.current = null;
    }
    stopAudioPlayback();
    stopAudioStreaming();
    setIsLiveActive(false);
    setIsMuted(false);
    onLiveStateChange?.(false);
    setLiveStatus('');
    if (onLiveComplete) onLiveComplete();
  };

  const toggleMute = () => {
    if (audioStreamRef.current) {
      const audioTracks = audioStreamRef.current.getAudioTracks();
      if (audioTracks.length > 0) {
        const newMutedState = !isMuted;
        audioTracks[0].enabled = !newMutedState;
        setIsMuted(newMutedState);
      }
    }
  };

  const startAudioStreamingWithStream = async (stream: MediaStream, audioContext: AudioContext) => {
    try {
      const source = audioContext.createMediaStreamSource(stream);
      sourceRef.current = source;
      
      // Using ScriptProcessor with smaller buffer for lower latency
      const processor = audioContext.createScriptProcessor(2048, 1, 1);
      processorRef.current = processor;
      
      processor.onaudioprocess = (e) => {
        if (!liveSessionRef.current) return;
        
        const inputData = e.inputBuffer.getChannelData(0);
        // Convert Float32 to Int16 PCM
        const pcmData = new Int16Array(inputData.length);
        for (let i = 0; i < inputData.length; i++) {
          pcmData[i] = Math.max(-1, Math.min(1, inputData[i])) * 0x7FFF;
        }
        
        const uint8Array = new Uint8Array(pcmData.buffer);
        let binary = '';
        for (let i = 0; i < uint8Array.byteLength; i++) {
          binary += String.fromCharCode(uint8Array[i]);
        }
        const base64Data = btoa(binary);
        
        liveSessionRef.current.sendRealtimeInput({
          audio: { data: base64Data, mimeType: 'audio/pcm;rate=16000' }
        });
      };
      
      // Create a GainNode with 0 volume to prevent microphone feedback loop
      const gainNode = audioContext.createGain();
      gainNode.gain.value = 0;
      
      source.connect(processor);
      processor.connect(gainNode);
      gainNode.connect(audioContext.destination);
    } catch (err: any) {
      console.error("Audio streaming error:", err);
      alert("Failed to start audio streaming: " + err.message);
      stopLiveMode();
    }
  };

  const stopAudioStreaming = () => {
    if (processorRef.current) {
      processorRef.current.disconnect();
      processorRef.current = null;
    }
    if (sourceRef.current) {
      sourceRef.current.disconnect();
      sourceRef.current = null;
    }
    if (audioContextRef.current) {
      audioContextRef.current.close();
      audioContextRef.current = null;
    }
    if (audioStreamRef.current) {
      audioStreamRef.current.getTracks().forEach(t => t.stop());
      audioStreamRef.current = null;
    }
  };

  const playAudioResponse = async (base64Data: string) => {
    if (!audioContextRef.current) return;
    
    const binary = atob(base64Data);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
    
    const pcmData = new Int16Array(bytes.buffer);
    const floatData = new Float32Array(pcmData.length);
    for (let i = 0; i < pcmData.length; i++) floatData[i] = pcmData[i] / 0x7FFF;
    
    const buffer = audioContextRef.current.createBuffer(1, floatData.length, 24000);
    buffer.getChannelData(0).set(floatData);
    
    const source = audioContextRef.current.createBufferSource();
    source.buffer = buffer;
    source.connect(audioContextRef.current.destination);

    const currentTime = audioContextRef.current.currentTime;
    if (nextPlayTimeRef.current < currentTime) {
      nextPlayTimeRef.current = currentTime;
    }
    
    source.start(nextPlayTimeRef.current);
    nextPlayTimeRef.current += buffer.duration;
    
    scheduledSourcesRef.current.push(source);
    source.onended = () => {
      scheduledSourcesRef.current = scheduledSourcesRef.current.filter(s => s !== source);
    };
  };

  const stopAudioPlayback = () => {
    scheduledSourcesRef.current.forEach(source => {
      try {
        source.stop();
        source.disconnect();
      } catch (e) {
        // Ignore errors if already stopped
      }
    });
    scheduledSourcesRef.current = [];
    if (audioContextRef.current) {
      nextPlayTimeRef.current = audioContextRef.current.currentTime;
    }
  };

  // Standard Submit
  const handleSubmit = (e?: React.FormEvent) => {
    e?.preventDefault();
    if (!input.trim() && !selectedFile && ytLinks.length === 0) {
      if (!isLiveActive) startLiveMode();
      return;
    }
    
    onSendMessage(input, selectedFile, ytLinks, isUrlUnderstanding);
    setInput('');
    setSelectedFile(null); 
    setYtLinks([]);
    if (fileInputRef.current) fileInputRef.current.value = '';
    
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }
  };

  const startRecording = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mediaRecorder = new MediaRecorder(stream);
      mediaRecorderRef.current = mediaRecorder;
      chunksRef.current = [];

      mediaRecorder.ondataavailable = (e) => {
        if (e.data.size > 0) {
          chunksRef.current.push(e.data);
        }
      };

      mediaRecorder.onstop = async () => {
        const blob = new Blob(chunksRef.current, { type: 'audio/webm' });
        const file = new File([blob], "voice_input.webm", { type: 'audio/webm' });
        
        // Cleanup stream tracks
        stream.getTracks().forEach(track => track.stop());

        setIsRecording(false);
        setIsTranscribing(true);
        
        try {
          const text = await transcribeAudio(file);
          if (text && text.trim()) {
            onSendMessage(text.trim(), selectedFileRef.current, ytLinksRef.current, isUrlUnderstandingRef.current);
            setSelectedFile(null);
            setYtLinks([]);
            if (fileInputRef.current) fileInputRef.current.value = '';
          }
        } catch (err) {
          console.error("Transcription error:", err);
          alert("Failed to transcribe audio.");
        } finally {
          setIsTranscribing(false);
        }
      };

      mediaRecorder.start();
      setIsRecording(true);
    } catch (err) {
      console.error("Error accessing microphone:", err);
      alert("Microphone access denied or not available.");
    }
  };

  const stopRecording = () => {
    if (mediaRecorderRef.current && isRecording) {
      mediaRecorderRef.current.stop();
    }
  };

  const toggleVoice = () => {
    if (isLoading || isTranscribing) return;
    if (isRecording) stopRecording();
    else startRecording();
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    // Alt + S : Send
    if (e.altKey && e.key.toLowerCase() === 's') {
      e.preventDefault();
      handleSubmit();
      return;
    }
    // Alt + A : Open Add Menu
    if (e.altKey && e.key.toLowerCase() === 'a') {
      e.preventDefault();
      setIsMenuOpen(prev => !prev);
      return;
    }
    // Alt + V : Voice
    if (e.altKey && e.key.toLowerCase() === 'v') {
      e.preventDefault();
      toggleVoice();
      return;
    }
    // Enter to send (without Shift)
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
      setIsMenuOpen(false);
      textareaRef.current?.focus();
    }
  };

  const handleMenuKeyDown = (e: React.KeyboardEvent) => {
     if (e.key === 'Escape') {
         setIsMenuOpen(false);
         textareaRef.current?.focus();
         return;
     }

     const items = menuItemsRef.current.filter(el => el !== null);
     if (items.length === 0) return;

     const currentIndex = items.indexOf(document.activeElement as HTMLButtonElement);
     
     if (e.key === 'ArrowDown') {
         e.preventDefault();
         const nextIndex = (currentIndex + 1) % items.length;
         items[nextIndex]?.focus();
     } else if (e.key === 'ArrowUp') {
         e.preventDefault();
         const prevIndex = (currentIndex - 1 + items.length) % items.length;
         items[prevIndex]?.focus();
     } else if (e.key === 'Tab') {
         e.preventDefault();
         // Do nothing on tab press as requested
     }
  };

  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 200)}px`;
    }
  }, [input]);

  useEffect(() => {
    return () => {
      stopLiveMode();
      stopRecording();
    };
  }, []);

  const isSendDisabled = isLoading || isRecording || isTranscribing;
  const showLiveButton = !input.trim() && !selectedFile && ytLinks.length === 0;

  return (
    <div className="w-full bg-surface border-t border-gray-700/50 px-4 pt-4 pb-6">
      <div role="alert" aria-live="polite" className="sr-only">
        {statusMessage}
        {isRecording && "Recording voice input"}
        {isTranscribing && "Transcribing audio"}
        {isLiveActive && `Live mode active: ${liveStatus}`}
      </div>

      <div className="max-w-3xl mx-auto relative flex flex-col gap-3">
        {/* Attachment Display */}
        <AnimatePresence>
          {(selectedFile || ytLinks.length > 0 || isUrlUnderstanding) && !isLiveActive && (
          <motion.div 
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            className="flex flex-wrap gap-2 mb-1"
          >
            {selectedFile && (
               <motion.div 
                 initial={{ opacity: 0, scale: 0.8 }}
                 animate={{ opacity: 1, scale: 1 }}
                 exit={{ opacity: 0, scale: 0.8 }}
                 className="flex items-center gap-2 bg-background border border-gray-700 rounded-lg px-3 py-2"
               >
                 <div className="flex flex-col">
                    <p className="text-[10px] text-primary font-bold uppercase tracking-wider">File</p>
                    <p className="text-sm text-gray-200 truncate max-w-[150px]">{selectedFile.name}</p>
                 </div>
                 <motion.button 
                   whileHover={{ scale: 1.1 }}
                   whileTap={{ scale: 0.9 }}
                   onClick={() => { setSelectedFile(null); if (fileInputRef.current) fileInputRef.current.value = ''; }} 
                   className="ml-2 flex items-center gap-1 p-1 hover:bg-gray-700 rounded-lg text-gray-400 hover:text-white transition-colors border border-transparent hover:border-gray-600"
                   title="Remove"
                 >
                   <X size={14} />
                 </motion.button>
               </motion.div>
            )}

            {ytLinks.map((link, idx) => (
              <motion.div 
                 key={idx}
                 initial={{ opacity: 0, scale: 0.8 }}
                 animate={{ opacity: 1, scale: 1 }}
                 exit={{ opacity: 0, scale: 0.8 }}
                 className="flex items-center gap-2 bg-background border border-gray-700 rounded-lg px-3 py-2"
              >
                 <div className="flex flex-col">
                    <p className="text-[10px] text-red-500 font-bold uppercase tracking-wider">YouTube</p>
                    <p className="text-sm text-gray-200 truncate max-w-[200px]">{link}</p>
                 </div>
                 <motion.button 
                   whileHover={{ scale: 1.1 }}
                   whileTap={{ scale: 0.9 }}
                   onClick={() => setYtLinks(prev => prev.filter((_, i) => i !== idx))} 
                   className="ml-2 flex items-center gap-1 p-1 hover:bg-gray-700 rounded-lg text-gray-400 hover:text-white transition-colors border border-transparent hover:border-gray-600"
                   title="Remove"
                 >
                   <X size={14} />
                 </motion.button>
               </motion.div>
            ))}

            {isUrlUnderstanding && (
                <motion.div 
                 initial={{ opacity: 0, scale: 0.8 }}
                 animate={{ opacity: 1, scale: 1 }}
                 exit={{ opacity: 0, scale: 0.8 }}
                 className="flex items-center gap-2 bg-blue-500/10 border border-blue-500/30 rounded-lg px-3 py-2"
                >
                 <div className="flex flex-col">
                    <p className="text-[10px] text-blue-400 font-bold uppercase tracking-wider">Mode</p>
                    <p className="text-sm text-gray-200">URL Understanding</p>
                 </div>
                 <motion.button 
                   whileHover={{ scale: 1.1 }}
                   whileTap={{ scale: 0.9 }}
                   onClick={() => setIsUrlUnderstanding(false)} 
                   className="ml-2 flex items-center gap-1 p-1 hover:bg-blue-500/20 rounded-lg text-blue-400 hover:text-white transition-colors border border-transparent"
                   title="Disable"
                 >
                   <X size={14} />
                 </motion.button>
               </motion.div>
            )}
          </motion.div>
          )}
        </AnimatePresence>

        <form onSubmit={handleSubmit} className="relative flex flex-col gap-3">
          {/* Input Area */}
          {!isLiveActive && (
            <div className="relative bg-background rounded-xl border border-gray-700/50 focus-within:border-primary/50 focus-within:ring-1 focus-within:ring-primary/20 transition-all shadow-lg p-2">
               <textarea
                  ref={textareaRef}
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  onKeyDown={handleKeyDown}
                  placeholder={isRecording ? "Listening..." : "Ask anything..."}
                  disabled={isRecording || isTranscribing}
                  className={`w-full bg-transparent text-gray-100 placeholder-gray-500 resize-none outline-none p-2 min-h-[50px] max-h-[200px] text-sm md:text-base scrollbar-hide ${isRecording ? 'animate-pulse text-red-400' : ''}`}
                  rows={1}
              />
            </div>
          )}
          
          <input type="file" ref={fileInputRef} onChange={handleFileChange} className="hidden" accept="image/*,video/*,audio/*,.pdf,.txt,.md,.js,.ts,.py,.java,.c,.cpp,.html,.css,.json,.csv" />

          {/* Toolbar */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
               {!isLiveActive && (
                 <>
                   {/* Voice Button */}
                   <motion.button
                     whileHover={{ scale: 1.05 }}
                     whileTap={{ scale: 0.95 }}
                     type="button"
                     onClick={toggleVoice}
                     disabled={isLoading || isTranscribing}
                     className={`flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${isRecording ? 'bg-red-500/10 text-red-500 border border-red-500/50' : 'bg-surface hover:bg-gray-800 text-gray-400 hover:text-white border border-transparent'}`}
                     title="Toggle Voice (Alt+V)"
                   >
                     {isTranscribing ? <Loader2 size={18} className="animate-spin" /> : isRecording ? <Square size={18} fill="currentColor" /> : <Mic size={18} />}
                     <span className="hidden sm:inline">{isRecording ? 'Stop' : 'Voice'}</span>
                   </motion.button>

                   {/* Add Menu */}
                   <div className="relative" ref={menuRef}>
                      <motion.button
                        whileHover={{ scale: 1.05 }}
                        whileTap={{ scale: 0.95 }}
                        type="button"
                        onClick={() => setIsMenuOpen(!isMenuOpen)}
                        disabled={isRecording}
                        className={`flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${isMenuOpen ? 'bg-gray-800 text-white' : 'bg-surface hover:bg-gray-800 text-gray-400 hover:text-white'} border border-transparent`}
                        title="Add content (Alt+A)"
                        aria-haspopup="true"
                        aria-expanded={isMenuOpen}
                        aria-controls="add-menu-dropdown"
                      >
                        <Plus size={18} />
                        <span>Add</span>
                      </motion.button>
                      
                      <AnimatePresence>
                        {isMenuOpen && (
                          <motion.div 
                              initial={{ opacity: 0, y: 10, scale: 0.95 }}
                              animate={{ opacity: 1, y: 0, scale: 1 }}
                              exit={{ opacity: 0, y: 10, scale: 0.95 }}
                              transition={{ duration: 0.15 }}
                              id="add-menu-dropdown"
                              role="menu" 
                              className="absolute bottom-full left-0 mb-2 w-72 bg-gray-800 rounded-xl border border-gray-700 shadow-xl overflow-hidden z-50 flex flex-col p-1"
                              onKeyDown={handleMenuKeyDown}
                          >
                              <button 
                                  ref={el => { menuItemsRef.current[0] = el; }}
                                  role="menuitem"
                                  tabIndex={-1}
                                  type="button"
                                  onClick={() => { 
                                      fileInputRef.current?.click(); 
                                      setIsMenuOpen(false); 
                                      textareaRef.current?.focus(); 
                                  }}
                                  className="flex items-center gap-3 w-full px-3 py-2.5 text-left text-sm text-gray-200 hover:bg-gray-700 rounded-lg transition-colors focus:outline-none focus:bg-gray-700"
                              >
                                  <FileUp size={16} className="text-blue-400" />
                                  <span>Upload Files</span>
                              </button>
                              <button 
                                  ref={el => { menuItemsRef.current[1] = el; }}
                                  role="menuitem"
                                  tabIndex={-1}
                                  type="button"
                                  onClick={() => { 
                                      setIsYtModalOpen(true); 
                                      setIsMenuOpen(false); 
                                      // Modal handles its own focus, then returns focus to input on close
                                  }}
                                  className="flex items-center gap-3 w-full px-3 py-2.5 text-left text-sm text-gray-200 hover:bg-gray-700 rounded-lg transition-colors focus:outline-none focus:bg-gray-700"
                              >
                                  <Youtube size={16} className="text-red-500" />
                                  <span>YouTube Link</span>
                              </button>
                              <div className="h-px bg-gray-700 my-1 mx-2" role="separator" />
                              <button 
                                  ref={el => { menuItemsRef.current[2] = el; }}
                                  role="menuitemcheckbox"
                                  aria-checked={isUrlUnderstanding}
                                  tabIndex={-1}
                                  type="button"
                                  onClick={() => { 
                                      setIsUrlUnderstanding(!isUrlUnderstanding); 
                                      setIsMenuOpen(false); 
                                      textareaRef.current?.focus(); 
                                  }}
                                  className="flex items-center justify-between w-full px-3 py-2.5 text-left text-sm text-gray-200 hover:bg-gray-700 rounded-lg transition-colors focus:outline-none focus:bg-gray-700"
                              >
                                  <div className="flex items-center gap-3">
                                      <Globe size={16} className={isUrlUnderstanding ? "text-blue-400" : "text-gray-400"} />
                                      <span>URL Understanding</span>
                                  </div>
                                  {isUrlUnderstanding && <Check size={14} className="text-blue-400" />}
                              </button>
                          </motion.div>
                        )}
                      </AnimatePresence>
                   </div>
                 </>
               )}
               
               {isLiveActive && (
                 <div className="flex items-center gap-3 px-3 py-2 bg-emerald-500/10 border border-emerald-500/30 rounded-lg">
                    <div className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse" />
                    <span className="text-xs font-bold text-emerald-400 uppercase tracking-widest">{liveStatus}</span>
                 </div>
               )}
            </div>

            <div className="flex flex-col gap-2 items-end">
              {isLiveActive && (
                <motion.button
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  type="button"
                  onClick={toggleMute}
                  className={`flex items-center gap-2 px-4 py-1.5 rounded-lg font-bold text-xs transition-colors ${isMuted ? 'bg-orange-600 text-white hover:bg-orange-500 shadow-lg shadow-orange-500/20' : 'bg-gray-700 text-gray-200 hover:bg-gray-600'}`}
                >
                  {isMuted ? <MicOff size={14} /> : <Mic size={14} />}
                  <span>{isMuted ? "Unmute" : "Mute"}</span>
                </motion.button>
              )}
              <motion.button
                  whileHover={!isSendDisabled ? { scale: 1.05 } : {}}
                  whileTap={!isSendDisabled ? { scale: 0.95 } : {}}
                  type="submit"
                  onClick={() => isLiveActive ? stopLiveMode() : handleSubmit()}
                  disabled={isSendDisabled}
                  title={isLiveActive ? "Stop Voice" : showLiveButton ? "Start Live" : "Send"}
                  className={`flex items-center gap-2 px-6 py-2 rounded-lg font-bold text-sm transition-colors ${isSendDisabled ? 'bg-gray-800 text-gray-500 cursor-not-allowed' : isLiveActive ? 'bg-red-600 text-white hover:bg-red-500 shadow-lg shadow-red-500/20' : showLiveButton ? 'bg-emerald-600 text-white hover:bg-emerald-500 shadow-lg shadow-emerald-500/20' : 'bg-primary text-white hover:bg-blue-600 shadow-lg shadow-blue-500/20'}`}
              >
                  {isLoading ? <Loader2 size={18} className="animate-spin" /> : isLiveActive ? <StopCircle size={18} /> : showLiveButton ? <Radio size={18} /> : <Send size={18} />}
                  <span>{isLiveActive ? "Stop Voice" : showLiveButton ? "Start Live" : "Send"}</span>
              </motion.button>
            </div>
          </div>
        </form>
      </div>

      <YouTubeModal 
        isOpen={isYtModalOpen} 
        onClose={() => { setIsYtModalOpen(false); textareaRef.current?.focus(); }} 
        onAdd={(url) => setYtLinks(prev => [...prev, url])} 
      />
    </div>
  );
});

ChatInput.displayName = 'ChatInput';