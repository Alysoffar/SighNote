# Jetpack Ink API Notes & Architecture

## 1. Version Pinning & Artifacts

- **androidx.ink Library Version**: `1.1.0-alpha12` (Pinned: November 2026)
- **androidx.input:input-motionprediction**: `1.0.1`

All `androidx.ink` artifacts must share the exact same version across the build:
1. `androidx.ink:ink-authoring`
2. `androidx.ink:ink-authoring-compose`
3. `androidx.ink:ink-brush`
4. `androidx.ink:ink-brush-compose`
5. `androidx.ink:ink-geometry`
6. `androidx.ink:ink-geometry-compose`
7. `androidx.ink:ink-rendering`
8. `androidx.ink:ink-strokes`
9. `androidx.ink:ink-storage`
10. `androidx.ink:ink-nativeloader`

---

## 2. Wet / Dry Ink Two-Phase Architecture

Jetpack Ink splits inking into two rendering paths:

```
[Stylus MotionEvent]
        │
        ▼
[Input Filtering Layer] (Reject finger from drawing; detect stylus & eraser)
        │
        ├─────────────────────────────────────────────────┐
        ▼                                                 ▼
[Wet Ink Layer: InProgressStrokes]              [Dry Ink Layer: CanvasStrokeRenderer]
- Front-buffered / low-latency path             - Standard double-buffered Android Canvas
- Live stroke prediction & smoothing            - Renders finalized immutable Stroke objects
- Renders active stroke while stylus touches    - Re-renders during pan / zoom / viewport changes
        │                                                 ▲
        └──────────────(On Stroke Finished)───────────────┘
                                │
                                ▼
                     [InkStrokeMapper (in :ink)]
                                │
                                ▼
                       [:model Domain Stroke]
                                │
                                ▼
                       [Room Persistence & Autosave]
```

### InProgressStrokes Composable
- `InProgressStrokes` captures live pointer events and draws wet ink directly into a low-latency front-buffer.
- Required callbacks:
  - `defaultBrush: Brush`
  - `nextBrush: () -> Brush`
  - `onStrokesFinished: (List<Stroke>) -> Unit`
- Transform matrix: Uses `strokeToScreenTransform` and `screenToStrokeTransform` represented as 16-element FloatArrays (`Matrix.values`).
- When the stylus lifts, `onStrokesFinished` emits the finalized `androidx.ink.strokes.Stroke` vector objects.

### CanvasStrokeRenderer (Dry Ink)
- Created via `CanvasStrokeRenderer.create()`.
- Dry strokes are drawn on a standard Android Canvas via `renderer.draw(canvas, stroke, strokeToScreenMatrix)`.
- The matrix passed to `renderer.draw` is an `android.graphics.Matrix` (not Compose Matrix), converting page-space coordinates to physical screen pixels.

---

## 3. Input Handling & Tool Type Demuxing

Stylus interactions are segregated by `MotionEvent.getToolType(pointerIndex)`:
- `TOOL_TYPE_STYLUS` / `PointerType.Stylus`:
  - **Draws**: Passed to `InProgressStrokes` to generate wet ink with pressure sensitivity.
  - S Pen side button (`MotionEvent.BUTTON_STYLUS_PRIMARY` or `BUTTON_SECONDARY`): Erases strokes intersected by pointer.
- `TOOL_TYPE_ERASER` / `PointerType.Eraser`:
  - **Erases**: Stylus tail-end or dedicated hardware eraser tool erases whole strokes at touch coordinates.
- `TOOL_TYPE_FINGER` / `PointerType.Touch`:
  - **Pans and Pinch-Zooms**: Drives viewport translation (`translateX`, `translateY`) and scale (`zoom`).
  - **Never draws**: Touch events are filtered out upstream so accidental finger touches cannot leave ink marks.
- `ACTION_CANCEL` & `FLAG_CANCELED`:
  - Palm rejection: Android OS marks palm touches with `FLAG_CANCELED` or issues `ACTION_CANCEL`. InProgressStrokes cancels active wet strokes, and cancelled strokes are discarded without calling `onStrokesFinished`.

---

## 4. Isolation of androidx.ink Types

To adhere to strict architectural boundaries:
- **No `androidx.ink.*` types leak outside the `:ink` module.**
- `InkStrokeMapper` converts `androidx.ink.strokes.Stroke` into `com.notes.model.Stroke`.
- The `:app` module interacts with the canvas via `InkCanvasComposable` and `InkCanvasViewModel`, depending only on `:model` domain objects.

---

## 5. API Gotchas & Observations

1. **Transform Types**:
   - `InProgressStrokes` accepts Compose `float[]` (16-element 4x4 matrix from `Matrix.values`).
   - `CanvasStrokeRenderer.draw` accepts `android.graphics.Matrix` (3x3 affine matrix) or `androidx.ink.geometry.AffineTransform`.
2. **Brush Creation Constraints**:
   - `Brush.createWithComposeColor(...)` requires `epsilon > 0` and `epsilon <= size`. Standard setting: `size = 8f, epsilon = 0.1f`.
3. **StrokeInput Tilt Coordinates**:
   - `StrokeInput` exposes `tiltRadians` (single angle from perpendicular), not separate `tiltX` and `tiltY`. In our mapper, tilt angle in degrees is computed as `Math.toDegrees(tiltRadians.toDouble()).toFloat()`.
4. **Native Loader & JNI Constraints**:
   - Jetpack Ink relies on native C++ libraries packaged within `ink-nativeloader` (`libink.so`). Hardware acceleration must be enabled in `AndroidManifest.xml` (`android:hardwareAccelerated="true"`).
   - Because `libink.so` is built for Android ELF ABI targets (`arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`), host JVM unit tests on Windows/macOS/Linux throw `UnsatisfiedLinkError` when instantiating `StockBrushes` or native ink strokes directly without mock/stubbing. Tests asserting native ink C++ lifecycle run via instrumented `androidTest` on an Android device or emulator.
