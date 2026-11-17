package com.twinhearts.app.feature.chat.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/**
 * Why this file exists: Android runs this by itself as soon as the phone has internet again, even if
 * the app was closed in between, and sends the chat messages that were waiting in [ChatOutbox].
 * If the connection drops again halfway, it asks Android to try again later (growing pauses).
 */
class ChatOutboxWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun chatRepository(): ChatRepository
    }

    override suspend fun doWork(): Result {
        val repository = EntryPointAccessors.fromApplication(applicationContext, Deps::class.java).chatRepository()
        return if (repository.flushOutbox()) Result.success() else Result.retry()
    }

    companion object {
        private const val UNIQUE_NAME = "chat-outbox"

        /** Safe to call as often as needed: if a run is already waiting, this keeps it. */
        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<ChatOutboxWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()
            runCatching {
                WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.KEEP, request)
            }
        }
    }
}
