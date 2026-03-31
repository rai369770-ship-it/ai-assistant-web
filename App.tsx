import React, { useState, useRef, useEffect } from 'react';
import { ChatMessage, GroundingChunk, AttachedFile } from './types';
import { streamGeminiResponse } from './services/api';
import { MessageBubble } from './components/MessageBubble';
import { ChatInput, ChatInputHandle } from './components/ChatInput';
import { Sparkles, BrainCircuit, MessageSquarePlus, Lightbulb, ShieldAlert, Youtube, FileText, Trash2 } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

const SIMPLE_PROMPTS = [
  { text: "Explain quantum computing principles simply.", icon: Lightbulb },
  { text: "Analyze the attached code for security vulnerabilities.", icon: ShieldAlert },
  { text: "Summarize the key takeaways from a YouTube video.", icon: Youtube },
  { text: "Transcribe this video in the original language without timestamps.", icon: FileText }
];

const App: React.FC = () => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [alertMessage, setAlertMessage] = useState<string>('');
  const [statusMessage, setStatusMessage] = useState<string>('');
  const [selectedPrompt, setSelectedPrompt] = useState<string>('');
  const [activeFile, setActiveFile] = useState<AttachedFile | null>(null);
  const [isLiveActive, setIsLiveActive] = useState(false);
  
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const chatInputRef = useRef<ChatInputHandle>(null);

  useEffect(() => {
    const handleGlobalKeyDown = (e: KeyboardEvent) => {
      // Alt + / : Focus Input
      if (e.altKey && e.key === '/') {
        e.preventDefault();
        chatInputRef.current?.focus();
      }
      // Alt + N : New Chat
      if (e.altKey && e.key.toLowerCase() === 'n') {
        e.preventDefault();
        handleNewChat();
      }
    };
    window.addEventListener('keydown', handleGlobalKeyDown);
    return () => window.removeEventListener('keydown', handleGlobalKeyDown);
  }, [messages.length]); 

  const showAlert = (msg: string) => {
    setAlertMessage(msg);
    setTimeout(() => setAlertMessage(''), 3000);
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const handleNewChat = () => {
    if (messages.length > 0) {
      if (window.confirm("Start new chat? Current history will be cleared.")) {
        setMessages([]);
        setActiveFile(null);
        showAlert("Started a new chat");
        chatInputRef.current?.focus();
      }
    } else {
       chatInputRef.current?.focus();
    }
  };

  const handleEditPrompt = (text: string) => {
    setSelectedPrompt(text);
  };

  const handleSendMessage = async (content: string, file: File | null, youtubeLinks: string[], isUrlUnderstanding: boolean) => {
    setIsLoading(true);
    setStatusMessage("AI is thinking...");

    // File uploaded only for this request if provided
    const requestFile = file ? {
      file: file,
      mimeType: file.type || 'application/octet-stream',
      name: file.name
    } : null;

    const newUserMessage: ChatMessage = { role: 'user', content, isComplete: true };
    const updatedMessages = [...messages, newUserMessage];
    
    setMessages(prev => [...prev, newUserMessage]);
    setMessages(prev => [...prev, { role: 'assistant', content: '', isComplete: false }]);

    await streamGeminiResponse(
      updatedMessages,
      requestFile,
      youtubeLinks,
      isUrlUnderstanding,
      'gemini-2.5-flash',
      (content, groundingChunks) => {
        setMessages(prev => {
          const newHistory = [...prev];
          const lastMsg = newHistory[newHistory.length - 1];
          if (lastMsg.role === 'assistant') {
            lastMsg.content = content;
            if (groundingChunks) lastMsg.groundingChunks = groundingChunks;
          }
          return newHistory;
        });
      },
      () => {
        setIsLoading(false);
        setStatusMessage("Ready");
        setMessages(prev => {
          const newHistory = [...prev];
          const lastMsg = newHistory[newHistory.length - 1];
          if (lastMsg.role === 'assistant') {
            lastMsg.isComplete = true;
          }
          return newHistory;
        });
      },
      (error) => {
        setIsLoading(false);
        setStatusMessage("Error");
        setMessages(prev => {
          const newHistory = [...prev];
          const lastMsg = newHistory[newHistory.length - 1];
          if (lastMsg.role === 'assistant') {
              lastMsg.content += `\n\n**Error:** ${error.message}`;
              lastMsg.isComplete = true; 
          }
          return newHistory;
        });
      }
    );
  };

  const handleLiveTranscription = (text: string, role: 'user' | 'assistant', isComplete?: boolean) => {
    setMessages(prev => {
      const lastMsg = prev[prev.length - 1];
      if (lastMsg && lastMsg.role === role && !lastMsg.isComplete) {
        // Update existing live message
        const newHistory = [...prev];
        newHistory[newHistory.length - 1] = { ...lastMsg, content: lastMsg.content + text, isComplete: isComplete || false };
        return newHistory;
      } else {
        // Start new live message only if there is text
        if (text) {
          return [...prev, { role, content: text, isComplete: isComplete || false }];
        }
        return prev;
      }
    });
  };

  const handleLiveComplete = () => {
    setMessages(prev => {
      const newHistory = [...prev];
      if (newHistory.length > 0) {
        newHistory[newHistory.length - 1] = { ...newHistory[newHistory.length - 1], isComplete: true };
      }
      return newHistory;
    });
  };

  return (
    <div className="flex flex-col h-screen bg-background text-gray-100 font-sans selection:bg-primary/30">
      <AnimatePresence>
        {alertMessage && (
          <motion.div 
            initial={{ opacity: 0, y: -20, x: "-50%" }}
            animate={{ opacity: 1, y: 0, x: "-50%" }}
            exit={{ opacity: 0, y: -20, x: "-50%" }}
            className="fixed top-20 left-1/2 bg-gray-900/90 text-white px-6 py-2 rounded-full text-sm font-medium backdrop-blur-sm border border-gray-700 z-50 shadow-xl"
          >
            {alertMessage}
          </motion.div>
        )}
      </AnimatePresence>

      <header className="flex-shrink-0 h-16 border-b border-gray-700/50 bg-surface/50 backdrop-blur-md sticky top-0 z-10 flex items-center px-4 md:px-8 justify-between">
        <h1 className="font-bold text-lg tracking-tight text-white flex items-center gap-2">
          <Sparkles size={18} className="text-primary" />
          AI assistant
        </h1>
        <div className="flex items-center gap-2">
          <motion.button 
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            onClick={handleNewChat} 
            title="New Chat (Alt+N)"
            className="flex items-center gap-2 px-4 py-2 rounded-lg bg-primary hover:bg-blue-600 shadow-lg shadow-blue-500/20 text-xs font-bold text-white transition-colors border border-transparent"
          >
            <MessageSquarePlus size={16} />
            <span>Start New Chat</span>
          </motion.button>
          {messages.length > 0 && (
            <motion.button 
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={() => { if(window.confirm("Clear all messages?")) setMessages([]); }} 
              title="Clear History"
              className="flex items-center gap-2 px-3 py-2 rounded-lg bg-surface hover:bg-red-500/20 text-gray-400 hover:text-red-400 transition-colors border border-gray-700"
            >
              <Trash2 size={16} />
            </motion.button>
          )}
        </div>
      </header>

      <main className="flex-1 overflow-y-auto px-4 py-6 md:px-8 scroll-smooth">
        <div className="max-w-4xl mx-auto min-h-full flex flex-col">
          <AnimatePresence mode="wait">
            {messages.length === 0 && !isLiveActive ? (
              <motion.div 
                key="empty-state"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                transition={{ duration: 0.5 }}
                className="flex-1 flex flex-col items-center justify-center pb-10"
              >
                <motion.div 
                  initial={{ scale: 0.8, opacity: 0 }}
                  animate={{ scale: 1, opacity: 1 }}
                  transition={{ delay: 0.2, type: "spring" }}
                  className="w-16 h-16 rounded-2xl bg-surface border border-gray-700 flex items-center justify-center mb-6 shadow-xl"
                >
                  <BrainCircuit size={32} className="text-gray-400" />
                </motion.div>
                <motion.h2 
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  transition={{ delay: 0.3 }}
                  className="text-2xl font-bold text-gray-200 mb-2"
                >
                  How can I help you?
                </motion.h2>
                <motion.p 
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  transition={{ delay: 0.4 }}
                  className="text-gray-400 mb-8 text-center max-w-md text-sm"
                >
                  I can summarize YouTube videos, analyze files, and browse the web with URL Understanding.
                </motion.p>
                <motion.div 
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.5, staggerChildren: 0.1 }}
                  className="grid grid-cols-1 md:grid-cols-2 gap-3 w-full max-w-2xl"
                >
                  {SIMPLE_PROMPTS.map((prompt, i) => {
                    const Icon = prompt.icon;
                    return (
                      <motion.button 
                        key={i} 
                        whileHover={{ scale: 1.02, backgroundColor: "rgba(30, 41, 59, 1)" }}
                        whileTap={{ scale: 0.98 }}
                        onClick={() => setSelectedPrompt(prompt.text)} 
                        className="group flex items-center gap-3 text-left p-4 rounded-xl bg-surface/30 border border-gray-700/50 hover:border-primary/40 transition-colors duration-200"
                      >
                        <div className="p-2 rounded-lg bg-background border border-gray-700 group-hover:border-primary/50 group-hover:text-primary transition-colors">
                          <Icon size={18} className="text-gray-400 group-hover:text-primary transition-colors" />
                        </div>
                        <span className="text-sm font-medium text-gray-300 group-hover:text-white">{prompt.text}</span>
                      </motion.button>
                    );
                  })}
                </motion.div>
              </motion.div>
            ) : (
              <motion.div 
                key="chat-messages"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                className="flex flex-col gap-2"
              >
                {messages.map((msg, idx) => (
                  <MessageBubble 
                    key={idx} 
                    message={msg} 
                    onEdit={msg.role === 'user' ? handleEditPrompt : undefined}
                  />
                ))}
              </motion.div>
            )}
          </AnimatePresence>
          <div ref={messagesEndRef} />
        </div>
      </main>

      <div className="flex-shrink-0 bg-surface border-t border-gray-700/50">
        <ChatInput 
          ref={chatInputRef}
          onSendMessage={handleSendMessage} 
          isLoading={isLoading} 
          prefilledText={selectedPrompt}
          onClearPrefill={() => setSelectedPrompt('')}
          statusMessage={statusMessage}
          onLiveTranscription={handleLiveTranscription}
          onLiveComplete={handleLiveComplete}
          onLiveStateChange={setIsLiveActive}
        />
      </div>
    </div>
  );
};

export default App;