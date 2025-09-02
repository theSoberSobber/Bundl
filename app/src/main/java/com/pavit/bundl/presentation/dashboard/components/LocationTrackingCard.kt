package com.pavit.bundl.presentation.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pavit.bundl.presentation.location.LocationTrackingViewModel

/**
 * Location tracking toggle component for dashboard
 * Shows current location status and allows users to enable/disable background tracking
 */
@Composable
fun LocationTrackingCard(
    modifier: Modifier = Modifier,
    viewModel: LocationTrackingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (uiState.isTrackingEnabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = if (uiState.isTrackingEnabled) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = "Nearby Orders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (uiState.isTrackingEnabled) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        
                        Text(
                            text = if (uiState.isTrackingEnabled) {
                                if (uiState.hasActiveSubscriptions) {
                                    "${uiState.currentGeohashes.size} areas (~200m radius)"
                                } else {
                                    "Getting precise location..."
                                }
                            } else {
                                "Get precise nearby order notifications (200m)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.isTrackingEnabled) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            }
                        )
                    }
                }
                
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Switch(
                        checked = uiState.isTrackingEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                viewModel.startLocationTracking()
                            } else {
                                viewModel.stopLocationTracking()
                            }
                        }
                    )
                }
            }
            
            // Show error if any
            if (uiState.error != null && !uiState.isLocationPermissionRequired) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.error ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            // Show permission prompt if needed
            if (uiState.isLocationPermissionRequired) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.startLocationTracking() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enable Location Access")
                }
            }
        }
    }
}

/**
 * Quick status indicator for nearby orders - can be used in top bar
 */
@Composable
fun NearbyOrdersIndicator(
    modifier: Modifier = Modifier,
    viewModel: LocationTrackingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    if (uiState.isTrackingEnabled && uiState.hasActiveSubscriptions) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location Active",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = "${uiState.currentGeohashes.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
