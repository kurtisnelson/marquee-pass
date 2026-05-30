package com.thisisnotajoke.marqueepass

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.thisisnotajoke.marqueepass.di.AppGraph
import dev.zacsweers.metro.createGraphFactory

class MarqueePassApplication : Application() {
    lateinit var appGraph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        appGraph = createGraphFactory<AppGraph.Factory>().create(this)
        setupCrashlytics()
    }

    private fun setupCrashlytics() {
        val crashlytics = FirebaseCrashlytics.getInstance()

        // Keep the Crashlytics user ID in sync with Firebase Auth so crash
        // reports are attributed to the correct user (anonymous or signed-in).
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                crashlytics.setUserId(user.uid)
                crashlytics.setCustomKey("is_anonymous", user.isAnonymous)
            } else {
                crashlytics.setUserId("")
            }
        }
    }
}
