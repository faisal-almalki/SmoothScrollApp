package com.densitech.scrollsmooth.ui.commerce.data.api

import kotlinx.serialization.Serializable

/**
 * The server returns one error shape for everything: { error: { code, message } }.
 *
 * `message` is written to be shown to a person as-is, so screens display it
 * directly rather than inventing their own wording; `code` is the stable string
 * to branch on when a screen needs to react to a specific failure.
 */
@Serializable
data class ApiErrorBody(val error: ApiErrorDetail)

@Serializable
data class ApiErrorDetail(
    val code: String,
    val message: String,
)

class ApiException(
    val status: Int,
    val code: String,
    override val message: String,
) : Exception(message) {

    val isUnauthorized: Boolean get() = status == 401
    val isRateLimited: Boolean get() = status == 429

    companion object {
        /** Used when the failure was the network itself, so nothing came back to parse. */
        fun offline(cause: Throwable) = ApiException(
            status = 0,
            code = "offline",
            message = "No connection. Check your internet and try again.",
        ).also { it.initCause(cause) }

        fun unreadable(status: Int) = ApiException(
            status = status,
            code = "unreadable_response",
            message = "Something went wrong. Try again.",
        )
    }
}
