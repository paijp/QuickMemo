package com.example.quickmemo

import android.content.Intent
import android.service.quicksettings.TileService

class MemoTileService : TileService() {
    override fun onClick() {
        super.onClick()
        startActivityAndCollapse(Intent(this, MemoActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        })
    }
}
