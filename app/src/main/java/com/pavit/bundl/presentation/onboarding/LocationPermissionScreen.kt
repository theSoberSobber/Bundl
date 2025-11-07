package com.pavit.bundl.presentation.onboarding

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
import com.pavit.bundl.R
import com.pavit.bundl.presentation.theme.Blue

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
            // Foreground location granted - that's all we need with foreground service!
            onPermissionGranted()
        } else {
            permissionStep = PermissionStep.BasicLocationDenied
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
            
            PermissionStep.BasicLocationDenied -> {
                LocationDeniedContent(
                    title = "Location Access Required",
                    message = "Bundl needs location access to find orders nearby. Please enable location permissions in settings.",
                    onOpenSettingsClick = {
                        openAppSettings(context)
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

    Spacer(modifier = Modifier.height(24.dp))
    
    // Prominent notification feature card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔔",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column {
                Text(
                    text = "Get Notified of Nearby Orders",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select 'While using the app' to receive notifications when orders appear near you!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onAllowClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Allow Location Access")
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
    BasicLocationDenied,
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
    
    // Foreground location is all we need - foreground service handles the rest!
    return PermissionStep.AllPermissionsGranted
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
} 