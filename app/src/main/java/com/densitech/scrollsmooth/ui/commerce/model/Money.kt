package com.densitech.scrollsmooth.ui.commerce.model

import java.util.Locale
import kotlin.math.roundToLong

/**
 * Prices are stored as an integer number of minor units (halalas) so arithmetic never drifts.
 * Formatting happens only at the edge, right before a price is drawn.
 */
const val CURRENCY_CODE = "SAR"

/**
 * Classified ads are almost always whole amounts, so the minor units are dropped unless the
 * price actually has them. Thousands are grouped, because "SAR 45000" is hard to read at a glance.
 */
fun Long.formatMoney(currency: String = CURRENCY_CODE): String {
    val negative = this < 0
    val absolute = if (negative) -this else this
    val major = absolute / 100
    val minor = absolute % 100
    val grouped = String.format(Locale.US, "%,d", major)
    val body = if (minor == 0L) {
        "$currency $grouped"
    } else {
        String.format(Locale.US, "%s %s.%02d", currency, grouped, minor)
    }
    return if (negative) "-$body" else body
}

/** Compact form for badges where horizontal space is scarce: 1.2K, 34.5K, 1.1M. */
fun Int.formatCompact(): String = toLong().formatCompact()

fun Long.formatCompact(): String = when {
    this >= 1_000_000 -> String.format(Locale.US, "%.1fM", this / 1_000_000.0)
    this >= 1_000 -> String.format(Locale.US, "%.1fK", this / 1_000.0)
    else -> toString()
}

fun parsePriceToCents(input: String): Long? {
    val cleaned = input.trim()
        .removePrefix(CURRENCY_CODE)
        .trim()
        .replace(",", "")
    if (cleaned.isEmpty()) return null
    val value = cleaned.toDoubleOrNull() ?: return null
    if (value < 0) return null
    return (value * 100).roundToLong()
}

/** Relative age of an ad, the way a classifieds listing reads: "3 h ago", "2 d ago". */
fun formatPostedAge(postedAtMillis: Long, nowMillis: Long): String {
    val elapsed = (nowMillis - postedAtMillis).coerceAtLeast(0L)
    val minutes = elapsed / 60_000L
    val hours = minutes / 60L
    val days = hours / 24L
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours h ago"
        days < 30 -> "$days d ago"
        else -> "${days / 30} mo ago"
    }
}

/**
 * A non-negative hash. `Int.MIN_VALUE.absoluteValue` is still negative, which would index an
 * array out of bounds, so mask the sign bit off instead of taking an absolute value.
 */
fun stableHash(value: String): Int = value.hashCode() and Int.MAX_VALUE

fun stableHash(value: Long): Int = (value xor (value ushr 32)).toInt() and Int.MAX_VALUE

/** Digits only, so a stored number can be handed to a dialler intent safely. */
fun String.toDialableNumber(): String = filter { it.isDigit() || it == '+' }
