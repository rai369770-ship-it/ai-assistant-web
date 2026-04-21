const { getDefaultConfig } = require('expo/metro-config');

const config = getDefaultConfig(__dirname);

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
config.resolver.blockList = [
  ...config.resolver.blockList,
  /.*\.test\..*/,
  /.*\.spec\..*/,
];

module.exports = config;
