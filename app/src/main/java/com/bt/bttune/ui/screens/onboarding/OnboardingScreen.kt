package com.bt.bttune.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun OnboardingScreen(
    navController: NavController
) {
    GuestProfileSetupScreen(navController = navController)
}
