package com.bundl.app.domain.model

import java.util.Date

data class ApiKey(
    val id: String,
    val name: String,
    val key: String,
    val createdAt: Date,
    val lastUsed: Date?
) 