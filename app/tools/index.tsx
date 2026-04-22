import React, { useState } from 'react';
import { View, FlatList, StyleSheet, SafeAreaView, Text } from 'react-native';
import { CategoryHeader } from '@/components/CategoryHeader';
import { ToolItem } from '@/components/ToolItem';
import { Toast } from '@/components/Toast';
import { getToolsByCategory } from '@/utils/toolsData';

export default function ToolsScreen() {
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
      <FlatList
        data={categories}
        renderItem={renderCategory}
        keyExtractor={(item) => item.name}
        contentContainerStyle={styles.listContent}
        showsVerticalScrollIndicator={false}
        accessibilityLabel="Tools list"
      />

      <View style={styles.comingSoonContainer}>
        <Text style={styles.comingSoonText}>More tools coming soon</Text>
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
