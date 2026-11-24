# Handwriting Math Recognition Spike — Evaluation Report

Generated timestamp: 1795551287257

## Overall Summary

| Recognizer | Offline? | Exact Match (%) | Symbol Acc (%) | Latency p50 | Latency p95 | Notes |
| :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| Google ML Kit Digital Ink (On-Device) | Yes | 2.0% | 33.6% | 14 ms | 29 ms | On-device, zero cost. Trained for natural language text, not structured LaTeX math. May perform poorly on fractions, radicals, and matrices. |

## Per-Category Breakdown

### Google ML Kit Digital Ink (On-Device)

| Category | Samples | Exact Match (%) | Symbol Acc (%) | p50 Latency |
| :--- | :---: | :---: | :---: | :---: |
| Systems of Equations | 7 | 0.0% | 62.7% | 12 ms |
| Exponents | 38 | 0.0% | 32.7% | 15 ms |
| Messy / Rapid Writing | 10 | 20.0% | 49.9% | 13 ms |
| Subscripts | 25 | 0.0% | 30.3% | 13 ms |
| Fractions | 17 | 0.0% | 16.4% | 18 ms |
| Roots & Radicals | 1 | 0.0% | 31.8% | 14 ms |
| Greek Letters | 2 | 0.0% | 55.0% | 13 ms |

