package com.thisisnotajoke.marqueepass.di

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.thisisnotajoke.marqueepass.data.ShowRepository
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory

@DependencyGraph(scope = AppScope::class)
interface AppGraph {
    val viewModelFactory: MetroViewModelFactory

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides context: Context): AppGraph
    }

    @Provides
    fun provideFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    fun provideViewModelFactory(
        providers: Map<kotlin.reflect.KClass<out androidx.lifecycle.ViewModel>, () -> androidx.lifecycle.ViewModel>
    ): MetroViewModelFactory {
        return object : MetroViewModelFactory() {
            override val viewModelProviders = providers
            override val assistedFactoryProviders = emptyMap<kotlin.reflect.KClass<out androidx.lifecycle.ViewModel>, () -> dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory>()
            override val manualAssistedFactoryProviders = emptyMap<kotlin.reflect.KClass<out dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory>, () -> dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory>()
        }
    }
}
