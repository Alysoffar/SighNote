# MyScript iink SDK Setup & Licensing Guide

## Overview
The MyScript Interactive Ink (iink) SDK provides on-device, high-accuracy handwriting recognition for structured math expressions, converting freeform stylus strokes into LaTeX or MathML entirely offline.

Unlike cloud APIs or open-source libraries, MyScript distributes its binaries and recognition assets through proprietary licensing contracts. It is **not published on public Maven repositories** and requires individual developer certificates.

---

## Step-by-Step Setup Instructions

### 1. Register for a Developer Account
1. Visit the [MyScript Developer Portal](https://developer.myscript.com/).
2. Create an account and register a new Android application.
3. Note your package name (e.g. `com.notes.app`).

### 2. Download Developer Certificates & Keys
1. In your dashboard, generate an **Application Key** and an **HMAC Key / Certificate**.
2. Download the `MyCertificate.kt` or `MyCertificate.java` file provided by the dashboard.
3. Save your keys securely in `local.properties`:
   ```properties
   myscript.applicationKey=YOUR_APPLICATION_KEY
   myscript.hmacKey=YOUR_HMAC_KEY
   ```

### 3. Obtain SDK Binaries and Recognition Assets
1. Clone the reference implementation repository:
   ```bash
   git clone https://github.com/MyScript/interactive-ink-examples-android.git
   ```
2. Copy the proprietary `iink.aar` and native `.so` files (`armeabi-v7a`, `arm64-v8a`, `x86_64`) into a local library module in this repository (e.g., `android/recognition/libs/`).
3. Download the language asset packs:
   - For mathematical recognition, download the `math-gss.res` and `math-ak.res` recognition asset files from the MyScript portal.
   - Place them under `android/app/src/main/assets/recognition-assets/`.

### 4. Wire up the Adapter
Once the above steps are completed, update [MyScriptIInkRecognizer.kt](file:///d:/WORK/projects/Notes/android/recognition/src/main/kotlin/com/notes/recognition/adapters/MyScriptIInkRecognizer.kt):
1. Initialize the `com.myscript.iink.Engine` with your certificate.
2. Create a math `Editor` session:
   ```kotlin
   val package_ = engine.createPackage(packageFile)
   val part = package_.createPart("Math")
   editor.part = part
   ```
3. Map the input [Stroke] coordinates to `PointerEvent` sequences and invoke `editor.export_(editor.rootBlock, MimeType.LATEX)`.
4. Update `isAvailable()` to return `true` when engine initialization succeeds.
