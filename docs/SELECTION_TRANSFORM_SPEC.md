# Selection & Transform Tools Specification

This document provides complete technical specifications for implementing professional selection and transform tools.

---

## Architecture Overview

```
Selection System
├── Selection Mask (1-bit or 8-bit alpha)
├── Selection Tools
│   ├── Automatic (Flood Fill)
│   ├── Freehand (Path-based)
│   ├── Rectangle
│   └── Ellipse
├── Selection Operations
│   ├── Add, Subtract, Intersect, Invert
│   └── Feathering, Growing, Shrinking
└── Transform
    ├── Freeform
    ├── Uniform
    ├── Distort (Perspective)
    └── Warp (Mesh)
```

---

## 1. Selection Data Model

### Selection Mask

```kotlin
data class Selection(
    val id: String = UUID.randomUUID().toString(),
    val mask: Bitmap,                  // 8-bit alpha mask
    val bounds: Rect,                  // Bounding box
    val feather: Float = 0f,           // Edge softness (0-100px)
    val antiAlias: Boolean = true,
    val inverted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Check if a point is selected
     */
    fun isSelected(x: Int, y: Int): Boolean {
        if (!bounds.contains(x, y)) return false
        
        val alpha = mask.getPixel(x, y) and 0xFF
        return if (inverted) alpha < 128 else alpha >= 128
    }
    
    /**
     * Get selection strength at point (for feathering)
     */
    fun getSelectionStrength(x: Int, y: Int): Float {
        if (!bounds.contains(x, y)) return 0f
        
        val alpha = (mask.getPixel(x, y) and 0xFF) / 255f
        return if (inverted) 1f - alpha else alpha
    }
    
    /**
     * Create inverted selection
     */
    fun invert(): Selection {
        return copy(inverted = !inverted)
    }
    
    companion object {
        /**
         * Create full canvas selection
         */
        fun all(width: Int, height: Int): Selection {
            val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
            val canvas = Canvas(mask)
            canvas.drawColor(0xFFFFFFFF.toInt())  // All white = fully selected
            
            return Selection(
                mask = mask,
                bounds = Rect(0, 0, width, height)
            )
        }
        
        /**
         * Create empty selection
         */
        fun none(width: Int, height: Int): Selection {
            val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
            return Selection(
                mask = mask,
                bounds = Rect(0, 0, 0, 0)
            )
        }
    }
}
```

---

## 2. Automatic Selection (Magic Wand / Flood Fill)

### Algorithm Implementation

```kotlin
class AutomaticSelectionTool {
    
    /**
     * Create selection based on color threshold
     */
    fun select(
        sourceBitmap: Bitmap,
        seedPoint: Point,
        threshold: Float,        // 0-100% color similarity
        continuous: Boolean,     // Only connected pixels
        antiAlias: Boolean = true
    ): Selection {
        val width = sourceBitmap.width
        val height = sourceBitmap.height
        
        val selectionMask = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ALPHA_8
        )
        
        if (continuous) {
            floodFillSelection(
                sourceBitmap,
                selectionMask,
                seedPoint.x.toInt(),
                seedPoint.y.toInt(),
                threshold
            )
        } else {
            colorBasedSelection(
                sourceBitmap,
                selectionMask,
                seedPoint.x.toInt(),
                seedPoint.y.toInt(),
                threshold
            )
        }
        
        if (antiAlias) {
            applyAntiAliasing(selectionMask)
        }
        
        val bounds = calculateBounds(selectionMask)
        
        return Selection(
            mask = selectionMask,
            bounds = bounds,
            antiAlias = antiAlias
        )
    }
    
    private fun floodFillSelection(
        source: Bitmap,
        mask: Bitmap,
        startX: Int,
        startY: Int,
        threshold: Float
    ) {
        val targetColor = source.getPixel(startX, startY)
        val visited = Array(source.height) { BooleanArray(source.width) }
        
        val queue = ArrayDeque<Point>()
        queue.add(Point(startX.toFloat(), startY.toFloat(), 0f))
        
        while (queue.isNotEmpty()) {
            val point = queue.removeFirst()
            val x = point.x.toInt()
            val y = point.y.toInt()
            
            // Bounds check
            if (x < 0 || x >= source.width || y < 0 || y >= source.height) continue
            if (visited[y][x]) continue
            
            visited[y][x] = true
            
            // Color similarity check
            val pixelColor = source.getPixel(x, y)
            val similarity = colorSimilarity(targetColor, pixelColor)
            
            if (similarity >= threshold / 100f) {
                // Mark as selected
                mask.setPixel(x, y, 0xFFFFFFFF.toInt())
                
                // Add neighbors to queue
                queue.add(Point((x + 1).toFloat(), y.toFloat(), 0f))
                queue.add(Point((x - 1).toFloat(), y.toFloat(), 0f))
                queue.add(Point(x.toFloat(), (y + 1).toFloat(), 0f))
                queue.add(Point(x.toFloat(), (y - 1).toFloat(), 0f))
            }
        }
    }
    
    /**
     * Calculate color similarity in LAB color space (perceptual)
     */
    private fun colorSimilarity(color1: Int, color2: Int): Float {
        // Convert to LAB for perceptual color distance
        val lab1 = rgbToLab(color1)
        val lab2 = rgbToLab(color2)
        
        // Delta E (CIE76) - simplified
        val deltaL = lab1[0] - lab2[0]
        val deltaA = lab1[1] - lab2[1]
        val deltaB = lab1[2] - lab2[2]
        
        val deltaE = sqrt(deltaL * deltaL + deltaA * deltaA + deltaB * deltaB)
        
        // Normalize to 0-1 (deltaE typically 0-100)
        val similarity = 1f - (deltaE / 100f).coerceIn(0f, 1f)
        
        return similarity
    }
    
    private fun rgbToLab(color: Int): FloatArray {
        // RGB → XYZ → LAB conversion
        var r = Color.red(color) / 255f
        var g = Color.green(color) / 255f
        var b = Color.blue(color) / 255f
        
        // Gamma correction
        r = if (r > 0.04045f) pow((r + 0.055f) / 1.055f, 2.4f) else r / 12.92f
        g = if (g > 0.04045f) pow((g + 0.055f) / 1.055f, 2.4f) else g / 12.92f
        b = if (b > 0.04045f) pow((b + 0.055f) / 1.055f, 2.4f) else b / 12.92f
        
        // RGB → XYZ
        var x = r * 0.4124f + g * 0.3576f + b * 0.1805f
        var y = r * 0.2126f + g * 0.7152f + b * 0.0722f
        var z = r * 0.0193f + g * 0.1192f + b * 0.9505f
        
        // Normalize to D65 white point
        x /= 0.95047f
        y /= 1.00000f
        z /= 1.08883f
        
        // XYZ → LAB
        x = if (x > 0.008856f) pow(x, 1f / 3f) else (7.787f * x) + 16f / 116f
        y = if (y > 0.008856f) pow(y, 1f / 3f) else (7.787f * y) + 16f / 116f
        z = if (z > 0.008856f) pow(z, 1f / 3f) else (7.787f * z) + 16f / 116f
        
        val L = (116f * y) - 16f
        val a = 500f * (x - y)
        val b2 = 200f * (y - z)
        
        return floatArrayOf(L, a, b2)
    }
}
```

