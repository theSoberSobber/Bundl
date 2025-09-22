package com.pavit.bundl.presentation.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var showGpsDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        // Check if we have location permissions first
        if (!viewModel.hasLocationPermissions()) {
            Log.d("SplashScreen", "No location permissions, proceeding to navigation flow")
            onCheckComplete()
            return@LaunchedEffect
        }
        
        // Check if GPS/Location services are enabled
        if (!viewModel.isLocationServicesEnabled()) {
            Log.d("SplashScreen", "Location services disabled, showing dialog")
            showGpsDialog = true
            return@LaunchedEffect
        }
    }
    
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
    
    // GPS Dialog
    if (showGpsDialog) {
        AlertDialog(
            onDismissRequest = { /* Don't allow dismissing */ },
            title = { Text("Location Services Required") },
            text = { Text("This app requires location services to be enabled. Please enable GPS in your device settings and restart the app.") },
            confirmButton = {
                Button(
                    onClick = { 
                        viewModel.openLocationSettings()
                        // Exit app - user needs to restart after enabling GPS
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        // Exit app - location is mandatory
                        android.os.Process.killProcess(android.os.Process.myPid())
                    }
                ) {
                    Text("Exit App")
                }
            }
        )
    }
} 