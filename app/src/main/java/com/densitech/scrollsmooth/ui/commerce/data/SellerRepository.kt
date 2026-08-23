package com.densitech.scrollsmooth.ui.commerce.data

import com.densitech.scrollsmooth.ui.commerce.model.Seller
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The signed in person, who is also a seller. Everyone here can post an ad, so "become a seller"
 * is really just filling in your name and deciding how buyers may reach you.
 */
object SellerRepository {

    const val MY_SELLER_ID = "s_me"

    private const val KEY_ME = "my_seller"
    private const val KEY_FOLLOWING = "following"

    // Declared before the state that reads it: an object initialises its properties top to
    // bottom, so a default referenced from above would still be null.
    private val DEFAULT_ME = Seller(
        id = MY_SELLER_ID,
        handle = "your.account",
        displayName = "Your account",
        bio = "Add your name and city, then post your first ad.",
        emoji = "🙂",
        rating = 5.0f,
        ratingCount = 0,
        followers = 0,
        isVerified = false,
        city = "Riyadh",
        phoneNumber = "",
        allowCalls = false,
        allowMessages = true,
    )

    private val _me = MutableStateFlow(CommerceStore.readOrNull<Seller>(KEY_ME) ?: DEFAULT_ME)
    val me: StateFlow<Seller> = _me.asStateFlow()

    private val _following = MutableStateFlow(
        CommerceStore.readOrNull<Set<String>>(KEY_FOLLOWING) ?: emptySet()
    )
    val following: StateFlow<Set<String>> = _following.asStateFlow()

    fun updateProfile(
        displayName: String,
        handle: String,
        bio: String,
        emoji: String,
        city: String,
    ) {
        val updated = _me.value.copy(
            displayName = displayName.ifBlank { _me.value.displayName },
            handle = handle.ifBlank { _me.value.handle }.removePrefix("@"),
            bio = bio,
            emoji = emoji.ifBlank { _me.value.emoji },
            city = city.ifBlank { _me.value.city },
        )
        persistMe(updated)
    }

    /**
     * Publishing a number is an explicit, reversible choice. Clearing the number also turns calls
     * off, so a seller can never end up with calls enabled and nothing to dial.
     */
    fun updateContactPreferences(
        phoneNumber: String,
        allowCalls: Boolean,
        allowMessages: Boolean,
    ) {
        val cleanedNumber = phoneNumber.trim()
        persistMe(
            _me.value.copy(
                phoneNumber = cleanedNumber,
                allowCalls = allowCalls && cleanedNumber.isNotBlank(),
                allowMessages = allowMessages,
            )
        )
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

    private fun persistMe(seller: Seller) {
        _me.value = seller
        CommerceStore.writeValue(KEY_ME, seller)
    }
}
