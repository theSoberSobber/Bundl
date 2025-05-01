package com.orvio.app.presentation.navigation

sealed class Route(val route: String) {
    object Splash : Route("splash")
    object Onboarding : Route("onboarding")
    object PhonePermission : Route("phone_permission")
    object SmsPermission : Route("sms_permission")
    object Login : Route("login")
    object Otp : Route("otp/{transactionId}/{phoneNumber}") {
        fun createRoute(transactionId: String, phoneNumber: String): String {
            return "otp/$transactionId/$phoneNumber"
        }
    }
    object Dashboard : Route("dashboard")
} 