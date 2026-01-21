# UI/UX Design Specification

This document defines the complete user interface and experience design for Artboard to match Procreate's intuitive, gesture-driven workflow.

---

## Design Philosophy

**Core Principles**:
1. **Canvas-First**: Maximum space for artwork, minimal chrome
2. **Gesture-Driven**: Two/three-finger shortcuts for common actions
3. **Context-Aware**: Tools appear when needed, hide when not
4. **No Nested Menus**: One tap to access any tool
5. **Professional Feel**: Dark theme, focused, distraction-free

---

## 1. Screen Layout Architecture

### Full-Screen Canvas Mode (Default)

```
┌─────────────────────────────────────────┐
│  [☰] [⟲] [✓]              [👁] [□] [⋮]  │  ← Minimal top bar (32dp)
├─────────────────────────────────────────┤
│                                         │
│                                         │
│                                         │
│            CANVAS AREA                  │
│         (Full screen space)             │
│                                         │
│                                         │
│                                         │
│                                         │
├─────────────────────────────────────────┤
│  🖌️ ⬛ [10px]         ↶  ↷  [Layers]   │  ← Tool bar (56dp)
└─────────────────────────────────────────┘

Total UI height: 88dp
Canvas: 100% - 88dp
```

### Tool Panel Mode (When Editing)

```
┌─────────────────────────────────────────┐
│  [☰] Brush Studio           [✓] [✗]    │  ← Context header
├─────────────────────────────────────────┤
│  ┌───────────────┐  │                  │
│  │               │  │                  │
│  │    Canvas     │  │   Tool Panel    │  ← Split view
│  │   (60% width) │  │   (40% width)   │
│  │               │  │                  │
│  └───────────────┘  │                  │
└─────────────────────────────────────────┘
```

### Layer Panel Mode

```
┌─────────────────────────────────────────┐
│                                         │
│                                         │
│            CANVAS                       │  ← Top 60%
│                                         │
├─────────────────────────────────────────┤
│  LAYERS                    [+] [⋮]     │  ← Layer panel header
│  ┌────┐ Layer 3      [👁] [🔒]        │
│  │img │ 85% • Multiply                │  ← Bottom 40%
│  ┌────┐ Background   [👁] [ ]         │
└─────────────────────────────────────────┘
```

---

## 2. Gesture System Specification

### Two-Finger Gestures

| Gesture | Action | Detection Logic | Cancellation |
|---------|--------|-----------------|--------------|
| **Tap** (quick) | Undo | 2 touches, duration < 200ms, movement < 20px | Any movement |
| **Pinch** | Zoom | 2 touches, distance changing | Lift either finger |
| **Rotate** | Rotate canvas | 2 touches, angle changing > 5° | Pinch or pan |
| **Pan** | Move canvas | 2 touches, midpoint moving | Pinch or rotate |
| **Long-press** | QuickMenu | 2 touches, hold > 500ms | Any movement |

### Three-Finger Gestures

| Gesture | Action | Detection Logic |
|---------|--------|-----------------|
| **Swipe Down** | Copy/Cut/Paste menu | 3 touches, vertical velocity > 1000px/s |
| **Swipe Right** | Undo | 3 touches, horizontal velocity > 1000px/s |
| **Swipe Left** | Redo | 3 touches, horizontal velocity > 1000px/s |
| **Tap** | Clear layer | 3 touches, duration < 200ms |

### Gesture Implementation

```kotlin
class GestureDetector {
    
    private val activePointers = mutableMapOf<Int, PointerInfo>()
    private var gestureStartTime = 0L
    private var gestureType: GestureType? = null
    
    fun onTouchEvent(event: MotionEvent): GestureResult? {
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerCount = event.pointerCount
                
                when (pointerCount) {
                    2 -> handleTwoFingerStart(event)
                    3 -> handleThreeFingerStart(event)
                }
            }
            
            MotionEvent.ACTION_MOVE -> {
                val pointerCount = event.pointerCount
                
                when (pointerCount) {
                    2 -> return handleTwoFingerMove(event)
                    3 -> return handleThreeFingerMove(event)
                }
            }
            
            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP -> {
                return handleGestureEnd(event)
            }
        }
        
        return null
    }
    
    private fun handleTwoFingerMove(event: MotionEvent): GestureResult? {
        val p0 = PointF(event.getX(0), event.getY(0))
        val p1 = PointF(event.getX(1), event.getY(1))
        
        val currentDistance = distance(p0, p1)
        val currentAngle = angle(p0, p1)
        val currentMidpoint = midpoint(p0, p1)
        
        // Determine gesture type if not yet determined
        if (gestureType == null) {
            val distanceChange = abs(currentDistance - initialDistance)
            val angleChange = abs(currentAngle - initialAngle)
            val midpointMovement = distance(currentMidpoint, initialMidpoint)
            
            gestureType = when {
                distanceChange > 50 -> GestureType.PINCH
                angleChange > 10 -> GestureType.ROTATE
                midpointMovement > 30 -> GestureType.PAN
                else -> null
            }
        }
        
        // Process gesture
        return when (gestureType) {
            GestureType.PINCH -> GestureResult.Zoom(currentDistance / initialDistance)
            GestureType.ROTATE -> GestureResult.Rotate(currentAngle - initialAngle)
            GestureType.PAN -> GestureResult.Pan(
                dx = currentMidpoint.x - lastMidpoint.x,
                dy = currentMidpoint.y - lastMidpoint.y
            )
            else -> null
        }
    }
    
    private fun handleThreeFingerMove(event: MotionEvent): GestureResult? {
        // Calculate average movement vector
        val avgVelocity = calculateAverageVelocity(event)
        
        // Detect swipe direction
        if (abs(avgVelocity.x) > 1000 || abs(avgVelocity.y) > 1000) {
            return when {
                avgVelocity.y > 1000 -> GestureResult.ThreeFingerSwipeDown
                avgVelocity.x > 1000 -> GestureResult.ThreeFingerSwipeRight
                avgVelocity.x < -1000 -> GestureResult.ThreeFingerSwipeLeft
                else -> null
            }
        }
        
        return null
    }
    
    private fun handleGestureEnd(event: MotionEvent): GestureResult? {
        val duration = System.currentTimeMillis() - gestureStartTime
        val pointerCount = event.pointerCount
        
        // Check for tap gestures
        if (duration < 200) {
            return when (pointerCount) {
                2 -> GestureResult.TwoFingerTap  // Undo
                3 -> GestureResult.ThreeFingerTap  // Clear layer
                else -> null
            }
        }
        
        // Check for long-press
        if (duration > 500 && pointerCount == 2) {
            return GestureResult.TwoFingerLongPress  // QuickMenu
        }
        
        gestureType = null
        return null
    }
}

sealed class GestureResult {
    data class Zoom(val scale: Float) : GestureResult()
    data class Rotate(val degrees: Float) : GestureResult()
    data class Pan(val dx: Float, val dy: Float) : GestureResult()
    object TwoFingerTap : GestureResult()
    object TwoFingerLongPress : GestureResult()
    object ThreeFingerSwipeDown : GestureResult()
    object ThreeFingerSwipeRight : GestureResult()
    object ThreeFingerSwipeLeft : GestureResult()
    object ThreeFingerTap : GestureResult()
}
```

