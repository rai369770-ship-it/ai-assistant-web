import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';

interface BottomTabProps {
  tabs: string[];
  activeTab: string;
  onTabPress: (tab: string) => void;
}

export const BottomTab: React.FC<BottomTabProps> = ({ tabs, activeTab, onTabPress }) => {
  return (
    <View style={styles.container} accessibilityRole="tablist">
      {tabs.map((tab) => {
        const isActive = tab === activeTab;
        return (
          <TouchableOpacity
            key={tab}
            style={[styles.tab, isActive && styles.activeTab]}
            onPress={() => onTabPress(tab)}
            accessibilityRole="tab"
            accessibilityState={{ selected: isActive }}
            accessibilityLabel={`${tab}${isActive ? ', selected' : ''}`}
          >
            <Text style={[styles.tabText, isActive && styles.activeTabText]}>
              {tab}
            </Text>
            {isActive && <View style={styles.activeIndicator} />}
          </TouchableOpacity>
        );
      })}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    backgroundColor: 'rgba(31, 41, 55, 0.95)',
    borderTopWidth: 1,
    borderTopColor: 'rgba(75, 85, 99, 0.5)',
    paddingBottom: 20,
    paddingTop: 8,
  },
  tab: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 8,
    position: 'relative',
  },
  activeTab: {
    backgroundColor: 'transparent',
  },
  tabText: {
    fontSize: 12,
    fontWeight: '500',
    color: '#9CA3AF',
  },
  activeTabText: {
    color: '#60A5FA',
    fontWeight: '600',
  },
  activeIndicator: {
    position: 'absolute',
    bottom: 4,
    width: 20,
    height: 3,
    backgroundColor: '#60A5FA',
    borderRadius: 2,
  },
});
