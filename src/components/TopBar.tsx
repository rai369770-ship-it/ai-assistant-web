import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';

interface TopBarProps {
  leftText?: string;
  centerText: string;
  onMorePress?: () => void;
}

export const TopBar: React.FC<TopBarProps> = ({ 
  leftText = 'SToolkit', 
  centerText, 
  onMorePress 
}) => {
  return (
    <View style={styles.container}>
      <View style={styles.leftContainer}>
        {leftText && (
          <Text style={styles.leftText} accessibilityLabel="App name">
            {leftText}
          </Text>
        )}
      </View>
      <View style={styles.centerContainer}>
        <Text style={styles.centerText} numberOfLines={1}>
          {centerText}
        </Text>
      </View>
      <View style={styles.rightContainer}>
        <TouchableOpacity
          onPress={onMorePress}
          style={styles.moreButton}
          accessibilityLabel="More options"
          accessibilityRole="button"
          accessibilityHint="Opens more options menu"
        >
          <Text style={styles.moreButtonText}>⋮</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(55, 65, 81, 0.5)',
    backgroundColor: 'rgba(31, 41, 55, 0.5)',
    height: 56,
  },
  leftContainer: {
    flex: 1,
    alignItems: 'flex-start',
  },
  centerContainer: {
    flex: 2,
    alignItems: 'center',
  },
  rightContainer: {
    flex: 1,
    alignItems: 'flex-end',
  },
  leftText: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#FFFFFF',
  },
  centerText: {
    fontSize: 18,
    fontWeight: '600',
    color: '#E5E7EB',
    textAlign: 'center',
  },
  moreButton: {
    padding: 8,
    borderRadius: 8,
    backgroundColor: 'rgba(55, 65, 81, 0.5)',
  },
  moreButtonText: {
    fontSize: 20,
    color: '#FFFFFF',
    fontWeight: 'bold',
  },
});
