package com.artboard.ui.brush.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Brush category enum for organizing brushes
 */
enum class BrushCategory(val displayName: String) {
    SKETCH("Sketch"),
    PAINT("Paint"),
    TEXTURE("Texture"),
    EFFECTS("Effects"),
    FAVORITES("Favorites")
}

/**
 * Category tabs with sliding indicator animation
 * 
 * Visual specifications:
 * - Height: 48dp
 * - Active text: #FFFFFF, 14sp Medium
 * - Inactive text: #888888, 14sp Regular
 * - Indicator: 4dp height, #4A90E2
 * - Animation: 300ms ease-out slide
 */
@Composable
fun CategoryTabs(
    categories: List<BrushCategory>,
    selectedCategory: BrushCategory,
    onCategorySelected: (BrushCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = categories.indexOf(selectedCategory)
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color(0xFF1A1A1A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            categories.forEachIndexed { index, category ->
                CategoryTab(
                    category = category,
                    isSelected = category == selectedCategory,
                    onClick = { onCategorySelected(category) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Sliding indicator
        SlidingIndicator(
            selectedIndex = selectedIndex,
            totalTabs = categories.size
        )
    }
}

/**
 * Individual category tab
 */
@Composable
private fun CategoryTab(
    category: BrushCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Show star icon for Favorites category
            if (category == BrushCategory.FAVORITES) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else Color(0xFF888888),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            
            Text(
                text = category.displayName,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = if (isSelected) Color.White else Color(0xFF888888)
            )
        }
    }
}

/**
 * Animated sliding indicator that moves between tabs
 */
@Composable
private fun SlidingIndicator(
    selectedIndex: Int,
    totalTabs: Int
) {
    // Calculate indicator position and width
    val indicatorWidth = 1f / totalTabs
    val indicatorOffset by animateDpAsState(
        targetValue = (selectedIndex * indicatorWidth * 100).dp,
        animationSpec = tween(durationMillis = 300),
        label = "indicator_position"
    )
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
    ) {
        val tabWidth = maxWidth / totalTabs
        
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset * maxWidth.value / 100)
                .width(tabWidth)
                .height(4.dp)
                .background(
                    color = Color(0xFF4A90E2),
                    shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                )
        )
    }
}

/**
 * Simpler horizontal scrollable tabs version (alternative implementation)
 */
@Composable
fun CategoryTabsScrollable(
    categories: List<BrushCategory>,
    selectedCategory: BrushCategory,
    onCategorySelected: (BrushCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    ScrollableTabRow(
        selectedTabIndex = categories.indexOf(selectedCategory),
        modifier = modifier.height(48.dp),
        containerColor = Color(0xFF1A1A1A),
        contentColor = Color.White,
        indicator = { tabPositions ->
            if (tabPositions.isNotEmpty() && selectedCategory in categories) {
                val selectedIndex = categories.indexOf(selectedCategory)
                if (selectedIndex in tabPositions.indices) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color(0xFF4A90E2))
                    )
                }
            }
        },
        divider = {}
    ) {
        categories.forEach { category ->
            Tab(
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                text = {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (category == BrushCategory.FAVORITES) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (category == selectedCategory) Color.White else Color(0xFF888888),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        
                        Text(
                            text = category.displayName,
                            fontSize = 14.sp,
                            fontWeight = if (category == selectedCategory) FontWeight.Medium else FontWeight.Normal,
                            color = if (category == selectedCategory) Color.White else Color(0xFF888888)
                        )
                    }
                }
            )
        }
    }
}
