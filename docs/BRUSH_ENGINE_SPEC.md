# Brush Engine Implementation Specification

This document provides complete technical specifications for implementing Artboard's professional brush engine to match Procreate's capabilities.

---

## Architecture Overview

```
Brush Engine Pipeline
├── Input Processing
│   ├── Touch/Stylus Events (x, y, pressure, tilt, azimuth)
│   ├── Stroke Smoothing (StreamLine)
│   └── Point Generation
├── Stroke Dynamics
│   ├── Pressure Curve Application
│   ├── Tilt/Azimuth Processing
│   ├── Velocity Calculation
│   └── Spacing Calculation
├── Stamp Rendering
│   ├── Shape Generation (circle, custom, grain)
│   ├── Dynamics Application (size, opacity, rotation, scatter)
│   ├── Texture Blending (grain system)
│   └── Color Dynamics (jitter)
└── Compositing
    ├── Blend Mode Application
    ├── Layer Update
    └── Display Update (60-120 FPS)
```

---

## 1. Brush Data Model

### Complete Brush Class

```kotlin
data class Brush(
    // Basic Properties
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled Brush",
    val category: BrushCategory = BrushCategory.DRAWING,
    
    // Stroke Path
    val spacing: Float = 10f,              // 0-300% of brush size
    val streamLine: Float = 0f,            // 0-100% stabilization
    val jitter: Float = 0f,                // 0-100% position randomness
    val fallOff: Float = 0f,               // 0-100% end tapering
    
    // Taper
    val pressureTaper: Boolean = true,
    val tipTaper: Float = 0f,              // 0-100% start fade
    val endTaper: Float = 0f,              // 0-100% end fade
    val classicTaper: Boolean = false,
    
    // Shape
    val shapeSource: BrushShape = BrushShape.Circle,
    val shapeImage: Bitmap? = null,        // Custom shape
    val scatter: Float = 0f,               // 0-100% perpendicular offset
    val rotation: Float = 0f,              // 0-360° base rotation
    val rotationRandomized: Boolean = false,
    val azimuthRotation: Boolean = false,  // Follow stylus angle
    val stampCount: Int = 1,               // 1-16 stamps per point
    
    // Grain
    val grainSource: Bitmap? = null,
    val grainScale: Float = 100f,          // 1-300%
    val grainZoom: Float = 100f,           // 1-200%
    val grainBlendMode: GrainBlendMode = GrainBlendMode.MULTIPLY,
    val grainDepth: Float = 50f,           // 0-100% intensity
    val grainMovement: GrainMovement = GrainMovement.MOVING,
    val grainFiltering: Boolean = true,
    
    // Rendering
    val renderingMode: RenderingMode = RenderingMode.UNIFORM,
    val opacity: Float = 100f,             // 0-100% base opacity
    val flow: Float = 100f,                // 0-100% paint buildup
    val blendMode: BlendMode = BlendMode.NORMAL,
    
    // Wet Mix (Advanced)
    val wetMixEnabled: Boolean = false,
    val dilution: Float = 0f,              // 0-100%
    val charge: Float = 50f,               // 0-100%
    val attack: Float = 50f,               // 0-100%
    val pull: Float = 0f,                  // 0-100%
    val gradient: Boolean = false,
    
    // Color Dynamics
    val hueJitter: Float = 0f,             // 0-100%
    val saturationJitter: Float = 0f,      // 0-100%
    val brightnessJitter: Float = 0f,      // 0-100%
    val colorPressure: Boolean = false,
    val stampColorJitter: Boolean = false,
    
    // Apple Pencil / Stylus
    val pressureCurve: PressureCurve = PressureCurve.linear(),
    val tiltSensitivity: Float = 0f,      // 0-100%
    val opacityCurve: PressureCurve = PressureCurve.linear(),
    val sizeCurve: PressureCurve = PressureCurve.linear(),
    val bleed: Float = 0f,                 // 0-100% size increase
    val minSize: Float = 0f,               // 0-100% minimum size
    val minOpacity: Float = 0f,            // 0-100% minimum opacity
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val author: String = "User",
    val isCustom: Boolean = false
)

enum class BrushCategory {
    SKETCHING,
    INKING,
    DRAWING,
    PAINTING,
    ARTISTIC,
    CALLIGRAPHY,
    AIRBRUSHING,
    TEXTURES,
    LUMINANCE,
    INDUSTRIAL,
    ORGANIC,
    TOUCHUPS
}

enum class BrushShape {
    CIRCLE,
    SQUARE,
    CUSTOM_IMAGE
}

enum class GrainBlendMode {
    MULTIPLY,
    SCREEN,
    OVERLAY,
    ADD,
    SUBTRACT,
    DIFFERENCE,
    LINEAR_BURN,
    LINEAR_DODGE
}

enum class GrainMovement {
    MOVING,        // Texture moves with stroke
    TEXTURIZED     // Texture fixed to canvas
}

enum class RenderingMode {
    LIGHT_GLAZE,      // Minimal buildup
    GLAZED,           // Light buildup
    UNIFORM,          // Even (default)
    INTENSE_GLAZE,    // Medium buildup
    HEAVY_GLAZE,      // Strong buildup
    INTENSE_BLENDING  // Maximum mixing
}
```

### Pressure Curve

