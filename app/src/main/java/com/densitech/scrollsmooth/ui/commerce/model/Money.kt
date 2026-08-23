package com.densitech.scrollsmooth.ui.commerce.model

import java.util.Locale
import kotlin.math.roundToLong

/**
 * Every price in the app is stored as an integer number of minor units (cents) so that we never
 * accumulate rounding error while summing a cart. Formatting happens only at the edge, right
 * before the value is drawn.
 */
const val DEFAULT_CURRENCY_SYMBOL = "$"

fun Long.formatMoney(currencySymbol: String = DEFAULT_CURRENCY_SYMBOL): String {
    val negative = this < 0
    val absolute = if (negative) -this else this
    val formatted = String.format(Locale.US, "%s%d.%02d", currencySymbol, absolute / 100, absolute % 100)
    return if (negative) "-$formatted" else formatted
}

/** Compact form used on badges where horizontal space is scarce: 1.2K, 34.5K, 1.1M. */
fun Int.formatCompact(): String = toLong().formatCompact()

fun Long.formatCompact(): String = when {
    this >= 1_000_000 -> String.format(Locale.US, "%.1fM", this / 1_000_000.0)
    this >= 1_000 -> String.format(Locale.US, "%.1fK", this / 1_000.0)
    else -> toString()
}

fun percentOff(priceCents: Long, compareAtPriceCents: Long?): Int? {
    if (compareAtPriceCents == null || compareAtPriceCents <= priceCents) return null
    val off = (compareAtPriceCents - priceCents).toDouble() / compareAtPriceCents.toDouble()
    return (off * 100).roundToLong().toInt()
}

fun parsePriceToCents(input: String): Long? {
    val cleaned = input.trim().removePrefix(DEFAULT_CURRENCY_SYMBOL).replace(",", "")
    if (cleaned.isEmpty()) return null
    val value = cleaned.toDoubleOrNull() ?: return null
    if (value < 0) return null
    return (value * 100).roundToLong()
}

/**
 * A non-negative hash. `Int.MIN_VALUE.absoluteValue` is still negative, which would index an
 * array out of bounds, so mask the sign bit off instead of taking an absolute value.
 */
fun stableHash(value: String): Int = value.hashCode() and Int.MAX_VALUE

fun stableHash(value: Long): Int = (value xor (value ushr 32)).toInt() and Int.MAX_VALUE
