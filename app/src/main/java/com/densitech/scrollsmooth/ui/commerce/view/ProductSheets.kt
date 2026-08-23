@file:OptIn(ExperimentalMaterial3Api::class)

package com.densitech.scrollsmooth.ui.commerce.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.R
import com.densitech.scrollsmooth.ui.commerce.data.ProductRepository
import com.densitech.scrollsmooth.ui.commerce.model.Product
import com.densitech.scrollsmooth.ui.commerce.model.ProductSelection
import com.densitech.scrollsmooth.ui.commerce.model.Seller
import com.densitech.scrollsmooth.ui.commerce.model.formatCompact
import com.densitech.scrollsmooth.ui.commerce.model.formatMoney
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple

/**
 * The buy sheet. Reached from a feed video, a live room, a storefront or the shop grid, so it
 * never assumes a navigation host: callers get the selection back and decide what happens next.
 */
@Composable
fun ProductDetailSheet(
    product: Product,
    onDismiss: () -> Unit,
    onAddToCart: (ProductSelection) -> Unit,
    onBuyNow: (ProductSelection) -> Unit,
    modifier: Modifier = Modifier,
    livePriceCents: Long? = null,
    liveBadge: String? = null,
    onOpenSeller: ((String) -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CommerceColors.Surface,
        contentColor = CommerceColors.OnSurface,
        dragHandle = { SheetGrabber() },
        modifier = modifier,
    ) {
        ProductDetailSheetContent(
            product = product,
            livePriceCents = livePriceCents,
            liveBadge = liveBadge,
            onAddToCart = onAddToCart,
            onBuyNow = onBuyNow,
            onOpenSeller = onOpenSeller,
        )
    }
}

@Composable
fun SheetGrabber(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(38.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(CommerceColors.Outline),
        )
    }
}

@Composable
fun ProductDetailSheetContent(
    product: Product,
    onAddToCart: (ProductSelection) -> Unit,
    onBuyNow: (ProductSelection) -> Unit,
    modifier: Modifier = Modifier,
    livePriceCents: Long? = null,
    liveBadge: String? = null,
    onOpenSeller: ((String) -> Unit)? = null,
) {
    val seller = ProductRepository.seller(product.sellerId)
    var selectedOptions by remember(product.id) { mutableStateOf(product.defaultSelection()) }
    var quantity by remember(product.id) { mutableStateOf(1) }

    // A live room price replaces the shelf price for as long as the sale is running.
    val effectivePriceCents = livePriceCents ?: product.priceCents
    val strikeThroughCents = if (livePriceCents != null) product.priceCents else product.compareAtPriceCents

    val selection = ProductSelection(
        product = product,
        options = selectedOptions,
        quantity = quantity,
        unitPriceCents = effectivePriceCents,
    )

    Column(modifier = modifier.navigationBarsPadding()) {
        Column(
            modifier = Modifier
                .heightIn(max = 460.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CommerceDimens.ScreenPadding),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                ProductImage(
                    seed = product.id,
                    emoji = product.emoji,
                    imageUrl = product.imageUrl,
                    modifier = Modifier.size(112.dp),
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (liveBadge != null) {
                        Text(
                            text = liveBadge,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CommerceColors.Live)
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    PriceRow(
                        priceCents = effectivePriceCents,
                        compareAtPriceCents = strikeThroughCents,
                        priceSize = 24,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = product.title,
                        color = CommerceColors.OnSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    RatingRow(
                        rating = product.rating,
                        ratingCount = product.ratingCount,
                        soldCount = product.soldCount,
                        textSize = 12,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            product.options.forEach { option ->
                OptionPicker(
                    optionName = option.name,
                    values = option.values,
                    selectedValue = selectedOptions[option.name],
                    onSelect = { value ->
                        selectedOptions = selectedOptions.toMutableMap().apply { put(option.name, value) }
                    },
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Quantity",
                    color = CommerceColors.OnSurfaceMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                QuantityStepper(
                    quantity = quantity,
                    onDecrement = { if (quantity > 1) quantity-- },
                    onIncrement = { if (quantity < product.stock) quantity++ },
                    maxQuantity = product.stock.coerceAtMost(99),
                )
            }

            if (product.stock in 1..5) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (product.stock == 1) "Only 1 left" else "Only ${product.stock} left",
                    color = CommerceColors.Discount,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(18.dp))

            if (seller != null) {
                SellerRow(
                    seller = seller,
                    onClick = onOpenSeller?.let { open -> { open(seller.id) } },
                )
                Spacer(Modifier.height(16.dp))
            }

            ShippingRow(
                freeShipping = product.freeShipping,
                shipsInDays = product.shipsInDays,
                shipsFrom = seller?.shipsFrom,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "About this item",
                color = CommerceColors.OnSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = product.description,
                color = CommerceColors.OnSurfaceMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
            Spacer(Modifier.height(20.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CommerceColors.Surface)
                .padding(CommerceDimens.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SecondaryButton(
                text = "Add to cart",
                onClick = { onAddToCart(selection) },
                enabled = product.isInStock,
                modifier = Modifier.weight(1f),
            )
            BuyButton(
                text = "Buy now · ${(effectivePriceCents * quantity).formatMoney()}",
                onClick = { onBuyNow(selection) },
                enabled = product.isInStock,
                modifier = Modifier.weight(1.3f),
            )
        }
    }
}

@Composable
fun SellerRow(
    seller: Seller,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CommerceDimens.CardCorner))
            .background(CommerceColors.SurfaceElevated)
            .let { if (onClick != null) it.clickableNoRipple { onClick() } else it }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SellerAvatar(seller = seller, size = 40.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = seller.displayName,
                    color = CommerceColors.OnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (seller.isVerified) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_verified_24),
                        contentDescription = "Verified seller",
                        tint = CommerceColors.Accent,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Text(
                text = "${seller.followers.formatCompact()} followers · ⭐ ${
                    String.format(java.util.Locale.US, "%.1f", seller.rating)
                } seller rating",
                color = CommerceColors.OnSurfaceMuted,
                fontSize = 11.sp,
            )
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = CommerceColors.OnSurfaceMuted,
            )
        }
    }
}

@Composable
fun ShippingRow(
    freeShipping: Boolean,
    shipsInDays: Int,
    modifier: Modifier = Modifier,
    shipsFrom: String? = null,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = R.drawable.ic_truck_24),
            contentDescription = null,
            tint = CommerceColors.Success,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = if (freeShipping) "Free shipping" else "Standard shipping",
                color = CommerceColors.OnSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = buildString {
                    append("Arrives in about $shipsInDays days")
                    if (shipsFrom != null) append(" · from $shipsFrom")
                },
                color = CommerceColors.OnSurfaceMuted,
                fontSize = 11.sp,
            )
        }
    }
}

