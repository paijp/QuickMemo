package com.example.quickmemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 1001
    }
    private lateinit var statusText: TextView
    private lateinit var toggleButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusText = findViewById(R.id.statusText)
        toggleButton = findViewById(R.id.toggleButton)
        toggleButton.setOnClickListener {
            if (MemoNotificationService.isRunning) stopMemoService() else startMemoService()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_CODE)
            }
        }
    }
    override fun onResume() { super.onResume(); updateUI() }
    private fun startMemoService() {
        ContextCompat.startForegroundService(this, Intent(this, MemoNotificationService::class.java))
        Toast.makeText(this, getString(R.string.msg_enabled), Toast.LENGTH_SHORT).show()
        toggleButton.postDelayed({ updateUI() }, 500)
    }
    private fun stopMemoService() {
        stopService(Intent(this, MemoNotificationService::class.java))
        Toast.makeText(this, getString(R.string.msg_disabled), Toast.LENGTH_SHORT).show()
        toggleButton.postDelayed({ updateUI() }, 300)
    }
    private fun updateUI() {
        if (MemoNotificationService.isRunning) {
            statusText.text = getString(R.string.status_active)
            toggleButton.text = getString(R.string.btn_disable)
        } else {
            statusText.text = getString(R.string.status_inactive)
            toggleButton.text = getString(R.string.btn_enable)
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, getString(R.string.msg_notif_granted), Toast.LENGTH_SHORT).show()
            else Toast.makeText(this, getString(R.string.msg_notif_required), Toast.LENGTH_LONG).show()
        }
    }
}