```kotlin
data class PressureCurve(
    val points: List<CurvePoint>  // Control points
) {
    fun evaluate(input: Float): Float {
        // Cubic interpolation through control points
        // Input: raw pressure (0-1)
        // Output: adjusted pressure (0-1)
        
        if (points.isEmpty()) return input
        if (points.size == 1) return points[0].y
        
        // Find surrounding control points
        val clampedInput = input.coerceIn(0f, 1f)
        
        val leftIdx = points.indexOfLast { it.x <= clampedInput }
        val rightIdx = points.indexOfFirst { it.x >= clampedInput }
        
        if (leftIdx < 0) return points.first().y
        if (rightIdx < 0) return points.last().y
        if (leftIdx == rightIdx) return points[leftIdx].y
        
        // Cubic hermite interpolation
        val p0 = points[leftIdx]
        val p1 = points[rightIdx]
        
        val t = (clampedInput - p0.x) / (p1.x - p0.x)
        val t2 = t * t
        val t3 = t2 * t
        
        // Hermite basis functions
        val h00 = 2 * t3 - 3 * t2 + 1
        val h10 = t3 - 2 * t2 + t
        val h01 = -2 * t3 + 3 * t2
        val h11 = t3 - t2
        
        val m0 = p0.tangent
        val m1 = p1.tangent
        
        return (h00 * p0.y + h10 * m0 + h01 * p1.y + h11 * m1).coerceIn(0f, 1f)
    }
    
    companion object {
        fun linear() = PressureCurve(
            listOf(
                CurvePoint(0f, 0f, 1f),
                CurvePoint(1f, 1f, 1f)
            )
        )
        
        fun easeIn() = PressureCurve(
            listOf(
                CurvePoint(0f, 0f, 0f),
                CurvePoint(0.5f, 0.2f, 1f),
                CurvePoint(1f, 1f, 1f)
            )
        )
        
        fun easeOut() = PressureCurve(
            listOf(
                CurvePoint(0f, 0f, 1f),
                CurvePoint(0.5f, 0.8f, 1f),
                CurvePoint(1f, 1f, 0f)
            )
        )
    }
}

data class CurvePoint(
    val x: Float,       // Input (0-1)
    val y: Float,       // Output (0-1)
    val tangent: Float  // Slope at this point
)
```

---

## 2. Stroke Smoothing (StreamLine)

### Algorithm: Exponential Moving Average

```kotlin
class StrokeStabilizer(
    private val streamLineAmount: Float  // 0-100%
) {
    private val pointBuffer = ArrayDeque<Point>(maxSize = 10)
    private var lastOutputPoint: Point? = null
    
    fun addPoint(point: Point): Point {
        if (streamLineAmount <= 0) return point
        
        pointBuffer.addLast(point)
        if (pointBuffer.size > 10) pointBuffer.removeFirst()
        
        // Calculate weighted average
        val weight = streamLineAmount / 100f
        val windowSize = (5 * weight).toInt().coerceIn(1, pointBuffer.size)
        
        val recentPoints = pointBuffer.takeLast(windowSize)
        
        var sumX = 0f
        var sumY = 0f
        var sumPressure = 0f
        var totalWeight = 0f
        
        recentPoints.forEachIndexed { index, p ->
            // More recent points have higher weight
            val w = (index + 1).toFloat()
            sumX += p.x * w
            sumY += p.y * w
            sumPressure += p.pressure * w
            totalWeight += w
        }
        
        val smoothedPoint = Point(
            x = sumX / totalWeight,
            y = sumY / totalWeight,
            pressure = sumPressure / totalWeight,
            timestamp = point.timestamp
        )
        
        lastOutputPoint = smoothedPoint
        return smoothedPoint
    }
    
    fun reset() {
        pointBuffer.clear()
        lastOutputPoint = null
    }
}
```

### Predictive Rendering (Latency Reduction)

```kotlin
class PredictiveRenderer {
    
    private val velocityHistory = ArrayDeque<Vector2>(maxSize = 5)
    
    fun predictNextPoint(
        currentPoint: Point,
        previousPoint: Point
    ): Point {
        // Calculate velocity
        val velocity = Vector2(
            x = currentPoint.x - previousPoint.x,
            y = currentPoint.y - previousPoint.y
        )
        
        velocityHistory.addLast(velocity)
        if (velocityHistory.size > 5) velocityHistory.removeFirst()
        
        // Average velocity
        val avgVelocity = velocityHistory.reduce { acc, v -> 
            Vector2(acc.x + v.x, acc.y + v.y)
        }.scale(1f / velocityHistory.size)
        
        // Predict next point (typically 16ms ahead)
        val predictionTime = 16f  // One frame at 60 FPS
        
        return Point(
            x = currentPoint.x + avgVelocity.x * predictionTime,
            y = currentPoint.y + avgVelocity.y * predictionTime,
            pressure = currentPoint.pressure,
            timestamp = currentPoint.timestamp + 16
        )
    }
}

data class Vector2(val x: Float, val y: Float) {
    fun scale(factor: Float) = Vector2(x * factor, y * factor)
}
```

---

## 3. Advanced Brush Rendering

### Shape System

