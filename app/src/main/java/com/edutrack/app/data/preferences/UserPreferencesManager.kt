package com.edutrack.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.edutrack.app.data.local.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "edutrack_preferences")

data class AppPreferences(
    val onboardingCompleted: Boolean = false,
    val activeJourneyId: Long = -1L,
    val themePreference: ThemePreference = ThemePreference.DARK,
    val notificationsEnabled: Boolean = true,
    val studentName: String = ""
)

class UserPreferencesManager(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val ACTIVE_JOURNEY_ID = longPreferencesKey("active_journey_id")
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val STUDENT_NAME = stringPreferencesKey("student_name")
    }

    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_PREFERENCE] ?: ThemePreference.DARK.name
        val theme = runCatching { ThemePreference.valueOf(themeStr) }
            .getOrDefault(ThemePreference.DARK)
        AppPreferences(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            activeJourneyId = prefs[Keys.ACTIVE_JOURNEY_ID] ?: -1L,
            themePreference = theme,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            studentName = prefs[Keys.STUDENT_NAME] ?: ""
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean, activeJourneyId: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = completed
            prefs[Keys.ACTIVE_JOURNEY_ID] = activeJourneyId
        }
    }

    suspend fun setActiveJourneyId(journeyId: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACTIVE_JOURNEY_ID] = journeyId
        }
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_PREFERENCE] = theme.name
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setStudentName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.STUDENT_NAME] = name.trim()
        }
    }
}