---

## 3. Freehand Selection

### Path-Based Selection

```kotlin
class FreehandSelectionTool {
    
    private val currentPath = Path()
    private val pathPoints = mutableListOf<Point>()
    
    fun beginSelection(point: Point) {
        currentPath.reset()
        pathPoints.clear()
        
        currentPath.moveTo(point.x, point.y)
        pathPoints.add(point)
    }
    
    fun continueSelection(point: Point) {
        // Smooth the path using Catmull-Rom
        if (pathPoints.size >= 3) {
            val p0 = pathPoints[pathPoints.size - 2]
            val p1 = pathPoints[pathPoints.size - 1]
            val p2 = point
            
            // Cubic bezier from p1 to p2
            val cp1x = p1.x + (p2.x - p0.x) * 0.25f
            val cp1y = p1.y + (p2.y - p0.y) * 0.25f
            val cp2x = p2.x - (p2.x - p1.x) * 0.25f
            val cp2y = p2.y - (p2.y - p1.y) * 0.25f
            
            currentPath.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
        } else {
            currentPath.lineTo(point.x, point.y)
        }
        
        pathPoints.add(point)
    }
    
    fun endSelection(canvasWidth: Int, canvasHeight: Int): Selection {
        // Close the path
        if (pathPoints.size > 2) {
            currentPath.close()
        }
        
        // Rasterize path to selection mask
        val mask = Bitmap.createBitmap(
            canvasWidth,
            canvasHeight,
            Bitmap.Config.ALPHA_8
        )
        
        val canvas = Canvas(mask)
        val paint = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        canvas.drawPath(currentPath, paint)
        
        val bounds = calculatePathBounds(currentPath)
        
        return Selection(
            mask = mask,
            bounds = bounds
        )
    }
}
```

---

## 4. Rectangle & Ellipse Selection

### Geometric Selection Tools

```kotlin
class RectangleSelectionTool {
    
    private var startPoint: Point? = null
    
    fun beginSelection(point: Point) {
        startPoint = point
    }
    
    fun updateSelection(
        currentPoint: Point,
        maintainAspect: Boolean = false
    ): Rect {
        val start = startPoint ?: return Rect(0f, 0f, 0f, 0f)
        
        var width = currentPoint.x - start.x
        var height = currentPoint.y - start.y
        
        if (maintainAspect) {
            // Make square
            val size = max(abs(width), abs(height))
            width = if (width >= 0) size else -size
            height = if (height >= 0) size else -size
        }
        
        return Rect(
            left = min(start.x, start.x + width),
            top = min(start.y, start.y + height),
            right = max(start.x, start.x + width),
            bottom = max(start.y, start.y + height)
        )
    }
    
    fun endSelection(
        canvasWidth: Int,
        canvasHeight: Int,
        bounds: Rect
    ): Selection {
        val mask = Bitmap.createBitmap(
            canvasWidth,
            canvasHeight,
            Bitmap.Config.ALPHA_8
        )
        
        val canvas = Canvas(mask)
        val paint = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        canvas.drawRect(bounds.toAndroidRect(), paint)
        
        return Selection(
            mask = mask,
            bounds = bounds
        )
    }
}

class EllipseSelectionTool {
    
    fun endSelection(
        canvasWidth: Int,
        canvasHeight: Int,
        bounds: Rect
    ): Selection {
        val mask = Bitmap.createBitmap(
            canvasWidth,
            canvasHeight,
            Bitmap.Config.ALPHA_8
        )
        
        val canvas = Canvas(mask)
        val paint = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        val oval = RectF(
            bounds.left,
            bounds.top,
            bounds.right,
            bounds.bottom
        )
        
        canvas.drawOval(oval, paint)
        
        return Selection(
            mask = mask,
            bounds = bounds
        )
    }
}
```

---

## 5. Selection Operations

### Combine Selections

