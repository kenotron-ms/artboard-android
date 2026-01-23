package com.artboard.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artboard.ui.onboarding.components.PageIndicator
import com.artboard.ui.onboarding.screens.*

/**
 * Complete onboarding flow with 4 screens
 * - Welcome
 * - Gesture Guide
 * - Canvas Tour
 * - Create Prompt
 * 
 * Features:
 * - Skip button (top-right)
 * - Page indicator (bottom)
 * - "Don't show again" checkbox
 * - Smooth page transitions
 */
@Composable
fun OnboardingFlow(
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    onExploreSample: (() -> Unit)? = null,
    viewModel: OnboardingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 4 })
    var dontShowAgain by remember { mutableStateOf(false) }
    
    // Update ViewModel when page changes
    LaunchedEffect(pagerState.currentPage) {
        viewModel.setCurrentPage(pagerState.currentPage)
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> WelcomeScreen()
                1 -> GestureGuideScreen()
                2 -> CanvasTourScreen()
                3 -> CreatePromptScreen(
                    onGetStarted = {
                        viewModel.completeOnboarding(context, dontShowAgain)
                        onComplete()
                    },
                    onExploreSample = onExploreSample
                )
            }
        }
        
        // Skip button (top-right) - hide on last page
        if (pagerState.currentPage < 3) {
            TextButton(
                onClick = {
                    viewModel.completeOnboarding(context, dontShowAgain)
                    onSkip()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Skip",
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = Color(0xFFAAAAAA)
                    )
                )
            }
        }
        
        // Page indicator and "Don't show again" (bottom)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PageIndicator(
                    currentPage = pagerState.currentPage,
                    pageCount = 4
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Don't show again checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                ) {
                    Checkbox(
                        checked = dontShowAgain,
                        onCheckedChange = { 
                            dontShowAgain = it
                            viewModel.setDontShowAgain(it)
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF4A90E2),
                            uncheckedColor = Color(0xFF666666)
                        )
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "Don't show again",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = Color(0xFFAAAAAA)
                        )
                    )
                }
            }
        }
    }
}
