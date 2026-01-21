# Artboard - Android Digital Art Application Architecture

## Overview

A professional-grade digital art application for Android with zero compromises on performance or user experience. Built to compete with Procreate.

## Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose + Custom Views
- **Min SDK**: 29 (Android 10)
- **Target SDK**: 34
- **Build**: Gradle with Kotlin DSL

## Architecture Pattern: Clean Architecture with MVVM

```
┌─────────────────────────────────────────────────────────┐
│                    UI Layer (Compose)                    │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │ MainActivity│  │ ToolbarScreen│  │ LayerPanelUI  │  │
│  └─────────────┘  └──────────────┘  └───────────────┘  │
│         │                  │                  │          │
│         └──────────────────┴──────────────────┘          │
│                           │                              │
├───────────────────────────┼──────────────────────────────┤
│                    ViewModel Layer                       │
│                  ┌─────────────────┐                     │
│                  │  CanvasViewModel │                    │
│                  └─────────────────┘                     │
│                           │                              │
├───────────────────────────┼──────────────────────────────┤
│                     Domain Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ DrawingEngine│  │  BrushEngine │  │ LayerManager │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
├──────────────────────────────────────────────────────────┤
│                   Data Layer                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ProjectRepo   │  │ FileStorage  │  │   Database   │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└──────────────────────────────────────────────────────────┘
```

## Module Structure

```
app/
├── src/main/
│   ├── kotlin/com/artboard/
│   │   ├── MainActivity.kt
│   │   │
│   │   ├── ui/
│   │   │   ├── canvas/
│   │   │   │   ├── CanvasView.kt          # Custom View for drawing
│   │   │   │   ├── CanvasViewModel.kt
│   │   │   │   └── TouchEventProcessor.kt
│   │   │   ├── toolbar/
│   │   │   │   ├── ToolbarScreen.kt
│   │   │   │   ├── ColorPicker.kt
│   │   │   │   └── BrushSelector.kt
│   │   │   ├── layers/
│   │   │   │   ├── LayerPanel.kt
│   │   │   │   └── LayerItem.kt
│   │   │   └── theme/
│   │   │       ├── Theme.kt
│   │   │       └── Color.kt
│   │   │
│   │   ├── domain/
│   │   │   ├── engine/
│   │   │   │   ├── DrawingEngine.kt       # Core drawing loop
│   │   │   │   ├── StrokeRenderer.kt      # Renders strokes to bitmap
│   │   │   │   └── Compositor.kt          # Layers → final image
│   │   │   ├── brush/
│   │   │   │   ├── BrushEngine.kt
│   │   │   │   ├── Brush.kt               # Brush data class
│   │   │   │   ├── BrushPresets.kt
│   │   │   │   └── StrokeInterpolator.kt  # Bezier smoothing
│   │   │   ├── layer/
│   │   │   │   ├── LayerManager.kt
│   │   │   │   ├── Layer.kt
│   │   │   │   └── BlendMode.kt
│   │   │   └── history/
│   │   │       ├── HistoryManager.kt      # Undo/redo
│   │   │       └── Command.kt
│   │   │
│   │   ├── data/
│   │   │   ├── model/
│   │   │   │   ├── Project.kt
│   │   │   │   ├── Stroke.kt
│   │   │   │   └── Point.kt
│   │   │   ├── repository/
│   │   │   │   └── ProjectRepository.kt
│   │   │   ├── storage/
│   │   │   │   ├── FileStorage.kt         # Save/load bitmaps
│   │   │   │   └── ProjectSerializer.kt   # JSON serialization
│   │   │   └── database/
│   │   │       ├── AppDatabase.kt
│   │   │       ├── ProjectDao.kt
│   │   │       └── ProjectEntity.kt
│   │   │
│   │   └── util/
│   │       ├── BitmapPool.kt              # Memory management
│   │       ├── Performance.kt             # FPS monitoring
│   │       └── Extensions.kt
│   │
│   └── res/
│       ├── values/
│       │   ├── colors.xml
│       │   └── strings.xml
│       └── drawable/
│           └── [icons]
└── build.gradle.kts
```

## Core Components

### 1. CanvasView (Custom View)

**Responsibility**: Low-latency touch input and rendering

```kotlin
class CanvasView : View {
    - Handles MotionEvent (touch/stylus)
    - Maintains current stroke buffer
    - Renders layers + current stroke
    - 60+ fps rendering loop
}
```

**Key Algorithms**:
- Touch event batching and interpolation
- Stroke point generation with pressure
- Dirty region tracking for partial redraws
- Hardware-accelerated Canvas rendering

### 2. DrawingEngine

**Responsibility**: Core drawing logic

```kotlin
class DrawingEngine {
    fun beginStroke(x, y, pressure, brush)
    fun continueStroke(x, y, pressure)
    fun endStroke(): Stroke
    fun renderStroke(stroke, targetBitmap)
}
```

**Threading**: Runs on dedicated render thread

### 3. BrushEngine

**Responsibility**: Brush behavior and stroke rendering

```kotlin
data class Brush(
    val size: Float,           // 1-500px
    val opacity: Float,        // 0.0-1.0
    val hardness: Float,       // 0.0-1.0 (edge softness)
    val flow: Float,           // 0.0-1.0 (paint buildup)
    val spacing: Float,        // 0.01-1.0 (stamp spacing)
    val pressureSizeEnabled: Boolean,
    val pressureOpacityEnabled: Boolean,
    val type: BrushType        // PENCIL, PEN, AIRBRUSH, ERASER
)
```

**Rendering Strategy**: Stamp-based (paint circles along stroke path)

### 4. LayerManager

**Responsibility**: Layer operations and compositing

