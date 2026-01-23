package com.artboard.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for onboarding flow state management
 * Manages onboarding completion status and user preferences
 */
class OnboardingViewModel : ViewModel() {
    
    private val _shouldShowOnboarding = MutableStateFlow(true)
    val shouldShowOnboarding: StateFlow<Boolean> = _shouldShowOnboarding.asStateFlow()
    
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()
    
    private val _dontShowAgain = MutableStateFlow(false)
    val dontShowAgain: StateFlow<Boolean> = _dontShowAgain.asStateFlow()
    
    companion object {
        private const val PREFS_NAME = "onboarding"
        private const val KEY_COMPLETED = "completed"
    }
    
    /**
     * Check if user has already completed onboarding
     */
    fun checkOnboardingStatus(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasSeenOnboarding = prefs.getBoolean(KEY_COMPLETED, false)
        _shouldShowOnboarding.value = !hasSeenOnboarding
    }
    
    /**
     * Mark onboarding as completed
     */
    fun completeOnboarding(context: Context, dontShowAgain: Boolean = true) {
        if (dontShowAgain) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_COMPLETED, true)
                .apply()
        }
        _shouldShowOnboarding.value = false
    }
    
    /**
     * Reset onboarding status (for "View Tutorial Again" in settings)
     */
    fun resetOnboarding(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_COMPLETED, false)
            .apply()
        _shouldShowOnboarding.value = true
    }
    
    /**
     * Update current page
     */
    fun setCurrentPage(page: Int) {
        _currentPage.value = page
    }
    
    /**
     * Toggle "don't show again" preference
     */
    fun setDontShowAgain(value: Boolean) {
        _dontShowAgain.value = value
    }
}
