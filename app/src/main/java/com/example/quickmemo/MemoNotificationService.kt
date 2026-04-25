package com.example.quickmemo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MemoNotificationService : Service() {

    companion object {
        const val CHANNEL_ID = "quick_memo_channel"
        const val NOTIFICATION_ID = 1001
        var isRunning = false; private set

        fun updateNotification(context: Context) {
            if (!isRunning) return
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, buildNotification(context))
        }

        fun buildNotification(context: Context): Notification {
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            val locked = km.isKeyguardLocked

            val openIntent = Intent(context, MemoActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPI = PendingIntent.getActivity(
                context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val count = MemoRepository.count(context)

            val text = if (locked) {
                context.getString(R.string.notif_locked)
            } else {
                if (count > 0) {
                    context.getString(R.string.notif_unlocked_count, count)
                } else {
                    context.getString(R.string.notif_unlocked_empty)
                }
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_memo)
                .setContentText(text)
                .setContentIntent(openPI)
                .setFullScreenIntent(openPI, true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)

            return builder.build()
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateNotification(context)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        startForeground(NOTIFICATION_ID, buildNotification(this))
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        unregisterReceiver(screenReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Quick Memo", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Persistent memo notification"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }
}
