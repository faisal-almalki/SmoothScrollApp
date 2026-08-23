package com.densitech.scrollsmooth.ui.commerce.model

import kotlinx.serialization.Serializable

@Serializable
data class Address(
    val fullName: String = "",
    val line1: String = "",
    val city: String = "",
    val postalCode: String = "",
    val country: String = "United States",
    val phone: String = "",
) {
    val isComplete: Boolean
        get() = fullName.isNotBlank() && line1.isNotBlank() && city.isNotBlank() && postalCode.isNotBlank()

    val singleLine: String get() = listOf(line1, city, postalCode, country).filter { it.isNotBlank() }.joinToString(", ")
}

@Serializable
enum class PaymentMethod(val label: String, val emoji: String) {
    CARD("Card ending 4242", "💳"),
    WALLET("ShopPay balance", "👛"),
    COD("Cash on delivery", "💵"),
}

@Serializable
enum class OrderStatus(val label: String) {
    PLACED("Order placed"),
    PACKED("Packed"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
}

@Serializable
data class OrderLine(
    val productId: String,
    val sellerId: String,
    val title: String,
    val emoji: String,
    val selectedOptions: Map<String, String> = emptyMap(),
    val quantity: Int,
    val unitPriceCents: Long,
) {
    val lineTotalCents: Long get() = unitPriceCents * quantity
}

@Serializable
data class Order(
    val id: String,
    val placedAtMillis: Long,
    val lines: List<OrderLine>,
    val subtotalCents: Long,
    val shippingCents: Long,
    val taxCents: Long,
    val totalCents: Long,
    val status: OrderStatus = OrderStatus.PLACED,
    val address: Address,
    val paymentMethod: PaymentMethod = PaymentMethod.CARD,
    /** Set when the order was placed from a live room, so the seller can attribute the sale. */
    val sourceLiveStreamId: String? = null,
    val sourceVideoId: String? = null,
) {
    val itemCount: Int get() = lines.sumOf { it.quantity }
}
