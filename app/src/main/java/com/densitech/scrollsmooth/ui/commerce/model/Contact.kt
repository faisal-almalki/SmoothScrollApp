package com.densitech.scrollsmooth.ui.commerce.model

/**
 * How a buyer can reach a seller. Sellers opt in to phone calls; messaging is always available.
 */
enum class ContactMethod(val label: String, val emoji: String) {
    MESSAGE("Message", "💬"),
    CALL("Call", "📞"),
}

/** Whether a given seller currently offers a given contact method. */
fun Seller.supports(method: ContactMethod): Boolean = when (method) {
    ContactMethod.MESSAGE -> allowMessages
    ContactMethod.CALL -> allowCalls && phoneNumber.isNotBlank()
}
