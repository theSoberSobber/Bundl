package com.pavit.bundl.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bundl_tokens")

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")
    
    companion object {
        private const val TAG = "TokenManager"
    }
    
    suspend fun saveTokens(accessToken: String?, refreshToken: String?) {
        if (accessToken == null || refreshToken == null) {
            Log.e(TAG, "Attempted to save null tokens: access=${accessToken != null}, refresh=${refreshToken != null}")
            return
        }
        
        Log.d(TAG, "Saving new tokens: access=${accessToken.take(10)}..., refresh=${refreshToken.take(10)}...")
        context.dataStore.edit { preferences ->
            preferences[accessTokenKey] = accessToken
            preferences[refreshTokenKey] = refreshToken
        }
    }
    
    suspend fun clearTokens() {
        Log.w(TAG, "Clearing all tokens - USER WILL BE LOGGED OUT")
        context.dataStore.edit { preferences ->
            preferences.remove(accessTokenKey)
            preferences.remove(refreshTokenKey)
        }
    }
    
    fun getAccessToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            val token = preferences[accessTokenKey]
            if (token != null) {
                Log.v(TAG, "Retrieved access token: ${token.take(10)}...")
            }
            token
        }
    }
    
    fun getRefreshToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            val token = preferences[refreshTokenKey]
            if (token != null) {
                Log.v(TAG, "Retrieved refresh token: ${token.take(10)}...")
            }
            token
        }
    }
    
    fun isLoggedIn(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            val accessToken = preferences[accessTokenKey]
            val refreshToken = preferences[refreshTokenKey]
            val isLoggedIn = !accessToken.isNullOrEmpty() && !refreshToken.isNullOrEmpty()
            Log.v(TAG, "isLoggedIn check: $isLoggedIn")
            isLoggedIn
        }
    }
} 