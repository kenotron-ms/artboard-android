# Artboard Animation System

Centralized animation system for smooth 60 FPS transitions throughout the app.

## 📁 File Structure

```
animation/
├── AnimationSpecs.kt              # Centralized timing/easing definitions
├── ButtonAnimations.kt            # Button press animations with spring physics
├── PanelAnimations.kt             # Panel slide+fade transitions
├── DialogAnimations.kt            # Dialog scale+fade animations
├── ListAnimations.kt              # Layer/brush card reorder animations
├── FeedbackAnimations.kt          # Undo/redo visual feedback
├── LoadingAnimations.kt           # Spinners and progress indicators
├── SharedElementTransitions.kt    # Gallery↔Canvas shared element transitions
├── AnimationExtensions.kt         # Reusable Modifier extensions
└── README.md                      # This file
```

## 🎯 Design Principles

1. **Purposeful** - Every animation communicates state or provides feedback
2. **Consistent** - Similar actions use similar timing/easing
3. **Natural** - Motion feels organic, not mechanical
4. **Fast** - Never slow down workflow (200-400ms max)
5. **Respectful** - 60 FPS always, respects reduced motion settings

## ⏱️ Animation Timings

### Duration Constants

```kotlin
ArtboardAnimations.Duration.INSTANT    // 0ms   - No animation
ArtboardAnimations.Duration.MICRO      // 100ms - Very quick feedback
ArtboardAnimations.Duration.QUICK      // 150ms - Button press, ripple
ArtboardAnimations.Duration.FAST       // 200ms - Toggle, quick state changes
ArtboardAnimations.Duration.NORMAL     // 300ms - Default for most UI
ArtboardAnimations.Duration.SLOW       // 400ms - Emphasized actions
ArtboardAnimations.Duration.DELIBERATE // 600ms - Important state changes
```

### Easing Curves

```kotlin
ArtboardAnimations.Easing.FAST_OUT_SLOW_IN  // Most natural (Material Design standard)
ArtboardAnimations.Easing.LINEAR_OUT_SLOW_IN // Good for exits
ArtboardAnimations.Easing.EASE_OUT          // Appearing elements
ArtboardAnimations.Easing.EASE_IN           // Disappearing elements
ArtboardAnimations.Easing.LINEAR            // Progress indicators
```

### Spring Physics

```kotlin
ArtboardAnimations.Springs.BOUNCY  // Playful button presses (dampingRatio ~0.5-0.6)
ArtboardAnimations.Springs.SMOOTH  // Cards and panels (dampingRatio ~0.75)
ArtboardAnimations.Springs.GENTLE  // List reordering (dampingRatio ~0.8)
ArtboardAnimations.Springs.STIFF   // Precise movements
```

## 🔘 Button Animations

### AnimatedButton

Standard button with press scale effect:

```kotlin
AnimatedButton(
    onClick = { /* action */ },
    enabled = true,
    color = Color(0xFF4A90E2)
) {
    Icon(Icons.Default.Brush, contentDescription = "Paint")
    Spacer(modifier = Modifier.width(8.dp))
    Text("Paint")
}
```

### AnimatedIconButton

Circular icon button with scale effect:

```kotlin
AnimatedIconButton(
    onClick = { /* action */ },
    size = 48.dp
) {
    Icon(Icons.Default.Undo, contentDescription = "Undo")
}
```

### AnimatedToggleButton

Button with selected state:

```kotlin
AnimatedToggleButton(
    selected = isSelected,
    onToggle = { isSelected = !isSelected },
    selectedColor = Color(0xFF4A90E2)
) {
    Text("Brush")
}
```

## 📱 Panel Animations

### AnimatedBottomSheet

Slides in from bottom:

```kotlin
AnimatedBottomSheet(
    isVisible = isPanelVisible,
    onDismiss = { isPanelVisible = false }
) {
    // Panel content
    Text("Layer Settings")
}
```

### AnimatedSidePanel

Slides in from left or right:

```kotlin
AnimatedSidePanel(
    isVisible = isSidePanelVisible,
    onDismiss = { isSidePanelVisible = false },
    fromLeft = true
) {
    // Panel content
    Text("Settings")
}
```

### AnimatedToolbar

Auto-hiding toolbar:

