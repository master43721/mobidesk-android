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
 * Dedicated, crash-safe touch controller for FreeRDP sessions.
 *
 * Implements two live input modes:
 * 1. [InputMode.TRACKPAD]: Laptop touchpad feel with a free virtual cursor.
 *    - 1-finger relative movement with sensitivity.
 *    - 1-finger tap: left click.
 *    - 1-finger double-tap & drag: left-button drag.
 *    - 2-finger tap: right click.
 *    - 2-finger vertical drag: smooth mouse wheel scroll.
 * 2. [InputMode.DIRECT_TOUCH]: Direct touch mapping.
 *    - 1-finger tap/drag: maps touch directly to desktop coordinates and clicks/drags.
 *    - 2-finger tap: right click at midpoint.
 *    - 2-finger vertical drag: smooth mouse wheel scroll.
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
    }

    enum class InputMode {
        TRACKPAD,
        DIRECT_TOUCH
    }

    interface CursorListener {
        fun onCursorMoved(desktopX: Int, desktopY: Int, screenX: Float, screenY: Float)
        fun onModeChanged(mode: InputMode)
    }

    var cursorListener: CursorListener? = null

    var mode: InputMode = InputMode.TRACKPAD
        set(value) {
            field = value
            cursorListener?.onModeChanged(value)
            notifyCursorPosition()
        }

    private var desktopWidth: Int = 1280
    private var desktopHeight: Int = 720

    // Virtual cursor in desktop coordinate space [0, desktopWidth - 1], [0, desktopHeight - 1]
    var cursorX: Float = 640f
        private set
    var cursorY: Float = 360f
        private set

    // Touch slop and scroll step in physical pixels
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
    private var downMidX2F = 0f
    private var downMidY2F = 0f
    private var lastMidY2F = 0f
    private var accumulatedScrollY = 0f
    private var isTwoFingerScrolling = false

    fun setDesktopSize(width: Int, height: Int) {
        desktopWidth = maxOf(width, 1)
        desktopHeight = maxOf(height, 1)
        cursorX = cursorX.coerceIn(0f, (desktopWidth - 1).toFloat())
        cursorY = cursorY.coerceIn(0f, (desktopHeight - 1).toFloat())
        notifyCursorPosition()
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        // Prevent parent from intercepting any touch events
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

        // Always consume 100% of touch events to prevent fallthrough crashes
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
                isTwoFingerScrolling = false
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
                    isTwoFingerScrolling = false
                    downTime2F = event.eventTime
                    downMidX2F = (event.getX(0) + event.getX(1)) / 2f
                    downMidY2F = (event.getY(0) + event.getY(1)) / 2f
                    lastMidY2F = downMidY2F
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
                    val zoom = sessionView.zoom.coerceAtLeast(0.01f)
                    val dx = (event.x - lastX1F) / zoom * TRACKPAD_SENSITIVITY
                    val dy = (event.y - lastY1F) / zoom * TRACKPAD_SENSITIVITY
                    lastX1F = event.x
                    lastY1F = event.y

                    cursorX = (cursorX + dx).coerceIn(0f, (desktopWidth - 1).toFloat())
                    cursorY = (cursorY + dy).coerceIn(0f, (desktopHeight - 1).toFloat())

                    sendCursorEvent(cursorX.toInt(), cursorY.toInt(), Mouse.getMoveEvent())
                    notifyCursorPosition()
                } else if (pointerCount >= 2 && isTwoFingerDown) {
                    // 2-finger vertical scroll
                    val midY = (event.getY(0) + event.getY(1)) / 2f
                    val deltaY = midY - lastMidY2F
                    lastMidY2F = midY
                    accumulatedScrollY += deltaY

                    if (abs(midY - downMidY2F) > touchSlopPx) {
                        isTwoFingerScrolling = true
                    }

                    // Throttle wheel scroll steps
                    while (abs(accumulatedScrollY) >= scrollStepPx) {
                        val scrollDown = accumulatedScrollY < 0 // swiping up scrolls content down
                        sendCursorEvent(cursorX.toInt(), cursorY.toInt(), Mouse.getScrollEvent(context, scrollDown))
                        if (scrollDown) {
                            accumulatedScrollY += scrollStepPx
                        } else {
                            accumulatedScrollY -= scrollStepPx
                        }
                    }
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (isTwoFingerDown && pointerCount == 2) {
                    val duration = event.eventTime - downTime2F
                    val midX = (event.getX(0) + event.getX(1)) / 2f
                    val midY = (event.getY(0) + event.getY(1)) / 2f
                    val distMoved = hypot(midX - downMidX2F, midY - downMidY2F)

                    // 2-finger tap: right click
                    if (!isTwoFingerScrolling && duration < TAP_TIMEOUT_MS && distMoved < maxTapSlopPx) {
                        sendCursorEvent(cursorX.toInt(), cursorY.toInt(), Mouse.getRightButtonEvent(context, true))
                        sendCursorEvent(cursorX.toInt(), cursorY.toInt(), Mouse.getRightButtonEvent(context, false))
                    }

                    isTwoFingerDown = false
                    isTwoFingerScrolling = false
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

                    // 1-finger tap: left click
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
                isTwoFingerScrolling = false
            }

            MotionEvent.ACTION_CANCEL -> {
                if (isLeftDragging) {
                    isLeftDragging = false
                    sendCursorEvent(cursorX.toInt(), cursorY.toInt(), Mouse.getLeftButtonEvent(context, false))
                }
                isOneFingerDown = false
                isTwoFingerDown = false
                isTwoFingerScrolling = false
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
                isTwoFingerScrolling = false
                downTime1F = event.eventTime
                downX1F = event.x
                downY1F = event.y

                val (desktopX, desktopY) = screenToDesktop(event.x, event.y)
                cursorX = desktopX.toFloat()
                cursorY = desktopY.toFloat()
                notifyCursorPosition()

                sendCursorEvent(desktopX, desktopY, Mouse.getMoveEvent())
                sendCursorEvent(desktopX, desktopY, Mouse.getLeftButtonEvent(context, true))
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (pointerCount == 2) {
                    isTwoFingerDown = true
                    isTwoFingerScrolling = false
                    downTime2F = event.eventTime
                    downMidX2F = (event.getX(0) + event.getX(1)) / 2f
                    downMidY2F = (event.getY(0) + event.getY(1)) / 2f
                    lastMidY2F = downMidY2F
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
                    notifyCursorPosition()
                    sendCursorEvent(desktopX, desktopY, Mouse.getMoveEvent())
                } else if (pointerCount >= 2 && isTwoFingerDown) {
                    val midY = (event.getY(0) + event.getY(1)) / 2f
                    val deltaY = midY - lastMidY2F
                    lastMidY2F = midY
                    accumulatedScrollY += deltaY

                    if (abs(midY - downMidY2F) > touchSlopPx) {
                        isTwoFingerScrolling = true
                    }

                    val (desktopX, desktopY) = screenToDesktop(downMidX2F, midY)
                    while (abs(accumulatedScrollY) >= scrollStepPx) {
                        val scrollDown = accumulatedScrollY < 0
                        sendCursorEvent(desktopX, desktopY, Mouse.getScrollEvent(context, scrollDown))
                        if (scrollDown) {
                            accumulatedScrollY += scrollStepPx
                        } else {
                            accumulatedScrollY -= scrollStepPx
                        }
                    }
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (isTwoFingerDown && pointerCount == 2) {
                    val duration = event.eventTime - downTime2F
                    val midX = (event.getX(0) + event.getX(1)) / 2f
                    val midY = (event.getY(0) + event.getY(1)) / 2f
                    val distMoved = hypot(midX - downMidX2F, midY - downMidY2F)

                    // 2-finger tap: right click at midpoint
                    if (!isTwoFingerScrolling && duration < TAP_TIMEOUT_MS && distMoved < maxTapSlopPx) {
                        val (desktopX, desktopY) = screenToDesktop(midX, midY)
                        sendCursorEvent(desktopX, desktopY, Mouse.getRightButtonEvent(context, true))
                        sendCursorEvent(desktopX, desktopY, Mouse.getRightButtonEvent(context, false))
                    }

                    isTwoFingerDown = false
                    isTwoFingerScrolling = false
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isOneFingerDown && !isTwoFingerDown) {
                    val (desktopX, desktopY) = screenToDesktop(event.x, event.y)
                    sendCursorEvent(desktopX, desktopY, Mouse.getLeftButtonEvent(context, false))
                }
                isOneFingerDown = false
                isTwoFingerDown = false
                isTwoFingerScrolling = false
            }

            MotionEvent.ACTION_CANCEL -> {
                val (desktopX, desktopY) = screenToDesktop(event.x, event.y)
                sendCursorEvent(desktopX, desktopY, Mouse.getLeftButtonEvent(context, false))
                isOneFingerDown = false
                isTwoFingerDown = false
                isTwoFingerScrolling = false
            }
        }
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
    // Helper Methods
    // =========================================================================

    private fun screenToDesktop(screenX: Float, screenY: Float): Pair<Int, Int> {
        val zoom = sessionView.zoom.coerceAtLeast(0.01f)
        val x = (screenX / zoom).toInt().coerceIn(0, desktopWidth - 1)
        val y = (screenY / zoom).toInt().coerceIn(0, desktopHeight - 1)
        return Pair(x, y)
    }

    private fun desktopToScreen(desktopX: Float, desktopY: Float): Pair<Float, Float> {
        val zoom = sessionView.zoom.coerceAtLeast(0.01f)
        return Pair(desktopX * zoom, desktopY * zoom)
    }

    private fun notifyCursorPosition() {
        val (screenX, screenY) = desktopToScreen(cursorX, cursorY)
        cursorListener?.onCursorMoved(cursorX.toInt(), cursorY.toInt(), screenX, screenY)
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
