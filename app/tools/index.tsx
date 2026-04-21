import React, { useState } from 'react';
import { View, FlatList, StyleSheet, SafeAreaView, Text } from 'react-native';
import { useRouter } from 'expo-router';
import { TopBar } from '@/components/TopBar';
import { BottomTab } from '@/components/BottomTab';
import { CategoryHeader } from '@/components/CategoryHeader';
import { ToolItem } from '@/components/ToolItem';
import { Toast } from '@/components/Toast';
import { getToolsByCategory } from '@/utils/toolsData';

const TABS = ['Tools', 'Articles', 'Favorites', 'More'];

export default function ToolsScreen() {
  const router = useRouter();
  const [activeTab, setActiveTab] = useState('Tools');
  const [toastVisible, setToastVisible] = useState(false);
  const [toastMessage, setToastMessage] = useState('');

  const categories = getToolsByCategory();

  const showToast = (message: string) => {
    setToastMessage(message);
    setToastVisible(true);
    setTimeout(() => setToastVisible(false), 2500);
  };

  const handleToolPress = (toolName: string) => {
    showToast('Coming soon');
  };

  const handleMoreOptions = () => {
    showToast('Coming soon');
  };

  const handleTabPress = (tab: string) => {
    if (tab === 'Tools') {
      setActiveTab('Tools');
    } else if (tab === 'Articles') {
      router.push('/articles');
    } else if (tab === 'Favorites') {
      router.push('/favorites');
    } else if (tab === 'More') {
      router.push('/more');
    }
  };

  const renderCategory = ({ item }: { item: { name: string; tools: any[] } }) => (
    <View>
      <CategoryHeader categoryName={item.name} />
      {item.tools.map((tool) => (
        <ToolItem
          key={tool.id}
          name={tool.name}
          description={tool.description}
          onPress={() => handleToolPress(tool.name)}
        />
      ))}
    </View>
  );

  return (
    <SafeAreaView style={styles.container}>
      <TopBar
        leftText="SToolkit"
        centerText="Tools"
        onMorePress={handleMoreOptions}
      />
      
      <FlatList
        data={categories}
        renderItem={renderCategory}
        keyExtractor={(item) => item.name}
        contentContainerStyle={styles.listContent}
        showsVerticalScrollIndicator={false}
        accessibilityLabel="Tools list"
      />

      {/* More tools coming soon text */}
      <View style={styles.comingSoonContainer}>
        <Text style={styles.comingSoonText}>More tools coming soon</Text>
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
  listContent: {
    paddingBottom: 16,
  },
  comingSoonContainer: {
    paddingHorizontal: 16,
    paddingVertical: 20,
    alignItems: 'center',
  },
  comingSoonText: {
    fontSize: 14,
    color: '#6B7280',
    fontStyle: 'italic',
  },
});
