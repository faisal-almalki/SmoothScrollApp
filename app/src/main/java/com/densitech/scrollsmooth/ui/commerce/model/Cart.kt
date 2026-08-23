package com.densitech.scrollsmooth.ui.commerce.model

import kotlinx.serialization.Serializable

/**
 * One row of the cart. [lineId] folds the product and the chosen variant together so that the
 * same product in two sizes stays two separate rows.
 */
@Serializable
data class CartLine(
    val lineId: String,
    val productId: String,
    val sellerId: String,
    val title: String,
    val emoji: String,
    val selectedOptions: Map<String, String> = emptyMap(),
    val quantity: Int = 1,
    val unitPriceCents: Long,
    val imageUrl: String? = null,
) {
    val lineTotalCents: Long get() = unitPriceCents * quantity

    val optionSummary: String
        get() = selectedOptions.entries.joinToString(" · ") { it.value }

    companion object {
        fun idFor(productId: String, selectedOptions: Map<String, String>): String {
            if (selectedOptions.isEmpty()) return productId
            val suffix = selectedOptions.entries
                .sortedBy { it.key }
                .joinToString(",") { "${it.key}=${it.value}" }
            return "$productId|$suffix"
        }
    }
}

@Serializable
data class Cart(
    val lines: List<CartLine> = emptyList(),
) {
    val itemCount: Int get() = lines.sumOf { it.quantity }

    val subtotalCents: Long get() = lines.sumOf { it.lineTotalCents }

    val isEmpty: Boolean get() = lines.isEmpty()

    /** Shipping is free above the threshold, which the cart screen nudges the shopper towards. */
    val shippingCents: Long
        get() = when {
            lines.isEmpty() -> 0L
            subtotalCents >= FREE_SHIPPING_THRESHOLD_CENTS -> 0L
            else -> FLAT_SHIPPING_CENTS
        }

    val taxCents: Long get() = (subtotalCents * TAX_BASIS_POINTS) / 10_000L

    val totalCents: Long get() = subtotalCents + shippingCents + taxCents

    val amountToFreeShippingCents: Long
        get() = (FREE_SHIPPING_THRESHOLD_CENTS - subtotalCents).coerceAtLeast(0L)

    companion object {
        const val FREE_SHIPPING_THRESHOLD_CENTS = 3_500L
        const val FLAT_SHIPPING_CENTS = 499L
        const val TAX_BASIS_POINTS = 825L // 8.25%
    }
}