---

## 3. QuickMenu System

### Radial Menu Design

```
        [Eraser]
            │
    [Undo]──●──[Brush]
            │
        [Color]
```

### QuickMenu Implementation

```kotlin
@Composable
fun QuickMenu(
    centerPosition: Offset,
    actions: List<QuickMenuAction>,
    onActionSelected: (QuickMenuAction) -> Unit,
    onDismiss: () -> Unit
) {
    val radius = 120.dp.toPx()
    val angleStep = 360f / actions.size
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Draw center circle
        drawCircle(
            color = Color.DarkGray,
            radius = 40f,
            center = centerPosition,
            alpha = 0.9f
        )
        
        // Draw action segments
        actions.forEachIndexed { index, action ->
            val angle = angleStep * index - 90f  // Start at top
            val radians = Math.toRadians(angle.toDouble())
            
            val x = centerPosition.x + radius * cos(radians).toFloat()
            val y = centerPosition.y + radius * sin(radians).toFloat()
            
            // Action circle
            drawCircle(
                color = Color.Gray,
                radius = 50f,
                center = Offset(x, y),
                alpha = 0.9f
            )
            
            // Action icon
            drawIcon(action.icon, Offset(x, y))
            
            // Action label
            drawText(action.label, Offset(x, y + 70f))
        }
        
        // Draw connecting lines
        actions.forEachIndexed { index, _ ->
            val angle = angleStep * index - 90f
            val radians = Math.toRadians(angle.toDouble())
            
            val startX = centerPosition.x + 40f * cos(radians).toFloat()
            val startY = centerPosition.y + 40f * sin(radians).toFloat()
            val endX = centerPosition.x + (radius - 50f) * cos(radians).toFloat()
            val endY = centerPosition.y + (radius - 50f) * sin(radians).toFloat()
            
            drawLine(
                color = Color.White,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 2f,
                alpha = 0.5f
            )
        }
    }
}

data class QuickMenuAction(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val action: () -> Unit
)

object DefaultQuickMenuActions {
    fun getDrawingQuickMenu() = listOf(
        QuickMenuAction("brush", "Brush", Icons.Default.Edit) { /* Show brushes */ },
        QuickMenuAction("eraser", "Eraser", Icons.Default.Delete) { /* Switch to eraser */ },
        QuickMenuAction("color", "Color", Icons.Default.Palette) { /* Show color picker */ },
        QuickMenuAction("undo", "Undo", Icons.Default.Undo) { /* Undo */ },
        QuickMenuAction("layers", "Layers", Icons.Default.Layers) { /* Show layers */ },
        QuickMenuAction("clear", "Clear", Icons.Default.Clear) { /* Clear layer */ }
    )
}
```

---

## 4. Canvas Navigation

### Zoom, Pan, Rotate Implementation

