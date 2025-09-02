package com.pavit.bundl.domain.model

data class CreditPackage(
    val id: String,
    val credits: Int,
    val price: Int,
    val name: String,
    val description: String
) 