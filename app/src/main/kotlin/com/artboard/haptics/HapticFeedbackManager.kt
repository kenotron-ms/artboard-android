package com.artboard.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.RequiresApi
import kotlin.math.abs

/**
 * Central manager for all haptic feedback in the app
 * 
 * Features:
 * - Five intensity levels (Light, Medium, Heavy, Success, Warning)
 * - Respects user preferences (HapticSettings)
 * - Respects system haptic settings
 * - Android API compatibility (API 26+ with fallbacks)
 * - Category-based filtering
 * - Performance optimized (< 1ms overhead)
 * 
 * Based on HAPTIC_FEEDBACK.md specification
 */
class HapticFeedbackManager(
    private val context: Context
) {
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    // Cache settings for performance
    private var cachedSettings: HapticSettings = HapticSettings.load(context)
    private var lastSettingsCheck: Long = System.currentTimeMillis()
    private val settingsCheckInterval = 1000L // Refresh settings every second
    
    /**
     * Perform haptic feedback with specified intensity and category
     * 
     * @param intensity The haptic intensity level
     * @param category The haptic category (for user preference filtering)
     */
    fun perform(intensity: HapticIntensity, category: HapticCategory = HapticCategory.BUTTON) {
        // Refresh settings periodically (performance optimization)
        refreshSettingsIfNeeded()
        
        // Check if haptics are enabled (user preference + category)
        if (!cachedSettings.isEnabled(category)) return
        
        // Check system haptic setting
        if (!isSystemHapticsEnabled()) return
        
        // Check if device has vibrator
        if (!vibrator.hasVibrator()) return
        
        // Perform the haptic feedback
        performInternal(intensity)
    }
    
    /**
     * Perform haptic feedback using View (for Compose integration)
     * 
     * @param view The view to perform haptic on
     * @param intensity The haptic intensity level
     * @param category The haptic category (for user preference filtering)
     */
    fun performView(view: View, intensity: HapticIntensity, category: HapticCategory = HapticCategory.BUTTON) {
        refreshSettingsIfNeeded()
        
        if (!cachedSettings.isEnabled(category)) return
        if (!isSystemHapticsEnabled()) return
        
        view.performHapticFeedback(
            intensity.toHapticFeedbackConstant(),
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
        )
    }
    
    /**
     * Perform haptic if value changed significantly (for sliders)
     * 
     * @param newValue Current value
     * @param oldValue Previous value
     * @param threshold Minimum change to trigger haptic (default 0.1 = 10%)
     * @param intensity Haptic intensity (default LIGHT)
     */
    fun performIfChanged(
        newValue: Float,
        oldValue: Float,
        threshold: Float = 0.1f,
        intensity: HapticIntensity = HapticIntensity.LIGHT
    ) {
        if (abs(newValue - oldValue) >= threshold) {
            perform(intensity, HapticCategory.SLIDER)
        }
    }
    
    /**
     * Perform haptic pattern (for special feedback)
     * 
     * @param pattern Vibration pattern (timings in ms)
     * @param amplitudes Vibration amplitudes (0-255)
     * @param category Haptic category
     */
    fun performPattern(
        pattern: LongArray,
        amplitudes: IntArray,
        category: HapticCategory = HapticCategory.FEEDBACK
    ) {
        refreshSettingsIfNeeded()
        
        if (!cachedSettings.isEnabled(category)) return
        if (!isSystemHapticsEnabled()) return
        if (!vibrator.hasVibrator()) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(pattern, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            // Fallback for older APIs
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }
    
    /**
     * Perform snap point haptic (zoom 100%, rotation 90°, etc.)
     */
    fun performSnapPoint() {
        perform(HapticIntensity.LIGHT, HapticCategory.TRANSFORM)
    }
    
    /**
     * Perform gesture completion haptic
     */
    fun performGestureComplete() {
        perform(HapticIntensity.MEDIUM, HapticCategory.GESTURE)
    }
    
    /**
     * Perform success haptic (save complete, export done)
     */
    fun performSuccess() {
        performPattern(
            HapticPatterns.SUCCESS.first,
            HapticPatterns.SUCCESS.second,
            HapticCategory.FEEDBACK
        )
    }
    
    /**
     * Perform warning haptic (destructive action, error)
     */
    fun performWarning() {
        perform(HapticIntensity.WARNING, HapticCategory.FEEDBACK)
    }
    
    /**
     * Perform layer operation haptic
     */
    fun performLayerOperation(intensity: HapticIntensity = HapticIntensity.MEDIUM) {
        perform(intensity, HapticCategory.LAYER)
    }
    
    /**
     * Perform file operation haptic
     */
    fun performFileOperation(success: Boolean) {
        if (success) {
            perform(HapticIntensity.SUCCESS, HapticCategory.FILE)
        } else {
            perform(HapticIntensity.WARNING, HapticCategory.FILE)
        }
    }
    
    /**
     * Cancel any ongoing haptic feedback
     */
    fun cancel() {
        vibrator.cancel()
    }
    
    /**
     * Force reload settings (call when settings change)
     */
    fun reloadSettings() {
        cachedSettings = HapticSettings.load(context)
        lastSettingsCheck = System.currentTimeMillis()
    }
    
    // Internal methods
    
    private fun performInternal(intensity: HapticIntensity) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10+ - Use predefined effects
                performApi29(intensity)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                // Android 8-9 - Use VibrationEffect
                performApi26(intensity)
            }
            else -> {
                // Android 7 and below - Use deprecated vibrate
                performLegacy(intensity)
            }
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun performApi29(intensity: HapticIntensity) {
        val effect = when (intensity) {
            HapticIntensity.LIGHT -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            HapticIntensity.MEDIUM -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            HapticIntensity.HEAVY -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            HapticIntensity.SUCCESS -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            HapticIntensity.WARNING -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
        }
        vibrator.vibrate(effect)
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun performApi26(intensity: HapticIntensity) {
        val effect = VibrationEffect.createOneShot(
            intensity.getDuration(),
            intensity.getAmplitude()
        )
        vibrator.vibrate(effect)
    }
    
    @Suppress("DEPRECATION")
    private fun performLegacy(intensity: HapticIntensity) {
        vibrator.vibrate(intensity.getDuration())
    }
    
    private fun refreshSettingsIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - lastSettingsCheck > settingsCheckInterval) {
            cachedSettings = HapticSettings.load(context)
            lastSettingsCheck = now
        }
    }
    
    /**
     * Check if system haptics are enabled
     */
    private fun isSystemHapticsEnabled(): Boolean {
        return try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                1
            ) == 1
        } catch (e: Exception) {
            // If we can't read the setting, assume enabled
            true
        }
    }
    
    /**
     * Check if haptic feedback is available on this device
     */
    fun isHapticAvailable(): Boolean {
        return vibrator.hasVibrator()
    }
    
    companion object {
        // Singleton instance for app-wide access
        @Volatile
        private var instance: HapticFeedbackManager? = null
        
        /**
         * Get singleton instance
         */
        fun getInstance(context: Context): HapticFeedbackManager {
            return instance ?: synchronized(this) {
                instance ?: HapticFeedbackManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
