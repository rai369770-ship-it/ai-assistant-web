import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  SafeAreaView,
  TouchableOpacity,
  Linking,
  Platform,
  Alert,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import * as FileSystem from 'expo-file-system';
import * as PermissionsAndroid from 'react-native';
import * as Camera from 'expo-camera';
import * as MediaLibrary from 'expo-media-library';

const CONFIG_PATH = `${FileSystem.documentDirectory}settings.json`;

interface Settings {
  isFirstRun?: boolean;
}

export default function WelcomeScreen() {
  const navigation = useNavigation();
  const [isLoading, setIsLoading] = useState(false);

  const saveSettings = async (data: Settings) => {
    try {
      await FileSystem.writeAsStringAsync(CONFIG_PATH, JSON.stringify(data), { encoding: FileSystem.EncodingType.UTF8 });
    } catch (error) {
      console.error('Error saving settings:', error);
    }
  };

  const loadSettings = async (): Promise<Settings | null> => {
    try {
      const fileInfo = await FileSystem.getInfoAsync(CONFIG_PATH);
      if (fileInfo.exists) {
        const content = await FileSystem.readAsStringAsync(CONFIG_PATH, { encoding: FileSystem.EncodingType.UTF8 });
        return JSON.parse(content);
      }
      return null;
    } catch (error) {
      console.error('Error loading settings:', error);
      return null;
    }
  };

  const requestAndroidPermissions = async () => {
    const androidVersion = parseInt(Platform.Version.toString(), 10);

    if (androidVersion >= 11) {
      // Android 11+ - Show dialog for all files access
      Alert.alert(
        'All files access required',
        'All files permissions is required for files operations. Allow by granting in the setting.',
        [
          {
            text: 'Grant Access',
            onPress: () => {
              Linking.openSettings();
            },
          },
        ]
      );
    } else {
      // Android 10 and below - Request storage permissions
      try {
        const readGranted = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.READ_EXTERNAL_STORAGE,
          {
            title: 'Storage Read Permission',
            message: 'This app needs access to read files from your device.',
            buttonPositive: 'Allow',
            buttonNegative: 'Deny',
          }
        );

        const writeGranted = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE,
          {
            title: 'Storage Write Permission',
            message: 'This app needs access to write files to your device.',
            buttonPositive: 'Allow',
            buttonNegative: 'Deny',
          }
        );

        if (readGranted !== PermissionsAndroid.RESULTS.GRANTED ||
            writeGranted !== PermissionsAndroid.RESULTS.GRANTED) {
          console.log('Storage permissions not granted');
        }
      } catch (err) {
        console.warn('Permission request error:', err);
      }
    }

    // Request camera and microphone permissions
    await requestMediaPermissions();
  };

  const requestMediaPermissions = async () => {
    try {
      // Request camera permission
      const cameraStatus = await Camera.requestCameraPermissionsAsync();
      if (cameraStatus.status !== 'granted') {
        console.log('Camera permission not granted');
      }

      // Request microphone permission
      const micStatus = await Camera.requestMicrophonePermissionsAsync();
      if (micStatus.status !== 'granted') {
        console.log('Microphone permission not granted');
      }

      // Request media library permission
      const mediaStatus = await MediaLibrary.requestPermissionsAsync();
      if (mediaStatus.status !== 'granted') {
        console.log('Media library permission not granted');
      }
    } catch (err) {
      console.warn('Media permission error:', err);
    }
  };

  const handleContinue = async () => {
    setIsLoading(true);

    // Save settings to mark first run as complete
    await saveSettings({ isFirstRun: false });

    // Request permissions based on platform
    if (Platform.OS === 'android') {
      await requestAndroidPermissions();
    } else {
      await requestMediaPermissions();
    }

    setIsLoading(false);

    // Navigate to Tools tab after permissions
    setTimeout(() => {
      (navigation as any).getParent()?.navigate('Tools');
    }, 500);
  };

  const openLink = () => {
    Linking.openURL('https://blindtechnexus.pages.dev');
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.content}>
        <Text style={styles.title} accessibilityRole="header">
          Welcome to SToolkit
        </Text>

        <Text style={styles.welcomeText}>
          Welcome to Stoolkit, your all-in-one multi-toolkit for Android. This app brings together powerful AI tools, productivity features, and utilities for documents, images, audio, and video—all in one place. Designed to simplify your daily tasks, Stoolkit helps you create, manage, and explore with ease. Discover useful articles, smart features, and practical resources that support your work and learning. Simple, fast, and efficient, Stoolkit gives you everything you need to do more in a single app.
        </Text>

        <Text style={styles.developerLabel}>Developed by:</Text>

        <TouchableOpacity
          onPress={openLink}
          accessibilityRole="link"
          accessibilityHint="Opens Blind Tech Nexus website"
        >
          <Text style={styles.linkText}>Blind Tech Nexus</Text>
        </TouchableOpacity>

        <Text style={styles.instructionText}>
          Click continue to start using the app.
        </Text>

        <TouchableOpacity
          style={[styles.continueButton, isLoading && styles.disabledButton]}
          onPress={handleContinue}
          disabled={isLoading}
          accessibilityRole="button"
          accessibilityLabel="Continue to app"
        >
          <Text style={styles.continueButtonText}>
            {isLoading ? 'Loading...' : 'Continue'}
          </Text>
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0B0F19',
  },
  content: {
    flex: 1,
    paddingHorizontal: 24,
    justifyContent: 'center',
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#FFFFFF',
    textAlign: 'center',
    marginBottom: 24,
  },
  welcomeText: {
    fontSize: 16,
    color: '#E5E7EB',
    lineHeight: 24,
    textAlign: 'center',
    marginBottom: 24,
  },
  developerLabel: {
    fontSize: 14,
    color: '#9CA3AF',
    textAlign: 'center',
    marginTop: 16,
    marginBottom: 4,
  },
  linkText: {
    fontSize: 16,
    color: '#60A5FA',
    textAlign: 'center',
    textDecorationLine: 'underline',
    marginBottom: 24,
  },
  instructionText: {
    fontSize: 14,
    color: '#9CA3AF',
    textAlign: 'center',
    marginBottom: 24,
  },
  continueButton: {
    backgroundColor: '#60A5FA',
    paddingVertical: 16,
    paddingHorizontal: 32,
    borderRadius: 12,
    alignItems: 'center',
    marginHorizontal: 24,
  },
  disabledButton: {
    backgroundColor: '#4B5563',
  },
  continueButtonText: {
    fontSize: 18,
    fontWeight: '600',
    color: '#0B0F19',
  },
});
