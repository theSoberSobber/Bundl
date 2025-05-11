package com.bundl.app.presentation.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bundl.app.domain.maps.MapProvider
import com.bundl.app.presentation.MainActivity
import com.bundl.app.presentation.auth.AuthViewModel
import com.bundl.app.presentation.auth.LoginScreen
import com.bundl.app.presentation.auth.OtpScreen
import com.bundl.app.presentation.dashboard.DashboardScreen
import com.bundl.app.presentation.splash.SplashScreen
import com.bundl.app.screens.onboarding.LocationPermissionScreen
import com.bundl.app.screens.onboarding.NotificationPermissionScreen
import com.bundl.app.utils.PermissionHandler
import com.bundl.app.presentation.credits.GetMoreCreditsScreen
import com.bundl.app.presentation.dummy.DummyScreen
import com.bundl.app.presentation.orders.MyOrdersScreen
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@Composable
fun Navigation(
    context: Context,
    activity: MainActivity,
    mapProvider: MapProvider
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState(initial = false)
    
    // Flag to track whether auth status has been checked
    var hasCheckedAuthStatus by rememberSaveable { mutableStateOf(false) }
    
    // Check permissions
    val hasLocationPermissions = remember { 
        PermissionHandler.hasPermissions(context, PermissionHandler.locationPermissions)
    }
    val hasNotificationPermissions = remember {
        PermissionHandler.hasPermissions(context, PermissionHandler.notificationPermissions)
    }
    
    // Start with the splash screen
    NavHost(
        navController = navController,
        startDestination = Route.Splash.route
    ) {
        composable(Route.Splash.route) {
            SplashScreen(
                onCheckComplete = {
                    hasCheckedAuthStatus = true
                    if (isLoggedIn) {
                        navController.navigate(Route.Dashboard.route) {
                            popUpTo(Route.Splash.route) { inclusive = true }
                        }
                    } else if (!hasLocationPermissions) {
                        navController.navigate(Route.LocationPermission.route) {
                            popUpTo(Route.Splash.route) { inclusive = true }
                        }
                    } else if (!hasNotificationPermissions) {
                        navController.navigate(Route.NotificationPermission.route) {
                            popUpTo(Route.Splash.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Route.Login.route) {
                            popUpTo(Route.Splash.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        
        composable(Route.LocationPermission.route) {
            LocationPermissionScreen(
                navController = navController,
                onPermissionGranted = {
                    if (PermissionHandler.hasPermissions(context, PermissionHandler.notificationPermissions)) {
                        navController.navigate(Route.Login.route) {
                            popUpTo(Route.LocationPermission.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Route.NotificationPermission.route)
                    }
                }
            )
        }

        composable(Route.NotificationPermission.route) {
            NotificationPermissionScreen(
                navController = navController,
                onPermissionGranted = {
                    navController.navigate(Route.Login.route) {
                        popUpTo(Route.LocationPermission.route) { inclusive = true }
                    }
                }
            )
        }
    
        composable(Route.Login.route) {
            LoginScreen(
                onNavigateToOtp = { tid, phoneNumber ->
                    navController.navigate(Route.Otp.createRoute(tid, phoneNumber))
                }
            )
        }
        
        composable(
            route = Route.Otp.route,
            arguments = listOf(
                navArgument("tid") { type = NavType.StringType },
                navArgument("phoneNumber") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val tid = backStackEntry.arguments?.getString("tid") ?: ""
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            
            OtpScreen(
                tid = tid,
                phoneNumber = phoneNumber,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDashboard = {
                    navController.navigate(Route.Dashboard.route) {
                        popUpTo(Route.LocationPermission.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Route.Dashboard.route) {
            DashboardScreen(
                navController = navController,
                viewModel = hiltViewModel(),
                mapProvider = mapProvider,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Route.Login.route) {
                        popUpTo(Route.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Route.GetMoreCredits.route) {
            GetMoreCreditsScreen(
                navController = navController
            )
        }
        
        composable(Route.DummyScreen.route) {
            DummyScreen()
        }

        composable(Route.MyOrdersScreen.route) {
            MyOrdersScreen(navController = navController)
        }
    }
    
    // If auth status changes after initial check (e.g., token expires), handle it
    LaunchedEffect(isLoggedIn) {
        if (hasCheckedAuthStatus) {
            if (isLoggedIn && navController.currentDestination?.route != Route.Dashboard.route) {
                navController.navigate(Route.Dashboard.route) {
                    popUpTo(0) { inclusive = true }
                }
            } else if (!isLoggedIn && navController.currentDestination?.route == Route.Dashboard.route) {
                navController.navigate(Route.LocationPermission.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }
} 