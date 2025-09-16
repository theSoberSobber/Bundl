package com.pavit.bundl.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pavit.bundl.domain.maps.MapProvider
import com.pavit.bundl.domain.repository.NavigationRepository
import com.pavit.bundl.presentation.auth.AuthViewModel
import com.pavit.bundl.presentation.auth.LoginScreen
import com.pavit.bundl.presentation.auth.OtpScreen
import com.pavit.bundl.presentation.dashboard.DashboardScreen
import com.pavit.bundl.presentation.splash.SplashScreen
import com.pavit.bundl.presentation.onboarding.LocationPermissionScreen
import com.pavit.bundl.presentation.onboarding.NotificationPermissionScreen
import com.pavit.bundl.presentation.credits.GetMoreCreditsScreen
import com.pavit.bundl.presentation.dummy.DummyScreen
import com.pavit.bundl.presentation.orders.MyOrdersScreen
import com.pavit.bundl.presentation.onboarding.OnboardingScreen
import com.pavit.bundl.presentation.chat.ChatScreen
import kotlinx.coroutines.launch
import javax.inject.Inject

@Composable
fun Navigation(
    mapProvider: MapProvider
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState(initial = false)
    val coroutineScope = rememberCoroutineScope()
    
    // Inject navigation repository through Hilt
    val navigationRepository: NavigationRepository = hiltViewModel<NavigationViewModel>().navigationRepository
    
    // State machine for clean navigation flow
    val navigationStateManager = remember { NavigationStateManager(navigationRepository) }
    var currentNavigationState by remember { mutableStateOf<NavigationState>(NavigationState.Loading) }
    
    // Helper function to navigate - clear backstack only for dashboard
    fun navigateToRoute(route: String, clearBackstack: Boolean = false) {
        navController.navigate(route) {
            if (clearBackstack) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
            launchSingleTop = true
        }
    }
    
    // Handle navigation events - just navigate, let state machine handle logic
    fun handleNavigationEvent(event: NavigationEvent) {
        coroutineScope.launch {
            val navigationResult = navigationStateManager.handleEvent(currentNavigationState, event, isLoggedIn)
            currentNavigationState = navigationResult.newState
            val route = navigationStateManager.getRouteForState(navigationResult.newState)
            
            // State machine decides if we should clear backstack
            navigateToRoute(route, navigationResult.shouldClearBackstack)
        }
    }
    
    // Handle auth state changes (token expiry, logout)
    LaunchedEffect(isLoggedIn) {
        if (currentNavigationState != NavigationState.Loading) {
            if (!isLoggedIn && currentNavigationState == NavigationState.Dashboard) {
                // this never gets triggered right now because I do not have an auth check at launch I think, TODO if not
                handleNavigationEvent(NavigationEvent.TokenExpired)
            } else if (isLoggedIn && currentNavigationState == NavigationState.Login) {
                handleNavigationEvent(NavigationEvent.LoginSuccessful)
            }
        }
    }
    
    // Start with the splash screen
    NavHost(
        navController = navController,
        startDestination = Route.Splash.route
    ) {
        composable(Route.Splash.route) {
            SplashScreen(
                onCheckComplete = {
                    handleNavigationEvent(NavigationEvent.SplashCompleted)
                }
            )
        }

        composable(Route.Onboarding.route) {
            OnboardingScreen(
                navController = navController,
                onComplete = {
                    handleNavigationEvent(NavigationEvent.OnboardingCompleted)
                }
            )
        }
        
        composable(Route.LocationPermission.route) {
            LocationPermissionScreen(
                onPermissionGranted = {
                    handleNavigationEvent(NavigationEvent.LocationPermissionGranted)
                }
            )
        }

        composable(Route.NotificationPermission.route) {
            NotificationPermissionScreen(
                onPermissionGranted = {
                    handleNavigationEvent(NavigationEvent.NotificationPermissionGranted)
                }
            )
        }
    
        composable(Route.Login.route) {
            LoginScreen(
                onNavigateToOtp = { tid, phoneNumber ->
                    handleNavigationEvent(NavigationEvent.OtpRequested(tid, phoneNumber))
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
                    handleNavigationEvent(NavigationEvent.LoginSuccessful)
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
                    handleNavigationEvent(NavigationEvent.LogoutRequested)
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

        composable(Route.MyOrders.route) {
            MyOrdersScreen(navController = navController)
        }

        composable(
            route = Route.Chat.route,
            arguments = listOf(
                navArgument("orderId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            ChatScreen(
                orderId = orderId,
                navController = navController
            )
        }
    }
} 