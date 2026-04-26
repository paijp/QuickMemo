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
            val memoIntent = Intent(context, MemoActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }

            // Try immediately
            context.startActivity(memoIntent)

            // Retry after short delay in case first attempt was blocked
            handler.postDelayed({
                try {
                    context.startActivity(memoIntent)
                } catch (_: Exception) {}
                pending.finish()
            }, 300)
        }
    }
}
