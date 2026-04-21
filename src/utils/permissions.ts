import React, { useState, useEffect } from 'react';
import { PermissionsAndroid, Platform } from 'react-native';
import * as ExpoPermissions from 'expo-permissions';

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
    // iOS uses expo-permissions
    try {
      await ExpoPermissions.askAsync(
        ExpoPermissions.CAMERA,
        ExpoPermissions.AUDIO_RECORDING,
        ExpoPermissions.MEDIA_LIBRARY
      );
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
