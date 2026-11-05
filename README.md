# PS5 Title Camera

Native Android app for recognising PlayStation 5 printed title IDs with the phone's rear camera. It uses CameraX and on-device Google ML Kit text recognition, validates IDs in the exact format `PPSA` plus five digits, and looks up the game name in the community PS5 title list. Manual code entry is included.

## Download the APK

GitHub Actions builds a debug APK whenever code is pushed to `main`, and can also be run manually. Open the **Actions** tab, choose the latest **Android APK** workflow run, and download the `ps5-title-camera-debug-apk` artifact. Artifacts are retained for 30 days.

To create a persistent download, publish a version tag beginning with `v` (for example `v0.1.0`). The workflow attaches the APK to a GitHub Release.

## Project status and limitations

This is an initial source build. The first GitHub Actions run must complete successfully before an APK is available; the app has not yet been tested on a physical device. First launch needs internet access to download the title list. OCR is performed on-device; camera frames are not uploaded. This is an unsigned debug APK, not a Play Store release.

## Tech

- Kotlin, Android SDK 35, minimum Android 6.0 (API 23)
- CameraX rear-camera preview and image analysis
- Google ML Kit Text Recognition
- PS5 title database: `andshrew/PlayStation-Titles`
