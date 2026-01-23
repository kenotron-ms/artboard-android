package com.artboard.haptics

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * User preferences for haptic feedback system
 * 
 * Provides granular control over different haptic types:
 * - Global enable/disable
 * - Per-category toggles (buttons, sliders, gestures, feedback)
 * - Persistence using SharedPreferences
 * 
 * Based on HAPTIC_FEEDBACK.md specification
 */
data class HapticSettings(
    /**
     * Master toggle - disables all haptics when false
     */
    val hapticsEnabled: Boolean = true,
    
    /**
     * Button tap haptics (all buttons, icon buttons, cards)
     */
    val buttonHaptics: Boolean = true,
    
    /**
     * Slider/control haptics (color sliders, brush size, opacity)
     */
    val sliderHaptics: Boolean = true,
    
    /**
     * Gesture haptics (undo, redo, zoom, pan, eyedropper)
     */
    val gestureHaptics: Boolean = true,
    
    /**
     * Feedback haptics (success, warning, error states)
     */
    val feedbackHaptics: Boolean = true,
    
    /**
     * Transform haptics (snap points, rotation, scale)
     */
    val transformHaptics: Boolean = true,
    
    /**
     * Layer operation haptics (select, reorder, visibility)
     */
    val layerHaptics: Boolean = true,
    
    /**
     * File operation haptics (save, load, export)
     */
    val fileHaptics: Boolean = true
) {
    /**
     * Check if a specific haptic category is enabled
     * Always respects the master toggle
     */
    fun isEnabled(category: HapticCategory): Boolean {
        if (!hapticsEnabled) return false
        
        return when (category) {
            HapticCategory.BUTTON -> buttonHaptics
            HapticCategory.SLIDER -> sliderHaptics
            HapticCategory.GESTURE -> gestureHaptics
            HapticCategory.FEEDBACK -> feedbackHaptics
            HapticCategory.TRANSFORM -> transformHaptics
            HapticCategory.LAYER -> layerHaptics
            HapticCategory.FILE -> fileHaptics
        }
    }
    
    companion object {
        private const val PREFS_NAME = "haptic_settings"
        
        // Keys
        private const val KEY_ENABLED = "enabled"
        private const val KEY_BUTTONS = "buttons"
        private const val KEY_SLIDERS = "sliders"
        private const val KEY_GESTURES = "gestures"
        private const val KEY_FEEDBACK = "feedback"
        private const val KEY_TRANSFORM = "transform"
        private const val KEY_LAYER = "layer"
        private const val KEY_FILE = "file"
        
        /**
         * Load settings from SharedPreferences
         */
        fun load(context: Context): HapticSettings {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            
            return HapticSettings(
                hapticsEnabled = prefs.getBoolean(KEY_ENABLED, true),
                buttonHaptics = prefs.getBoolean(KEY_BUTTONS, true),
                sliderHaptics = prefs.getBoolean(KEY_SLIDERS, true),
                gestureHaptics = prefs.getBoolean(KEY_GESTURES, true),
                feedbackHaptics = prefs.getBoolean(KEY_FEEDBACK, true),
                transformHaptics = prefs.getBoolean(KEY_TRANSFORM, true),
                layerHaptics = prefs.getBoolean(KEY_LAYER, true),
                fileHaptics = prefs.getBoolean(KEY_FILE, true)
            )
        }
        
        /**
         * Save settings to SharedPreferences
         */
        fun save(context: Context, settings: HapticSettings) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                putBoolean(KEY_ENABLED, settings.hapticsEnabled)
                putBoolean(KEY_BUTTONS, settings.buttonHaptics)
                putBoolean(KEY_SLIDERS, settings.sliderHaptics)
                putBoolean(KEY_GESTURES, settings.gestureHaptics)
                putBoolean(KEY_FEEDBACK, settings.feedbackHaptics)
                putBoolean(KEY_TRANSFORM, settings.transformHaptics)
                putBoolean(KEY_LAYER, settings.layerHaptics)
                putBoolean(KEY_FILE, settings.fileHaptics)
            }
        }
        
        /**
         * Reset to default settings
         */
        fun reset(context: Context) {
            save(context, HapticSettings())
        }
        
        /**
         * Get SharedPreferences instance for observation
         */
        fun getPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
}

/**
 * Haptic feedback categories for granular control
 */
enum class HapticCategory {
    /**
     * Button taps, icon buttons, card selections
     */
    BUTTON,
    
    /**
     * Slider drags, color adjustments, brush size changes
     */
    SLIDER,
    
    /**
     * Multi-finger gestures, undo/redo, zoom/pan
     */
    GESTURE,
    
    /**
     * Success/warning/error feedback
     */
    FEEDBACK,
    
    /**
     * Transform operations, snap points, rotation
     */
    TRANSFORM,
    
    /**
     * Layer operations, reordering, visibility
     */
    LAYER,
    
    /**
     * File operations, save, load, export
     */
    FILE
}

/**
 * Haptic settings observer for live updates
 */
interface HapticSettingsObserver {
    fun onSettingsChanged(settings: HapticSettings)
}

/**
 * Manager for observing haptic settings changes
 */
class HapticSettingsManager(private val context: Context) {
    private val observers = mutableListOf<HapticSettingsObserver>()
    private var currentSettings = HapticSettings.load(context)
    
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        val newSettings = HapticSettings.load(context)
        if (newSettings != currentSettings) {
            currentSettings = newSettings
            notifyObservers(newSettings)
        }
    }
    
    init {
        HapticSettings.getPreferences(context)
            .registerOnSharedPreferenceChangeListener(preferenceListener)
    }
    
    fun addObserver(observer: HapticSettingsObserver) {
        observers.add(observer)
    }
    
    fun removeObserver(observer: HapticSettingsObserver) {
        observers.remove(observer)
    }
    
    private fun notifyObservers(settings: HapticSettings) {
        observers.forEach { it.onSettingsChanged(settings) }
    }
    
    fun getSettings(): HapticSettings = currentSettings
    
    fun updateSettings(settings: HapticSettings) {
        HapticSettings.save(context, settings)
        currentSettings = settings
    }
    
    fun cleanup() {
        HapticSettings.getPreferences(context)
            .unregisterOnSharedPreferenceChangeListener(preferenceListener)
        observers.clear()
    }
}
