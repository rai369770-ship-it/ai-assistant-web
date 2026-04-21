import React from 'react';
import { View, Text, StyleSheet } from 'react-native';

interface ToastProps {
  message: string;
  visible: boolean;
}

export const Toast: React.FC<ToastProps> = ({ message, visible }) => {
  if (!visible) return null;

  return (
    <View style={styles.toastContainer} accessibilityRole="alert" accessibilityLiveRegion="polite">
      <Text style={styles.toastText}>{message}</Text>
    </View>
  );
};

const styles = StyleSheet.create({
  toastContainer: {
    position: 'absolute',
    top: 80,
    left: '50%',
    transform: [{ translateX: -120 }],
    backgroundColor: 'rgba(31, 41, 55, 0.95)',
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 9999,
    borderWidth: 1,
    borderColor: 'rgba(75, 85, 99, 0.5)',
    zIndex: 1000,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 8,
  },
  toastText: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '500',
  },
});
