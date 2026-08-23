package com.densitech.scrollsmooth.ui.commerce.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.json.Json

/**
 * Thin persistence seam for the commerce feature. The repositories keep their state in memory and
 * mirror it here so a cart or an order list survives process death.
 *
 * [init] is called once from the Application entry point; every read before that returns the
 * fallback, so the repositories still work in previews and unit tests.
 */
object CommerceStore {

    private const val PREFS_NAME = "commerce_store"

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Volatile
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        synchronized(this) {
            if (prefs == null) {
                prefs = context.applicationContext
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }
        }
    }

    fun read(key: String): String? = prefs?.getString(key, null)

    fun write(key: String, value: String) {
        prefs?.edit()?.putString(key, value)?.apply()
    }

    fun clear(key: String) {
        prefs?.edit()?.remove(key)?.apply()
    }

    /** Decoding never throws outwards: a stored blob from an older schema is dropped, not fatal. */
    inline fun <reified T> readOrNull(key: String): T? {
        val raw = read(key) ?: return null
        return runCatching { json.decodeFromString<T>(raw) }.getOrNull()
    }

    inline fun <reified T> writeValue(key: String, value: T) {
        runCatching { write(key, json.encodeToString(value)) }
    }
}
