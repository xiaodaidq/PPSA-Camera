# PS5 Title Camera

Native Android app that uses the rear camera and on-device ML Kit OCR to recognise printed PlayStation 5 title IDs in the exact format `PPSA` followed by five digits. It looks up the game name in the community PS5 title database and displays the result over the camera preview. Manual code entry is also available.

## Build APK

GitHub Actions builds a debug APK on pushes to `main`, pull requests, and manual workflow runs. Open **Actions**, select the latest **Android APK** run, and download the `ps5-title-camera-debug-apk` artifact (retained for 30 days).

To publish a persistent APK, push a version tag such as `v0.2.0`; the workflow attaches the APK to a GitHub Release.

## Current status

This is an initial source project. The first cloud build is needed to verify compilation; it has not yet been built or tested on a physical device. The app downloads the title list at startup and requires internet access for that lookup. Camera frames are processed locally by ML Kit and are not uploaded.
