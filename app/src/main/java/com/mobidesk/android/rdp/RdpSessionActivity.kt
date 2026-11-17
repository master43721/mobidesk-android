package com.mobidesk.android.rdp

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.freerdp.freerdpcore.R
import com.freerdp.freerdpcore.presentation.ExtendedKeyboardView
import com.freerdp.freerdpcore.presentation.FloatingToolbar
import com.freerdp.freerdpcore.presentation.ScrollView2D
import com.freerdp.freerdpcore.presentation.SessionInputManager
import com.freerdp.freerdpcore.presentation.SessionView
import com.freerdp.freerdpcore.presentation.TouchPointerView
import com.freerdp.freerdpcore.services.LibFreeRDP
import com.freerdp.freerdpcore.utils.Mouse

/**
 * Dedicated Native RDP Session Activity (FP-11).
 *
 * Security & Presentation:
 * 1. Screen recording allowed for user capture and demo workflows (FLAG_SECURE disabled).
 * 2. Forced landscape orientation (sensorLandscape) for authentic 16:9 PC workstation experience.
 * 3. Fluid Microsoft Windows App-style direct touch input & absolute cursor coordinate translation.
 * 4. Cyberpunk loading screen overlay dismissed on the first Windows desktop frame.
 * 5. Consumes ephemeral parameters from [RdpSessionCarrier]; zero Intent extra credential persistence.
 * 6. Deterministic teardown and zeroing on [onDestroy] or user disconnect.
 */
class RdpSessionActivity : ComponentActivity(), RdpSessionListener {

    private lateinit var sessionManager: RdpSessionManager
    private lateinit var sessionView: SessionView
    private lateinit var scrollView: ScrollView2D
    private var touchPointerView: TouchPointerView? = null
    private var inputManager: SessionInputManager? = null
    private var desktopBitmap: Bitmap? = null
    private var floatingToolbar: FloatingToolbar? = null
    private var loadingOverlay: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // FLAG_SECURE removed to allow session recording for student demo & MVP UX
        // window.setFlags(
        //     WindowManager.LayoutParams.FLAG_SECURE,
        //     WindowManager.LayoutParams.FLAG_SECURE
        // )

        super.onCreate(savedInstanceState)
        hideSystemBars()

