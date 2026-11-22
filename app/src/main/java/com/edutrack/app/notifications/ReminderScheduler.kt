package com.edutrack.app.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val DAILY_WORK_NAME = "edutrack_daily_study_reminder"

    fun scheduleDailyCheck(context: Context) {
        val request = PeriodicWorkRequestBuilder<EduTrackReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(4, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            DAILY_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancelDailyCheck(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(DAILY_WORK_NAME)
    }

    fun scheduleOneTimeReminder(
        context: Context,
        uniqueKey: String,
        title: String,
        body: String,
        triggerAtMillis: Long,
        notificationId: Int
    ) {
        val delayMillis = (triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(5_000L)
        val data = workDataOf(
            EduTrackReminderWorker.KEY_TITLE to title,
            EduTrackReminderWorker.KEY_BODY to body,
            EduTrackReminderWorker.KEY_NOTIFICATION_ID to notificationId
        )
        val request = OneTimeWorkRequestBuilder<EduTrackReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            uniqueKey,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelReminder(context: Context, uniqueKey: String) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(uniqueKey)
    }
}
