package com.pavit.bundl.presentation.navigation

import com.pavit.bundl.domain.repository.NavigationRepository

/**
 * Navigation State Machine for clean user flow management
 */
sealed class NavigationState {
    object Loading : NavigationState()
    object Onboarding : NavigationState()
    object LocationPermission : NavigationState()
    object NotificationPermission : NavigationState()
    object Login : NavigationState()
    data class Otp(val tid: String, val phoneNumber: String) : NavigationState()
    object Dashboard : NavigationState()
    
    companion object {
        /**
         * Determines the next navigation state based on current app state
         */
        suspend fun determineInitialState(
            isLoggedIn: Boolean,
            navigationRepository: NavigationRepository
        ): NavigationState {
            // Check permissions first - BOTH are mandatory for the app to function
            val hasLocationPermissions = navigationRepository.hasLocationPermissions()
            val hasNotificationPermissions = navigationRepository.hasNotificationPermissions()
            
            return when {
                // Missing location permission - highest priority
                !hasLocationPermissions -> LocationPermission
                
                // Has location but missing notification permission
                !hasNotificationPermissions -> NotificationPermission
                
                // Has both permissions and logged in - go to dashboard
                isLoggedIn -> Dashboard
                
                // Has both permissions but not logged in - start onboarding
                else -> Onboarding
            }
        }
        
        /**
         * Determines what comes after onboarding based on missing permissions
         */
        suspend fun determineNextAfterOnboarding(navigationRepository: NavigationRepository): NavigationState {
            val hasLocationPermissions = navigationRepository.hasLocationPermissions()
            val hasNotificationPermissions = navigationRepository.hasNotificationPermissions()
            
            return when {
                !hasLocationPermissions -> LocationPermission
                !hasNotificationPermissions -> NotificationPermission
                else -> Login
            }
        }
        
        /**
         * Determines what comes after location permission
         */
        suspend fun determineNextAfterLocation(
            navigationRepository: NavigationRepository,
            isLoggedIn: Boolean
        ): NavigationState {
            // Check notification permissions - MANDATORY for both logged-in and new users
            val hasNotificationPermissions = navigationRepository.hasNotificationPermissions()
            
            if (!hasNotificationPermissions) {
                // Need notification permission regardless of login status
                return NotificationPermission
            }
            
            // Has both location and notification permissions
            return if (isLoggedIn) {
                // Already logged in - go straight to dashboard
                Dashboard
            } else {
                // Not logged in - need to login first
                Login
            }
        }
        
        /**
         * Determines what comes after notification permission
         */
        fun determineNextAfterNotification(isLoggedIn: Boolean): NavigationState {
            return if (isLoggedIn) {
                // Already logged in - go to dashboard
                Dashboard
            } else {
                // Not logged in - go to login
                Login
            }
        }
    }
}

/**
 * Navigation Events - Actions that can trigger state changes
 */
sealed class NavigationEvent {
    object SplashCompleted : NavigationEvent()
    object OnboardingCompleted : NavigationEvent()
    object LocationPermissionGranted : NavigationEvent()
    object NotificationPermissionGranted : NavigationEvent()
    data class OtpRequested(val tid: String, val phoneNumber: String) : NavigationEvent()
    object LoginSuccessful : NavigationEvent()
    object LogoutRequested : NavigationEvent()
    object TokenExpired : NavigationEvent()
}

/**
 * Navigation result containing the new state and navigation behavior
 */
data class NavigationResult(
    val newState: NavigationState,
    val shouldClearBackstack: Boolean = false
)

/**
 * Navigation State Machine Manager
 */
class NavigationStateManager(private val navigationRepository: NavigationRepository) {
    
    suspend fun handleEvent(
        currentState: NavigationState,
        event: NavigationEvent,
        isLoggedIn: Boolean
    ): NavigationResult {
        return when (event) {
            NavigationEvent.SplashCompleted -> {
                val newState = NavigationState.determineInitialState(isLoggedIn, navigationRepository)
                NavigationResult(newState, shouldClearBackstack = false)
            }
            
            NavigationEvent.OnboardingCompleted -> {
                // Mark onboarding as seen in repository
                navigationRepository.markOnboardingAsSeen()
                val newState = NavigationState.determineNextAfterOnboarding(navigationRepository)
                NavigationResult(newState, shouldClearBackstack = false)
            }
            
            NavigationEvent.LocationPermissionGranted -> {
                val newState = NavigationState.determineNextAfterLocation(navigationRepository, isLoggedIn)
                NavigationResult(newState, shouldClearBackstack = false)
            }
            
            NavigationEvent.NotificationPermissionGranted -> {
                val newState = NavigationState.determineNextAfterNotification(isLoggedIn)
                NavigationResult(newState, shouldClearBackstack = false)
            }
            
            is NavigationEvent.OtpRequested -> {
                val newState = NavigationState.Otp(event.tid, event.phoneNumber)
                NavigationResult(newState, shouldClearBackstack = false)
            }
            
            NavigationEvent.LoginSuccessful -> {
                // Dashboard is the main destination - clear backstack
                NavigationResult(NavigationState.Dashboard, shouldClearBackstack = true)
            }
            
            NavigationEvent.LogoutRequested, 
            NavigationEvent.TokenExpired -> {
                // Start fresh onboarding flow
                NavigationResult(NavigationState.Onboarding, shouldClearBackstack = true)
            }
        }
    }
    
    /**
     * Convert navigation state to route for navigation
     */
    fun getRouteForState(state: NavigationState): String {
        return when (state) {
            NavigationState.Loading -> Route.Splash.route
            NavigationState.Onboarding -> Route.Onboarding.route
            NavigationState.LocationPermission -> Route.LocationPermission.route
            NavigationState.NotificationPermission -> Route.NotificationPermission.route
            NavigationState.Login -> Route.Login.route
            is NavigationState.Otp -> Route.Otp.createRoute(state.tid, state.phoneNumber)
            NavigationState.Dashboard -> Route.Dashboard.route
        }
    }
}
