package com.artboard.ui.transform.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.artboard.data.model.TransformType

/**
 * Toolbar for transform mode with mode selector and quick actions
 */
@Composable
fun TransformToolbar(
    transformType: TransformType,
    onTransformTypeChange: (TransformType) -> Unit,
    onFlipHorizontal: () -> Unit,
    onFlipVertical: () -> Unit,
    onRotate90: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .padding(16.dp)
    ) {
        // Mode selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TransformTypeButton(
                type = TransformType.FREE,
                label = "Free",
                icon = "🔄",
                isSelected = transformType == TransformType.FREE,
                onClick = { onTransformTypeChange(TransformType.FREE) }
            )
            
            TransformTypeButton(
                type = TransformType.UNIFORM,
                label = "Uniform",
                icon = "📏",
                isSelected = transformType == TransformType.UNIFORM,
                onClick = { onTransformTypeChange(TransformType.UNIFORM) }
            )
            
            TransformTypeButton(
                type = TransformType.DISTORT,
                label = "Distort",
                icon = "📐",
                isSelected = transformType == TransformType.DISTORT,
                onClick = { onTransformTypeChange(TransformType.DISTORT) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Quick actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickActionButton(
                label = "Flip H",
                icon = "↔️",
                onClick = onFlipHorizontal
            )
            
            QuickActionButton(
                label = "Flip V",
                icon = "↕️",
                onClick = onFlipVertical
            )
            
            QuickActionButton(
                label = "90° CW",
                icon = "🔄",
                onClick = onRotate90
            )
            
            QuickActionButton(
                label = "Reset",
                icon = "↺",
                onClick = onReset
            )
        }
    }
}

/**
 * Button for transform type selection
 */
@Composable
private fun TransformTypeButton(
    type: TransformType,
    label: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF4A90E2) else Color(0xFF2A2A2A),
            contentColor = Color.White
        ),
        modifier = Modifier
            .width(100.dp)
            .height(48.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/**
 * Button for quick transform actions
 */
@Composable
private fun QuickActionButton(
    label: String,
    icon: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = Color.White
        ),
        modifier = Modifier.width(80.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/**
 * Transform status bar showing current transform values
 */
@Composable
fun TransformStatusBar(
    scalePercentage: Int,
    rotationAngle: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A).copy(alpha = 0.9f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Scale: $scalePercentage%",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White
        )
        
        Text(
            text = "Rotation: ${formatRotationAngle(rotationAngle)}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White
        )
    }
}

/**
 * Header bar with Cancel and Apply buttons
 */
@Composable
fun TransformHeaderBar(
    onCancel: () -> Unit,
    onApply: () -> Unit,
    canApply: Boolean,
    isApplying: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onCancel,
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color.White
            )
        ) {
            Text("Cancel")
        }
        
        Text(
            text = "Transform Mode",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        
        Button(
            onClick = onApply,
            enabled = canApply && !isApplying,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4A90E2),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF2A2A2A),
                disabledContentColor = Color(0xFF666666)
            )
        ) {
            Text(if (isApplying) "Applying..." else "Apply")
        }
    }
}
