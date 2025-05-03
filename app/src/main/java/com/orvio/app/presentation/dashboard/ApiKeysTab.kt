package com.orvio.app.presentation.dashboard

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orvio.app.R
import com.orvio.app.domain.model.ApiKey
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeysTab(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val apiKeys by viewModel.apiKeys.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val success by viewModel.successMessage.collectAsState()
    val context = LocalContext.current
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showTestDialog by remember { mutableStateOf(false) }
    var selectedApiKey by remember { mutableStateOf<ApiKey?>(null) }
    
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    LaunchedEffect(success) {
        success?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearSuccess()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Keys") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_add),
                    contentDescription = "Add API Key"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && apiKeys.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (apiKeys.isEmpty()) {
                Text(
                    text = "No API keys found. Create one with the + button.",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(apiKeys) { apiKey ->
                        ApiKeyCard(
                            apiKey = apiKey,
                            onTestClick = { 
                                selectedApiKey = apiKey
                                showTestDialog = true
                            },
                            onDeleteClick = {
                                viewModel.deleteApiKey(apiKey.key, apiKey.name)
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Create API Key Dialog
    if (showCreateDialog) {
        CreateApiKeyDialog(
            onDismiss = { showCreateDialog = false },
            onCreateClick = { name ->
                viewModel.createApiKey(name) {
                    showCreateDialog = false
                }
            }
        )
    }
    
    // Test API Key Dialog
    if (showTestDialog && selectedApiKey != null) {
        TestApiKeyDialog(
            apiKey = selectedApiKey!!,
            onDismiss = { 
                showTestDialog = false
                selectedApiKey = null
                viewModel.clearTestResult()
            },
            onTestClick = { key, phoneNumber ->
                viewModel.testApiKey(key, phoneNumber)
            },
            isLoading = viewModel.isTestingKey.collectAsState().value,
            testResult = viewModel.testResult.collectAsState().value
        )
    }
}

@Composable
fun ApiKeyCard(
    apiKey: ApiKey,
    onTestClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
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
                Text(
                    text = apiKey.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row {
                    // Copy button
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(apiKey.key))
                            Toast.makeText(context, "API key copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_copy),
                            contentDescription = "Copy API Key"
                        )
                    }
                    
                    // Test button
                    IconButton(
                        onClick = onTestClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_test),
                            contentDescription = "Test API Key"
                        )
                    }
                    
                    // Delete button
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_delete),
                            contentDescription = "Delete API Key"
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = apiKey.key,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Created: ${dateFormat.format(apiKey.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                
                apiKey.lastUsed?.let { lastUsed ->
                    Text(
                        text = "Last used: ${dateFormat.format(lastUsed)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun CreateApiKeyDialog(
    onDismiss: () -> Unit,
    onCreateClick: (String) -> Unit
) {
    var apiKeyName by remember { mutableStateOf("") }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
                .imePadding()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .windowInsetsPadding(WindowInsets.ime),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
            ) {
                // Title
                Text(
                    text = "Create API Key",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Enter a name for your new API key")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = apiKeyName,
                    onValueChange = { apiKeyName = it },
                    label = { Text("API Key Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (apiKeyName.isNotBlank()) {
                                onCreateClick(apiKeyName)
                            }
                        }
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = { onCreateClick(apiKeyName) },
                        enabled = apiKeyName.isNotBlank()
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
fun TestApiKeyDialog(
    apiKey: ApiKey,
    onDismiss: () -> Unit,
    onTestClick: (String, String) -> Unit,
    isLoading: Boolean,
    testResult: Boolean?
) {
    var phoneNumber by remember { mutableStateOf("") }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
                .imePadding()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .windowInsetsPadding(WindowInsets.ime),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "Test API Key",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Testing API key: ${apiKey.name}")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp)
                    )
                } else if (testResult != null) {
                    if (testResult) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_success),
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("API key is working correctly!")
                    } else {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_error),
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("API key test failed.")
                    }
                } else {
                    // Initial state, show phone input and test button
                    
                    // Phone number input field
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Recipient Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Phone
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (phoneNumber.isNotBlank()) {
                                    onTestClick(apiKey.key, phoneNumber)
                                }
                            }
                        ),
                        placeholder = { Text("Enter recipient phone number") }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { onTestClick(apiKey.key, phoneNumber) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = phoneNumber.isNotBlank()
                    ) {
                        Text("Test Key")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
} 