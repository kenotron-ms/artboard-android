# Procreate Feature Analysis & Technical Specifications

This document provides a comprehensive analysis of Procreate's features to guide Artboard's development toward professional-grade capability.

---

## Executive Summary

Procreate's dominance comes from three pillars:
1. **Performance**: 120 FPS drawing, < 20ms stylus latency
2. **Professional Tools**: Layer system, brush engine, selection tools match desktop apps
3. **Intuitive UX**: Gesture-driven, no nested menus, immediate access to tools

**Goal for Artboard**: Match 80% of features at 100% of the performance to create a viable Procreate alternative.

---

## 1. Layer System Architecture

### Layer Types

| Type | Purpose | Implementation |
|------|---------|----------------|
| **Paint Layer** | Standard raster drawing | Bitmap with blend mode + opacity |
| **Text Layer** | Vector text (rasterizes on merge) | TextLayout until flattened |
| **Group Layer** | Organize layers into folders | Container with own blend/opacity |
| **Reference Layer** | Non-exported guide layer | Standard layer with export flag |

### Blend Modes (27 Total)

**Category: Darken** (6 modes)
- Normal, Darken, Multiply, Color Burn, Linear Burn, Darker Color

**Category: Lighten** (6 modes)  
- Screen, Lighten, Color Dodge, Add (Linear Dodge), Lighter Color

**Category: Contrast** (6 modes)
- Overlay, Soft Light, Hard Light, Vivid Light, Linear Light, Pin Light

**Category: Difference** (3 modes)
- Difference, Exclusion, Subtract

**Category: HSL** (4 modes)
- Hue, Saturation, Color, Luminosity

**Category: Alpha** (2 modes)
- Alpha Lock, Clipping Mask (not blend modes, layer properties)

### Layer Properties

| Property | Range | Default | Technical Notes |
|----------|-------|---------|-----------------|
| **Opacity** | 0-100% | 100% | Applied during composite |
| **Blend Mode** | 27 modes | Normal | Porter-Duff + W3C specs |
| **Visible** | on/off | on | Skip in composite |
| **Locked** | on/off | off | Prevent edits |
| **Alpha Lock** | on/off | off | Paint only in existing pixels |
| **Clipping Mask** | on/off | off | Mask to layer below |
| **Name** | String | "Layer N" | User-editable |

### Layer Operations

| Operation | Description | Implementation |
|-----------|-------------|----------------|
| **Duplicate** | Copy layer with all properties | Bitmap.copy() + metadata |
| **Clear** | Delete all pixels | Fill with transparent |
| **Fill** | Solid color fill | drawColor() |
| **Invert** | Invert colors | ColorMatrix.invert() |
| **Merge Down** | Combine with layer below | Composite → new layer |
| **Flatten** | All layers → single layer | Full composite |
| **Combine Down** | Merge preserving blend mode | Pre-blend then merge |
| **Rasterize** | Text/vector → bitmap | Already done for text |

### Memory Calculation

```
maxLayers = availableRAM / (canvasWidth × canvasHeight × 4)

Example:
- Device RAM: 8GB
- Available: ~4GB for app
- Canvas: 2048×2048 pixels
- Bytes per layer: 2048 × 2048 × 4 = 16MB
- Max layers: 4000MB / 16MB = 250 layers
```

**Optimization**: Compress inactive layers, tile-based rendering for huge canvases

---

## 2. Brush Engine Specification

### Core Brush Attributes (14 Categories)

#### 2.1 Stroke Path
| Property | Range | Effect |
|----------|-------|--------|
| Spacing | 0-300% | Distance between stamps (% of brush size) |
| StreamLine | 0-100% | Stroke smoothing/stabilization |
| Jitter | 0-100% | Random position offset |
| Fall-off | 0-100% | Pressure tapering at stroke ends |

#### 2.2 Taper
| Property | Range | Effect |
|----------|-------|--------|
| Pressure | On/Off | Taper based on pressure |
| Tip | 0-100% | Fade at stroke start |
| End | 0-100% | Fade at stroke end |
| Classic Taper | On/Off | Traditional calligraphy taper |

