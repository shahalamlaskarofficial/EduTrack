package com.edutrack.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.edutrack.app.MainActivity
import com.edutrack.app.R
import com.edutrack.app.data.local.EduTrackDatabase
import com.edutrack.app.data.preferences.UserPreferencesManager
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class EduTrackReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = UserPreferencesManager(applicationContext).preferencesFlow.first()
        if (!prefs.notificationsEnabled) return Result.success()

        val customTitle = inputData.getString(KEY_TITLE)
        val customBody = inputData.getString(KEY_BODY)

        if (!customTitle.isNullOrBlank()) {
            showNotification(
                notificationId = inputData.getInt(KEY_NOTIFICATION_ID, 2001),
                title = customTitle,
                body = customBody ?: "Time to focus on your study mission."
            )
            return Result.success()
        }

        // Daily offline check for pending tasks & due revisions
        val dao = EduTrackDatabase.getInstance(applicationContext).eduTrackDao()
        val today = LocalDate.now().toEpochDay()
        val dueRevisions = dao.getDueRevisionsCountSync(today)
        val pendingTasks = dao.getPendingTasksCountSync(today)

        if (dueRevisions > 0 || pendingTasks > 0) {
            val parts = mutableListOf<String>()
            if (pendingTasks > 0) parts.add("$pendingTasks study mission${if (pendingTasks == 1) "" else "s"}")
            if (dueRevisions > 0) parts.add("$dueRevisions revision${if (dueRevisions == 1) "" else "s"} due")
            showNotification(
                notificationId = DAILY_NOTIFICATION_ID,
                title = "EduTrack Daily Study Check",
                body = "Today's queue: ${parts.joinToString(" and ")}. Every chapter brings you closer."
            )
        }

        return Result.success()
    }

    private fun showNotification(notificationId: Int, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "EduTrack Study & Revision Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Offline reminders for scheduled study missions and spaced-repetition revisions"
        }
        manager.createNotificationChannel(channel)

        val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
    }

    companion object {
        const val CHANNEL_ID = "edutrack_study_reminders"
        const val KEY_TITLE = "reminder_title"
        const val KEY_BODY = "reminder_body"
        const val KEY_NOTIFICATION_ID = "reminder_id"
        const val DAILY_NOTIFICATION_ID = 1001
    }
}
