// data/repository/MailRepository.kt
package com.mailgw.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mailgw.data.api.MailApiService
import com.mailgw.data.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "mailgw_prefs")

class MailRepository(
    private val context: Context,
    private val apiService: MailApiService
) {
    companion object {
        private val ACCOUNTS_KEY = stringPreferencesKey("accounts")
        private val ACTIVE_ACCOUNT_KEY = stringPreferencesKey("active_account")
    }

    suspend fun getDomains(page: Int = 1): Result<List<Domain>> {
        return try {
            val response = apiService.getDomains(page)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.members)
            } else {
                Result.failure(Exception("Failed to fetch domains: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createEmailAccount(username: String, domain: String, password: String): Result<Account> {
        val address = "$username@$domain"
        val request = AccountRequest(address, password)

        return try {
            val createResponse = apiService.createAccount(request)
            if (!createResponse.isSuccessful) {
                return Result.failure(Exception("Failed to create account: ${createResponse.code()}"))
            }

            val tokenResponse = apiService.getToken(request)
            if (!tokenResponse.isSuccessful || tokenResponse.body() == null) {
                return Result.failure(Exception("Failed to get token"))
            }

            val accountData = createResponse.body()!!
            val token = tokenResponse.body()!!

            val account = Account(
                id = accountData.id,
                email = address,
                password = password,
                token = token.token,
                createdAt = System.currentTimeMillis()
            )

            saveAccount(account)
            Result.success(account)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveAccount(account: Account) {
        val accounts = getAllAccounts().getOrNull() ?: emptyList()
        val updatedAccounts = accounts + account
        val accountsJson = updatedAccounts.joinToString("|||") {
            "${it.id}|${it.email}|${it.password}|${it.token}|${it.createdAt}"
        }
        context.dataStore.edit { preferences ->
            preferences[ACCOUNTS_KEY] = accountsJson
        }
    }

    suspend fun getAllAccounts(): Result<List<Account>> {
        return try {
            //val preferences = context.dataStore.data.first().catch { }.firstOrNull()
            val preferences = context.dataStore.data.catch { }.firstOrNull()
            val accountsJson = preferences?.get(ACCOUNTS_KEY) ?: ""
            if (accountsJson.isEmpty()) {
                Result.success(emptyList())
            } else {
                val accounts = accountsJson.split("|||").mapNotNull { accountStr ->
                    val parts = accountStr.split("|")
                    if (parts.size == 5) {
                        Account(
                            id = parts[0],
                            email = parts[1],
                            password = parts[2],
                            token = parts[3],
                            createdAt = parts[4].toLongOrNull() ?: 0
                        )
                    } else null
                }
                Result.success(accounts)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setActiveAccount(account: Account) {
        context.dataStore.edit { preferences ->
            preferences[ACTIVE_ACCOUNT_KEY] = account.id
        }
    }

    fun getActiveAccountFlow(): Flow<Account?> {
        return context.dataStore.data.map { preferences ->
            val activeId = preferences[ACTIVE_ACCOUNT_KEY]
            if (activeId != null) {
                getAllAccounts().getOrNull()?.find { it.id == activeId }
            } else null
        }
    }

    suspend fun getMessages(account: Account, page: Int = 1): Result<List<MessageSummary>> {
        return try {
            val response = apiService.getMessages("Bearer ${account.token}", page)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.members)
            } else {
                Result.failure(Exception("Failed to fetch messages"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMessageDetail(account: Account, messageId: String): Result<MessageDetail> {
        return try {
            val response = apiService.getMessageDetail("Bearer ${account.token}", messageId)
            if (response.isSuccessful && response.body() != null) {
                if (!response.body()!!.seen) {
                    markMessageAsRead(account, messageId)
                }
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch message detail"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markMessageAsRead(account: Account, messageId: String): Result<Boolean> {
        return try {
            val response = apiService.markAsRead("Bearer ${account.token}", messageId)
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to mark as read"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(account: Account, messageId: String): Result<Boolean> {
        return try {
            val response = apiService.deleteMessage("Bearer ${account.token}", messageId)
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to delete message"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccountPermanently(account: Account): Result<Boolean> {
        return try {
            val response = apiService.deleteAccount("Bearer ${account.token}", account.id)
            if (response.isSuccessful) {
                val allAccounts = getAllAccounts().getOrNull() ?: emptyList()
                val updatedAccounts = allAccounts.filter { it.id != account.id }
                val accountsJson = updatedAccounts.joinToString("|||") {
                    "${it.id}|${it.email}|${it.password}|${it.token}|${it.createdAt}"
                }
                context.dataStore.edit { preferences ->
                    preferences[ACCOUNTS_KEY] = accountsJson
                }
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to delete account"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}