```kotlin
class CanvasTransform {
    
    var scale: Float = 1f
        private set
    var translateX: Float = 0f
        private set
    var translateY: Float = 0f
        private set
    var rotation: Float = 0f
        private set
    
    private val matrix = Matrix()
    
    fun zoom(scaleFactor: Float, pivotX: Float, pivotY: Float) {
        scale = (scale * scaleFactor).coerceIn(0.1f, 10f)
        
        // Zoom around pivot point
        translateX = pivotX - (pivotX - translateX) * scaleFactor
        translateY = pivotY - (pivotY - translateY) * scaleFactor
        
        updateMatrix()
    }
    
    fun pan(dx: Float, dy: Float) {
        translateX += dx
        translateY += dy
        updateMatrix()
    }
    
    fun rotate(degrees: Float, pivotX: Float, pivotY: Float) {
        rotation = (rotation + degrees) % 360f
        
        // Rotate around pivot
        matrix.postRotate(degrees, pivotX, pivotY)
        
        updateMatrix()
    }
    
    fun reset() {
        scale = 1f
        translateX = 0f
        translateY = 0f
        rotation = 0f
        updateMatrix()
    }
    
    fun resetRotation() {
        rotation = 0f
        updateMatrix()
    }
    
    private fun updateMatrix() {
        matrix.reset()
        matrix.postScale(scale, scale)
        matrix.postRotate(rotation)
        matrix.postTranslate(translateX, translateY)
    }
    
    /**
     * Convert screen coordinates to canvas coordinates
     */
    fun screenToCanvas(screenX: Float, screenY: Float): PointF {
        val inverse = Matrix()
        matrix.invert(inverse)
        
        val point = floatArrayOf(screenX, screenY)
        inverse.mapPoints(point)
        
        return PointF(point[0], point[1])
    }
    
    /**
     * Convert canvas coordinates to screen coordinates
     */
    fun canvasToScreen(canvasX: Float, canvasY: Float): PointF {
        val point = floatArrayOf(canvasX, canvasY)
        matrix.mapPoints(point)
        
        return PointF(point[0], point[1])
    }
    
    fun getTransformMatrix(): Matrix = matrix
}
```

### Canvas with Transform

```kotlin
override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    
    // Apply canvas transform
    canvas.save()
    canvas.setMatrix(canvasTransform.getTransformMatrix())
    
    // Draw background
    canvas.drawColor(backgroundColor)
    
    // Draw composite
    compositeBitmap?.let { bitmap ->
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
    }
    
    // Draw current stroke
    if (drawingEngine.isDrawing()) {
        drawingBitmap?.let { bitmap ->
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
        }
    }
    
    canvas.restore()
    
    // Draw UI overlays (not transformed)
    drawUIOverlays(canvas)
}
```

---

## 5. Tool Switcher Interface

### Bottom Tool Bar

```kotlin
@Composable
fun ToolBar(
    currentTool: Tool,
    currentBrush: Brush,
    currentColor: Color,
    canUndo: Boolean,
    canRedo: Boolean,
    onToolSelect: (Tool) -> Unit,
    onBrushClick: () -> Unit,
    onColorClick: () -> Unit,
    onSizeChange: (Float) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onLayersClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Tool selection
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brush
                ToolButton(
                    icon = Icons.Default.Edit,
                    label = currentBrush.name,
                    selected = currentTool == Tool.BRUSH,
                    onClick = onBrushClick
                )
                
                // Color swatch
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(currentColor, CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                        .clickable(onClick = onColorClick)
                )
                
                // Size indicator
                Text(
                    "${currentBrush.size.toInt()}px",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
            
            // Center: History
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        "Undo",
                        tint = if (canUndo) Color.White else Color.Gray
                    )
                }
                
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        "Redo",
                        tint = if (canRedo) Color.White else Color.Gray
                    )
                }
            }
            
            // Right: Layer controls
            Row {
                IconButton(onClick = onLayersClick) {
                    Badge(
                        badgeContent = { Text("3") }  // Layer count
                    ) {
                        Icon(Icons.Default.Layers, "Layers", tint = Color.White)
                    }
                }
            }
        }
    }
}

enum class Tool {
    BRUSH,
    ERASER,
    SMUDGE,
    SELECTION,
    FILL,
    EYEDROPPER,
    TRANSFORM
}
```

---

## 6. Color Picker Interface

### Disc Color Picker (Primary)

```kotlin
@Composable
fun DiscColorPicker(
    currentColor: Color,
    onColorChange: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .size(350.dp)
                .padding(16.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(contentAlignment = Alignment.Center) {
                // HSV Color Disc
                ColorDiscCanvas(
                    currentColor = currentColor,
                    onColorChange = onColorChange,
                    modifier = Modifier.size(300.dp)
                )
                
                // Center preview circle
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(currentColor, CircleShape)
                        .border(3.dp, Color.White, CircleShape)
                )
            }
        }
    }
}

@Composable
fun ColorDiscCanvas(
    currentColor: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPoint by remember { mutableStateOf<Offset?>(null) }
    
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val point = change.position
                    
                    val dx = point.x - center.x
                    val dy = point.y - center.y
                    val distance = sqrt(dx * dx + dy * dy)
                    val radius = size.width / 2f
                    
                    // Determine if in hue ring or saturation/value triangle
                    if (distance > radius * 0.7f) {
                        // Hue ring
                        val angle = atan2(dy, dx)
                        val hue = (Math.toDegrees(angle.toDouble()).toFloat() + 360f) % 360f
                        
                        val hsv = FloatArray(3)
                        currentColor.toArgb().let { android.graphics.Color.colorToHSV(it, hsv) }
                        hsv[0] = hue
                        
                        val newColor = Color(android.graphics.Color.HSVToColor(hsv))
                        onColorChange(newColor)
                    } else {
                        // Saturation/Value triangle
                        val hsv = FloatArray(3)
                        currentColor.toArgb().let { android.graphics.Color.colorToHSV(it, hsv) }
                        
                        // Convert point to saturation/value
                        val (sat, value) = pointToSV(point, center, radius * 0.7f, hsv[0])
                        hsv[1] = sat.coerceIn(0f, 1f)
                        hsv[2] = value.coerceIn(0f, 1f)
                        
                        val newColor = Color(android.graphics.Color.HSVToColor(hsv))
                        onColorChange(newColor)
                    }
                    
                    selectedPoint = point
                }
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width / 2f
        
        // Draw hue ring
        for (i in 0 until 360) {
            val angle = Math.toRadians(i.toDouble())
            val innerRadius = radius * 0.7f
            val outerRadius = radius
            
            val color = Color.hsv(i.toFloat(), 1f, 1f)
            
            drawArc(
                color = color,
                startAngle = i.toFloat() - 0.5f,
                sweepAngle = 1f,
                useCenter = false,
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = Size(outerRadius * 2, outerRadius * 2),
                style = Stroke(width = outerRadius - innerRadius)
            )
        }
        
        // Draw saturation/value triangle
        drawSVTriangle(center, radius * 0.7f, currentColor)
        
        // Draw selection indicator
        selectedPoint?.let { point ->
            drawCircle(
                color = Color.White,
                radius = 10f,
                center = point,
                style = Stroke(width = 3f)
            )
        }
    }
}
```

