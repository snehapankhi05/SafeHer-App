package com.sneha.safeherapp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sneha.safeherapp.ui.theme.LightPurple
import com.sneha.safeherapp.ui.theme.SoftPurple

@Composable
fun SafeHerLogo(
    size: Dp = 100.dp,
    iconColor: Color = Color.White
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(MaterialTheme.colorScheme.primary, SoftPurple)
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        // Shield Layer
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = iconColor.copy(alpha = 0.3f),
            modifier = Modifier.size(size * 0.7f)
        )
        
        // Person/Silhouette Layer
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "SafeHer Logo",
            tint = iconColor,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}
