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
import android.graphics.Rect
import android.graphics.Typeface
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import java.util.Calendar

class MemoNotificationService : Service() {

    companion object {
        const val CHANNEL_ID = "quick_memo_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_OPEN = "com.example.quickmemo.ACTION_OPEN"
        var isRunning = false; private set
        private var lastNotifText: String? = null

        fun updateNotification(context: Context) {
            if (!isRunning) return
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, buildNotification(context, false))
        }

        private fun trimBitmap(src: Bitmap): Bitmap {
            val w = src.width
            val h = src.height
            var top = 0; var bottom = h - 1; var left = 0; var right = w - 1

            outer@ for (y in 0 until h) { for (x in 0 until w) {
                if (src.getPixel(x, y) != Color.TRANSPARENT) { top = y; break@outer }
            } }
            outer@ for (y in h - 1 downTo 0) { for (x in 0 until w) {
                if (src.getPixel(x, y) != Color.TRANSPARENT) { bottom = y; break@outer }
            } }
            outer@ for (x in 0 until w) { for (y in 0 until h) {
                if (src.getPixel(x, y) != Color.TRANSPARENT) { left = x; break@outer }
            } }
            outer@ for (x in w - 1 downTo 0) { for (y in 0 until h) {
                if (src.getPixel(x, y) != Color.TRANSPARENT) { right = x; break@outer }
            } }

            val trimW = right - left + 1; val trimH = bottom - top + 1
            if (trimW <= 0 || trimH <= 0) return src
            return Bitmap.createBitmap(src, left, top, trimW, trimH)
        }

        private fun renderText(text: String, textSize: Float): Bitmap {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE; this.textSize = textSize
                typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.LEFT
            }
            val bounds = Rect()
            paint.getTextBounds(text, 0, text.length, bounds)
            val w = (bounds.width() + 4).coerceAtLeast(1)
            val h = (bounds.height() + 4).coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            Canvas(bmp).drawText(text, -bounds.left.toFloat() + 2, -bounds.top.toFloat() + 2, paint)
            return trimBitmap(bmp)
        }

        private fun createDateIcon(): IconCompat {
            val cal = Calendar.getInstance()
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH).toString()
            val jpDays = arrayOf("日", "月", "火", "水", "木", "金", "土")
            val dayOfWeek = jpDays[cal.get(Calendar.DAY_OF_WEEK) - 1]

            val weekBmp = renderText(dayOfWeek, 80f)
            val dayBmp = renderText(dayOfMonth, 120f)

            val totalH = weekBmp.height + dayBmp.height
            val maxW = maxOf(weekBmp.width, dayBmp.width)
            val scale = minOf(96f / maxW, 96f / totalH)
            val sWeekW = (weekBmp.width * scale).toInt().coerceAtLeast(1)
            val sWeekH = (weekBmp.height * scale).toInt().coerceAtLeast(1)
            val sDayW = (dayBmp.width * scale).toInt().coerceAtLeast(1)
            val sDayH = (dayBmp.height * scale).toInt().coerceAtLeast(1)

            val scaledWeek = Bitmap.createScaledBitmap(weekBmp, sWeekW, sWeekH, true)
            val scaledDay = Bitmap.createScaledBitmap(dayBmp, sDayW, sDayH, true)

            val outSize = 96
            val result = Bitmap.createBitmap(outSize, outSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            val combinedH = sWeekH + sDayH
            val yOffset = (outSize - combinedH) / 2f
            canvas.drawBitmap(scaledWeek, (outSize - sWeekW) / 2f, yOffset, null)
            canvas.drawBitmap(scaledDay, (outSize - sDayW) / 2f, yOffset + sWeekH, null)

            weekBmp.recycle(); dayBmp.recycle()
            scaledWeek.recycle(); scaledDay.recycle()

            return IconCompat.createWithBitmap(result)
        }

        fun buildNotification(context: Context, force: Boolean): Notification {
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

            lastNotifText = text

            val remoteViews = RemoteViews(context.packageName, R.layout.notification_memo)
            remoteViews.setTextViewText(R.id.notifText, text)
            remoteViews.setOnClickPendingIntent(R.id.notifRoot, broadcastPI)

            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(createDateIcon())
                .setCustomContentView(remoteViews)
                .setCustomBigContentView(remoteViews)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setWhen(Long.MAX_VALUE)
                .setSortKey("0")
                .setShowWhen(false)
                .setOnlyAlertOnce(true)
                .build()
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Only update if text would actually change
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            val locked = km.isKeyguardLocked
            val count = MemoRepository.count(context)

            val newText = if (locked) {
                context.getString(R.string.notif_locked)
            } else {
                if (count > 0) {
                    context.getString(R.string.notif_unlocked_count, count)
                } else {
                    context.getString(R.string.notif_unlocked_empty)
                }
            }

            if (newText != lastNotifText) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, buildNotification(context, false))
            }
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
        startForeground(NOTIFICATION_ID, buildNotification(this, true))
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
            CHANNEL_ID, "Quick Memo", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Persistent memo notification"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }
}