---

## 7. Brush Library Interface

### Brush Browser

```kotlin
@Composable
fun BrushLibrary(
    brushes: List<Brush>,
    currentBrush: Brush,
    onBrushSelect: (Brush) -> Unit,
    onBrushCustomize: (Brush) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(BrushCategory.SKETCHING) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Brush Library",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }
                
                // Category tabs
                ScrollableTabRow(selectedTabIndex = selectedCategory.ordinal) {
                    BrushCategory.values().forEach { category ->
                        Tab(
                            selected = category == selectedCategory,
                            onClick = { selectedCategory = category },
                            text = { Text(category.name) }
                        )
                    }
                }
                
                // Brush grid
                val categoryBrushes = brushes.filter { it.category == selectedCategory }
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categoryBrushes) { brush ->
                        BrushCard(
                            brush = brush,
                            isSelected = brush.id == currentBrush.id,
                            onClick = {
                                onBrushSelect(brush)
                                onDismiss()
                            },
                            onCustomize = { onBrushCustomize(brush) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BrushCard(
    brush: Brush,
    isSelected: Boolean,
    onClick: () -> Unit,
    onCustomize: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Brush preview stroke
            BrushPreviewCanvas(
                brush = brush,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
            
            // Brush name
            Text(
                text = brush.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            
            // Customize button (if selected)
            if (isSelected) {
                IconButton(
                    onClick = onCustomize,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Settings, "Customize", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
```

---

## 8. Size and Opacity Sliders (Side HUD)

### Floating Adjustment Controls

```kotlin
@Composable
fun BrushAdjustmentHUD(
    currentBrush: Brush,
    onSizeChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    position: Alignment = Alignment.CenterEnd
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = position
    ) {
        Column(
            modifier = Modifier
                .width(80.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Size slider (vertical)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Brush, "Size", tint = Color.White)
                
                VerticalSlider(
                    value = currentBrush.size,
                    onValueChange = onSizeChange,
                    valueRange = 1f..200f,
                    modifier = Modifier.height(150.dp)
                )
                
                Text(
                    "${currentBrush.size.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
            
            Divider()
            
            // Opacity slider (vertical)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Opacity, "Opacity", tint = Color.White)
                
                VerticalSlider(
                    value = currentBrush.opacity,
                    onValueChange = onOpacityChange,
                    valueRange = 0f..100f,
                    modifier = Modifier.height(150.dp)
                )
                
                Text(
                    "${currentBrush.opacity.toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    // Rotated horizontal slider to make it vertical
    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = 270f }
    ) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.height(48.dp)
        )
    }
}
```

---

## 9. Eyedropper Tool

### Color Sampling with Magnifier

```kotlin
class EyedropperTool {
    
    private var magnifierVisible = false
    private var samplePosition: PointF? = null
    
    fun onLongPress(position: PointF) {
        magnifierVisible = true
        samplePosition = position
    }
    
    fun onMove(position: PointF) {
        if (!magnifierVisible) return
        samplePosition = position
    }
    
    fun onRelease(compositeBitmap: Bitmap): Int? {
        val pos = samplePosition ?: return null
        
        val x = pos.x.toInt().coerceIn(0, compositeBitmap.width - 1)
        val y = pos.y.toInt().coerceIn(0, compositeBitmap.height - 1)
        
        val sampledColor = compositeBitmap.getPixel(x, y)
        
        magnifierVisible = false
        samplePosition = null
        
        return sampledColor
    }
}

@Composable
fun EyedropperMagnifier(
    position: Offset,
    compositeBitmap: Bitmap,
    magnification: Float = 4f
) {
    val magnifierSize = 120.dp
    val pixelGridSize = 11  // Show 11x11 pixel grid
    
    Canvas(
        modifier = Modifier
            .offset { IntOffset(position.x.toInt(), position.y.toInt()) }
            .size(magnifierSize)
    ) {
        // Sample pixels around position
        val centerX = position.x.toInt()
        val centerY = position.y.toInt()
        val halfGrid = pixelGridSize / 2
        
        // Draw magnified pixels
        for (y in -halfGrid..halfGrid) {
            for (x in -halfGrid..halfGrid) {
                val srcX = (centerX + x).coerceIn(0, compositeBitmap.width - 1)
                val srcY = (centerY + y).coerceIn(0, compositeBitmap.height - 1)
                
                val color = compositeBitmap.getPixel(srcX, srcY)
                
                val rectSize = size.width / pixelGridSize
                val rectX = (x + halfGrid) * rectSize
                val rectY = (y + halfGrid) * rectSize
                
                drawRect(
                    color = Color(color),
                    topLeft = Offset(rectX, rectY),
                    size = Size(rectSize, rectSize)
                )
            }
        }
        
        // Draw crosshair on center pixel
        val centerRect = size.width / 2f
        drawRect(
            color = Color.White,
            topLeft = Offset(centerRect - 5, centerRect - 5),
            size = Size(10f, 10f),
            style = Stroke(width = 2f)
        )
        
        // Draw circle border
        drawCircle(
            color = Color.White,
            radius = size.width / 2f,
            style = Stroke(width = 4f)
        )
    }
}
```

