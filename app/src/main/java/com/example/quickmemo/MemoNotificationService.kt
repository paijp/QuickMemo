package com.example.quickmemo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat

class MemoNotificationService : Service() {
    companion object {
        const val CHANNEL_ID = "quick_memo_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_OPEN_MEMO = "com.example.quickmemo.ACTION_OPEN_MEMO"
        const val ACTION_NEW_MEMO = "com.example.quickmemo.ACTION_NEW_MEMO"
        const val ACTION_LIST = "com.example.quickmemo.ACTION_LIST"
        const val ACTION_REMIND = "com.example.quickmemo.ACTION_REMIND"
        const val ACTION_PHOTO = "com.example.quickmemo.ACTION_PHOTO"
        var isRunning = false; private set
    }
    override fun onCreate() { super.onCreate(); createNotificationChannel() }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true; startForeground(NOTIFICATION_ID, buildNotification()); return START_STICKY
    }
    override fun onDestroy() { isRunning = false; super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Quick Memo", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Persistent memo notification on lock screen"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
        }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val rv = RemoteViews(packageName, R.layout.notification_memo)
        val openPI = PendingIntent.getActivity(this, 0,
            Intent(this, MemoActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        rv.setOnClickPendingIntent(R.id.memoTapArea, openPI)
        fun actionPI(reqCode: Int, action: String) = PendingIntent.getBroadcast(this, reqCode,
            Intent(this, NotificationActionReceiver::class.java).apply { this.action = action },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        rv.setOnClickPendingIntent(R.id.btnNotifNew, actionPI(1, ACTION_NEW_MEMO))
        rv.setOnClickPendingIntent(R.id.btnNotifList, actionPI(2, ACTION_LIST))
        rv.setOnClickPendingIntent(R.id.btnNotifRemind, actionPI(3, ACTION_REMIND))
        rv.setOnClickPendingIntent(R.id.btnNotifPhoto, actionPI(4, ACTION_PHOTO))
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_memo)
            .setCustomContentView(rv).setCustomBigContentView(rv)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true).setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openPI).build()
    }
}