```kotlin
class SelectionOperations {
    
    fun add(selection1: Selection, selection2: Selection): Selection {
        val result = selection1.mask.copy(Bitmap.Config.ALPHA_8, true)
        
        // Union: OR operation on masks
        for (y in 0 until result.height) {
            for (x in 0 until result.width) {
                val alpha1 = selection1.mask.getPixel(x, y) and 0xFF
                val alpha2 = selection2.mask.getPixel(x, y) and 0xFF
                
                val combined = max(alpha1, alpha2)
                result.setPixel(x, y, combined or (combined shl 8) or (combined shl 16) or (combined shl 24))
            }
        }
        
        return Selection(
            mask = result,
            bounds = selection1.bounds.union(selection2.bounds)
        )
    }
    
    fun subtract(selection1: Selection, selection2: Selection): Selection {
        val result = selection1.mask.copy(Bitmap.Config.ALPHA_8, true)
        
        // Difference: selection1 AND NOT selection2
        for (y in 0 until result.height) {
            for (x in 0 until result.width) {
                val alpha1 = selection1.mask.getPixel(x, y) and 0xFF
                val alpha2 = selection2.mask.getPixel(x, y) and 0xFF
                
                val combined = max(0, alpha1 - alpha2)
                result.setPixel(x, y, combined or (combined shl 8) or (combined shl 16) or (combined shl 24))
            }
        }
        
        return Selection(
            mask = result,
            bounds = selection1.bounds  // May shrink, recalculate if needed
        )
    }
    
    fun intersect(selection1: Selection, selection2: Selection): Selection {
        val result = selection1.mask.copy(Bitmap.Config.ALPHA_8, true)
        
        // Intersection: AND operation
        for (y in 0 until result.height) {
            for (x in 0 until result.width) {
                val alpha1 = selection1.mask.getPixel(x, y) and 0xFF
                val alpha2 = selection2.mask.getPixel(x, y) and 0xFF
                
                val combined = min(alpha1, alpha2)
                result.setPixel(x, y, combined or (combined shl 8) or (combined shl 16) or (combined shl 24))
            }
        }
        
        val bounds = selection1.bounds.intersect(selection2.bounds)
        
        return Selection(
            mask = result,
            bounds = bounds ?: Rect(0f, 0f, 0f, 0f)
        )
    }
}
```

### Feathering (Edge Softening)

```kotlin
class SelectionFeathering {
    
    /**
     * Apply Gaussian blur to selection edge
     */
    fun feather(selection: Selection, radius: Float): Selection {
        if (radius <= 0) return selection
        
        val featheredMask = gaussianBlur(selection.mask, radius)
        
        return selection.copy(
            mask = featheredMask,
            feather = radius
        )
    }
    
    private fun gaussianBlur(source: Bitmap, radius: Float): Bitmap {
        val width = source.width
        val height = source.height
        
        // Create kernel
        val kernelSize = (radius * 2).toInt() or 1  // Ensure odd
        val kernel = createGaussianKernel(kernelSize, radius)
        
        // Horizontal pass
        val temp = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        convolveHorizontal(source, temp, kernel)
        
        // Vertical pass
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        convolveVertical(temp, result, kernel)
        
        temp.recycle()
        return result
    }
    
    private fun createGaussianKernel(size: Int, sigma: Float): FloatArray {
        val kernel = FloatArray(size)
        val center = size / 2
        var sum = 0f
        
        for (i in 0 until size) {
            val x = i - center
            kernel[i] = exp(-(x * x) / (2f * sigma * sigma))
            sum += kernel[i]
        }
        
        // Normalize
        for (i in 0 until size) {
            kernel[i] /= sum
        }
        
        return kernel
    }
}
```

### Grow & Shrink Selection

```kotlin
class SelectionModification {
    
    fun grow(selection: Selection, pixels: Int): Selection {
        return dilate(selection.mask, pixels).let { dilatedMask ->
            Selection(
                mask = dilatedMask,
                bounds = selection.bounds.expand(pixels.toFloat())
            )
        }
    }
    
    fun shrink(selection: Selection, pixels: Int): Selection {
        return erode(selection.mask, pixels).let { erodedMask ->
            Selection(
                mask = erodedMask,
                bounds = calculateBounds(erodedMask)
            )
        }
    }
    
    private fun dilate(mask: Bitmap, pixels: Int): Bitmap {
        val result = mask.copy(Bitmap.Config.ALPHA_8, true)
        
        // Morphological dilation
        for (iter in 0 until pixels) {
            val temp = result.copy(Bitmap.Config.ALPHA_8, true)
            
            for (y in 1 until mask.height - 1) {
                for (x in 1 until mask.width - 1) {
                    val current = temp.getPixel(x, y) and 0xFF
                    
                    if (current > 0) {
                        // Expand to neighbors
                        result.setPixel(x + 1, y, 0xFFFFFFFF.toInt())
                        result.setPixel(x - 1, y, 0xFFFFFFFF.toInt())
                        result.setPixel(x, y + 1, 0xFFFFFFFF.toInt())
                        result.setPixel(x, y - 1, 0xFFFFFFFF.toInt())
                    }
                }
            }
        }
        
        return result
    }
    
    private fun erode(mask: Bitmap, pixels: Int): Bitmap {
        // Similar to dilate but removes pixels if any neighbor is empty
        // Implementation omitted for brevity
    }
}
```

---

## 6. Transform System

### Transform Data Model

```kotlin
data class Transform(
    val mode: TransformMode,
    
    // Transform matrix (3x3 for 2D affine)
    val matrix: Matrix = Matrix(),
    
    // For warp mode
    val meshRows: Int = 4,
    val meshCols: Int = 4,
    val meshPoints: Array<PointF>? = null,
    
    // Settings
    val interpolation: InterpolationMode = InterpolationMode.BILINEAR,
    val snapAngle: Boolean = true,
    val snapDistance: Int = 10,  // pixels
    val maintainAspect: Boolean = false
)

enum class TransformMode {
    FREEFORM,      // Free scaling, rotation, translation
    UNIFORM,       // Proportional scaling only
    DISTORT,       // 4-point perspective
    WARP           // Mesh-based deformation
}

enum class InterpolationMode {
    NEAREST,       // Fast, pixelated
    BILINEAR,      // Good quality (default)
    BICUBIC        // Best quality, slower
}
```

### Freeform Transform

