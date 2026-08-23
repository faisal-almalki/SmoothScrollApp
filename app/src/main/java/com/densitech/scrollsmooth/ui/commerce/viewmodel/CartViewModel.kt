package com.densitech.scrollsmooth.ui.commerce.viewmodel

import androidx.lifecycle.ViewModel
import com.densitech.scrollsmooth.ui.commerce.data.CartRepository
import com.densitech.scrollsmooth.ui.commerce.data.OrderRepository
import com.densitech.scrollsmooth.ui.commerce.model.Address
import com.densitech.scrollsmooth.ui.commerce.model.Order
import com.densitech.scrollsmooth.ui.commerce.model.PaymentMethod
import com.densitech.scrollsmooth.ui.commerce.model.ProductSelection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Cart plus checkout. Kept together because the checkout form only makes sense against the cart
 * it is about to convert into an order.
 *
 * Held at activity scope by [com.densitech.scrollsmooth.ui.main.MainScreen] so that adding from a
 * feed video, a live room and the shop tab all land in the same cart.
 */
@HiltViewModel
class CartViewModel @Inject constructor() : ViewModel() {

    val cart = CartRepository.cart
    val orders = OrderRepository.orders

    private val _address = MutableStateFlow(OrderRepository.savedAddress.value)
    val address = _address.asStateFlow()

    private val _paymentMethod = MutableStateFlow(PaymentMethod.CARD)
    val paymentMethod = _paymentMethod.asStateFlow()

    /** Set after a successful checkout so the confirmation screen has something to show. */
    private val _lastPlacedOrder = MutableStateFlow<Order?>(null)
    val lastPlacedOrder = _lastPlacedOrder.asStateFlow()

    /** Transient "added to cart" confirmation shown over whichever surface triggered it. */
    private val _banner = MutableStateFlow<String?>(null)
    val banner = _banner.asStateFlow()

    /** Where the current cart came from, so a live sale can be attributed to the room. */
    private var pendingLiveStreamId: String? = null
    private var pendingVideoId: String? = null

    fun addToCart(
        selection: ProductSelection,
        liveStreamId: String? = null,
        videoId: String? = null,
    ) {
        CartRepository.add(
            product = selection.product,
            selectedOptions = selection.options,
            quantity = selection.quantity,
            unitPriceCentsOverride = selection.unitPriceCents,
        )
        if (liveStreamId != null) pendingLiveStreamId = liveStreamId
        if (videoId != null) pendingVideoId = videoId
        _banner.value = "${selection.product.title} added to your cart"
    }

    fun dismissBanner() {
        _banner.value = null
        CartRepository.consumeLastAdded()
    }

    fun increment(lineId: String) = CartRepository.increment(lineId)

    fun decrement(lineId: String) = CartRepository.decrement(lineId)

    fun remove(lineId: String) = CartRepository.remove(lineId)

    fun clearCart() = CartRepository.clear()

    fun updateAddress(address: Address) {
        _address.value = address
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        _paymentMethod.value = method
    }

    val canPlaceOrder: Boolean
        get() = !cart.value.isEmpty && _address.value.isComplete

    /**
     * Converts the cart into an order and empties it. Returns null when the cart is empty or the
     * address is incomplete, so the caller can keep the user on the form.
     */
    fun placeOrder(nowMillis: Long = System.currentTimeMillis()): Order? {
        if (!canPlaceOrder) return null
        val order = OrderRepository.placeOrder(
            cart = cart.value,
            address = _address.value,
            paymentMethod = _paymentMethod.value,
            nowMillis = nowMillis,
            sourceLiveStreamId = pendingLiveStreamId,
            sourceVideoId = pendingVideoId,
        ) ?: return null

        CartRepository.clear()
        pendingLiveStreamId = null
        pendingVideoId = null
        _lastPlacedOrder.value = order
        _banner.value = null
        return order
    }

    fun order(id: String?): Order? = OrderRepository.order(id)
}
