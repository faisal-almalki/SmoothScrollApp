package com.densitech.scrollsmooth.ui.commerce.model

import kotlinx.serialization.Serializable

/**
 * A single sellable item. Products are always owned by a [Seller]; there is no first party
 * inventory in this app, every listing belongs to a creator.
 */
@Serializable
data class Product(
    val id: String,
    val sellerId: String,
    val title: String,
    val description: String,
    val priceCents: Long,
    val compareAtPriceCents: Long? = null,
    val category: String,
    val emoji: String,
    val rating: Float = 4.7f,
    val ratingCount: Int = 0,
    val soldCount: Int = 0,
    val stock: Int = 250,
    val options: List<ProductOption> = emptyList(),
    val freeShipping: Boolean = true,
    val shipsInDays: Int = 3,
    val imageUrl: String? = null,
) {
    val isInStock: Boolean get() = stock > 0

    val discountPercent: Int? get() = percentOff(priceCents, compareAtPriceCents)

    /** The cheapest valid selection, used to pre-fill a variant picker. */
    fun defaultSelection(): Map<String, String> =
        options.filter { it.values.isNotEmpty() }.associate { it.name to it.values.first() }
}

/** A variant axis, e.g. name = "Size", values = ["S", "M", "L"]. */
@Serializable
data class ProductOption(
    val name: String,
    val values: List<String>,
)

@Serializable
enum class ProductCategory(val label: String, val emoji: String) {
    ALL("All", "🔥"),
    BEAUTY("Beauty", "💄"),
    FASHION("Fashion", "👗"),
    TECH("Tech", "🎧"),
    HOME("Home", "🍽"),
    FITNESS("Fitness", "🏋"),
    SNACKS("Snacks", "🍫"),
}
