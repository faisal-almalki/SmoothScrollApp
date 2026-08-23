package com.densitech.scrollsmooth.ui.commerce.view

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.densitech.scrollsmooth.ui.commerce.model.stableHash

/**
 * The commerce surfaces paint their own colours rather than leaning on the dynamic Material
 * scheme, so a product card looks the same on every device and reads correctly over video.
 */
object CommerceColors {
    val Background = Color(0xFF0B0B0D)
    val Surface = Color(0xFF16161A)
    val SurfaceElevated = Color(0xFF1F1F25)
    val Outline = Color(0xFF2C2C34)
    val OnSurface = Color(0xFFF4F4F6)
    val OnSurfaceMuted = Color(0xFF9A9AA6)

    /** The buy colour. Used only for actions that move money. */
    val Accent = Color(0xFFFE2C55)
    val AccentPressed = Color(0xFFD41F44)
    val Live = Color(0xFFFF2D55)
    val Success = Color(0xFF25D07A)
    val Warning = Color(0xFFFFC53D)
    val Discount = Color(0xFFFF4D4D)
    val Scrim = Color(0x99000000)
}

object CommerceDimens {
    val ScreenPadding = 16.dp
    val CardCorner = 14.dp
    val PillCorner = 999.dp
    val GridGutter = 10.dp
}

/**
 * There are no product photographs in this build, so each product renders as a stable gradient
 * derived from its id with the product emoji on top. The same product always gets the same
 * gradient, which makes the grids feel like a real catalogue rather than grey boxes.
 */
private val placeholderPalettes: List<Pair<Color, Color>> = listOf(
    Color(0xFF2B5876) to Color(0xFF4E4376),
    Color(0xFF603813) to Color(0xFFB29F94),
    Color(0xFF16222A) to Color(0xFF3A6073),
    Color(0xFF41295A) to Color(0xFF2F0743),
    Color(0xFF134E5E) to Color(0xFF71B280),
    Color(0xFF8E2DE2) to Color(0xFF4A00E0),
    Color(0xFFDD5E89) to Color(0xFFF7BB97),
    Color(0xFF0F2027) to Color(0xFF2C5364),
    Color(0xFF6A3093) to Color(0xFFA044FF),
    Color(0xFF334D50) to Color(0xFFCBCAA5),
    Color(0xFF1D976C) to Color(0xFF93F9B9),
    Color(0xFFB24592) to Color(0xFFF15F79),
)

fun placeholderBrush(seed: String): Brush {
    val palette = placeholderPalettes[stableHash(seed) % placeholderPalettes.size]
    return Brush.linearGradient(listOf(palette.first, palette.second))
}
