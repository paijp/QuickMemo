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
            // Close notification shade first, then launch activity
            @Suppress("DEPRECATION")
            context.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))

            Handler(Looper.getMainLooper()).postDelayed({
                val memoIntent = Intent(context, MemoActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
                context.startActivity(memoIntent)
                pending.finish()
            }, 150)
        }
    }
}
