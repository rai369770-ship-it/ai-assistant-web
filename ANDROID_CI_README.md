# Android CI/CD Setup for React Native

This document describes the GitHub Actions workflow for building the React Native Android application.

## Workflow Overview

The GitHub Actions workflow (`.github/workflows/android.yml`) automates the build process for the Android application. When triggered, it:

1. Checks out the code
2. Sets up Node.js 18
3. Sets up Java JDK 17 (Temurin)
4. Sets up Android SDK
5. Installs npm dependencies
6. Caches Gradle packages for faster builds
7. Builds Debug APK
8. Builds Release APK
9. Uploads both APKs as artifacts

## Trigger Conditions

The workflow runs on:
- Push to `main` or `master` branches
- Pull requests targeting `main` or `master` branches

## Build Artifacts

After a successful build, two APK files are available as downloadable artifacts:
- `app-debug-apk`: Debug version for testing
- `app-release-apk`: Release version for distribution

Artifacts are retained for 30 days.

## Required Files

### Android Project Structure
```
android/
├── app/
│   ├── src/main/
│   │   ├── java/com/aiassistantrn/
│   │   │   ├── MainActivity.kt
│   │   │   └── MainApplication.kt
│   │   ├── res/
│   │   │   ├── drawable/
│   │   │   ├── mipmap-*/
│   │   │   ├── values/
│   │   │   └── xml/
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── debug.keystore
├── gradle/wrapper/
│   └── gradle-wrapper.properties
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
└── local.properties
```

### Key Configuration Files

#### `android/build.gradle`
- Defines project-wide build configuration
- Sets Android SDK versions (compileSdkVersion: 34, minSdkVersion: 24, targetSdkVersion: 34)
- Configures Kotlin version (1.9.24)
- Sets up repositories (Google, Maven Central, JitPack)

#### `android/app/build.gradle`
- Application-specific configuration
- Package name: `com.aiassistantrn`
- Version: 1.0.0 (versionCode: 1)
- Signing configurations for debug and release
- Enables Hermes engine
- ProGuard rules for release builds

#### `android/gradle.properties`
- JVM arguments for Gradle daemon
- AndroidX enabled
- React Native architecture settings
- Hermes enabled
- Supported architectures: armeabi-v7a, arm64-v8a, x86, x86_64

#### `android/gradle/wrapper/gradle-wrapper.properties`
- Gradle version: 8.6
- Distribution URL configured

## Building Locally

### Prerequisites
- Node.js 18+
- Java JDK 17
- Android SDK with API level 34
- Android NDK 26.1.10909125

### Commands

```bash
# Install dependencies
npm install

# Navigate to android directory
cd android

# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease

# Clean build
./gradlew clean

# Run on connected device
./gradlew installDebug
```

## Release Signing

For production releases, configure signing in `android/gradle.properties`:

```properties
RELEASE_STORE_FILE=/path/to/keystore.jks
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=your_key_alias
RELEASE_KEY_PASSWORD=your_key_password
```

Then update `android/app/build.gradle` to use these properties for the release signing config.

## Troubleshooting

### Common Issues

1. **Out of Memory Errors**
   - Increase JVM heap size in `gradle.properties`
   - Current setting: `-Xmx4g`

2. **SDK Not Found**
   - Ensure `ANDROID_HOME` environment variable is set
   - GitHub Actions handles this automatically

3. **Build Cache Issues**
   - Clean Gradle cache: `./gradlew clean`
   - Delete `.gradle` folder and rebuild

4. **Dependency Resolution Failures**
   - Check internet connectivity
   - Verify repository URLs in `build.gradle`
   - Clear npm cache: `npm cache clean --force`

## Security Notes

- Never commit release keystore passwords to version control
- Use GitHub Secrets for sensitive information in CI/CD
- The included `debug.keystore` is only for development/debugging
- For production, generate a new secure keystore

## Performance Optimization

The workflow includes several optimizations:
- Gradle package caching between runs
- Parallel Gradle execution enabled
- Build cache enabled
- Increased JVM memory allocation
- Stack traces enabled for better error debugging
