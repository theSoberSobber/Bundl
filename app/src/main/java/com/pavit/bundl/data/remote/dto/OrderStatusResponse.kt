package com.pavit.bundl.data.remote.dto

data class OrderStatusResponse(
    val id: String,
    val amountNeeded: String,
    val totalPledge: String,
    val totalUsers: Int,
    val longitude: String,
    val latitude: String,
    val creatorId: String,
    val platform: String,
    val status: String,
    val pledgeMap: Map<String, Int>,
    val phoneNumerMap: Map<String, Int>? = null,
    val note: String? = null
)
