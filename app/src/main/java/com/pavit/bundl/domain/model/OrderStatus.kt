package com.pavit.bundl.domain.model

data class OrderStatus(
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
    val phoneNumberMap: Map<String, Int>? = null,
    val note: String? = null
)
