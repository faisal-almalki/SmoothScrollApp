package com.densitech.scrollsmooth.ui.commerce.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.ui.commerce.model.Cart
import com.densitech.scrollsmooth.ui.commerce.model.CartLine
import com.densitech.scrollsmooth.ui.commerce.model.formatMoney
import com.densitech.scrollsmooth.ui.commerce.view.BuyButton
import com.densitech.scrollsmooth.ui.commerce.view.CommerceColors
import com.densitech.scrollsmooth.ui.commerce.view.CommerceTopBar
import com.densitech.scrollsmooth.ui.commerce.view.CommerceDimens
import com.densitech.scrollsmooth.ui.commerce.view.EmptyState
import com.densitech.scrollsmooth.ui.commerce.view.ProductImage
import com.densitech.scrollsmooth.ui.commerce.view.QuantityStepper
import com.densitech.scrollsmooth.ui.commerce.viewmodel.CartViewModel
import com.densitech.scrollsmooth.ui.commerce.viewmodel.ShopViewModel
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple

@Composable
fun CartScreen(
    cartViewModel: CartViewModel,
    shopViewModel: ShopViewModel,
    onBack: () -> Unit,
    onCheckout: () -> Unit,
    onKeepShopping: () -> Unit,
    onOpenSeller: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cart by cartViewModel.cart.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CommerceColors.Background),
    ) {
        CommerceTopBar(
            title = "Cart",
            subtitle = if (cart.isEmpty) null else "${cart.itemCount} items",
            onBack = onBack,
            trailing = {
                if (!cart.isEmpty) {
                    Text(
                        text = "Clear",
                        color = CommerceColors.OnSurfaceMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.clickableNoRipple { cartViewModel.clearCart() },
                    )
                }
            },
        )

        if (cart.isEmpty) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    emoji = "🛒",
                    title = "Your cart is empty",
                    subtitle = "Tap the shop tag on any video, or browse the Shop tab.",
                    actionText = "Start shopping",
                    onActionClick = onKeepShopping,
                )
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = CommerceDimens.ScreenPadding,
                end = CommerceDimens.ScreenPadding,
                bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (cart.amountToFreeShippingCents > 0) {
                item {
                    Text(
                        text = "Add ${cart.amountToFreeShippingCents.formatMoney()} more for free shipping",
                        color = CommerceColors.Success,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CommerceColors.Success.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }

            items(cart.lines, key = { it.lineId }) { line ->
                CartLineRow(
                    line = line,
                    sellerHandle = shopViewModel.seller(line.sellerId)?.atHandle,
                    onIncrement = { cartViewModel.increment(line.lineId) },
                    onDecrement = { cartViewModel.decrement(line.lineId) },
                    onRemove = { cartViewModel.remove(line.lineId) },
                    onSellerClick = { onOpenSeller(line.sellerId) },
                )
            }

            item {
                Spacer(Modifier.height(6.dp))
                OrderTotals(cart = cart)
            }
        }

        CartCheckoutBar(
            totalCents = cart.totalCents,
            onCheckout = onCheckout,
        )
    }
}

@Composable
private fun CartLineRow(
    line: CartLine,
    sellerHandle: String?,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
    onSellerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CommerceDimens.CardCorner))
            .background(CommerceColors.Surface)
            .padding(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        ProductImage(
            seed = line.productId,
            emoji = line.emoji,
            imageUrl = line.imageUrl,
            emojiSize = 26,
            corner = 10.dp,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (sellerHandle != null) {
                Text(
                    text = sellerHandle,
                    color = CommerceColors.OnSurfaceMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickableNoRipple { onSellerClick() },
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = line.title,
                color = CommerceColors.OnSurface,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp,
            )
            if (line.selectedOptions.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = line.optionSummary,
                    color = CommerceColors.OnSurfaceMuted,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = line.lineTotalCents.formatMoney(),
                    color = CommerceColors.Accent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove from cart",
                        tint = CommerceColors.OnSurfaceMuted,
                        modifier = Modifier
                            .size(18.dp)
                            .clickableNoRipple { onRemove() },
                    )
                    Spacer(Modifier.width(10.dp))
                    QuantityStepper(
                        quantity = line.quantity,
                        onDecrement = onDecrement,
                        onIncrement = onIncrement,
                    )
                }
            }
        }
    }
}

@Composable
fun OrderTotals(
    cart: Cart,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CommerceDimens.CardCorner))
            .background(CommerceColors.Surface)
            .padding(14.dp),
    ) {
        TotalsRow(label = "Subtotal", value = cart.subtotalCents.formatMoney())
        Spacer(Modifier.height(8.dp))
        TotalsRow(
            label = "Shipping",
            value = if (cart.shippingCents == 0L) "Free" else cart.shippingCents.formatMoney(),
            valueColor = if (cart.shippingCents == 0L) CommerceColors.Success else CommerceColors.OnSurface,
        )
        Spacer(Modifier.height(8.dp))
        TotalsRow(label = "Estimated tax", value = cart.taxCents.formatMoney())
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(CommerceColors.Outline),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Total",
                color = CommerceColors.OnSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = cart.totalCents.formatMoney(),
                color = CommerceColors.Accent,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TotalsRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = CommerceColors.OnSurface,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, color = CommerceColors.OnSurfaceMuted, fontSize = 13.sp)
        Text(text = value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CartCheckoutBar(
    totalCents: Long,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CommerceColors.Surface)
            .navigationBarsPadding()
            .padding(CommerceDimens.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Total", color = CommerceColors.OnSurfaceMuted, fontSize = 11.sp)
            Text(
                text = totalCents.formatMoney(),
                color = CommerceColors.OnSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(16.dp))
        BuyButton(
            text = "Checkout",
            onClick = onCheckout,
            modifier = Modifier.weight(1.2f),
        )
    }
}
