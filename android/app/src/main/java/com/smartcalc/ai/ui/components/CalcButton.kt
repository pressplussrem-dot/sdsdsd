package com.smartcalc.ai.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape

enum class CalcButtonStyle { NUMBER, OPERATOR, FUNCTION, EQUALS }

@Composable
fun RowScope.CalcButton(
    label: String? = null,
    icon: ImageVector? = null,
    style: CalcButtonStyle = CalcButtonStyle.NUMBER,
    contentDescription: String? = null,
    weight: Float = 1f,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val background: Color = when (style) {
        CalcButtonStyle.NUMBER -> colors.surface
        CalcButtonStyle.OPERATOR -> colors.primaryContainer
        CalcButtonStyle.FUNCTION -> colors.surfaceVariant
        CalcButtonStyle.EQUALS -> colors.primary
    }
    val foreground: Color = when (style) {
        CalcButtonStyle.NUMBER -> colors.onSurface
        CalcButtonStyle.OPERATOR -> colors.onPrimaryContainer
        CalcButtonStyle.FUNCTION -> colors.onSurfaceVariant
        CalcButtonStyle.EQUALS -> colors.onPrimary
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = background,
        contentColor = foreground,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
        modifier = Modifier
            .weight(weight)
            .aspectRatio(if (weight > 1f) 2.1f else 1.15f)
            .padding(4.dp)
            .then(
                contentDescription?.let { description ->
                    Modifier.semantics { this.contentDescription = description }
                } ?: Modifier
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            when {
                icon != null -> Icon(imageVector = icon, contentDescription = null)
                label != null -> Text(
                    text = label,
                    fontSize = if (style == CalcButtonStyle.NUMBER) 26.sp else 23.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}
