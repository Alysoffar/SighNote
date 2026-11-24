package com.notes.app.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notes.ink.ui.InkCanvasView
import com.notes.model.Stroke
import com.notes.recognition.Recognizer
import com.notes.recognition.adapters.MathpixRecognizer
import com.notes.recognition.adapters.MlKitDigitalInkRecognizer
import com.notes.recognition.adapters.MyScriptIInkRecognizer
import com.notes.recognition.evaluation.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Properties
import java.util.UUID

/**
 * Debug-only data collection & evaluation screen.
 * Allows the user to:
 * 1. Cycle through ~200 mathematical prompt expressions across 10 categories.
 * 2. Handwrite each expression using the S Pen.
 * 3. Save the handwritten sample (strokes + ground truth LaTeX + category tag) to local JSON.
 * 4. Export all samples to a single JSON archive.
 * 5. Run the evaluation harness across enabled recognizers (ML Kit, Mathpix if key present, MyScript)
 *    and output formatted Markdown + CSV reports.
 */
@Composable
fun DataCollectionScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val json = remember { Json { prettyPrint = true; ignoreUnknownKeys = true } }

    var prompts by remember { mutableStateOf<List<MathPrompt>>(emptyList()) }
    var currentPromptIndex by remember { mutableIntStateOf(0) }
    var groundTruthInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(MathCategory.FRACTIONS) }

    val capturedStrokes = remember { mutableStateListOf<Stroke>() }
    var sampleCount by remember { mutableIntStateOf(0) }
    var isEvaluating by remember { mutableStateOf(false) }
    var evaluationSummaryText by remember { mutableStateOf<String?>(null) }

    // Load credentials from local.properties if present
    val (mathpixAppId, mathpixAppKey) = remember {
        loadMathpixCredentials(context)
    }

    // Candidate Recognizers
    val recognizers = remember(mathpixAppId, mathpixAppKey) {
        listOf<Recognizer>(
            MlKitDigitalInkRecognizer(languageTag = "en"),
            MathpixRecognizer(appId = mathpixAppId, appKey = mathpixAppKey),
            MyScriptIInkRecognizer()
        )
    }

    // Load bundled prompts from assets on launch
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val assetManager = context.assets
                val jsonString = assetManager.open("math_prompts.json").bufferedReader().use { it.readText() }
                val loadedPrompts = json.decodeFromString<List<MathPrompt>>(jsonString)
                withContext(Dispatchers.Main) {
                    prompts = loadedPrompts
                    if (loadedPrompts.isNotEmpty()) {
                        groundTruthInput = loadedPrompts[0].groundTruthLatex
                        selectedCategory = loadedPrompts[0].category
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Count existing saved samples
            val samplesDir = File(context.filesDir, "recognition_samples")
            samplesDir.mkdirs()
            val existing = samplesDir.listFiles { _, name -> name.endsWith(".json") }?.size ?: 0
            withContext(Dispatchers.Main) {
                sampleCount = existing
            }
        }
    }

    val currentPrompt = prompts.getOrNull(currentPromptIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onNavigateBack) {
                Text("Back to Notes")
            }
            Text(
                "Handwriting Math Recognition Spike",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                "Collected: $sampleCount samples",
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1976D2)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Prompt & Ground Truth Selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Prompt ${currentPromptIndex + 1} of ${prompts.size} [${selectedCategory.displayName}]",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF455A64)
                    )

                    Row {
                        Button(
                            onClick = {
                                if (currentPromptIndex > 0) {
                                    currentPromptIndex--
                                    prompts.getOrNull(currentPromptIndex)?.let {
                                        groundTruthInput = it.groundTruthLatex
                                        selectedCategory = it.category
                                    }
                                }
                            },
                            enabled = currentPromptIndex > 0
                        ) {
                            Text("Prev")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (currentPromptIndex < prompts.size - 1) {
                                    currentPromptIndex++
                                    prompts.getOrNull(currentPromptIndex)?.let {
                                        groundTruthInput = it.groundTruthLatex
                                        selectedCategory = it.category
                                    }
                                }
                            },
                            enabled = currentPromptIndex < prompts.size - 1
                        ) {
                            Text("Next")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = groundTruthInput,
                    onValueChange = { groundTruthInput = it },
                    label = { Text("Ground Truth LaTeX") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Handwriting Inking Area (Native View wrapped in AndroidView)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(2.dp, Color(0xFFCFD8DC), RoundedCornerShape(8.dp))
                .background(Color.White)
        ) {
            androidx.compose.ui.viewinterop.AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    InkCanvasView(ctx).apply {
                        this.onModelStrokeFinished = { modelStroke ->
                            capturedStrokes.add(modelStroke)
                        }
                    }
                },
                update = { view ->
                    view.strokes = capturedStrokes.toList()
                }
            )

            if (capturedStrokes.isEmpty()) {
                Text(
                    "Handwrite the expression above here using the S Pen...",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                Button(
                    onClick = { capturedStrokes.clear() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Clear Canvas")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (capturedStrokes.isEmpty()) {
                            Toast.makeText(context, "Write something first!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val sample = HandwrittenSample(
                            id = UUID.randomUUID().toString(),
                            timestamp = System.currentTimeMillis(),
                            category = selectedCategory,
                            groundTruthLatex = groundTruthInput.trim(),
                            strokes = capturedStrokes.toList()
                        )

                        coroutineScope.launch(Dispatchers.IO) {
                            val samplesDir = File(context.filesDir, "recognition_samples")
                            samplesDir.mkdirs()
                            val sampleFile = File(samplesDir, "${sample.id}.json")
                            sampleFile.writeText(json.encodeToString(sample))

                            val count = samplesDir.listFiles { _, name -> name.endsWith(".json") }?.size ?: 0
                            withContext(Dispatchers.Main) {
                                sampleCount = count
                                capturedStrokes.clear()
                                Toast.makeText(context, "Sample saved! Total: $count", Toast.LENGTH_SHORT).show()
                                // Auto advance to next prompt
                                if (currentPromptIndex < prompts.size - 1) {
                                    currentPromptIndex++
                                    prompts.getOrNull(currentPromptIndex)?.let {
                                        groundTruthInput = it.groundTruthLatex
                                        selectedCategory = it.category
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Save Sample (${capturedStrokes.size} strokes)")
                }
            }

            Row {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val assetManager = context.assets
                                val jsonString = assetManager.open("crohme_benchmark_samples.json").bufferedReader().use { it.readText() }
                                val benchmarkSamples = json.decodeFromString<List<HandwrittenSample>>(jsonString)
                                val samplesDir = File(context.filesDir, "recognition_samples")
                                samplesDir.mkdirs()
                                for (sample in benchmarkSamples) {
                                    val f = File(samplesDir, "${sample.id}.json")
                                    f.writeText(json.encodeToString(sample))
                                }
                                val count = samplesDir.listFiles { _, name -> name.endsWith(".json") }?.size ?: 0
                                withContext(Dispatchers.Main) {
                                    sampleCount = count
                                    Toast.makeText(context, "Loaded ${benchmarkSamples.size} CROHME benchmark samples! Total: $count", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Failed to load benchmark: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("Load CROHME (100)")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            val samplesDir = File(context.filesDir, "recognition_samples")
                            val files = samplesDir.listFiles { _, name -> name.endsWith(".json") } ?: emptyArray()
                            val allSamples = files.mapNotNull { f ->
                                try {
                                    json.decodeFromString<HandwrittenSample>(f.readText())
                                } catch (_: Exception) {
                                    null
                                }
                            }
                            val exportFile = File(context.getExternalFilesDir(null) ?: context.filesDir, "all_handwritten_samples.json")
                            exportFile.writeText(json.encodeToString(allSamples))
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Exported ${allSamples.size} samples to ${exportFile.name}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("Export Samples")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        isEvaluating = true
                        coroutineScope.launch(Dispatchers.IO) {
                            val samplesDir = File(context.filesDir, "recognition_samples")
                            val files = samplesDir.listFiles { _, name -> name.endsWith(".json") } ?: emptyArray()
                            val samples = files.mapNotNull { f ->
                                try {
                                    json.decodeFromString<HandwrittenSample>(f.readText())
                                } catch (_: Exception) {
                                    null
                                }
                            }

                            if (samples.isEmpty()) {
                                withContext(Dispatchers.Main) {
                                    isEvaluating = false
                                    Toast.makeText(context, "No samples collected yet to evaluate!", Toast.LENGTH_SHORT).show()
                                }
                                return@launch
                            }

                            val harness = RecognitionEvaluationHarness(recognizers)
                            val summaries = harness.evaluate(samples)
                            val outputDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "recognition_reports")
                            harness.exportReports(summaries, outputDir)

                            val displayText = summaries.joinToString("\n\n") { s ->
                                "${s.recognizerName}:\n  Exact Match: ${(s.exactMatchAccuracy * 100f)}%\n  Symbol Acc: ${(s.meanSymbolAccuracy * 100f)}%\n  p50: ${s.p50LatencyMs}ms, p95: ${s.p95LatencyMs}ms"
                            }

                            withContext(Dispatchers.Main) {
                                isEvaluating = false
                                evaluationSummaryText = displayText
                                Toast.makeText(context, "Reports exported to ${outputDir.absolutePath}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isEvaluating
                ) {
                    Text(if (isEvaluating) "Evaluating..." else "Run Evaluation Harness")
                }
            }
        }

        // Evaluation Summary display
        evaluationSummaryText?.let { summary ->
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Evaluation Complete", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(summary, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                }
            }
        }
    }
}

private fun loadMathpixCredentials(context: Context): Pair<String?, String?> {
    return try {
        val prop = Properties()
        // Try reading local.properties from file or BuildConfig/Asset
        val localPropFile = File(context.filesDir.parentFile, "local.properties")
        if (localPropFile.exists()) {
            localPropFile.inputStream().use { prop.load(it) }
        }
        val appId = prop.getProperty("mathpix.appId") ?: System.getenv("MATHPIX_APP_ID")
        val appKey = prop.getProperty("mathpix.appKey") ?: System.getenv("MATHPIX_APP_KEY")
        Pair(appId, appKey)
    } catch (_: Exception) {
        Pair(null, null)
    }
}
