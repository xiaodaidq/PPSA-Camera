# PS5 Title Camera

A native Android camera app that reads printed PlayStation 5 title IDs and overlays the matching game title on the live rear-camera preview.

## Features

- CameraX rear-camera preview and image analysis.
- Google ML Kit text recognition runs on-device; camera frames are not uploaded.
- OCR recognises the exact token form `PPSA` plus five digits. It tolerates spaces inserted by OCR, but rejects adjacent extra letters or digits.
- Manual PPSA code entry is available.
- The app downloads the community PS5 title list and maps database IDs such as `PPSA01284_00` to the printed short code `PPSA01284`.
- The title list is downloaded over HTTPS from the `andshrew/PlayStation-Titles` GitHub repository.

## Download the APK

Open the repository's **Actions** tab, select the latest successful **Android APK** workflow run, and download the `ps5-title-camera-debug-apk` artifact. GitHub retains each artifact for 30 days.

Latest successful build at the time of this update: run #6. A new build is triggered by changes pushed to `main`.

## Build

The GitHub Actions workflow uses JDK 17, Android SDK Platform 35, Android Gradle Plugin 8.7.3 and Gradle 8.9. It runs `gradle :app:assembleDebug` and uploads the resulting APK.

For a local build, open this project in Android Studio with JDK 17 and Android SDK Platform 35 installed, then build the debug APK.

## Database format and checked example

The upstream `PS5_Titles.tsv` dataset has columns `titleId`, `conceptId`, `name`, `contentId`, `region`, and `publisherId`. The app reads the first, third columns, validates the full database ID as `PPSA[0-9]{5}_NN`, strips the suffix, and keeps the first non-empty name for each short ID.

Checked example from the upstream dataset:
- `PPSA01284_00` → `Returnal`

The upstream list is community-maintained, not an official Sony API; names and coverage depend on that dataset.

## Permissions and privacy

- Camera permission is requested at runtime for rear-camera scanning.
- Internet permission is used only to download the title list.
- OCR runs on the device. Camera images are not sent to the database host.

## Current limitations

- The debug APK has compiled successfully in GitHub Actions, and its ZIP/APK structure has been checked. It has **not** yet been installed or tested on a physical phone.
- The title database is fetched at startup; offline caching and a manual refresh control are not yet implemented.
- This is an unsigned debug APK for testing, not a Play Store release. Android may show an install warning for an APK downloaded outside the Play Store.
- OCR accuracy depends on lighting, focus, print size and camera distance.
