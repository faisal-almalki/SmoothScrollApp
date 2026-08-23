package com.densitech.scrollsmooth.ui.commerce.creator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.ui.commerce.model.Product
import com.densitech.scrollsmooth.ui.commerce.model.formatMoney
import com.densitech.scrollsmooth.ui.commerce.view.BuyButton
import com.densitech.scrollsmooth.ui.commerce.view.CommerceColors
import com.densitech.scrollsmooth.ui.commerce.view.CommerceDimens
import com.densitech.scrollsmooth.ui.commerce.view.CommerceTopBar
import com.densitech.scrollsmooth.ui.commerce.view.ProductImage
import com.densitech.scrollsmooth.ui.commerce.view.SecondaryButton
import com.densitech.scrollsmooth.ui.commerce.viewmodel.CreatorViewModel
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple

/**
 * The publishing step that turns a video into a storefront: pick which of your listings appear
 * as the shop tag while the video plays.
 */
@Composable
fun TagProductsScreen(
    videoId: String,
    creatorViewModel: CreatorViewModel,
    onBack: () -> Unit,
    onPublished: () -> Unit,
    onListProduct: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val me by creatorViewModel.me.collectAsState()
    val allProducts by creatorViewModel.allProducts.collectAsState()
    val selectedIds by creatorViewModel.pendingVideoTags.collectAsState()

    val myProducts = allProducts.filter { it.sellerId == me.id }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CommerceColors.Background),
    ) {
        CommerceTopBar(
            title = "Tag products",
            subtitle = "Buyers tap these while your video plays",
            onBack = onBack,
        )

        if (myProducts.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(CommerceDimens.ScreenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = "🏷", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "You have nothing to tag yet",
                    color = CommerceColors.OnSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "List a product first, then come back and attach it to this video.",
                    color = CommerceColors.OnSurfaceMuted,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(20.dp))
                BuyButton(
                    text = "List a product",
                    onClick = onListProduct,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = CommerceDimens.ScreenPadding,
                    end = CommerceDimens.ScreenPadding,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(myProducts, key = { it.id }) { product ->
                    TaggableProductRow(
                        product = product,
                        selected = selectedIds.contains(product.id),
                        onToggle = { creatorViewModel.togglePendingTag(product.id) },
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CommerceColors.Surface)
                .navigationBarsPadding()
                .padding(CommerceDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (selectedIds.isEmpty()) {
                    "No products tagged. The video will post without a shop tag."
                } else {
                    "${selectedIds.size} tagged. The first one shows as the pill on the video."
                },
                color = CommerceColors.OnSurfaceMuted,
                fontSize = 12.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(
                    text = "Skip",
                    onClick = {
                        creatorViewModel.clearPendingTags()
                        onPublished()
                    },
                    modifier = Modifier.weight(1f),
                )
                BuyButton(
                    text = "Post video",
                    onClick = {
                        creatorViewModel.publishTags(videoId)
                        onPublished()
                    },
                    modifier = Modifier.weight(1.4f),
                )
            }
        }
    }
}

@Composable
private fun TaggableProductRow(
    product: Product,
    selected: Boolean,
    onToggle: () -> Unit,
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
            .clickableNoRipple { onToggle() }
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${product.priceCents.formatMoney()} · ${product.stock} in stock",
                color = CommerceColors.OnSurfaceMuted,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (selected) CommerceColors.Accent else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (selected) CommerceColors.Accent else CommerceColors.Outline,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
