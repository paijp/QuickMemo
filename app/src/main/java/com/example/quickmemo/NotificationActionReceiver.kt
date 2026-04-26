package com.example.quickmemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == MemoNotificationService.ACTION_OPEN) {
            val pending = goAsync()
            val handler = Handler(Looper.getMainLooper())

            // Close notification panel (works on Android 11 and below)
            @Suppress("DEPRECATION")
            try {
                context.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
            } catch (_: Exception) {}

            val memoIntent = Intent(context, MemoActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }

            context.startActivity(memoIntent)

            handler.postDelayed({
                try {
                    context.startActivity(memoIntent)
                } catch (_: Exception) {}
                pending.finish()
            }, 300)
        }
    }
}