```kotlin
AnimatedToolbar(
    isVisible = isToolbarVisible
) {
    AnimatedIconButton(onClick = { /* undo */ }) {
        Icon(Icons.Default.Undo, contentDescription = "Undo")
    }
    AnimatedIconButton(onClick = { /* redo */ }) {
        Icon(Icons.Default.Redo, contentDescription = "Redo")
    }
}
```

## 💬 Dialog Animations

### AnimatedDialog

Standard dialog with scale+fade:

```kotlin
AnimatedDialog(
    isVisible = isDialogVisible,
    onDismiss = { isDialogVisible = false }
) {
    Text("Dialog Title", style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(16.dp))
    Text("Dialog content goes here")
}
```

### AnimatedConfirmationDialog

Pre-styled confirmation dialog:

```kotlin
AnimatedConfirmationDialog(
    isVisible = showConfirmation,
    onDismiss = { showConfirmation = false },
    onConfirm = { /* delete layer */ },
    confirmText = "Delete",
    cancelText = "Cancel"
) {
    Text("Delete this layer?", style = MaterialTheme.typography.titleMedium)
    Text("This action cannot be undone.")
}
```

## 📋 List Animations

### AnimatedLayerCard

Reorderable layer card:

```kotlin
AnimatedLayerCard(
    isSelected = layer.isSelected,
    isDragging = isDragging,
    isVisible = layer.isVisible
) {
    Row {
        Text(layer.name)
        Spacer(modifier = Modifier.weight(1f))
        Text("${layer.opacity}%")
    }
}
```

### AnimatedColorSwatch

Color selection with scale animation:

```kotlin
AnimatedColorSwatch(
    color = Color.Red,
    isSelected = currentColor == Color.Red
) {
    if (isSelected) {
        Icon(Icons.Default.Check, contentDescription = "Selected")
    }
}
```

## ✨ Feedback Animations

### UndoRedoFeedback

Visual confirmation for undo/redo:

```kotlin
var undoTrigger by remember { mutableStateOf<UndoRedoTrigger?>(null) }

UndoRedoFeedback(trigger = undoTrigger)

// Trigger undo feedback
Button(onClick = {
    performUndo()
    undoTrigger = UndoRedoTrigger.UNDO
}) {
    Text("Undo")
}
```

### SaveSuccessFeedback

Success notification:

```kotlin
SaveSuccessFeedback(
    isVisible = showSaveSuccess,
    message = "Saved successfully",
    icon = Icons.Default.CheckCircle
)
```

### ToastNotification

Generic toast notification:

```kotlin
ToastNotification(
    isVisible = showToast,
    message = "Layer merged",
    icon = Icons.Default.Layers
)
```

## ⏳ Loading Animations

### ArtboardLoadingSpinner

Themed loading spinner:

```kotlin
ArtboardLoadingSpinner(
    size = 48.dp,
    color = Color(0xFF4A90E2)
)
```

### AnimatedLinearProgress

Progress bar with percentage:

```kotlin
AnimatedLinearProgress(
    progress = exportProgress,
    showPercentage = true
)
```

### CanvasLoadingIndicator

Combined progress indicator:

```kotlin
CanvasLoadingIndicator(
    progress = renderProgress,
    status = "Rendering canvas..."
)
```

## 🔄 Modifier Extensions

Convenient animation extensions:

```kotlin
// Press scale
Box(modifier = Modifier.pressScale(pressed = isPressed))

// Selection scale
Card(modifier = Modifier.selectionScale(selected = isSelected))

// Pulse animation
Icon(modifier = Modifier.pulse(enabled = isImportant))

// Shake animation (e.g., for errors)
TextField(modifier = Modifier.shake(trigger = errorCount))

// Rotating spinner
Icon(modifier = Modifier.rotating(enabled = isLoading))

// Fade in/out
Text(modifier = Modifier.animatedVisibility(visible = isVisible))

// Pop-in effect
Dialog(modifier = Modifier.popIn(visible = isDialogVisible))

// Drag scale
Card(modifier = Modifier.dragScale(isDragging = isDragging))

// Glow effect
Icon(modifier = Modifier.glow(enabled = isActive))

// Bounce animation
Badge(modifier = Modifier.bounce(enabled = hasNotification))
```

## ♿ Accessibility

### Reduced Motion Support

All animations automatically respect the system's reduced motion setting:

```kotlin
@Composable
fun MyAnimatedComponent() {
    val reducedMotion = rememberReducedMotion()
    
    if (reducedMotion) {
        // Simplified or no animation
        if (isVisible) {
            Content()
        }
    } else {
        // Full animation
        AnimatedVisibility(visible = isVisible) {
            Content()
        }
    }
}
```

