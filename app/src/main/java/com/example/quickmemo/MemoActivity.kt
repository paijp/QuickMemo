package com.example.quickmemo

import android.app.AlertDialog
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import java.util.Calendar

class MemoActivity : AppCompatActivity() {

    private lateinit var rootView: View
    private lateinit var memoInput: EditText
    private lateinit var listContainer: LinearLayout
    private lateinit var keywordContainer: LinearLayout
    private lateinit var btnEditKeywords: ImageButton
    private lateinit var calendarView: CompactCalendarView
    private var updateBanner: View? = null
    private var isLocked = false
    private var sessionAddedCount = 0

    companion object {
        private const val LONG_PRESS_DURATION = 600L
        private const val PROGRESS_INTERVAL = 16L
    }

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true); setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        setContentView(R.layout.activity_memo)

        rootView = findViewById(android.R.id.content)
        memoInput = findViewById(R.id.memoInput)
        listContainer = findViewById(R.id.listContainer)
        keywordContainer = findViewById(R.id.keywordContainer)
        btnEditKeywords = findViewById(R.id.btnEditKeywords)
        calendarView = findViewById(R.id.calendarView)
        updateBanner = findViewById(R.id.updateBanner)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val text = memoInput.text.toString().trim()
            if (text.isNotEmpty()) {
                MemoRepository.add(this, text); sessionAddedCount++
                memoInput.text.clear(); refreshList()
                MemoNotificationService.updateNotification(this)
            }
        }
        btnEditKeywords.setOnClickListener { showEditKeywordsDialog() }

        calendarView.onDateTap = { year, month, day ->
            val now = Calendar.getInstance()
            val tapped = Calendar.getInstance().apply { set(year, month, day) }
            val diffMs = kotlin.math.abs(now.timeInMillis - tapped.timeInMillis)
            val sixMonthsMs = 6L * 30 * 24 * 60 * 60 * 1000
            val dateStr = if (diffMs <= sixMonthsMs) "${month + 1}/${day}"
                else "${year}/${month + 1}/${day}"
            val start = memoInput.selectionStart.coerceAtLeast(0)
            val end = memoInput.selectionEnd.coerceAtLeast(0)
            memoInput.text.replace(start.coerceAtMost(end), start.coerceAtLeast(end), dateStr)
        }

        memoInput.requestFocus()
        HolidayRepository.fetchIfNeeded(this) { calendarView.refreshHolidays() }
    }

    override fun onResume() {
        super.onResume()
        val km = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        isLocked = km.isKeyguardLocked
        btnEditKeywords.visibility = if (isLocked) View.GONE else View.VISIBLE
        refreshKeywords(); refreshList()

        // Check for updates (only when unlocked)
        if (!isLocked) {
            UpdateChecker.checkIfNeeded(this) { info ->
                if (info != null) showUpdateBanner(info) else hideUpdateBanner()
            }
        } else {
            hideUpdateBanner()
        }
    }

    override fun onDestroy() {
        try { unregisterReceiver(screenOffReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    private fun showUpdateBanner(info: UpdateInfo) {
        updateBanner?.visibility = View.VISIBLE
        updateBanner?.findViewById<TextView>(R.id.updateText)?.text =
            getString(R.string.update_available, info.versionName)
        updateBanner?.findViewById<Button>(R.id.btnUpdate)?.setOnClickListener {
            UpdateChecker.openDownload(this, info)
        }
        updateBanner?.findViewById<Button>(R.id.btnUpdateDetails)?.setOnClickListener {
            UpdateChecker.openReleaseNotes(this, info)
        }
    }

    private fun hideUpdateBanner() {
        updateBanner?.visibility = View.GONE
    }

    private fun refreshKeywords() {
        keywordContainer.removeAllViews()
        for (kw in KeywordRepository.getAll(this)) {
            val btn = Button(this).apply {
                text = kw; textSize = 13f; setTextColor(0xFFDDDDDD.toInt())
                setBackgroundResource(R.drawable.bg_keyword_button)
                setPadding(28, 12, 28, 12)
                minHeight = 0; minimumHeight = 0; minWidth = 0; minimumWidth = 0; isAllCaps = false
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 8 }
            }
            btn.setOnClickListener {
                val s = memoInput.selectionStart.coerceAtLeast(0)
                val e = memoInput.selectionEnd.coerceAtLeast(0)
                memoInput.text.replace(s.coerceAtMost(e), s.coerceAtLeast(e), kw)
            }
            keywordContainer.addView(btn)
        }
    }

    private fun refreshList() {
        listContainer.removeAllViews()
        val all = MemoRepository.getAll(this)
        if (all.isEmpty()) { addInfoRow(getString(R.string.no_memos)); return }
        if (isLocked) {
            val show = sessionAddedCount.coerceAtMost(all.size)
            for (i in 0 until show) addMemoRow(all[i], i, false)
            val hidden = all.size - show
            if (hidden > 0) addInfoRow(getString(R.string.other_items, hidden))
        } else { for (i in all.indices) addMemoRow(all[i], i, true) }
    }

    private fun addMemoRow(text: String, index: Int, deletable: Boolean) {
        val row = layoutInflater.inflate(R.layout.item_memo, listContainer, false)
        row.findViewById<TextView>(R.id.memoText).text = text
        val pb = row.findViewById<View>(R.id.progressBar)
        if (deletable) {
            row.alpha = 0f
            row.animate().alpha(1f).setDuration(300).setStartDelay((index * 50).toLong()).start()
            setupLongPressDelete(row, pb, index)
        } else pb.visibility = View.GONE
        listContainer.addView(row)
    }

    private fun setupLongPressDelete(row: View, pb: View, index: Int) {
        val h = Handler(Looper.getMainLooper()); var pressing = false; var elapsed = 0L
        val r = object : Runnable { override fun run() {
            if (!pressing) return; elapsed += PROGRESS_INTERVAL
            pb.scaleX = (elapsed.toFloat() / LONG_PRESS_DURATION).coerceAtMost(1f)
            if (elapsed >= LONG_PRESS_DURATION) { pressing = false; pb.scaleX = 0f; performDelete(index) }
            else h.postDelayed(this, PROGRESS_INTERVAL)
        } }
        row.setOnTouchListener { _, ev -> when (ev.action) {
            MotionEvent.ACTION_DOWN -> { pressing = true; elapsed = 0L; pb.visibility = View.VISIBLE
                pb.scaleX = 0f; h.postDelayed(r, PROGRESS_INTERVAL); true }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { pressing = false; elapsed = 0L
                pb.scaleX = 0f; h.removeCallbacks(r); true }
            else -> false
        } }
    }

    private fun performDelete(index: Int) {
        val dt = MemoRepository.getAll(this).getOrNull(index) ?: return
        MemoRepository.removeAt(this, index); refreshList()
        MemoNotificationService.updateNotification(this)
        Snackbar.make(rootView, getString(R.string.deleted), Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.btn_undo)) {
                MemoRepository.insertAt(this, index, dt); refreshList()
                MemoNotificationService.updateNotification(this)
            }.setActionTextColor(0xFF5DCAA5.toInt()).show()
    }

    private fun addInfoRow(text: String) {
        listContainer.addView(TextView(this).apply {
            this.text = text; setTextColor(0xFF888888.toInt()); textSize = 14f; setPadding(0, 24, 0, 24)
        })
    }

    private fun showEditKeywordsDialog() {
        val old = KeywordRepository.getAll(this)
        val input = EditText(this).apply {
            setText(old.joinToString(", ")); hint = getString(R.string.keyword_edit_hint)
            setTextColor(0xFF222222.toInt()); textSize = 15f; setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this).setTitle(getString(R.string.keyword_edit_title)).setView(input)
            .setPositiveButton(getString(R.string.btn_save)) { _, _ ->
                val list = input.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (list.isNotEmpty()) {
                    KeywordRepository.save(this, list); refreshKeywords()
                    Snackbar.make(rootView, getString(R.string.keywords_changed), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.btn_undo)) {
                            KeywordRepository.save(this, old); refreshKeywords()
                        }.setActionTextColor(0xFF5DCAA5.toInt()).show()
                }
            }.setNegativeButton(getString(R.string.btn_cancel), null).show()
    }
}
