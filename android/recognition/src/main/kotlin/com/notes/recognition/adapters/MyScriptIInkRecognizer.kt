package com.notes.recognition.adapters

import com.notes.model.Stroke
import com.notes.recognition.RecognitionResult
import com.notes.recognition.Recognizer
import com.notes.recognition.RecognizerDescriptor

/**
 * Skeleton adapter for MyScript iink SDK (Interactive Ink).
 *
 * IMPORTANT & EXPLICIT STATUS:
 * NOT WIRED UP / SKELETON ONLY.
 * MyScript iink SDK requires developer account registration, commercial or evaluation certificate keys,
 * and downloading proprietary binary libraries (`.aar` / native `.so` files) that cannot be redistributed
 * publicly.
 *
 * For complete setup and license key generation instructions, see:
 * [docs/myscript-setup.md](file:///d:/WORK/projects/Notes/docs/myscript-setup.md).
 */
class MyScriptIInkRecognizer(
    private val applicationKey: String? = null,
    private val hmacKey: String? = null
) : Recognizer {

    override val descriptor: RecognizerDescriptor = RecognizerDescriptor(
        id = "myscript-iink",
        displayName = "MyScript iink SDK (On-Device, Skeleton)",
        isOfflineCapable = true,
        notes = "NOT WIRED UP. High-performance offline math recognition engine. Requires proprietary developer certificate, license keys, and native AAR integration (see docs/myscript-setup.md)."
    )

    override suspend fun isAvailable(): Boolean {
        // MyScript is currently not wired up because it requires manual developer license certificates.
        return false
    }

    override suspend fun recognize(strokes: List<Stroke>): RecognitionResult {
        return RecognitionResult(
            latex = "",
            confidence = 0f,
            latencyMs = 0L,
            rawResponse = "MyScript iink SDK is not wired up. Please follow docs/myscript-setup.md to acquire credentials and configure native binaries."
        )
    }
}
