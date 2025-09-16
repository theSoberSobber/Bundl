package com.pavit.bundl.presentation.navigation

sealed class Route(val route: String) {
    object Splash : Route("splash")
    object Onboarding : Route("onboarding")
    object LocationPermission : Route("location_permission")
    object NotificationPermission : Route("notification_permission")
    object Login : Route("login")
    object Otp : Route("otp/{tid}/{phoneNumber}") {
        fun createRoute(tid: String, phoneNumber: String): String {
            return "otp/$tid/$phoneNumber"
        }
    }
    object Dashboard : Route("dashboard")
    object GetMoreCredits : Route("get_more_credits")
    object DummyScreen : Route("dummy_screen")
    object MyOrders : Route("my_orders")
    object Chat : Route("chat/{orderId}") {
        fun createRoute(orderId: String): String {
            return "chat/$orderId"
        }
    }
} 