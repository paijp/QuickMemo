package com.example.quickmemo

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MemoActivity : AppCompatActivity() {

    private lateinit var memoInput: EditText
    private lateinit var listContainer: LinearLayout
    private var isLocked = false
    private var sessionAddedCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        isLocked = km.isKeyguardLocked

        setContentView(R.layout.activity_memo)

        memoInput = findViewById(R.id.memoInput)
        listContainer = findViewById(R.id.listContainer)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val text = memoInput.text.toString().trim()
            if (text.isNotEmpty()) {
                MemoRepository.add(this, text)
                sessionAddedCount++
                memoInput.text.clear()
                refreshList()
                MemoNotificationService.updateNotification(this)
            }
        }

        memoInput.requestFocus()
        refreshList()
    }

    private fun refreshList() {
        listContainer.removeAllViews()
        val all = MemoRepository.getAll(this)

        if (all.isEmpty()) {
            addInfoRow("No memos yet")
            return
        }

        if (isLocked) {
            // Show only items added in this session
            val showCount = sessionAddedCount.coerceAtMost(all.size)
            for (i in 0 until showCount) {
                addMemoRow(all[i], i, false)
            }
            val hiddenCount = all.size - showCount
            if (hiddenCount > 0) {
                addInfoRow("Other $hiddenCount item(s)")
            }
        } else {
            // Unlocked: show all, allow long-press delete
            for (i in all.indices) {
                addMemoRow(all[i], i, true)
            }
        }
    }

    private fun addMemoRow(text: String, index: Int, deletable: Boolean) {
        val row = layoutInflater.inflate(R.layout.item_memo, listContainer, false)
        val tv = row.findViewById<TextView>(R.id.memoText)
        tv.text = text

        if (deletable) {
            // Subtle animation hint: gentle pulse on first appear
            row.alpha = 0f
            row.animate().alpha(1f).setDuration(300).setStartDelay((index * 50).toLong()).start()

            row.setOnLongClickListener {
                // Shake animation then remove
                val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
                row.startAnimation(shake)
                row.postDelayed({
                    MemoRepository.removeAt(this, index)
                    refreshList()
                    MemoNotificationService.updateNotification(this)
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                }, 400)
                true
            }
        }

        listContainer.addView(row)
    }

    private fun addInfoRow(text: String) {
        val tv = TextView(this).apply {
            this.text = text
            setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            textSize = 14f
            setPadding(0, 24, 0, 24)
        }
        listContainer.addView(tv)
    }
}