#### 2.3 Shape
| Property | Range | Effect |
|----------|-------|--------|
| Shape Source | Brush/Grain | Circle, image, or custom |
| Scatter | 0-100% | Random perpendicular offset |
| Rotation | 0-360° | Stamp rotation |
| Randomized | On/Off | Random rotation per stamp |
| Azimuth | On/Off | Follow stylus orientation |
| Count | 1-16 | Multiple stamps per point |
| Stamp Preview | Image | Visual reference |

#### 2.4 Grain
| Property | Range | Effect |
|----------|-------|--------|
| Grain Source | Image | Texture to apply |
| Scale | 1-300% | Texture size |
| Zoom | 1-200% | Texture detail level |
| Blend Mode | 8 modes | How grain mixes with color |
| Depth | 0-100% | Grain intensity |
| Movement | Moving/Texturized | Moves with stroke or fixed |
| Filtering | None/Classic | Anti-aliasing mode |

#### 2.5 Rendering
| Property | Options | Effect |
|----------|---------|--------|
| Rendering Mode | 6 modes | Blend algorithm |
| Opacity | 0-100% | Base opacity |
| Flow | 0-100% | Paint buildup per stamp |
| Blend Mode | 27 modes | Layer interaction |

**Rendering Modes**:
1. **Light Glaze** - Minimal buildup, transparent
2. **Glazed** - Light buildup
3. **Uniform** - Even opacity (default)
4. **Intense Glaze** - Medium buildup
5. **Heavy Glaze** - Strong buildup
6. **Intense Blending** - Maximum paint mixing

#### 2.6 Wet Mix (Advanced)
| Property | Range | Effect |
|----------|-------|--------|
| Dilution | 0-100% | Paint transparency/wetness |
| Charge | 0-100% | Paint volume/load |
| Attack | 0-100% | Initial paint amount |
| Pull | 0-100% | Color blending from canvas |
| Gradient | On/Off | Gradual color mixing |

#### 2.7 Color Dynamics
| Property | Range | Effect |
|----------|-------|--------|
| Hue Jitter | 0-100% | Random hue variation |
| Saturation Jitter | 0-100% | Random saturation variation |
| Brightness Jitter | 0-100% | Random brightness variation |
| Color Pressure | On/Off | Pressure affects jitter |
| Stamp Color Jitter | On/Off | Per-stamp vs per-stroke variation |

#### 2.8 Apple Pencil Dynamics
| Property | Options | Effect |
|----------|---------|--------|
| Pressure | Curve graph | Custom pressure response |
| Tilt | 0-100% sensitivity | Tilt affects size/opacity |
| Opacity | Curve graph | Pressure → opacity mapping |
| Size | Curve graph | Pressure → size mapping |
| Bleed | 0-100% | Size increase with pressure |
| Min Size | 0-100% | Minimum size at zero pressure |
| Min Opacity | 0-100% | Minimum opacity at zero pressure |

### Brush Presets Library

Procreate ships with **200+ brushes** in 12 sets:

| Set | Count | Key Brushes |
|-----|-------|-------------|
| **Sketching** | 17 | HB Pencil, 6B Pencil, Technical Pen |
| **Inking** | 17 | Studio Pen, Dry Ink, Syrup |
| **Drawing** | 9 | Bonobo Chalk, Soft Pastel, Charcoal |
| **Painting** | 19 | Acrylic, Oil Paint, Gouache |
| **Artistic** | 18 | Burnt Tree, Salamanca, Abstract |
| **Calligraphy** | 10 | Monoline, Script, Brush Pen |
| **Airbrushing** | 7 | Soft Brush, Hard Brush, Medium Brush |
| **Textures** | 19 | Noise Brush, Grunge, Rusted Decay |
| **Luminance** | 13 | Nebula, Flare, Light Pen |
| **Industrial** | 10 | Rusty, Metal, Technical |
| **Organic** | 9 | Tar, Water, Leaks |
| **Touch-ups** | 9 | Smudge Tool variants |

**Implementation Priority**: Start with 10 essential brushes covering all categories, expand to 50+ in phase 2.

---

## 3. Selection & Transform System

### Selection Tools

#### 3.1 Automatic Selection
- **Algorithm**: Flood fill with color threshold
- **Parameters**:
  - Threshold: 0-100% (color similarity)
  - Continuous: On/Off (contiguous pixels only)
  - Preview: Real-time boundary visualization
- **Implementation**: Queue-based flood fill, color distance in LAB space

