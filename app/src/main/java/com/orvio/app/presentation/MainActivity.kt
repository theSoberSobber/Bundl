package com.orvio.app.presentation

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.orvio.app.presentation.navigation.Navigation
import com.orvio.app.presentation.theme.OrvioTheme
import com.orvio.app.utils.PermissionHandler
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OrvioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent()
                }
            }
        }
    }
}

@Composable
private fun AppContent() {
    val context = LocalContext.current
    var showPermissions by remember { mutableStateOf(true) }
    val missingPermissions = remember(showPermissions) { 
        PermissionHandler.getMissingPermissions(context) 
    }

    Column {
        // Show missing permissions if any
        if (showPermissions && missingPermissions.isNotEmpty()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Required Permissions",
                    style = MaterialTheme.typography.titleMedium
                )
                missingPermissions.forEach { permission ->
                    Text(
                        text = "• ${permission.split(".").last()}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
            }
        }

        // Request permissions
        PermissionHandler.RequestPermissions(
            onAllPermissionsGranted = {
                showPermissions = false
            },
            onPermissionDenied = { permission ->
                Toast.makeText(
                    context,
                    "${permission.split(".").last()} permission is required",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        // Main app content
        Navigation()
    }
} 