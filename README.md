# Math Notebook AI Tutor — Foundation (Phase 0)

A native Android tablet math notebook app with an agentic AI tutor backend. The client captures freeform stylus strokes (S Pen) and streams structured canvas regions to the backend over WebSockets; the backend tutor analyzes algebraic steps and responds with anchored canvas actions (such as error underlines and pedagogical sticky notes).

---

## 1. Monorepo Layout

```
├── /protocol                 # JSON Schema files (Single source of truth for wire format)
│   ├── envelope.schema.json
│   ├── data_model.schema.json
│   ├── message.schema.json
│   ├── payloads/            # Individual payload schemas
│   └── fixtures/            # Contract test fixtures
│       ├── valid/           # Valid example messages for all types
│       └── invalid/         # Invalid example messages for all types
├── /android                  # Android client (Kotlin + Jetpack Compose, minSdk 30)
│   ├── app/                 # Main Android app & Compose UI
│   ├── model/               # Pure Kotlin domain data models (page-space)
│   ├── net/                 # OkHttp WebSocket client & kotlinx.serialization
│   ├── ink/                 # Native stylus canvas placeholder (Phase 1)
│   └── recognition/         # Stroke recognition placeholder (Phase 2)
├── /backend                  # Python backend (FastAPI, WebSockets, Pydantic)
│   ├── protocol/            # Pydantic models matching /protocol schemas
│   ├── server/              # FastAPI application with /ws endpoint
│   ├── verification/        # Math verification subpackage stub
│   ├── rag/                 # RAG / knowledge retrieval subpackage stub
│   ├── agent/               # Tutor agent subpackage stub
│   └── tests/               # pytest contract and server integration tests
├── /docs                     # Architecture & Protocol specifications
│   ├── architecture.md
│   └── protocol.md
└── README.md                 # Setup & running instructions
```

---

## 2. Core Architecture & Protocol Rules

1. **Page-Space Coordinates**: All domain coordinates (`Stroke`, `BoundingBox`, `Line`) use logical page-space units, never physical screen pixels.
2. **Strict Action Anchoring**: Every canvas action (`action.underline`, `action.sticky_note`) anchors to a semantic `line_id` **PLUS** a `fallback_bbox`, never raw coordinates alone.
3. **Revision Counter**: Every page tracks a monotonically increasing integer `revision_counter` (incremented on every edit). Every tutor action carries the `page_rev` it was computed against to prevent applying stale actions.
4. **WebSocket Envelope (v1)**:
   ```json
   {
     "v": 1,
     "type": "<string>",
     "id": "<uuid>",
     "session_id": "<string>",
     "page_rev": <int>,
     "payload": { ... }
   }
   ```

---

## 3. Getting Started

### Prerequisites
- **Python**: 3.10+ (tested on Python 3.13)
- **Java**: JDK 17 or higher
- **Android SDK**: API 30+ (Build-Tools 35+)
- **Node/uv/pip**: standard Python packaging

---

### Backend Setup & Execution

1. Navigate to `/backend`:
   ```bash
   cd backend
   ```
2. Install dependencies (or run in your virtual environment):
   ```bash
   pip install -e ".[dev]"
   ```
3. Run automated tests (including schema contract tests):
   ```bash
   pytest -v
   ```
4. Start the development server:
   ```bash
   uvicorn backend.server.app:app --host 0.0.0.0 --port 8000 --reload
   ```
   The WebSocket endpoint is available at `ws://localhost:8000/ws` (or `ws://10.0.2.2:8000/ws` from an Android emulator).

---

### Android Setup & Execution

1. Navigate to `/android`:
   ```bash
   cd android
   ```
2. Run unit and contract tests across all modules:
   ```bash
   ./gradlew test
   ```
3. Build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
4. Install and run on an Android tablet emulator or device:
   ```bash
   ./gradlew installDebug
   ```

---

## 4. End-to-End Verification (Acceptance Test)

1. Start the backend:
   ```bash
   uvicorn backend.server.app:app --port 8000
   ```
2. Launch the Android app on an emulator (which connects by default to `10.0.2.2:8000/ws`).
3. Tap the **"Send test region"** button on the Compose screen.
4. Verify the incoming log cards display:
   - `[STATUS]` message (`code=analyzing`).
   - `[ACTION.UNDERLINE RECEIVED]` anchored to the submitted `line_id` with `fallback_bbox`, matching `page_rev`, `color="#E53935"`, and `style="wavy"`.

---

## 5. Contract Testing Details

Contract tests in both `/backend/tests/test_protocol_contracts.py` and `/android/net/src/test/kotlin/com/notes/net/ProtocolContractTest.kt` load the schemas from `/protocol`:
- Every fixture in `protocol/fixtures/valid/` is validated against `message.schema.json` and decoded into domain types.
- Every fixture in `protocol/fixtures/invalid/` is asserted to fail schema validation.
