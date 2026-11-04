# FaceTVNews Android — News & Phone Tools

Release 1.0.0 · 5 October 2026 · McPerry Imaginations

This is an Android application project, not a Flutter project. It uses a bundled responsive HTML/JS interface with native Java networking, notifications, Android file-picker access and phone utilities. Article pages open in a separate in-app WebView which has **no native JavaScript bridge**.

The recovered FaceTV News idea is implemented here as one project. The UI now includes the recovered Home, Watch, Topics and You navigation while retaining saved stories, external feeds, notification preferences and the phone-tool screens. The implementation remains native Android plus the bundled WebView interface; the recovered Flutter APK is kept as a forensic artifact outside the build inputs.

## Included

Current update: version 1.0.3 (code 4). This update adds an emulator-verified AdMob test banner, the FaceTV logo and animated splash, a safe one-tap phone-care boost report, improved featured-image extraction, and a Trending desk that can use curated WordPress trends or rank available feeds locally.

- FaceTVNews WordPress API with paginated posts, featured images, categories and search.
- Native adaptive banner advertising with Google test IDs for development and configurable production IDs for release builds.
- RSS fallback on the unfiltered first page when the API is unavailable.
- Cached first-page headlines when both live retrieval routes fail; cached status is clearly labelled.
- Original article pages inside the app, with a share button. Publisher restrictions, advertisements and paywalls still apply.
- Saved article links on the device; this is not full-article offline archiving.
- Configurable external HTTPS RSS/Atom feeds, attribution and in-app original-publisher reading. No publisher is enabled automatically and no independently computed trending ranking is claimed.
- Opt-in scheduled Android headline notifications, persisted across reboot. No Firebase keys or backend service are needed for this polling version.
- RAM/storage information, this app's managed cache cleanup, links to Android storage/battery settings.
- Exact-duplicate finder for a folder explicitly granted through Android's picker; size grouping plus SHA-256 comparison; confirmation and identical-copy revalidation before deletion.
- SHA-256 calculation for a selected file and a consented VirusTotal report lookup. No file upload and no antivirus-engine verdict.

## Important boundaries

**This is a preview/test release.** It has been installed and exercised on the `McPerry_Native_Test` Android emulator. The emulator loaded the official AdMob test banner successfully. Live FaceTVNews API and RSS checks returned HTTP 502 from this environment; this does not establish that the website is down for your readers. Verify both endpoints from your network and check any WordPress/WAF rules that block the app.

There is no system-wide cache cleaner, task killer, guaranteed speed booster or full antivirus scanner. Clearing cache can save space but can also cause subsequent downloads. The security tool does not certify that a file or the phone is safe. Unknown VirusTotal fingerprints do not mean clean. To offer a genuine in-app antivirus verdict, integrate and license a security vendor's Android engine or approved analysis backend, along with coverage, update and privacy controls.

## Install the included test APK

`test-apk/FaceTVNews_v1.0.0_Test.apk` is provided for manual testing. It was compiled and signed with a newly generated test certificate, and its v2/v3 signatures were verified. It is not a Play Store release or an AAB.

1. Copy the APK to your Android phone and open it.
2. If prompted, allow that file-opening app to install this particular APK.
3. Launch FaceTVNews, check the live headlines and open several articles.
4. Enable notifications in Settings and allow Android's notification permission.
5. Use a disposable folder containing duplicate copies to test the duplicate finder; do not begin with irreplaceable files.

If a previously installed app uses this package name with another signing key, Android will reject an update. Do not uninstall an app containing needed local data merely to bypass that issue; build this project under a separate test applicationId instead.

The test APK was manually assembled against available Android API 35 platform resources, with targetSdk 36 and minSdk 26. The reproducible Gradle project is configured to compile and target API 36; run its normal build and device acceptance checks before release. The APK contains no native `.so` libraries.

## Build on Windows / Android Studio

1. Install Android Studio with JDK 17 and the Android SDK.
2. Open the folder containing `settings.gradle` as a project.
3. Install Android SDK Platform 36 and SDK Build Tools 35.0.0 through SDK Manager.
4. Let Gradle sync. The wrapper downloads Gradle 8.13 and uses Android Gradle Plugin 8.11.1.
5. From a terminal in this folder, run:

```bat
gradlew.bat assembleDebug
```

Your APK will be at `app\build\outputs\apk\debug\app-debug.apk`. The SDK location is normally written by Android Studio to `local.properties`. Do not commit that machine-specific file.

On Linux/macOS:

```sh
chmod +x gradlew
./gradlew assembleDebug
```

## Codemagic

Push this project to your own Git repository, connect it to Codemagic and select the `android-test` workflow in `codemagic.yaml`. It installs API 36 and produces a debug APK. No publishing, signing credentials or automatic store submission are configured.

For a signed release, use Android Studio's **Generate Signed Bundle / APK** with your own upload key. Keep the keystore and passwords outside this project. Register the correct applicationId, complete store disclosures and test the signed AAB before submitting it. If updating an existing FaceTVNews app, use that app's exact package ID, correct signing lineage and an incremented versionCode; the working ID here is `com.mcperry.facetvnews`.

This project also accepts a private root-level `keystore.properties` file based on `keystore.properties.example`. When all four values are present, `gradlew bundleRelease` signs the AAB with that upload key. Without it, Gradle produces an unsigned release bundle that cannot be uploaded to Google Play.

## Configure news

Edit `app/src/main/assets/sources.json`:

