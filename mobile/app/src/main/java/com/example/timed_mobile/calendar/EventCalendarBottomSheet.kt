package com.example.timed_mobile.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.timed_mobile.R
import com.example.timed_mobile.adapter.CalendarEventAdapter
import com.example.timed_mobile.model.CalendarEvent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class EventCalendarBottomSheet : BottomSheetDialogFragment() {

    private lateinit var monthYearText: TextView
    private lateinit var dayGrid: GridLayout
    private lateinit var headersGrid: GridLayout
    private lateinit var recycler: RecyclerView
    private lateinit var selectedDateText: TextView
    private lateinit var adapter: CalendarEventAdapter

    private val calendar: Calendar = Calendar.getInstance()
    private val dayFormat = SimpleDateFormat("d", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val keyFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

    private var selectedDayKey: String? = null
    private val allEvents = mutableListOf<CalendarEvent>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_event_calendar_bottom_sheet, container, false)
        monthYearText = view.findViewById(R.id.text_month_year)
        dayGrid = view.findViewById(R.id.calendar_day_grid)
        headersGrid = view.findViewById(R.id.calendar_day_headers)
        recycler = view.findViewById(R.id.recycler_calendar_events)
        selectedDateText = view.findViewById(R.id.text_selected_date)

        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = CalendarEventAdapter(emptyList())
        recycler.adapter = adapter

        view.findViewById<ImageButton>(R.id.btn_prev_month).setOnClickListener { changeMonth(-1) }
        view.findViewById<ImageButton>(R.id.btn_next_month).setOnClickListener { changeMonth(1) }
        view.findViewById<Button>(R.id.btn_close_calendar).setOnClickListener { dismiss() }

        buildHeaders()
        generateMockEventsForMonth()
        renderCalendar()
        return view
    }

    private fun buildHeaders() {
        headersGrid.removeAllViews()
        val days = listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")
        val size = resources.getDimensionPixelSize(R.dimen.calendar_cell_size)
        val colGap = resources.getDimensionPixelSize(R.dimen.calendar_cell_col_spacing)
        val rowGap = resources.getDimensionPixelSize(R.dimen.calendar_cell_row_spacing)
        days.forEachIndexed { index, d ->
            val tv = TextView(requireContext())
            tv.text = d
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
            tv.textSize = 12f
            tv.gravity = android.view.Gravity.CENTER
            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = size
            // Only add left spacing if not first column
            val left = if (index == 0) 0 else colGap
            params.setMargins(left, 0, 0, rowGap/2)
            params.columnSpec = GridLayout.spec(index,1f)
            tv.layoutParams = params
            headersGrid.addView(tv)
        }
    }

    private fun generateMockEventsForMonth() {
        allEvents.clear()
        val baseCal = calendar.clone() as Calendar
        baseCal.set(Calendar.DAY_OF_MONTH, 1)
        val daysInMonth = baseCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val statuses = listOf("upcoming","ongoing","ended","cancelled")
        // Generate between 6-12 mock events
        val eventCount = Random.nextInt(6, 13)
        for (i in 0 until eventCount) {
            val day = Random.nextInt(1, daysInMonth + 1)
            val eventCal = baseCal.clone() as Calendar
            eventCal.set(Calendar.DAY_OF_MONTH, day)
            val hour = Random.nextInt(8, 18)
            eventCal.set(Calendar.HOUR_OF_DAY, hour)
            eventCal.set(Calendar.MINUTE, listOf(0,15,30,45).random())
            val status = statuses.random()
            val timeLabel = SimpleDateFormat("h:mm a", Locale.getDefault()).format(eventCal.time)
            allEvents.add(CalendarEvent(title = "Mock Event ${i+1}", date = eventCal.time, status = status, timeLabel = timeLabel))
        }
    }

    private fun changeMonth(offset: Int) {
        calendar.add(Calendar.MONTH, offset)
        selectedDayKey = null
        generateMockEventsForMonth()
        renderCalendar()
    }

    private fun renderCalendar() {
        monthYearText.text = monthYearFormat.format(calendar.time)
        dayGrid.removeAllViews()

        val tempCal = calendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeekIndex = tempCal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sunday
        val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val totalCells = firstDayOfWeekIndex + daysInMonth
        val rows = ((totalCells + 6) / 7)
        dayGrid.rowCount = rows

        val size = resources.getDimensionPixelSize(R.dimen.calendar_cell_size)
        val colGap = resources.getDimensionPixelSize(R.dimen.calendar_cell_col_spacing)
        val rowGap = resources.getDimensionPixelSize(R.dimen.calendar_cell_row_spacing)
        for (i in 0 until firstDayOfWeekIndex) {
            val placeholder = View(requireContext())
            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = size
            val left = if (i == 0) 0 else colGap
            params.setMargins(left, rowGap/2, 0, rowGap/2)
            params.columnSpec = GridLayout.spec(i,1f)
            placeholder.layoutParams = params
            dayGrid.addView(placeholder)
        }

        for (day in 1..daysInMonth) {
            val container = LinearLayout(requireContext())
            container.orientation = LinearLayout.VERTICAL
            container.gravity = android.view.Gravity.CENTER
            val numberView = TextView(requireContext())
            numberView.text = day.toString()
            numberView.textSize = 14f
            numberView.gravity = android.view.Gravity.CENTER

            val statusDot = View(requireContext())
            val dotParams = LinearLayout.LayoutParams(12,12)
            dotParams.topMargin = 2
            statusDot.layoutParams = dotParams

            val cellCal = tempCal.clone() as Calendar
            cellCal.set(Calendar.DAY_OF_MONTH, day)
            val dayKey = keyFormat.format(cellCal.time)
            val hasEvent = allEvents.any { keyFormat.format(it.date) == dayKey }
            val baseTextColor = if (hasEvent) R.color.primary_deep_blue else R.color.medium_gray
            numberView.setTextColor(ContextCompat.getColor(requireContext(), baseTextColor))

            // Static demo attendance status rule
            val attendanceStatus = when {
                day % 7 == 0 -> "absent"
                day % 5 == 0 -> "late"
                else -> "present"
            }
            when (attendanceStatus) {
                "present" -> statusDot.background = ContextCompat.getDrawable(requireContext(), R.drawable.dot_attendance_present)
                "late" -> statusDot.background = ContextCompat.getDrawable(requireContext(), R.drawable.dot_attendance_late)
                "absent" -> statusDot.background = ContextCompat.getDrawable(requireContext(), R.drawable.dot_attendance_absent)
            }

            container.addView(numberView)
            container.addView(statusDot)

            styleDayContainer(container, numberView, selected = (dayKey == selectedDayKey))

            container.setOnClickListener { onDaySelectedDayContainer(dayKey, cellCal.time, container) }

            val absoluteIndex = firstDayOfWeekIndex + (day - 1)
            val column = absoluteIndex % 7
            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = size
            val left = if (column == 0) 0 else colGap
            params.setMargins(left, rowGap/2, 0, rowGap/2)
            params.columnSpec = GridLayout.spec(column,1f)
            container.layoutParams = params
            dayGrid.addView(container)
        }

        // Clear list if selection cleared
        if (selectedDayKey == null) {
            adapter.submit(emptyList())
            selectedDateText.text = "Select a date to view events"
        } else {
            filterForSelected()
        }
    }

    private fun styleDayContainer(container: LinearLayout, numberView: TextView, selected: Boolean) {
        if (selected) {
            container.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_calendar_day_selected)
            numberView.setTypeface(numberView.typeface, android.graphics.Typeface.BOLD)
        } else {
            container.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_calendar_day_unselected)
            numberView.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
    }

    private fun onDaySelectedDayContainer(dayKey: String, date: Date, container: LinearLayout) {
        selectedDayKey = if (selectedDayKey == dayKey) null else dayKey
        for (i in 0 until dayGrid.childCount) {
            val child = dayGrid.getChildAt(i)
            if (child is LinearLayout && child.childCount > 0 && child.getChildAt(0) is TextView) {
                val numberView = child.getChildAt(0) as TextView
                val dayNum = numberView.text.toString().toIntOrNull() ?: continue
                val cal = calendar.clone() as Calendar
                cal.set(Calendar.DAY_OF_MONTH, dayNum)
                val key = keyFormat.format(cal.time)
                styleDayContainer(child, numberView, key == selectedDayKey)
            }
        }
        filterForSelected()
    }

    private fun filterForSelected() {
        if (selectedDayKey == null) {
            adapter.submit(emptyList())
            selectedDateText.text = "Select a date to view events"
            return
        }
        val filtered = allEvents.filter { keyFormat.format(it.date) == selectedDayKey }
        adapter.submit(filtered.sortedBy { it.date })
        val parsed = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).parse(selectedDayKey!!)
        selectedDateText.text = if (filtered.isEmpty()) {
            "No events for the selected date"
        } else {
            SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(parsed!!)
        }
    }
}