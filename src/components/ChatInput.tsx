import React, { useState, useRef, useEffect, useImperativeHandle, forwardRef } from 'react';
import {
  View,
  TextInput,
  TouchableOpacity,
  Text,
  StyleSheet,
  ActivityIndicator,
  Modal,
  FlatList,
  Alert,
  AccessibilityInfo,
  findNodeHandle,
  Platform,
  PermissionsAndroid,
} from 'react-native';
import { SystemIcon } from './SystemIcon';
import { streamGeminiResponse, transcribeAudio, getApiKeys } from '../services/api';
import { GoogleGenAI, Modality } from "@google/genai";

type VoiceModule = {
  onSpeechStart?: () => void;
  onSpeechEnd?: () => void;
  onSpeechResults?: (e: { value?: string[] }) => void;
  onSpeechError?: (e: unknown) => void;
  start: (locale: string) => Promise<void>;
  stop: () => Promise<void>;
  destroy: () => Promise<void>;
  removeAllListeners: () => void;
};

const getVoiceModule = (): VoiceModule | null => {
  try {
    const pkg = require('@react-native-voice/voice');
    return pkg?.default ?? pkg;
  } catch (error) {
    return null;
  }
};

const getDocumentPickerModule = () => {
  try {
    return require('@react-native-documents/picker');
  } catch (error) {
    return null;
  }
};

interface ChatInputProps {
  onSendMessage: (content: string, file: any | null, youtubeLinks: string[], isUrlUnderstanding: boolean) => void;
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
  const [selectedFile, setSelectedFile] = useState<any | null>(null);
  const [ytLinks, setYtLinks] = useState<string[]>([]);
  const [isUrlUnderstanding, setIsUrlUnderstanding] = useState(false);
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  
  // Voice Input State
  const [isRecording, setIsRecording] = useState(false);
  const [isTranscribing, setIsTranscribing] = useState(false);
  
  // Live Mode State
  const [isLiveActive, setIsLiveActive] = useState(false);
  const [isMuted, setIsMuted] = useState(false);
  const [liveStatus, setLiveStatus] = useState<string>('');
  const liveSessionRef = useRef<any>(null);
  
  const inputRef = useRef<TextInput>(null);

  useImperativeHandle(ref, () => ({
    focus: () => {
      inputRef.current?.focus();
    }
  }));

  useEffect(() => {
    if (prefilledText) {
      setInput(prefilledText);
      inputRef.current?.focus();
      if (onClearPrefill) onClearPrefill();
    }
  }, [prefilledText, onClearPrefill]);

  // Setup Voice recognition
  useEffect(() => {
    const voice = getVoiceModule();
    if (!voice) {
      return;
    }

    voice.onSpeechStart = () => {
      setIsRecording(true);
    };
    voice.onSpeechEnd = () => {
      setIsRecording(false);
    };
    voice.onSpeechResults = async (e) => {
      if (e.value && e.value[0]) {
        setIsRecording(false);
        setIsTranscribing(true);
        try {
          onSendMessage(e.value[0], selectedFile, ytLinks, isUrlUnderstanding);
          setInput('');
        } catch (err) {
          Alert.alert('Error', 'Failed to send voice message');
        } finally {
          setIsTranscribing(false);
        }
      }
    };
    voice.onSpeechError = (e) => {
      console.error('Voice error:', e);
      setIsRecording(false);
      Alert.alert('Error', 'Voice recognition failed');
    };

    return () => {
      void voice.destroy().then(() => voice.removeAllListeners());
    };
  }, [selectedFile, ytLinks, isUrlUnderstanding]);