```kotlin
sealed class BrushStamp {
    data class Circle(val radius: Float) : BrushStamp()
    data class CustomImage(val bitmap: Bitmap) : BrushStamp()
    
    fun render(
        canvas: Canvas,
        x: Float,
        y: Float,
        size: Float,
        rotation: Float,
        color: Int,
        opacity: Float
    ) {
        when (this) {
            is Circle -> renderCircle(canvas, x, y, radius * size, color, opacity)
            is CustomImage -> renderImage(canvas, x, y, size, rotation, color, opacity)
        }
    }
}

class StampRenderer {
    
    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }
    
    private val stampCache = LruCache<String, Bitmap>(50)
    
    fun renderStamp(
        canvas: Canvas,
        point: Point,
        brush: Brush,
        color: Int,
        dynamics: StampDynamics
    ) {
        // Calculate effective properties
        val size = brush.sizeCurve.evaluate(point.pressure) * dynamics.baseSize
        val opacity = brush.opacityCurve.evaluate(point.pressure) * dynamics.baseOpacity
        val rotation = calculateRotation(brush, point, dynamics)
        val scatter = calculateScatter(brush, dynamics)
        
        // Apply scatter offset
        val scatterOffset = Vector2(
            x = scatter * sin(dynamics.scatterAngle),
            y = scatter * cos(dynamics.scatterAngle)
        )
        
        val finalX = point.x + scatterOffset.x
        val finalY = point.y + scatterOffset.y
        
        // Multi-stamp rendering
        for (i in 0 until brush.stampCount) {
            val stampRotation = rotation + (i * 360f / brush.stampCount)
            
            renderSingleStamp(
                canvas,
                finalX,
                finalY,
                size,
                stampRotation,
                color,
                opacity,
                brush
            )
        }
    }
    
    private fun renderSingleStamp(
        canvas: Canvas,
        x: Float,
        y: Float,
        size: Float,
        rotation: Float,
        color: Int,
        opacity: Float,
        brush: Brush
    ) {
        canvas.save()
        
        // Transform to stamp position
        canvas.translate(x, y)
        canvas.rotate(rotation)
        canvas.scale(size, size)
        
        // Render shape
        if (brush.shapeImage != null) {
            renderCustomShape(canvas, brush.shapeImage, color, opacity)
        } else {
            renderCircleShape(canvas, color, opacity, brush.hardness)
        }
        
        // Apply grain if present
        if (brush.grainSource != null) {
            applyGrain(canvas, brush, x, y)
        }
        
        canvas.restore()
    }
    
    private fun renderCircleShape(
        canvas: Canvas,
        color: Int,
        opacity: Float,
        hardness: Float
    ) {
        if (hardness >= 0.99f) {
            // Hard edge - simple circle
            paint.color = color
            paint.alpha = (opacity * 255).toInt()
            paint.shader = null
            canvas.drawCircle(0f, 0f, 1f, paint)
        } else {
            // Soft edge - radial gradient
            val gradient = RadialGradient(
                0f, 0f, 1f,
                intArrayOf(
                    applyAlpha(color, opacity),
                    applyAlpha(color, opacity * hardness),
                    applyAlpha(color, 0f)
                ),
                floatArrayOf(0f, hardness, 1f),
                Shader.TileMode.CLAMP
            )
            
            paint.shader = gradient
            paint.alpha = 255
            canvas.drawCircle(0f, 0f, 1f, paint)
        }
    }
}

data class StampDynamics(
    val baseSize: Float,
    val baseOpacity: Float,
    val scatterAngle: Float,
    val strokeDistance: Float,  // Total distance drawn
    val strokeVelocity: Float   // Current speed
)
```

---

## 4. Grain System Implementation

### Moving Grain (Texture Scrolls with Stroke)

```kotlin
class MovingGrainRenderer {
    
    private var cumulativeDistance = 0f
    
    fun renderGrain(
        canvas: Canvas,
        point: Point,
        brush: Brush,
        distanceSinceLastStamp: Float
    ) {
        val grain = brush.grainSource ?: return
        
        cumulativeDistance += distanceSinceLastStamp
        
        // Calculate UV coordinates based on stroke path
        val grainScale = brush.grainScale / 100f
        val grainZoom = brush.grainZoom / 100f
        
        val u = (cumulativeDistance * grainScale) % grain.width
        val v = 0f  // Or perpendicular offset for variety
        
        // Sample grain texture
        val grainColor = sampleGrainTexture(grain, u, v, grainZoom)
        
        // Blend grain with brush color
        val blended = blendGrain(
            baseColor = getCurrentColor(),
            grainColor = grainColor,
            mode = brush.grainBlendMode,
            depth = brush.grainDepth / 100f
        )
        
        // Render blended result
        paint.color = blended
        canvas.drawCircle(point.x, point.y, brush.size / 2f, paint)
    }
}
```

### Texturized Grain (Fixed Texture)

```kotlin
class TexturizedGrainRenderer {
    
    fun renderGrain(
        canvas: Canvas,
        point: Point,
        brush: Brush,
        canvasOrigin: Vector2  // For fixed positioning
    ) {
        val grain = brush.grainSource ?: return
        
        // Calculate canvas-space UV coordinates
        val grainScale = brush.grainScale / 100f
        val grainZoom = brush.grainZoom / 100f
        
        val u = ((point.x - canvasOrigin.x) * grainScale) % grain.width
        val v = ((point.y - canvasOrigin.y) * grainScale) % grain.height
        
        // Sample and blend (same as moving grain)
        // ...
    }
}
```

### Grain Blending