---

## 10. Actions Menu (Top-Left)

### Gallery, Settings, Export

```kotlin
@Composable
fun ActionsMenu(
    onNewProject: () -> Unit,
    onOpenGallery: () -> Unit,
    onSave: () -> Unit,
    onExport: () -> Unit,
    onSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("Gallery") },
            leadingIcon = { Icon(Icons.Default.PhotoLibrary, null) },
            onClick = onOpenGallery
        )
        
        DropdownMenuItem(
            text = { Text("New") },
            leadingIcon = { Icon(Icons.Default.Add, null) },
            onClick = onNewProject
        )
        
        DropdownMenuItem(
            text = { Text("Save") },
            leadingIcon = { Icon(Icons.Default.Save, null) },
            onClick = onSave
        )
        
        Divider()
        
        DropdownMenuItem(
            text = { Text("Export Image") },
            leadingIcon = { Icon(Icons.Default.FileDownload, null) },
            onClick = onExport
        )
        
        Divider()
        
        DropdownMenuItem(
            text = { Text("Settings") },
            leadingIcon = { Icon(Icons.Default.Settings, null) },
            onClick = onSettings
        )
    }
}
```

---

## 11. Gallery View

### Project Thumbnail Grid

```kotlin
@Composable
fun GalleryScreen(
    projects: List<ProjectMetadata>,
    onProjectOpen: (String) -> Unit,
    onProjectDelete: (String) -> Unit,
    onNewProject: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gallery") },
                actions = {
                    IconButton(onClick = onNewProject) {
                        Icon(Icons.Default.Add, "New Project")
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(padding)
        ) {
            items(projects) { project ->
                ProjectCard(
                    project = project,
                    onClick = { onProjectOpen(project.id) },
                    onDelete = { onProjectDelete(project.id) }
                )
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: ProjectMetadata,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
    ) {
        Box {
            // Thumbnail
            AsyncImage(
                model = project.thumbnailPath,
                contentDescription = project.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Info overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
                    .padding(8.dp)
            ) {
                Text(
                    project.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    "${project.width}×${project.height} • ${project.layerCount} layers",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                
                Text(
                    formatDate(project.modifiedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            
            // Context menu
            if (showMenu) {
                DropdownMenu(
                    expanded = true,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Open") },
                        onClick = { onClick(); showMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        onClick = { /* Duplicate */ }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = { /* Share */ }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { onDelete(); showMenu = false }
                    )
                }
            }
        }
    }
}

data class ProjectMetadata(
    val id: String,
    val name: String,
    val thumbnailPath: String,
    val width: Int,
    val height: Int,
    val layerCount: Int,
    val createdAt: Long,
    val modifiedAt: Long
)
```

---

## 12. Settings Screen

### App Configuration

```kotlin
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Canvas settings
            item {
                SectionHeader("Canvas")
            }
            
            item {
                SettingItem(
                    title = "Default Canvas Size",
                    subtitle = "${settings.defaultCanvasWidth}×${settings.defaultCanvasHeight}",
                    onClick = { /* Show canvas size picker */ }
                )
            }
            
            item {
                SwitchSettingItem(
                    title = "Show FPS Counter",
                    checked = settings.showFPS,
                    onCheckedChange = { onSettingsChange(settings.copy(showFPS = it)) }
                )
            }
            
            item {
                SectionHeader("Gestures")
            }
            
            item {
                SwitchSettingItem(
                    title = "Two-Finger Undo",
                    checked = settings.twoFingerUndo,
                    onCheckedChange = { onSettingsChange(settings.copy(twoFingerUndo = it)) }
                )
            }
            
            item {
                SwitchSettingItem(
                    title = "Three-Finger Gestures",
                    checked = settings.threeFingerGestures,
                    onCheckedChange = { onSettingsChange(settings.copy(threeFingerGestures = it)) }
                )
            }
            
            item {
                SectionHeader("Performance")
            }
            
            item {
                SliderSettingItem(
                    title = "Undo History Depth",
                    value = settings.undoDepth.toFloat(),
                    valueRange = 10f..250f,
                    onValueChange = { onSettingsChange(settings.copy(undoDepth = it.toInt())) },
                    valueLabel = "${settings.undoDepth} steps"
                )
            }
            
            item {
                SwitchSettingItem(
                    title = "GPU Acceleration",
                    subtitle = "May drain battery faster",
                    checked = settings.gpuAcceleration,
                    onCheckedChange = { onSettingsChange(settings.copy(gpuAcceleration = it)) }
                )
            }
            
            item {
                SectionHeader("Interface")
            }
            
            item {
                SettingItem(
                    title = "Interface Theme",
                    subtitle = settings.theme.name,
                    onClick = { /* Show theme picker */ }
                )
            }
            
            item {
                SwitchSettingItem(
                    title = "Left-Handed Mode",
                    checked = settings.leftHandedMode,
                    onCheckedChange = { onSettingsChange(settings.copy(leftHandedMode = it)) }
                )
            }
        }
    }
}

data class AppSettings(
    val defaultCanvasWidth: Int = 2048,
    val defaultCanvasHeight: Int = 2048,
    val showFPS: Boolean = false,
    val twoFingerUndo: Boolean = true,
    val threeFingerGestures: Boolean = true,
    val undoDepth: Int = 100,
    val gpuAcceleration: Boolean = true,
    val theme: AppTheme = AppTheme.DARK,
    val leftHandedMode: Boolean = false,
    val palmRejection: Boolean = true,
    val stylusPressureCalibration: Float = 1f
)

enum class AppTheme {
    DARK,
    LIGHT,
    BLACK  // Pure black for OLED
}
```

