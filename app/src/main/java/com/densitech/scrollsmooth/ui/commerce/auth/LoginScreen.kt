package com.densitech.scrollsmooth.ui.commerce.auth

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.ui.commerce.view.CommerceColors
import com.densitech.scrollsmooth.ui.commerce.view.CommerceDimens
import com.densitech.scrollsmooth.ui.commerce.view.PrimaryButton
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple

/**
 * Sign in with a phone number and a six-digit code.
 *
 * Browsing does not require an account, so this is only reached from an action
 * that needs an identity — messaging a seller, or posting an ad.
 */
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val step by authViewModel.step.collectAsState()
    val phone by authViewModel.phone.collectAsState()
    val code by authViewModel.code.collectAsState()
    val busy by authViewModel.busy.collectAsState()
    val error by authViewModel.error.collectAsState()
    val devCode by authViewModel.devCode.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CommerceColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(CommerceDimens.ScreenPadding),
    ) {
        if (onDismiss != null) {
            Text(
                text = "✕",
                color = CommerceColors.OnSurfaceMuted,
                fontSize = 18.sp,
                modifier = Modifier.clickableNoRipple { onDismiss() },
            )
        }

        Spacer(Modifier.height(48.dp))

        Text(text = "🏷", fontSize = 44.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (step == AuthStep.PHONE) "Sign in" else "Enter your code",
            color = CommerceColors.OnSurface,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (step == AuthStep.PHONE) {
                "We will text you a six-digit code. No password to remember."
            } else {
                "Sent to $phone."
            },
            color = CommerceColors.OnSurfaceMuted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )

        Spacer(Modifier.height(28.dp))

        if (step == AuthStep.PHONE) {
            LoginField(
                value = phone,
                onValueChange = authViewModel::onPhoneChange,
                placeholder = "05xxxxxxxx",
                keyboardType = KeyboardType.Phone,
            )
        } else {
            LoginField(
                value = code,
                onValueChange = authViewModel::onCodeChange,
                placeholder = "······",
                keyboardType = KeyboardType.NumberPassword,
                centered = true,
                letterSpaced = true,
            )
            if (devCode != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Development build: your code is $devCode",
                    color = CommerceColors.Warning,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CommerceColors.Warning.copy(alpha = 0.12f))
                        .padding(10.dp),
                )
            }
        }

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(text = error.orEmpty(), color = CommerceColors.Alert, fontSize = 13.sp)
        }

        Spacer(Modifier.height(20.dp))

        PrimaryButton(
            text = when {
                busy -> "Please wait…"
                step == AuthStep.PHONE -> "Send code"
                else -> "Sign in"
            },
            enabled = !busy,
            onClick = {
                if (step == AuthStep.PHONE) authViewModel.requestCode() else authViewModel.verifyCode()
            },
            modifier = Modifier.fillMaxWidth(),
        )

        if (step == AuthStep.CODE) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Use a different number",
                    color = CommerceColors.Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickableNoRipple { authViewModel.back() },
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = "Your number is used to sign you in. It is never shown on your ads " +
                "unless you publish it yourself in Contact settings.",
            color = CommerceColors.OnSurfaceMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    centered: Boolean = false,
    letterSpaced: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CommerceColors.Surface)
            .border(1.dp, CommerceColors.Outline, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        contentAlignment = if (centered) Alignment.Center else Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = CommerceColors.OnSurfaceMuted,
                fontSize = if (letterSpaced) 24.sp else 17.sp,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = CommerceColors.OnSurface,
                fontSize = if (letterSpaced) 24.sp else 17.sp,
                fontWeight = if (letterSpaced) FontWeight.Bold else FontWeight.Normal,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            ),
            cursorBrush = SolidColor(CommerceColors.Accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
