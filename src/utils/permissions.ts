import React, { useState, useEffect } from 'react';
import { Platform, PermissionsAndroid } from 'react-native';
import { Camera } from 'expo-camera';
import * as MediaLibrary from 'expo-media-library';

export const requestAllPermissions = async () => {
  if (Platform.OS === 'android') {
    try {
      // Request storage permissions using React Native PermissionsAndroid
      const androidVersion = parseInt(Platform.Version.toString(), 10);
      
      if (androidVersion >= 11) {
        // Android 11+ - Storage access is more restricted
        console.log('Android 11+ detected, storage permissions handled via app settings');
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
          
          if (readGranted === PermissionsAndroid.RESULTS.GRANTED) {
            console.log('READ_EXTERNAL_STORAGE granted');
          } else {
            console.log('READ_EXTERNAL_STORAGE denied');
          }
          
          if (writeGranted === PermissionsAndroid.RESULTS.GRANTED) {
            console.log('WRITE_EXTERNAL_STORAGE granted');
          } else {
            console.log('WRITE_EXTERNAL_STORAGE denied');
          }
        } catch (err) {
          console.warn('Error requesting storage permissions:', err);
        }
      }

      const permissionsToRequest = [
        PermissionsAndroid.PERMISSIONS.RECORD_AUDIO,
        PermissionsAndroid.PERMISSIONS.CAMERA,
      ];

      for (const permission of permissionsToRequest) {
        try {
          const granted = await PermissionsAndroid.request(permission, {
            title: `${permission} Permission`,
            message: 'This app needs access to this feature for full functionality.',
            buttonPositive: 'Allow',
            buttonNegative: 'Deny',
          });
          
          if (granted === PermissionsAndroid.RESULTS.GRANTED) {
            console.log(`${permission} granted`);
          } else {
            console.log(`${permission} denied`);
          }
        } catch (err) {
          console.warn(`Error requesting ${permission}:`, err);
        }
      }
    } catch (err) {
      console.error('Permission request error:', err);
    }
  } else {
    // iOS uses module-specific permission methods
    try {
      await Camera.requestCameraPermissionsAsync();
      await Camera.requestMicrophonePermissionsAsync();
      await MediaLibrary.requestPermissionsAsync();
    } catch (err) {
      console.warn('iOS permission error:', err);
    }
  }
};

export const useAppPermissions = () => {
  const [permissionsGranted, setPermissionsGranted] = useState(false);

  useEffect(() => {
    const grantPermissions = async () => {
      await requestAllPermissions();
      setPermissionsGranted(true);
    };

    grantPermissions();
  }, []);

  return permissionsGranted;
};
