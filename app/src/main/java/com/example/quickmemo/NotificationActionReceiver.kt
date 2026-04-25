package com.example.quickmemo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Not used - actions handled via PendingIntent.getActivity in addAction
    }
}
