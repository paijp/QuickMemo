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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import java.util.Calendar
import java.util.Locale

class MemoNotificationService : Service() {

    companion object {
        const val CHANNEL_ID = "quick_memo_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_OPEN = "com.example.quickmemo.ACTION_OPEN"
        var isRunning = false; private set

        fun updateNotification(context: Context) {
            if (!isRunning) return
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, buildNotification(context))
        }

        private fun createDateIcon(context: Context): android.graphics.drawable.Icon {
            val cal = Calendar.getInstance()
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH).toString()

            val jpDays = arrayOf("日", "月", "火", "水", "木", "金", "土")
            val dayOfWeek = jpDays[cal.get(Calendar.DAY_OF_WEEK) - 1]

            val size = 96
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }

            // Day of week (top)
            paint.textSize = 30f
            canvas.drawText(dayOfWeek, size / 2f, 32f, paint)

            // Day number (bottom, larger)
            paint.textSize = 48f
            canvas.drawText(dayOfMonth, size / 2f, 80f, paint)

            return android.graphics.drawable.Icon.createWithBitmap(bitmap)
        }

        fun buildNotification(context: Context): Notification {
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            val locked = km.isKeyguardLocked
            val count = MemoRepository.count(context)

            val broadcastIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_OPEN
            }
            val broadcastPI = PendingIntent.getBroadcast(
                context, 0, broadcastIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val text = if (locked) {
                context.getString(R.string.notif_locked)
            } else {
                if (count > 0) {
                    context.getString(R.string.notif_unlocked_count, count)
                } else {
                    context.getString(R.string.notif_unlocked_empty)
                }
            }

            val remoteViews = RemoteViews(context.packageName, R.layout.notification_memo)
            remoteViews.setTextViewText(R.id.notifText, text)
            remoteViews.setOnClickPendingIntent(R.id.notifRoot, broadcastPI)

            val icon = createDateIcon(context)

            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(icon)
                .setCustomContentView(remoteViews)
                .setCustomBigContentView(remoteViews)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
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
