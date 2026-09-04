# WB Land Utility

Native Kotlin Android app for using the BanglarBhumi land information portal inside a WebView and printing a formatted A4 summary report.

## Features

- Loads `https://banglarbhumi.gov.in/BanglarBhumi/Home.action` inside the app.
- Keeps portal navigation inside the app.
- Preserves WebView cookies, third-party cookies, JavaScript, DOM storage, and database support for portal sessions and CAPTCHA state.
- Refreshes the portal with one tap.
- Extracts the active page text and sends a formatted A4 report to Android's native print dialog, where it can be saved as PDF or sent to a printer.

## Build

The project targets Android SDK 34, supports Android 7.0 (API 24) and newer, and uses Java 17-compatible Gradle settings.

GitHub Actions runs automatically on every branch push:

1. Sets up Temurin Java 17.
2. Installs Gradle 8.2.1.
3. Runs `gradle assembleDebug`.
4. Uploads the APK as the `WB-Land-App` workflow artifact.

To build locally, install Android SDK 34 and Gradle 8.2.1, then run:

```bash
gradle assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.