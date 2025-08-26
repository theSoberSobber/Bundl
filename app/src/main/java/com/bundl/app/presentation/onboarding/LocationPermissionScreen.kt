package com.bundl.app.presentation.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.bundl.app.R
import com.bundl.app.presentation.theme.Blue

@Composable
fun LocationPermissionScreen(
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    var permissionStep by remember { mutableStateOf(getInitialPermissionStep(context)) }
    
    // Launchers for different permission requests
    val basicLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        if (fineLocationGranted || coarseLocationGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissionStep = PermissionStep.NeedBackgroundLocation
            } else {
                // Android 9 and below - basic location is enough
                onPermissionGranted()
            }
        } else {
            permissionStep = PermissionStep.BasicLocationDenied
        }
    }
    
    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPermissionGranted()
        } else {
            permissionStep = PermissionStep.BackgroundLocationDenied
        }
    }
    
    // Check permissions when returning from settings
    LaunchedEffect(Unit) {
        permissionStep = getInitialPermissionStep(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Location icon
        Image(
            painter = painterResource(id = R.drawable.ic_location),
            contentDescription = "Location Permission",
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        when (permissionStep) {
            PermissionStep.NeedBasicLocation -> {
                BasicLocationPermissionContent(
                    onAllowClick = {
                        basicLocationLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
            }
            
            PermissionStep.NeedBackgroundLocation -> {
                BackgroundLocationPermissionContent(
                    onAllowClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        }
                    },
                    onOpenSettingsClick = {
                        openLocationSettings(context)
                    }
                )
            }
            
            PermissionStep.BasicLocationDenied -> {
                LocationDeniedContent(
                    title = "Location Access Required",
                    message = "Bundl needs location access to find orders nearby. Please enable location permissions in settings.",
                    onOpenSettingsClick = {
                        openAppSettings(context)
                    }
                )
            }
            
            PermissionStep.BackgroundLocationDenied -> {
                LocationDeniedContent(
                    title = "Always Allow Location",
                    message = "For the best experience, please set location access to 'Allow all the time' in your device settings. This helps us show you relevant orders even when the app is closed.",
                    onOpenSettingsClick = {
                        openLocationSettings(context)
                    }
                )
            }
            
            PermissionStep.AllPermissionsGranted -> {
                LaunchedEffect(Unit) {
                    onPermissionGranted()
                }
            }
        }
    }
}

@Composable
private fun BasicLocationPermissionContent(
    onAllowClick: () -> Unit
) {
    Text(
        text = "Enable Location Services",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Bundl needs your location to:",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Medium
    )
    
    Spacer(modifier = Modifier.height(12.dp))
    
    Text(
        text = "• Find orders near you\n• Let you create orders at your location\n• Connect you with nearby users\n• Enable shared deliveries in your area",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Start
    )

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onAllowClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Allow Location Access")
    }
}

@Composable
private fun BackgroundLocationPermissionContent(
    onAllowClick: () -> Unit,
    onOpenSettingsClick: () -> Unit
) {
    Text(
        text = "Allow All The Time",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "For the best experience, we need access to your location even when the app is closed.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
    
    Spacer(modifier = Modifier.height(12.dp))
    
    Text(
        text = "This helps us:\n• Notify you about nearby orders\n• Keep your location updated for deliveries\n• Provide location-based features",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Start
    )

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onAllowClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Allow All The Time")
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    
    TextButton(
        onClick = onOpenSettingsClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Open Location Settings")
    }
}

@Composable
private fun LocationDeniedContent(
    title: String,
    message: String,
    onOpenSettingsClick: () -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onOpenSettingsClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Open Settings")
    }
}

private enum class PermissionStep {
    NeedBasicLocation,
    NeedBackgroundLocation, 
    BasicLocationDenied,
    BackgroundLocationDenied,
    AllPermissionsGranted
}

private fun getInitialPermissionStep(context: Context): PermissionStep {
    val hasFineLocation = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    
    val hasCoarseLocation = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    
    val hasAnyLocation = hasFineLocation || hasCoarseLocation
    
    if (!hasAnyLocation) {
        return PermissionStep.NeedBasicLocation
    }
    
    // Check background location for Android 10+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val hasBackgroundLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        return if (hasBackgroundLocation) {
            PermissionStep.AllPermissionsGranted
        } else {
            PermissionStep.NeedBackgroundLocation
        }
    }
    
    // Android 9 and below - basic location is enough
    return PermissionStep.AllPermissionsGranted
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

private fun openLocationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    context.startActivity(intent)
} 