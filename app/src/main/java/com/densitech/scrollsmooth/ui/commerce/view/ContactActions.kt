package com.densitech.scrollsmooth.ui.commerce.view

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.densitech.scrollsmooth.ui.commerce.model.Seller
import com.densitech.scrollsmooth.ui.commerce.model.toDialableNumber

/**
 * Hands a number to the system dialler rather than placing the call.
 *
 * ACTION_DIAL only pre-fills the keypad, so it needs no CALL_PHONE permission and the person
 * still chooses whether to press the button. That is the right default for a stranger's number.
 */
fun dialSeller(context: Context, phoneNumber: String): Boolean {
    val digits = phoneNumber.toDialableNumber()
    if (digits.isBlank()) return false
    val intent = Intent(Intent.ACTION_DIAL, "tel:$digits".toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        context.startActivity(intent)
        true
    } catch (notFound: ActivityNotFoundException) {
        // A tablet or emulator with no dialler. The number is still shown on screen to copy.
        false
    }
}

/**
 * The only conversion in the app: message the seller, or call them when they have chosen to
 * publish a number. Everything else on a listing exists to get someone as far as this row.
 */
@Composable
fun ContactActions(
    seller: Seller?,
    onMessage: () -> Unit,
    modifier: Modifier = Modifier,
    onCalled: (() -> Unit)? = null,
    compact: Boolean = false,
) {
    val context = LocalContext.current
    val canMessage = seller?.allowMessages == true
    // Held as a non-null local so the number is only reachable where it is known to exist.
    val callableSeller = seller?.takeIf { it.hasPublicPhone }
    val canCall = callableSeller != null

    Column(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(
                text = if (compact) "Message" else "Message seller",
                leadingEmoji = "💬",
                enabled = canMessage,
                onClick = onMessage,
                modifier = Modifier.weight(1f),
            )
            if (callableSeller != null) {
                PrimaryButton(
                    text = "Call",
                    leadingEmoji = "📞",
                    container = CommerceColors.Call,
                    onClick = {
                        if (dialSeller(context, callableSeller.phoneNumber)) onCalled?.invoke()
                    },
                    modifier = Modifier.weight(0.75f),
                )
            }
        }

        if (callableSeller != null && !compact) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = callableSeller.phoneNumber,
                color = CommerceColors.OnSurfaceMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (!canCall && !compact) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "This seller has not published a phone number. Message them instead.",
                color = CommerceColors.OnSurfaceMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (!canMessage && !canCall) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "This seller has turned off contact for now.",
                color = CommerceColors.Warning,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CommerceColors.Warning.copy(alpha = 0.12f))
                    .padding(8.dp),
            )
        }
    }
}

/** Safety note shown wherever a deal is about to move off the platform. */
@Composable
fun MeetSafelyNote(modifier: Modifier = Modifier) {
    Text(
        text = "Deals happen between you and the seller. Meet in a public place, check the item " +
            "before you pay, and never send money in advance.",
        color = CommerceColors.OnSurfaceMuted,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CommerceColors.SurfaceElevated)
            .padding(12.dp),
    )
}
