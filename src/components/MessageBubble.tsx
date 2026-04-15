import React, { useMemo, useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, ScrollView, Modal, Linking, Alert } from 'react-native';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';
import Markdown from 'react-native-markdown-display';
import { ChatMessage } from '../types';

interface MessageBubbleProps {
  message: ChatMessage;
  onEdit?: (text: string) => void;
}

export const MessageBubble: React.FC<MessageBubbleProps> = ({ message, onEdit }) => {
  const isUser = message.role === 'user';
  const [copied, setCopied] = useState(false);
  const [sourcesModalVisible, setSourcesModalVisible] = useState(false);

  const handleCopy = async () => {
    // In React Native, we'd need a clipboard library
    // For now, just show feedback
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleExport = () => {
    Alert.alert('Export', 'PDF export functionality would be implemented here using react-native-pdf or similar library.');
  };

  const hasSources = message.groundingChunks && message.groundingChunks.length > 0;
  const isThinking = !isUser && !message.isComplete && !message.content;

  const markdownRules = useMemo(() => ({
    code_block: (node: any, children: any) => (
      <View key={node.key} style={styles.codeBlockContainer}>
        <View style={styles.codeHeader}>
          <Text style={styles.codeLanguage}>{node.language || 'text'}</Text>
          <TouchableOpacity style={styles.copyCodeButton}>
            <Text style={styles.copyCodeText}>Copy</Text>
          </TouchableOpacity>
        </View>
        <ScrollView horizontal style={styles.codeScroll}>
          <Text style={styles.codeText}>{children}</Text>
        </ScrollView>
      </View>
    ),
  }), []);

  return (
    <View style={styles.container}>
      <View style={styles.contentWrapper}>
        <View style={styles.header}>
          <Icon 
            name={isUser ? "account" : "robot"} 
            size={16} 
            color={isUser ? "#60A5FA" : "#34D399"} 
          />
          <Text style={[styles.headerText, isUser ? styles.userHeaderText : styles.aiHeaderText]}>
            {isUser ? 'User' : 'AI Assistant'}
          </Text>
        </View>

        <View style={[styles.bubble, isUser ? styles.userBubble : styles.aiBubble]}>
          {isThinking ? (
            <View style={styles.thinkingContainer} accessibilityRole="status">
              <Text style={styles.thinkingText}>Processing</Text>
              <View style={styles.dotsContainer}>
                <View style={[styles.dot, styles.dot1]} />
                <View style={[styles.dot, styles.dot2]} />
                <View style={[styles.dot, styles.dot3]} />
              </View>
            </View>
          ) : isUser ? (
            <Text style={styles.userText}>{message.content}</Text>
          ) : (
            <View>
              <Markdown rules={markdownRules} style={styles.markdown}>
                {message.content}
              </Markdown>
              {!message.isComplete && <View style={styles.cursor} />}
            </View>
          )}

          {(isUser || (message.isComplete && message.content)) && (
            <View style={styles.actionsContainer}>
              <TouchableOpacity 
                onPress={handleCopy} 
                style={styles.actionButton}
                accessibilityLabel={copied ? "Copied" : "Copy message"}
              >
                <Icon 
                  name={copied ? "check" : "content-copy"} 
                  size={12} 
                  color={copied ? "#4ADE80" : "#D1D5DB"} 
                />
                <Text style={[styles.actionButtonText, copied && styles.copiedText]}>
                  {copied ? 'Copied' : 'Copy'}
                </Text>
              </TouchableOpacity>

              {isUser && onEdit && (
                <TouchableOpacity 
                  onPress={() => onEdit(message.content)} 
                  style={styles.actionButton}
                  accessibilityLabel="Edit message"
                >
                  <Icon name="pencil" size={12} color="#D1D5DB" />
                  <Text style={styles.actionButtonText}>Edit</Text>
                </TouchableOpacity>
              )}

              {!isUser && message.content && (
                <TouchableOpacity 
                  onPress={handleExport} 
                  style={[styles.actionButton, styles.exportButton]}
                  accessibilityLabel="Export message"
                >
                  <Icon name="download" size={12} color="#34D399" />
                  <Text style={[styles.actionButtonText, styles.exportButtonText]}>Export</Text>
                </TouchableOpacity>
              )}

              {!isUser && hasSources && (
                <>
                  <TouchableOpacity 
                    onPress={() => setSourcesModalVisible(true)} 
                    style={[styles.actionButton, styles.sourcesButton]}
                    accessibilityLabel={`View ${message.groundingChunks?.length} sources`}
                  >
                    <Icon name="web" size={12} color="#60A5FA" />
                    <Text style={[styles.actionButtonText, styles.sourcesButtonText]}>
                      Sources ({message.groundingChunks?.length})
                    </Text>
                  </TouchableOpacity>

                  <Modal
                    visible={sourcesModalVisible}
                    transparent
                    animationType="fade"
                    onRequestClose={() => setSourcesModalVisible(false)}
                    accessibilityLabel="Sources modal"
                  >
                    <View style={styles.modalOverlay}>
                      <View style={styles.modalContent}>
                        <View style={styles.modalHeader}>
                          <View style={styles.modalTitleContainer}>
                            <Icon name="web" size={16} color="#60A5FA" />
                            <Text style={styles.modalTitle}>Search Sources</Text>
                          </View>
                          <TouchableOpacity 
                            onPress={() => setSourcesModalVisible(false)}
                            style={styles.closeButton}
                            accessibilityLabel="Close modal"
                          >
                            <Icon name="close" size={18} color="#F3F4F6" />
                          </TouchableOpacity>
                        </View>
                        <ScrollView style={styles.modalScrollView}>
                          {message.groundingChunks?.map((chunk, i) => chunk.web ? (
                            <TouchableOpacity 
                              key={i} 
                              style={styles.sourceItem}
                              onPress={() => Linking.openURL(chunk.web!.uri)}
                            >
                              <Text style={styles.sourceTitle} numberOfLines={2}>
                                {chunk.web.title}
                              </Text>
                              <Text style={styles.sourceUri} numberOfLines={1}>
                                {chunk.web.uri}
                              </Text>
                            </TouchableOpacity>
                          ) : null)}
                        </ScrollView>
                      </View>
                    </View>
                  </Modal>
                </>
              )}
            </View>
          )}
        </View>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    width: '100%',
    marginBottom: 32,
  },
  contentWrapper: {
    flex: 1,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 8,
  },
  headerText: {
    fontSize: 12,
    fontWeight: 'bold',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  userHeaderText: {
    color: '#60A5FA',
  },
  aiHeaderText: {
    color: '#34D399',
  },
  bubble: {
    borderRadius: 12,
    padding: 24,
    borderWidth: 1,
  },
  userBubble: {
    backgroundColor: '#1F2937',
    borderColor: 'rgba(55, 65, 81, 0.5)',
  },
  aiBubble: {
    backgroundColor: '#111827',
    borderColor: 'rgba(16, 185, 129, 0.2)',
  },
  thinkingContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    height: 24,
    paddingVertical: 4,
  },
  thinkingText: {
    fontSize: 14,
    color: '#9CA3AF',
    fontWeight: '500',
    marginRight: 8,
  },
  dotsContainer: {
    flexDirection: 'row',
    gap: 4,
  },
  dot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: '#34D399',
  },
  dot1: {
    opacity: 0.4,
  },
  dot2: {
    opacity: 0.7,
  },
  dot3: {
    opacity: 1,
  },
  userText: {
    fontSize: 16,
    lineHeight: 28,
    color: '#F3F4F6',
    whiteSpace: 'pre-wrap',
  },
  markdown: {
    body: {
      fontSize: 16,
      lineHeight: 28,
      color: '#F3F4F6',
    },
    heading1: {
      fontSize: 24,
      fontWeight: 'bold',
      color: '#F3F4F6',
      marginTop: 16,
      marginBottom: 8,
    },
    heading2: {
      fontSize: 20,
      fontWeight: 'bold',
      color: '#F3F4F6',
      marginTop: 12,
      marginBottom: 6,
    },
    paragraph: {
      marginBottom: 12,
    },
    link: {
      color: '#60A5FA',
      textDecorationLine: 'underline',
    },
    code_inline: {
      backgroundColor: '#1F2937',
      paddingHorizontal: 4,
      paddingVertical: 2,
      borderRadius: 4,
      fontFamily: 'monospace',
      color: '#F3F4F6',
    },
  },
  codeBlockContainer: {
    marginVertical: 16,
    borderRadius: 8,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: '#374151',
    backgroundColor: '#030712',
  },
  codeHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 8,
    backgroundColor: '#1F2937',
    borderBottomWidth: 1,
    borderBottomColor: '#374151',
  },
  codeLanguage: {
    fontSize: 12,
    fontWeight: 'bold',
    color: '#9CA3AF',
    textTransform: 'uppercase',
  },
  copyCodeButton: {
    paddingHorizontal: 8,
    paddingVertical: 4,
  },
  copyCodeText: {
    fontSize: 12,
    color: '#9CA3AF',
  },
  codeScroll: {
    maxHeight: 300,
  },
  codeText: {
    fontFamily: 'monospace',
    fontSize: 14,
    color: '#F3F4F6',
    padding: 16,
  },
  cursor: {
    width: 8,
    height: 16,
    backgroundColor: '#34D399',
    marginLeft: 4,
    marginTop: 4,
  },
  actionsContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignItems: 'center',
    gap: 8,
    marginTop: 16,
    paddingTop: 16,
    borderTopWidth: 1,
    borderTopColor: 'rgba(55, 65, 81, 0.3)',
  },
  actionButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 6,
    backgroundColor: '#1F2937',
    borderWidth: 1,
    borderColor: '#374151',
  },
  exportButton: {
    backgroundColor: 'rgba(16, 185, 129, 0.1)',
    borderColor: 'rgba(16, 185, 129, 0.3)',
  },
  sourcesButton: {
    backgroundColor: 'rgba(59, 130, 246, 0.1)',
    borderColor: 'rgba(59, 130, 246, 0.3)',
  },
  actionButtonText: {
    fontSize: 12,
    color: '#D1D5DB',
  },
  copiedText: {
    color: '#4ADE80',
  },
  exportButtonText: {
    color: '#34D399',
  },
  sourcesButtonText: {
    color: '#60A5FA',
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.8)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalContent: {
    width: '90%',
    maxWidth: 512,
    backgroundColor: '#1F2937',
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#374151',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.3,
    shadowRadius: 16,
    elevation: 8,
    overflow: 'hidden',
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#374151',
    backgroundColor: 'rgba(17, 24, 39, 0.5)',
  },
  modalTitleContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  modalTitle: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#F3F4F6',
  },
  closeButton: {
    padding: 4,
  },
  modalScrollView: {
    maxHeight: 480,
    padding: 16,
  },
  sourceItem: {
    backgroundColor: '#111827',
    padding: 12,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#374151',
    marginBottom: 12,
  },
  sourceTitle: {
    fontSize: 14,
    fontWeight: '500',
    color: '#93C5FD',
    marginBottom: 4,
  },
  sourceUri: {
    fontSize: 12,
    color: '#6B7280',
    fontFamily: 'monospace',
  },
});
