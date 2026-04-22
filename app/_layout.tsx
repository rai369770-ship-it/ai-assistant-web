import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import WelcomeScreen from './welcome';
import ToolsScreen from './tools';
import ArticlesScreen from './articles';
import FavoritesScreen from './favorites';
import MoreScreen from './more';

const Tab = createBottomTabNavigator();

export default function RootLayout() {
  return (
    <SafeAreaProvider>
      <NavigationContainer independent={true}>
        <StatusBar style="light" />
        <Tab.Navigator
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
          <Tab.Screen 
            name="Welcome" 
            component={WelcomeScreen}
            options={{ tabBarStyle: { display: 'none' } }}
          />
          <Tab.Screen 
            name="Tools" 
            component={ToolsScreen}
            options={{
              tabBarLabel: 'Tools',
            }}
          />
          <Tab.Screen 
            name="Articles" 
            component={ArticlesScreen}
            options={{
              tabBarLabel: 'Articles',
            }}
          />
          <Tab.Screen 
            name="Favorites" 
            component={FavoritesScreen}
            options={{
              tabBarLabel: 'Favorites',
            }}
          />
          <Tab.Screen 
            name="More" 
            component={MoreScreen}
            options={{
              tabBarLabel: 'More',
            }}
          />
        </Tab.Navigator>
      </NavigationContainer>
    </SafeAreaProvider>
  );
}
