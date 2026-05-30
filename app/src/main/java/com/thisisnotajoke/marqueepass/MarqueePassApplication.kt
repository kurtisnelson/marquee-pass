package com.thisisnotajoke.marqueepass

import android.app.Application
import com.google.firebase.analytics.FirebaseAnalytics
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
        setupFirebaseServices()
    }

    private fun setupFirebaseServices() {
        val crashlytics = FirebaseCrashlytics.getInstance()
        val analytics = FirebaseAnalytics.getInstance(this)

        // Keep the Crashlytics and Analytics user ID in sync with Firebase Auth
        // so reports and user properties are attributed to the correct user.
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                crashlytics.setUserId(user.uid)
                crashlytics.setCustomKey("is_anonymous", user.isAnonymous)
                
                analytics.setUserId(user.uid)
                analytics.setUserProperty("is_anonymous", user.isAnonymous.toString())
            } else {
                crashlytics.setUserId("")
                analytics.setUserId(null)
            }
        }
    }
}
