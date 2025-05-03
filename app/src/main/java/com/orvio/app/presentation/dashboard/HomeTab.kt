package com.orvio.app.presentation.dashboard

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.orvio.app.R
import com.orvio.app.domain.model.AllDevicesStats
import com.orvio.app.domain.model.ConsumerStats
import com.orvio.app.domain.model.ProviderStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeTab(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val fcmToken by viewModel.fcmToken.collectAsState()
    val isRegistered by viewModel.isRegistered.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val credits by viewModel.credits.collectAsState()
    val creditMode by viewModel.creditMode.collectAsState()
    val isLoadingCredits by viewModel.isLoadingCredits.collectAsState()
    val secondsUntilRefresh by viewModel.secondsUntilRefresh.collectAsState()
    val userStats by viewModel.userStats.collectAsState()
    val isLoadingStats by viewModel.isLoadingStats.collectAsState()
    
    val coroutineScope = rememberCoroutineScope()
    val refreshing = remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing.value,
        onRefresh = {
            refreshing.value = true
            coroutineScope.launch {
                viewModel.refreshCredits()
                // Add a small delay to make the refresh indicator visible
                delay(500)
                refreshing.value = false
            }
        }
    )
    
    // Show error toast if there's an error
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Orvio") },
                actions = {
                    // Display credits in the app bar with cash emoji
                    if (!isLoadingCredits) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "💰 $credits",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_logout),
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WelcomeCard()
                
                // Credits card
                CreditsCard(
                    credits = credits, 
                    creditMode = creditMode, 
                    isLoading = isLoadingCredits,
                    secondsUntilRefresh = secondsUntilRefresh
                )
                
                // Device stats card
                DeviceStatsCard(
                    isRegistered = isRegistered,
                    providerStats = userStats?.provider,
                    isLoading = isLoadingStats
                )
                
                // API Key stats card
                ApiKeyStatsCard(
                    consumerStats = userStats?.consumer,
                    isLoading = isLoadingStats
                )
                
                // Add a hint for pull-to-refresh
                Text(
                    text = "Pull down to refresh stats",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Pull-to-refresh indicator
            PullRefreshIndicator(
                refreshing = refreshing.value,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun WelcomeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Welcome to Orvio",
                style = MaterialTheme.typography.titleLarge
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Manage your API keys and device registration",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun CreditsCard(
    credits: Int, 
    creditMode: String, 
    isLoading: Boolean,
    secondsUntilRefresh: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Your Credits",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = "Mode: $creditMode",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Text(
                    text = "💰",
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "$credits",
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Available Credits",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Show refresh countdown
                Text(
                    text = "Refreshing in $secondsUntilRefresh seconds",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun DeviceStatsCard(
    isRegistered: Boolean,
    providerStats: ProviderStats?,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Device Stats",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading || providerStats == null) {
                // Show loading or placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    } else {
                        Text(
                            text = "No device stats available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha =.6f)
                        )
                    }
                }
            } else {
                // Display device status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        title = "Device Status",
                        value = if (isRegistered) "Registered" else "Registering...",
                        modifier = Modifier.weight(1f)
                    )
                    
                    StatItem(
                        title = "Active Devices",
                        value = "${providerStats.allDevices.activeDevices}/${providerStats.allDevices.totalDevices}",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                
                // Message stats
                Text(
                    text = "Message Stats",
                    style = MaterialTheme.typography.titleSmall
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // First row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        title = "Total Sent",
                        value = "${providerStats.allDevices.totalMessagesSent}",
                        modifier = Modifier.weight(1f)
                    )
                    
                    StatItem(
                        title = "Msg Tried",
                        value = "${providerStats.allDevices.messageTried}",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Second row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        title = "Sent Success",
                        value = "${providerStats.allDevices.messageSentSuccessfully}",
                        modifier = Modifier.weight(1f)
                    )
                    
                    StatItem(
                        title = "Failed Ack",
                        value = "${providerStats.allDevices.failedToSendAck}",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Third row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        title = "Ack Verified",
                        value = "${providerStats.allDevices.sentAckVerified}",
                        modifier = Modifier.weight(1f)
                    )
                    
                    StatItem(
                        title = "Ack Not Verified",
                        value = "${providerStats.allDevices.sentAckNotVerified}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun ApiKeyStatsCard(
    consumerStats: ConsumerStats?,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "API Key Stats",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading || consumerStats == null) {
                // Show loading or placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    } else {
                        Text(
                            text = "No API key stats available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f)
                        )
                    }
                }
            } else {
                // Key stats summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        title = "Total Keys",
                        value = "${consumerStats.aggregate.totalKeys}",
                        modifier = Modifier.weight(1f)
                    )
                    
                    StatItem(
                        title = "Active Keys",
                        value = "${consumerStats.aggregate.activeKeys}",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                if (consumerStats.keys.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Recent keys
                    Text(
                        text = "Latest API Keys",
                        style = MaterialTheme.typography.titleSmall
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Show up to 3 most recent keys
                    for (key in consumerStats.keys.take(3)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = key.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            // Format the date
                            val createdAt = key.createdAt.take(10) // just take YYYY-MM-DD
                            Text(
                                text = "Created: $createdAt",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall
        )
    }
} 