## 🎬 Screen Transitions

### Standard Screen Transitions

For navigation between screens:

```kotlin
composable(
    route = "canvas",
    enterTransition = { ScreenTransitions.enterTransition() },
    exitTransition = { ScreenTransitions.exitTransition() },
    popEnterTransition = { ScreenTransitions.popEnterTransition() },
    popExitTransition = { ScreenTransitions.popExitTransition() }
) {
    CanvasScreen()
}
```

### Horizontal Slide Transitions

For tabbed navigation:

```kotlin
composable(
    route = "settings",
    enterTransition = { HorizontalSlideTransitions.enterTransition() },
    exitTransition = { HorizontalSlideTransitions.exitTransition() },
    popEnterTransition = { HorizontalSlideTransitions.popEnterTransition() },
    popExitTransition = { HorizontalSlideTransitions.popExitTransition() }
) {
    SettingsScreen()
}
```

## 📊 Performance Requirements

- **60 FPS minimum** for all animations
- **GPU-accelerated** properties (scale, translation, rotation, alpha)
- **<5 MB** memory for animation system
- **<10% CPU** during animations

### GPU-Accelerated Properties (Fast)

✅ `alpha`, `scaleX/Y`, `translationX/Y`, `rotationX/Y/Z`

### CPU-Bound Properties (Avoid)

❌ `width/height` (triggers layout), `padding/margin` (triggers layout)

## 🧪 Testing

Tests are located in:
- `AnimationSpecsTest.kt` - Duration, easing, spring tests
- `AnimationExtensionsTest.kt` - Extension function tests

Run tests:
```bash
./gradlew test
```

## 📝 Usage Examples

### Complete Example: Animated Layer Panel

```kotlin
@Composable
fun LayerPanel(
    layers: List<Layer>,
    onLayerClick: (Layer) -> Unit,
    onDismiss: () -> Unit
) {
    var isPanelVisible by remember { mutableStateOf(true) }
    
    AnimatedBottomSheet(
        isVisible = isPanelVisible,
        onDismiss = onDismiss
    ) {
        Text(
            text = "Layers",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
        
        LazyColumn {
            items(layers) { layer ->
                AnimatedLayerCard(
                    isSelected = layer.isSelected,
                    isVisible = layer.isVisible,
                    modifier = Modifier
                        .animatedItemPlacement()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(layer.name)
                        Text("${layer.opacity}%")
                    }
                }
            }
        }
        
        AnimatedButton(
            onClick = { /* add layer */ },
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("New Layer")
        }
    }
}
```

## 🎨 Animation Timing Reference

| Element | Action | Duration | Easing | Scale |
|---------|--------|----------|--------|-------|
| Button | Press | 150ms | Spring bouncy | 0.95 |
| Icon Button | Press | 150ms | Spring smooth | 0.90 |
| Panel | Slide in | 300ms | Fast out slow in | - |
| Dialog | Appear | 300ms | Fast out slow in | 0.80→1.0 |
| Card | Select | 200ms | Spring smooth | 1.05 |
| Toast | Slide in | 300ms | Fast out slow in | - |
| Loading | Rotate | 1000ms | Linear | - |
| Undo | Flash | 300ms | Scale in/out | 0.5→1.5 |

## 🚀 Quick Start

1. **Import the animation package:**
```kotlin
import com.artboard.ui.animation.*
```

2. **Use pre-built animated components:**
```kotlin
AnimatedButton(onClick = { }) {
    Text("Click Me")
}
```

3. **Or apply animation modifiers:**
```kotlin
Box(modifier = Modifier.pressScale(pressed = isPressed))
```

4. **Check reduced motion:**
```kotlin
val reducedMotion = rememberReducedMotion()
```

## ✅ Success Criteria

- [x] All animations run at 60 FPS
- [x] Timing matches specifications (150-400ms)
- [x] Spring physics for organic feel
- [x] Reduced motion support
- [x] Consistent across all features
- [x] GPU-accelerated transforms
- [x] No animations block user input

## 📚 Additional Resources

- **Specification**: `feature-specs/phase3-polish/ANIMATION_SYSTEM.md`
- **Timing Reference**: `specs/interactions/AnimationTimings.md`
- **Tests**: `app/src/test/kotlin/com/artboard/ui/animation/`

---

**Built with ❤️ for 60 FPS smooth animations**
