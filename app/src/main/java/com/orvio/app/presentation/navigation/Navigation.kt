package com.orvio.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.orvio.app.presentation.auth.AuthViewModel
import com.orvio.app.presentation.auth.LoginScreen
import com.orvio.app.presentation.auth.OtpScreen
import com.orvio.app.presentation.dashboard.DashboardScreen

@Composable
fun Navigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState(initial = false)
    val startDestination = if (isLoggedIn) Route.Dashboard.route else Route.Login.route
    
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn && navController.currentDestination?.route != Route.Dashboard.route) {
            navController.navigate(Route.Dashboard.route) {
                popUpTo(Route.Login.route) { inclusive = true }
            }
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Route.Login.route) {
            LoginScreen(
                onNavigateToOtp = { transactionId, phoneNumber ->
                    navController.navigate(Route.Otp.createRoute(transactionId, phoneNumber))
                }
            )
        }
        
        composable(
            route = Route.Otp.route,
            arguments = listOf(
                navArgument("transactionId") { type = NavType.StringType },
                navArgument("phoneNumber") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            
            OtpScreen(
                transactionId = transactionId,
                phoneNumber = phoneNumber,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDashboard = {
                    navController.navigate(Route.Dashboard.route) {
                        popUpTo(Route.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Route.Dashboard.route) {
            DashboardScreen(
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Route.Login.route) {
                        popUpTo(Route.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }
    }
} 