---

## 13. Animation & Transitions

### Smooth UI Transitions

```kotlin
// Layer panel slide-in animation
val layerPanelOffset by animateDpAsState(
    targetValue = if (layerPanelVisible) 0.dp else 300.dp,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
)

// Tool panel fade animation
val toolPanelAlpha by animateFloatAsState(
    targetValue = if (toolPanelVisible) 1f else 0f,
    animationSpec = tween(durationMillis = 200)
)

// QuickMenu scale-in animation
val quickMenuScale by animateFloatAsState(
    targetValue = if (quickMenuVisible) 1f else 0f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
)
```

---

## 14. Accessibility Features

### Touch Target Sizes

| Element | Minimum Size | Recommended | Current |
|---------|--------------|-------------|---------|
| Tool buttons | 44×44 dp | 48×48 dp | 48×48 dp |
| Layer items | 48 dp height | 60 dp | 60 dp |
| Color swatches | 44×44 dp | 48×48 dp | 48×48 dp |
| Slider thumbs | 44×44 dp | 48×48 dp | 48×48 dp |

### Haptic Feedback

```kotlin
class HapticFeedback(context: Context) {
    
    private val vibrator = context.getSystemService(Vibrator::class.java)
    
    fun toolSelected() {
        // Light click
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
    }
    
    fun layerChanged() {
        // Medium bump
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
    }
    
    fun actionCompleted() {
        // Success feedback
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
    }
    
    fun error() {
        // Double tap
        vibrator.vibrate(VibrationEffect.createWaveform(
            longArrayOf(0, 50, 50, 50),
            intArrayOf(0, 100, 0, 100),
            -1
        ))
    }
}
```

---

## 15. Notification & Feedback System

### Toast Messages

```kotlin
object FeedbackSystem {
    
    fun showUndo(context: Context) {
        Toast.makeText(context, "Undo", Toast.LENGTH_SHORT).show()
    }
    
    fun showRedo(context: Context) {
        Toast.makeText(context, "Redo", Toast.LENGTH_SHORT).show()
    }
    
    fun showSaved(context: Context, filename: String) {
        Snackbar.make(view, "Saved: $filename", Snackbar.LENGTH_SHORT).show()
    }
    
    fun showExported(context: Context, path: String) {
        Snackbar.make(view, "Exported to $path", Snackbar.LENGTH_LONG)
            .setAction("Open") { /* Open in gallery */ }
            .show()
    }
    
    fun showError(context: Context, message: String) {
        Snackbar.make(view, "Error: $message", Snackbar.LENGTH_LONG)
            .setBackgroundTint(Color.RED)
            .show()
    }
}
```

---

## 16. Onboarding Experience

### First Launch Tutorial

```kotlin
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    
    val steps = listOf(
        OnboardingStep(
            title = "Welcome to Artboard",
            description = "Professional digital art on Android",
            illustration = R.drawable.onboarding_welcome
        ),
        OnboardingStep(
            title = "Draw with Your Stylus",
            description = "Pressure-sensitive brushes for natural drawing",
            illustration = R.drawable.onboarding_stylus
        ),
        OnboardingStep(
            title = "Two-Finger Gestures",
            description = "Tap to undo, pinch to zoom, rotate the canvas",
            illustration = R.drawable.onboarding_gestures
        ),
        OnboardingStep(
            title = "Layers & Blending",
            description = "Professional layer system with blend modes",
            illustration = R.drawable.onboarding_layers
        ),
        OnboardingStep(
            title = "Start Creating!",
            description = "Everything you need is one tap away",
            illustration = R.drawable.onboarding_final
        )
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Progress indicator
        LinearProgressIndicator(
            progress = (currentStep + 1).toFloat() / steps.size,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(Modifier.height(32.dp))
        
        // Current step
        val step = steps[currentStep]
        
        Image(
            painter = painterResource(step.illustration),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )
        
        Spacer(Modifier.height(32.dp))
        
        Text(
            step.title,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            step.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        
        Spacer(Modifier.weight(1f))
        
        // Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = { if (currentStep > 0) currentStep-- },
                enabled = currentStep > 0
            ) {
                Text("Back")
            }
            
            Button(
                onClick = {
                    if (currentStep < steps.size - 1) {
                        currentStep++
                    } else {
                        onComplete()
                    }
                }
            ) {
                Text(if (currentStep < steps.size - 1) "Next" else "Get Started")
            }
        }
    }
}

data class OnboardingStep(
    val title: String,
    val description: String,
    val illustration: Int  // Drawable resource
)
```

---

## 17. Dark Theme Specification

### Color Palette

