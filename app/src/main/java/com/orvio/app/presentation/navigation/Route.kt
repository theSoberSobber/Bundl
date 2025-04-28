package com.orvio.app.presentation.navigation

sealed class Route(val route: String) {
    object Login : Route("login")
    object Otp : Route("otp/{transactionId}/{phoneNumber}") {
        fun createRoute(transactionId: String, phoneNumber: String): String {
            return "otp/$transactionId/$phoneNumber"
        }
    }
    object Dashboard : Route("dashboard")
} 