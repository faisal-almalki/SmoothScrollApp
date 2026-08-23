package com.densitech.scrollsmooth.ui.commerce.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.densitech.scrollsmooth.ui.commerce.data.CommerceSync
import com.densitech.scrollsmooth.ui.commerce.data.api.ApiClient
import com.densitech.scrollsmooth.ui.commerce.data.api.ApiException
import com.densitech.scrollsmooth.ui.commerce.data.api.TokenStore
import com.densitech.scrollsmooth.ui.commerce.push.PushTokens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Which half of the sign-in flow is on screen. */
enum class AuthStep { PHONE, CODE }

class AuthViewModel() : ViewModel() {

    val isSignedIn = TokenStore.isSignedIn

    private val _step = MutableStateFlow(AuthStep.PHONE)
    val step = _step.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone = _phone.asStateFlow()

    private val _code = MutableStateFlow("")
    val code = _code.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    /**
     * Outside production the server hands the code back instead of texting it,
     * so the flow can be exercised before an SMS provider exists. Shown on
     * screen rather than only logged, because a tester should not need logcat.
     */
    private val _devCode = MutableStateFlow<String?>(null)
    val devCode = _devCode.asStateFlow()

    fun onPhoneChange(value: String) {
        _phone.value = value.filter { it.isDigit() || it == '+' }.take(16)
        _error.value = null
    }

    fun onCodeChange(value: String) {
        _code.value = value.filter { it.isDigit() }.take(6)
        _error.value = null
    }

    fun back() {
        _step.value = AuthStep.PHONE
        _code.value = ""
        _devCode.value = null
        _error.value = null
    }

    fun requestCode() {
        if (_busy.value) return
        val number = _phone.value.trim()
        if (number.length < 9) {
            _error.value = "Enter your mobile number, for example 05xxxxxxxx."
            return
        }

        _busy.value = true
        viewModelScope.launch {
            try {
                val result = ApiClient.requestOtp(number)
                _devCode.value = result.devCode
                _step.value = AuthStep.CODE
                _error.value = null
            } catch (api: ApiException) {
                _error.value = api.message
            } catch (error: Exception) {
                _error.value = "Could not reach the server. Check your connection."
            } finally {
                _busy.value = false
            }
        }
    }

    fun verifyCode() {
        if (_busy.value) return
        if (_code.value.length != 6) {
            _error.value = "The code is six digits."
            return
        }

        _busy.value = true
        viewModelScope.launch {
            try {
                ApiClient.verifyOtp(_phone.value.trim(), _code.value)
                _error.value = null
                _code.value = ""
                _devCode.value = null
                // Pull the account and its data straight away so the app does
                // not land on an empty screen after signing in.
                CommerceSync.refreshAll()
                // Now there is an account to attach this device to.
                PushTokens.register()
            } catch (api: ApiException) {
                _error.value = api.message
            } catch (error: Exception) {
                _error.value = "Could not reach the server. Check your connection."
            } finally {
                _busy.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            // Before the session is cleared, while the access token still works.
            // Afterwards the request would be rejected and this device would go
            // on receiving the previous user's messages.
            PushTokens.unregister()
            runCatching { ApiClient.signOut() }
            _step.value = AuthStep.PHONE
            _phone.value = ""
            _code.value = ""
        }
    }

    /**
     * Lets someone use the app without an account. Browsing is public, so the
     * login wall only appears when an action actually needs an identity.
     */
    fun continueWithoutSigningIn() {
        _error.value = null
    }
}
