# Canvas Note & AI Tutor Architecture

A high-performance Android stylus handwriting canvas combined with an asynchronous math tutor backend. The client captures low-latency digital ink with strict page-space coordinate transforms, while the backend verifies symbolic mathematical derivations and delivers anchored pedagogical guidance over WebSockets.

---

## Architecture Overview

```
                 +--------------------------------------------+
                 |            Android Tablet Client           |
                 |                                            |
                 |  +--------------------+  (Front-Buffered)  |
                 |  | InProgressStrokes  | - Low Latency Ink  |
                 |  +--------------------+                    |
                 |            | (On Finished)                 |
                 |  +--------------------+                    |
                 |  | CanvasStrokeRender | - Page-space conc. |
                 |  +--------------------+                    |
                 |            |                               |
                 |  +--------------------+                    |
                 |  | Room DB (Page-Spc) |                    |
                 |  +--------------------+                    |
                 +--------------------------------------------+
                                     |
                                     | WebSocket (JSON Schema Envelope)
                                     v
                 +--------------------------------------------+
                 |            Python Tutor Service            |
                 |                                            |
                 |   FastAPI / Uvicorn + Pydantic v2 Models   |
                 |   - Real-time stroke batch validation      |
                 |   - Anchored action generation (underline) |
                 |   - Monotonic page revision conflict guards|
                 +--------------------------------------------+
```

---

## Key Technical Specifications

### 1. Unified Page-Space Coordinate Pipeline
- All strokes, line bounds, and UI annotations are strictly modeled in logical page-space units.
- **Input Pipeline**: Raw hardware `MotionEvent` screen coordinates are transformed using the inverse viewport matrix:
  $$\text{Page} = (\text{Viewport})^{-1} \times \text{Screen}$$
- **Render Pipeline**: Dry ink strokes and background elements are rendered via matrix concatenation:
  $$\text{Screen} = \text{Viewport} \times \text{Page}$$
- **Front-Buffered In-Progress Ink**: Integrated with Android Jetpack Ink (`androidx.ink.authoring`), binding `motionEventToWorldTransform` directly to the active inverse viewport matrix to guarantee zero spatial drift or jump during wet-to-dry stroke transitions.

### 2. Bidirectional Streaming Contract
- Strict contract enforcement using JSON Schema definitions under `/protocol`.
- Every client envelope carries a monotonic `page_rev` counter to protect against out-of-order execution or stale agent feedback.
- Tutor actions anchor directly to semantic identifiers (`line_id` + `fallback_bbox`) to ensure visual alignment regardless of viewport scaling.

---

## Directory Structure

```
.
├── .github/
│   └── workflows/
│       ├── android.yml        # Android build, lint, and unit test CI
│       └── backend.yml        # Python verification and pytest suite
├── android/                   # Multi-module Kotlin/Compose project
│   ├── app/                   # App shell, navigation, and benchmark collection UI
│   ├── ink/                   # Jetpack Ink authoring, Room DB, rendering
│   ├── model/                 # Pure Kotlin domain data classes
│   ├── net/                   # WebSocket client & serialization
│   └── recognition/           # Stroke grouping & mathematical recognition adapters
├── backend/                   # Python FastAPI & Agentic reasoning service
│   ├── agent/                 # Tutor agent orchestration
│   ├── protocol/              # Pydantic v2 schemas reflecting /protocol
│   ├── server/                # FastAPI application & WebSocket handlers
│   └── tests/                 # Contract & integration tests
├── protocol/                  # Shared JSON schemas and test fixtures
└── docs/                      # Architectural specifications & decision memos
    ├── recognition-decision.md        # Phase 2 Recognizer evaluation spike memo
    ├── myscript-setup.md              # MyScript iink SDK licensing & integration guide
    └── recognition_evaluation_report.* # Benchmark results from tablet execution
```

---

## Handwriting-Math Recognition Spike (Phase 2)

The `:recognition` module provides a swappable, asynchronous mathematical handwriting recognition engine evaluated across on-device and cloud candidates:

### Architecture
- **Unified Domain Contract** (`Recognizer`): Accepts logical page-space `Stroke`s and produces normalized LaTeX outputs with symbol-level and expression-level confidences.
- **Line Clustering** (`LineGrouper`): Rule-based clustering of 2D strokes into mathematical expressions using vertical baseline projections, fraction bar bounding heuristics, and superscript/subscript proximity.
- **Pluggable Adapters**:
  - `Google ML Kit Digital Ink`: Zero-cost on-device handwriting model.
  - `Mathpix Cloud REST API`: High-precision math OCR using `/v3/strokes` (optional, credentials via `local.properties`).
  - `MyScript iink SDK`: Production on-device math engine skeleton.

### Evaluation & CROHME Benchmark
The repository bundles 100 mathematical handwriting test expressions from the official **CROHME / MathWriting** online dataset (`crohme_benchmark_samples.json`) spanning 7 mathematical categories.

#### Tablet Benchmark Results (Galaxy Tab S10+):
| Recognizer | Deployment | Exact Match (%) | Symbol Acc (%) | p50 Latency |
| :--- | :---: | :---: | :---: | :---: |
| **Google ML Kit Digital Ink** | On-Device | **2.0%** | **33.6%** | **14 ms** |
| **Mathpix Cloud** | Cloud | *Key required* | *Key required* | ~300 ms (est) |
| **MyScript iink** | On-Device | *SDK required* | *SDK required* | ~40 ms (est) |

*Detailed breakdown, category analysis, and future custom model roadmap: see [docs/recognition-decision.md](file:///d:/WORK/projects/Notes/docs/recognition-decision.md).*

---

## Getting Started

### Prerequisites
- **Android SDK**: API 34+ / minSdk 30
- **JDK**: Java 17+
- **Python**: 3.10+

### Backend Setup
```bash
cd backend
python -m venv .venv
source .venv/bin/activate  # Or on Windows: .venv\Scripts\activate
pip install -e ".[dev]"
pytest -v
uvicorn backend.server.app:app --host 0.0.0.0 --port 8000 --reload
```

### Android Client Setup
```bash
cd android
./gradlew :ink:testDebugUnitTest
./gradlew :app:assembleDebug
```

---

## Verification & Testing
- **Unit Tests**: Full transform round-trip invariance tests across 6 scale/pan configurations.
- **Instrumented Tests**: Wet-to-dry stroke coordinate equality checks under active zoom and panning.
- **Stress Testing**: 2,000+ synthetic procedural strokes evaluated against a 120 FPS target budget.
