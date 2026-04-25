package com.example.quickmemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            MemoNotificationService.ACTION_NEW_MEMO, MemoNotificationService.ACTION_OPEN_MEMO -> {
                context.startActivity(Intent(context, MemoActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                })
            }
            MemoNotificationService.ACTION_LIST -> Toast.makeText(context, "List: not implemented", Toast.LENGTH_SHORT).show()
            MemoNotificationService.ACTION_REMIND -> Toast.makeText(context, "Remind: not implemented", Toast.LENGTH_SHORT).show()
            MemoNotificationService.ACTION_PHOTO -> Toast.makeText(context, "Photo: not implemented", Toast.LENGTH_SHORT).show()
        }
    }
}
