package com.pavit.bundl.presentation.splash

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import androidx.lifecycle.ViewModel
import com.pavit.bundl.data.utils.LocationManager as AppLocationManager
import com.pavit.bundl.domain.repository.NavigationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    val locationManager: AppLocationManager,
    private val navigationRepository: NavigationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    suspend fun hasLocationPermissions(): Boolean {
        return navigationRepository.hasLocationPermissions()
    }
    
    fun isLocationServicesEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || 
               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    
    fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}