```kotlin
fun blendGrain(
    baseColor: Int,
    grainColor: Int,
    mode: GrainBlendMode,
    depth: Float
): Int {
    val baseR = Color.red(baseColor) / 255f
    val baseG = Color.green(baseColor) / 255f
    val baseB = Color.blue(baseColor) / 255f
    
    val grainR = Color.red(grainColor) / 255f
    val grainG = Color.green(grainColor) / 255f
    val grainB = Color.blue(grainColor) / 255f
    
    val blendedR = when (mode) {
        GrainBlendMode.MULTIPLY -> baseR * grainR
        GrainBlendMode.SCREEN -> 1f - (1f - baseR) * (1f - grainR)
        GrainBlendMode.OVERLAY -> if (baseR < 0.5f) {
            2f * baseR * grainR
        } else {
            1f - 2f * (1f - baseR) * (1f - grainR)
        }
        // ... other modes
        else -> baseR
    }
    
    val blendedG = /* same for green */
    val blendedB = /* same for blue */
    
    // Apply depth (mix between base and blended)
    val finalR = baseR + (blendedR - baseR) * depth
    val finalG = baseG + (blendedG - baseG) * depth
    val finalB = baseB + (blendedB - baseB) * depth
    
    return Color.rgb(
        (finalR * 255).toInt(),
        (finalG * 255).toInt(),
        (finalB * 255).toInt()
    )
}
```

---

## 5. Wet Mix System (Advanced)

### Paint Mixing Simulation

```kotlin
class WetMixEngine {
    
    private val canvasColorCache = mutableMapOf<Pair<Int, Int>, Int>()
    
    fun renderWetMixStroke(
        canvas: Canvas,
        point: Point,
        brush: Brush,
        brushColor: Int,
        targetBitmap: Bitmap
    ) {
        val size = brush.sizeCurve.evaluate(point.pressure) * brush.size
        val opacity = brush.opacityCurve.evaluate(point.pressure) * (brush.opacity / 100f)
        
        // Sample canvas color at point
        val canvasColor = sampleCanvasColor(targetBitmap, point.x.toInt(), point.y.toInt())
        
        // Mix colors based on wet mix parameters
        val mixedColor = mixWetPaint(
            canvasColor = canvasColor,
            brushColor = brushColor,
            dilution = brush.dilution / 100f,
            charge = brush.charge / 100f,
            pull = brush.pull / 100f,
            opacity = opacity
        )
        
        // Render with mixed color
        paint.color = mixedColor
        paint.alpha = (opacity * 255 * (brush.flow / 100f)).toInt()
        
        canvas.drawCircle(point.x, point.y, size / 2f, paint)
        
        // Cache for gradient feature
        if (brush.gradient) {
            canvasColorCache[Pair(point.x.toInt(), point.y.toInt())] = mixedColor
        }
    }
    
    private fun mixWetPaint(
        canvasColor: Int,
        brushColor: Int,
        dilution: Float,
        charge: Float,
        pull: Float,
        opacity: Float
    ): Int {
        // Dilution: How transparent/watery the paint is
        val effectiveOpacity = opacity * (1f - dilution * 0.5f)
        
        // Pull: How much canvas color mixes into brush
        val pullAmount = pull * effectiveOpacity
        
        val brushR = Color.red(brushColor) / 255f
        val brushG = Color.green(brushColor) / 255f
        val brushB = Color.blue(brushColor) / 255f
        
        val canvasR = Color.red(canvasColor) / 255f
        val canvasG = Color.green(canvasColor) / 255f
        val canvasB = Color.blue(canvasColor) / 255f
        
        // Mix colors
        val mixedR = brushR * (1f - pullAmount) + canvasR * pullAmount
        val mixedG = brushG * (1f - pullAmount) + canvasG * pullAmount
        val mixedB = brushB * (1f - pullAmount) + canvasB * pullAmount
        
        // Charge affects saturation (more paint = more saturated)
        val chargeFactor = 0.5f + charge * 0.5f
        
        return Color.rgb(
            (mixedR * chargeFactor * 255).toInt(),
            (mixedG * chargeFactor * 255).toInt(),
            (mixedB * chargeFactor * 255).toInt()
        )
    }
}
```

---

## 6. Color Dynamics

### Hue/Saturation/Brightness Jitter

```kotlin
class ColorDynamicsEngine {
    
    private val random = Random()
    private var strokeBaseColor: Int? = null
    
    fun applyColorDynamics(
        baseColor: Int,
        brush: Brush,
        pressure: Float,
        isFirstStamp: Boolean
    ): Int {
        // For stamp jitter, randomize each stamp
        // For stroke jitter, randomize once per stroke
        if (!brush.stampColorJitter && strokeBaseColor != null) {
            return strokeBaseColor!!
        }
        
        val hsv = FloatArray(3)
        Color.colorToHSV(baseColor, hsv)
        
        var h = hsv[0]
        var s = hsv[1]
        var v = hsv[2]
        
        // Apply jitter
        if (brush.hueJitter > 0) {
            val jitterAmount = (brush.hueJitter / 100f) * 360f
            val jitter = random.nextFloat() * jitterAmount - jitterAmount / 2f
            h = (h + jitter) % 360f
            if (h < 0) h += 360f
        }
        
        if (brush.saturationJitter > 0) {
            val jitterAmount = brush.saturationJitter / 100f
            val jitter = random.nextFloat() * jitterAmount - jitterAmount / 2f
            s = (s + jitter).coerceIn(0f, 1f)
        }
        
        if (brush.brightnessJitter > 0) {
            val jitterAmount = brush.brightnessJitter / 100f
            val jitter = random.nextFloat() * jitterAmount - jitterAmount / 2f
            v = (v + jitter).coerceIn(0f, 1f)
        }
        
        // Apply pressure modulation if enabled
        if (brush.colorPressure) {
            val pressureFactor = 0.5f + pressure * 0.5f
            s *= pressureFactor
            v *= pressureFactor
        }
        
        val result = Color.HSVToColor(floatArrayOf(h, s, v))
        
        if (!brush.stampColorJitter && isFirstStamp) {
            strokeBaseColor = result
        }
        
        return result
    }
    
    fun resetStroke() {
        strokeBaseColor = null
    }
}
```