  const requestAudioPermission = async (): Promise<boolean> => {
    if (Platform.OS !== 'android') return true;
    const result = await PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.RECORD_AUDIO);
    return result === PermissionsAndroid.RESULTS.GRANTED;
  };

  const requestStoragePermission = async (): Promise<boolean> => {
    if (Platform.OS !== 'android') return true;

    if (Platform.Version >= 33) {
      const result = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.READ_MEDIA_IMAGES
      );
      return result === PermissionsAndroid.RESULTS.GRANTED;
    }

    const permission =
      Platform.Version <= 32
        ? PermissionsAndroid.PERMISSIONS.READ_EXTERNAL_STORAGE
        : PermissionsAndroid.PERMISSIONS.READ_MEDIA_IMAGES;
    const result = await PermissionsAndroid.request(permission);
    return result === PermissionsAndroid.RESULTS.GRANTED;
  };

  // Live Mode Logic
  const startLiveMode = async () => {
    if (isLiveActive) return;
    
    setIsLiveActive(true);
    onLiveStateChange?.(true);
    setLiveStatus('Requesting microphone...');
    
    try {
      const keys = await getApiKeys();
      if (keys.length === 0) {
        Alert.alert('Error', 'No API keys available');
        setIsLiveActive(false);
        onLiveStateChange?.(false);
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
              },
              onmessage: async (message: any) => {
                if (message.serverContent?.outputTranscription) {
                  const text = message.serverContent.outputTranscription.text || '';
                  const finished = message.serverContent.outputTranscription.finished;
                  if (text || finished) {
                    onLiveTranscription?.(text, 'assistant', finished);
                  }
                }
                
                if (message.serverContent?.inputTranscription) {
                   const text = message.serverContent.inputTranscription.text || '';
                   const finished = message.serverContent.inputTranscription.finished;
                   if (text || finished) {
                     onLiveTranscription?.(text, 'user', finished);
                   }
                }

                if (message.serverContent?.turnComplete) {
                  onLiveComplete?.();
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
          console.warn(`Failed to connect with key`, err);
          continue;
        }
      }

      if (!connected) {
        setIsLiveActive(false);
        onLiveStateChange?.(false);
        setLiveStatus('');
      }
    } catch (err: any) {
      console.error("Live mode error:", err);
      setIsLiveActive(false);
      onLiveStateChange?.(false);
      setLiveStatus('');
    }
  };

  const stopLiveMode = () => {
    if (liveSessionRef.current) {
      liveSessionRef.current.close();
      liveSessionRef.current = null;
    }
    setIsLiveActive(false);
    setIsMuted(false);
    onLiveStateChange?.(false);
    setLiveStatus('');
    onLiveComplete?.();
  };

  const toggleVoice = async () => {
    if (isLoading || isTranscribing) return;
    const voice = getVoiceModule();
    if (!voice) {
      Alert.alert('Unsupported', 'Voice module is unavailable in this build.');
      return;
    }

    const hasPermission = await requestAudioPermission();
    if (!hasPermission) {
      Alert.alert('Permission needed', 'Microphone permission is required for voice input.');
      return;
    }

    if (isRecording) {
      await voice.stop();
    } else {
      try {
        await voice.start('en-US');
      } catch (err) {
        Alert.alert('Error', 'Microphone access denied');
      }
    }
  };

  const handleSubmit = () => {
    if (!input.trim() && !selectedFile && ytLinks.length === 0) {
      if (!isLiveActive) startLiveMode();
      return;
    }
    
    onSendMessage(input, selectedFile, ytLinks, isUrlUnderstanding);
    setInput('');
    setSelectedFile(null);
    setYtLinks([]);
  };

  const pickFile = async () => {
    const picker = getDocumentPickerModule();
    if (!picker) {
      Alert.alert('Unsupported', 'File picker module is unavailable in this build.');
      return;
    }

    const hasPermission = await requestStoragePermission();
    if (!hasPermission) {
      Alert.alert('Permission needed', 'Storage permission is required to select files.');
      return;
    }

    try {
      const [result] = await picker.pick({
        type: [picker.types.allFiles],
      });
      setSelectedFile({
        uri: result.uri,
        name: result.name,
        mimeType: result.type || 'application/octet-stream',
      });
      setIsMenuOpen(false);
      inputRef.current?.focus();
    } catch (err: unknown) {
      const pickerError = err as { code?: string };
      if (!picker.isErrorWithCode(err) || pickerError.code !== picker.errorCodes.OPERATION_CANCELED) {
        Alert.alert('Error', 'Failed to pick file');
      }
    }
  };

  const addYouTubeLink = () => {
    if (Platform.OS !== 'ios') {
      Alert.alert('Add YouTube Link', 'Enter the link directly in chat input for now.');
      setIsMenuOpen(false);
      return;
    }

    Alert.prompt(
      'Add YouTube Link',
      'Enter a YouTube video URL to summarize or transcribe:',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Add',
          onPress: (url) => {
            if (url && url.startsWith('https://')) {
              setYtLinks(prev => [...prev, url]);
            } else {
              Alert.alert('Error', 'Invalid URL. Must start with https://');
            }
          }
        }
      ]
    );
    setIsMenuOpen(false);
  };

  const handleKeyDown = () => {
    // Mobile doesn't have keyboard shortcuts like desktop
  };

  const isSendDisabled = isLoading || isRecording || isTranscribing;
  const showLiveButton = !input.trim() && !selectedFile && ytLinks.length === 0;

  return (
    <View style={styles.container}>
      {/* Status Message for Accessibility */}
      <Text accessibilityRole="alert" style={styles.srOnly}>
        {statusMessage}
        {isRecording && " Recording voice input"}
        {isTranscribing && " Transcribing audio"}
        {isLiveActive && ` Live mode active: ${liveStatus}`}
      </Text>

      <View style={styles.contentContainer}>
        {/* Attachment Display */}
        {(selectedFile || ytLinks.length > 0 || isUrlUnderstanding) && !isLiveActive && (
          <View style={styles.attachmentsContainer}>
            {selectedFile && (
              <View style={styles.attachmentChip}>
                <View style={styles.attachmentInfo}>
                  <Text style={styles.attachmentType}>File</Text>
                  <Text style={styles.attachmentName} numberOfLines={1}>{selectedFile.name}</Text>
                </View>
                <TouchableOpacity
                  onPress={() => setSelectedFile(null)}
                  style={styles.removeButton}
                  accessibilityLabel="Remove file"
                >
                  <SystemIcon name="close" size={14} color="#9CA3AF" />
                </TouchableOpacity>
              </View>
            )}

            {ytLinks.map((link, idx) => (
              <View key={idx} style={[styles.attachmentChip, styles.youtubeChip]}>
                <View style={styles.attachmentInfo}>
                  <Text style={styles.youtubeText}>YouTube</Text>
                  <Text style={styles.attachmentName} numberOfLines={1}>{link}</Text>
                </View>
                <TouchableOpacity
                  onPress={() => setYtLinks(prev => prev.filter((_, i) => i !== idx))}
                  style={styles.removeButton}
                  accessibilityLabel="Remove YouTube link"
                >
                  <SystemIcon name="close" size={14} color="#9CA3AF" />
                </TouchableOpacity>
              </View>
            ))}

            {isUrlUnderstanding && (
              <View style={[styles.attachmentChip, styles.urlUnderstandingChip]}>
                <View style={styles.attachmentInfo}>
                  <Text style={styles.urlUnderstandingText}>Mode</Text>
                  <Text style={styles.attachmentName}>URL Understanding</Text>
                </View>
                <TouchableOpacity
                  onPress={() => setIsUrlUnderstanding(false)}
                  style={styles.removeButton}
                  accessibilityLabel="Disable URL understanding"
                >
                  <SystemIcon name="close" size={14} color="#60A5FA" />
                </TouchableOpacity>
              </View>
            )}
          </View>
        )}

        {/* Input Area */}
        {!isLiveActive && (
          <View style={styles.inputWrapper}>
            <TextInput
              ref={inputRef}
              value={input}
              onChangeText={setInput}
              placeholder={isRecording ? "Listening..." : "Ask anything..."}
              placeholderTextColor="#6B7280"
              editable={!isRecording && !isTranscribing}
              multiline
              style={[
                styles.input,
                isRecording && styles.recordingInput
              ]}
              accessibilityLabel="Chat input"
              accessibilityHint="Type your message here"
            />
          </View>
        )}

        {/* Toolbar */}
        <View style={styles.toolbar}>
          <View style={styles.leftButtons}>
            {!isLiveActive && (
              <>
                {/* Voice Button */}
                <TouchableOpacity
                  onPress={toggleVoice}
                  disabled={isLoading || isTranscribing}
                  style={[
                    styles.iconButton,
                    isRecording && styles.recordingButton
                  ]}
                  accessibilityLabel={isRecording ? "Stop recording" : "Start voice input"}
                  accessibilityHint="Press to record voice message"
                >
                  <SystemIcon 
                    name={isTranscribing ? "loading" : isRecording ? "stop" : "microphone"} 
                    size={18} 
                    color={isRecording ? "#EF4444" : "#9CA3AF"} 
                  />
                  <Text style={[styles.buttonText, isRecording && styles.recordingButtonText]}>
                    {isRecording ? 'Stop' : 'Voice'}
                  </Text>
                </TouchableOpacity>

                {/* Add Menu */}
                <View style={styles.menuContainer}>
                  <TouchableOpacity
                    onPress={() => setIsMenuOpen(!isMenuOpen)}
                    disabled={isRecording}
                    style={[
                      styles.iconButton,
                      isMenuOpen && styles.menuButtonActive
                    ]}
                    accessibilityLabel="Add content"
                    accessibilityHint="Press to add files or YouTube links"
                    accessibilityState={{ expanded: isMenuOpen }}
                  >
                    <SystemIcon name="plus" size={18} color="#9CA3AF" />
                    <Text style={styles.buttonText}>Add</Text>
                  </TouchableOpacity>

                  {isMenuOpen && (
                    <View style={styles.menuDropdown}>
                      <TouchableOpacity
                        onPress={pickFile}
                        style={styles.menuItem}
                        accessibilityLabel="Upload files"
                      >
                        <SystemIcon name="upload" size={16} color="#60A5FA" />
                        <Text style={styles.menuItemText}>Upload Files</Text>
                      </TouchableOpacity>
                      <TouchableOpacity
                        onPress={addYouTubeLink}
                        style={styles.menuItem}
                        accessibilityLabel="Add YouTube link"
                      >
                        <SystemIcon name="youtube" size={16} color="#EF4444" />
                        <Text style={styles.menuItemText}>YouTube Link</Text>
                      </TouchableOpacity>
                      <View style={styles.menuSeparator} />
                      <TouchableOpacity
                        onPress={() => {
                          setIsUrlUnderstanding(!isUrlUnderstanding);
                          setIsMenuOpen(false);
                        }}
                        style={styles.menuItem}
                        accessibilityLabel="Toggle URL understanding"
                        accessibilityState={{ checked: isUrlUnderstanding }}
                      >
                        <View style={styles.menuItemLeft}>
                          <SystemIcon name="web" size={16} color={isUrlUnderstanding ? "#60A5FA" : "#9CA3AF"} />
                          <Text style={styles.menuItemText}>URL Understanding</Text>
                        </View>
                        {isUrlUnderstanding && <SystemIcon name="check" size={14} color="#60A5FA" />}
                      </TouchableOpacity>
                    </View>
                  )}
                </View>
              </>
            )}

            {isLiveActive && (
              <View style={styles.liveStatusContainer}>
                <View style={styles.liveDot} />
                <Text style={styles.liveStatusText}>{liveStatus}</Text>
              </View>
            )}
          </View>

          <View style={styles.rightButtons}>
            {isLiveActive && (
              <TouchableOpacity
                onPress={() => setIsMuted(!isMuted)}
                style={[
                  styles.muteButton,
                  isMuted && styles.unmuteButton
                ]}
                accessibilityLabel={isMuted ? "Unmute" : "Mute"}
              >
                <SystemIcon 
                  name={isMuted ? "microphone-off" : "microphone"} 
                  size={14} 
                  color="#FFFFFF" 
                />
                <Text style={styles.muteButtonText}>
                  {isMuted ? "Unmute" : "Mute"}
                </Text>
              </TouchableOpacity>
            )}
            <TouchableOpacity
              onPress={() => isLiveActive ? stopLiveMode() : handleSubmit()}
              disabled={isSendDisabled}
              style={[
                styles.sendButton,
                isSendDisabled && styles.sendButtonDisabled,
                isLiveActive && styles.stopButton,
                showLiveButton && !isLiveActive && styles.liveStartButton
              ]}
              accessibilityLabel={isLiveActive ? "Stop voice" : showLiveButton ? "Start live mode" : "Send message"}
              accessibilityState={{ disabled: isSendDisabled }}
            >
              {isLoading ? (
                <ActivityIndicator size="small" color="#FFFFFF" />
              ) : (
                <SystemIcon 
                  name={isLiveActive ? "stop-circle" : showLiveButton ? "radio" : "send"} 
                  size={18} 
                  color="#FFFFFF" 
                />
              )}
              <Text style={styles.sendButtonText}>
                {isLiveActive ? "Stop Voice" : showLiveButton ? "Start Live" : "Send"}
              </Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </View>
  );
});