```kotlin
class FreeformTransform {
    
    private var originalBitmap: Bitmap? = null
    private val matrix = Matrix()
    
    // Control points
    private var centerPoint = PointF(0f, 0f)
    private var corners = Array(4) { PointF(0f, 0f) }
    
    fun beginTransform(bitmap: Bitmap) {
        originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        
        // Initialize control points
        centerPoint = PointF(bitmap.width / 2f, bitmap.height / 2f)
        corners[0] = PointF(0f, 0f)  // Top-left
        corners[1] = PointF(bitmap.width.toFloat(), 0f)  // Top-right
        corners[2] = PointF(bitmap.width.toFloat(), bitmap.height.toFloat())  // Bottom-right
        corners[3] = PointF(0f, bitmap.height.toFloat())  // Bottom-left
        
        matrix.reset()
    }
    
    fun scale(scaleX: Float, scaleY: Float, pivot: PointF = centerPoint) {
        matrix.postScale(scaleX, scaleY, pivot.x, pivot.y)
        updateCorners()
    }
    
    fun rotate(degrees: Float, pivot: PointF = centerPoint) {
        matrix.postRotate(degrees, pivot.x, pivot.y)
        updateCorners()
    }
    
    fun translate(dx: Float, dy: Float) {
        matrix.postTranslate(dx, dy)
        updateCorners()
    }
    
    fun applyTransform(): Bitmap {
        val original = originalBitmap ?: throw IllegalStateException("No bitmap to transform")
        
        // Calculate destination bounds
        val rect = RectF(0f, 0f, original.width.toFloat(), original.height.toFloat())
        matrix.mapRect(rect)
        
        val destWidth = rect.width().toInt()
        val destHeight = rect.height().toInt()
        
        val result = Bitmap.createBitmap(destWidth, destHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        
        // Apply transform
        canvas.translate(-rect.left, -rect.top)
        canvas.setMatrix(matrix)
        
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true  // Bilinear filtering
        }
        
        canvas.drawBitmap(original, 0f, 0f, paint)
        
        return result
    }
    
    private fun updateCorners() {
        val points = floatArrayOf(
            0f, 0f,                                          // Top-left
            originalBitmap!!.width.toFloat(), 0f,           // Top-right
            originalBitmap!!.width.toFloat(), 
            originalBitmap!!.height.toFloat(),              // Bottom-right
            0f, originalBitmap!!.height.toFloat()           // Bottom-left
        )
        
        matrix.mapPoints(points)
        
        for (i in 0 until 4) {
            corners[i] = PointF(points[i * 2], points[i * 2 + 1])
        }
    }
}
```

---

## 7. Distort Transform (Perspective)

### 4-Point Perspective Transform

```kotlin
class DistortTransform {
    
    /**
     * Apply perspective transform defined by 4 corner points
     */
    fun applyPerspective(
        source: Bitmap,
        srcCorners: Array<PointF>,  // Original 4 corners
        dstCorners: Array<PointF>   // Destination 4 corners
    ): Bitmap {
        // Calculate perspective transform matrix (homography)
        val matrix = Matrix()
        
        val srcPoints = srcCorners.flatMap { listOf(it.x, it.y) }.toFloatArray()
        val dstPoints = dstCorners.flatMap { listOf(it.x, it.y) }.toFloatArray()
        
        matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)
        
        // Calculate destination bounds
        val dstBounds = calculateBounds(dstCorners)
        
        val result = Bitmap.createBitmap(
            dstBounds.width.toInt(),
            dstBounds.height.toInt(),
            Bitmap.Config.ARGB_8888
        )
        
        val canvas = Canvas(result)
        canvas.translate(-dstBounds.left, -dstBounds.top)
        canvas.setMatrix(matrix)
        
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        
        canvas.drawBitmap(source, 0f, 0f, paint)
        
        return result
    }
}
```

---

## 8. Warp Transform (Mesh-Based)

### Mesh Deformation

```kotlin
class WarpTransform {
    
    data class WarpMesh(
        val rows: Int,
        val cols: Int,
        val points: Array<PointF>  // (rows+1) × (cols+1) points
    ) {
        fun getPoint(row: Int, col: Int): PointF {
            return points[row * (cols + 1) + col]
        }
        
        fun setPoint(row: Int, col: Int, point: PointF) {
            points[row * (cols + 1) + col] = point
        }
    }
    
    fun createMesh(width: Int, height: Int, rows: Int = 4, cols: Int = 4): WarpMesh {
        val points = Array((rows + 1) * (cols + 1)) { i ->
            val row = i / (cols + 1)
            val col = i % (cols + 1)
            
            PointF(
                col * width.toFloat() / cols,
                row * height.toFloat() / rows
            )
        }
        
        return WarpMesh(rows, cols, points)
    }
    
    fun applyWarp(source: Bitmap, mesh: WarpMesh): Bitmap {
        val result = Bitmap.createBitmap(
            source.width,
            source.height,
            Bitmap.Config.ARGB_8888
        )
        
        // For each destination pixel, find source pixel
        for (y in 0 until result.height) {
            for (x in 0 until result.width) {
                val srcPoint = inverseMeshMap(x.toFloat(), y.toFloat(), mesh, source.width, source.height)
                
                if (srcPoint != null) {
                    val color = bilinearSample(source, srcPoint.x, srcPoint.y)
                    result.setPixel(x, y, color)
                }
            }
        }
        
        return result
    }
    
    /**
     * Find which quad a point belongs to and map back to source
     */
    private fun inverseMeshMap(
        dstX: Float,
        dstY: Float,
        mesh: WarpMesh,
        srcWidth: Int,
        srcHeight: Int
    ): PointF? {
        // Find containing quad
        for (row in 0 until mesh.rows) {
            for (col in 0 until mesh.cols) {
                val p0 = mesh.getPoint(row, col)
                val p1 = mesh.getPoint(row, col + 1)
                val p2 = mesh.getPoint(row + 1, col + 1)
                val p3 = mesh.getPoint(row + 1, col)
                
                if (pointInQuad(dstX, dstY, p0, p1, p2, p3)) {
                    // Found containing quad, compute bilinear inverse
                    val u = (dstX - p0.x) / (p1.x - p0.x)
                    val v = (dstY - p0.y) / (p3.y - p0.y)
                    
                    // Map back to source coordinates
                    val srcX = col * srcWidth / mesh.cols + u * srcWidth / mesh.cols
                    val srcY = row * srcHeight / mesh.rows + v * srcHeight / mesh.rows
                    
                    return PointF(srcX, srcY)
                }
            }
        }
        
        return null
    }
    
    private fun bilinearSample(bitmap: Bitmap, x: Float, y: Float): Int {
        val x0 = x.toInt()
        val y0 = y.toInt()
        val x1 = (x0 + 1).coerceIn(0, bitmap.width - 1)
        val y1 = (y0 + 1).coerceIn(0, bitmap.height - 1)
        
        val dx = x - x0
        val dy = y - y0
        
        val c00 = bitmap.getPixel(x0, y0)
        val c10 = bitmap.getPixel(x1, y0)
        val c01 = bitmap.getPixel(x0, y1)
        val c11 = bitmap.getPixel(x1, y1)
        
        // Bilinear interpolation per channel
        val r = interpolate2D(
            Color.red(c00), Color.red(c10), Color.red(c01), Color.red(c11),
            dx, dy
        )
        val g = interpolate2D(
            Color.green(c00), Color.green(c10), Color.green(c01), Color.green(c11),
            dx, dy
        )
        val b = interpolate2D(
            Color.blue(c00), Color.blue(c10), Color.blue(c01), Color.blue(c11),
            dx, dy
        )
        val a = interpolate2D(
            Color.alpha(c00), Color.alpha(c10), Color.alpha(c01), Color.alpha(c11),
            dx, dy
        )
        
        return Color.argb(a, r, g, b)
    }
    
    private fun interpolate2D(v00: Int, v10: Int, v01: Int, v11: Int, dx: Float, dy: Float): Int {
        val v0 = v00 * (1f - dx) + v10 * dx
        val v1 = v01 * (1f - dx) + v11 * dx
        return (v0 * (1f - dy) + v1 * dy).toInt()
    }
}
```

