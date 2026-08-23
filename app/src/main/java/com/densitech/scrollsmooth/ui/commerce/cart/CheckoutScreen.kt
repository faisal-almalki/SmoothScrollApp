package com.densitech.scrollsmooth.ui.commerce.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.ui.commerce.model.PaymentMethod
import com.densitech.scrollsmooth.ui.commerce.model.formatMoney
import com.densitech.scrollsmooth.ui.commerce.view.BuyButton
import com.densitech.scrollsmooth.ui.commerce.view.CommerceColors
import com.densitech.scrollsmooth.ui.commerce.view.CommerceDimens
import com.densitech.scrollsmooth.ui.commerce.view.CommerceTopBar
import com.densitech.scrollsmooth.ui.commerce.view.ProductImage
import com.densitech.scrollsmooth.ui.commerce.view.SectionHeader
import com.densitech.scrollsmooth.ui.commerce.viewmodel.CartViewModel
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple

/**
 * Checkout. No payment processor is wired up in this build: choosing a method records the choice
 * on the order and nothing is charged.
 */
@Composable
fun CheckoutScreen(
    cartViewModel: CartViewModel,
    onBack: () -> Unit,
    onOrderPlaced: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cart by cartViewModel.cart.collectAsState()
    val address by cartViewModel.address.collectAsState()
    val paymentMethod by cartViewModel.paymentMethod.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CommerceColors.Background),
    ) {
        CommerceTopBar(title = "Checkout", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CommerceDimens.ScreenPadding)
                .imePadding(),
        ) {
            SectionHeader(title = "Delivery address")
            Spacer(Modifier.height(12.dp))

            LabelledField(
                label = "Full name",
                value = address.fullName,
                onValueChange = { cartViewModel.updateAddress(address.copy(fullName = it)) },
            )
            Spacer(Modifier.height(10.dp))
            LabelledField(
                label = "Street address",
                value = address.line1,
                onValueChange = { cartViewModel.updateAddress(address.copy(line1 = it)) },
            )
            Spacer(Modifier.height(10.dp))
            Row {
                LabelledField(
                    label = "City",
                    value = address.city,
                    onValueChange = { cartViewModel.updateAddress(address.copy(city = it)) },
                    modifier = Modifier.weight(1.4f),
                )
                Spacer(Modifier.width(10.dp))
                LabelledField(
                    label = "Postcode",
                    value = address.postalCode,
                    onValueChange = { cartViewModel.updateAddress(address.copy(postalCode = it)) },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            LabelledField(
                label = "Phone (optional)",
                value = address.phone,
                onValueChange = { cartViewModel.updateAddress(address.copy(phone = it)) },
                keyboardType = KeyboardType.Phone,
            )

            Spacer(Modifier.height(24.dp))
            SectionHeader(title = "Payment")
            Spacer(Modifier.height(12.dp))
            PaymentMethod.entries.forEach { method ->
                PaymentOptionRow(
                    method = method,
                    selected = method == paymentMethod,
                    onClick = { cartViewModel.selectPaymentMethod(method) },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
            SectionHeader(title = "Order summary")
            Spacer(Modifier.height(12.dp))
            cart.lines.forEach { line ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProductImage(
                        seed = line.productId,
                        emoji = line.emoji,
                        imageUrl = line.imageUrl,
                        emojiSize = 20,
                        corner = 8.dp,
                        modifier = Modifier.size(44.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = line.title,
                            color = CommerceColors.OnSurface,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = buildString {
                                append("x${line.quantity}")
                                if (line.selectedOptions.isNotEmpty()) append(" · ${line.optionSummary}")
                            },
                            color = CommerceColors.OnSurfaceMuted,
                            fontSize = 11.sp,
                        )
                    }
                    Text(
                        text = line.lineTotalCents.formatMoney(),
                        color = CommerceColors.OnSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            OrderTotals(cart = cart)
            Spacer(Modifier.height(24.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CommerceColors.Surface)
                .navigationBarsPadding()
                .padding(CommerceDimens.ScreenPadding),
        ) {
            if (!address.isComplete) {
                Text(
                    text = "Add a name, street, city and postcode to place the order.",
                    color = CommerceColors.Warning,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            BuyButton(
                text = "Place order · ${cart.totalCents.formatMoney()}",
                enabled = cartViewModel.canPlaceOrder,
                onClick = {
                    cartViewModel.placeOrder()?.let { order -> onOrderPlaced(order.id) }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LabelledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(modifier = modifier) {
        Text(text = label, color = CommerceColors.OnSurfaceMuted, fontSize = 11.sp)
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CommerceColors.Surface)
                .border(1.dp, CommerceColors.Outline, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = CommerceColors.OnSurface, fontSize = 14.sp),
                cursorBrush = SolidColor(CommerceColors.Accent),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PaymentOptionRow(
    method: PaymentMethod,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CommerceDimens.CardCorner))
            .background(CommerceColors.Surface)
            .border(
                width = 1.dp,
                color = if (selected) CommerceColors.Accent else CommerceColors.Outline,
                shape = RoundedCornerShape(CommerceDimens.CardCorner),
            )
            .clickableNoRipple { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = method.emoji, fontSize = 18.sp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = method.label,
            color = CommerceColors.OnSurface,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = CommerceColors.Accent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Confirmation after checkout, with the shipment timeline the order will follow. */
@Composable
fun OrderPlacedScreen(
    orderId: String,
    cartViewModel: CartViewModel,
    onKeepShopping: () -> Unit,
    onViewOrders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val order = cartViewModel.order(orderId)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CommerceColors.Background),
    ) {
        CommerceTopBar(title = "Order confirmed")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CommerceDimens.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(38.dp))
                    .background(CommerceColors.Success.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "🎉", fontSize = 36.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "You're all set",
                color = CommerceColors.OnSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (order != null) {
                    "Order ${order.id} · ${order.totalCents.formatMoney()}"
                } else {
                    "Order $orderId"
                },
                color = CommerceColors.OnSurfaceMuted,
                fontSize = 13.sp,
            )

            Spacer(Modifier.height(28.dp))

            if (order != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CommerceDimens.CardCorner))
                        .background(CommerceColors.Surface)
                        .padding(16.dp),
                ) {
                    Text(
                        text = "Shipping to",
                        color = CommerceColors.OnSurfaceMuted,
                        fontSize = 11.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = order.address.fullName,
                        color = CommerceColors.OnSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = order.address.singleLine,
                        color = CommerceColors.OnSurfaceMuted,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "${order.itemCount} items · paid with ${order.paymentMethod.label}",
                        color = CommerceColors.OnSurfaceMuted,
                        fontSize = 12.sp,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CommerceColors.Surface)
                .navigationBarsPadding()
                .padding(CommerceDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BuyButton(
                text = "Keep shopping",
                onClick = onKeepShopping,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "View my orders",
                color = CommerceColors.Accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickableNoRipple { onViewOrders() }
                    .padding(vertical = 10.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
