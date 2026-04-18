import React, { useState, useRef, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  FlatList,
  SafeAreaView,
  StatusBar,
  Alert,
  AccessibilityInfo,
} from 'react-native';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';
import { ChatMessage, GroundingChunk, AttachedFile } from '../types';
import { streamGeminiResponse } from './services/api';
import { MessageBubble } from './components/MessageBubble';
import { ChatInput, ChatInputHandle } from './components/ChatInput';

const SIMPLE_PROMPTS = [
  { text: "Explain quantum computing principles simply.", icon: "lightbulb-on" },
  { text: "Analyze the attached code for security vulnerabilities.", icon: "shield-alert" },
  { text: "Summarize the key takeaways from a YouTube video.", icon: "youtube" },
  { text: "Transcribe this video in the original language without timestamps.", icon: "file-document" }
];

const App: React.FC = () => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [alertMessage, setAlertMessage] = useState<string>('');
  const [statusMessage, setStatusMessage] = useState<string>('Ready');
  const [selectedPrompt, setSelectedPrompt] = useState<string>('');
  const [activeFile, setActiveFile] = useState<AttachedFile | null>(null);
  const [isLiveActive, setIsLiveActive] = useState(false);
  
  const chatInputRef = useRef<ChatInputHandle>(null);

  const showAlert = (msg: string) => {
    setAlertMessage(msg);
    setTimeout(() => setAlertMessage(''), 3000);
  };

  const handleNewChat = () => {
    if (messages.length > 0) {
      Alert.alert(
        "New Chat",
        "Start new chat? Current history will be cleared.",
        [
          { text: "Cancel", style: "cancel" },
          {
            text: "Yes",
            onPress: () => {
              setMessages([]);
              setActiveFile(null);
              showAlert("Started a new chat");
              chatInputRef.current?.focus();
            }
          }
        ]
      );
    } else {
      chatInputRef.current?.focus();
    }
  };

  const handleClearHistory = () => {
    Alert.alert(
      "Clear History",
      "Clear all messages?",
      [
        { text: "Cancel", style: "cancel" },
        {
          text: "Yes",
          onPress: () => setMessages([])
        }
      ]
    );
  };

  const handleEditPrompt = (text: string) => {
    setSelectedPrompt(text);
  };

  const handleSendMessage = async (content: string, file: any | null, youtubeLinks: string[], isUrlUnderstanding: boolean) => {
    setIsLoading(true);
    setStatusMessage("AI is thinking...");

    const requestFile = file ? {
      uri: file.uri,
      name: file.name,
      mimeType: file.mimeType || 'application/octet-stream',
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
        const newHistory = [...prev];
        newHistory[newHistory.length - 1] = { ...lastMsg, content: lastMsg.content + text, isComplete: isComplete || false };
        return newHistory;
      } else {
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

  const renderEmptyState = () => (
    <View style={styles.emptyState}>
      <View style={styles.emptyIconContainer}>
        <Icon name="brain" size={32} color="#9CA3AF" />
      </View>
      <Text style={styles.emptyTitle}>How can I help you?</Text>
      <Text style={styles.emptySubtitle}>
        I can summarize YouTube videos, analyze files, and browse the web with URL Understanding.
      </Text>
      <View style={styles.promptsGrid}>
        {SIMPLE_PROMPTS.map((prompt, i) => (
          <TouchableOpacity 
            key={i} 
            onPress={() => setSelectedPrompt(prompt.text)} 
            style={styles.promptButton}
            accessibilityLabel={prompt.text}
          >
            <View style={styles.promptIconContainer}>
              <Icon name={prompt.icon} size={18} color="#9CA3AF" />
            </View>
            <Text style={styles.promptText}>{prompt.text}</Text>
          </TouchableOpacity>
        ))}
      </View>
    </View>
  );

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#111827" />
      
      {/* Alert Toast */}
      {alertMessage && (
        <View style={styles.alertToast} accessibilityRole="alert">
          <Text style={styles.alertText}>{alertMessage}</Text>
        </View>
      )}

      {/* Header */}
      <View style={styles.header}>
        <View style={styles.headerLeft}>
          <Icon name="sparkles" size={18} color="#60A5FA" />
          <Text style={styles.headerTitle}>AI assistant</Text>
        </View>
        <View style={styles.headerRight}>
          <TouchableOpacity 
            onPress={handleNewChat} 
            style={styles.newChatButton}
            accessibilityLabel="Start new chat"
          >
            <Icon name="message-plus-outline" size={16} color="#FFFFFF" />
            <Text style={styles.newChatButtonText}>Start New Chat</Text>
          </TouchableOpacity>
          {messages.length > 0 && (
            <TouchableOpacity 
              onPress={handleClearHistory} 
              style={styles.clearButton}
              accessibilityLabel="Clear history"
            >
              <Icon name="trash-can-outline" size={16} color="#9CA3AF" />
            </TouchableOpacity>
          )}
        </View>
      </View>

      {/* Messages List */}
      <FlatList
        data={messages}
        keyExtractor={(_, index) => index.toString()}
        renderItem={({ item, index }) => (
          <MessageBubble 
            message={item} 
            onEdit={item.role === 'user' ? handleEditPrompt : undefined}
          />
        )}
        ListEmptyComponent={renderEmptyState}
        contentContainerStyle={styles.messagesContainer}
        showsVerticalScrollIndicator={false}
        onContentSizeChange={() => {
          // Auto-scroll to bottom when content changes
        }}
        accessibilityLabel="Chat messages"
      />

      {/* Input Area */}
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
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0B0F19',
  },
  alertToast: {
    position: 'absolute',
    top: 60,
    left: '50%',
    transform: [{ translateX: -100 }],
    backgroundColor: 'rgba(17, 24, 39, 0.9)',
    paddingHorizontal: 24,
    paddingVertical: 8,
    borderRadius: 9999,
    borderWidth: 1,
    borderColor: '#374151',
    zIndex: 100,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 8,
  },
  alertText: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '500',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(55, 65, 81, 0.5)',
    backgroundColor: 'rgba(31, 41, 55, 0.5)',
  },
  headerLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#FFFFFF',
    letterSpacing: -0.5,
  },
  headerRight: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  newChatButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: '#3B82F6',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 8,
    shadowColor: '#3B82F6',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.3,
    shadowRadius: 4,
    elevation: 4,
  },
  newChatButtonText: {
    fontSize: 12,
    fontWeight: 'bold',
    color: '#FFFFFF',
  },
  clearButton: {
    padding: 8,
    backgroundColor: '#1F2937',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#374151',
  },
  messagesContainer: {
    flexGrow: 1,
    paddingHorizontal: 16,
    paddingVertical: 24,
  },
  emptyState: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingBottom: 80,
  },
  emptyIconContainer: {
    width: 64,
    height: 64,
    borderRadius: 24,
    backgroundColor: '#1F2937',
    borderWidth: 1,
    borderColor: '#374151',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 24,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 8,
  },
  emptyTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#E5E7EB',
    marginBottom: 8,
    textAlign: 'center',
  },
  emptySubtitle: {
    fontSize: 14,
    color: '#9CA3AF',
    textAlign: 'center',
    marginBottom: 32,
    maxWidth: 320,
    lineHeight: 20,
  },
  promptsGrid: {
    width: '100%',
    maxWidth: 512,
  },
  promptButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: 'rgba(31, 41, 55, 0.3)',
    borderWidth: 1,
    borderColor: 'rgba(55, 65, 81, 0.5)',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
  },
  promptIconContainer: {
    width: 36,
    height: 36,
    borderRadius: 8,
    backgroundColor: '#111827',
    borderWidth: 1,
    borderColor: '#374151',
    justifyContent: 'center',
    alignItems: 'center',
  },
  promptText: {
    flex: 1,
    fontSize: 14,
    fontWeight: '500',
    color: '#D1D5DB',
  },
});

export default App;
