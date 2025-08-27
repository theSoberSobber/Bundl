package com.bundl.app.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.bundl.app.presentation.theme.BundlColors

@Composable
fun UserCircles(
    count: Int,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        BundlColors.GoogleBlue, // Google Blue
        BundlColors.GoogleRed, // Google Red
        BundlColors.GoogleYellow, // Google Yellow
        BundlColors.GoogleGreen, // Google Green
        BundlColors.Purple  // Purple
    )
    
    Box(modifier = modifier) {
        // Show up to 4 circles + a count if there are more
        val circleSize = 20.dp
        val overlap = 12.dp // Amount of overlap between circles
        val maxVisibleCircles = if (count < 4) count else 4
        
        // Draw circles from right to left
        for (i in 0 until maxVisibleCircles) {
            Box(
                modifier = Modifier
                    .size(circleSize)
                    .offset(x = -(overlap * i))
                    .zIndex((maxVisibleCircles - i).toFloat()) // Higher z-index for left circles
                    .background(colors[i], CircleShape)
                    .border(1.dp, BundlColors.SurfaceLight, CircleShape)
            ) {
                // If this is the last circle and we have more users, show the count
                if (i == maxVisibleCircles - 1 && count > 4) {
                    Text(
                        text = "+${count - 3}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}