#### 3.2 Freehand Selection
- **Input**: Touch/stylus path
- **Smoothing**: Catmull-Rom spline (same as brush strokes)
- **Closure**: Auto-close on release
- **Feathering**: Gaussian blur on mask edge

#### 3.3 Rectangle/Ellipse Selection
- **Creation**: Drag to define bounds
- **Constraints**: Hold shift for square/circle
- **Modification**: 8-point resize handles

### Selection Operations

| Operation | Algorithm | Use Case |
|-----------|-----------|----------|
| **Add** | Union of masks | Extend selection |
| **Subtract** | Difference | Remove from selection |
| **Intersect** | AND operation | Refine selection |
| **Invert** | NOT operation | Select opposite |
| **Feather** | Gaussian blur | Soft edges |
| **Clear** | Delete selected pixels | Remove content |
| **Fill** | Paint in mask | Color region |
| **Copy/Paste** | New layer from selection | Duplicate content |

### Transform Tools

#### 3.3 Transform Modes

**Freeform**
- Free scaling, rotation, translation
- 4 corner handles + 4 edge handles + center rotation
- Bilinear interpolation (default)

**Uniform**
- Proportional scaling only
- Maintains aspect ratio
- Uses Lanczos resampling for quality

**Distort**
- 4-point perspective transform
- Homography matrix (3×3)
- Use case: Straighten photos, perspective correction

**Warp**
- Mesh-based deformation (typically 4×4 grid)
- Drag mesh points for localized warping
- Bilinear interpolation per quad

#### 3.4 Transform Features

| Feature | Description | Implementation |
|---------|-------------|----------------|
| **Snapping** | Magnetic guides at 45°, 90° | Angular threshold (±2°) |
| **Magnetics** | Snap to edges/center | Distance threshold |
| **Interpolation** | Nearest/Bilinear/Bicubic | Quality vs speed |
| **Preview** | Real-time transform | Render to temp buffer |
| **Multi-layer** | Transform selection across layers | Process each layer bitmap |

---

## 4. Gesture System

### Two-Finger Gestures

| Gesture | Action | Technical |
|---------|--------|-----------|
| **Tap** | Undo | Multi-touch event, count=2, duration<200ms |
| **Pinch** | Zoom canvas | Scale factor from touch distance |
| **Rotate** | Rotate canvas | Angle from touch vector |
| **Pan** | Move canvas | Delta from touch midpoint |
| **Long-press** | QuickMenu | Touch hold >500ms |

### Three-Finger Gestures

| Gesture | Action | Technical |
|---------|--------|-----------|
| **Swipe Down** | Copy/Paste menu | Velocity detection |
| **Swipe Left** | Redo | Direction + velocity |
| **Swipe Right** | Undo | Direction + velocity |
| **Pinch** | Clear layer | Pinch with 3 touches |

### Four-Finger Gestures

| Gesture | Action |
|---------|--------|
| **Tap** | Toggle full-screen mode |

### Palm Rejection

- **Algorithm**: Largest touch area = palm, ignore it
- **Stylus priority**: If stylus detected, ignore all finger touches
- **Threshold**: Touch area > 20mm² = palm

---

## 5. Color System

### Color Pickers (4 Types)

#### 5.1 Disc Picker (Primary)
- **Outer Ring**: Hue (0-360°)
- **Inner Triangle**: Saturation (horizontal) + Value (vertical)
- **HSV Color Space**: Intuitive for artists
- **Implementation**: Polar coordinates for ring, barycentric for triangle

#### 5.2 Classic Picker
- **Sliders**: RGB or HSB
- **Hex Input**: #RRGGBB
- **Implementation**: Standard slider controls

#### 5.3 Value Picker
- **Numeric Input**: R/G/B or H/S/B values
- **Precision**: 0-255 or 0-100%

#### 5.4 Palettes
- **Storage**: 30 colors per palette
- **Management**: Create, duplicate, import, export
- **Format**: JSON with RGB/HSB values

### Color Features

| Feature | Description |
|---------|-------------|
| **Color History** | Last 10 colors used (circular buffer) |
| **Eyedropper** | Long-press canvas to sample color |
| **ColorDrop** | Drag color to area, flood-fill with threshold |
| **Continuous Fill** | Adjust threshold in real-time before releasing |

### Color Harmony System

