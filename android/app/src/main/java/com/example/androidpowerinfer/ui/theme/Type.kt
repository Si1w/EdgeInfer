package com.example.androidpowerinfer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

val Typography.senderTextStyle: TextStyle
    get() = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp,
        color = Color.Gray
    )

val Typography.messageTextStyle: TextStyle
    get() = bodyLarge.copy(
        color = Color.Black
    )

val UserBubbleColor = Color(0xFFE1FFC7)
val AssistantBubbleColor = Color(0xFFEEF1F5)
val OtherBubbleColor = Color.LightGray
val BubbleCornerShape = 16.dp
val SmallBubbleCornerShape = 8.dp