---

## 9. ColorDrop (Smart Fill)

### Interactive Threshold Adjustment

```kotlin
class ColorDropTool {
    
    private var currentFill: Bitmap? = null
    private var currentThreshold: Float = 0.5f
    
    fun beginColorDrop(
        sourceBitmap: Bitmap,
        seedPoint: Point,
        fillColor: Int,
        initialThreshold: Float = 0.5f
    ) {
        currentThreshold = initialThreshold
        currentFill = performFill(sourceBitmap, seedPoint, fillColor, currentThreshold)
    }
    
    fun updateThreshold(newThreshold: Float, sourceBitmap: Bitmap, seedPoint: Point, fillColor: Int) {
        currentThreshold = newThreshold
        currentFill = performFill(sourceBitmap, seedPoint, fillColor, currentThreshold)
    }
    
    fun commitFill(): Bitmap? {
        return currentFill
    }
    
    fun cancelFill() {
        currentFill?.recycle()
        currentFill = null
    }
    
    private fun performFill(
        source: Bitmap,
        seedPoint: Point,
        fillColor: Int,
        threshold: Float
    ): Bitmap {
        // Use automatic selection algorithm
        val selection = AutomaticSelectionTool().select(
            source,
            seedPoint,
            threshold * 100f,
            continuous = true,
            antiAlias = true
        )
        
        // Create filled bitmap
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        
        // Fill selected area
        for (y in 0 until result.height) {
            for (x in 0 until result.width) {
                val selectionStrength = selection.getSelectionStrength(x, y)
                
                if (selectionStrength > 0) {
                    val currentColor = result.getPixel(x, y)
                    val blended = blendColors(currentColor, fillColor, selectionStrength)
                    result.setPixel(x, y, blended)
                }
            }
        }
        
        return result
    }
}
```

---

## 10. Transform UI Components

### Transform Handle System

```kotlin
@Composable
fun TransformOverlay(
    bounds: Rect,
    mode: TransformMode,
    onCornerDrag: (Int, PointF) -> Unit,
    onEdgeDrag: (Int, PointF) -> Unit,
    onRotate: (Float) -> Unit,
    onComplete: () -> Unit
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Draw bounding box
        drawRect(
            color = Color.Blue,
            topLeft = Offset(bounds.left, bounds.top),
            size = Size(bounds.width, bounds.height),
            style = Stroke(width = 2f)
        )
        
        when (mode) {
            TransformMode.FREEFORM, TransformMode.UNIFORM -> {
                // 4 corner handles + 4 edge handles + center rotation
                drawCornerHandles(bounds)
                drawEdgeHandles(bounds)
                drawRotationHandle(bounds)
            }
            
            TransformMode.DISTORT -> {
                // 4 corner points only
                drawCornerHandles(bounds)
            }
            
            TransformMode.WARP -> {
                // Mesh grid
                drawWarpMesh(bounds, meshRows = 4, meshCols = 4)
            }
        }
    }
}

fun drawCornerHandles(bounds: Rect) {
    val handleSize = 20f
    val corners = listOf(
        Offset(bounds.left, bounds.top),
        Offset(bounds.right, bounds.top),
        Offset(bounds.right, bounds.bottom),
        Offset(bounds.left, bounds.bottom)
    )
    
    corners.forEach { corner ->
        drawCircle(
            color = Color.White,
            radius = handleSize,
            center = corner,
            style = Fill
        )
        drawCircle(
            color = Color.Blue,
            radius = handleSize,
            center = corner,
            style = Stroke(width = 2f)
        )
    }
}
```

---

## 11. Selection Rendering

### Marching Ants Animation