| Mode | Algorithm | Use Case |
|------|-----------|----------|
| **Complementary** | Hue + 180° | Contrasting colors |
| **Split Complementary** | Hue ± 150° | Balanced contrast |
| **Analogous** | Hue ± 30° | Harmonious palette |
| **Triadic** | Hue + 120°, 240° | Vibrant balance |
| **Tetradic** | Hue + 90°, 180°, 270° | Rich palette |

---

## 6. Brush Engine Deep Dive

### Rendering Pipeline

```
Input Event (x, y, pressure, tilt, azimuth)
    ↓
Stroke Interpolation (Catmull-Rom splines)
    ↓
Calculate Stamp Positions (based on Spacing)
    ↓
For each stamp:
    ↓
    Apply Dynamics:
    - Size (pressure curve, tilt, velocity)
    - Opacity (pressure curve, flow)
    - Rotation (azimuth, random, angle)
    - Scatter (perpendicular offset)
    ↓
    Render Stamp:
    - Shape mask (circle, custom image)
    - Apply Grain texture
    - Color dynamics (jitter)
    - Blend with canvas (blend mode, rendering mode)
    ↓
Composite to Layer Bitmap
```

### Critical Implementation Details

**StreamLine (Stabilization)**
- Algorithm: Weighted average of last N points
- Parameters: Weight curve, distance threshold
- Effect: Reduces jitter, smoother curves
- Implementation: Moving average filter with adaptive window

**Grain System**
- **Moving Grain**: Texture follows brush path
  - UV coordinates based on stroke path
  - Texture scrolls with movement
- **Texturized Grain**: Fixed texture overlay
  - Canvas-space UV coordinates
  - Texture stays stationary

**Wet Mix System** (Advanced)
- Simulates paint mixing on canvas
- Samples existing canvas color
- Blends with brush color based on Pull parameter
- Charge affects paint volume (opacity over strokes)
- Most complex feature - implement in Phase 3+

### Performance Targets

| Metric | Target | Strategy |
|--------|--------|----------|
| **Frame Rate** | 120 FPS | GPU shaders, stamp caching |
| **Latency** | < 20ms | Predictive rendering, low-level APIs |
| **Brush Complexity** | 100+ params | Pre-compute lookup tables |
| **Large Canvas** | 8K support | Tiled rendering, viewport culling |

---

## 7. Effects & Adjustments

### Color Adjustments

#### 7.1 Hue/Saturation/Brightness
- **Algorithm**: RGB → HSV → adjust → RGB
- **Parameters**: 
  - Hue: -180° to +180°
  - Saturation: -100% to +100%
  - Brightness: -100% to +100%

#### 7.2 Curves
- **Interface**: Drag curve in histogram
- **Implementation**: 
  - Lookup table (LUT) with 256 entries
  - Cubic interpolation between control points
  - Separate curves for RGB or combined
  - Real-time preview

#### 7.3 Color Balance
- **Shadows/Midtones/Highlights** independently adjustable
- **Cyan-Red, Magenta-Green, Yellow-Blue** sliders
- **Algorithm**: Luminosity-based masking + color shift

### Blur Effects

#### 7.4 Gaussian Blur
- **Algorithm**: Separable 2D convolution
- **Kernel Size**: 1-100px radius
- **Optimization**: Two 1D passes (horizontal + vertical)
- **GPU**: Shader-based for real-time

#### 7.5 Motion Blur
- **Direction**: 0-360°
- **Distance**: 1-100px
- **Algorithm**: Directional blur kernel

#### 7.6 Perspective Blur
- **4-point control**: Define depth gradient
- **Algorithm**: Variable kernel size based on distance

### Liquify Tool

**6 Deformation Modes**:
1. **Push** - Displace pixels forward
2. **Twirl Right/Left** - Circular rotation
3. **Pinch** - Contract toward center
4. **Expand** - Push away from center
5. **Reconstruct** - Restore original

**Parameters**:
- Pressure: 0-100%
- Size: 10-500px
- Distortion: 0-100%
- Momentum: 0-100% (continues after release)

**Algorithm**: 
- Mesh-based displacement
- Store displacement vectors
- Real-time preview with overlay
- Apply on commit

---

## 8. Animation System

### Frame Structure

- **Concept**: Each layer = potential animation frame
- **Groups**: Layers can be grouped as frames
- **Onion Skinning**: See previous/next frames while drawing

