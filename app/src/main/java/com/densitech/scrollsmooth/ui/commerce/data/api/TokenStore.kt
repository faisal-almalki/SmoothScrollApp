package com.densitech.scrollsmooth.ui.commerce.data.api

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Where the signed-in session lives on the device.
 *
 * These are app-private SharedPreferences, which other apps cannot read on a
 * device that has not been rooted. That is deliberately not the strongest option
 * available: EncryptedSharedPreferences would also survive physical extraction
 * of the storage. It is not used here because the refresh token rotates on every
 * use and can be revoked server-side, so a stolen copy is short-lived — but if
 * this app ever holds anything more sensitive than a session, this is the first
 * thing to upgrade.
 */
object TokenStore {

    private const val PREFS_NAME = "commerce_session"
    private const val KEY_ACCESS = "access_token"
    private const val KEY_REFRESH = "refresh_token"
    private const val KEY_ACCOUNT = "account_id"

    @Volatile
    private var prefs: SharedPreferences? = null

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _accountId = MutableStateFlow<String?>(null)
    val accountId: StateFlow<String?> = _accountId.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        synchronized(this) {
            if (prefs == null) {
                prefs = context.applicationContext
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                _accountId.value = prefs?.getString(KEY_ACCOUNT, null)
                _isSignedIn.value = prefs?.getString(KEY_REFRESH, null) != null
            }
        }
    }

    val accessToken: String? get() = prefs?.getString(KEY_ACCESS, null)

    val refreshToken: String? get() = prefs?.getString(KEY_REFRESH, null)

    fun save(accountId: String, accessToken: String, refreshToken: String) {
        prefs?.edit()
            ?.putString(KEY_ACCOUNT, accountId)
            ?.putString(KEY_ACCESS, accessToken)
            ?.putString(KEY_REFRESH, refreshToken)
            ?.apply()
        _accountId.value = accountId
        _isSignedIn.value = true
    }

    /** Replaces just the pair after a refresh, keeping the account id. */
    fun updateTokens(accessToken: String, refreshToken: String) {
        prefs?.edit()
            ?.putString(KEY_ACCESS, accessToken)
            ?.putString(KEY_REFRESH, refreshToken)
            ?.apply()
    }

    fun clear() {
        prefs?.edit()?.clear()?.apply()
        _accountId.value = null
        _isSignedIn.value = false
    }
}
