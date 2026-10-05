// workers/MessagePollingWorker.kt
package com.mailgw.workers

import android.content.Context
import androidx.work.*
import com.mailgw.data.repository.MailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MessagePollingWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: MailRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val accounts = repository.getAllAccounts().getOrNull() ?: emptyList()

                for (account in accounts) {
                    val messages = repository.getMessages(account).getOrNull()
                    messages?.let { unreadMessages ->
                        val newUnread = unreadMessages.filter { !it.seen }
                        if (newUnread.isNotEmpty()) {
                            // Send notification for new messages
                            sendNotification(account.email, newUnread.size)
                        }
                    }
                }
                Result.success()
            } catch (e: Exception) {
                Result.retry()
            }
        }
    }

    private fun sendNotification(email: String, count: Int) {
        // Implement notification logic
    }

    companion object {
        fun startPeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<MessagePollingWorker>(
                15, TimeUnit.MINUTES
            ).setConstraints(constraints).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "message_polling",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}