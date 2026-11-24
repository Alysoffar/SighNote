package com.notes.ink.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.compose.createWithComposeColor
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke as InkStroke
import com.notes.ink.mapping.InkStrokeMapper
import com.notes.ink.transform.ViewportTransform
import com.notes.model.Stroke as ModelStroke

/**
 * Native custom view combining low-latency front-buffered stylus wet ink (via InProgressStrokesView)
 * and double-buffered dry stroke rendering (via CanvasStrokeRenderer).
 *
 * Implements strict MotionEvent tool type demuxing:
 * - TOOL_TYPE_STYLUS: Draws with pressure-sensitive pen, or erases when barrel button is held.
 * - TOOL_TYPE_ERASER: Erases whole strokes.
 * - TOOL_TYPE_FINGER: Exclusively drives pan and pinch-to-zoom; never draws ink.
 * - ACTION_CANCEL: Discards active strokes (palm rejection).
 */
class InkCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val inProgressStrokesView = InProgressStrokesView(context)
    private val canvasStrokeRenderer = CanvasStrokeRenderer.create()
    private val inkStrokeCache = mutableMapOf<String, InkStroke>()

    private val ruledLinePaint = Paint().apply {
        color = 0x1F000000
        strokeWidth = 1f
        isAntiAlias = true
    }

    private val bgPaint = Paint().apply {
        color = 0xFFFCFCFC.toInt()
    }

    var currentBrush: Brush = Brush.createWithComposeColor(
        family = StockBrushes.pressurePen(),
        color = androidx.compose.ui.graphics.Color.Black,
        size = 6f,
        epsilon = 0.1f
    )

    var currentTool: CanvasTool = CanvasTool.PEN
    var transform: ViewportTransform = ViewportTransform()
        set(value) {
            field = value
            // InProgressStrokesView takes MotionEvents in view-space.
            // When motionEventToWorldTransform = screenToPageMatrix in startStroke,
            // InProgressStrokesView automatically computes:
            //   strokeToViewTransform = (motionEventToStrokeTransform)^-1 * motionEventToViewTransform
            //                         = pageToScreenMatrix * motionEventToViewTransform.
            // Therefore, motionEventToViewTransform MUST remain Identity (since MotionEvent coordinates
            // are already 1:1 with InProgressStrokesView coordinates).
            inProgressStrokesView.motionEventToViewTransform = android.graphics.Matrix()
            invalidate()
        }
    var pdfPath: String? = null
        set(value) {
            field = value
            invalidate()
        }
    var pdfPageIndex: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    // Debug crosshair at the latest raw pen position
    var showPenCrosshair: Boolean = true
    private var lastPenRawX: Float = -1f
    private var lastPenRawY: Float = -1f

    private val crosshairPaint = Paint().apply {
        color = 0xFFFF0000.toInt() // Red crosshair
        strokeWidth = 2f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    var strokes: List<ModelStroke> = emptyList()
        set(value) {
            field = value
            val currentIds = value.map { it.id }.toSet()
            inkStrokeCache.keys.retainAll(currentIds)
            invalidate()
        }

    var onStrokeFinished: ((InkStroke) -> Unit)? = null
    var onEraseAt: ((Float, Float) -> Unit)? = null
    var onPanAndZoom: ((Float, Float, Float) -> Unit)? = null

    private var activeStylusStrokeId: InProgressStrokeId? = null

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val factor = detector.scaleFactor
            if (factor > 0.1f && factor < 10f) {
                onPanAndZoom?.invoke(0f, 0f, factor)
            }
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            onPanAndZoom?.invoke(-distanceX, -distanceY, 1f)
            return true
        }
    })

    init {
        setWillNotDraw(false)

        addView(
            inProgressStrokesView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )

        inProgressStrokesView.addFinishedStrokesListener(object : InProgressStrokesFinishedListener {
            override fun onStrokesFinished(strokes: Map<InProgressStrokeId, InkStroke>) {
                for ((_, stroke) in strokes) {
                    onStrokeFinished?.invoke(stroke)
                }
                // Crucial: remove from InProgressStrokesView so they hand off to CanvasStrokeRenderer
                inProgressStrokesView.removeFinishedStrokes(strokes.keys)
                activeStylusStrokeId = null
                invalidate()
            }
        })
    }

    private fun resolveBrush(brushId: String): Brush {
        val color = when {
            brushId.contains("blue") -> androidx.compose.ui.graphics.Color(0xFF1E88E5)
            brushId.contains("red") -> androidx.compose.ui.graphics.Color(0xFFE53935)
            brushId.contains("highlighter_yellow") -> androidx.compose.ui.graphics.Color(0x66FFEB3B)
            brushId.contains("highlighter_blue") -> androidx.compose.ui.graphics.Color(0x6680D8FF)
            brushId.contains("highlighter_green") -> androidx.compose.ui.graphics.Color(0x66CCFF90)
            else -> androidx.compose.ui.graphics.Color(0xFF111111)
        }
        val size = when {
            brushId.contains("highlighter") -> 24f
            brushId.contains("size_4") || brushId.contains("size_3") -> 4f
            brushId.contains("size_16") || brushId.contains("size_12") -> 16f
            else -> 8f
        }
        return Brush.createWithComposeColor(
            family = StockBrushes.pressurePen(),
            color = color,
            size = size,
            epsilon = 0.1f
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Draw Background (White / Paper)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Draw PDF Page or Ruled Lines inside Page-Space Transformation
        canvas.save()
        val pageToScreenMatrix = transform.toPageToScreenAndroidMatrix()
        canvas.concat(pageToScreenMatrix)

        val currentPdfPath = pdfPath
        if (currentPdfPath != null) {
            val pdfBitmap = com.notes.ink.pdf.PdfManager.renderPageBitmap(currentPdfPath, pdfPageIndex, 1200, 1600)
            if (pdfBitmap != null) {
                canvas.drawBitmap(pdfBitmap, 0f, 0f, null)
            }
        } else {
            // Draw Ruled Lines in logical page space
            val lineSpacingPage = 40f
            val pageWidth = 2400f
            val totalLines = 200
            for (i in 1..totalLines) {
                val py = i * lineSpacingPage
                canvas.drawLine(0f, py, pageWidth, py, ruledLinePaint)
            }
        }

        // 3. Draw Dry Strokes in Page-Space (Canvas is already transformed by pageToScreenMatrix)
        // Pass identity matrix so CanvasStrokeRenderer utilizes the transformed Canvas coordinate space
        val identityMatrix = android.graphics.Matrix()
        val (highlighters, pens) = strokes.partition { it.brush_id.contains("highlighter") }

        for (stroke in highlighters) {
            val inkStroke = inkStrokeCache.getOrPut(stroke.id) {
                resolveBrush(stroke.brush_id).let { brush ->
                    InkStrokeMapper.toInkStroke(stroke, brush)
                }
            }
            canvasStrokeRenderer.draw(canvas, inkStroke, identityMatrix)
        }

        for (stroke in pens) {
            val inkStroke = inkStrokeCache.getOrPut(stroke.id) {
                resolveBrush(stroke.brush_id).let { brush ->
                    InkStrokeMapper.toInkStroke(stroke, brush)
                }
            }
            canvasStrokeRenderer.draw(canvas, inkStroke, identityMatrix)
        }

        canvas.restore()

        // 4. Debug Crosshair at the latest raw pen position (drawn in screen space)
        if (showPenCrosshair && lastPenRawX >= 0f && lastPenRawY >= 0f) {
            val chSize = 20f
            canvas.drawLine(lastPenRawX - chSize, lastPenRawY, lastPenRawX + chSize, lastPenRawY, crosshairPaint)
            canvas.drawLine(lastPenRawX, lastPenRawY - chSize, lastPenRawX, lastPenRawY + chSize, crosshairPaint)
            canvas.drawCircle(lastPenRawX, lastPenRawY, 6f, crosshairPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked

        // Check for palm rejection cancellation flag
        if ((event.flags and MotionEvent.FLAG_CANCELED) != 0) {
            val strokeId = activeStylusStrokeId
            if (strokeId != null) {
                inProgressStrokesView.cancelStroke(strokeId, event)
                activeStylusStrokeId = null
            }
            return true
        }

        // Scan all active pointers to find stylus or eraser
        var stylusIndex = -1
        var eraserIndex = -1

        for (i in 0 until event.pointerCount) {
            when (event.getToolType(i)) {
                MotionEvent.TOOL_TYPE_STYLUS -> {
                    stylusIndex = i
                    break
                }
                MotionEvent.TOOL_TYPE_ERASER -> {
                    eraserIndex = i
                    break
                }
            }
        }

        // -------------------------------------------------------------
        // Case 1: S Pen / Stylus Input (High Priority - Overrides Palm/Fingers)
        // -------------------------------------------------------------
        if (stylusIndex != -1) {
            val isButtonPressed = (event.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0 ||
                    (event.buttonState and MotionEvent.BUTTON_SECONDARY) != 0

            // If S Pen side button is pressed or Eraser mode is selected, erase
            if (isButtonPressed || currentTool == CanvasTool.ERASER) {
                if (action == MotionEvent.ACTION_DOWN ||
                    action == MotionEvent.ACTION_MOVE ||
                    action == MotionEvent.ACTION_POINTER_DOWN
                ) {
                    onEraseAt?.invoke(event.getX(stylusIndex), event.getY(stylusIndex))
                }
                return true
            }

            // Normal Stylus Drawing: Feed directly into InProgressStrokesView
            lastPenRawX = event.getX(stylusIndex)
            lastPenRawY = event.getY(stylusIndex)
            if (showPenCrosshair) {
                invalidate()
            }
            when (action) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (activeStylusStrokeId == null) {
                        val screenToPageMatrix = transform.toScreenToPageAndroidMatrix()
                        activeStylusStrokeId = inProgressStrokesView.startStroke(
                            event,
                            stylusIndex,
                            currentBrush,
                            screenToPageMatrix
                        )
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    val strokeId = activeStylusStrokeId
                    if (strokeId != null) {
                        inProgressStrokesView.addToStroke(event, stylusIndex, strokeId)
                    } else {
                        val screenToPageMatrix = transform.toScreenToPageAndroidMatrix()
                        activeStylusStrokeId = inProgressStrokesView.startStroke(
                            event,
                            stylusIndex,
                            currentBrush,
                            screenToPageMatrix
                        )
                    }
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_POINTER_UP -> {
                    val strokeId = activeStylusStrokeId
                    if (strokeId != null) {
                        inProgressStrokesView.finishStroke(event, stylusIndex, strokeId)
                        activeStylusStrokeId = null
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    val strokeId = activeStylusStrokeId
                    if (strokeId != null) {
                        inProgressStrokesView.cancelStroke(strokeId, event)
                        activeStylusStrokeId = null
                    }
                }
            }
            return true
        }

        // -------------------------------------------------------------
        // Case 2: Stylus Eraser End (TOOL_TYPE_ERASER)
        // -------------------------------------------------------------
        if (eraserIndex != -1) {
            if (action == MotionEvent.ACTION_DOWN ||
                action == MotionEvent.ACTION_MOVE ||
                action == MotionEvent.ACTION_POINTER_DOWN
            ) {
                onEraseAt?.invoke(event.getX(eraserIndex), event.getY(eraserIndex))
            }
            return true
        }

        // -------------------------------------------------------------
        // Case 3: Finger Input (TOOL_TYPE_FINGER) -> Pan & Zoom Only
        // -------------------------------------------------------------
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }
}
