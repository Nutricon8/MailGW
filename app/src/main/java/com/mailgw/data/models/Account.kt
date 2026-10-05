// data/models/Account.kt
package com.mailgw.data.models

import com.google.gson.annotations.SerializedName

data class AccountRequest(
    val address: String,
    val password: String
)

data class AccountResponse(
    @SerializedName("@id")
    val id: String,
    val address: String,
    val quota: Int,
    val used: Int,
    val isDisabled: Boolean,
    val isDeleted: Boolean,
    val createdAt: String,
    val retentionAt: String?,
    val updatedAt: String
)

data class TokenResponse(
    val id: String,
    val token: String
)

data class Account(
    val id: String,
    val email: String,
    val password: String,
    val token: String,
    val createdAt: Long
)