### Animation Settings

| Property | Range | Description |
|----------|-------|-------------|
| **Frames per Second** | 1-60 | Playback speed |
| **Onion Frames** | 0-5 | Frames shown before/after |
| **Onion Opacity** | 0-100% | Visibility of onion skins |
| **Onion Color** | Color | Tint for previous/next frames |
| **Loop** | On/Off | Continuous playback |
| **Ping-Pong** | On/Off | Reverse playback |

### Animation Assist Features

- **Frame Durations**: Individual frame hold time
- **Background Layer**: Static layer visible in all frames
- **Foreground Layer**: Overlay on all frames
- **Audio Import**: Sync animation to sound (advanced)

### Export Options

| Format | Features | Use Case |
|--------|----------|----------|
| **Animated GIF** | 256 colors, looping | Web, social media |
| **Animated PNG** | Full color, alpha | High quality web |
| **MP4** | H.264, 1080p+ | Video, Instagram |
| **HEVC** | H.265, smaller size | Modern devices |
| **PNG Sequence** | Individual frames | Further editing |

---

## 9. File Format Specification

### .procreate File Structure

```
artwork.procreate (ZIP archive)
├── Document.archive           # Binary plist with metadata
├── QuickLook/
│   ├── Thumbnail.png         # Gallery preview
│   └── Thumbnail@2x.png
├── video.mp4                 # Time-lapse (if enabled)
├── SilicaDocument            # Main document (binary)
├── 0/                        # Layer 0
│   ├── 0.chunk              # Tile data
│   ├── 1.chunk
│   └── ...
├── 1/                        # Layer 1
└── ...
```

### Document.archive Contents

```xml
<?xml version="1.0"?>
<plist version="1.0">
<dict>
    <key>version</key>
    <integer>5</integer>
    <key>canvasWidth</key>
    <integer>2048</integer>
    <key>canvasHeight</key>
    <integer>2048</integer>
    <key>DPI</key>
    <integer>300</integer>
    <key>colorProfile</key>
    <string>sRGB</string>
    <key>layers</key>
    <array>
        <dict>
            <key>UUID</key>
            <string>layer-uuid</string>
            <key>name</key>
            <string>Background</string>
            <key>opacity</key>
            <real>1.0</real>
            <key>blendMode</key>
            <integer>0</integer>
            <key>visible</key>
            <true/>
        </dict>
    </array>
</dict>
</plist>
```

### Artboard File Format (Simpler)

```
artwork.artboard (ZIP)
├── manifest.json              # JSON metadata
├── thumbnail.png              # Preview
├── layers/
│   ├── layer_0.png           # Layer bitmaps
│   ├── layer_1.png
│   └── ...
└── timelapse.mp4              # Optional time-lapse
```

**manifest.json**:
```json
{
  "version": "1.0",
  "canvas": {
    "width": 2048,
    "height": 2048,
    "backgroundColor": "#FFFFFF",
    "dpi": 300
  },
  "layers": [
    {
      "id": "uuid",
      "name": "Background",
      "filename": "layer_0.png",
      "opacity": 1.0,
      "blendMode": "NORMAL",
      "visible": true,
      "locked": false,
      "alphaLock": false,
      "clippingMask": false
    }
  ],
  "createdAt": "2026-01-21T12:00:00Z",
  "modifiedAt": "2026-01-21T12:30:00Z"
}
```

---

## 10. QuickMenu System

### Customizable Radial Menu

- **Positions**: 4-12 action slots arranged in circle
- **Actions**: Any tool, blend mode, brush, color, etc.
- **Activation**: Two-finger long-press
- **Visual**: Radial segments with icons + labels

### Common QuickMenu Configurations

**Drawing Workflow**:
- Brush library
- Eraser
- Smudge
- Eyedropper
- Layer menu
- Undo/Redo
- Size slider
- Opacity slider

**Painting Workflow**:
- Color palette
- Blend modes
- Liquify
- Adjustments
- Reference toggle
- Symmetry

---

## 11. Advanced Features

### 11.1 Drawing Guides

| Type | Description | Implementation |
|------|-------------|----------------|
| **2D Grid** | Perspective grid with 1-3 vanishing points | Line equation rendering |
| **Symmetry** | Vertical, Horizontal, Quadrant, Radial | Mirror stroke rendering |
| **Drawing Assist** | Snap to guides | Projection to nearest guide line |
| **QuickShape** | Auto-detect and perfect shapes | Shape recognition algorithm |

