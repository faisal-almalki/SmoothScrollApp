package com.densitech.scrollsmooth.ui.commerce.profile

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.ui.commerce.model.Order
import com.densitech.scrollsmooth.ui.commerce.model.Product
import com.densitech.scrollsmooth.ui.commerce.model.formatCompact
import com.densitech.scrollsmooth.ui.commerce.model.formatMoney
import com.densitech.scrollsmooth.ui.commerce.view.BuyButton
import com.densitech.scrollsmooth.ui.commerce.view.CommerceColors
import com.densitech.scrollsmooth.ui.commerce.view.CommerceDimens
import com.densitech.scrollsmooth.ui.commerce.view.CommerceTopBar
import com.densitech.scrollsmooth.ui.commerce.view.ProductImage
import com.densitech.scrollsmooth.ui.commerce.view.SectionHeader
import com.densitech.scrollsmooth.ui.commerce.viewmodel.CreatorViewModel
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple

/**
 * Creator Studio: what your shop earned, what is listed, and what buyers ordered. Revenue here
 * only counts orders actually placed in this app, so a fresh install starts at zero.
 */
@Composable
fun SellerDashboardScreen(
    creatorViewModel: CreatorViewModel,
    onBack: () -> Unit,
    onListProduct: () -> Unit,
    onOpenMyShop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val me by creatorViewModel.me.collectAsState()
    val allProducts by creatorViewModel.allProducts.collectAsState()
    val orders by creatorViewModel.orders.collectAsState()

    val myProducts = allProducts.filter { it.sellerId == me.id }
    val myOrders = orders.filter { order -> order.lines.any { it.sellerId == me.id } }
    val grossCents = creatorViewModel.myGrossRevenueCents()
    val netCents = creatorViewModel.myNetPayoutCents()
    val unitsSold = creatorViewModel.myUnitsSold()

    var editingShop by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CommerceColors.Background),
    ) {
        CommerceTopBar(
            title = "Creator Studio",
            subtitle = me.atHandle,
            onBack = onBack,
            trailing = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit shop",
                    tint = CommerceColors.OnSurfaceMuted,
                    modifier = Modifier
                        .size(20.dp)
                        .clickableNoRipple { editingShop = !editingShop },
                )
            },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CommerceDimens.ScreenPadding),
        ) {
            if (editingShop) {
                ShopEditor(
                    initialName = me.displayName,
                    initialHandle = me.handle,
                    initialBio = me.bio,
                    initialEmoji = me.emoji,
                    onSave = { name, handle, bio, emoji ->
                        creatorViewModel.updateShop(name, handle, bio, emoji)
                        editingShop = false
                    },
                    onCancel = { editingShop = false },
                    modifier = Modifier.padding(bottom = 20.dp),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile(
                    label = "Gross sales",
                    value = grossCents.formatMoney(),
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    label = "Your payout",
                    value = netCents.formatMoney(),
                    caption = "after 5% platform fee",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile(
                    label = "Units sold",
                    value = unitsSold.formatCompact(),
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    label = "Orders",
                    value = myOrders.size.toString(),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(24.dp))
            SectionHeader(
                title = "Your listings",
                action = "View shop",
                onActionClick = onOpenMyShop,
            )
            Spacer(Modifier.height(12.dp))

            if (myProducts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CommerceDimens.CardCorner))
                        .background(CommerceColors.Surface)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = "🏷", fontSize = 32.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Nothing listed yet",
                        color = CommerceColors.OnSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "List a product, then attach it to a video so people can buy while they watch.",
                        color = CommerceColors.OnSurfaceMuted,
                        fontSize = 12.sp,
                    )
                }
            } else {
                myProducts.forEach { product ->
                    ListingRow(
                        product = product,
                        unitsSold = unitsSoldOf(myOrders, product.id),
                        onUnlist = { creatorViewModel.unlistProduct(product.id) },
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionHeader(title = "Recent orders")
            Spacer(Modifier.height(12.dp))

            if (myOrders.isEmpty()) {
                Text(
                    text = "No orders yet. Sales from your videos and lives land here.",
                    color = CommerceColors.OnSurfaceMuted,
                    fontSize = 13.sp,
                )
            } else {
                myOrders.take(10).forEach { order ->
                    SellerOrderRow(
                        order = order,
                        sellerId = me.id,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CommerceColors.Surface)
                .navigationBarsPadding()
                .padding(CommerceDimens.ScreenPadding),
        ) {
            BuyButton(
                text = "List a product",
                onClick = onListProduct,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun unitsSoldOf(orders: List<Order>, productId: String): Int =
    orders.sumOf { order -> order.lines.filter { it.productId == productId }.sumOf { it.quantity } }

@Composable
private fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(CommerceDimens.CardCorner))
            .background(CommerceColors.Surface)
            .padding(14.dp),
    ) {
        Text(text = label, color = CommerceColors.OnSurfaceMuted, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            color = CommerceColors.OnSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (caption != null) {
            Spacer(Modifier.height(2.dp))
            Text(text = caption, color = CommerceColors.OnSurfaceMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ListingRow(
    product: Product,
    unitsSold: Int,
    onUnlist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CommerceDimens.CardCorner))
            .background(CommerceColors.Surface)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProductImage(
            seed = product.id,
            emoji = product.emoji,
            imageUrl = product.imageUrl,
            emojiSize = 22,
            corner = 10.dp,
            modifier = Modifier.size(52.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.title,
                color = CommerceColors.OnSurface,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${product.priceCents.formatMoney()} · ${product.stock} in stock · $unitsSold sold",
                color = CommerceColors.OnSurfaceMuted,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Unlist product",
            tint = CommerceColors.OnSurfaceMuted,
            modifier = Modifier
                .size(18.dp)
                .clickableNoRipple { onUnlist() },
        )
    }
}

@Composable
private fun SellerOrderRow(
    order: Order,
    sellerId: String,
    modifier: Modifier = Modifier,
) {
    val myLines = order.lines.filter { it.sellerId == sellerId }
    val myTotal = myLines.sumOf { it.lineTotalCents }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CommerceDimens.CardCorner))
            .background(CommerceColors.Surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(CommerceColors.SurfaceElevated),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = myLines.firstOrNull()?.emoji ?: "📦", fontSize = 16.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = order.id,
                color = CommerceColors.OnSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${myLines.sumOf { it.quantity }} items · ships to ${order.address.city.ifBlank { "buyer" }}",
                color = CommerceColors.OnSurfaceMuted,
                fontSize = 11.sp,
            )
        }
        Text(
            text = myTotal.formatMoney(),
            color = CommerceColors.Accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Inline form for naming the shop. Kept in the studio because it is a one field at a time job. */
@Composable
private fun ShopEditor(
    initialName: String,
    initialHandle: String,
    initialBio: String,
    initialEmoji: String,
    onSave: (String, String, String, String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf(initialName) }
    var handle by remember { mutableStateOf(initialHandle) }
    var bio by remember { mutableStateOf(initialBio) }
    var emoji by remember { mutableStateOf(initialEmoji) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CommerceDimens.CardCorner))
            .background(CommerceColors.Surface)
            .padding(14.dp),
    ) {
        Text(
            text = "Shop details",
            color = CommerceColors.OnSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        StudioField(label = "Shop name", value = name, onValueChange = { name = it })
        Spacer(Modifier.height(10.dp))
        StudioField(label = "Handle", value = handle, onValueChange = { handle = it })
        Spacer(Modifier.height(10.dp))
        StudioField(label = "Bio", value = bio, onValueChange = { bio = it })
        Spacer(Modifier.height(10.dp))
        StudioField(label = "Shop emoji", value = emoji, onValueChange = { emoji = it })
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Cancel",
                color = CommerceColors.OnSurfaceMuted,
                fontSize = 14.sp,
                modifier = Modifier
                    .clickableNoRipple { onCancel() }
                    .padding(vertical = 10.dp, horizontal = 12.dp),
            )
            BuyButton(
                text = "Save",
                onClick = { onSave(name, handle, bio, emoji) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun StudioField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minHeight: Int = 44,
) {
    Column(modifier = modifier) {
        Text(text = label, color = CommerceColors.OnSurfaceMuted, fontSize = 11.sp)
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(minHeight.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CommerceColors.SurfaceElevated)
                .border(1.dp, CommerceColors.Outline, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = TextStyle(color = CommerceColors.OnSurface, fontSize = 14.sp),
                cursorBrush = SolidColor(CommerceColors.Accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