```kotlin
val DarkColorScheme = darkColorScheme(
    // Primary (accent color for active elements)
    primary = Color(0xFF6B5CE7),           // Purple
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A3FB8),
    
    // Background (main canvas area)
    background = Color(0xFF1A1A1A),        // Near black
    onBackground = Color.White,
    
    // Surface (panels, toolbars)
    surface = Color(0xFF2D2D2D),           // Dark gray
    onSurface = Color.White,
    surfaceVariant = Color(0xFF3D3D3D),    // Slightly lighter
    
    // Secondary (less important actions)
    secondary = Color(0xFF4CAF50),         // Green
    onSecondary = Color.White,
    
    // Error
    error = Color(0xFFE57373),             // Light red
    onError = Color.White,
    
    // Outline
    outline = Color(0xFF555555),           // Subtle dividers
)
```

### Dark Theme Best Practices

1. **Contrast**: Ensure 4.5:1 minimum for text
2. **Elevation**: Use subtle shadows, not just color
3. **Canvas Brightness**: Option to dim UI without affecting canvas
4. **Eye Strain**: Avoid pure white on pure black

---

## 18. Tablet-Specific Optimizations

### Split-Screen Mode Support

```kotlin
// Handle multi-window mode
override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean) {
    if (isInMultiWindowMode) {
        // Adjust UI for smaller screen
        toolbarCompact = true
        layerPanelCompact = true
    } else {
        toolbarCompact = false
        layerPanelCompact = false
    }
}
```

### Landscape vs Portrait

```kotlin
@Composable
fun AdaptiveLayout(
    orientation: Int,
    content: @Composable (Boolean) -> Unit
) {
    val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE
    
    if (isLandscape) {
        // Landscape: Toolbars on sides
        Row {
            VerticalToolbar(modifier = Modifier.width(72.dp))
            
            Box(modifier = Modifier.weight(1f)) {
                content(true)
            }
            
            VerticalLayerPanel(modifier = Modifier.width(280.dp))
        }
    } else {
        // Portrait: Toolbars top/bottom
        Column {
            TopToolbar(modifier = Modifier.fillMaxWidth())
            
            Box(modifier = Modifier.weight(1f)) {
                content(false)
            }
            
            BottomToolbar(modifier = Modifier.fillMaxWidth())
        }
    }
}
```

---

## 19. Interaction Patterns

### Drawing Mode Behavior

```kotlin
sealed class InteractionMode {
    object Drawing : InteractionMode()           // Default: touch draws
    object Erasing : InteractionMode()           // Touch erases
    object Selecting : InteractionMode()         // Touch creates selection
    object Transforming : InteractionMode()      // Manipulate transform
    object ColorPicking : InteractionMode()      // Touch samples color
    object Panning : InteractionMode()           // Two-finger pan active
}

class InteractionManager {
    
    var currentMode: InteractionMode = InteractionMode.Drawing
        private set
    
    fun handleTouch(event: MotionEvent): Boolean {
        return when (currentMode) {
            InteractionMode.Drawing -> handleDrawing(event)
            InteractionMode.Erasing -> handleErasing(event)
            InteractionMode.Selecting -> handleSelecting(event)
            InteractionMode.Transforming -> handleTransform(event)
            InteractionMode.ColorPicking -> handleColorPick(event)
            InteractionMode.Panning -> handlePanning(event)
        }
    }
    
    fun switchMode(newMode: InteractionMode) {
        // Clean up previous mode
        when (currentMode) {
            InteractionMode.Drawing -> cancelCurrentStroke()
            InteractionMode.Selecting -> commitOrCancelSelection()
            InteractionMode.Transforming -> commitOrCancelTransform()
            else -> {}
        }
        
        currentMode = newMode
        
        // Provide feedback
        haptics.toolSelected()
    }
}
```

---

## 20. Loading States & Progress

### Project Load Progress

```kotlin
@Composable
fun LoadingOverlay(
    progress: Float,  // 0-1
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    message,
                    style = MaterialTheme.typography.bodyLarge
                )
                
                if (progress > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
```

---

## 21. Export Dialog

### Format Selection Interface

```kotlin
@Composable
fun ExportDialog(
    projectName: String,
    onExport: (ExportOptions) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.PNG) }
    var includeBackground by remember { mutableStateOf(true) }
    var quality by remember { mutableStateOf(100) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Image") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Format selection
                Text("Format", style = MaterialTheme.typography.titleSmall)
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FormatChip(
                        format = ExportFormat.PNG,
                        selected = selectedFormat == ExportFormat.PNG,
                        onClick = { selectedFormat = ExportFormat.PNG }
                    )
                    FormatChip(
                        format = ExportFormat.JPEG,
                        selected = selectedFormat == ExportFormat.JPEG,
                        onClick = { selectedFormat = ExportFormat.JPEG }
                    )
                    FormatChip(
                        format = ExportFormat.PSD,
                        selected = selectedFormat == ExportFormat.PSD,
                        onClick = { selectedFormat = ExportFormat.PSD }
                    )
                }
                
                // Options
                if (selectedFormat == ExportFormat.PNG) {
                    SwitchSettingItem(
                        title = "Include Background",
                        checked = includeBackground,
                        onCheckedChange = { includeBackground = it }
                    )
                }
                
                if (selectedFormat == ExportFormat.JPEG) {
                    Column {
                        Text("Quality: $quality%")
                        Slider(
                            value = quality.toFloat(),
                            onValueChange = { quality = it.toInt() },
                            valueRange = 1f..100f
                        )
                    }
                }
                
                // Preview size
                Text(
                    "Output: 2048×2048 (~8MB)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onExport(
                        ExportOptions(
                            format = selectedFormat,
                            includeBackground = includeBackground,
                            quality = quality,
                            filename = "$projectName.${selectedFormat.extension}"
                        )
                    )
                    onDismiss()
                }
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

data class ExportOptions(
    val format: ExportFormat,
    val includeBackground: Boolean,
    val quality: Int,
    val filename: String
)

enum class ExportFormat(val extension: String) {
    PNG("png"),
    JPEG("jpg"),
    PSD("psd"),
    WEBP("webp")
}
```

