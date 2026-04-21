import React, { useState, useEffect } from 'react';
import { PermissionsAndroid, Platform } from 'react-native';
import * as Camera from 'expo-camera';
import * as MediaLibrary from 'expo-media-library';

export const requestAllPermissions = async () => {
  if (Platform.OS === 'android') {
    try {
      const permissionsToRequest = [
        PermissionsAndroid.PERMISSIONS.RECORD_AUDIO,
        PermissionsAndroid.PERMISSIONS.CAMERA,
        PermissionsAndroid.PERMISSIONS.READ_EXTERNAL_STORAGE,
        PermissionsAndroid.PERMISSIONS.WRITE_EXTERNAL_STORAGE,
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
      await Camera.Camera.requestCameraPermissionsAsync();
      await Camera.Camera.requestMicrophonePermissionsAsync();
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
