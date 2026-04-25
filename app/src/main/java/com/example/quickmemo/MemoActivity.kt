package com.example.quickmemo

import android.app.AlertDialog
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

class MemoActivity : AppCompatActivity() {

    private lateinit var rootView: View
    private lateinit var memoInput: EditText
    private lateinit var listContainer: LinearLayout
    private lateinit var keywordContainer: LinearLayout
    private lateinit var btnEditKeywords: ImageButton
    private var isLocked = false
    private var sessionAddedCount = 0

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen - works on API 27+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        // Also set window flags for broader compatibility (API 27 / Android 8.1)
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Listen for screen off to finish activity
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

        setContentView(R.layout.activity_memo)

        rootView = findViewById(android.R.id.content)
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
        btnEditKeywords.visibility = if (isLocked) View.GONE else View.VISIBLE
        refreshKeywords()
        refreshList()
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (_: Exception) {}
        super.onDestroy()
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
                    val deletedText = MemoRepository.getAll(this).getOrNull(index) ?: return@postDelayed
                    val deletedIndex = index
                    MemoRepository.removeAt(this, deletedIndex)
                    refreshList()
                    MemoNotificationService.updateNotification(this)

                    Snackbar.make(rootView, getString(R.string.deleted), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.btn_undo)) {
                            MemoRepository.insertAt(this, deletedIndex, deletedText)
                            refreshList()
                            MemoNotificationService.updateNotification(this)
                        }
                        .setActionTextColor(0xFF5DCAA5.toInt())
                        .show()
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
        val oldKeywords = KeywordRepository.getAll(this)
        val input = EditText(this).apply {
            setText(oldKeywords.joinToString(", "))
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
                val newList = text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (newList.isNotEmpty()) {
                    KeywordRepository.save(this, newList)
                    refreshKeywords()

                    Snackbar.make(rootView, getString(R.string.keywords_changed), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.btn_undo)) {
                            KeywordRepository.save(this, oldKeywords)
                            refreshKeywords()
                        }
                        .setActionTextColor(0xFF5DCAA5.toInt())
                        .show()
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }
}
