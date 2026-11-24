package com.notes.recognition.adapters

import com.notes.model.Stroke
import com.notes.recognition.RecognitionResult
import com.notes.recognition.Recognizer
import com.notes.recognition.RecognizerDescriptor
import com.notes.recognition.SymbolConfidence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

/**
 * Cloud-based Mathpix OCR API integration using the v3/strokes endpoint.
 *
 * Designed specifically for handwritten mathematical notation, returning high-accuracy LaTeX.
 * API keys MUST be loaded from local.properties (or system environment), never committed.
 * If credentials are missing or empty, this adapter gracefully reports isAvailable() == false.
 */
class MathpixRecognizer(
    private val appId: String?,
    private val appKey: String?,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) : Recognizer {

    override val descriptor: RecognizerDescriptor = RecognizerDescriptor(
        id = "mathpix-cloud",
        displayName = "Mathpix Cloud API (v3/strokes)",
        isOfflineCapable = false,
        notes = "Cloud API specialized in handwritten mathematics. High LaTeX accuracy. Requires internet connection and API subscription."
    )

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun isAvailable(): Boolean {
        return !appId.isNullOrBlank() && !appKey.isNullOrBlank()
    }

    override suspend fun recognize(strokes: List<Stroke>): RecognitionResult {
        if (!isAvailable()) {
            return RecognitionResult(
                latex = "",
                confidence = 0f,
                rawResponse = "Mathpix API credentials not configured in local.properties"
            )
        }

        if (strokes.isEmpty()) {
            return RecognitionResult(latex = "", confidence = 1f, latencyMs = 0L)
        }

        return withContext(Dispatchers.IO) {
            // Build Mathpix v3/strokes payload format:
            // { "strokes": { "strokes": { "x": [[x1, x2, ...]], "y": [[y1, y2, ...]] } } }
            val xArrays = mutableListOf<List<Float>>()
            val yArrays = mutableListOf<List<Float>>()

            for (stroke in strokes) {
                val xs = mutableListOf<Float>()
                val ys = mutableListOf<Float>()
                for (pt in stroke.points) {
                    xs.add(pt.x)
                    ys.add(pt.y)
                }
                xArrays.add(xs)
                yArrays.add(ys)
            }

            val requestBodyJson = buildJsonObject {
                put("strokes", buildJsonObject {
                    put("strokes", buildJsonObject {
                        putJsonArray("x") {
                            for (arr in xArrays) {
                                add(buildJsonObject { }.let {
                                    // JsonArray of Numbers
                                    kotlinx.serialization.json.JsonArray(arr.map { kotlinx.serialization.json.JsonPrimitive(it) })
                                })
                            }
                        }
                        putJsonArray("y") {
                            for (arr in yArrays) {
                                add(buildJsonObject { }.let {
                                    kotlinx.serialization.json.JsonArray(arr.map { kotlinx.serialization.json.JsonPrimitive(it) })
                                })
                            }
                        }
                    })
                })
                putJsonArray("formats") {
                    add(kotlinx.serialization.json.JsonPrimitive("latex"))
                    add(kotlinx.serialization.json.JsonPrimitive("text"))
                }
            }.toString()

            val request = Request.Builder()
                .url("https://api.mathpix.com/v3/strokes")
                .header("app_id", appId!!)
                .header("app_key", appKey!!)
                .header("Content-Type", "application/json")
                .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
                .build()

            var responseBodyString = ""
            var recognizedLatex = ""
            var overallConfidence: Float? = null
            val symbolConfidences = mutableListOf<SymbolConfidence>()

            val latencyMs = measureTimeMillis {
                try {
                    client.newCall(request).execute().use { response ->
                        responseBodyString = response.body?.string().orEmpty()
                        if (response.isSuccessful && responseBodyString.isNotEmpty()) {
                            val parsed = json.parseToJsonElement(responseBodyString).jsonObject

                            // Extract latex output
                            recognizedLatex = parsed["latex"]?.jsonPrimitive?.content
                                ?: parsed["text"]?.jsonPrimitive?.content
                                ?: ""

                            // Confidence if present
                            overallConfidence = parsed["confidence"]?.jsonPrimitive?.floatOrNull
                                ?: parsed["confidence_rate"]?.jsonPrimitive?.floatOrNull

                            // If text contains delimiters, extract internal LaTeX
                            if (recognizedLatex.startsWith("$") && recognizedLatex.endsWith("$")) {
                                recognizedLatex = recognizedLatex.removeSurrounding("$").trim()
                            } else if (recognizedLatex.startsWith("\\[") && recognizedLatex.endsWith("\\]")) {
                                recognizedLatex = recognizedLatex.removeSurrounding("\\[", "\\]").trim()
                            }
                        } else {
                            recognizedLatex = ""
                        }
                    }
                } catch (e: Exception) {
                    return@withContext RecognitionResult(
                        latex = "",
                        confidence = 0f,
                        latencyMs = 0L,
                        rawResponse = "Network error: ${e.message}"
                    )
                }
            }

            RecognitionResult(
                latex = recognizedLatex,
                confidence = overallConfidence ?: if (recognizedLatex.isNotEmpty()) 0.9f else 0f,
                symbolConfidences = symbolConfidences,
                latencyMs = latencyMs,
                rawResponse = responseBodyString
            )
        }
    }
}