---

## 7. Rendering Modes

### Flow-Based Rendering

```kotlin
class RenderingModeEngine {
    
    fun applyRenderingMode(
        mode: RenderingMode,
        baseOpacity: Float,
        flow: Float,
        strokeDistance: Float
    ): Float {
        return when (mode) {
            RenderingMode.LIGHT_GLAZE -> {
                // Very low buildup
                baseOpacity * 0.3f * (flow / 100f)
            }
            
            RenderingMode.GLAZED -> {
                baseOpacity * 0.6f * (flow / 100f)
            }
            
            RenderingMode.UNIFORM -> {
                // Standard - opacity is constant
                baseOpacity
            }
            
            RenderingMode.INTENSE_GLAZE -> {
                // Medium buildup with distance factor
                val buildupFactor = min(1f, strokeDistance / 100f)
                baseOpacity * (0.7f + buildupFactor * 0.3f) * (flow / 100f)
            }
            
            RenderingMode.HEAVY_GLAZE -> {
                // Strong buildup
                val buildupFactor = min(1f, strokeDistance / 50f)
                baseOpacity * (0.8f + buildupFactor * 0.2f) * (flow / 100f)
            }
            
            RenderingMode.INTENSE_BLENDING -> {
                // Maximum paint mixing
                baseOpacity * 1.2f * (flow / 100f)  // Can exceed 100%
            }
        }
    }
}
```

---

## 8. Tilt and Azimuth Support

### Stylus Orientation Processing

```kotlin
class StylusOrientationProcessor {
    
    /**
     * Calculate brush rotation from stylus azimuth
     * Azimuth: compass direction of stylus (0-360°)
     */
    fun calculateAzimuthRotation(event: MotionEvent): Float {
        val orientation = event.orientation
        
        // Convert orientation (-PI to PI) to degrees (0-360)
        var degrees = Math.toDegrees(orientation.toDouble()).toFloat()
        if (degrees < 0) degrees += 360f
        
        return degrees
    }
    
    /**
     * Calculate size/opacity modification from tilt
     * Tilt: angle from vertical (0° = perpendicular, 90° = flat)
     */
    fun calculateTiltEffect(
        event: MotionEvent,
        tiltSensitivity: Float
    ): Float {
        val tilt = event.getAxisValue(MotionEvent.AXIS_TILT)
        
        // Tilt is 0 when perpendicular, increases when angled
        // We want larger brush when flat (high tilt)
        val tiltFactor = tilt * (tiltSensitivity / 100f)
        
        return 1f + tiltFactor
    }
}
```

---

## 9. Brush Presets Library

### Essential Brushes (Phase 1)

```kotlin
object BrushPresets {
    
    fun getPencil() = Brush(
        name = "HB Pencil",
        category = BrushCategory.SKETCHING,
        spacing = 5f,
        streamLine = 10f,
        opacity = 70f,
        flow = 60f,
        hardness = 0.3f,
        grainDepth = 30f,
        renderingMode = RenderingMode.GLAZED,
        sizeCurve = PressureCurve.easeIn()
    )
    
    fun getTechnicalPen() = Brush(
        name = "Technical Pen",
        category = BrushCategory.INKING,
        spacing = 8f,
        streamLine = 35f,
        opacity = 100f,
        flow = 100f,
        hardness = 0.95f,
        renderingMode = RenderingMode.UNIFORM,
        sizeCurve = PressureCurve.linear(),
        pressureTaper = true
    )
    
    fun getAirbrush() = Brush(
        name = "Soft Airbrush",
        category = BrushCategory.AIRBRUSHING,
        spacing = 2f,
        streamLine = 5f,
        opacity = 30f,
        flow = 40f,
        hardness = 0.1f,
        scatter = 15f,
        renderingMode = RenderingMode.LIGHT_GLAZE,
        sizeCurve = PressureCurve.linear()
    )
    
    fun getMarker() = Brush(
        name = "Chisel Marker",
        category = BrushCategory.DRAWING,
        spacing = 10f,
        opacity = 60f,
        flow = 80f,
        hardness = 0.5f,
        azimuthRotation = true,  // Follows stylus angle
        renderingMode = RenderingMode.GLAZED
    )
    
    fun getOilPaint() = Brush(
        name = "Oil Paint",
        category = BrushCategory.PAINTING,
        spacing = 5f,
        opacity = 80f,
        flow = 70f,
        hardness = 0.7f,
        wetMixEnabled = true,
        dilution = 20f,
        charge = 70f,
        pull = 40f,
        renderingMode = RenderingMode.INTENSE_BLENDING,
        grainDepth = 40f
    )
    
    fun getChalk() = Brush(
        name = "Soft Chalk",
        category = BrushCategory.DRAWING,
        spacing = 3f,
        opacity = 50f,
        flow = 50f,
        hardness = 0.2f,
        scatter = 20f,
        grainDepth = 60f,
        renderingMode = RenderingMode.HEAVY_GLAZE
    )
    
    fun getInkPen() = Brush(
        name = "Ink Bleed",
        category = BrushCategory.INKING,
        spacing = 8f,
        streamLine = 20f,
        opacity = 90f,
        flow = 85f,
        hardness = 0.6f,
        bleed = 30f,  // Size increases with pressure
        renderingMode = RenderingMode.INTENSE_GLAZE,
        sizeCurve = PressureCurve.easeOut()
    )
    
    fun getEraser() = Brush(
        name = "Hard Eraser",
        category = BrushCategory.TOUCHUPS,
        spacing = 10f,
        opacity = 100f,
        flow = 100f,
        hardness = 0.8f,
        blendMode = BlendMode.CLEAR,  // Special: removes pixels
        sizeCurve = PressureCurve.linear()
    )
    
    fun getSoftEraser() = Brush(
        name = "Soft Eraser",
        category = BrushCategory.TOUCHUPS,
        spacing = 5f,
        opacity = 50f,
        flow = 60f,
        hardness = 0.2f,
        blendMode = BlendMode.CLEAR,
        sizeCurve = PressureCurve.linear()
    )
    
    fun getSmudgeTool() = Brush(
        name = "Smudge",
        category = BrushCategory.TOUCHUPS,
        spacing = 5f,
        opacity = 80f,
        hardness = 0.5f,
        wetMixEnabled = true,
        pull = 100f,  // Maximum color pulling
        charge = 50f,
        renderingMode = RenderingMode.INTENSE_BLENDING
    )
}
```