### 11.2 QuickShape

- **Detection**: Circle, ellipse, rectangle, triangle, line, polygon
- **Algorithm**: 
  - Analyze stroke point distribution
  - Fit geometric primitives
  - Calculate fit score
  - If score > threshold, replace with perfect shape
- **Threshold**: Adjustable sensitivity
- **Preview**: Show perfect shape before committing

### 11.3 Reference Layer

- **Purpose**: Non-printing guide layer
- **Features**: 
  - Adjustable opacity
  - Excluded from export
  - Can import photos as reference
- **Use Cases**: Tracing, composition planning

### 11.4 Text Tool

**Text Properties**:
- Font family, size, weight, style
- Kerning, leading, tracking
- Alignment (left, center, right, justify)
- Color (solid, gradient in Procreate 5X)

**Implementation**:
- Vector text until rasterized
- TextLayout/TextPaint APIs
- Convert to pixels on merge

---

## 12. Performance Optimizations

### Rendering Strategies

| Technique | Description | Impact |
|-----------|-------------|--------|
| **Dirty Rectangles** | Only redraw changed regions | 10x faster updates |
| **Tiled Rendering** | 256×256 tiles, load on demand | Support 16K+ canvases |
| **GPU Shaders** | Metal/OpenGL for brushes + blends | 60-120 FPS |
| **Stamp Caching** | Pre-render brush stamps | 3-5x faster |
| **Layer Compression** | PNG compression for inactive layers | 10x less memory |
| **Undo Deltas** | Store changes only, not full layers | 20x less memory |

### Memory Management

```
Target Memory Budget (4GB device):
- Active Layer: 16MB (2048×2048×4)
- Visible Layers (5): 80MB
- Compressed Layers (50): 100MB
- Undo Stack (50): 200MB
- Brushes + UI: 50MB
- OS Reserve: 1GB
Total: ~1.5GB (safe margin)
```

**Strategies**:
1. Compress layers not being edited
2. Tile large canvases
3. Limit undo depth dynamically
4. Release preview bitmaps when not needed
5. Use 16-bit precision where possible (vs 32-bit)

---

## 13. User Experience Patterns

### Workflow: Digital Painting

1. **Setup**: Create canvas, choose DPI, color profile
2. **Sketch**: Light pencil on bottom layer, low opacity
3. **Lineart**: New layer, ink brush, full opacity
4. **Base Colors**: New layers per element, fill tool
5. **Shading**: Multiply layers, soft brushes
6. **Highlights**: Add/Screen layers, light colors
7. **Details**: Final layer, fine brushes
8. **Merge**: Flatten when satisfied

### Workflow: Photo Editing

1. **Import**: Photo as layer
2. **Duplicate**: Preserve original
3. **Adjustments**: Curves, HSB, color balance
4. **Selection**: Mask specific areas
5. **Effects**: Blur, sharpen, filters
6. **Cleanup**: Clone tool, smudge
7. **Export**: Save as JPEG/PNG

### Workflow: Animation

1. **Setup**: Enable animation assist
2. **Frame 1**: Draw first frame
3. **Duplicate**: Copy layer for frame 2
4. **Onion Skin**: See previous frame while drawing
5. **Iterate**: Add more frames
6. **Preview**: Test playback
7. **Export**: GIF or MP4

---

## 14. Technical Implementation Roadmap

### Phase 1: Core Foundation (Months 1-4)

**Drawing Engine**
- [x] Basic bitmap canvas (DONE)
- [x] Touch/stylus input with pressure (DONE)
- [x] Stroke interpolation (DONE)
- [ ] GPU-accelerated rendering pipeline
- [ ] Predictive rendering for latency reduction
- [ ] 120 FPS target on modern devices

**Layer System**
- [x] Multiple layers (DONE)
- [x] Basic blend modes: Normal, Multiply, Screen (DONE)
- [ ] Add 7 more essential blend modes (Overlay, Add, Darken, Lighten, Color Dodge, Color Burn, Difference)
- [ ] Alpha Lock
- [ ] Clipping Masks
- [ ] Layer groups

