package com.artboard.ui.brush

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artboard.data.model.Brush
import com.artboard.ui.brush.components.BrushCategory

/**
 * Compact Brush Selector Popover for Artboard
 * 
 * A compact, canvas-friendly brush picker that slides from the right edge.
 * Designed following the Tool Organization Philosophy:
 * - Canvas visibility: 70%+ maintained while popover is open
 * - Quick access to brushes without full-screen modal
 * - Progressive disclosure with favorites at top
 * 
 * Layout (280dp wide, max 450dp height):
 * ┌─────────────────────────────┐
 * │ Sketch | Paint | Tex | FX   │  Category tabs (40dp)
 * ├─────────────────────────────┤
 * │ ┌──────────┐ ┌──────────┐   │
 * │ │ ~~~~~~   │ │ ●●●●●●   │   │  Brush cards (2 columns)
 * │ │ Pencil ★ │ │ Ink      │   │  (120×80dp each)
 * │ └──────────┘ └──────────┘   │
 * │         ...more...          │  Scrollable
 * ├─────────────────────────────┤
 * │ Current: Pencil (Sketch) ✏️  │  Quick info (48dp)
 * └─────────────────────────────┘
 * 
 * Animations:
 * - Open: Slide from right + fade, 200ms spring
 * - Close: Slide to right + fade, 150ms ease-out
 * - Brush select: Scale pulse on selected card
 * - Tab switch: Crossfade content, 150ms
 * 
 * @param isVisible Whether the popover is visible
 * @param currentBrush Currently selected brush
 * @param anchorY Y position to anchor popover (below brush button)
 * @param onBrushSelected Callback when brush is selected
 * @param onDismiss Callback to dismiss popover
 * @param autoCloseOnSelect Whether to auto-close after brush selection (default: true)
 */
@Composable
fun BrushSelectorPopover(
    isVisible: Boolean,
    currentBrush: Brush,
    anchorY: Float = 0f,
    onBrushSelected: (Brush) -> Unit,
    onDismiss: () -> Unit,
    autoCloseOnSelect: Boolean = true,
    viewModel: BrushSelectorViewModel = viewModel()
) {
    val brushes by viewModel.filteredBrushes.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    
    var selectedBrush by remember(currentBrush) { mutableStateOf(currentBrush) }
    
    // Animation for popover visibility
    val density = LocalDensity.current
    val popoverWidth = 280.dp
    val popoverWidthPx = with(density) { popoverWidth.toPx() }
    
    // Swipe to dismiss tracking
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = if (isVisible) 0f else popoverWidthPx,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "popover_slide"
    )
    
    // Scrim for tap-outside-to-dismiss
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(150)),
        exit = fadeOut(animationSpec = tween(100))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }
    
    // Popover content
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            initialOffsetX = { it }, // Start from right
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = 300f
            )
        ) + fadeIn(animationSpec = tween(200)),
        exit = slideOutHorizontally(
            targetOffsetX = { it }, // Exit to right
            animationSpec = tween(150, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(150)),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Surface(
                modifier = Modifier
                    .width(popoverWidth)
                    .heightIn(max = 450.dp)
                    .padding(end = 8.dp, top = 56.dp) // Clear of edge bar, below brush button
                    .offset { IntOffset(swipeOffset.toInt(), 0) }
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                // Dismiss if swiped more than 40% to the right
                                if (swipeOffset > popoverWidthPx * 0.4f) {
                                    onDismiss()
                                }
                                swipeOffset = 0f
                            },
                            onDragCancel = { swipeOffset = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                // Only allow swipe to the right (positive)
                                swipeOffset = (swipeOffset + dragAmount).coerceAtLeast(0f)
                            }
                        )
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume clicks to prevent dismissal
                    ),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1C1C1E), // Dark background per spec
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Category tabs (compact, text-only)
                    BrushCategoryTabs(
                        categories = listOf(
                            BrushCategory.SKETCH,
                            BrushCategory.PAINT,
                            BrushCategory.TEXTURE,
                            BrushCategory.EFFECTS
                        ),
                        selectedCategory = selectedCategory,
                        onCategorySelected = viewModel::selectCategory,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Brush grid (2 columns, scrollable)
                    CompactBrushGrid(
                        brushes = brushes,
                        selectedBrush = selectedBrush,
                        onBrushSelected = { brush ->
                            selectedBrush = brush
                            onBrushSelected(brush)
                            if (autoCloseOnSelect) {
                                onDismiss()
                            }
                        },
                        onFavoriteToggle = viewModel::toggleFavorite,
                        isFavorite = viewModel::isFavorite,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    
                    // Quick info bar
                    BrushQuickInfo(
                        brush = selectedBrush,
                        category = selectedCategory,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Compact category tabs with abbreviated labels and underline indicator
 * 
 * Per spec:
 * - 4 categories: Sketch, Paint, Texture (Tex), Effects (FX)
 * - Compact tabs (text only, no icons)
 * - Active tab: underline indicator
 * - Swipe to switch categories
 */
@Composable
fun BrushCategoryTabs(
    categories: List<BrushCategory>,
    selectedCategory: BrushCategory,
    onCategorySelected: (BrushCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = categories.indexOf(selectedCategory).coerceAtLeast(0)
    
    // Swipe gesture for category switching
    var swipeAccumulator by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 100f
    
    Column(
        modifier = modifier
            .height(40.dp)
            .background(Color(0xFF2C2C2E))
            .pointerInput(categories) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (swipeAccumulator < -swipeThreshold && selectedIndex < categories.lastIndex) {
                            onCategorySelected(categories[selectedIndex + 1])
                        } else if (swipeAccumulator > swipeThreshold && selectedIndex > 0) {
                            onCategorySelected(categories[selectedIndex - 1])
                        }
                        swipeAccumulator = 0f
                    },
                    onDragCancel = { swipeAccumulator = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        swipeAccumulator += dragAmount
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { category ->
                CompactCategoryTab(
                    category = category,
                    isSelected = category == selectedCategory,
                    onClick = { onCategorySelected(category) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Animated underline indicator
        CompactUnderlineIndicator(
            selectedIndex = selectedIndex,
            totalTabs = categories.size
        )
    }
}

@Composable
private fun CompactCategoryTab(
    category: BrushCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFF8E8E93),
        animationSpec = tween(150),
        label = "tab_color"
    )
    
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = category.compactLabel,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor
        )
    }
}

@Composable
private fun CompactUnderlineIndicator(
    selectedIndex: Int,
    totalTabs: Int
) {
    val indicatorOffset by animateFloatAsState(
        targetValue = selectedIndex.toFloat() / totalTabs,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "indicator_offset"
    )
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
    ) {
        val tabWidth = maxWidth / totalTabs
        
        Box(
            modifier = Modifier
                .offset(x = tabWidth * selectedIndex)
                .width(tabWidth)
                .height(2.dp)
                .background(
                    color = Color(0xFF007AFF), // System blue per EdgeControlBar spec
                    shape = RoundedCornerShape(topStart = 1.dp, topEnd = 1.dp)
                )
        )
    }
}

/**
 * Compact label for category tabs (abbreviated for narrow width)
 */
private val BrushCategory.compactLabel: String
    get() = when (this) {
        BrushCategory.SKETCH -> "Sketch"
        BrushCategory.PAINT -> "Paint"
        BrushCategory.TEXTURE -> "Tex"
        BrushCategory.EFFECTS -> "FX"
        BrushCategory.FAVORITES -> "★"
    }