---

## 22. Palm Rejection

### Touch Discrimination

```kotlin
class PalmRejectionFilter {
    
    fun filterTouchEvent(event: MotionEvent): MotionEvent? {
        val pointerCount = event.pointerCount
        
        if (pointerCount == 1) {
            // Single touch - check if it's a stylus
            val toolType = event.getToolType(0)
            
            if (toolType == MotionEvent.TOOL_TYPE_STYLUS) {
                return event  // Always accept stylus
            }
            
            // Check touch size for palm detection
            val touchMajor = event.getTouchMajor(0)
            val touchMinor = event.getTouchMinor(0)
            
            // Large touch area = likely palm
            if (touchMajor > 50f || touchMinor > 50f) {
                return null  // Reject palm
            }
            
            return event  // Accept finger
        }
        
        if (pointerCount > 1) {
            // Multi-touch - check if any is a stylus
            for (i in 0 until pointerCount) {
                if (event.getToolType(i) == MotionEvent.TOOL_TYPE_STYLUS) {
                    // Stylus present - reject all finger touches
                    return event.filterToStylusOnly()
                }
            }
            
            // All fingers - could be gesture, accept
            return event
        }
        
        return event
    }
}
```

---

## 23. Performance Indicators

### FPS Counter (Debug Mode)

```kotlin
@Composable
fun PerformanceOverlay(
    fps: Float,
    memoryMB: Int,
    layerCount: Int,
    visible: Boolean
) {
    if (!visible) return
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopEnd
    ) {
        Surface(
            modifier = Modifier.padding(16.dp),
            color = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    "${fps.toInt()} FPS",
                    color = if (fps >= 60) Color.Green else Color.Yellow,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                
                Text(
                    "${memoryMB}MB",
                    color = if (memoryMB < 500) Color.Green else Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                
                Text(
                    "$layerCount layers",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
```

---

## 24. UI Implementation Roadmap

### Phase 1: Core Interface (Current → +1 month)

**Current Status**:
- [x] Basic toolbar
- [x] Color picker (simple grid)
- [x] Brush selector (basic)
- [x] Layer panel (minimal)

**Add**:
- [ ] QuickMenu (radial menu)
- [ ] Two-finger gestures (undo, zoom, pan)
- [ ] Canvas rotation
- [ ] Size/opacity sliders (side HUD)
- [ ] Gallery view
- [ ] Settings screen

### Phase 2: Professional Tools (+2-3 months)

- [ ] Disc color picker (HSV wheel)
- [ ] Eyedropper with magnifier
- [ ] Advanced layer panel (thumbnails, blend modes)
- [ ] Brush library browser
- [ ] Three-finger gestures
- [ ] QuickMenu customization
- [ ] Export dialog with options

### Phase 3: Polish (+3-4 months)

- [ ] Onboarding experience
- [ ] Haptic feedback throughout
- [ ] Smooth animations
- [ ] Split-screen support
- [ ] Landscape/portrait optimization
- [ ] Performance monitoring
- [ ] Gesture customization

---

## 25. Critical UX Patterns

### Immediate Feedback

| Action | Visual Feedback | Haptic | Audio |
|--------|----------------|--------|-------|
| Tool switch | Highlight tool | Light click | Soft beep |
| Layer switch | Highlight layer | Tick | None |
| Undo/Redo | Toast + canvas update | Medium click | None |
| Brush size change | Preview circle | None | None |
| Color change | Swatch update | None | None |
| Save complete | Snackbar | Success | Chime |
| Error | Red snackbar | Double tap | Error sound |

### Progressive Disclosure

1. **Default View**: Canvas + minimal toolbar
2. **One Tap**: Tool panels (brushes, colors, layers)
3. **Two Taps**: Advanced options (brush customization, layer properties)
4. **Gestures**: Shortcuts for common actions

**Never More Than 2 Taps** to any feature.

---

## Summary

This specification provides:
- ✅ Complete screen layouts for all modes
- ✅ Gesture system implementation
- ✅ QuickMenu design and code
- ✅ Color picker interfaces (disc + grid)
- ✅ Brush library browser
- ✅ Layer panel with full controls
- ✅ Gallery view for project management
- ✅ Settings screen
- ✅ Onboarding experience
- ✅ Dark theme specification
- ✅ Palm rejection
- ✅ Accessibility features
- ✅ Performance indicators
- ✅ Export dialog

**Current Implementation Status**:
- [x] Basic toolbar (done)
- [x] Simple color picker (done)
- [x] Basic brush selector (done)
- [x] Minimal layer panel (done)
- [ ] Gestures (specified, not implemented)
- [ ] QuickMenu (specified, not implemented)
- [ ] Disc color picker (specified, not implemented)
- [ ] Gallery (specified, not implemented)
- [ ] Settings (specified, not implemented)

**Next Steps**:
1. Implement two-finger gestures (immediate UX improvement)
2. Add disc color picker (professional feel)
3. Improve layer panel with thumbnails
4. Add size/opacity HUD sliders
5. Build gallery view for project management

This gives your daughter an intuitive, professional interface matching Procreate's UX excellence!
