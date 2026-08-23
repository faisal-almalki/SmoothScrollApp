package com.densitech.scrollsmooth.ui.commerce.model

import kotlinx.serialization.Serializable

/**
 * A creator. Every creator on the platform is also a shop: the storefront and the video profile
 * are the same page.
 */
@Serializable
data class Seller(
    val id: String,
    val handle: String,
    val displayName: String,
    val bio: String,
    val emoji: String,
    val rating: Float = 4.8f,
    val followers: Int = 0,
    val isVerified: Boolean = false,
    val shipsFrom: String = "Los Angeles, CA",
) {
    val atHandle: String get() = "@$handle"
}
