package com.pavit.bundl.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val hasSeenOnboardingKey = booleanPreferencesKey("has_seen_onboarding")
    
    companion object {
        private const val TAG = "UserPreferencesManager"
    }
    
    suspend fun markOnboardingAsSeen() {
        context.userPrefsDataStore.edit { preferences ->
            preferences[hasSeenOnboardingKey] = true
        }
    }
    
    fun hasSeenOnboarding(): Flow<Boolean> {
        return context.userPrefsDataStore.data.map { preferences ->
            preferences[hasSeenOnboardingKey] ?: false
        }
    }
    
    suspend fun clearAllPreferences() {
        context.userPrefsDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
