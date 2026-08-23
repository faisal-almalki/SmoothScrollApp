package com.densitech.scrollsmooth.ui.commerce.data

import com.densitech.scrollsmooth.ui.commerce.model.Address
import com.densitech.scrollsmooth.ui.commerce.model.Cart
import com.densitech.scrollsmooth.ui.commerce.model.Order
import com.densitech.scrollsmooth.ui.commerce.model.OrderLine
import com.densitech.scrollsmooth.ui.commerce.model.OrderStatus
import com.densitech.scrollsmooth.ui.commerce.model.PaymentMethod
import com.densitech.scrollsmooth.ui.commerce.model.stableHash
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object OrderRepository {

    private const val KEY_ORDERS = "orders"
    private const val KEY_ADDRESS = "shipping_address"

    private val _orders = MutableStateFlow(
        CommerceStore.readOrNull<List<Order>>(KEY_ORDERS) ?: emptyList()
    )
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _savedAddress = MutableStateFlow(
        CommerceStore.readOrNull<Address>(KEY_ADDRESS) ?: DEMO_ADDRESS
    )
    val savedAddress: StateFlow<Address> = _savedAddress.asStateFlow()

    fun saveAddress(address: Address) {
        _savedAddress.value = address
        CommerceStore.writeValue(KEY_ADDRESS, address)
    }

    fun placeOrder(
        cart: Cart,
        address: Address,
        paymentMethod: PaymentMethod,
        nowMillis: Long,
        sourceLiveStreamId: String? = null,
        sourceVideoId: String? = null,
    ): Order? {
        if (cart.isEmpty) return null

        val order = Order(
            id = newOrderId(nowMillis),
            placedAtMillis = nowMillis,
            lines = cart.lines.map {
                OrderLine(
                    productId = it.productId,
                    sellerId = it.sellerId,
                    title = it.title,
                    emoji = it.emoji,
                    selectedOptions = it.selectedOptions,
                    quantity = it.quantity,
                    unitPriceCents = it.unitPriceCents,
                )
            },
            subtotalCents = cart.subtotalCents,
            shippingCents = cart.shippingCents,
            taxCents = cart.taxCents,
            totalCents = cart.totalCents,
            status = OrderStatus.PLACED,
            address = address,
            paymentMethod = paymentMethod,
            sourceLiveStreamId = sourceLiveStreamId,
            sourceVideoId = sourceVideoId,
        )

        val updated = listOf(order) + _orders.value
        _orders.value = updated
        CommerceStore.writeValue(KEY_ORDERS, updated)
        saveAddress(address)
        return order
    }

    fun order(id: String?): Order? = id?.let { wanted -> _orders.value.firstOrNull { it.id == wanted } }

    /** Orders that contain at least one line sold by this creator. */
    fun ordersForSeller(sellerId: String): List<Order> =
        _orders.value.filter { order -> order.lines.any { it.sellerId == sellerId } }

    fun grossRevenueCentsForSeller(sellerId: String): Long =
        _orders.value.sumOf { order ->
            order.lines.filter { it.sellerId == sellerId }.sumOf { it.lineTotalCents }
        }

    fun unitsSoldForSeller(sellerId: String): Int =
        _orders.value.sumOf { order ->
            order.lines.filter { it.sellerId == sellerId }.sumOf { it.quantity }
        }

    private fun newOrderId(nowMillis: Long): String {
        val suffix = (stableHash(nowMillis) % 100_000).toString().padStart(5, '0')
        return "SS-${nowMillis % 1_000_000}-$suffix"
    }

    private val DEMO_ADDRESS = Address(
        fullName = "",
        line1 = "",
        city = "",
        postalCode = "",
        country = "United States",
        phone = "",
    )
}
