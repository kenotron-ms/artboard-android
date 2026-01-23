package com.artboard.ui.brush

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artboard.data.model.Brush
import com.artboard.ui.brush.components.*

/**
 * Full-screen brush selector panel with live previews
 * 
 * Layout:
 * - Header: Title and close button (56dp)
 * - Category tabs: Sketch, Paint, Texture, Effects, Favorites (48dp)
 * - Brush grid: 3-4 column adaptive grid with 120×120dp cards
 * - Quick controls: Size, Opacity, Flow sliders (always visible)
 * - Advanced settings: Expandable Brush Studio panel
 * 
 * Visual specifications:
 * - Background: #F0000000 (94% opacity black)
 * - Grid spacing: 12dp between cards, 16dp padding
 * - Smooth animations: 300ms category switch, 400ms expand/collapse
 */
@Composable
fun BrushSelectorPanel(
    currentBrush: Brush,
    onBrushSelected: (Brush) -> Unit,
    onDismiss: () -> Unit,
    viewModel: BrushSelectorViewModel = viewModel()
) {
    val brushes by viewModel.filteredBrushes.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val tempBrush by viewModel.tempBrush.collectAsState()
    val showBrushStudio by viewModel.showBrushStudio.collectAsState()
    
    var selectedBrush by remember { mutableStateOf(currentBrush) }
    val activeBrush = tempBrush ?: selectedBrush
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xF0000000) // 94% opacity black
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                BrushSelectorHeader(onDismiss = onDismiss)
                
                // Category tabs
                CategoryTabs(
                    categories = listOf(
                        BrushCategory.SKETCH,
                        BrushCategory.PAINT,
                        BrushCategory.TEXTURE,
                        BrushCategory.EFFECTS,
                        BrushCategory.FAVORITES
                    ),
                    selectedCategory = selectedCategory,
                    onCategorySelected = viewModel::selectCategory
                )
                
                // Brush grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(120.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(
                        items = brushes,
                        key = { it.hashCode() }
                    ) { brush ->
                        BrushPreviewCard(
                            brush = brush,
                            isSelected = brush == selectedBrush,
                            isFavorite = viewModel.isFavorite(brush),
                            onClick = {
                                selectedBrush = brush
                                onBrushSelected(brush)
                                onDismiss()
                            },
                            onLongPress = {
                                // TODO: Show context menu
                            },
                            onFavoriteClick = {
                                viewModel.toggleFavorite(brush)
                            },
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                }
                
                // Quick controls
                BrushQuickControls(
                    brush = activeBrush,
                    onBrushChanged = { updated ->
                        viewModel.setTempBrush(updated)
                        selectedBrush = updated
                    },
                    onAdvancedClick = {
                        viewModel.toggleBrushStudio()
                    }
                )
                
                // Brush Studio (expandable)
                AnimatedVisibility(
                    visible = showBrushStudio,
                    enter = expandVertically(animationSpec = tween(300)),
                    exit = shrinkVertically(animationSpec = tween(300))
                ) {
                    BrushStudioPanel(
                        brush = activeBrush,
                        onBrushChanged = { updated ->
                            viewModel.setTempBrush(updated)
                            selectedBrush = updated
                        }
                    )
                }
            }
        }
    }
}

/**
 * Header with title and close button
 */
@Composable
private fun BrushSelectorHeader(
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        color = Color(0xFF1A1A1A)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Brushes",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Brush Studio panel for advanced brush settings
 * Shown when user taps "Advanced Settings" button
 */
@Composable
private fun BrushStudioPanel(
    brush: Brush,
    onBrushChanged: (Brush) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1A1A1A),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "ADVANCED SETTINGS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF888888),
                letterSpacing = 1.sp
            )
            
            // Core properties
            Text(
                text = "Core Properties",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            
            QuickSlider(
                label = "Hardness",
                value = brush.hardness,
                valueRange = 0f..1f,
                onValueChange = { onBrushChanged(brush.copy(hardness = it)) },
                valueDisplay = "${(brush.hardness * 100).toInt()}%",
                logarithmic = false
            )
            
            QuickSlider(
                label = "Spacing",
                value = brush.spacing,
                valueRange = 0.01f..1f,
                onValueChange = { onBrushChanged(brush.copy(spacing = it)) },
                valueDisplay = "${(brush.spacing * 100).toInt()}%",
                logarithmic = false
            )
            
            // Pressure dynamics
            Text(
                text = "Pressure Dynamics",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Size Dynamics",
                    fontSize = 14.sp,
                    color = Color.White
                )
                Switch(
                    checked = brush.pressureSizeEnabled,
                    onCheckedChange = { onBrushChanged(brush.copy(pressureSizeEnabled = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF4A90E2),
                        checkedTrackColor = Color(0xFF4A90E2).copy(alpha = 0.5f)
                    )
                )
            }
            
            if (brush.pressureSizeEnabled) {
                QuickSlider(
                    label = "Min Size",
                    value = brush.minPressureSize,
                    valueRange = 0f..1f,
                    onValueChange = { onBrushChanged(brush.copy(minPressureSize = it)) },
                    valueDisplay = "${(brush.minPressureSize * 100).toInt()}%",
                    logarithmic = false
                )
            }
            
            // Tilt dynamics
            Text(
                text = "Tilt Dynamics",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tilt Affects Size",
                    fontSize = 14.sp,
                    color = Color.White
                )
                Switch(
                    checked = brush.tiltSizeEnabled,
                    onCheckedChange = { onBrushChanged(brush.copy(tiltSizeEnabled = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF4A90E2),
                        checkedTrackColor = Color(0xFF4A90E2).copy(alpha = 0.5f)
                    )
                )
            }
            
            if (brush.tiltSizeEnabled) {
                QuickSlider(
                    label = "Tilt Min",
                    value = brush.tiltSizeMin,
                    valueRange = 0.1f..3f,
                    onValueChange = { onBrushChanged(brush.copy(tiltSizeMin = it)) },
                    valueDisplay = "${brush.tiltSizeMin.format(1)}×",
                    logarithmic = false
                )
                
                QuickSlider(
                    label = "Tilt Max",
                    value = brush.tiltSizeMax,
                    valueRange = 0.1f..5f,
                    onValueChange = { onBrushChanged(brush.copy(tiltSizeMax = it)) },
                    valueDisplay = "${brush.tiltSizeMax.format(1)}×",
                    logarithmic = false
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tilt Affects Angle",
                    fontSize = 14.sp,
                    color = Color.White
                )
                Switch(
                    checked = brush.tiltAngleEnabled,
                    onCheckedChange = { onBrushChanged(brush.copy(tiltAngleEnabled = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF4A90E2),
                        checkedTrackColor = Color(0xFF4A90E2).copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

/**
 * Extension function to format float with decimal places
 */
private fun Float.format(decimals: Int): String = "%.${decimals}f".format(this)
