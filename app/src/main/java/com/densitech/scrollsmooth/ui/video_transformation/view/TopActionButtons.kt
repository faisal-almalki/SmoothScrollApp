package com.densitech.scrollsmooth.ui.video_transformation.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.densitech.scrollsmooth.ui.commerce.view.CommerceColors
import com.densitech.scrollsmooth.ui.utils.clickableNoRipple

@Composable
fun TopActionButtons(
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    onHeightChange: (Dp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.DarkGray)
                .onGloballyPositioned {
                    onHeightChange(with(density) {
                        (it.size.height + it.size.height.toFloat() / 2).toDp()
                    })
                }
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close"
            )
        }

        // Moves the creator on to tagging products before the video is posted.
        Text(
            text = "Next",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(CommerceColors.Accent)
                .clickableNoRipple { onNextClick() }
                .padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }
}
