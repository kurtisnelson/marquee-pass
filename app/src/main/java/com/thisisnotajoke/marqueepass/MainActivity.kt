package com.thisisnotajoke.marqueepass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.thisisnotajoke.marqueepass.ui.screens.MainDashboard
import com.thisisnotajoke.marqueepass.ui.theme.MarqueePassTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MarqueePassTheme {
                MainDashboard()
            }
        }
    }
}
