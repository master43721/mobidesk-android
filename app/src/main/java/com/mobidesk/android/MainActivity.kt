package com.mobidesk.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mobidesk.android.ui.MobiDeskApp
import com.mobidesk.android.ui.MobiDeskTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MobiDeskTheme {
                MobiDeskApp()
            }
        }
    }
}