**Brush System**
- [x] 5 basic brushes (DONE)
- [ ] Pressure curve customization
- [ ] Tilt support
- [ ] Grain/texture system
- [ ] 15 essential brushes (covering Sketching, Inking, Painting)

**UI/UX**
- [x] Toolbar with basic controls (DONE)
- [x] Color picker - disc mode (DONE)
- [x] Layer panel with visibility (DONE - basic)
- [ ] Two-finger gestures (undo, zoom, pan, rotate)
- [ ] Canvas rotation
- [ ] Eyedropper (long-press)

**Project Management**
- [ ] Save/load projects (.artboard format)
- [ ] Export PNG/JPEG
- [ ] Gallery view with thumbnails
- [ ] Recent projects

**Target**: Fully usable for digital sketching and painting

---

### Phase 2: Essential Professional Tools (Months 5-7)

**Selection Tools**
- [ ] Automatic selection (flood fill)
- [ ] Freehand selection
- [ ] Rectangle/Ellipse selection
- [ ] Selection operations (add, subtract, invert)
- [ ] Feathering

**Transform Tools**
- [ ] Freeform transform
- [ ] Uniform transform (maintain aspect)
- [ ] Snapping guides
- [ ] Bilinear/Bicubic interpolation

**Color Tools**
- [ ] Color harmony picker
- [ ] ColorDrop with threshold adjustment
- [ ] Color history (last 10)
- [ ] Custom palettes

**More Brushes**
- [ ] 30+ brushes (complete Sketching, Inking, Painting sets)
- [ ] Basic brush customization (size, opacity, flow, spacing)
- [ ] Import/export brush presets

**Effects**
- [ ] Gaussian Blur
- [ ] Sharpen
- [ ] HSB Adjustments
- [ ] Curves adjustment

**File Format**
- [ ] Import PSD (with layers)
- [ ] Export PSD (with layers)
- [ ] TIFF support

**Target**: Complete toolset for professional illustration work

---

### Phase 3: Advanced Features (Months 8-11)

**Brush Engine Advanced**
- [ ] Complete 14-attribute brush system
- [ ] Brush Studio UI (full customization)
- [ ] Wet Mix simulation
- [ ] Color dynamics
- [ ] Shape designer
- [ ] Grain designer
- [ ] 100+ brush library

**Layer System Advanced**
- [ ] Reference layers
- [ ] Text layers
- [ ] Layer masks
- [ ] Layer effects (drop shadow, glow, etc.)

**Advanced Selection**
- [ ] Magic Wand
- [ ] Grow/Shrink selection
- [ ] Smooth selection
- [ ] Color range selection

**Transform Advanced**
- [ ] Distort (perspective)
- [ ] Warp (mesh-based)
- [ ] Liquify tool
- [ ] Multi-layer transform

**Effects Suite**
- [ ] Motion Blur
- [ ] Perspective Blur
- [ ] Clone tool
- [ ] Noise filter
- [ ] Color balance
- [ ] Recolor

**Gestures**
- [ ] Three-finger gestures
- [ ] QuickMenu customization
- [ ] Gesture customization

**Target**: Feature parity with Procreate for 90% of workflows

---

### Phase 4: Professional & Polish (Months 12-15)

**Animation**
- [ ] Animation assist
- [ ] Onion skinning
- [ ] Timeline/frame management
- [ ] Export GIF, MP4, APNG

**Drawing Guides**
- [ ] 2D Grid (1-3 vanishing points)
- [ ] Symmetry (vertical, horizontal, radial)
- [ ] Drawing Assist (snap to guides)
- [ ] QuickShape (auto-detect geometric shapes)

**Advanced Effects**
- [ ] Bloom
- [ ] Glitch
- [ ] Halftone
- [ ] Chromatic Aberration
- [ ] Perspective blur

**Project Management**
- [ ] Cloud sync (Google Drive, Dropbox)
- [ ] Version history
- [ ] Stack organization
- [ ] Search and filter

**Professional**
- [ ] CMYK color mode
- [ ] Display P3 color profile
- [ ] Brush marketplace/sharing
- [ ] Time-lapse recording
- [ ] Custom canvas templates

**Target**: Competitive with Procreate, unique Android advantages

---

## 15. Key Differentiators for Artboard

### Advantages Android Can Have

1. **Open Ecosystem**
   - Import/export more formats
   - Integration with desktop apps (Photoshop, Krita, GIMP)
   - File system access (SD card, USB)

