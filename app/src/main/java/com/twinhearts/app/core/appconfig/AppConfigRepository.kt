package com.twinhearts.app.core.appconfig

import android.content.Context
import com.twinhearts.app.BuildConfig
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

/** An update the admin published for this app. [required] = the person cannot dismiss it. */
data class UpdateInfo(
    val version: String,
    val title: String,
    val message: String,
    val url: String?,
    val required: Boolean,
)

/**
 * Keeps two things the admin dashboard controls: the "update available / required" notice and
 * the feature flags. If the server is down or offline, the last known flags are kept and no
 * update screen is invented (a version-check outage must never lock anyone out).
 */
@Singleton
class AppConfigRepository @Inject constructor(
    private val api: AppConfigApi,
    private val sessionStore: SessionStore,
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("app_config", Context.MODE_PRIVATE)

    private val _update = MutableStateFlow<UpdateInfo?>(null)
    val update: StateFlow<UpdateInfo?> = _update.asStateFlow()

    private val _flags = MutableStateFlow(loadCachedFlags())
    val flags: StateFlow<Map<String, Boolean>> = _flags.asStateFlow()

    /** True when the admin turned [key] on for this person; [default] when the flag does not exist. */
    fun isOn(key: String, default: Boolean = false): Boolean = _flags.value[key] ?: default

    suspend fun refresh() {
        val userId = sessionStore.session.first()?.userId

        val release = safeApiCall { api.checkRelease(BuildConfig.VERSION_NAME, "android") }
        if (release is AppResult.Success) {
            val d = release.data
            val rel = d.release
            _update.value = if (d.updateAvailable && rel != null) {
                UpdateInfo(
                    version = rel.version,
                    title = rel.title.orEmpty(),
                    message = rel.message.orEmpty(),
                    url = rel.updateUrl,
                    required = d.updateType == "required",
                )
            } else null
        }

        val flags = safeApiCall { api.flags(userId) }
        if (flags is AppResult.Success && !flags.data.degraded) {
            _flags.value = flags.data.flags
            prefs.edit().putStringSet(KEY_ON, flags.data.flags.filterValues { it }.keys)
                .putStringSet(KEY_OFF, flags.data.flags.filterValues { !it }.keys).apply()
        }
    }

    private fun loadCachedFlags(): Map<String, Boolean> {
        val on = prefs.getStringSet(KEY_ON, emptySet()).orEmpty().associateWith { true }
        val off = prefs.getStringSet(KEY_OFF, emptySet()).orEmpty().associateWith { false }
        return on + off
    }

    private companion object {
        const val KEY_ON = "flags_on"
        const val KEY_OFF = "flags_off"
    }
}

@Module
@InstallIn(SingletonComponent::class)
object AppConfigModule {
    @Provides
    @Singleton
    fun provideAppConfigApi(retrofit: Retrofit): AppConfigApi = retrofit.create(AppConfigApi::class.java)
}
