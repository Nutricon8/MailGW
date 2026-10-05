// data/api/MailApiService.kt
package com.mailgw.data.api

import com.mailgw.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface MailApiService {
    @GET("/domains")
    suspend fun getDomains(@Query("page") page: Int = 1): Response<DomainResponse>

    @POST("/accounts")
    suspend fun createAccount(@Body request: AccountRequest): Response<AccountResponse>

    @POST("/token")
    suspend fun getToken(@Body request: AccountRequest): Response<TokenResponse>

    @GET("/me")
    suspend fun getCurrentAccount(@Header("Authorization") token: String): Response<AccountResponse>

    @GET("/messages")
    suspend fun getMessages(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1
    ): Response<MessagesResponse>

    @GET("/messages/{id}")
    suspend fun getMessageDetail(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<MessageDetail>

    @PATCH("/messages/{id}")
    suspend fun markAsRead(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Map<String, Boolean>>

    @DELETE("/messages/{id}")
    suspend fun deleteMessage(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Unit>

    @DELETE("/accounts/{id}")
    suspend fun deleteAccount(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Unit>
}