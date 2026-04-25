package com.example.quickmemo

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MemoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen WITHOUT dismissing keyguard (no password prompt)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            // Do NOT call requestDismissKeyguard - it triggers password input
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                // Do NOT include FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_memo)

        val memoInput = findViewById<EditText>(R.id.memoInput)

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val text = memoInput.text.toString().trim()
            if (text.isNotEmpty()) {
                Toast.makeText(this, "Memo saved: $text", Toast.LENGTH_SHORT).show()
            }
            finish()
        }

        findViewById<Button>(R.id.btnDiscard).setOnClickListener {
            Toast.makeText(this, "Memo discarded", Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<ImageButton>(R.id.btnNewMemo).setOnClickListener {
            memoInput.text.clear()
            memoInput.requestFocus()
            Toast.makeText(this, "New memo", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageButton>(R.id.btnList).setOnClickListener {
            Toast.makeText(this, "List: not implemented in this sample", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageButton>(R.id.btnRemind).setOnClickListener {
            Toast.makeText(this, "Remind: not implemented in this sample", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageButton>(R.id.btnPhoto).setOnClickListener {
            Toast.makeText(this, "Photo: not implemented in this sample", Toast.LENGTH_SHORT).show()
        }

        memoInput.requestFocus()
    }
}
