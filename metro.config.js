const { getDefaultConfig } = require('expo/metro-config');

const config = getDefaultConfig(__dirname);

// Required for expo-router
config.resolver.unstable_enablePackageExports = true;

// Optimize for smaller bundle size
config.transformer = {
  ...config.transformer,
  minifierConfig: {
    ...config.transformer.minifierConfig,
    compress: {
      ...config.transformer.minifierConfig.compress,
      drop_console: true,
    },
  },
};

// Exclude unnecessary files from the bundle
const blockList = config.resolver.blockList;
let newBlockList = [
  /.*\.test\..*/,
  /.*\.spec\..*/,
];

// If there's an existing blockList, handle it appropriately
if (Array.isArray(blockList)) {
  newBlockList = [...blockList, ...newBlockList];
} else if (blockList instanceof RegExp) {
  newBlockList = [blockList, ...newBlockList];
} else if (blockList) {
  // For any other truthy blockList, try to convert to array or use as-is
  try {
    const converted = Array.from(blockList);
    newBlockList = [...converted, ...newBlockList];
  } catch {
    // If conversion fails, just use our new patterns
    newBlockList = newBlockList;
  }
}

config.resolver.blockList = newBlockList;

module.exports = config;
