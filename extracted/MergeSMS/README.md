# MergeSMS Android

A native Android starter app for CSV-based personalized bulk SMS. The app is configured for Java 17, Android Gradle Plugin 8.6.1, compile SDK 35, minimum Android 6.0 (API 23), and GitHub Actions APK builds.

## Features

- Import a CSV file from your phone storage.
- Use the first row as column headers.
- Select which column contains phone numbers.
- Select recipients individually or all at once (up to 1,000 data rows).
- Personalize messages using placeholders matching CSV column headers, e.g. `Hello {name}, your balance is {Balance}.`
- 500-character template limit.
- Requests Android SMS permission and uses the phone's SIM/carrier SMS service.
- Prompts to choose an active SIM when Android reports more than one active SIM.
- Branded MergeSMS launcher icon in all legacy Android densities plus adaptive/round launcher icons.
- Reusable vector icons for SMS, contacts, import, send, and confirmation.
- GitHub Actions workflow builds a debug APK.

## Important file format note

This build reads CSV text files, not native `.xlsx` workbook files. In Microsoft Excel on Android, open your workbook and use **Save as / Export → CSV** (wording varies by Excel version), then import the resulting `.csv` file into MergeSMS. Keep the first row as column headers, such as `name,phone no,Balance`.

Example:

```csv
name,phone no,Balance
Amina,0712345678,250
Brian,0723456789,1250
```

Template:

```text
Hello {name}, your balance is {Balance}.
```

Placeholder names must match the CSV header exactly, including capitalization and spaces. For example, if the header is `phone no`, the matching placeholder is `{phone no}`.

## Build and verify the APK

A successful APK build requires Gradle, JDK 17, Android SDK platform 35, and Android build-tools 35.0.0. This workspace does not have the Android SDK or Gradle installed and cannot reach the Gradle distribution host, so the APK cannot be compiled or device-tested in this environment. The project includes a GitHub Actions workflow to perform the real Android build and publish the debug APK as an artifact. Do not treat XML/static checks as proof that the APK builds or sends SMS correctly.

## Build the APK on GitHub using your phone

1. Create or open your GitHub repository.
2. Upload the contents of this ZIP into the repository root. Do not upload the ZIP alone.
3. Ensure `.github/workflows/build-android.yml` is present.
4. Commit the files to `main` (or `master`).
5. Open the repository's **Actions** tab and select **Build MergeSMS Android APK**.
6. Open the newest successful run and scroll to **Artifacts**.
7. Download `MergeSMS-debug-APK`. Extract the downloaded artifact ZIP to get `app-debug.apk`.
8. Open the APK on your Android phone and approve installation from that source if Android asks.

The workflow installs Android SDK platform/build tools and uses Gradle 8.7 through the Gradle build action. It does not require a Gradle wrapper file in the repository.

## SMS and safety notes

- The app sends through Android's SMS service and your SIM, not a paid SMS gateway. Your carrier's standard SMS charges, limits, fair-use rules, or spam controls may apply.
- Sending is not the same as confirmed delivery. This starter submits messages to Android's SMS service but does not track delivery reports.
- Start by testing with one number you control. Confirm the selected SIM, format, and carrier charges before sending to a large list.
- Android or device firmware may restrict SMS permissions or SIM selection. Dual-SIM behavior can differ by phone.
- This is a debug APK build for personal testing, not a Play Store release. Google Play has additional SMS permission policies.
- CSV parsing supports ordinary quoted CSV cells, commas, and escaped quotes; it is not a full spreadsheet parser.
- The 500-character limit applies to the template before merge and each rendered message. SMS billing is commonly based on SMS segments, so a long message can cost more than one SMS.

## Project structure

```text
MergeSMS/
├── .github/workflows/build-android.yml
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/mergesms/app/MainActivity.java
│       └── res/
│           ├── drawable/ (foreground and reusable vector icons)
│           ├── mipmap*/ (launcher icons for Android densities)
│           ├── mipmap-anydpi-v26/ (adaptive launcher icons)
│           └── values/ (theme and brand colors)
├── build.gradle
├── gradle.properties
├── settings.gradle
└── README.md
```

## Troubleshooting

- **Build fails:** check the Actions log for the first actual error; SDK package `tools` is intentionally not requested because it is obsolete.
- **APK artifact missing:** make sure the build job completed successfully and `app/build/outputs/apk/debug/app-debug.apk` was created.
- **File import fails:** export the spreadsheet as UTF-8 CSV and import that `.csv` file.
- **No SMS sent:** check SIM service, SMS permission, prepaid balance, and whether your carrier/device permits app-originated SMS.
