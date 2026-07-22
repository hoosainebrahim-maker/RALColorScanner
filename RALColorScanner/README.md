# RAL Color Scanner

An Android app that uses your phone's camera to identify the color of whatever
it's pointed at, and matches it to the closest **RAL Classic** shade (213
standard colors), showing the color name and RAL code in a panel at the
bottom of the screen.

## How it works

- **CameraX** streams a live preview to the screen.
- Every ~250 ms, the app reads a small square of pixels from the exact
  center of the preview (marked with a white circular reticle) and averages
  them to reduce sensor noise.
- The averaged RGB value is converted to **CIE L\*a\*b\*** color space and
  compared against all 213 RAL Classic colors using the **CIEDE2000**
  formula — the industry-standard way to measure color difference the way
  human eyes actually perceive it (much more accurate than comparing raw RGB
  numbers).
- The closest match's name and RAL code are shown in the bottom panel, with
  a live swatch of the detected color.
- **Tap anywhere on screen (or the button) to lock** the current reading in
  place; tap again to resume live scanning.

## Project structure

```
RALColorScanner/
├── app/
│   ├── build.gradle.kts              # App module config + dependencies
│   └── src/main/
│       ├── AndroidManifest.xml       # Camera permission + activity
│       ├── java/com/ralscanner/colordetector/
│       │   ├── MainActivity.kt       # Camera setup, sampling, UI updates
│       │   ├── ColorMatcher.kt       # RGB→Lab conversion + CIEDE2000 matching
│       │   ├── RalColor.kt           # Data model for one RAL color
│       │   └── RalColorDatabase.kt   # All 213 RAL Classic colors
│       └── res/
│           ├── layout/activity_main.xml
│           ├── values/               # strings, colors, theme
│           └── drawable/             # swatch, panel, reticle, button backgrounds
├── build.gradle.kts                  # Project-level Gradle config
├── settings.gradle.kts
└── gradle.properties
```

## Building the app — no software install, entirely in your browser

This project includes a GitHub Actions workflow (`.github/workflows/build-apk.yml`)
that compiles a real, installable APK automatically in GitHub's free cloud
servers. You never install Android Studio, the SDK, or Gradle on your own
device. Steps:

1. **Create a free GitHub account** at [github.com/join](https://github.com/join)
   if you don't already have one.
2. **Create a new repository**: click the **+** icon (top right) → **New
   repository**. Give it any name (e.g. `ral-color-scanner`), leave it
   **Public** (simplest — public repos get unlimited free Actions minutes),
   and click **Create repository**. Don't add a README/gitignore on this
   screen — leave the repo empty.
3. **Upload the project**: unzip `RALColorScanner.zip` on your computer
   first. On the empty repo's page, click **uploading an existing file**
   (or **Add file → Upload files**). Drag the whole unzipped
   `RALColorScanner` folder onto the upload box — modern browsers (Chrome,
   Edge) preserve the folder structure when you drag a folder in. Scroll
   down and click **Commit changes**.
4. **Watch it build**: click the **Actions** tab at the top of the repo.
   You'll see a "Build APK" run start automatically (triggered by your
   upload). Click into it — it takes roughly 3–6 minutes. A green checkmark
   means success.
5. **Download the APK**: still on that finished run's page, scroll down to
   the **Artifacts** section and click **RAL-Color-Scanner-debug-apk** to
   download it. It downloads as a `.zip` — unzip it once to get
   `app-debug.apk`.
6. **Get it onto your phone**: send yourself that `.apk` file (email,
   Google Drive, WhatsApp, USB cable — any method works) and open it on
   your phone. Android will prompt you to allow installing from that
   source the first time — approve it, then tap **Install**.

If the build fails, click the failed step in the Actions log to see why —
paste the error back to me and I'll fix the project files.

## Building the app — with Android Studio (alternative)


1. **Open in Android Studio** (Giraffe / 2023.x or newer recommended):
   `File > Open` and select the `RALColorScanner` folder.
2. Let Android Studio sync Gradle (it will download the Gradle wrapper
   automatically the first time — you'll need an internet connection for
   this one-time step).
3. Connect your Android phone via USB with **Developer Options > USB
   debugging** enabled (or use a virtual device).
4. Click **Run ▶** in Android Studio, or from a terminal in the project
   folder run:
   ```
   ./gradlew installDebug
   ```
5. Grant the camera permission when prompted on first launch.

The app requires **Android 8.0 (API 26) or newer**.

### Building an installable APK without Android Studio

If you have the Android command-line SDK and Gradle installed, you can build
directly from a terminal:
```
./gradlew assembleDebug
```
The resulting APK will be at:
```
app/build/outputs/apk/debug/app-debug.apk
```
Copy that file to your phone and open it to install (you'll need to allow
"install from unknown sources" for your file manager/browser app).

## Notes on accuracy

Camera sensors, white balance, and ambient lighting all affect the RGB
values captured, so treat the RAL match as a close approximation rather
than a certified color reading — for that, a dedicated colorimeter/
spectrophotometer is needed. For best results, scan under neutral white
light and avoid strong colored ambient lighting or glare.
