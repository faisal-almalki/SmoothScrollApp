package com.densitech.scrollsmooth.ui.commerce.data

import com.densitech.scrollsmooth.ui.commerce.model.Cart
import com.densitech.scrollsmooth.ui.commerce.model.CartLine
import com.densitech.scrollsmooth.ui.commerce.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for the cart, shared by the feed, the live rooms, the shop tab and
 * checkout. Everything that can add to the cart goes through [add].
 */
object CartRepository {

    private const val KEY_CART = "cart"

    private val _cart = MutableStateFlow(CommerceStore.readOrNull<Cart>(KEY_CART) ?: Cart())
    val cart: StateFlow<Cart> = _cart.asStateFlow()

    /** Emits every time something lands in the cart so the UI can show a confirmation. */
    private val _lastAdded = MutableStateFlow<CartLine?>(null)
    val lastAdded: StateFlow<CartLine?> = _lastAdded.asStateFlow()

    fun add(
        product: Product,
        selectedOptions: Map<String, String> = product.defaultSelection(),
        quantity: Int = 1,
        unitPriceCentsOverride: Long? = null,
    ) {
        if (quantity <= 0) return
        val lineId = CartLine.idFor(product.id, selectedOptions)
        val unitPrice = unitPriceCentsOverride ?: product.priceCents
        val existing = _cart.value.lines.firstOrNull { it.lineId == lineId }

        val updatedLines = if (existing != null) {
            _cart.value.lines.map {
                if (it.lineId == lineId) {
                    it.copy(
                        quantity = (it.quantity + quantity).coerceAtMost(MAX_QUANTITY_PER_LINE),
                        // A live-room price beats the shelf price the line was created with.
                        unitPriceCents = minOf(it.unitPriceCents, unitPrice),
                    )
                } else {
                    it
                }
            }
        } else {
            _cart.value.lines + CartLine(
                lineId = lineId,
                productId = product.id,
                sellerId = product.sellerId,
                title = product.title,
                emoji = product.emoji,
                selectedOptions = selectedOptions,
                quantity = quantity.coerceAtMost(MAX_QUANTITY_PER_LINE),
                unitPriceCents = unitPrice,
                imageUrl = product.imageUrl,
            )
        }

        persist(_cart.value.copy(lines = updatedLines))
        _lastAdded.value = updatedLines.first { it.lineId == lineId }
    }

    fun setQuantity(lineId: String, quantity: Int) {
        val updated = if (quantity <= 0) {
            _cart.value.lines.filterNot { it.lineId == lineId }
        } else {
            _cart.value.lines.map {
                if (it.lineId == lineId) it.copy(quantity = quantity.coerceAtMost(MAX_QUANTITY_PER_LINE)) else it
            }
        }
        persist(_cart.value.copy(lines = updated))
    }

    fun increment(lineId: String) {
        val line = _cart.value.lines.firstOrNull { it.lineId == lineId } ?: return
        setQuantity(lineId, line.quantity + 1)
    }

    fun decrement(lineId: String) {
        val line = _cart.value.lines.firstOrNull { it.lineId == lineId } ?: return
        setQuantity(lineId, line.quantity - 1)
    }

    fun remove(lineId: String) {
        persist(_cart.value.copy(lines = _cart.value.lines.filterNot { it.lineId == lineId }))
    }

    fun clear() {
        persist(Cart())
        _lastAdded.value = null
    }

    fun consumeLastAdded() {
        _lastAdded.value = null
    }

    private fun persist(cart: Cart) {
        _cart.value = cart
        CommerceStore.writeValue(KEY_CART, cart)
    }

    const val MAX_QUANTITY_PER_LINE = 99
}
