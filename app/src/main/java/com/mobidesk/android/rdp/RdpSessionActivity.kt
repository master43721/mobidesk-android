package com.mobidesk.android.rdp

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.WindowManager
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

/**
 * Dedicated Native RDP Session Activity (FP-11).
 *
 * Security Invariants:
 * 1. [WindowManager.LayoutParams.FLAG_SECURE] is strictly applied BEFORE any layout inflation
 *    or remote desktop pixels are rendered. This protects remote session data against screen
 *    recording, screenshotting, and Android Recent Tasks preview capture.
 * 2. Unexported activity (`android:exported="false"`).
 * 3. Consumes ephemeral parameters from [RdpSessionCarrier]; zero Intent extra credential persistence.
 * 4. Deterministic teardown and zeroing on [onDestroy] or user disconnect.
 */
class RdpSessionActivity : ComponentActivity(), RdpSessionListener {

    private lateinit var sessionManager: RdpSessionManager
    private lateinit var sessionView: SessionView
    private lateinit var scrollView: ScrollView2D
    private var touchPointerView: TouchPointerView? = null
    private var inputManager: SessionInputManager? = null
    private var desktopBitmap: Bitmap? = null
    private var floatingToolbar: FloatingToolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enforce FLAG_SECURE before any view creation or pixel display
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

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

        inputManager = SessionInputManager(this, scrollView, sessionView, touchPointerView, keyboard)
        sessionView.setSessionViewListener(inputManager)
        touchPointerView?.setTouchPointerListener(inputManager)
        sessionView.setScaleGestureDetector(
            ScaleGestureDetector(this, inputManager!!.pinchZoomListener)
        )

        floatingToolbar = FloatingToolbar(this, object : FloatingToolbar.Listener {
            override fun onToggleTouchPointer() {
                inputManager?.toggleTouchPointer()
            }

            override fun onToggleKeyboard() {
                inputManager?.toggleKeyboard()
            }
        })

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (inputManager?.handleKeyboardBack() == true) return
                if (inputManager?.handleBackAsAltF4() == true) return
                disconnectAndExit()
            }
        })

        sessionManager = RdpSessionManager(this)
        sessionManager.setListener(this)

        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels.coerceAtLeast(800)
        val height = displayMetrics.heightPixels.coerceAtLeast(600)

        sessionManager.startSession(
            grant = sessionData.grant,
            credentials = sessionData.credentials,
            relayUrl = sessionData.relayUrl,
            desktopWidth = width,
            desktopHeight = height
        )
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
        }
    }

    override fun onGraphicsUpdate(x: Int, y: Int, width: Int, height: Int) {
        val session = sessionManager.getSessionState()
        val bitmap = desktopBitmap
        if (session != null && bitmap != null) {
            LibFreeRDP.updateGraphics(session.instance, bitmap, x, y, width, height)
            sessionView.addInvalidRegion(Rect(x, y, x + width, y + height))
            sessionView.invalidateRegion()
        }
    }

    override fun onError(message: String, isFatal: Boolean) {
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
        sessionManager.disconnect()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::sessionManager.isInitialized) {
            sessionManager.close()
        }
        desktopBitmap?.recycle()
        desktopBitmap = null
        RdpSessionCarrier.clear()
    }
}
