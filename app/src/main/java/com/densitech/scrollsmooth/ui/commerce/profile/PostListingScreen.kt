@file:OptIn(ExperimentalLayoutApi::class)

package com.densitech.scrollsmooth.ui.commerce.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.ui.commerce.model.ListingCategory
import com.densitech.scrollsmooth.ui.commerce.model.ListingCondition
import com.densitech.scrollsmooth.ui.commerce.model.formatMoney
import com.densitech.scrollsmooth.ui.commerce.model.parsePriceToCents
import com.densitech.scrollsmooth.ui.commerce.view.CommerceChip
import com.densitech.scrollsmooth.ui.commerce.view.CommerceColors
import com.densitech.scrollsmooth.ui.commerce.view.CommerceDimens
import com.densitech.scrollsmooth.ui.commerce.view.CommerceTopBar
import com.densitech.scrollsmooth.ui.commerce.view.ListingImage
import com.densitech.scrollsmooth.ui.commerce.view.PrimaryButton
import com.densitech.scrollsmooth.ui.commerce.view.SectionHeader
import com.densitech.scrollsmooth.ui.commerce.viewmodel.MyListingsViewModel
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple

/**
 * Posting an ad. Short on purpose: someone filming on a phone will not fill in twenty fields, and
 * a buyer only needs the price, the city and enough description to decide whether to call.
 */
@Composable
fun PostListingScreen(
    myListingsViewModel: MyListingsViewModel,
    onBack: () -> Unit,
    onPosted: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val me by myListingsViewModel.me.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var isNegotiable by remember { mutableStateOf(true) }
    var emoji by remember { mutableStateOf("📦") }
    var category by remember { mutableStateOf(ListingCategory.OTHER) }
    var condition by remember { mutableStateOf(ListingCondition.USED) }
    var city by remember { mutableStateOf(me.city) }

    val priceCents = parsePriceToCents(priceText)
    val canPost = title.isNotBlank() && priceCents != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CommerceColors.Background),
    ) {
        CommerceTopBar(title = "Post an ad", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CommerceDimens.ScreenPadding)
                .imePadding(),
        ) {
            // Live preview of the card buyers will see in the grid.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CommerceDimens.CardCorner))
                    .background(CommerceColors.Surface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ListingImage(
                    seed = title.ifBlank { "preview" },
                    emoji = emoji.ifBlank { "📦" },
                    emojiSize = 28,
                    corner = 10.dp,
                    modifier = Modifier.size(64.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = when {
                                priceCents == null -> "Price"
                                priceCents <= 0L -> "Free"
                                else -> priceCents.formatMoney()
                            },
                            color = if (priceCents == null) {
                                CommerceColors.OnSurfaceMuted
                            } else {
                                CommerceColors.OnSurface
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (isNegotiable && (priceCents ?: 0L) > 0L) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "negotiable",
                                color = CommerceColors.OnSurfaceMuted,
                                fontSize = 11.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = title.ifBlank { "Your ad title" },
                        color = if (title.isBlank()) {
                            CommerceColors.OnSurfaceMuted
                        } else {
                            CommerceColors.OnSurface
                        },
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "$city · ${condition.label}",
                        color = CommerceColors.OnSurfaceMuted,
                        fontSize = 11.sp,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader(title = "The ad")
            Spacer(Modifier.height(12.dp))

            StudioField(label = "Title", value = title, onValueChange = { title = it })
            Spacer(Modifier.height(10.dp))
            StudioField(
                label = "Description",
                value = description,
                onValueChange = { description = it },
                singleLine = false,
                minHeight = 96,
            )
            Spacer(Modifier.height(10.dp))
            Row {
                StudioField(
                    label = "Price ($CURRENCY_HINT)",
                    value = priceText,
                    onValueChange = { priceText = it },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                StudioField(
                    label = "Emoji",
                    value = emoji,
                    onValueChange = { emoji = it },
                    modifier = Modifier.weight(0.5f),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CommerceChip(
                    text = if (isNegotiable) "Price negotiable ✓" else "Price negotiable",
                    selected = isNegotiable,
                    onClick = { isNegotiable = !isNegotiable },
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
                ListingCategory.entries
                    .filter { it != ListingCategory.ALL }
                    .forEach { item ->
                        CommerceChip(
                            text = "${item.emoji}  ${item.label}",
                            selected = item == category,
                            onClick = { category = item },
                        )
                    }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader(title = "Condition")
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ListingCondition.entries.forEach { item ->
                    CommerceChip(
                        text = item.label,
                        selected = item == condition,
                        onClick = { condition = item },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionHeader(title = "City")
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                myListingsViewModel.cities().forEach { item ->
                    CommerceChip(
                        text = item,
                        selected = item == city,
                        onClick = { city = item },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = "Buyers will contact you with the options set in your profile. " +
                    "Your phone number is only shown if you have published it.",
                color = CommerceColors.OnSurfaceMuted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
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
            PrimaryButton(
                text = "Post ad",
                enabled = canPost,
                onClick = {
                    val cents = priceCents
                    if (cents != null) {
                        val listing = myListingsViewModel.postListing(
                            title = title,
                            description = description.ifBlank { "No description provided." },
                            priceCents = cents,
                            isNegotiable = isNegotiable,
                            category = category,
                            condition = condition,
                            emoji = emoji,
                            city = city,
                        )
                        onPosted(listing.id)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private const val CURRENCY_HINT = "SAR"

/** Shared single line field used by the posting form and the profile editor. */
@Composable
internal fun StudioField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minHeight: Int = 46,
    keyboardType: KeyboardType = KeyboardType.Text,
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
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