---

## 10. Brush Studio UI (Phase 3)

### Brush Customization Interface

```
┌─────────────────────────────────────────────┐
│  BRUSH STUDIO                       [Done]  │
├─────────────────────────────────────────────┤
│  Preview                                    │
│  ┌───────────────────────────────────────┐ │
│  │                                       │ │
│  │     [Brush stroke preview here]       │ │
│  │                                       │ │
│  └───────────────────────────────────────┘ │
├─────────────────────────────────────────────┤
│  Categories:                                │
│  [Stroke] [Shape] [Grain] [Rendering]      │
│  [Dynamics] [Apple Pencil]                  │
├─────────────────────────────────────────────┤
│  ┌─ Stroke Path ──────────────────────┐    │
│  │ Spacing:      10% [▬▬▬▬▬○────]     │    │
│  │ StreamLine:   35% [▬▬▬○──────]     │    │
│  │ Jitter:        0% [○──────────]     │    │
│  │ Fall-off:      0% [○──────────]     │    │
│  └────────────────────────────────────┘    │
│                                             │
│  ┌─ Taper ─────────────────────────────┐   │
│  │ ☑ Pressure Taper                    │   │
│  │ Tip:           0% [○──────────]     │   │
│  │ End:           0% [○──────────]     │   │
│  │ ☐ Classic Taper                     │   │
│  └────────────────────────────────────┘    │
└─────────────────────────────────────────────┘
```

### Brush Property Widgets

```kotlin
@Composable
fun BrushPropertySlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float> = 0f..100f,
    onValueChange: (Float) -> Unit,
    unit: String = "%"
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label)
            Text("${value.toInt()}$unit")
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun PressureCurveEditor(
    curve: PressureCurve,
    onCurveChange: (PressureCurve) -> Unit
) {
    // Interactive curve editor
    Canvas(modifier = Modifier.size(200.dp)) {
        // Draw grid
        drawLine(...)
        
        // Draw curve
        val path = Path()
        curve.points.forEach { point ->
            val x = point.x * size.width
            val y = (1f - point.y) * size.height
            if (path.isEmpty) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        drawPath(path, color = Color.White, style = Stroke(width = 2f))
        
        // Draw control points
        curve.points.forEach { point ->
            drawCircle(
                color = Color.Blue,
                radius = 8f,
                center = Offset(
                    point.x * size.width,
                    (1f - point.y) * size.height
                )
            )
        }
    }
}
```

---

## 11. GPU Acceleration (Phase 2+)

### OpenGL Shader for Brush Stamps

```glsl
// Fragment shader for brush rendering
precision mediump float;

uniform sampler2D u_ShapeTexture;
uniform sampler2D u_GrainTexture;
uniform vec4 u_Color;
uniform float u_Opacity;
uniform float u_Hardness;
uniform float u_GrainDepth;
uniform int u_GrainBlendMode;

varying vec2 v_TexCoord;
varying vec2 v_GrainCoord;

void main() {
    // Sample shape
    float shapeMask = texture2D(u_ShapeTexture, v_TexCoord).a;
    
    // Apply hardness (softness on edges)
    float distance = length(v_TexCoord - vec2(0.5, 0.5)) * 2.0;
    float softness = 1.0 - u_Hardness;
    shapeMask *= smoothstep(1.0, 1.0 - softness, distance);
    
    // Sample grain
    vec4 grain = texture2D(u_GrainTexture, v_GrainCoord);
    
    // Blend grain with color
    vec3 color = u_Color.rgb;
    if (u_GrainDepth > 0.0) {
        // Multiply blend for grain
        vec3 grainBlended = color * grain.rgb;
        color = mix(color, grainBlended, u_GrainDepth);
    }
    
    // Apply opacity and shape mask
    float alpha = u_Opacity * shapeMask;
    
    gl_FragColor = vec4(color, alpha);
}
```

