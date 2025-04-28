package com.orvio.app.presentation.dashboard

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.orvio.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    
    val tabs = listOf(
        TabItem.Home,
        TabItem.ApiKeys
    )
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                imageVector = ImageVector.vectorResource(id = item.iconResId),
                                contentDescription = stringResource(id = item.titleResId)
                            )
                        },
                        label = { Text(stringResource(id = item.titleResId)) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> HomeTab(
                modifier = Modifier.padding(innerPadding),
                onLogout = onLogout
            )
            1 -> ApiKeysTab(
                modifier = Modifier.padding(innerPadding),
                viewModel = viewModel
            )
        }
    }
}

sealed class TabItem(val titleResId: Int, val iconResId: Int) {
    object Home : TabItem(R.string.home_tab, R.drawable.ic_home)
    object ApiKeys : TabItem(R.string.api_keys_tab, R.drawable.ic_key)
} 