```kotlin
class SelectionRenderer {
    
    private var animationOffset = 0f
    private val dashLength = 8f
    
    fun drawSelection(
        canvas: Canvas,
        selection: Selection,
        animate: Boolean = true
    ) {
        if (animate) {
            animationOffset = (animationOffset + 1f) % (dashLength * 2)
        }
        
        val paint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
            pathEffect = DashPathEffect(
                floatArrayOf(dashLength, dashLength),
                animationOffset
            )
        }
        
        // Draw selection outline
        val path = createSelectionOutline(selection.mask)
        canvas.drawPath(path, paint)
        
        // Draw white outline underneath for contrast
        paint.color = Color.WHITE
        paint.pathEffect = DashPathEffect(
            floatArrayOf(dashLength, dashLength),
            animationOffset + dashLength
        )
        canvas.drawPath(path, paint)
    }
    
    /**
     * Trace selection boundary using marching squares
     */
    private fun createSelectionOutline(mask: Bitmap): Path {
        val path = Path()
        val visited = Array(mask.height) { BooleanArray(mask.width) }
        
        // Find first selected pixel
        for (y in 0 until mask.height) {
            for (x in 0 until mask.width) {
                if (isSelected(mask, x, y) && !visited[y][x]) {
                    traceBoundary(mask, x, y, path, visited)
                }
            }
        }
        
        return path
    }
    
    private fun traceBoundary(
        mask: Bitmap,
        startX: Int,
        startY: Int,
        path: Path,
        visited: Array<BooleanArray>
    ) {
        // Moore-Neighbor tracing algorithm
        var x = startX
        var y = startY
        var direction = 0  // Start going right
        
        path.moveTo(x.toFloat(), y.toFloat())
        
        do {
            visited[y][x] = true
            
            // Check next pixel in current direction
            val (nextX, nextY) = getNextPixel(x, y, direction)
            
            if (isSelected(mask, nextX, nextY)) {
                x = nextX
                y = nextY
                path.lineTo(x.toFloat(), y.toFloat())
                direction = (direction - 1 + 8) % 8  // Turn left
            } else {
                direction = (direction + 1) % 8  // Turn right
            }
            
        } while (x != startX || y != startY)
        
        path.close()
    }
}
```

---

## 12. Copy/Paste with Selection

### Selection-Based Operations

```kotlin
class SelectionClipboard {
    
    fun copy(layer: Layer, selection: Selection): Bitmap {
        val bounds = selection.bounds
        
        val copied = Bitmap.createBitmap(
            bounds.width.toInt(),
            bounds.height.toInt(),
            Bitmap.Config.ARGB_8888
        )
        
        // Copy pixels within selection
        for (y in 0 until copied.height) {
            for (x in 0 until copied.width) {
                val srcX = bounds.left.toInt() + x
                val srcY = bounds.top.toInt() + y
                
                val strength = selection.getSelectionStrength(srcX, srcY)
                
                if (strength > 0) {
                    val pixel = layer.bitmap.getPixel(srcX, srcY)
                    
                    // Apply selection strength to alpha
                    val alpha = Color.alpha(pixel)
                    val adjustedAlpha = (alpha * strength).toInt()
                    
                    copied.setPixel(
                        x, y,
                        Color.argb(
                            adjustedAlpha,
                            Color.red(pixel),
                            Color.green(pixel),
                            Color.blue(pixel)
                        )
                    )
                }
            }
        }
        
        return copied
    }
    
    fun cut(layer: Layer, selection: Selection): Bitmap {
        val copied = copy(layer, selection)
        
        // Clear selected area
        val canvas = Canvas(layer.bitmap)
        val paint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        
        // Clear with selection mask
        for (y in 0 until layer.bitmap.height) {
            for (x in 0 until layer.bitmap.width) {
                val strength = selection.getSelectionStrength(x, y)
                
                if (strength > 0) {
                    paint.alpha = (strength * 255).toInt()
                    canvas.drawPoint(x.toFloat(), y.toFloat(), paint)
                }
            }
        }
        
        return copied
    }
    
    fun paste(clipboard: Bitmap, position: PointF): Layer {
        // Create new layer with clipboard content
        val layer = Layer.create(
            clipboard.width,
            clipboard.height,
            "Pasted"
        )
        
        val canvas = Canvas(layer.bitmap)
        canvas.drawBitmap(clipboard, position.x, position.y, null)
        
        return layer
    }
}
```

---

## 13. Fill Tool (Bucket)

### Flood Fill with Preview

```kotlin
class FillTool {
    
    fun fill(
        targetBitmap: Bitmap,
        seedPoint: Point,
        fillColor: Int,
        threshold: Float,
        contiguous: Boolean = true,
        antiAlias: Boolean = true
    ): Bitmap {
        // Use selection algorithm to find fill area
        val selection = AutomaticSelectionTool().select(
            targetBitmap,
            seedPoint,
            threshold * 100f,
            contiguous,
            antiAlias
        )
        
        // Fill the selected area
        val result = targetBitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        for (y in 0 until result.height) {
            for (x in 0 until result.width) {
                val strength = selection.getSelectionStrength(x, y)
                
                if (strength > 0) {
                    val currentColor = result.getPixel(x, y)
                    val blended = blendColors(currentColor, fillColor, strength)
                    result.setPixel(x, y, blended)
                }
            }
        }
        
        return result
    }
    
    /**
     * Preview fill without committing
     */
    fun previewFill(
        targetBitmap: Bitmap,
        seedPoint: Point,
        fillColor: Int,
        threshold: Float
    ): Bitmap {
        return fill(targetBitmap, seedPoint, fillColor, threshold)
    }
}
```

---

## 14. Transform Interpolation Quality

### Bicubic Interpolation (Highest Quality)