```kotlin
class LayerManager {
    val layers: List<Layer>
    var activeLayerIndex: Int
    
    fun addLayer()
    fun deleteLayer(index)
    fun moveLayer(from, to)
    fun mergeLayers(indices)
    fun composite(): Bitmap  // All layers → final image
}
```

**Blend Modes**: Normal, Multiply, Screen, Overlay, Add

### 5. HistoryManager

**Responsibility**: Undo/redo with command pattern

```kotlin
sealed class Command {
    abstract fun execute()
    abstract fun undo()
}

class AddStrokeCommand(stroke, layerIndex) : Command
class DeleteLayerCommand(layer, index) : Command
// etc.
```

**Memory Strategy**: Keep last 100 commands, prune older

## Data Models

### Point
```kotlin
data class Point(
    val x: Float,
    val y: Float,
    val pressure: Float,      // 0.0-1.0
    val timestamp: Long       // For velocity calculation
)
```

### Stroke
```kotlin
data class Stroke(
    val id: String,
    val points: List<Point>,
    val brush: Brush,
    val color: Int            // ARGB color
)
```

### Layer
```kotlin
data class Layer(
    val id: String,
    val name: String,
    val bitmap: Bitmap,       // The actual pixels
    val opacity: Float,       // 0.0-1.0
    val blendMode: BlendMode,
    val isVisible: Boolean
)
```

### Project
```kotlin
data class Project(
    val id: String,
    val name: String,
    val width: Int,           // Canvas width in pixels
    val height: Int,          // Canvas height in pixels
    val layers: List<Layer>,
    val createdAt: Long,
    val modifiedAt: Long
)
```

## Performance Optimizations

### 1. Stroke Interpolation
- Use Catmull-Rom splines for smooth curves
- Downsample high-frequency touch events
- Predictive tracking for lower latency

### 2. Memory Management
- BitmapPool for layer bitmap reuse
- Tile-based rendering for large canvases
- Aggressive bitmap recycling
- WeakReference caching

### 3. Threading
```
UI Thread:
  - Touch events
  - View updates
  - User interactions

Render Thread:
  - Stroke rendering
  - Layer compositing
  - Bitmap operations

Background Thread:
  - File I/O
  - Project save/load
  - Export operations
```

### 4. Rendering Pipeline
```
MotionEvent → TouchEventProcessor → DrawingEngine
                                           ↓
                                    StrokeRenderer
                                           ↓
                                   Layer.bitmap (update)
                                           ↓
                                      Compositor
                                           ↓
                                   CanvasView.draw()
```

## File Format

Projects saved as:
```
/sdcard/Android/data/com.artboard/files/projects/
├── project_id/
│   ├── project.json          # Metadata
│   ├── layer_0.png           # Layer bitmaps
│   ├── layer_1.png
│   └── thumbnail.jpg         # Gallery preview
```

**project.json**:
```json
{
  "id": "uuid",
  "name": "My Artwork",
  "width": 2048,
  "height": 2048,
  "layers": [
    {
      "id": "layer-uuid",
      "name": "Background",
      "filename": "layer_0.png",
      "opacity": 1.0,
      "blendMode": "NORMAL",
      "isVisible": true
    }
  ],
  "createdAt": 1234567890,
  "modifiedAt": 1234567890
}
```

## Touch Event Processing

```kotlin
override fun onTouchEvent(event: MotionEvent): Boolean {
    when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
            val pressure = event.getPressure()
            val x = event.x
            val y = event.y
            drawingEngine.beginStroke(x, y, pressure, currentBrush)
        }
        MotionEvent.ACTION_MOVE -> {
            // Handle historical events for smoothness
            for (i in 0 until event.historySize) {
                val x = event.getHistoricalX(i)
                val y = event.getHistoricalY(i)
                val pressure = event.getHistoricalPressure(i)
                drawingEngine.continueStroke(x, y, pressure)
            }
            // Handle current event
            drawingEngine.continueStroke(event.x, event.y, event.pressure)
            invalidate()  // Trigger redraw
        }
        MotionEvent.ACTION_UP -> {
            val stroke = drawingEngine.endStroke()
            historyManager.execute(AddStrokeCommand(stroke, activeLayer))
        }
    }
    return true
}
```

## UI Layout

```
┌────────────────────────────────────────┐
│  [Toolbar: Brush | Color | Size | ... ]│
├────────────────────────────────────────┤
│                                        │
│                                        │
│           Canvas View                  │
│         (Full-screen drawing)          │
│                                        │
│                                        │
├────────────────────────────────────────┤
│  [Layers Panel - Swipe up to reveal]  │
│  ┌──────┐  Background   [👁]  [...]   │
│  │      │  Layer 1      [👁]  [...]   │
│  │      │  Sketch       [👁]  [...]   │
│  └──────┘                              │
└────────────────────────────────────────┘
```

## Success Criteria

1. **Performance**
   - 60 fps drawing (120 fps on capable devices)
   - Touch-to-pixel latency < 50ms
   - No dropped frames during normal use

2. **Functionality**
   - 5+ brush types working smoothly
   - Unlimited layers (memory permitting)
   - Undo/redo 100+ steps
   - Save/load projects without data loss

3. **UX**
   - Intuitive gesture controls
   - Zero ads, zero cruft
   - Professional feel

## Build Configuration

**build.gradle.kts (app)**:
```kotlin
plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
}

android {
    namespace = "com.artboard"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.artboard"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
}

dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

## Next Steps

1. Set up Android project with this structure
2. Implement DrawingEngine + BrushEngine (core functionality)
3. Create CanvasView with touch handling
4. Build LayerManager and compositing
5. Add HistoryManager (undo/redo)
6. Implement UI with Compose
7. Add project persistence
8. Performance optimization pass
