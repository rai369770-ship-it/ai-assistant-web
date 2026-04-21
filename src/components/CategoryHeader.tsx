import React from 'react';
import { View, Text, StyleSheet } from 'react-native';

interface CategoryHeaderProps {
  categoryName: string;
}

export const CategoryHeader: React.FC<CategoryHeaderProps> = ({ categoryName }) => {
  return (
    <View style={styles.container} accessibilityRole="header">
      <Text style={styles.text} accessibilityLabel={`Category: ${categoryName}`}>
        {categoryName}
      </Text>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    paddingHorizontal: 16,
    paddingVertical: 16,
    marginTop: 8,
  },
  text: {
    fontSize: 14,
    fontWeight: '700',
    color: '#60A5FA',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
});