```kotlin
class BicubicInterpolation {
    
    fun sample(bitmap: Bitmap, x: Float, y: Float): Int {
        val x0 = floor(x).toInt()
        val y0 = floor(y).toInt()
        
        val dx = x - x0
        val dy = y - y0
        
        // Sample 4x4 neighborhood
        val samples = Array(4) { IntArray(4) }
        for (j in 0 until 4) {
            for (i in 0 until 4) {
                val sx = (x0 + i - 1).coerceIn(0, bitmap.width - 1)
                val sy = (y0 + j - 1).coerceIn(0, bitmap.height - 1)
                samples[j][i] = bitmap.getPixel(sx, sy)
            }
        }
        
        // Bicubic interpolation per channel
        val r = bicubicInterpolate(samples, dx, dy) { Color.red(it) }
        val g = bicubicInterpolate(samples, dx, dy) { Color.green(it) }
        val b = bicubicInterpolate(samples, dx, dy) { Color.blue(it) }
        val a = bicubicInterpolate(samples, dx, dy) { Color.alpha(it) }
        
        return Color.argb(a, r, g, b)
    }
    
    private fun bicubicInterpolate(
        samples: Array<IntArray>,
        dx: Float,
        dy: Float,
        extract: (Int) -> Int
    ): Int {
        // Extract channel values
        val values = Array(4) { FloatArray(4) }
        for (j in 0 until 4) {
            for (i in 0 until 4) {
                values[j][i] = extract(samples[j][i]).toFloat()
            }
        }
        
        // Cubic interpolation in x direction
        val cols = FloatArray(4)
        for (j in 0 until 4) {
            cols[j] = cubicInterpolate(
                values[j][0], values[j][1], values[j][2], values[j][3],
                dx
            )
        }
        
        // Cubic interpolation in y direction
        val result = cubicInterpolate(cols[0], cols[1], cols[2], cols[3], dy)
        
        return result.toInt().coerceIn(0, 255)
    }
    
    private fun cubicInterpolate(p0: Float, p1: Float, p2: Float, p3: Float, t: Float): Float {
        val a = -0.5f * p0 + 1.5f * p1 - 1.5f * p2 + 0.5f * p3
        val b = p0 - 2.5f * p1 + 2f * p2 - 0.5f * p3
        val c = -0.5f * p0 + 0.5f * p2
        val d = p1
        
        return a * t * t * t + b * t * t + c * t + d
    }
}
```

---

## 15. Selection & Transform UI

### Selection Toolbar

```kotlin
@Composable
fun SelectionToolbar(
    currentTool: SelectionTool,
    selectionActive: Boolean,
    onToolSelect: (SelectionTool) -> Unit,
    onOperationSelect: (SelectionOperation) -> Unit,
    onFeather: (Float) -> Unit,
    onInvert: () -> Unit,
    onDeselect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text("Selection Tools", style = MaterialTheme.typography.titleMedium)
        
        Spacer(Modifier.height(8.dp))
        
        // Tool selection
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SelectionToolButton(
                icon = Icons.Default.AutoAwesome,
                label = "Auto",
                selected = currentTool == SelectionTool.AUTOMATIC,
                onClick = { onToolSelect(SelectionTool.AUTOMATIC) }
            )
            
            SelectionToolButton(
                icon = Icons.Default.Gesture,
                label = "Freehand",
                selected = currentTool == SelectionTool.FREEHAND,
                onClick = { onToolSelect(SelectionTool.FREEHAND) }
            )
            
            SelectionToolButton(
                icon = Icons.Default.CropSquare,
                label = "Rectangle",
                selected = currentTool == SelectionTool.RECTANGLE,
                onClick = { onToolSelect(SelectionTool.RECTANGLE) }
            )
            
            SelectionToolButton(
                icon = Icons.Default.Circle,
                label = "Ellipse",
                selected = currentTool == SelectionTool.ELLIPSE,
                onClick = { onToolSelect(SelectionTool.ELLIPSE) }
            )
        }
        
        if (selectionActive) {
            Spacer(Modifier.height(16.dp))
            Divider()
            Spacer(Modifier.height(16.dp))
            
            // Operations
            Text("Operations", style = MaterialTheme.typography.titleSmall)
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { onOperationSelect(SelectionOperation.ADD) }) {
                    Text("Add")
                }
                Button(onClick = { onOperationSelect(SelectionOperation.SUBTRACT) }) {
                    Text("Subtract")
                }
                Button(onClick = { onOperationSelect(SelectionOperation.INTERSECT) }) {
                    Text("Intersect")
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Feather slider
            var featherValue by remember { mutableStateOf(0f) }
            Text("Feather: ${featherValue.toInt()}px")
            Slider(
                value = featherValue,
                onValueChange = {
                    featherValue = it
                    onFeather(it)
                },
                valueRange = 0f..100f
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onInvert) {
                    Text("Invert")
                }
                OutlinedButton(onClick = onDeselect) {
                    Text("Deselect")
                }
            }
        }
    }
}

enum class SelectionTool {
    AUTOMATIC,
    FREEHAND,
    RECTANGLE,
    ELLIPSE
}

enum class SelectionOperation {
    ADD,
    SUBTRACT,
    INTERSECT,
    INVERT
}
```

---

## 16. Performance Optimization

### Fast Selection Algorithms

```kotlin
class OptimizedFloodFill {
    
    /**
     * Scanline flood fill - much faster than stack-based
     */
    fun floodFill(
        source: Bitmap,
        mask: Bitmap,
        x: Int,
        y: Int,
        threshold: Float
    ) {
        val targetColor = source.getPixel(x, y)
        val width = source.width
        val height = source.height
        
        val stack = ArrayDeque<Int>()  // Store y values
        stack.add(y)
        
        while (stack.isNotEmpty()) {
            val currentY = stack.removeLast()
            
            // Find leftmost matching pixel
            var left = x
            while (left > 0 && colorMatches(source.getPixel(left - 1, currentY), targetColor, threshold)) {
                left--
            }
            
            // Find rightmost matching pixel
            var right = x
            while (right < width - 1 && colorMatches(source.getPixel(right + 1, currentY), targetColor, threshold)) {
                right++
            }
            
            // Fill scanline
            for (scanX in left..right) {
                mask.setPixel(scanX, currentY, 0xFFFFFFFF.toInt())
            }
            
            // Check above and below
            if (currentY > 0) {
                var inSpan = false
                for (scanX in left..right) {
                    val matches = colorMatches(source.getPixel(scanX, currentY - 1), targetColor, threshold)
                    val alreadyFilled = mask.getPixel(scanX, currentY - 1) and 0xFF > 0
                    
                    if (!inSpan && matches && !alreadyFilled) {
                        stack.add(currentY - 1)
                        inSpan = true
                    } else if (inSpan && (!matches || alreadyFilled)) {
                        inSpan = false
                    }
                }
            }
            
            // Same for below (currentY + 1)
            // ...
        }
    }
}
```

---

## 17. Testing Specifications

### Selection Tests

