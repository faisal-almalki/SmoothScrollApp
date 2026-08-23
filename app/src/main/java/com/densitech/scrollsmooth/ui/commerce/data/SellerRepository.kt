package com.densitech.scrollsmooth.ui.commerce.data

import com.densitech.scrollsmooth.ui.commerce.model.Seller
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The signed in person, who is also a creator. Everyone on this platform can sell, so the
 * "become a seller" step is really just naming your shop.
 */
object SellerRepository {

    private const val KEY_ME = "my_seller"
    private const val KEY_FOLLOWING = "following"

    const val MY_SELLER_ID = "s_me"

    private val _me = MutableStateFlow(
        CommerceStore.readOrNull<Seller>(KEY_ME) ?: DEFAULT_ME
    )
    val me: StateFlow<Seller> = _me.asStateFlow()

    private val _following = MutableStateFlow(
        CommerceStore.readOrNull<Set<String>>(KEY_FOLLOWING) ?: emptySet()
    )
    val following: StateFlow<Set<String>> = _following.asStateFlow()

    fun updateShop(displayName: String, handle: String, bio: String, emoji: String) {
        val updated = _me.value.copy(
            displayName = displayName.ifBlank { _me.value.displayName },
            handle = handle.ifBlank { _me.value.handle }.removePrefix("@"),
            bio = bio,
            emoji = emoji.ifBlank { _me.value.emoji },
        )
        _me.value = updated
        CommerceStore.writeValue(KEY_ME, updated)
    }

    fun isMe(sellerId: String?): Boolean = sellerId == MY_SELLER_ID

    fun customSeller(id: String?): Seller? = if (isMe(id)) _me.value else null

    fun isFollowing(sellerId: String): Boolean = _following.value.contains(sellerId)

    fun toggleFollow(sellerId: String) {
        val updated = _following.value.toMutableSet()
        if (!updated.add(sellerId)) updated.remove(sellerId)
        _following.value = updated
        CommerceStore.writeValue<Set<String>>(KEY_FOLLOWING, updated)
    }

    private val DEFAULT_ME = Seller(
        id = MY_SELLER_ID,
        handle = "your.shop",
        displayName = "Your Shop",
        bio = "Tap edit to name your shop, then list your first product.",
        emoji = "🛍",
        rating = 5.0f,
        followers = 0,
        isVerified = false,
        shipsFrom = "Set in Creator Studio",
    )
}
