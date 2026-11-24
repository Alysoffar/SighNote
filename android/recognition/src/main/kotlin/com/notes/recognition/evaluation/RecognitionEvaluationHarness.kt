package com.notes.recognition.evaluation

import com.notes.recognition.Recognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/**
 * Summary metrics for an evaluation run over a dataset.
 */
data class EvaluationSummary(
    val recognizerId: String,
    val recognizerName: String,
    val totalSamples: Int,
    val exactMatches: Int,
    val exactMatchAccuracy: Float,
    val meanSymbolAccuracy: Float,
    val p50LatencyMs: Long,
    val p95LatencyMs: Long,
    val isOfflineCapable: Boolean,
    val categoryBreakdown: Map<MathCategory, CategoryMetrics>
)

data class CategoryMetrics(
    val count: Int,
    val exactMatches: Int,
    val exactMatchAccuracy: Float,
    val meanSymbolAccuracy: Float,
    val p50LatencyMs: Long
)

/**
 * Runs benchmarks across all candidate recognizers and outputs Markdown + CSV reports.
 */
class RecognitionEvaluationHarness(
    private val recognizers: List<Recognizer>
) {

    suspend fun evaluate(samples: List<HandwrittenSample>): List<EvaluationSummary> {
        val summaries = mutableListOf<EvaluationSummary>()

        for (recognizer in recognizers) {
            if (!recognizer.isAvailable()) continue

            val latencies = mutableListOf<Long>()
            var exactMatches = 0
            var sumSymbolAccuracy = 0f

            val categoryResults = mutableMapOf<MathCategory, MutableList<SampleEvaluation>>()

            for (sample in samples) {
                val result = recognizer.recognize(sample.strokes)
                latencies.add(result.latencyMs)

                val exact = LatexNormalizer.isExactMatch(sample.groundTruthLatex, result.latex)
                if (exact) exactMatches++

                val symbolAcc = LatexNormalizer.computeSymbolAccuracy(sample.groundTruthLatex, result.latex)
                sumSymbolAccuracy += symbolAcc

                val sampleEval = SampleEvaluation(
                    sample = sample,
                    recognizedLatex = result.latex,
                    isExactMatch = exact,
                    symbolAccuracy = symbolAcc,
                    latencyMs = result.latencyMs
                )
                categoryResults.getOrPut(sample.category) { mutableListOf() }.add(sampleEval)
            }

            val total = samples.size
            val exactMatchAcc = if (total > 0) exactMatches.toFloat() / total.toFloat() else 0f
            val meanSymbolAcc = if (total > 0) sumSymbolAccuracy / total.toFloat() else 0f

            val sortedLatencies = latencies.sorted()
            val p50 = if (sortedLatencies.isNotEmpty()) sortedLatencies[(sortedLatencies.size * 0.50).toInt()] else 0L
            val p95 = if (sortedLatencies.isNotEmpty()) sortedLatencies[minOf(sortedLatencies.size - 1, (sortedLatencies.size * 0.95).toInt())] else 0L

            val breakdown = categoryResults.mapValues { (_, evals) ->
                val catExact = evals.count { it.isExactMatch }
                val catSymbolSum = evals.sumOf { it.symbolAccuracy.toDouble() }.toFloat()
                val catLatencies = evals.map { it.latencyMs }.sorted()
                val catP50 = if (catLatencies.isNotEmpty()) catLatencies[(catLatencies.size * 0.50).toInt()] else 0L

                CategoryMetrics(
                    count = evals.size,
                    exactMatches = catExact,
                    exactMatchAccuracy = if (evals.isNotEmpty()) catExact.toFloat() / evals.size.toFloat() else 0f,
                    meanSymbolAccuracy = if (evals.isNotEmpty()) catSymbolSum / evals.size.toFloat() else 0f,
                    p50LatencyMs = catP50
                )
            }

            summaries.add(
                EvaluationSummary(
                    recognizerId = recognizer.descriptor.id,
                    recognizerName = recognizer.descriptor.displayName,
                    totalSamples = total,
                    exactMatches = exactMatches,
                    exactMatchAccuracy = exactMatchAcc,
                    meanSymbolAccuracy = meanSymbolAcc,
                    p50LatencyMs = p50,
                    p95LatencyMs = p95,
                    isOfflineCapable = recognizer.descriptor.isOfflineCapable,
                    categoryBreakdown = breakdown
                )
            )
        }

        return summaries
    }

    /**
     * Exports evaluation report to Markdown and CSV files in the designated export directory.
     */
    suspend fun exportReports(
        summaries: List<EvaluationSummary>,
        outputDir: File
    ) = withContext(Dispatchers.IO) {
        outputDir.mkdirs()

        // 1. Generate Markdown Report
        val mdFile = File(outputDir, "recognition_evaluation_report.md")
        val mdContent = buildString {
            appendLine("# Handwriting Math Recognition Spike — Evaluation Report")
            appendLine()
            appendLine("Generated timestamp: ${System.currentTimeMillis()}")
            appendLine()
            appendLine("## Overall Summary")
            appendLine()
            appendLine("| Recognizer | Offline? | Exact Match (%) | Symbol Acc (%) | Latency p50 | Latency p95 | Notes |")
            appendLine("| :--- | :---: | :---: | :---: | :---: | :---: | :--- |")
            for (s in summaries) {
                appendLine(
                    "| ${s.recognizerName} | ${if (s.isOfflineCapable) "Yes" else "No"} | " +
                            "${(s.exactMatchAccuracy * 1000).roundToInt() / 10f}% | " +
                            "${(s.meanSymbolAccuracy * 1000).roundToInt() / 10f}% | " +
                            "${s.p50LatencyMs} ms | ${s.p95LatencyMs} ms | " +
                            "${recognizers.find { it.descriptor.id == s.recognizerId }?.descriptor?.notes} |"
                )
            }
            appendLine()
            appendLine("## Per-Category Breakdown")
            appendLine()
            for (s in summaries) {
                appendLine("### ${s.recognizerName}")
                appendLine()
                appendLine("| Category | Samples | Exact Match (%) | Symbol Acc (%) | p50 Latency |")
                appendLine("| :--- | :---: | :---: | :---: | :---: |")
                for ((cat, metrics) in s.categoryBreakdown) {
                    appendLine(
                        "| ${cat.displayName} | ${metrics.count} | " +
                                "${(metrics.exactMatchAccuracy * 1000).roundToInt() / 10f}% | " +
                                "${(metrics.meanSymbolAccuracy * 1000).roundToInt() / 10f}% | " +
                                "${metrics.p50LatencyMs} ms |"
                    )
                }
                appendLine()
            }
        }
        mdFile.writeText(mdContent)

        // 2. Generate CSV Report
        val csvFile = File(outputDir, "recognition_evaluation_report.csv")
        val csvContent = buildString {
            appendLine("recognizer_id,recognizer_name,category,total_samples,exact_matches,exact_match_pct,symbol_accuracy_pct,p50_latency_ms")
            for (s in summaries) {
                appendLine("${s.recognizerId},\"${s.recognizerName}\",ALL,${s.totalSamples},${s.exactMatches},${s.exactMatchAccuracy * 100f},${s.meanSymbolAccuracy * 100f},${s.p50LatencyMs}")
                for ((cat, m) in s.categoryBreakdown) {
                    appendLine("${s.recognizerId},\"${s.recognizerName}\",${cat.name},${m.count},${m.exactMatches},${m.exactMatchAccuracy * 100f},${m.meanSymbolAccuracy * 100f},${m.p50LatencyMs}")
                }
            }
        }
        csvFile.writeText(csvContent)
    }

    private data class SampleEvaluation(
        val sample: HandwrittenSample,
        val recognizedLatex: String,
        val isExactMatch: Boolean,
        val symbolAccuracy: Float,
        val latencyMs: Long
    )
}
