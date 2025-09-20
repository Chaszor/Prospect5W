package com.yourname.prospect5w.notify

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.yourname.prospect5w.MainActivity
import com.yourname.prospect5w.R

class FollowUpWorker(appCtx: Context, params: WorkerParameters) : CoroutineWorker(appCtx, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getLong("interactionId", -1)
        if (id <= 0) return Result.success()

        val intent = PendingIntent.getActivity(
            applicationContext, 0,
            MainActivity.intent(applicationContext), PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(applicationContext, CHANNEL_FOLLOWUPS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Follow-up due")
            .setContentText("You have a scheduled follow-up.")
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(id.toInt(), notif)
        return Result.success()
    }
}

class ReminderScheduler(private val ctx: Context) {
    fun schedule(interactionId: Long, atMillis: Long) {
        val delay = (atMillis - System.currentTimeMillis()).coerceAtLeast(0)
        val req = OneTimeWorkRequestBuilder<FollowUpWorker>()
            .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("interactionId" to interactionId))
            .build()
        WorkManager.getInstance(ctx).enqueue(req)
    }
}
