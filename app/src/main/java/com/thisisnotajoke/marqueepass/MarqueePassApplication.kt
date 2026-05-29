package com.thisisnotajoke.marqueepass

import android.app.Application
import com.thisisnotajoke.marqueepass.di.AppGraph
import dev.zacsweers.metro.createGraphFactory

class MarqueePassApplication : Application() {
    lateinit var appGraph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        appGraph = createGraphFactory<AppGraph.Factory>().create(this)
    }
}
