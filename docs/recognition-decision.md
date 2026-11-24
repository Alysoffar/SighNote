# Handwriting-to-Math Recognition Decision Memo (Phase 2 Spike)

**Author:** Engineering Team  
**Status:** Evaluation Spike In Progress (Harness Ready)  
**Objective:** Compare candidate handwriting-to-LaTeX recognition engines fairly under unified real-world handwriting benchmarks to select an engine for the Math Notebook AI Tutor.

---

## 1. Candidate Overview

| Candidate | Deployment | Type | Math Specialization | Licensing / Cost |
| :--- | :--- | :--- | :--- | :--- |
| **Google ML Kit Digital Ink** | On-Device | Native Android SDK | Low (Text & Shapes) | Free / Zero cost |
| **Mathpix Cloud API** | Cloud (REST) | Hosted API (`v3/strokes`) | High (Dedicated Math OCR) | Subscription / Usage-based |
| **MyScript iink SDK** | On-Device | Native Embedded SDK | High (Dedicated Math Engine) | Commercial / Proprietary SDK |
| **Custom Stroke-to-LaTeX** | Hybrid / On-Device | Proprietary (e.g. CROHME-trained) | Very High (Custom Tuned) | High initial R&D; zero per-call cost |

---

## 2. Decision Criteria

1. **Accuracy on Real Handwriting**:
   - Evaluated across 10 core categories: Fractions, Exponents, Roots/Radicals, Subscripts, Greek Letters, Integrals, Summations/Limits, Systems of Equations, Matrices, and Messy Writing.
   - Evaluated by both **Normalized Whole-Expression Exact Match (%)** and **Mean Symbol Levenshtein Accuracy (%)**.
2. **Latency (p50 / p95)**:
   - Target budget for interactive step-level tutor feedback: $< 350\text{ ms}$ for real-time verification.
3. **Offline Capability & Privacy**:
   - Zero-leak student privacy mode; capability to verify math during exams or without connectivity.
4. **Cost & Commercial Predictability**:
   - Predictable cost scaling per user vs per-API call cost spikes.
5. **Swappability & Modularity**:
   - Clean architectural decoupling behind the `Recognizer` interface in `:recognition`.

---

## 3. Evaluation Results Summary (Populated from Benchmark Harness)

The benchmark was executed directly on the Samsung Galaxy Tab S10+ over 100 mathematical handwriting test expressions from the CROHME/MathWriting online stroke dataset.

| Candidate | Samples Tested | Exact Match (%) | Symbol Acc (%) | Latency p50 (ms) | Latency p95 (ms) | Offline? | Notes |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **Google ML Kit Digital Ink** | 100 | **2.0%** | **33.6%** | **14 ms** | **29 ms** | Yes | Free, blazing fast, but fails on 2D math layout (fractions, exponents). |
| **Mathpix Cloud** | 0 (Skipped) | *Pending API key* | *Pending API key* | ~300 ms (est) | ~600 ms (est) | No | Disabled until `MATHPIX_APP_ID` & `MATHPIX_APP_KEY` provided in `local.properties`. |
| **MyScript iink** | 0 (Unwired) | *Requires SDK* | *Requires SDK* | ~40 ms (est) | ~90 ms (est) | Yes | Adapter skeleton in place; see `docs/myscript-setup.md` for licensing setup. |

---

## 4. Per-Category Accuracy Breakdown

Empirical results from running ML Kit on device across categories:

| Category | Samples | ML Kit Exact Match (%) | ML Kit Symbol Acc (%) | ML Kit p50 Latency (ms) |
| :--- | :---: | :---: | :---: | :---: |
| Systems of Equations | 7 | 0.0% | 62.7% | 12 ms |
| Greek Letters | 2 | 0.0% | 55.0% | 13 ms |
| Messy / Rapid Writing | 10 | 20.0% | 49.9% | 13 ms |
| Exponents | 38 | 0.0% | 32.7% | 15 ms |
| Roots & Radicals | 1 | 0.0% | 31.8% | 14 ms |
| Subscripts | 25 | 0.0% | 30.3% | 13 ms |
| Fractions | 17 | 0.0% | 16.4% | 18 ms |

### Empirical Insights on ML Kit Digital Ink:
1. **Speed**: Extremely lightweight and responsive (14 ms p50, 29 ms p95 on Dimensity 9300+ / Snapdragon hardware).
2. **2D Spatial Failure**: Because ML Kit Digital Ink is trained for linear text transcription, it projects multi-level math (like $\frac{a}{b}$, $x^2$, $\sqrt{x}$) onto a single horizontal line, outputting jumbled characters rather than valid LaTeX commands like `\frac{...}{...}` or `^{...}`.
3. **Linear Accuracy**: On flat horizontal expressions without superscripts (like simple equations), character recognition reaches ~50-63% symbol recall, but exact match remains near zero.
4. **Conclusion**: ML Kit is **unsuitable as a primary math recognizer**, but excellent for linear text, scratch notes, or basic inline variable names.

---

## 5. Potential Future Custom Model (CROHME-style Stroke-to-LaTeX)

If commercial licensing or offline restrictions limit candidate adoption:
- **Dataset**: CROHME (Competition on Recognition of On-line Handwritten Mathematical Expressions) dataset (~12,000 online handwritten mathematical expressions with stroke coordinates and MathML/LaTeX ground truth).
- **Architecture**:
  - Encoder: 1D CNN / BiLSTM or Transformer processing normalized temporal stroke point sequences `(x, y, Δt, pen_up)`.
  - Spatial Visualizer: Lightweight ResNet backbone on the rendered ink image.
  - Decoder: Autoregressive LaTeX token decoder with attention over spatial and temporal representations.
- **Inference Target**: TensorFlow Lite / ONNX Runtime Mobile running on tablet NPU/GPU (`< 80\text{ ms}` latency budget).

---

## 6. Final Recommendation

*(To be written following empirical sample evaluation on tablet handwriting data).*