```json
{
  "site": "https://facetvnews.com",
  "name": "FaceTVNews",
  "api": "https://akomaconnect.facetvnews.com/?rest_route=/wp/v2",
  "feed": "https://facetvnews.com/feed/",
  "pollMinutes": 30,
  "sources": []
}
```

The core publisher must expose public posts through the WordPress REST API. This build uses `/?rest_route=/wp/v2` because the current host returns 404 for the pretty `/wp-json/wp/v2` paths; `_embed` supplies featured media and categories. The RSS fallback also recognizes media thumbnails, enclosures and image markup. Authentication is not implemented for private posts. Readers can use **Load more** to page through the public archive. RSS fallback only returns items supplied in the feed and cannot guarantee the complete archive, search or category filtering. WordPress category retrieval is capped at 100 categories.

External feeds are added per device in Settings. HTTPS RSS and Atom are supported, with up to five sources and 200 items per feed. These are latest feed items, not a live popularity index. Get any necessary publisher permissions and retain attribution. Full articles open the original source; this app does not republish third-party full text.

## Ads

Debug and default release builds use Google's official test application and banner IDs so emulator testing does not create invalid ad traffic. Before a production release, provide your own AdMob IDs and explicitly disable test mode:

```bat
gradlew.bat bundleRelease -PadmobAppId=ca-app-pub-your-app-id~your-app-id -PbannerAdUnitId=ca-app-pub-your-banner-id/your-banner-id -PadsUseTestIds=false
```

The production path includes the User Messaging Platform consent flow. Do not ship Google's test IDs as a monetized production configuration.

## Notifications

- Off until explicitly enabled.
- Android 13+ requires runtime notification permission.
- A first successful background check establishes the baseline without alerting old stories. Enabling schedules a baseline job, but the OS still controls when it runs.
- Default periodic interval is 30 minutes; values below 15 minutes are raised to 15 minutes.
- Checks fetch up to five pages of recent FaceTVNews posts, at most 100 stories per successful cycle. This is not a guarantee to notify every item during extended offline periods or very high publication volume.
- Up to five individual notifications per cycle; larger batches receive a summary.
- Android can delay jobs due to battery, Doze, network and device policies. Force-stopping the app can prevent jobs until it is opened again.
- For near-instant newsroom alerts, add an authenticated WordPress/server push service and Firebase Cloud Messaging. That integration is not included.

## Phone tools and privacy

The app requests Internet, network state, notifications and reboot scheduling permissions. It does not request broad all-files access, installed-app inventory, accessibility, contacts or a background-process-killing permission.

Duplicate scans are local and intentionally bounded: at most 3,000 files, 10 GB of selected file sizes, 30 directory levels and 512 MB per file. Empty, unknown-size, unreadable or larger files are skipped; the result is not a full-phone scan. Android restricts access to some protected directories. The file picker grants temporary URI access; no folder permission is retained across app restarts.

Selected deletion is permanent where the storage provider has no recycle bin. The app refuses selecting every copy in a group, and rehashes a retained copy and each selected file before deleting. A scan scope can omit copies outside the selected folder. Deleted-byte figures are estimates based on file sizes; cloud storage providers may not immediately reclaim local phone storage.

The cache tool clears this app's cache and cached first-page news copy. Saved links and alert preferences remain. The byte count reports only the managed cache directory; it does not include any separate WebView cache cleared by Android.

A fingerprint lookup opens VirusTotal in the isolated reader after a native consent dialog. Only the SHA-256 is submitted through the URL; it may identify the file. VirusTotal's policies, account requirements and report availability apply. No API key or file upload is embedded.

Saved links, user-selected sources, cached headlines and notification history are stored locally. Article pages load publisher cookies/analytics. Android backup is disabled in the manifest. Before public release, provide the final operator identity, support contact and privacy-policy URL, and complete accurate store Data Safety disclosures for both the app and embedded publishers.

## Validation performed

- Java source compilation against Android API classes, with generated resources for the test APK.
- JavaScript syntax check.
- Focused Java checks: HTTPS-only reader/source validation, managed cache byte accounting/deletion, and rejecting deletion of every duplicate copy or invalid tokens.
- Focused Node checks: bridge commands, escaping fetched titles, main/external story layouts, security scope wording and duplicate selection controls.
- Test APK assembly, DEX generation, package/permissions inspection and APK signature verification.

Not completed: a full Gradle build in this environment, emulator/device execution, live source fetching, end-to-end notification delivery, native file-provider interaction or visual rendering on a phone. Follow `DEVICE-ACCEPTANCE.md` before release. These tests do not replace independent security review.

To rerun the JavaScript checks:

```sh
node --check app/src/main/assets/app.js
node tests/UiChecks.cjs
```

`tests/CoreChecks.java` is a focused harness. Its recorded run used an Android API implementation jar for Java-only methods; a normal SDK stub jar cannot execute Android methods directly. Run or adapt it in a suitable Android/Robolectric test environment.

## Source references checked

- Android shared-document access: https://developer.android.com/training/data-storage/shared/documents-files
- Scheduled job constraints: https://developer.android.com/reference/android/app/job/JobInfo.Builder
- Android process restrictions: https://developer.android.com/about/versions/14/behavior-changes-all
- AGP 8.11 / API 36 / Gradle 8.13: https://developer.android.com/build/releases/agp-8-11-0-release-notes
- Play target requirements: https://support.google.com/googleplay/android-developer/answer/11926878