```kotlin
@Test
fun testFloodFill_Contiguous() {
    val bitmap = createTestBitmap(100, 100)
    
    // Draw a red circle
    drawCircle(bitmap, 50, 50, 30, Color.RED)
    
    // Fill from inside circle
    val selection = AutomaticSelectionTool().select(
        bitmap,
        Point(50f, 50f, 0f),
        threshold = 10f,
        continuous = true
    )
    
    // Verify selection matches circle
    assertTrue(selection.isSelected(50, 50))
    assertTrue(selection.isSelected(45, 50))
    assertFalse(selection.isSelected(10, 10))  // Outside
}

@Test
fun testFeathering_EdgeSoftness() {
    val selection = createRectangleSelection(100, 100, Rect(20f, 20f, 80f, 80f))
    
    // No feather - hard edge
    assertEquals(1f, selection.getSelectionStrength(21, 21))
    assertEquals(0f, selection.getSelectionStrength(19, 19))
    
    // With feather
    val feathered = SelectionFeathering().feather(selection, 10f)
    
    // Edge should be soft
    val edgeStrength = feathered.getSelectionStrength(19, 19)
    assertTrue(edgeStrength > 0f && edgeStrength < 1f)
}

@Test
fun testTransform_PreservesQuality() {
    val source = createTestBitmap(100, 100)
    val transform = FreeformTransform()
    
    transform.beginTransform(source)
    transform.scale(2f, 2f)
    val result = transform.applyTransform()
    
    // Verify size doubled
    assertEquals(200, result.width)
    assertEquals(200, result.height)
    
    // Verify no severe quality loss (compare histograms)
    val qualityScore = compareImageQuality(source, result)
    assertTrue(qualityScore > 0.8f)
}
```

---

## 18. Implementation Roadmap

### Phase 1: Basic Selection (Month 1-2)

- [ ] Rectangle selection
- [ ] Ellipse selection
- [ ] Selection rendering (marching ants)
- [ ] Copy/paste
- [ ] Delete selection
- [ ] Deselect
- [ ] Invert selection

### Phase 2: Advanced Selection (Month 3-4)

- [ ] Automatic selection (flood fill)
- [ ] Threshold adjustment UI
- [ ] Freehand selection
- [ ] Selection operations (add, subtract, intersect)
- [ ] Feathering
- [ ] Grow/shrink selection
- [ ] ColorDrop with threshold slider

### Phase 3: Transform Tools (Month 5-6)

- [ ] Freeform transform
- [ ] Uniform transform (maintain aspect)
- [ ] Transform preview
- [ ] Snapping guides
- [ ] Angle snapping (45°, 90°)
- [ ] Bilinear interpolation
- [ ] Bicubic interpolation (quality mode)

### Phase 4: Advanced Transform (Month 7-9)

- [ ] Distort (perspective)
- [ ] Warp (mesh-based)
- [ ] Interactive mesh editing UI
- [ ] Multi-layer transform
- [ ] Transform with selection mask
- [ ] Liquify tool (6 deformation modes)

---

## 19. UI/UX Design

### Selection Mode Interface

```
┌────────────────────────────────────────┐
│  [Auto][✋][□][○]    Threshold: 45%    │  ← Tool selection + settings
├────────────────────────────────────────┤
│                                        │
│   ╔════════════════╗                  │  ← Marching ants
│   ║                ║                  │
│   ║   SELECTED     ║                  │
│   ║     AREA       ║                  │
│   ║                ║                  │
│   ╚════════════════╝                  │
│                                        │
├────────────────────────────────────────┤
│  [Add] [Subtract] [Intersect]         │  ← Operations
│  Feather: [▬▬▬○──────] 15px           │
│  [Copy] [Cut] [Paste] [Deselect]      │  ← Actions
└────────────────────────────────────────┘
```

### Transform Mode Interface

```
┌────────────────────────────────────────┐
│  Transform: [Free▾] ✓ Snap  ✓ Aspect  │
├────────────────────────────────────────┤
│                                        │
│         ○────────────○                 │
│         │            │                 │
│         │            │                 │
│         │     ⟲      │                 │  ← Rotation handle
│         │            │                 │
│         ○────────────○                 │
│                                        │
├────────────────────────────────────────┤
│  Angle: 45°    Scale: 150%            │  ← Info
│  [Apply] [Cancel]                      │  ← Actions
└────────────────────────────────────────┘
```

---

## 20. Critical Features Summary

### Must-Have for Professional Use

| Feature | Why Critical | Priority |
|---------|--------------|----------|
| **Rectangle selection** | Basic masking | P0 |
| **Automatic selection** | Color-based isolation | P0 |
| **Copy/paste** | Layer management | P0 |
| **Freeform transform** | Resize artwork | P0 |
| **Feathering** | Professional edges | P1 |
| **Selection operations** | Complex masks | P1 |
| **Distort transform** | Perspective fixes | P2 |
| **Warp** | Advanced distortion | P3 |

### Performance Targets

| Operation | Target Time | Critical? |
|-----------|-------------|-----------|
| Automatic selection (2K canvas) | < 500ms | Yes |
| Freehand selection (smooth) | 60 FPS | Yes |
| Transform preview | 30 FPS | Yes |
| Feathering | < 200ms | No |
| Copy/paste | < 100ms | Yes |

---

## Summary

This specification provides:
- ✅ Complete selection data model
- ✅ Flood fill algorithm (automatic selection)
- ✅ Path-based selection (freehand)
- ✅ Selection operations (add, subtract, intersect)
- ✅ Feathering and anti-aliasing
- ✅ All transform modes (freeform, uniform, distort, warp)
- ✅ High-quality interpolation algorithms
- ✅ ColorDrop implementation
- ✅ UI/UX patterns
- ✅ Performance targets

**Implementation Priority**:
1. Rectangle selection (simplest, immediately useful)
2. Copy/paste/delete (essential workflow)
3. Freeform transform (critical for resizing)
4. Automatic selection (major feature)
5. Feathering (quality improvement)
6. Advanced selection operations
7. Distort and warp transforms

This gives your daughter professional selection and transform capabilities on par with Procreate!
