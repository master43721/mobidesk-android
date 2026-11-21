package com.mobidesk.android.rdp

import android.content.Context
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.freerdp.freerdpcore.presentation.SessionView
import com.freerdp.freerdpcore.services.LibFreeRDP
import com.freerdp.freerdpcore.utils.Mouse
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Dedicated, crash-safe touch controller for FreeRDP sessions (v0.6.0).
 *
 * Capabilities:
 * 1. Two live input modes:
 *    - [InputMode.TRACKPAD]: Laptop touchpad feel with a free virtual cursor (single Windows pointer).
 *      - 1-finger relative movement with sensitivity.
 *      - 1-finger short tap: left click.
 *      - 1-finger double-tap & drag: left-button drag.
 *    - [InputMode.DIRECT_TOUCH]: Direct touch mapping accounting for zoom and pan.
 * 2. Smooth 2-finger pinch-to-zoom (1.0x to 3.0x) and pan clamping on [sessionView].
 * 3. 2-finger tap: right click.
 * 4. 2-finger parallel vertical drag: smooth mouse wheel scroll.
 * 5. Single accurate cursor: No secondary overlay; coordinates strictly clamped to desktop bounds.
 *
 * Invariant: Consumes 100% of MotionEvents (always returns true) so multi-touch NEVER
 * falls through to legacy SessionView / DoubleGestureDetector / ScrollView2D.
 * All LibFreeRDP calls are guarded with instance != 0L and try/catch.
 */