### Vulkan Implementation (Modern)

```kotlin
class VulkanBrushRenderer {
    
    private lateinit var device: VkDevice
    private lateinit var pipeline: VkPipeline
    private lateinit var commandBuffer: VkCommandBuffer
    
    fun renderStroke(stroke: Stroke, targetTexture: VkImage) {
        // Begin command buffer
        vkBeginCommandBuffer(commandBuffer, beginInfo)
        
        // Bind pipeline
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline)
        
        // Render each stamp
        stroke.points.forEach { point ->
            // Update uniform buffer with stamp parameters
            updateStampUniforms(point, stroke.brush)
            
            // Draw quad with stamp shader
            vkCmdDraw(commandBuffer, 4, 1, 0, 0)
        }
        
        // End and submit
        vkEndCommandBuffer(commandBuffer)
        vkQueueSubmit(queue, submitInfo, fence)
    }
}
```

---

## 12. Performance Optimization

### Stamp Caching

```kotlin
class BrushStampCache {
    
    private val cache = LruCache<StampKey, Bitmap>(100)
    
    fun getOrCreateStamp(
        brush: Brush,
        size: Float,
        rotation: Float,
        color: Int
    ): Bitmap {
        val key = StampKey(brush.id, size.toInt(), rotation.toInt(), color)
        
        return cache.get(key) ?: run {
            val stamp = generateStamp(brush, size, rotation, color)
            cache.put(key, stamp)
            stamp
        }
    }
    
    private fun generateStamp(
        brush: Brush,
        size: Float,
        rotation: Float,
        color: Int
    ): Bitmap {
        val stampSize = (size * 2).toInt()
        val bitmap = Bitmap.createBitmap(stampSize, stampSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Render stamp to bitmap
        // ... shape + grain + color
        
        return bitmap
    }
}

data class StampKey(
    val brushId: String,
    val size: Int,       // Rounded to nearest 5px
    val rotation: Int,   // Rounded to nearest 15°
    val color: Int
)
```

### Multi-threaded Rendering

```kotlin
class ParallelStrokeRenderer {
    
    private val renderPool = Executors.newFixedThreadPool(4)
    
    fun renderStrokeParallel(
        stroke: Stroke,
        targetBitmap: Bitmap,
        onComplete: () -> Unit
    ) {
        // Split stroke into segments
        val pointsPerSegment = stroke.points.size / 4
        val segments = stroke.points.chunked(pointsPerSegment)
        
        val latch = CountDownLatch(segments.size)
        
        segments.forEach { segmentPoints ->
            renderPool.submit {
                try {
                    renderSegment(segmentPoints, stroke.brush, targetBitmap)
                } finally {
                    latch.countDown()
                }
            }
        }
        
        // Wait for all segments
        renderPool.submit {
            latch.await()
            onComplete()
        }
    }
}
```

---

## 13. Brush Import/Export

### Brush File Format (.artbrush)

```json
{
  "version": "1.0",
  "brush": {
    "name": "My Custom Brush",
    "category": "PAINTING",
    
    "strokePath": {
      "spacing": 10,
      "streamLine": 35,
      "jitter": 0,
      "fallOff": 0
    },
    
    "shape": {
      "source": "circle",
      "scatter": 0,
      "rotation": 0,
      "rotationRandomized": false,
      "azimuthRotation": false,
      "stampCount": 1
    },
    
    "grain": {
      "scale": 100,
      "zoom": 100,
      "blendMode": "MULTIPLY",
      "depth": 50,
      "movement": "MOVING",
      "filtering": true
    },
    
    "rendering": {
      "mode": "UNIFORM",
      "opacity": 100,
      "flow": 100,
      "blendMode": "NORMAL"
    },
    
    "applePencil": {
      "pressureCurve": [
        {"x": 0.0, "y": 0.0, "tangent": 1.0},
        {"x": 1.0, "y": 1.0, "tangent": 1.0}
      ],
      "tiltSensitivity": 0,
      "bleed": 0,
      "minSize": 0,
      "minOpacity": 0
    }
  },
  
  "assets": {
    "shapeImage": null,
    "grainImage": "base64_encoded_png..."
  }
}
```

### Brush Library Manager

```kotlin
class BrushLibrary {
    
    private val brushes = mutableMapOf<String, Brush>()
    private val categories = mutableMapOf<BrushCategory, List<String>>()
    
    fun loadDefaultBrushes() {
        // Load built-in presets
        addBrush(BrushPresets.getPencil())
        addBrush(BrushPresets.getTechnicalPen())
        addBrush(BrushPresets.getAirbrush())
        // ... all presets
        
        organizeByCategory()
    }
    
    fun importBrush(file: File): Brush {
        val json = file.readText()
        val brushData = Json.decodeFromString<BrushData>(json)
        
        // Convert JSON to Brush object
        val brush = brushData.toBrush()
        
        addBrush(brush)
        return brush
    }
    
    fun exportBrush(brush: Brush, file: File) {
        val brushData = BrushData.fromBrush(brush)
        val json = Json.encodeToString(brushData)
        
        file.writeText(json)
    }
    
    fun getBrushesByCategory(category: BrushCategory): List<Brush> {
        return categories[category]?.mapNotNull { brushes[it] } ?: emptyList()
    }
}
```

