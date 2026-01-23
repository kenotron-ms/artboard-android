package com.artboard.ui.color

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*

/**
 * ViewModel for managing color picker state.
 * Handles HSB values, recent colors, and color swap functionality.
 */
class ColorPickerViewModel : ViewModel() {
    // HSB state
    private val _hue = MutableStateFlow(0f)
    val hue: StateFlow<Float> = _hue.asStateFlow()
    
    private val _saturation = MutableStateFlow(1f)
    val saturation: StateFlow<Float> = _saturation.asStateFlow()
    
    private val _brightness = MutableStateFlow(1f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()
    
    private val _alpha = MutableStateFlow(1f)
    val alpha: StateFlow<Float> = _alpha.asStateFlow()
    
    // Derived current color
    val currentColor: StateFlow<Color> = combine(
        hue, saturation, brightness, alpha
    ) { h, s, b, a ->
        ColorUtils.hsbToColor(h, s, b, a)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = Color.Black
    )
    
    // Recent colors (stored as ARGB ints)
    private val _recentColors = MutableStateFlow<List<Int>>(emptyList())
    val recentColors: StateFlow<List<Int>> = _recentColors.asStateFlow()
    
    // Previous color (for swap functionality)
    private val _previousColor = MutableStateFlow(Color.Black)
    val previousColor: StateFlow<Color> = _previousColor.asStateFlow()
    
    // Input mode
    private val _inputMode = MutableStateFlow(InputMode.HSB)
    val inputMode: StateFlow<InputMode> = _inputMode.asStateFlow()
    
    /**
     * Set hue value (0-360 degrees)
     */
    fun setHue(hue: Float) {
        _hue.value = hue.coerceIn(0f, 360f)
    }
    
    /**
     * Set saturation value (0-1)
     */
    fun setSaturation(sat: Float) {
        _saturation.value = sat.coerceIn(0f, 1f)
    }
    
    /**
     * Set brightness value (0-1)
     */
    fun setBrightness(bri: Float) {
        _brightness.value = bri.coerceIn(0f, 1f)
    }
    
    /**
     * Set alpha value (0-1)
     */
    fun setAlpha(alpha: Float) {
        _alpha.value = alpha.coerceIn(0f, 1f)
    }
    
    /**
     * Set color from RGB Color object
     * Converts RGB to HSB internally
     */
    fun setColor(color: Color) {
        val hsb = ColorUtils.rgbToHsb(color)
        _hue.value = hsb[0]
        _saturation.value = hsb[1]
        _brightness.value = hsb[2]
        _alpha.value = color.alpha
    }
    
    /**
     * Confirm color selection
     * Saves current color to previous and adds to recent colors
     */
    fun confirmColor() {
        // Save current to previous
        _previousColor.value = currentColor.value
        
        // Add to recent (max 10)
        val color = currentColor.value.toArgb()
        val updated = _recentColors.value.toMutableList().apply {
            remove(color) // Remove if already present
            add(0, color) // Add to front
            if (size > 10) {
                // Remove last items to keep max 10
                while (size > 10) {
                    removeLast()
                }
            }
        }
        _recentColors.value = updated
    }
    
    /**
     * Swap current and previous colors
     * Useful for quick color comparison
     */
    fun swapCurrentPrevious() {
        val temp = currentColor.value
        setColor(_previousColor.value)
        _previousColor.value = temp
    }
    
    /**
     * Set input mode (HSB, RGB, or HEX)
     */
    fun setInputMode(mode: InputMode) {
        _inputMode.value = mode
    }
    
    /**
     * Initialize with a starting color
     */
    fun initialize(color: Color) {
        setColor(color)
        _previousColor.value = color
    }
    
    /**
     * Load recent colors from storage
     */
    fun loadRecentColors(colors: List<Int>) {
        _recentColors.value = colors.take(10) // Max 10 colors
    }
}

/**
 * Input modes for color selection
 */
enum class InputMode {
    HSB,    // Hue, Saturation, Brightness
    RGB,    // Red, Green, Blue
    HEX     // Hexadecimal input
}
