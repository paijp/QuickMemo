package com.example.quickmemo

import android.app.AlertDialog
import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MemoActivity : AppCompatActivity() {

    private lateinit var memoInput: EditText
    private lateinit var listContainer: LinearLayout
    private lateinit var keywordContainer: LinearLayout
    private lateinit var btnEditKeywords: ImageButton
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

        setContentView(R.layout.activity_memo)

        memoInput = findViewById(R.id.memoInput)
        listContainer = findViewById(R.id.listContainer)
        keywordContainer = findViewById(R.id.keywordContainer)
        btnEditKeywords = findViewById(R.id.btnEditKeywords)

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

        btnEditKeywords.setOnClickListener { showEditKeywordsDialog() }

        memoInput.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        isLocked = km.isKeyguardLocked
        btnEditKeywords.visibility = if (isLocked) android.view.View.GONE else android.view.View.VISIBLE
        refreshKeywords()
        refreshList()
    }

    private fun refreshKeywords() {
        keywordContainer.removeAllViews()
        val keywords = KeywordRepository.getAll(this)
        for (kw in keywords) {
            val btn = Button(this).apply {
                text = kw
                textSize = 13f
                setTextColor(0xFFDDDDDD.toInt())
                setBackgroundResource(R.drawable.bg_keyword_button)
                setPadding(28, 12, 28, 12)
                minHeight = 0
                minimumHeight = 0
                minWidth = 0
                minimumWidth = 0
                isAllCaps = false
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.marginEnd = 8
                layoutParams = lp
            }
            btn.setOnClickListener {
                val start = memoInput.selectionStart.coerceAtLeast(0)
                val end = memoInput.selectionEnd.coerceAtLeast(0)
                memoInput.text.replace(start.coerceAtMost(end), start.coerceAtLeast(end), kw)
            }
            keywordContainer.addView(btn)
        }
    }

    private fun refreshList() {
        listContainer.removeAllViews()
        val all = MemoRepository.getAll(this)

        if (all.isEmpty()) {
            addInfoRow(getString(R.string.no_memos))
            return
        }

        if (isLocked) {
            val showCount = sessionAddedCount.coerceAtMost(all.size)
            for (i in 0 until showCount) {
                addMemoRow(all[i], i, false)
            }
            val hiddenCount = all.size - showCount
            if (hiddenCount > 0) {
                addInfoRow(getString(R.string.other_items, hiddenCount))
            }
        } else {
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
            row.alpha = 0f
            row.animate().alpha(1f).setDuration(300).setStartDelay((index * 50).toLong()).start()

            row.setOnLongClickListener {
                val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
                row.startAnimation(shake)
                row.postDelayed({
                    MemoRepository.removeAt(this, index)
                    refreshList()
                    MemoNotificationService.updateNotification(this)
                    Toast.makeText(this, getString(R.string.deleted), Toast.LENGTH_SHORT).show()
                }, 400)
                true
            }
        }

        listContainer.addView(row)
    }

    private fun addInfoRow(text: String) {
        val tv = TextView(this).apply {
            this.text = text
            setTextColor(0xFF888888.toInt())
            textSize = 14f
            setPadding(0, 24, 0, 24)
        }
        listContainer.addView(tv)
    }

    private fun showEditKeywordsDialog() {
        val current = KeywordRepository.getAll(this)
        val input = EditText(this).apply {
            setText(current.joinToString(", "))
            hint = getString(R.string.keyword_edit_hint)
            setTextColor(0xFF222222.toInt())
            textSize = 15f
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.keyword_edit_title))
            .setView(input)
            .setPositiveButton(getString(R.string.btn_save)) { _, _ ->
                val text = input.text.toString()
                val list = text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (list.isNotEmpty()) {
                    KeywordRepository.save(this, list)
                    refreshKeywords()
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }
}
