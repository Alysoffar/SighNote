package com.notes.recognition.adapters

import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.notes.model.Stroke
import com.notes.recognition.RecognitionResult
import com.notes.recognition.Recognizer
import com.notes.recognition.RecognizerDescriptor
import com.notes.recognition.SymbolConfidence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

/**
 * On-device handwriting recognition using Google ML Kit Digital Ink Recognition.
 *
 * NOTE & HONEST APPRAISAL:
 * ML Kit's models (e.g. "en") are trained for standard cursive/print natural language text
 * and simple shapes, NOT nested mathematical notation (fractions, exponents, radicals, matrices).
 * It returns plain text unicode rather than structured LaTeX. Math expressions like fractions or
 * exponents will be recognized as sequential characters (e.g. "x2" instead of "x^2", "1/2" instead of "\\frac{1}{2}").
 * This candidate is evaluated as an on-device, zero-cost, privacy-first baseline.
 */
class MlKitDigitalInkRecognizer(
    private val languageTag: String = "en"
) : Recognizer {

    override val descriptor: RecognizerDescriptor = RecognizerDescriptor(
        id = "mlkit-digital-ink",
        displayName = "Google ML Kit Digital Ink (On-Device)",
        isOfflineCapable = true,
        notes = "On-device, zero cost. Trained for natural language text, not structured LaTeX math. May perform poorly on fractions, radicals, and matrices."
    )

    private val remoteModelManager = RemoteModelManager.getInstance()
    private var modelIdentifier: DigitalInkRecognitionModelIdentifier? = null
    private var model: DigitalInkRecognitionModel? = null
    private var recognizer: DigitalInkRecognizer? = null

    init {
        try {
            modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(languageTag)
            val id = modelIdentifier
            if (id != null) {
                val m = DigitalInkRecognitionModel.builder(id).build()
                model = m
                recognizer = DigitalInkRecognition.getClient(
                    DigitalInkRecognizerOptions.builder(m).build()
                )
            }
        } catch (_: Exception) {
            // Model tag initialization error
        }
    }

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        val targetModel = model ?: return@withContext false
        try {
            val isDownloaded = Tasks.await(
                remoteModelManager.isModelDownloaded(targetModel),
                10,
                TimeUnit.SECONDS
            )
            if (!isDownloaded) {
                Tasks.await(
                    remoteModelManager.download(targetModel, DownloadConditions.Builder().build()),
                    60,
                    TimeUnit.SECONDS
                )
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun recognize(strokes: List<Stroke>): RecognitionResult = withContext(Dispatchers.IO) {
        if (strokes.isEmpty()) {
            return@withContext RecognitionResult(latex = "", confidence = 1f, latencyMs = 0L)
        }

        val client = recognizer
            ?: return@withContext RecognitionResult(
                latex = "",
                confidence = 0f,
                rawResponse = "ML Kit Recognizer not initialized"
            )

        // Convert page-space strokes to ML Kit Ink object
        val inkBuilder = Ink.builder()
        for (stroke in strokes) {
            val strokeBuilder = Ink.Stroke.builder()
            for (pt in stroke.points) {
                strokeBuilder.addPoint(
                    Ink.Point.create(pt.x, pt.y, pt.t)
                )
            }
            inkBuilder.addStroke(strokeBuilder.build())
        }
        val ink = inkBuilder.build()

        var recognizedText = ""
        var score: Float? = null

        val latencyMs = measureTimeMillis {
            try {
                val recognitionResult = Tasks.await(client.recognize(ink), 15, TimeUnit.SECONDS)
                if (recognitionResult.candidates.isNotEmpty()) {
                    val topCandidate = recognitionResult.candidates[0]
                    recognizedText = topCandidate.text ?: ""
                    score = topCandidate.score
                }
            } catch (e: Exception) {
                return@withContext RecognitionResult(
                    latex = "",
                    confidence = 0f,
                    latencyMs = 0L,
                    rawResponse = "Inference error: ${e.message}"
                )
            }
        }

        val normalizedConfidence = score?.let { s ->
            (1.0f / (1.0f + kotlin.math.exp(-s.toDouble()))).toFloat()
        }

        val symbolConfidences = recognizedText.map { char ->
            SymbolConfidence(char.toString(), normalizedConfidence ?: 0.5f)
        }

        RecognitionResult(
            latex = recognizedText,
            confidence = normalizedConfidence,
            symbolConfidences = symbolConfidences,
            latencyMs = latencyMs,
            rawResponse = recognizedText
        )
    }
}