/**
 * The list of everything a single video or live room is selling. Tapping a row opens the buy
 * sheet for that product.
 */
@Composable
fun ProductListSheet(
    title: String,
    products: List<Product>,
    onDismiss: () -> Unit,
    onProductClick: (Product) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CommerceColors.Surface,
        contentColor = CommerceColors.OnSurface,
        dragHandle = { SheetGrabber() },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = CommerceDimens.ScreenPadding)
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = title,
                color = CommerceColors.OnSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(text = subtitle, color = CommerceColors.OnSurfaceMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                products.forEachIndexed { index, product ->
                    ProductListRow(
                        product = product,
                        index = index + 1,
                        onClick = { onProductClick(product) },
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun ProductListRow(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int? = null,
    trailingLabel: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CommerceDimens.CardCorner))
            .background(CommerceColors.SurfaceElevated)
            .clickableNoRipple { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            ProductImage(
                seed = product.id,
                emoji = product.emoji,
                imageUrl = product.imageUrl,
                emojiSize = 28,
                corner = 10.dp,
                modifier = Modifier.size(64.dp),
            )
            if (index != null) {
                Text(
                    text = index.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CommerceColors.Scrim)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.title,
                color = CommerceColors.OnSurface,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp,
            )
            Spacer(Modifier.height(4.dp))
            PriceRow(
                priceCents = product.priceCents,
                compareAtPriceCents = product.compareAtPriceCents,
                priceSize = 15,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = trailingLabel ?: "${product.soldCount.formatCompact()} sold",
                color = CommerceColors.OnSurfaceMuted,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Buy",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(CommerceDimens.PillCorner))
                .background(CommerceColors.Accent)
                .clickableNoRipple { onClick() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

/** Brief confirmation that something landed in the cart. */
@Composable
fun AddedToCartBanner(
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onViewCart: (() -> Unit)? = null,
) {
    LaunchedEffect(text) {
        kotlinx.coroutines.delay(2_600)
        onDismiss()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CommerceDimens.CardCorner))
            .background(CommerceColors.SurfaceElevated)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "✅", fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            color = CommerceColors.OnSurface,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (onViewCart != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "View cart",
                color = CommerceColors.Accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickableNoRipple { onViewCart() },
            )
        }
    }
}
