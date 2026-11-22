package com.edutrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edutrack.app.data.local.ThemePreference
import com.edutrack.app.data.preferences.AppPreferences
import com.edutrack.app.ui.navigation.EduTrackMainShell
import com.edutrack.app.ui.onboarding.OnboardingScreen
import com.edutrack.app.ui.onboarding.OnboardingViewModel
import com.edutrack.app.ui.theme.EduTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = (application as EduTrackApp).repository

        setContent {
            val preferences by repository.preferencesFlow.collectAsStateWithLifecycle(
                initialValue = AppPreferences()
            )
            var isAddingJourney by rememberSaveable { mutableStateOf(false) }

            val useDarkTheme = when (preferences.themePreference) {
                ThemePreference.DARK -> true
                ThemePreference.LIGHT -> false
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
            }

            EduTrackTheme(darkTheme = useDarkTheme) {
                if (!preferences.onboardingCompleted || isAddingJourney) {
                    val onboardingVm: OnboardingViewModel = viewModel(
                        key = "onboarding_${isAddingJourney}",
                        factory = OnboardingViewModel.factory(repository)
                    )
                    OnboardingScreen(
                        viewModel = onboardingVm,
                        isAddingAdditionalJourney = isAddingJourney,
                        onCancelAddJourney = { isAddingJourney = false },
                        onCompleted = { isAddingJourney = false }
                    )
                } else {
                    EduTrackMainShell(
                        repository = repository,
                        preferences = preferences,
                        onAddNewJourney = { isAddingJourney = true }
                    )
                }
            }
        }
    }
}
