package com.thisisnotajoke.marqueepass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.thisisnotajoke.marqueepass.ui.screens.MainDashboard
import com.thisisnotajoke.marqueepass.ui.theme.MarqueePassTheme

import androidx.compose.runtime.CompositionLocalProvider
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import com.thisisnotajoke.marqueepass.MarqueePassApplication

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appGraph = (application as MarqueePassApplication).appGraph
        setContent {
            CompositionLocalProvider(
                LocalMetroViewModelFactory provides appGraph.viewModelFactory
            ) {
                MarqueePassTheme {
                    MainDashboard()
                }
            }
        }
    }
}
