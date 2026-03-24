package com.cardkeeper.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography(
    titleLarge = TextStyle(       // Display: app bar / screen titles
        fontSize = 28.sp,
        fontWeight = FontWeight.Normal,  // 400
        lineHeight = (28 * 1.2).sp
    ),
    titleMedium = TextStyle(      // Heading: section headings
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,  // 500
        lineHeight = (20 * 1.3).sp
    ),
    bodyLarge = TextStyle(        // Body: primary content
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,  // 400
        lineHeight = (16 * 1.5).sp
    ),
    labelMedium = TextStyle(      // Label: nav labels, chips, metadata
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,  // 500
        lineHeight = (12 * 1.4).sp
    )
)
