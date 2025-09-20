package com.yourname.prospect5w.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

const val CHANNEL_FOLLOWUPS = "followups"

fun ensureNotificationChannel(ctx: Context) {
    if (Build.VERSION.SDK_INT >= 26) {
        val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(CHANNEL_FOLLOWUPS, "Follow-ups", NotificationManager.IMPORTANCE_DEFAULT)
        mgr.createNotificationChannel(ch)
    }
}
