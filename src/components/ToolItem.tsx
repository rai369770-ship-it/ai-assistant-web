import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';

interface ToolItemProps {
  name: string;
  description: string;
  onPress: () => void;
}

export const ToolItem: React.FC<ToolItemProps> = ({ name, description, onPress }) => {
  return (
    <TouchableOpacity
      style={styles.container}
      onPress={onPress}
      accessibilityLabel={`${name}: ${description}`}
      accessibilityRole="button"
      accessibilityHint="Opens tool details"
    >
      <View style={styles.content}>
        <Text style={styles.nameText} numberOfLines={2}>
          {name}
        </Text>
        <Text style={styles.descriptionText} numberOfLines={2}>
          {description}
        </Text>
      </View>
      <View style={styles.chevronContainer}>
        <Text style={styles.chevron}>›</Text>
      </View>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(31, 41, 55, 0.4)',
    borderRadius: 12,
    padding: 16,
    marginHorizontal: 16,
    marginVertical: 6,
    borderWidth: 1,
    borderColor: 'rgba(75, 85, 99, 0.3)',
  },
  content: {
    flex: 1,
    paddingRight: 12,
  },
  nameText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#F3F4F6',
    marginBottom: 4,
  },
  descriptionText: {
    fontSize: 13,
    color: '#9CA3AF',
    lineHeight: 18,
  },
  chevronContainer: {
    width: 24,
    height: 24,
    justifyContent: 'center',
    alignItems: 'center',
  },
  chevron: {
    fontSize: 24,
    color: '#6B7280',
    fontWeight: '300',
  },
});
