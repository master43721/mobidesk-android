package com.mobidesk.android.rdp

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Dedicated Native RDP Session Activity (FP-11 / v0.6.0).
 *
 * UX & Architecture Improvements:
 * 1. True Edge-to-Edge full screen with display cutout short edges (no black sidebars).
 * 2. Real physical screen metrics resolution calculation for authentic full landscape view.
 * 3. Single accurate Windows mouse cursor (no double-cursor or edge drift).
 * 4. Crash-safe [RdpTouchController] with 2-finger pinch-to-zoom (1.0x-3.0x), pan, 2-finger scroll & right-click.
 * 5. Draggable, collapsible top pill with "1x" reset zoom, safe exit confirmation, and soft keyboard + PC modifier keys.
 * 6. Responsive cyberpunk loader layout with non-black substantive frame detection.
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
    private var isDismissingLoader = false
    private var graphicsUpdateCount = 0

    // Touch Controller
    private lateinit var touchController: RdpTouchController

    // Draggable Top Control Pill & PC Keys UI
    private var topControlContainer: LinearLayout? = null
    private var pillBody: LinearLayout? = null
    private var keysBar: LinearLayout? = null
    private var modeButton: TextView? = null
    private var keyboardButton: TextView? = null
    private var resetZoomButton: TextView? = null
    private var exitButton: TextView? = null
    private var collapseHandle: TextView? = null
    private var isPillCollapsed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()

        val sessionData = RdpSessionCarrier.consumeSession()
        if (sessionData == null) {
            Toast.makeText(this, "Session expired or invalid.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContentView(R.layout.session)

        val rootView = findViewById<ViewGroup>(R.id.session_root_view)
        rootView.fitsSystemWindows = false
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, _ -> WindowInsetsCompat.CONSUMED }

        scrollView = findViewById(R.id.sessionScrollView)
        scrollView.fitsSystemWindows = false
        sessionView = findViewById(R.id.sessionView)
        sessionView.fitsSystemWindows = false
        touchPointerView = findViewById(R.id.touchPointerView)
        val keyboard: ExtendedKeyboardView = findViewById(R.id.extended_keyboard)

        // Disable and hide legacy clunky overlays
        touchPointerView?.visibility = View.GONE
        keyboard.visibility = View.GONE
        findViewById<View?>(R.id.floating_toolbar_container)?.visibility = View.GONE
        scrollView.setScrollEnabled(false)

        // Center session view inside the rail container
        findViewById<FrameLayout>(R.id.railContainer)?.let { rc ->
            rc.fitsSystemWindows = false
            (rc.layoutParams as? FrameLayout.LayoutParams)?.gravity = Gravity.CENTER
        }
        (sessionView.layoutParams as? FrameLayout.LayoutParams)?.gravity = Gravity.CENTER
        sessionView.isFocusable = true
        sessionView.isFocusableInTouchMode = true

        inputManager = SessionInputManager(this, scrollView, sessionView, touchPointerView, keyboard)
        sessionView.setSessionViewListener(inputManager)
        touchPointerView?.setTouchPointerListener(inputManager)

        // 1. Setup crash-safe touch controller with pinch-zoom & pan
        setupTouchController()

        // 2. Setup draggable, collapsible top control pill & PC Extra Keys Bar
        setupTopControlPill(rootView)

        // 3. Inject Cyberpunk Loading Screen Overlay
        val overlay = createCyberpunkLoadingOverlay()
        loadingOverlay = overlay
        rootView.addView(overlay)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (keysBar?.visibility == View.VISIBLE) {
                    toggleSoftKeyboard()
                    return
                }
                if (inputManager?.handleKeyboardBack() == true) return
                if (inputManager?.handleBackAsAltF4() == true) return
                confirmDisconnect()
            }
        })

        sessionManager = RdpSessionManager(this)
        sessionManager.setListener(this)

        // Dynamically compute edge-to-edge landscape resolution from physical screen metrics
        val (realW, realH) = getRealScreenDimensions()
        val screenW = max(realW, realH)
        val screenH = min(realW, realH)
        val targetHeight = 720
        val calculatedWidth = (((targetHeight.toFloat() * screenW / screenH).toInt()) / 4) * 4
        val targetWidth = calculatedWidth.coerceIn(1280, 1920)

        // Launch session with calculated edge-to-edge native landscape resolution
        sessionManager.startSession(
            grant = sessionData.grant,
            credentials = sessionData.credentials,
            relayUrl = sessionData.relayUrl,
            desktopWidth = targetWidth,
            desktopHeight = targetHeight
        )
    }

    private fun getRealScreenDimensions(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            Pair(bounds.width(), bounds.height())
        } else {
            val dm = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(dm)
            Pair(dm.widthPixels, dm.heightPixels)
        }
    }

    // =========================================================================
    // Touch Controller (Single Real Cursor, Pinch-to-Zoom & Pan)
    // =========================================================================

    private fun setupTouchController() {
        touchController = RdpTouchController(
            context = this,
            sessionView = sessionView,
            instanceProvider = { sessionManager.getSessionState()?.instance ?: 0L }
        )
        sessionView.setOnTouchListener(touchController)

        touchController.modeListener = object : RdpTouchController.ModeListener {
            override fun onModeChanged(mode: RdpTouchController.InputMode) {
                runOnUiThread {
                    updateModeButton(mode)
                }
            }
        }
    }

    // =========================================================================
    // Draggable & Collapsible Top Control Pill
    // =========================================================================

    private fun setupTopControlPill(rootView: ViewGroup) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_TOP)
                addRule(RelativeLayout.CENTER_HORIZONTAL)
                topMargin = dpToPx(6)
            }
            elevation = 45f
        }
        topControlContainer = container

        // 1. Horizontal Glass Pill
        val pill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(20).toFloat()
                setColor(Color.parseColor("#D90B101D")) // Dark glass
                setStroke(dpToPx(1), Color.parseColor("#3300E5FF"))
            }
            background = bg
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
        }
        pillBody = pill

        // Collapse / Expand Handle with Horizontal Dragging
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        var dragStartX = 0f
        var initialTransX = 0f
        var isDraggingPill = false

        val handle = TextView(this).apply {
            text = "▲"
            setTextColor(Color.parseColor("#80A0C0"))
            textSize = 10f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))

            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        dragStartX = event.rawX
                        initialTransX = container.translationX
                        isDraggingPill = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - dragStartX
                        if (abs(dx) > touchSlop) {
                            isDraggingPill = true
                        }
                        if (isDraggingPill) {
                            val (realW, realH) = getRealScreenDimensions()
                            val screenW = max(realW, realH)
                            val pillW = container.width.coerceAtLeast(dpToPx(50))
                            val maxOffset = (screenW - pillW) / 2f
                            container.translationX = (initialTransX + dx).coerceIn(-maxOffset, maxOffset)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDraggingPill) {
                            v.performClick()
                            togglePillCollapse()
                        }
                        isDraggingPill = false
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        isDraggingPill = false
                        true
                    }
                    else -> false
                }
            }
        }
        collapseHandle = handle
        pill.addView(handle)

        // Mode Toggle Button (Trackpad / Direct Touch)
        val modeBtn = createPillButton(
            text = "🖱 Trackpad",
            textColor = Color.parseColor("#00E5FF")
        ) {
            val nextMode = if (touchController.mode == RdpTouchController.InputMode.TRACKPAD) {
                RdpTouchController.InputMode.DIRECT_TOUCH
            } else {
                RdpTouchController.InputMode.TRACKPAD
            }
            touchController.mode = nextMode
        }
        modeButton = modeBtn
        pill.addView(modeBtn)

        // "1x" Reset Zoom Button
        val resetZoomBtn = createPillButton(
            text = "1x",
            textColor = Color.parseColor("#00E5FF")
        ) {
            touchController.resetZoom()
        }
        resetZoomButton = resetZoomBtn
        pill.addView(resetZoomBtn)

        // Keyboard Button (Toggles Soft Keyboard + PC Keys Bar)
        val kbdBtn = createPillButton(
            text = "⌨ Keyboard",
            textColor = Color.parseColor("#00E5FF")
        ) {
            toggleSoftKeyboard()
        }
        keyboardButton = kbdBtn
        pill.addView(kbdBtn)

        // Disconnect Button with Confirmation
        val exitBtn = createPillButton(
            text = "✕ Exit",
            textColor = Color.parseColor("#FF4466")
        ) {
            confirmDisconnect()
        }
        exitButton = exitBtn
        pill.addView(exitBtn)

        container.addView(pill)

        // 2. PC Extra Keys Bar (Esc, Tab, Ctrl, Alt, Win, Del, ←, ↑, ↓, →)
        val keys = createPcKeysBar()
        keysBar = keys
        container.addView(keys)

        rootView.addView(container)
    }

    private fun confirmDisconnect() {
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Disconnect")
            .setMessage("Disconnect from Windows PC?")
            .setPositiveButton("Disconnect") { _, _ ->
                disconnectAndExit()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createPcKeysBar(): LinearLayout {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(10).toFloat()
                setColor(Color.parseColor("#E60B101D"))
                setStroke(dpToPx(1), Color.parseColor("#2600E5FF"))
            }
            background = bg
            setPadding(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(4)
            }
            layoutParams = lp
            visibility = View.GONE
        }

        // Action Keys
        bar.addView(createRegularKey("Esc", R.integer.keycode_esc))
        bar.addView(createRegularKey("Tab", R.integer.keycode_tab))

        // Sticky Modifier Keys
        bar.addView(createModifierKey("Ctrl", R.integer.keycode_toggle_ctrl))
        bar.addView(createModifierKey("Alt", R.integer.keycode_toggle_alt))
        bar.addView(createModifierKey("Win", R.integer.keycode_toggle_win))

        // Delete & Arrow Keys
        bar.addView(createRegularKey("Del", R.integer.keycode_delete))
        bar.addView(createRegularKey("←", R.integer.keycode_left))
        bar.addView(createRegularKey("↑", R.integer.keycode_up))
        bar.addView(createRegularKey("↓", R.integer.keycode_down))
        bar.addView(createRegularKey("→", R.integer.keycode_right))

        return bar
    }

    private fun createPillButton(
        text: String,
        textColor: Int,
        bgColor: Int = Color.parseColor("#1C2433"),
        onClick: () -> Unit
    ): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(textColor)
            textSize = 11f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(14).toFloat()
                setColor(bgColor)
                setStroke(dpToPx(1), Color.parseColor("#2600E5FF"))
            }
            background = bg
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(4)
            }
            layoutParams = lp
            setOnClickListener { onClick() }
        }
    }

    private fun createModifierKey(label: String, resId: Int): TextView {
        var isStickyActive = false
        lateinit var tv: TextView
        val normalBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(6).toFloat()
            setColor(Color.parseColor("#1E2536"))
            setStroke(dpToPx(1), Color.parseColor("#3300E5FF"))
        }

        tv = TextView(this).apply {
            text = label
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 10f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            minWidth = dpToPx(36)
            minHeight = dpToPx(30)
            setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2))
            background = normalBg
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(3)
            }
            layoutParams = lp

            setOnClickListener {
                isStickyActive = !isStickyActive
                if (isStickyActive) {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = dpToPx(6).toFloat()
                        setColor(Color.parseColor("#00E5FF"))
                    }
                    setTextColor(Color.BLACK)
                } else {
                    background = normalBg
                    setTextColor(Color.parseColor("#00E5FF"))
                }
                inputManager?.onKey(resources.getInteger(resId))
            }
        }
        return tv
    }

    private fun createRegularKey(label: String, resId: Int): TextView {
        return TextView(this).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 10f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            minWidth = dpToPx(34)
            minHeight = dpToPx(30)
            setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(6).toFloat()
                setColor(Color.parseColor("#1E2536"))
                setStroke(dpToPx(1), Color.parseColor("#22FFFFFF"))
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(3)
            }
            layoutParams = lp
            setOnClickListener {
                inputManager?.onKey(resources.getInteger(resId))
            }
        }
    }

    private fun updateModeButton(mode: RdpTouchController.InputMode) {
        modeButton?.text = if (mode == RdpTouchController.InputMode.TRACKPAD) {
            "🖱 Trackpad"
        } else {
            "👆 Touch"
        }
    }

    private fun togglePillCollapse() {
        isPillCollapsed = !isPillCollapsed
        collapseHandle?.text = if (isPillCollapsed) "▼" else "▲"
        val buttonVisibility = if (isPillCollapsed) View.GONE else View.VISIBLE

        modeButton?.visibility = buttonVisibility
        keyboardButton?.visibility = buttonVisibility
        resetZoomButton?.visibility = buttonVisibility
        exitButton?.visibility = buttonVisibility

        // When collapsed, close the keys bar if it was open
        if (isPillCollapsed && keysBar?.visibility == View.VISIBLE) {
            toggleSoftKeyboard()
        }
    }

    private fun toggleSoftKeyboard() {
        val willOpen = keysBar?.visibility != View.VISIBLE
        keysBar?.visibility = if (willOpen) View.VISIBLE else View.GONE
        keyboardButton?.setTextColor(
            if (willOpen) Color.parseColor("#00FFCC") else Color.parseColor("#00E5FF")
        )

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        if (willOpen) {
            sessionView.isFocusable = true
            sessionView.isFocusableInTouchMode = true
            sessionView.requestFocus()
            imm?.showSoftInput(sessionView, InputMethodManager.SHOW_IMPLICIT)
        } else {
            imm?.hideSoftInputFromWindow(sessionView.windowToken, 0)
        }
    }

    // =========================================================================
    // Cyberpunk Loading Overlay & Smooth Fade-Out
    // =========================================================================

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
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ).apply {
                leftMargin = dpToPx(32)
                rightMargin = dpToPx(32)
            }
        }

        // Subtitle badge
        val badge = TextView(this).apply {
            text = "// [ SECURE WORKSPACE LINK ] //"
            setTextColor(Color.parseColor("#00FFCC"))
            textSize = 12f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.18f
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(16)
            }
        }
        container.addView(badge)

        // Progress spinner with neon cyan tint
        val spinner = ProgressBar(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(52), dpToPx(52)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dpToPx(20)
            }
            isIndeterminate = true
            indeterminateTintList = ColorStateList.valueOf(Color.parseColor("#00E5FF"))
        }
        container.addView(spinner)

        // Main Cyberpunk Title
        val title = TextView(this).apply {
            text = "MobiDesk // CONNECTING TO CLOUD WORKSPACE"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 16f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            letterSpacing = 0.1f
            gravity = Gravity.CENTER_HORIZONTAL
            setShadowLayer(14f, 0f, 0f, Color.parseColor("#00E5FF"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(10)
            }
        }
        container.addView(title)

        // Status description
        val subtitle = TextView(this).apply {
            text = "ESTABLISHING ENCRYPTED DESKTOP STREAM..."
            setTextColor(Color.parseColor("#80A0C0"))
            textSize = 12f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.08f
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(14)
            }
        }
        container.addView(subtitle)

        // Technical specs tag
        val specs = TextView(this).apply {
            text = "[ NATIVE RESOLUTION // PINCH-ZOOM & TRACKPAD ENGINE ]"
            setTextColor(Color.parseColor("#FF007F"))
            textSize = 10f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.12f
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
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

            // Scale to fill landscape device display with zero black sidebars
            val (realW, realH) = getRealScreenDimensions()
            val screenW = max(realW, realH)
            val screenH = min(realW, realH)
            val fitZoom = max(screenW.toFloat() / width, screenH.toFloat() / height)
            if (fitZoom > 0f) {
                sessionView.zoom = fitZoom
            }
            touchController.setDesktopSize(width, height)
        }
    }

    override fun onGraphicsUpdate(x: Int, y: Int, width: Int, height: Int) {
        val session = sessionManager.getSessionState()
        val bitmap = desktopBitmap
        if (session != null && bitmap != null) {
            LibFreeRDP.updateGraphics(session.instance, bitmap, x, y, width, height)
            sessionView.addInvalidRegion(Rect(x, y, x + width, y + height))
            sessionView.invalidateRegion()

            graphicsUpdateCount++

            // Smoothly dismiss cyberpunk loader only once non-black substantive desktop frame arrives
            if (loadingOverlay != null && !isDismissingLoader) {
                val hasContent = hasSubstantiveContent(bitmap, x, y, width, height)
                // Fallback: dismiss after 6 substantive updates or when non-black pixels are found
                if (hasContent || (graphicsUpdateCount >= 6 && width >= 200 && height >= 200)) {
                    isDismissingLoader = true
                    runOnUiThread {
                        loadingOverlay?.animate()
                            ?.alpha(0f)
                            ?.setDuration(350)
                            ?.withEndAction {
                                loadingOverlay?.visibility = View.GONE
                                loadingOverlay = null
                            }
                            ?.start()
                    }
                }
            }
        }
    }

    private fun hasSubstantiveContent(bitmap: Bitmap, x: Int, y: Int, w: Int, h: Int): Boolean {
        if (w < 16 || h < 16) return false
        try {
            val stepX = max(1, w / 8)
            val stepY = max(1, h / 8)
            var nonBlackCount = 0
            for (sampleY in (y + stepY) until (y + h) step stepY) {
                for (sampleX in (x + stepX) until (x + w) step stepX) {
                    if (sampleX < bitmap.width && sampleY < bitmap.height) {
                        val pixel = bitmap.getPixel(sampleX, sampleY)
                        // Non-black pixel check
                        if ((pixel and 0x00FFFFFF) != 0) {
                            nonBlackCount++
                            if (nonBlackCount >= 3) return true
                        }
                    }
                }
            }
        } catch (_: Throwable) {}
        return false
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

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (keysBar?.visibility == View.VISIBLE) {
                toggleSoftKeyboard()
                return true
            }
            return super.dispatchKeyEvent(event)
        }
        if (inputManager?.onAndroidKeyEvent(event) == true) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) return super.onKeyDown(keyCode, event)
        return inputManager?.onAndroidKeyEvent(event) ?: super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) return super.onKeyUp(keyCode, event)
        return inputManager?.onAndroidKeyEvent(event) ?: super.onKeyUp(keyCode, event)
    }

    override fun onKeyMultiple(keyCode: Int, repeatCount: Int, event: KeyEvent?): Boolean {
        if (event != null && inputManager?.onAndroidKeyEvent(event) == true) {
            return true
        }
        return super.onKeyMultiple(keyCode, repeatCount, event)
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