ChatInput.displayName = 'ChatInput';

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#1F2937',
    borderTopWidth: 1,
    borderTopColor: '#374151',
    paddingHorizontal: 16,
    paddingTop: 16,
    paddingBottom: 24,
  },
  srOnly: {
    position: 'absolute',
    width: 1,
    height: 1,
    padding: 0,
    margin: -1,
    overflow: 'hidden',
    borderWidth: 0,
  },
  contentContainer: {
    maxWidth: 576,
    width: '100%',
    alignSelf: 'center',
  },
  attachmentsContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 8,
  },
  attachmentChip: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#111827',
    borderWidth: 1,
    borderColor: '#374151',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 8,
    gap: 8,
  },
  youtubeChip: {
    borderColor: '#EF4444',
    backgroundColor: 'rgba(239, 68, 68, 0.1)',
  },
  urlUnderstandingChip: {
    borderColor: '#3B82F6',
    backgroundColor: 'rgba(59, 130, 246, 0.1)',
  },
  attachmentInfo: {
    flexDirection: 'column',
  },
  attachmentType: {
    fontSize: 10,
    fontWeight: 'bold',
    color: '#60A5FA',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  youtubeText: {
    fontSize: 10,
    fontWeight: 'bold',
    color: '#EF4444',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  urlUnderstandingText: {
    fontSize: 10,
    fontWeight: 'bold',
    color: '#60A5FA',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  attachmentName: {
    fontSize: 14,
    color: '#E5E7EB',
    maxWidth: 200,
  },
  removeButton: {
    padding: 4,
  },
  inputWrapper: {
    backgroundColor: '#111827',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#374151',
    padding: 8,
    marginBottom: 12,
  },
  input: {
    color: '#F3F4F6',
    fontSize: 16,
    lineHeight: 24,
    padding: 8,
    minHeight: 50,
    maxHeight: 200,
  },
  recordingInput: {
    color: '#F87171',
  },
  toolbar: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-end',
  },
  leftButtons: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  rightButtons: {
    flexDirection: 'column',
    alignItems: 'flex-end',
    gap: 8,
  },
  iconButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: '#1F2937',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'transparent',
  },
  menuButtonActive: {
    backgroundColor: '#1F2937',
  },
  recordingButton: {
    backgroundColor: 'rgba(239, 68, 68, 0.1)',
    borderColor: '#EF4444',
  },
  buttonText: {
    fontSize: 14,
    fontWeight: '500',
    color: '#9CA3AF',
  },
  recordingButtonText: {
    color: '#EF4444',
  },
  menuContainer: {
    position: 'relative',
  },
  menuDropdown: {
    position: 'absolute',
    bottom: '100%',
    left: 0,
    marginBottom: 8,
    width: 288,
    backgroundColor: '#1F2937',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#374151',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 8,
    padding: 4,
    zIndex: 1000,
  },
  menuItem: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderRadius: 8,
  },
  menuItemLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  menuItemText: {
    fontSize: 14,
    color: '#E5E7EB',
    marginLeft: 12,
  },
  menuSeparator: {
    height: 1,
    backgroundColor: '#374151',
    marginVertical: 4,
    marginHorizontal: 8,
  },
  liveStatusContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: 'rgba(16, 185, 129, 0.1)',
    borderWidth: 1,
    borderColor: '#10B981',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
  liveDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#10B981',
  },
  liveStatusText: {
    fontSize: 12,
    fontWeight: 'bold',
    color: '#34D399',
    textTransform: 'uppercase',
    letterSpacing: 1,
  },
  muteButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: '#374151',
    paddingHorizontal: 16,
    paddingVertical: 6,
    borderRadius: 8,
  },
  unmuteButton: {
    backgroundColor: '#EA580C',
  },
  muteButtonText: {
    fontSize: 12,
    fontWeight: 'bold',
    color: '#FFFFFF',
  },
  sendButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: '#3B82F6',
    paddingHorizontal: 24,
    paddingVertical: 8,
    borderRadius: 8,
    shadowColor: '#3B82F6',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.3,
    shadowRadius: 4,
    elevation: 4,
  },
  sendButtonDisabled: {
    backgroundColor: '#1F2937',
    shadowOpacity: 0,
  },
  stopButton: {
    backgroundColor: '#DC2626',
    shadowColor: '#DC2626',
  },
  liveStartButton: {
    backgroundColor: '#059669',
    shadowColor: '#059669',
  },
  sendButtonText: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#FFFFFF',
  },
});
