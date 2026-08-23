package com.densitech.scrollsmooth.ui.commerce.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.ui.commerce.view.CommerceChip
import com.densitech.scrollsmooth.ui.commerce.view.CommerceColors
import com.densitech.scrollsmooth.ui.commerce.view.CommerceDimens
import com.densitech.scrollsmooth.ui.commerce.view.CommerceTopBar
import com.densitech.scrollsmooth.ui.commerce.view.PrimaryButton
import com.densitech.scrollsmooth.ui.commerce.view.SectionHeader
import com.densitech.scrollsmooth.ui.commerce.view.placeholderBrush
import com.densitech.scrollsmooth.ui.commerce.viewmodel.MyListingsViewModel

/**
 * Who you are and how buyers may reach you.
 *
 * Publishing a phone number is the one setting that changes what strangers can see, so it is an
 * explicit switch with the consequence spelled out next to it, and clearing the number turns
 * calls off rather than leaving a dead button on your ads.
 */
@Composable
fun AccountSettingsScreen(
    myListingsViewModel: MyListingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val me by myListingsViewModel.me.collectAsState()

    var displayName by remember(me.id) { mutableStateOf(me.displayName) }
    var handle by remember(me.id) { mutableStateOf(me.handle) }
    var bio by remember(me.id) { mutableStateOf(me.bio) }
    var emoji by remember(me.id) { mutableStateOf(me.emoji) }
    var city by remember(me.id) { mutableStateOf(me.city) }
    var phoneNumber by remember(me.id) { mutableStateOf(me.phoneNumber) }
    var allowCalls by remember(me.id) { mutableStateOf(me.allowCalls) }
    var allowMessages by remember(me.id) { mutableStateOf(me.allowMessages) }

    var savedNotice by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CommerceColors.Background),
    ) {
        CommerceTopBar(title = "Contact settings", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CommerceDimens.ScreenPadding)
                .imePadding(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(placeholderBrush(me.id)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = emoji.ifBlank { "🙂" }, fontSize = 26.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName.ifBlank { "Your account" },
                        color = CommerceColors.OnSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "@${handle.ifBlank { "your.account" }} · $city",
                        color = CommerceColors.OnSurfaceMuted,
                        fontSize = 12.sp,
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            SectionHeader(title = "Your details")
            Spacer(Modifier.height(12.dp))

            StudioField(label = "Name", value = displayName, onValueChange = { displayName = it })
            Spacer(Modifier.height(10.dp))
            Row {
                StudioField(
                    label = "Handle",
                    value = handle,
                    onValueChange = { handle = it },
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
            Spacer(Modifier.height(10.dp))
            StudioField(
                label = "About you",
                value = bio,
                onValueChange = { bio = it },
                singleLine = false,
                minHeight = 80,
            )

            Spacer(Modifier.height(14.dp))
            Text(text = "City", color = CommerceColors.OnSurfaceMuted, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                myListingsViewModel.cities().forEach { item ->
                    CommerceChip(
                        text = item,
                        selected = item == city,
                        onClick = { city = item },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionHeader(title = "How buyers reach you")
            Spacer(Modifier.height(12.dp))

            SettingSwitchRow(
                title = "Allow messages",
                subtitle = "Buyers can open a chat with you from any of your ads.",
                checked = allowMessages,
                onCheckedChange = { allowMessages = it },
            )
            Spacer(Modifier.height(10.dp))
            SettingSwitchRow(
                title = "Show my phone number",
                subtitle = if (phoneNumber.isBlank()) {
                    "Add a number below to turn this on."
                } else {
                    "Anyone viewing your ads will see $phoneNumber and can call you."
                },
                checked = allowCalls && phoneNumber.isNotBlank(),
                enabled = phoneNumber.isNotBlank(),
                onCheckedChange = { allowCalls = it },
            )
            Spacer(Modifier.height(12.dp))
            StudioField(
                label = "Phone number",
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                keyboardType = KeyboardType.Phone,
            )

            Spacer(Modifier.height(12.dp))
            Text(
                text = "Your number is stored on this device only. Clearing it turns calls off.",
                color = CommerceColors.OnSurfaceMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )

            if (savedNotice) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Saved",
                    color = CommerceColors.Accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
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
            PrimaryButton(
                text = "Save",
                onClick = {
                    myListingsViewModel.updateProfile(displayName, handle, bio, emoji, city)
                    myListingsViewModel.updateContactPreferences(
                        phoneNumber = phoneNumber,
                        allowCalls = allowCalls,
                        allowMessages = allowMessages,
                    )
                    savedNotice = true
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CommerceDimens.CardCorner))
            .background(CommerceColors.Surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = CommerceColors.OnSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = CommerceColors.OnSurfaceMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CommerceColors.Accent,
                uncheckedThumbColor = CommerceColors.OnSurfaceMuted,
                uncheckedTrackColor = CommerceColors.SurfaceElevated,
            ),
        )
    }
}
