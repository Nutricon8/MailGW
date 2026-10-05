// data/models/Domain.kt
package com.mailgw.data.models

import com.google.gson.annotations.SerializedName

data class DomainResponse(
    @SerializedName("hydra:member")
    val members: List<Domain>,
    @SerializedName("hydra:totalItems")
    val totalItems: Int
)

data class Domain(
    @SerializedName("@id")
    val id: String,
    val domain: String,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String
)