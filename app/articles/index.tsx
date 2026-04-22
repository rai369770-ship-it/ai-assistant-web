import React, { useState, useCallback } from 'react';
import { View, StyleSheet, SafeAreaView, Text, BackHandler } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Toast } from '@/components/Toast';

export default function ArticlesScreen() {
  const [toastVisible, setToastVisible] = useState(false);
  const [toastMessage, setToastMessage] = useState('');
  const exitAttemptsRef = React.useRef(0);
  const exitTimeoutRef = React.useRef<NodeJS.Timeout | null>(null);

  // Handle back button press - exit app on 4th tab screen
  useFocusEffect(
    useCallback(() => {
      const onBackPress = () => {
        const now = Date.now();
        
        if (exitTimeoutRef.current) {
          clearTimeout(exitTimeoutRef.current);
        }
        
        if (exitAttemptsRef.current === 0 || now - (exitAttemptsRef.current * 1000) > 2000) {
          exitAttemptsRef.current = 1;
        } else {
          exitAttemptsRef.current += 1;
        }
        
        if (exitAttemptsRef.current >= 4) {
          BackHandler.exitApp();
          return true;
        }
        
        exitTimeoutRef.current = setTimeout(() => {
          exitAttemptsRef.current = 0;
        }, 2000);
        
        return true;
      };

      const subscription = BackHandler.addEventListener('hardwareBackPress', onBackPress);

      return () => {
        subscription.remove();
        if (exitTimeoutRef.current) {
          clearTimeout(exitTimeoutRef.current);
        }
      };
    }, [])
  );

  const showToast = (message: string) => {
    setToastMessage(message);
    setToastVisible(true);
    setTimeout(() => setToastVisible(false), 2500);
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.centerContent}>
        <Text style={styles.comingSoonText}>Articles coming soon</Text>
      </View>

      <Toast message={toastMessage} visible={toastVisible} />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0B0F19',
  },
  centerContent: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 16,
  },
  comingSoonText: {
    fontSize: 18,
    color: '#9CA3AF',
    textAlign: 'center',
  },
});