2. **Customization**
   - Themes and UI customization
   - Gesture remapping
   - Toolbar configuration
   - Plugin system (future)

3. **Hardware Diversity**
   - Support multiple stylus types (S-Pen, Wacom, generic)
   - Adaptive UI for different screen sizes
   - Performance scaling for hardware range

4. **AI Features** (Future)
   - AI-powered brush recommendations
   - Auto-colorization
   - Background removal
   - Style transfer
   - Smart selection refinement

5. **Price**
   - **Free, open-source** or very affordable
   - No subscription (unlike Adobe)
   - No platform lock-in

---

## 16. Development Dependencies

### Android Libraries Required

| Library | Purpose | Version |
|---------|---------|---------|
| **Jetpack Compose** | UI framework | 1.6+ |
| **Kotlin Coroutines** | Async operations | 1.7+ |
| **Room** | Database (projects) | 2.6+ |
| **DataStore** | Settings persistence | 1.0+ |
| **CameraX** | Photo import | 1.3+ |
| **ExoPlayer** | Video export | 2.19+ |
| **OpenGL ES** | GPU rendering | 3.0+ |
| **RenderScript** | Image processing | Deprecated, migrate to Vulkan |
| **Vulkan** | Modern GPU API | 1.1+ |

### Custom Components Needed

1. **CanvasRenderer** (OpenGL/Vulkan)
   - Hardware-accelerated drawing surface
   - Shader management
   - Texture atlas for brushes

2. **BrushCompiler**
   - Convert brush parameters → GPU shader
   - Stamp texture generation
   - LUT pre-computation

3. **LayerCompositor**
   - Blend mode shaders (27 modes)
   - Multi-pass rendering
   - Opacity handling

4. **StrokeOptimizer**
   - Point reduction (Douglas-Peucker)
   - Bezier fitting
   - Velocity smoothing

5. **FileHandler**
   - ZIP compression/decompression
   - PSD parser (read/write)
   - PNG optimization

---

## 17. Testing Strategy

### Performance Benchmarks

| Test | Target | Measurement |
|------|--------|-------------|
| **Touch Latency** | < 20ms | Input event → pixel drawn |
| **Frame Rate** | 120 FPS | During continuous drawing |
| **Brush Complexity** | 60 FPS | With all dynamics enabled |
| **Layer Switching** | < 100ms | Switch between 50 layers |
| **Undo** | < 50ms | Reverse last action |
| **Save Project** | < 2s | 50 layers, 2K canvas |
| **Export PNG** | < 1s | Flatten and save |

### Quality Benchmarks

1. **Brush Accuracy**: Compare with Procreate side-by-side
2. **Blend Mode Accuracy**: Match Photoshop/W3C standards
3. **PSD Compatibility**: Roundtrip test (export → Photoshop → import)
4. **Memory Stability**: 4-hour drawing session without crash
5. **Precision**: Sub-pixel accuracy for smooth curves

### Device Coverage

- **High-end**: Samsung Galaxy Tab S9 (S-Pen)
- **Mid-range**: Lenovo Tab P11 Pro
- **Budget**: Amazon Fire HD (without stylus)
- **Screen sizes**: 8" to 12.9"
- **Android versions**: 10, 11, 12, 13, 14

---

## 18. Success Metrics

### User Satisfaction
- Drawing feels "as good as Procreate"
- Professional artists can complete full illustrations
- No feature blockers for common workflows

### Technical Metrics
- 120 FPS on flagship devices
- 60 FPS on mid-range devices
- 4K canvas with 100+ layers
- < 100MB APK size
- < 500MB memory usage typical

### Feature Completeness
- 80% of Procreate features by Phase 3
- 100% of essential workflows supported
- Unique Android advantages

---

## Next Steps for Artboard

1. **Review this specification** - Identify must-have features for your daughter
2. **Prioritize Phase 1 features** - What needs to be added to current MVP?
3. **Create detailed design docs** - Layer system, brush engine, file format
4. **Begin implementation** - Phased approach, test frequently with your daughter
5. **Iterate based on feedback** - Real users (your daughter!) guide priorities

This specification provides a **complete blueprint** for building a Procreate-quality Android app. The phased approach ensures you can ship value quickly while building toward full feature parity.

**Ready to build the real thing?** 🎨