---

## 14. Testing & Validation

### Brush Performance Tests

```kotlin
@Test
fun testBrushPerformance_120fps() {
    val brush = BrushPresets.getTechnicalPen()
    val canvas = createTestCanvas(2048, 2048)
    
    // Simulate 1 second of drawing at 120 FPS
    val pointsPerFrame = 5  // Typical touch event density
    val totalPoints = 120 * pointsPerFrame
    
    val points = generateTestStroke(totalPoints)
    
    val startTime = System.nanoTime()
    
    brushEngine.renderStroke(
        Stroke(points = points, brush = brush, color = Color.BLACK),
        canvas
    )
    
    val endTime = System.nanoTime()
    val durationMs = (endTime - startTime) / 1_000_000
    
    // Should complete in < 1 second for 120 FPS
    assertTrue(durationMs < 1000, "Brush rendering took ${durationMs}ms, target <1000ms")
}

@Test
fun testPressureCurve_Accuracy() {
    val curve = PressureCurve.easeIn()
    
    assertEquals(0f, curve.evaluate(0f), 0.01f)
    assertEquals(1f, curve.evaluate(1f), 0.01f)
    
    // Ease-in should be slower at start
    assertTrue(curve.evaluate(0.25f) < 0.25f)
    assertTrue(curve.evaluate(0.75f) > 0.75f)
}

@Test
fun testColorJitter_Range() {
    val brush = Brush(
        hueJitter = 50f,
        saturationJitter = 30f,
        brightnessJitter = 20f
    )
    
    val baseColor = Color.rgb(255, 0, 0)  // Pure red
    
    repeat(100) {
        val jitteredColor = colorDynamics.applyColorDynamics(
            baseColor,
            brush,
            pressure = 0.5f,
            isFirstStamp = false
        )
        
        val hsv = FloatArray(3)
        Color.colorToHSV(jitteredColor, hsv)
        
        // Verify jitter stays in expected ranges
        // Original red is hue=0
        assertTrue(hsv[0] >= 0f && hsv[0] <= 90f)  // ±50% of 180° range
    }
}
```

---

## 15. Brush Implementation Roadmap

### Phase 1: Essential (Current → +2 months)

**Current Status**:
- [x] Basic stamp rendering
- [x] Pressure-sensitive size
- [x] Pressure-sensitive opacity
- [x] 5 brush presets
- [x] Catmull-Rom interpolation

**Add**:
- [ ] StreamLine stabilization
- [ ] Tilt support (basic)
- [ ] Hardness/softness control
- [ ] Flow control
- [ ] 10 total brush presets
- [ ] Basic grain texture support

### Phase 2: Professional (+2-4 months)

- [ ] Pressure curves (custom curves)
- [ ] Azimuth rotation
- [ ] Scatter
- [ ] Advanced grain (moving vs texturized)
- [ ] Color dynamics (basic jitter)
- [ ] 30+ brush presets
- [ ] Brush size/opacity sliders in UI
- [ ] Brush import/export

### Phase 3: Advanced (+4-8 months)

- [ ] Wet Mix system
- [ ] Full Brush Studio UI
- [ ] All 14 attribute categories
- [ ] Rendering modes (6 types)
- [ ] Shape designer (custom shapes)
- [ ] Grain designer (custom textures)
- [ ] 100+ brush library
- [ ] GPU shader-based rendering

### Phase 4: Expert (+8-12 months)

- [ ] Brush marketplace/sharing
- [ ] AI brush recommendations
- [ ] Advanced wet mix (gradient mode)
- [ ] Brush animation (animated grain)
- [ ] Procedural brushes
- [ ] Brush scripting API

---

## 16. Critical Success Factors

### Must Achieve

1. **Latency < 20ms**: Touch to pixel
2. **60 FPS minimum**: Smooth drawing always
3. **Pressure accuracy**: Match Procreate's feel
4. **Brush variety**: 30+ brushes covering all use cases
5. **GPU acceleration**: For complex brushes

### Quality Benchmarks

- **Side-by-side test**: Can users tell the difference from Procreate?
- **Artist acceptance**: Professional illustrators approve
- **Performance**: Works on 3-year-old devices
- **Consistency**: Same brush, same result every time

---

## Summary

This specification provides:
- ✅ Complete brush data model (14 attribute categories)
- ✅ Stroke smoothing and stabilization algorithms
- ✅ Advanced rendering techniques (grain, wet mix, color dynamics)
- ✅ GPU acceleration strategies
- ✅ 10 essential brush presets
- ✅ Brush Studio UI design
- ✅ Import/export format
- ✅ Performance targets and testing

**Current Artboard Status**:
- [x] Basic stamp-based rendering
- [x] Pressure-sensitive size and opacity
- [x] Catmull-Rom interpolation
- [x] 5 brush types
- [ ] StreamLine (specified, not implemented)
- [ ] Tilt/azimuth (specified, not implemented)
- [ ] Grain system (specified, not implemented)
- [ ] Wet mix (specified, not implemented)
- [ ] GPU acceleration (specified, not implemented)

**Next Implementation Steps**:
1. Add StreamLine stabilization (major UX improvement)
2. Implement tilt support for stylus
3. Add hardness parameter to existing brushes
4. Create 5 more essential brushes (10 total)
5. Build brush picker UI with size/opacity sliders
