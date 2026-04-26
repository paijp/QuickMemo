package com.example.quickmemo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import java.util.Calendar
import java.util.Locale

class CompactCalendarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val calendar = Calendar.getInstance()
    private var displayYear = calendar.get(Calendar.YEAR)
    private var displayMonth = calendar.get(Calendar.MONTH)

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val jpWeekdays = arrayOf("日", "月", "火", "水", "木", "金", "土")
    private val headerHeight get() = cellH * 1.2f
    private var cellW = 0f
    private var cellH = 0f

    private val leftArrowRect = RectF()
    private val rightArrowRect = RectF()

    fun refreshHolidays() {
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        cellW = w / 7f
        cellH = cellW * 0.75f

        val daysInMonth = getDaysInMonth(displayYear, displayMonth)
        val firstDow = getFirstDayOfWeek(displayYear, displayMonth)
        val totalCells = firstDow + daysInMonth
        val rows = (totalCells + 6) / 7

        val totalH = (headerHeight + cellH + cellH * rows).toInt()
        setMeasuredDimension(w, totalH)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        cellW = w / 7f
        cellH = cellW * 0.75f

        drawHeader(canvas, w)
        drawWeekdays(canvas)
        drawDays(canvas)
    }

    private fun drawHeader(canvas: Canvas, w: Float) {
        val headerText = if (Locale.getDefault().language == "ja") {
            "${displayYear}年 ${displayMonth + 1}月"
        } else {
            val monthNames = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
            "${monthNames[displayMonth]} $displayYear"
        }

        textPaint.apply {
            color = 0xFFDDDDDD.toInt()
            textSize = cellH * 0.55f
        }
        canvas.drawText(headerText, w / 2f, headerHeight * 0.7f, textPaint)

        textPaint.textSize = cellH * 0.6f
        val arrowY = headerHeight * 0.7f
        canvas.drawText("◀", cellW, arrowY, textPaint)
        canvas.drawText("▶", w - cellW, arrowY, textPaint)

        leftArrowRect.set(0f, 0f, cellW * 2, headerHeight)
        rightArrowRect.set(w - cellW * 2, 0f, w, headerHeight)
    }

    private fun drawWeekdays(canvas: Canvas) {
        val y = headerHeight + cellH * 0.7f
        textPaint.textSize = cellH * 0.45f

        for (i in 0..6) {
            textPaint.color = when (i) {
                0 -> 0xFFE05050.toInt()
                6 -> 0xFF5599CC.toInt()
                else -> 0xFFDDDDDD.toInt()
            }
            canvas.drawText(jpWeekdays[i], cellW * i + cellW / 2f, y, textPaint)
        }
    }

    private fun drawDays(canvas: Canvas) {
        val today = Calendar.getInstance()
        val isCurrentMonth = displayYear == today.get(Calendar.YEAR) &&
                displayMonth == today.get(Calendar.MONTH)
        val todayDate = today.get(Calendar.DAY_OF_MONTH)

        val daysInMonth = getDaysInMonth(displayYear, displayMonth)
        val firstDow = getFirstDayOfWeek(displayYear, displayMonth)

        textPaint.textSize = cellH * 0.5f
        val baseY = headerHeight + cellH

        for (day in 1..daysInMonth) {
            val cellIndex = firstDow + day - 1
            val col = cellIndex % 7
            val row = cellIndex / 7

            val cx = cellW * col + cellW / 2f
            val cy = baseY + cellH * row + cellH / 2f

            val isHoliday = HolidayRepository.isHoliday(context, displayYear, displayMonth, day)

            if (isCurrentMonth && day == todayDate) {
                circlePaint.color = 0xFF5DCAA5.toInt()
                val radius = cellH * 0.3f
                canvas.drawCircle(cx, cy, radius, circlePaint)
                textPaint.color = 0xFF1A1A2E.toInt()
            } else if (isHoliday || col == 0) {
                textPaint.color = 0xFFE05050.toInt()
            } else if (col == 6) {
                textPaint.color = 0xFF5599CC.toInt()
            } else {
                textPaint.color = 0xFFDDDDDD.toInt()
            }

            canvas.drawText(day.toString(), cx, cy + textPaint.textSize * 0.35f, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val x = event.x
            val y = event.y
            if (leftArrowRect.contains(x, y)) {
                prevMonth()
                return true
            }
            if (rightArrowRect.contains(x, y)) {
                nextMonth()
                return true
            }
        }
        return true
    }

    private fun prevMonth() {
        if (displayMonth == 0) { displayMonth = 11; displayYear-- }
        else displayMonth--
        requestLayout()
        invalidate()
    }

    private fun nextMonth() {
        if (displayMonth == 11) { displayMonth = 0; displayYear++ }
        else displayMonth++
        requestLayout()
        invalidate()
    }

    private fun getDaysInMonth(year: Int, month: Int): Int {
        val c = Calendar.getInstance()
        c.set(year, month, 1)
        return c.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private fun getFirstDayOfWeek(year: Int, month: Int): Int {
        val c = Calendar.getInstance()
        c.set(year, month, 1)
        return c.get(Calendar.DAY_OF_WEEK) - 1
    }
}
