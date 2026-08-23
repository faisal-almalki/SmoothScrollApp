package com.densitech.scrollsmooth.ui.commerce.model

import kotlinx.serialization.Serializable

/**
 * Someone who posts ads. Everybody on the platform can sell, so this is really the user profile;
 * the storefront and the creator page are the same page.
 *
 * Contact preferences live here rather than on a listing: a seller decides once whether their
 * number is public, and every ad they post follows that decision.
 */
@Serializable
data class Seller(
    val id: String,
    val handle: String,
    val displayName: String,
    val bio: String,
    val emoji: String,
    val rating: Float = 4.8f,
    val ratingCount: Int = 0,
    val followers: Int = 0,
    val isVerified: Boolean = false,
    val city: String = "Riyadh",
    val phoneNumber: String = "",
    /** Off by default. A number is only shown once the seller chooses to publish it. */
    val allowCalls: Boolean = false,
    val allowMessages: Boolean = true,
    val memberSinceMillis: Long = 0L,
) {
    val atHandle: String get() = "@$handle"

    val hasPublicPhone: Boolean get() = allowCalls && phoneNumber.isNotBlank()
}
