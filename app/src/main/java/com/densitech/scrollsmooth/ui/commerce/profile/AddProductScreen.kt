@file:OptIn(ExperimentalLayoutApi::class)

package com.densitech.scrollsmooth.ui.commerce.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.ui.commerce.model.ProductCategory
import com.densitech.scrollsmooth.ui.commerce.model.formatMoney
import com.densitech.scrollsmooth.ui.commerce.model.parsePriceToCents
import com.densitech.scrollsmooth.ui.commerce.model.percentOff
import com.densitech.scrollsmooth.ui.commerce.view.BuyButton
import com.densitech.scrollsmooth.ui.commerce.view.CommerceChip
import com.densitech.scrollsmooth.ui.commerce.view.CommerceColors
import com.densitech.scrollsmooth.ui.commerce.view.CommerceDimens
import com.densitech.scrollsmooth.ui.commerce.view.CommerceTopBar
import com.densitech.scrollsmooth.ui.commerce.view.ProductImage
import com.densitech.scrollsmooth.ui.commerce.view.SectionHeader
import com.densitech.scrollsmooth.ui.commerce.viewmodel.CreatorViewModel

/**
 * The listing form. Deliberately short: a creator filming on a phone will not fill in twenty
 * fields, so this asks for the four things a buyer actually needs before deciding.
 */
@Composable
fun AddProductScreen(
    creatorViewModel: CreatorViewModel,
    onBack: () -> Unit,
    onListed: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var compareAtText by remember { mutableStateOf("") }
    var stockText by remember { mutableStateOf("25") }
    var emoji by remember { mutableStateOf("🛍") }
    var category by remember { mutableStateOf(ProductCategory.FASHION) }
    var optionName by remember { mutableStateOf("") }
    var optionValuesText by remember { mutableStateOf("") }

    val priceCents = parsePriceToCents(priceText)
    val compareAtCents = parsePriceToCents(compareAtText)
    val stock = stockText.trim().toIntOrNull() ?: 0
    val canList = title.isNotBlank() && priceCents != null && priceCents > 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CommerceColors.Background),
    ) {
        CommerceTopBar(title = "List a product", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CommerceDimens.ScreenPadding)
                .imePadding(),
        ) {
            // Live preview so the creator sees the card buyers will see.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CommerceDimens.CardCorner))
                    .background(CommerceColors.Surface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProductImage(
                    seed = title.ifBlank { "preview" },
                    emoji = emoji.ifBlank { "🛍" },
                    emojiSize = 28,
                    corner = 10.dp,
                    modifier = Modifier.size(64.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title.ifBlank { "Your product title" },
                        color = if (title.isBlank()) {
                            CommerceColors.OnSurfaceMuted
                        } else {
                            CommerceColors.OnSurface
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = (priceCents ?: 0L).formatMoney(),
                            color = CommerceColors.Accent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        val off = percentOff(priceCents ?: 0L, compareAtCents)
                        if (off != null) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "-$off%",
                                color = CommerceColors.Discount,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader(title = "Details")
            Spacer(Modifier.height(12.dp))

            StudioField(label = "Title", value = title, onValueChange = { title = it })
            Spacer(Modifier.height(10.dp))
            StudioField(
                label = "Description",
                value = description,
                onValueChange = { description = it },
                singleLine = false,
                minHeight = 88,
            )
            Spacer(Modifier.height(10.dp))
            Row {
                StudioField(
                    label = "Price",
                    value = priceText,
                    onValueChange = { priceText = it },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                StudioField(
                    label = "Compare at (optional)",
                    value = compareAtText,
                    onValueChange = { compareAtText = it },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row {
                StudioField(
                    label = "Stock",
                    value = stockText,
                    onValueChange = { stockText = it },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                StudioField(
                    label = "Emoji",
                    value = emoji,
                    onValueChange = { emoji = it },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader(title = "Category")
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ProductCategory.entries
                    .filter { it != ProductCategory.ALL }
                    .forEach { item ->
                        CommerceChip(
                            text = "${item.emoji}  ${item.label}",
                            selected = item == category,
                            onClick = { category = item },
                        )
                    }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader(title = "Variants (optional)")
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Give buyers a choice, e.g. Size with values S, M, L.",
                color = CommerceColors.OnSurfaceMuted,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(12.dp))
            StudioField(
                label = "Option name",
                value = optionName,
                onValueChange = { optionName = it },
            )
            Spacer(Modifier.height(10.dp))
            StudioField(
                label = "Values, comma separated",
                value = optionValuesText,
                onValueChange = { optionValuesText = it },
            )

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
                text = "Publish listing",
                enabled = canList,
                onClick = {
                    val cents = priceCents
                    if (cents != null && cents > 0) {
                        val product = creatorViewModel.listProduct(
                            title = title,
                            description = description.ifBlank { "Listed by ${title.trim()}" },
                            priceCents = cents,
                            compareAtPriceCents = compareAtCents,
                            category = category,
                            emoji = emoji,
                            stock = stock,
                            optionName = optionName,
                            optionValues = optionValuesText.split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() },
                        )
                        onListed(product.id)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
