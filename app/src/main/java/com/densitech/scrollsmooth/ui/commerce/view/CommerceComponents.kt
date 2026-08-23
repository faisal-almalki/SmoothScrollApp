@file:OptIn(ExperimentalLayoutApi::class)

package com.densitech.scrollsmooth.ui.commerce.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.densitech.scrollsmooth.R
import com.densitech.scrollsmooth.ui.commerce.model.Product
import com.densitech.scrollsmooth.ui.commerce.model.Seller
import com.densitech.scrollsmooth.ui.commerce.model.formatCompact
import com.densitech.scrollsmooth.ui.commerce.model.formatMoney
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple

/**
 * Square product artwork. Falls back to a deterministic gradient plus the product emoji when the
 * listing has no photograph, which is every seeded product in this build.
 */
@Composable
fun ProductImage(
    seed: String,
    emoji: String,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    corner: Dp = CommerceDimens.CardCorner,
    emojiSize: Int = 44,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(placeholderBrush(seed)),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(text = emoji, fontSize = emojiSize.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun SellerAvatar(
    seller: Seller,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(placeholderBrush(seller.id)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = seller.emoji, fontSize = (size.value / 2).sp)
    }
}

@Composable
fun PriceRow(
    priceCents: Long,
    modifier: Modifier = Modifier,
    compareAtPriceCents: Long? = null,
    priceSize: Int = 16,
    accent: Color = CommerceColors.Accent,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = priceCents.formatMoney(),
            color = accent,
            fontSize = priceSize.sp,
            fontWeight = FontWeight.Bold,
        )
        if (compareAtPriceCents != null && compareAtPriceCents > priceCents) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = compareAtPriceCents.formatMoney(),
                color = CommerceColors.OnSurfaceMuted,
                fontSize = (priceSize - 4).sp,
                textDecoration = TextDecoration.LineThrough,
            )
        }
    }
}

@Composable
fun DiscountBadge(percentOff: Int, modifier: Modifier = Modifier) {
    Text(
        text = "-$percentOff%",
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CommerceColors.Discount)
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

@Composable
fun RatingRow(
    rating: Float,
    ratingCount: Int,
    soldCount: Int,
    modifier: Modifier = Modifier,
    textSize: Int = 11,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = CommerceColors.Warning,
            modifier = Modifier.size((textSize + 3).dp),
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = String.format(java.util.Locale.US, "%.1f", rating),
            color = CommerceColors.OnSurfaceMuted,
            fontSize = textSize.sp,
        )
        if (ratingCount > 0) {
            Text(
                text = " (${ratingCount.formatCompact()})",
                color = CommerceColors.OnSurfaceMuted,
                fontSize = textSize.sp,
            )
        }
        if (soldCount > 0) {
            Text(
                text = " · ${soldCount.formatCompact()} sold",
                color = CommerceColors.OnSurfaceMuted,
                fontSize = textSize.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Filled call to action. This is the only button style that moves money. */
@Composable
fun BuyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = CommerceColors.Accent,
    contentColor: Color = Color.White,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(CommerceDimens.PillCorner))
            .background(if (enabled) container else CommerceColors.Outline)
            .clickableNoRipple(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) contentColor else CommerceColors.OnSurfaceMuted,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(CommerceDimens.PillCorner))
            .border(1.dp, CommerceColors.Outline, RoundedCornerShape(CommerceDimens.PillCorner))
            .clickableNoRipple(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) CommerceColors.OnSurface else CommerceColors.OnSurfaceMuted,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Minus / count / plus control shared by the cart and the product sheet. */
@Composable
fun QuantityStepper(
    quantity: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
    minQuantity: Int = 1,
    maxQuantity: Int = 99,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(CommerceDimens.PillCorner))
            .border(1.dp, CommerceColors.Outline, RoundedCornerShape(CommerceDimens.PillCorner)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperIcon(
            enabled = quantity > minQuantity,
            onClick = onDecrement,
            content = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_minus_24),
                    contentDescription = "Decrease quantity",
                    tint = it,
                    modifier = Modifier.size(16.dp),
                )
            },
        )
        Text(
            text = quantity.toString(),
            color = CommerceColors.OnSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(34.dp),
        )
        StepperIcon(
            enabled = quantity < maxQuantity,
            onClick = onIncrement,
            content = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase quantity",
                    tint = it,
                    modifier = Modifier.size(16.dp),
                )
            },
        )
    }
}

@Composable
private fun StepperIcon(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable (Color) -> Unit,
) {
    val tint = if (enabled) CommerceColors.OnSurface else CommerceColors.OnSurfaceMuted
    Box(
        modifier = Modifier
            .size(34.dp)
            .clickableNoRipple(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        content(tint)
    }
}

/** Selectable chip used for categories and for product variant values. */
@Composable
fun CommerceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(CommerceDimens.PillCorner))
            .background(if (selected) CommerceColors.Accent else CommerceColors.SurfaceElevated)
            .border(
                width = 1.dp,
                color = if (selected) CommerceColors.Accent else CommerceColors.Outline,
                shape = RoundedCornerShape(CommerceDimens.PillCorner),
            )
            .clickableNoRipple { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else CommerceColors.OnSurface,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

/** One variant axis rendered as a wrapping row of chips. */
@Composable
fun OptionPicker(
    optionName: String,
    values: List<String>,
    selectedValue: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = optionName,
            color = CommerceColors.OnSurfaceMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            values.forEach { value ->
                CommerceChip(
                    text = value,
                    selected = value == selectedValue,
                    onClick = { onSelect(value) },
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            color = CommerceColors.OnSurface,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
        if (action != null && onActionClick != null) {
            Text(
                text = action,
                color = CommerceColors.Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickableNoRipple { onActionClick() },
            )
        }
    }
}

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = emoji, fontSize = 52.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            color = CommerceColors.OnSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = CommerceColors.OnSurfaceMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        if (actionText != null && onActionClick != null) {
            Spacer(Modifier.height(20.dp))
            BuyButton(
                text = actionText,
                onClick = onActionClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Grid tile used on the shop tab and on a creator storefront. */
@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sellerHandle: String? = null,
    onAddToCart: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(CommerceDimens.CardCorner))
            .background(CommerceColors.Surface)
            .clickableNoRipple { onClick() },
    ) {
        Box {
            ProductImage(
                seed = product.id,
                emoji = product.emoji,
                imageUrl = product.imageUrl,
                corner = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
            product.discountPercent?.let { percent ->
                DiscountBadge(
                    percentOff = percent,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                )
            }
            if (!product.isInStock) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(CommerceColors.Scrim),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "Sold out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            if (onAddToCart != null && product.isInStock) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CommerceColors.Accent)
                        .clickableNoRipple { onAddToCart() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add to cart",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = product.title,
                color = CommerceColors.OnSurface,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp,
            )
            Spacer(Modifier.height(6.dp))
            PriceRow(
                priceCents = product.priceCents,
                compareAtPriceCents = product.compareAtPriceCents,
                priceSize = 15,
            )
            Spacer(Modifier.height(4.dp))
            RatingRow(
                rating = product.rating,
                ratingCount = product.ratingCount,
                soldCount = product.soldCount,
            )
            if (sellerHandle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = sellerHandle,
                    color = CommerceColors.OnSurfaceMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
