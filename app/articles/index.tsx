import React, { useState } from 'react';
import { View, StyleSheet, SafeAreaView, Text } from 'react-native';
import { useRouter } from 'expo-router';
import { TopBar } from '@/components/TopBar';
import { BottomTab } from '@/components/BottomTab';
import { Toast } from '@/components/Toast';

const TABS = ['Tools', 'Articles', 'Favorites', 'More'];

export default function ArticlesScreen() {
  const router = useRouter();
  const [activeTab, setActiveTab] = useState('Articles');
  const [toastVisible, setToastVisible] = useState(false);
  const [toastMessage, setToastMessage] = useState('');

  const handleMoreOptions = () => {
    showToast('Coming soon');
  };

  const handleTabPress = (tab: string) => {
    if (tab === 'Tools') {
      router.push('/tools');
    } else if (tab === 'Articles') {
      setActiveTab('Articles');
    } else if (tab === 'Favorites') {
      router.push('/favorites');
    } else if (tab === 'More') {
      router.push('/more');
    }
  };

  const showToast = (message: string) => {
    setToastMessage(message);
    setToastVisible(true);
    setTimeout(() => setToastVisible(false), 2500);
  };

  return (
    <SafeAreaView style={styles.container}>
      <TopBar
        leftText="SToolkit"
        centerText="Articles"
        onMorePress={handleMoreOptions}
      />
      
      <View style={styles.centerContent}>
        <Text style={styles.comingSoonText}>Articles coming soon</Text>
      </View>

      <BottomTab
        tabs={TABS}
        activeTab={activeTab}
        onTabPress={handleTabPress}
      />

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
