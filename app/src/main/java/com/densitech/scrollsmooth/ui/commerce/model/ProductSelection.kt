package com.densitech.scrollsmooth.ui.commerce.model

/**
 * What the shopper is buying: a product, the variant they picked, how many, and the price that
 * was on screen at the moment they tapped (a live room price can differ from the shelf price).
 */
data class ProductSelection(
    val product: Product,
    val options: Map<String, String>,
    val quantity: Int,
    val unitPriceCents: Long,
) {
    val totalCents: Long get() = unitPriceCents * quantity
}
