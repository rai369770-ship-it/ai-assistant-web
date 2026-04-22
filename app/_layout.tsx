import React from 'react';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { Tabs } from 'expo-router';
import WelcomeScreen from './welcome';

export default function RootLayout() {
  return (
    <SafeAreaProvider>
      <StatusBar style="light" />
      <Tabs
        screenOptions={{
          headerShown: false,
          tabBarStyle: {
            backgroundColor: 'rgba(31, 41, 55, 0.95)',
            borderTopWidth: 1,
            borderTopColor: 'rgba(75, 85, 99, 0.5)',
            paddingBottom: 8,
            paddingTop: 8,
            height: 60,
          },
          tabBarActiveTintColor: '#60A5FA',
          tabBarInactiveTintColor: '#9CA3AF',
          tabBarLabelStyle: {
            fontSize: 12,
            fontWeight: '500',
          },
        }}
      >
        <Tabs.Screen 
          name="welcome" 
          component={WelcomeScreen}
          options={{ href: null }}
        />
        <Tabs.Screen 
          name="tools" 
          options={{
            tabBarLabel: 'Tools',
          }}
        />
        <Tabs.Screen 
          name="articles" 
          options={{
            tabBarLabel: 'Articles',
          }}
        />
        <Tabs.Screen 
          name="favorites" 
          options={{
            tabBarLabel: 'Favorites',
          }}
        />
        <Tabs.Screen 
          name="more" 
          options={{
            tabBarLabel: 'More',
          }}
        />
      </Tabs>
    </SafeAreaProvider>
  );
}
