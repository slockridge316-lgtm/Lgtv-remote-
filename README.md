# LG TV Remote

A simple Android touchpad remote for the LG webOS TV at `192.168.1.243`.

## Build on an Android tablet

This project is designed to build in a hosted Android/Gradle environment, so the tablet does not need the Android SDK installed locally.

### GitHub Actions (recommended)
1. Create a new GitHub repository.
2. Upload the contents of this folder to the repository.
3. Open the **Actions** tab.
4. Run **Build APK**.
5. Download the `LGTVRemote-debug-apk` artifact.

The included workflow installs Gradle and the Android SDK on the build server.

## App behavior
- First launch asks for your existing LG SSAP client key.
- The key is stored only in the app's private preferences on the device.
- TV address is preconfigured as `192.168.1.243`.
- Drag on the pad to move the TV pointer.
- Tap to click.
- Includes Back, Home, volume, and mute controls.

Do not put your real LG client key into the source code or repository.
