# Measuring Input-to-Photon Latency & Capturing Perfetto Traces

This guide details how to verify the ultra-low-latency inking performance of the Notes tablet canvas.

---

## 1. What is Input-to-Photon Latency?

Input-to-photon latency is the elapsed time from when the physical S Pen tip makes contact or moves across the tablet digitizer glass to when the corresponding ink pixels physically illuminate on the display.

- **Human Perception Threshold**: ~20ms–30ms begins to feel disconnected, like "dragging ink behind a string".
- **Target Performance**: `<10ms` on 120Hz displays with hardware prediction (mimicking real pen and paper, similar to Samsung Notes).

---

## 2. 240 FPS Slow-Motion Camera Measurement

A 240 frames-per-second (fps) camera captures one frame every **4.167 milliseconds**:
$$\Delta t = \frac{1000\text{ ms}}{240\text{ fps}} \approx 4.167\text{ ms/frame}$$

### Equipment Setup
1. Smartphone or high-speed camera capable of recording 240fps or 960fps slow-motion video.
2. Rigid tripod or clamp holding the recording camera at an acute angle (~30°–45°) focused closely on the S Pen tip and screen surface.
3. High-contrast stylus stroke (e.g., black ink on a bright white background).

### Measurement Procedure
1. Start slow-motion recording at 240fps.
2. In one rapid, continuous motion, draw a sharp zigzag or diagonal line across the canvas.
3. Import the recorded clip into a frame-by-frame video player (e.g., QuickTime, VLC, or Adobe Premiere).
4. Identify **Frame 0 (Touch Contact/Motion)**: The exact frame where the stylus tip moves across the glass.
5. Step forward frame-by-frame and identify **Frame N (Photon Emission)**: The first video frame where new ink pixels begin to glow under or directly trailing the pen tip.
6. **Calculate Latency**:
   $$\text{Latency (ms)} = (N - 0) \times 4.167\text{ ms}$$
   *Example*: If ink appears 2 frames after motion, latency is $2 \times 4.167\text{ ms} \approx 8.33\text{ ms}$ (under 1 frame on a 120Hz refresh panel).

---

## 3. Capturing Perfetto Traces for Stylus & Rendering Analysis

Perfetto gives exact kernel, scheduling, input pipeline, and RenderThread metrics down to the microsecond.

### Step 1: Create Perfetto Configuration (`trace_config.pbtxt`)
```protobuf
buffers: {
    size_kb: 65536
    fill_policy: RING_BUFFER
}
data_sources: {
    config {
        name: "linux.ftrace"
        ftrace_config {
            ftrace_events: "sched/sched_switch"
            ftrace_events: "power/cpu_frequency"
            ftrace_events: "power/gpu_frequency"
            ftrace_events: "android/input_dispatch"
            ftrace_events: "android/input_event"
            atrace_categories: "gfx"
            atrace_categories: "input"
            atrace_categories: "view"
            atrace_categories: "res"
            atrace_apps: "com.notes.app"
        }
    }
}
duration_ms: 10000
```

### Step 2: Trigger Trace Recording via ADB
Connect the tablet via USB and run:
```bash
# Push config to device and record 10 seconds while drawing vigorously
adb shell perfetto -c - --txt -o /data/misc/perfetto-traces/ink_trace.perfetto-trace < trace_config.pbtxt

# Pull trace file to your computer
adb pull /data/misc/perfetto-traces/ink_trace.perfetto-trace ./ink_trace.perfetto-trace
```

### Step 3: Inspect in Perfetto UI
1. Open [https://ui.perfetto.dev](https://ui.perfetto.dev) in Chrome.
2. Drag and drop `ink_trace.perfetto-trace`.
3. Key tracks to examine:
   - **`InputDispatcher` / `InputReader`**: Timestamp when touch events are read from the digitizer driver to when they are dispatched to `com.notes.app`.
   - **`RenderThread`**: Look at `Choreographer#doFrame` and verify frame intervals stay under **8.33ms** (on 120Hz displays) or **16.67ms** (on 60Hz displays).
   - **Front-Buffer Low Latency Path**: Verify that `InProgressStrokes` executes in the immediate touch event loop rather than waiting for next frame VSYNC.
   - **Jank Detection**: Red flags indicate dropped frames or UI thread preemption.