        val sessionData = RdpSessionCarrier.consumeSession()
        if (sessionData == null) {
            Toast.makeText(this, "Session expired or invalid.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContentView(R.layout.session)

        scrollView = findViewById(R.id.sessionScrollView)
        sessionView = findViewById(R.id.sessionView)
        touchPointerView = findViewById(R.id.touchPointerView)
        val keyboard: ExtendedKeyboardView = findViewById(R.id.extended_keyboard)

        // Disable and hide clunky overlays so they never block the workspace canvas
        touchPointerView?.visibility = View.GONE
        keyboard.visibility = View.GONE
        findViewById<View?>(R.id.floating_toolbar_container)?.visibility = View.GONE
        scrollView.setScrollEnabled(false)

        // Center the session view inside the container
        findViewById<FrameLayout>(R.id.railContainer)?.let { rc ->
            (rc.layoutParams as? FrameLayout.LayoutParams)?.gravity = Gravity.CENTER
        }
        (sessionView.layoutParams as? FrameLayout.LayoutParams)?.gravity = Gravity.CENTER

        inputManager = SessionInputManager(this, scrollView, sessionView, touchPointerView, keyboard)
        sessionView.setSessionViewListener(inputManager)
        touchPointerView?.setTouchPointerListener(inputManager)
        sessionView.setScaleGestureDetector(
            ScaleGestureDetector(this, inputManager!!.pinchZoomListener)
        )

        // Fluid Microsoft-style direct touch input: translates raw screen taps and drags
        // directly to LibFreeRDP cursor events with absolute coordinates.
        setupDirectTouchInput()

        floatingToolbar = FloatingToolbar(this, object : FloatingToolbar.Listener {
            override fun onToggleTouchPointer() {
                inputManager?.toggleTouchPointer()
            }

            override fun onToggleKeyboard() {
                inputManager?.toggleKeyboard()
            }
        })

        // Inject Cyberpunk Loading Screen Overlay
        val rootView = findViewById<ViewGroup>(R.id.session_root_view)
        val overlay = createCyberpunkLoadingOverlay()
        loadingOverlay = overlay
        rootView.addView(overlay)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (inputManager?.handleKeyboardBack() == true) return
                if (inputManager?.handleBackAsAltF4() == true) return
                disconnectAndExit()
            }
        })

        sessionManager = RdpSessionManager(this)
        sessionManager.setListener(this)

        // Launch session with standard 16:9 PC landscape resolution
        sessionManager.startSession(
            grant = sessionData.grant,
            credentials = sessionData.credentials,
            relayUrl = sessionData.relayUrl,
            desktopWidth = 1280,
            desktopHeight = 720
        )
    }

    private fun setupDirectTouchInput() {
        var isDirectTouchDown = false

        sessionView.setOnTouchListener { view, event ->
            // Pass physical external mouse events to FreeRDP's mouse handler
            if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
                return@setOnTouchListener false
            }

            // Multi-touch gestures (e.g. 2-finger scroll/pinch/tap):
            // Release single finger drag and delegate to SessionView
            if (event.pointerCount > 1) {
                if (isDirectTouchDown) {
                    isDirectTouchDown = false
                    val inst = sessionManager.getSessionState()?.instance ?: 0L
                    if (inst != 0L) {
                        val zoom = sessionView.zoom.coerceAtLeast(0.01f)
                        val touchX = (event.x / zoom).toInt().coerceIn(0, 1279)
                        val touchY = (event.y / zoom).toInt().coerceIn(0, 719)
                        LibFreeRDP.sendCursorEvent(inst, touchX, touchY, Mouse.getLeftButtonEvent(this, false))
                    }
                    inputManager?.onSessionViewEndTouch()
                }
                return@setOnTouchListener false
            }

            val inst = sessionManager.getSessionState()?.instance ?: 0L
            if (inst == 0L) {
                return@setOnTouchListener false
            }

            val zoom = sessionView.zoom.coerceAtLeast(0.01f)
            val currentDesktopW = desktopBitmap?.width ?: 1280
            val currentDesktopH = desktopBitmap?.height ?: 720
            val touchX = (event.x / zoom).toInt().coerceIn(0, currentDesktopW - 1)
            val touchY = (event.y / zoom).toInt().coerceIn(0, currentDesktopH - 1)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    view.parent?.requestDisallowInterceptTouchEvent(true)
                    isDirectTouchDown = true
                    inputManager?.onSessionViewBeginTouch()

                    // Absolute cursor move + left click down
                    LibFreeRDP.sendCursorEvent(inst, touchX, touchY, Mouse.getMoveEvent())
                    LibFreeRDP.sendCursorEvent(inst, touchX, touchY, Mouse.getLeftButtonEvent(this, true))
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    view.parent?.requestDisallowInterceptTouchEvent(true)
                    // Fluid absolute cursor drag
                    LibFreeRDP.sendCursorEvent(inst, touchX, touchY, Mouse.getMoveEvent())
                    true
                }
                MotionEvent.ACTION_UP -> {
                    isDirectTouchDown = false
                    // Release left button at absolute cursor position
                    LibFreeRDP.sendCursorEvent(inst, touchX, touchY, Mouse.getLeftButtonEvent(this, false))
                    inputManager?.onSessionViewEndTouch()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    if (isDirectTouchDown) {
                        isDirectTouchDown = false
                        LibFreeRDP.sendCursorEvent(inst, touchX, touchY, Mouse.getLeftButtonEvent(this, false))
                        inputManager?.onSessionViewEndTouch()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun createCyberpunkLoadingOverlay(): View {
        val overlay = FrameLayout(this).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#070A13"))
            isClickable = true
            isFocusable = true
            elevation = 50f
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        }

        // Subtitle badge: "// [ SECURE WORKSPACE LINK ] //"
        val badge = TextView(this).apply {
            text = "// [ SECURE WORKSPACE LINK ] //"
            setTextColor(Color.parseColor("#00FFCC"))
            textSize = 11f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.2f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dpToPx(16))
        }
        container.addView(badge)

        // Progress spinner with neon cyan tint
        val spinner = ProgressBar(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(56), dpToPx(56)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dpToPx(24)
            }
            isIndeterminate = true
            indeterminateTintList = ColorStateList.valueOf(Color.parseColor("#00E5FF"))
        }
        container.addView(spinner)

        // Main Cyberpunk Title
        val title = TextView(this).apply {
            text = "MobiDesk // CONNECTING TO CLOUD WORKSPACE"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 15f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            letterSpacing = 0.12f
            gravity = Gravity.CENTER
            setShadowLayer(14f, 0f, 0f, Color.parseColor("#00E5FF"))
            setPadding(dpToPx(16), 0, dpToPx(16), dpToPx(10))
        }
        container.addView(title)

        // Status description
        val subtitle = TextView(this).apply {
            text = "ESTABLISHING ENCRYPTED DESKTOP STREAM..."
            setTextColor(Color.parseColor("#80A0C0"))
            textSize = 11f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.1f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dpToPx(16))
        }
        container.addView(subtitle)

        // Technical specs tag
        val specs = TextView(this).apply {
            text = "[ 16:9 PC RESOLUTION // DIRECT TOUCH ENGINE ]"
            setTextColor(Color.parseColor("#FF007F"))
            textSize = 9f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.15f
            gravity = Gravity.CENTER
        }
        container.addView(specs)

        overlay.addView(container)
        return overlay
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    override fun onStateChanged(state: RdpSessionState) {
        if (state == RdpSessionState.DISCONNECTED) {
            runOnUiThread {
                loadingOverlay?.visibility = View.GONE
            }
            finish()
        }
    }

    override fun onCertificateVerified(fingerprint: String, accepted: Boolean) {
        if (!accepted) {
            Toast.makeText(this, "RDP certificate verification failed.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onSettingsChanged(width: Int, height: Int, bpp: Int) {
        val config = if (bpp > 16) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        val bitmap = Bitmap.createBitmap(width, height, config)
        desktopBitmap = bitmap

        val session = sessionManager.getSessionState()
        if (session != null) {
            session.surface = BitmapDrawable(resources, bitmap)
            sessionView.onSurfaceChange(session)
            inputManager?.attachSession(session.instance, bitmap)
            inputManager?.setScreenSize(width, height)

            // Scale to fit landscape device display preserving 16:9 aspect ratio
            val dm = resources.displayMetrics
            val screenW = maxOf(dm.widthPixels, dm.heightPixels)
            val screenH = minOf(dm.widthPixels, dm.heightPixels)
            val fitZoom = minOf(screenW.toFloat() / width, screenH.toFloat() / height)
            if (fitZoom > 0f) {
                sessionView.zoom = fitZoom
            }
        }
    }

    override fun onGraphicsUpdate(x: Int, y: Int, width: Int, height: Int) {
        // Instantly dismiss the cyberpunk loading overlay when the first frame arrives
        if (loadingOverlay?.visibility != View.GONE) {
            runOnUiThread {
                loadingOverlay?.visibility = View.GONE
            }
        }

        val session = sessionManager.getSessionState()
        val bitmap = desktopBitmap
        if (session != null && bitmap != null) {
            LibFreeRDP.updateGraphics(session.instance, bitmap, x, y, width, height)
            sessionView.addInvalidRegion(Rect(x, y, x + width, y + height))
            sessionView.invalidateRegion()
        }
    }

    override fun onError(message: String, isFatal: Boolean) {
        runOnUiThread {
            loadingOverlay?.visibility = View.GONE
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        if (isFatal) {
            disconnectAndExit()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) return super.onKeyDown(keyCode, event)
        return inputManager?.onAndroidKeyEvent(event) ?: super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) return super.onKeyUp(keyCode, event)
        return inputManager?.onAndroidKeyEvent(event) ?: super.onKeyUp(keyCode, event)
    }

    private fun disconnectAndExit() {
        runOnUiThread {
            loadingOverlay?.visibility = View.GONE
        }
        sessionManager.disconnect()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        loadingOverlay = null
        if (::sessionManager.isInitialized) {
            sessionManager.close()
        }
        desktopBitmap?.recycle()
        desktopBitmap = null
        RdpSessionCarrier.clear()
    }
}
