package com.pavit.bundl.data.remote.dto

import com.pavit.bundl.domain.model.CreditPackage

data class CreditPackageResponse(
    val packages: List<CreditPackageDto>
)

data class CreditPackageDto(
    val id: String,
    val credits: Int,
    val price: Int,
    val name: String,
    val description: String
) {
    fun toCreditPackage(): CreditPackage {
        return CreditPackage(
            id = id,
            credits = credits,
            price = price,
            name = name,
            description = description
        )
    }
} 