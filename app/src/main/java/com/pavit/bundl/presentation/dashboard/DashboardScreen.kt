package com.pavit.bundl.presentation.dashboard

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.pavit.bundl.domain.maps.MapProvider

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: HomeViewModel,
    onLogout: () -> Unit,
    mapProvider: MapProvider
) {
    // Directly show HomeTab without bottom navigation
    HomeTab(
        viewModel = viewModel,
        onLogout = onLogout,
        mapProvider = mapProvider,
        navController = navController
    )
} 