class RdpTouchController(
    private val context: Context,
    private val sessionView: SessionView,
    private val instanceProvider: () -> Long
) : View.OnTouchListener {

    companion object {
        private const val TAG = "RdpTouchController"
        private const val TAP_TIMEOUT_MS = 250L
        private const val DOUBLE_TAP_TIMEOUT_MS = 320L
        private const val MAX_TAP_SLOP_DP = 24f
        private const val SCROLL_STEP_DP = 16f
        private const val TRACKPAD_SENSITIVITY = 1.4f
        private const val MIN_ZOOM = 1.0f
        private const val MAX_ZOOM = 3.0f
    }

    enum class InputMode {
        TRACKPAD,
        DIRECT_TOUCH
    }

    private enum class TwoFingerGesture {
        UNDECIDED,
        PINCH_ZOOM_PAN,
        SCROLL
    }

    interface ModeListener {
        fun onModeChanged(mode: InputMode)
    }

    var modeListener: ModeListener? = null

    var mode: InputMode = InputMode.TRACKPAD
        set(value) {
            field = value
            modeListener?.onModeChanged(value)
        }

    private var desktopWidth: Int = 1280
    private var desktopHeight: Int = 720

    // Virtual cursor in desktop coordinate space [0, desktopWidth - 1], [0, desktopHeight - 1]
    var cursorX: Float = 640f
        private set
    var cursorY: Float = 360f
        private set

    // Pinch-to-zoom & Pan state
    var userZoom: Float = 1.0f
        private set
    var userPanX: Float = 0f
        private set
    var userPanY: Float = 0f
        private set

    // Touch metrics in physical pixels
    private val density = context.resources.displayMetrics.density
    private val maxTapSlopPx = MAX_TAP_SLOP_DP * density
    private val scrollStepPx = SCROLL_STEP_DP * density
    private val touchSlopPx = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

    // 1-finger tracking
    private var isOneFingerDown = false
    private var downTime1F = 0L
    private var downX1F = 0f
    private var downY1F = 0f
    private var lastX1F = 0f
    private var lastY1F = 0f
    private var isLeftDragging = false

    // Double-tap tracking
    private var lastTapUpTime = 0L
    private var lastTapUpX = 0f
    private var lastTapUpY = 0f

    // 2-finger tracking
    private var isTwoFingerDown = false
    private var downTime2F = 0L
    private var initialSpan = 0f
    private var lastSpan = 0f
    private var initialMidX = 0f
    private var initialMidY = 0f
    private var lastMidX = 0f
    private var lastMidY = 0f
    private var accumulatedScrollY = 0f
    private var twoFingerGesture = TwoFingerGesture.UNDECIDED

    fun setDesktopSize(width: Int, height: Int) {
        desktopWidth = maxOf(width, 1)
        desktopHeight = maxOf(height, 1)
        cursorX = cursorX.coerceIn(0f, (desktopWidth - 1).toFloat())
        cursorY = cursorY.coerceIn(0f, (desktopHeight - 1).toFloat())
    }

    /**
     * Resets user zoom and pan back to 1.0x (full view).
     */
    fun resetZoom() {
        userZoom = 1.0f
        userPanX = 0f
        userPanY = 0f
        applyTransform()
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        // Prevent parent container from intercepting any touch events
        view.parent?.requestDisallowInterceptTouchEvent(true)

        // Pass physical mouse events safely to FreeRDP if attached
        if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
            handlePhysicalMouseEvent(event)
            return true
        }

        try {
            when (mode) {
                InputMode.TRACKPAD -> handleTrackpadTouch(event)
                InputMode.DIRECT_TOUCH -> handleDirectTouch(event)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Unhandled touch controller error", t)
        }

        // Invariant: Always consume 100% of touch events to prevent fallthrough crashes
        return true
    }

    // =========================================================================
    // Trackpad Mode
    // =========================================================================

    private fun handleTrackpadTouch(event: MotionEvent) {
        val pointerCount = event.pointerCount

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isOneFingerDown = true
                isTwoFingerDown = false
                twoFingerGesture = TwoFingerGesture.UNDECIDED
                downTime1F = event.eventTime
                downX1F = event.x
                downY1F = event.y
                lastX1F = event.x
                lastY1F = event.y

                // Check for double-tap & drag
                val timeSinceLastTap = event.eventTime - lastTapUpTime
                val distFromLastTap = hypot(event.x - lastTapUpX, event.y - lastTapUpY)
                if (timeSinceLastTap < DOUBLE_TAP_TIMEOUT_MS && distFromLastTap < maxTapSlopPx) {
                    isLeftDragging = true
                    sendCursorEvent(cursorX.toInt(), cursorY.toInt(), Mouse.getLeftButtonEvent(context, true))
                } else {
                    isLeftDragging = false
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (pointerCount == 2) {
                    isTwoFingerDown = true
                    downTime2F = event.eventTime
                    twoFingerGesture = TwoFingerGesture.UNDECIDED

                    val span = hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1))
                    val midX = (event.getX(0) + event.getX(1)) / 2f
                    val midY = (event.getY(0) + event.getY(1)) / 2f

                    initialSpan = span
                    lastSpan = span
                    initialMidX = midX
                    initialMidY = midY
                    lastMidX = midX
                    lastMidY = midY
                    accumulatedScrollY = 0f

                    // Cancel 1-finger drag if user places second finger
                    if (isLeftDragging) {
                        isLeftDragging = false
                        sendCursorEvent(cursorX.toInt(), cursorY.toInt(), Mouse.getLeftButtonEvent(context, false))
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (pointerCount == 1 && !isTwoFingerDown) {
                    // 1-finger relative cursor movement
                    val effectiveZoom = (sessionView.zoom * userZoom).coerceAtLeast(0.01f)
                    val dx = (event.x - lastX1F) / effectiveZoom * TRACKPAD_SENSITIVITY
                    val dy = (event.y - lastY1F) / effectiveZoom * TRACKPAD_SENSITIVITY
                    lastX1F = event.x
                    lastY1F = event.y

                    cursorX = (cursorX + dx).coerceIn(0f, (desktopWidth - 1).toFloat())
                    cursorY = (cursorY + dy).coerceIn(0f, (desktopHeight - 1).toFloat())

                    sendCursorEvent(cursorX.toInt(), cursorY.toInt(), Mouse.getMoveEvent())
                } else if (pointerCount >= 2 && isTwoFingerDown) {
                    handleTwoFingerMove(event)
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (isTwoFingerDown && pointerCount == 2) {
                    val duration = event.eventTime - downTime2F
                    val midX = (event.getX(0) + event.getX(1)) / 2f
                    val midY = (event.getY(0) + event.getY(1)) / 2f
                    val distMoved = hypot(midX - initialMidX, midY - initialMidY)

                    // 2-finger tap: right click at current cursor position
                    if (twoFingerGesture == TwoFingerGesture.UNDECIDED && duration < TAP_TIMEOUT_MS && distMoved < maxTapSlopPx) {
                        sendCursorEvent(cursorX.toInt(), cursorY.toInt(), Mouse.getRightButtonEvent(context, true))
                        sendCursorEvent(cursorX.toInt(), cursorY.toInt(), Mouse.getRightButtonEvent(context, false))
                    }

                    isTwoFingerDown = false
                    twoFingerGesture = TwoFingerGesture.UNDECIDED

                    // Reset 1-finger tracking to remaining finger
                    val remainingIdx = if (event.actionIndex == 0) 1 else 0
                    lastX1F = event.getX(remainingIdx)
                    lastY1F = event.getY(remainingIdx)
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isLeftDragging) {
                    isLeftDragging = false
                    sendCursorEvent(cursorX.toInt(), cursorY.toInt(), Mouse.getLeftButtonEvent(context, false))
                } else if (isOneFingerDown && !isTwoFingerDown) {
                    val duration = event.eventTime - downTime1F
                    val distMoved = hypot(event.x - downX1F, event.y - downY1F)

                    // 1-finger tap: left click at current cursor position
                    if (duration < TAP_TIMEOUT_MS && distMoved < maxTapSlopPx) {
                        sendCursorEvent(cursorX.toInt(), cursorY.toInt(), Mouse.getLeftButtonEvent(context, true))
                        sendCursorEvent(cursorX.toInt(), cursorY.toInt(), Mouse.getLeftButtonEvent(context, false))

                        lastTapUpTime = event.eventTime
                        lastTapUpX = event.x
                        lastTapUpY = event.y
                    }
                }

                isOneFingerDown = false
                isTwoFingerDown = false
                twoFingerGesture = TwoFingerGesture.UNDECIDED
            }

            MotionEvent.ACTION_CANCEL -> {
                if (isLeftDragging) {
                    isLeftDragging = false
                    sendCursorEvent(cursorX.toInt(), cursorY.toInt(), Mouse.getLeftButtonEvent(context, false))
                }
                isOneFingerDown = false
                isTwoFingerDown = false
                twoFingerGesture = TwoFingerGesture.UNDECIDED
            }
        }
    }

    // =========================================================================
    // Direct Touch Mode
    // =========================================================================

    private fun handleDirectTouch(event: MotionEvent) {
        val pointerCount = event.pointerCount

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isOneFingerDown = true
                isTwoFingerDown = false
                twoFingerGesture = TwoFingerGesture.UNDECIDED
                downTime1F = event.eventTime
                downX1F = event.x
                downY1F = event.y

                val (desktopX, desktopY) = screenToDesktop(event.x, event.y)
                cursorX = desktopX.toFloat()
                cursorY = desktopY.toFloat()

                sendCursorEvent(desktopX, desktopY, Mouse.getMoveEvent())
                sendCursorEvent(desktopX, desktopY, Mouse.getLeftButtonEvent(context, true))
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (pointerCount == 2) {
                    isTwoFingerDown = true
                    downTime2F = event.eventTime
                    twoFingerGesture = TwoFingerGesture.UNDECIDED

                    val span = hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1))
                    val midX = (event.getX(0) + event.getX(1)) / 2f
                    val midY = (event.getY(0) + event.getY(1)) / 2f

                    initialSpan = span
                    lastSpan = span
                    initialMidX = midX
                    initialMidY = midY
                    lastMidX = midX
                    lastMidY = midY
                    accumulatedScrollY = 0f

                    // Release 1-finger touch down when second finger arrives
                    val (desktopX, desktopY) = screenToDesktop(downX1F, downY1F)
                    sendCursorEvent(desktopX, desktopY, Mouse.getLeftButtonEvent(context, false))
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (pointerCount == 1 && !isTwoFingerDown) {
                    val (desktopX, desktopY) = screenToDesktop(event.x, event.y)
                    cursorX = desktopX.toFloat()
                    cursorY = desktopY.toFloat()
                    sendCursorEvent(desktopX, desktopY, Mouse.getMoveEvent())
                } else if (pointerCount >= 2 && isTwoFingerDown) {
                    handleTwoFingerMove(event)
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (isTwoFingerDown && pointerCount == 2) {
                    val duration = event.eventTime - downTime2F
                    val midX = (event.getX(0) + event.getX(1)) / 2f
                    val midY = (event.getY(0) + event.getY(1)) / 2f
                    val distMoved = hypot(midX - initialMidX, midY - initialMidY)

                    // 2-finger tap: right click at midpoint
                    if (twoFingerGesture == TwoFingerGesture.UNDECIDED && duration < TAP_TIMEOUT_MS && distMoved < maxTapSlopPx) {
                        val (desktopX, desktopY) = screenToDesktop(midX, midY)
                        sendCursorEvent(desktopX, desktopY, Mouse.getRightButtonEvent(context, true))
                        sendCursorEvent(desktopX, desktopY, Mouse.getRightButtonEvent(context, false))
                    }

                    isTwoFingerDown = false
                    twoFingerGesture = TwoFingerGesture.UNDECIDED
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isOneFingerDown && !isTwoFingerDown) {
                    val (desktopX, desktopY) = screenToDesktop(event.x, event.y)
                    sendCursorEvent(desktopX, desktopY, Mouse.getLeftButtonEvent(context, false))
                }
                isOneFingerDown = false
                isTwoFingerDown = false
                twoFingerGesture = TwoFingerGesture.UNDECIDED
            }

            MotionEvent.ACTION_CANCEL -> {
                val (desktopX, desktopY) = screenToDesktop(event.x, event.y)
                sendCursorEvent(desktopX, desktopY, Mouse.getLeftButtonEvent(context, false))
                isOneFingerDown = false
                isTwoFingerDown = false
                twoFingerGesture = TwoFingerGesture.UNDECIDED
            }
        }
    }

    // =========================================================================
    // Two-Finger Gesture Handler (Pinch-to-Zoom & Pan vs. Mouse Wheel Scroll)
    // =========================================================================

    private fun handleTwoFingerMove(event: MotionEvent) {
        val span = hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1))
        val midX = (event.getX(0) + event.getX(1)) / 2f
        val midY = (event.getY(0) + event.getY(1)) / 2f

        val spanDelta = abs(span - initialSpan)
        val midYDelta = abs(midY - initialMidY)
        val midXDelta = abs(midX - initialMidX)

        // Classify gesture if still undecided
        if (twoFingerGesture == TwoFingerGesture.UNDECIDED) {
            val pinchThreshold = 22f * density
            val scrollThreshold = 14f * density

            if (spanDelta > pinchThreshold) {
                twoFingerGesture = TwoFingerGesture.PINCH_ZOOM_PAN
            } else if (midYDelta > scrollThreshold && spanDelta < 12f * density) {
                twoFingerGesture = TwoFingerGesture.SCROLL
            } else if (midXDelta > 20f * density || midYDelta > 20f * density) {
                twoFingerGesture = if (spanDelta > 14f * density) {
                    TwoFingerGesture.PINCH_ZOOM_PAN
                } else {
                    TwoFingerGesture.SCROLL
                }
            }
        }

        when (twoFingerGesture) {
            TwoFingerGesture.PINCH_ZOOM_PAN -> {
                if (lastSpan > 10f) {
                    val scaleFactor = span / lastSpan
                    val newZoom = (userZoom * scaleFactor).coerceIn(MIN_ZOOM, MAX_ZOOM)

                    val dMidX = midX - lastMidX
                    val dMidY = midY - lastMidY

                    // Zoom towards pinch center
                    val focusViewX = (midX - userPanX) / userZoom
                    val focusViewY = (midY - userPanY) / userZoom

                    userZoom = newZoom
                    userPanX = midX - (focusViewX * newZoom) + dMidX
                    userPanY = midY - (focusViewY * newZoom) + dMidY

                    clampPan()
                    applyTransform()
                }

                lastSpan = span
                lastMidX = midX
                lastMidY = midY
            }

            TwoFingerGesture.SCROLL -> {
                val deltaY = midY - lastMidY
                lastMidY = midY
                lastSpan = span
                accumulatedScrollY += deltaY

                val (desktopX, desktopY) = if (mode == InputMode.TRACKPAD) {
                    Pair(cursorX.toInt(), cursorY.toInt())
                } else {
                    screenToDesktop(midX, midY)
                }

                // Throttle mouse wheel events
                while (abs(accumulatedScrollY) >= scrollStepPx) {
                    val scrollDown = accumulatedScrollY < 0 // swiping up scrolls content down
                    sendCursorEvent(desktopX, desktopY, Mouse.getScrollEvent(context, scrollDown))
                    if (scrollDown) {
                        accumulatedScrollY += scrollStepPx
                    } else {
                        accumulatedScrollY -= scrollStepPx
                    }
                }
            }

            TwoFingerGesture.UNDECIDED -> {
                lastSpan = span
                lastMidX = midX
                lastMidY = midY
            }
        }
    }

    private fun clampPan() {
        val viewW = sessionView.width.toFloat().coerceAtLeast(1f)
        val viewH = sessionView.height.toFloat().coerceAtLeast(1f)

        if (userZoom <= 1.0f) {
            userPanX = 0f
            userPanY = 0f
            return
        }

        val minPanX = -viewW * (userZoom - 1f)
        val maxPanX = 0f
        val minPanY = -viewH * (userZoom - 1f)
        val maxPanY = 0f

        userPanX = userPanX.coerceIn(minPanX, maxPanX)
        userPanY = userPanY.coerceIn(minPanY, maxPanY)
    }

    private fun applyTransform() {
        sessionView.pivotX = 0f
        sessionView.pivotY = 0f
        sessionView.scaleX = userZoom
        sessionView.scaleY = userZoom
        sessionView.translationX = userPanX
        sessionView.translationY = userPanY
    }

    private fun handlePhysicalMouseEvent(event: MotionEvent) {
        val (desktopX, desktopY) = screenToDesktop(event.x, event.y)
        val action = event.actionMasked
        when (action) {
            MotionEvent.ACTION_BUTTON_PRESS -> {
                when (event.actionButton) {
                    MotionEvent.BUTTON_PRIMARY -> {
                        sendCursorEvent(desktopX, desktopY, Mouse.getLeftButtonEvent(context, true))
                    }
                    MotionEvent.BUTTON_SECONDARY -> {
                        sendCursorEvent(desktopX, desktopY, Mouse.getRightButtonEvent(context, true))
                    }
                    MotionEvent.BUTTON_TERTIARY -> {
                        sendCursorEvent(desktopX, desktopY, Mouse.getMiddleButtonEvent(true))
                    }
                }
            }
            MotionEvent.ACTION_BUTTON_RELEASE -> {
                when (event.actionButton) {
                    MotionEvent.BUTTON_PRIMARY -> {
                        sendCursorEvent(desktopX, desktopY, Mouse.getLeftButtonEvent(context, false))
                    }
                    MotionEvent.BUTTON_SECONDARY -> {
                        sendCursorEvent(desktopX, desktopY, Mouse.getRightButtonEvent(context, false))
                    }
                    MotionEvent.BUTTON_TERTIARY -> {
                        sendCursorEvent(desktopX, desktopY, Mouse.getMiddleButtonEvent(false))
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                sendCursorEvent(desktopX, desktopY, Mouse.getMoveEvent())
            }
            MotionEvent.ACTION_SCROLL -> {
                val vScroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                if (vScroll != 0f) {
                    sendCursorEvent(desktopX, desktopY, Mouse.getScrollEvent(context, vScroll < 0f))
                }
            }
        }
    }

    // =========================================================================
    // Coordinate Mapping (Accounting for Zoom and Pan)
    // =========================================================================

    private fun screenToDesktop(screenX: Float, screenY: Float): Pair<Int, Int> {
        val viewX = (screenX - userPanX) / userZoom.coerceAtLeast(0.01f)
        val viewY = (screenY - userPanY) / userZoom.coerceAtLeast(0.01f)
        val fitZoom = sessionView.zoom.coerceAtLeast(0.01f)
        val x = (viewX / fitZoom).toInt().coerceIn(0, desktopWidth - 1)
        val y = (viewY / fitZoom).toInt().coerceIn(0, desktopHeight - 1)
        return Pair(x, y)
    }

    private fun sendCursorEvent(x: Int, y: Int, flags: Int) {
        try {
            val inst = instanceProvider()
            if (inst != 0L) {
                LibFreeRDP.sendCursorEvent(inst, x, y, flags)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to send LibFreeRDP cursor event ($x, $y, $flags)", t)
        }
    }
}
