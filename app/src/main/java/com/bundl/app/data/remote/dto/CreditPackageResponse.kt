package com.bundl.app.data.remote.dto

import com.bundl.app.domain.model.CreditPackage

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