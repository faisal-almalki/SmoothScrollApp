package com.densitech.scrollsmooth.ui.media

import android.content.Context
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cronet.CronetDataSource
import androidx.media3.datasource.cronet.CronetUtil
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import java.util.concurrent.Executors

/**
 * The one place video HTTP traffic gets its transport.
 *
 * Cronet is worth having — it brings HTTP/3 and connection reuse tuned for
 * media — but it is not guaranteed to exist. `CronetEngine.Builder(context)
 * .build()` throws `RuntimeException: All available Cronet providers are
 * disabled` when no provider is installed, which is the case on any build
 * without Google Play services: AOSP emulators, de-Googled phones, and much of
 * the Chinese Android market.
 *
 * Three call sites used to construct that engine directly, so on those devices
 * the app died the moment it tried to play anything. `CronetUtil.buildCronetEngine`
 * is Media3's own answer: it looks for a provider and returns null instead of
 * throwing, which turns a crash into a choice.
 *
 * The fallback is OkHttp, which the app already depends on. Playback is
 * slightly less efficient and completely functional.
 */
@UnstableApi
object MediaHttpDataSource {

    private const val TAG = "MediaHttpDataSource"

    @Volatile
    private var cached: DataSource.Factory? = null

    /**
     * Built once and shared. Each engine owns a thread pool and a connection
     * pool, so the previous arrangement — one per player, one per download
     * manager — was paying for that several times over.
     */
    fun factory(context: Context): DataSource.Factory =
        cached ?: synchronized(this) {
            cached ?: build(context.applicationContext).also { cached = it }
        }

    private fun build(context: Context): DataSource.Factory {
        val engine = runCatching { CronetUtil.buildCronetEngine(context) }
            .onFailure { Log.w(TAG, "Cronet unavailable: ${it.message}") }
            .getOrNull()

        if (engine != null) {
            Log.i(TAG, "Using Cronet for media")
            return CronetDataSource.Factory(engine, Executors.newSingleThreadExecutor())
        }

        Log.i(TAG, "No Cronet provider; using OkHttp for media")
        return OkHttpDataSource.Factory(OkHttpClient())
    }
}
