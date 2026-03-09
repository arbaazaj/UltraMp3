package com.ultramp3.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ultramp3.ui.theme.MetallicGray
import com.ultramp3.ui.theme.MetallicLightGray

@Composable
fun RetroFrame(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .border(2.dp, MetallicLightGray, RoundedCornerShape(2.dp))
            .padding(2.dp)
            .background(Color.Black)
    ) {
        // Bolts in corners
        Canvas(modifier = Modifier.fillMaxSize()) {
            val boltSize = 4.dp.toPx()
            val inset = 4.dp.toPx()

            val boltBrush = Brush.radialGradient(
                colors = listOf(MetallicLightGray, MetallicGray),
                center = Offset(boltSize / 2, boltSize / 2),
                radius = boltSize
            )

            // Top Left
            drawCircle(brush = boltBrush, radius = boltSize / 2, center = Offset(inset, inset))
            // Top Right
            drawCircle(
                brush = boltBrush,
                radius = boltSize / 2,
                center = Offset(size.width - inset, inset)
            )
            // Bottom Left
            drawCircle(
                brush = boltBrush,
                radius = boltSize / 2,
                center = Offset(inset, size.height - inset)
            )
            // Bottom Right
            drawCircle(
                brush = boltBrush,
                radius = boltSize / 2,
                center = Offset(size.width - inset, size.height - inset)
            )
        }

        content()
    }
}
