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
│   ├── app/                   # App shell, navigation, and top-level UI
│   ├── ink/                   # Jetpack Ink authoring, Room DB, rendering
│   ├── model/                 # Pure Kotlin domain data classes
│   ├── net/                   # WebSocket client & serialization
│   └── recognition/           # Stroke grouping & mathematical recognition
├── backend/                   # Python FastAPI & Agentic reasoning service
│   ├── agent/                 # Tutor agent orchestration
│   ├── protocol/              # Pydantic v2 schemas reflecting /protocol
│   ├── server/                # FastAPI application & WebSocket handlers
│   └── tests/                 # Contract & integration tests
├── protocol/                  # Shared JSON schemas and test fixtures
└── docs/                      # Architectural specifications
```

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
