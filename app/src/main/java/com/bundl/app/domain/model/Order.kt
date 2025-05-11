package com.bundl.app.domain.model

data class Order(
    val id: String,
    val status: String,
    val creatorId: String,
    val amountNeeded: Int,
    val pledgeMap: Map<String, Int>,
    val phoneNumberMap: Map<String, Int>? = null,
    val note: String? = null,
    val totalPledge: Int,
    val totalUsers: Int,
    val platform: String,
    val latitude: Double,
    val longitude: Double,
    val totalAmount: Int = 0,
    val distanceKm: Double? = null,
    val createdAt: String? = null,
    val credits: Int = 50 // Default value for demo purposes
) 