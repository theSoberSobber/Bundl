package com.pavit.bundl.presentation.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.util.Log
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SplashScreen(
    onCheckComplete: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val locationData by viewModel.locationManager.currentLocation.collectAsState()
    
    LaunchedEffect(locationData) {
        Log.d("SplashScreen", "Location updated: lat=${locationData?.latitude}, lng=${locationData?.longitude}, isFromUser=${locationData?.isFromUser}")
        
        if (locationData?.isFromUser == true) {
            Log.d("SplashScreen", "Real location obtained, proceeding to main app")
            onCheckComplete()
        } else {
            Log.d("SplashScreen", "Still waiting for real location (using default)")
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(60.dp),
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = if (locationData?.isFromUser == true) "Ready!" else "Getting your location...",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
} 