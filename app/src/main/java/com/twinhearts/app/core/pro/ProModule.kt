package com.twinhearts.app.core.pro

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProModule {
    @Binds
    abstract fun bindProRepository(impl: ProRepositoryImpl): ProRepository

    companion object {
        @Provides
        @Singleton
        fun provideProApi(retrofit: Retrofit): ProApi = retrofit.create(ProApi::class.java)
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ProGateEntryPoint {
    fun proGate(): ProGate
}

/**
 * The shared [ProGate] for any composable, with no changes to ViewModel constructors:
 *     val gate = rememberProGate()
 *     onClick = { gate.require("Pin a Memory") { vm.requestPin() } }
 */
@Composable
fun rememberProGate(): ProGate {
    val app = LocalContext.current.applicationContext
    return remember(app) { EntryPointAccessors.fromApplication(app, ProGateEntryPoint::class.java).proGate() }
}
