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
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EventCalendarBottomSheet : BottomSheetDialogFragment() {

    var onDismissed: (() -> Unit)? = null

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
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
        timeZone = java.util.TimeZone.getTimeZone("Asia/Manila")
    }

    private var selectedDayKey: String? = null
    private val allEvents = mutableListOf<CalendarEvent>()
    private var departmentId: String? = null

    companion object {
        private const val ARG_DEPARTMENT_ID = "arg_department_id"
        private const val API_BASE_URL = "https://timed-utd9.onrender.com/api"
        fun newInstance(departmentId: String): EventCalendarBottomSheet {
            val sheet = EventCalendarBottomSheet()
            val args = Bundle()
            args.putString(ARG_DEPARTMENT_ID, departmentId)
            sheet.arguments = args
            return sheet
        }
    }

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
        departmentId = arguments?.getString(ARG_DEPARTMENT_ID)
        fetchEventsForCurrentMonth()
        renderCalendar()
        return view
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        onDismissed?.invoke()
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

    private fun fetchEventsForCurrentMonth() {
        allEvents.clear()
        val deptId = departmentId ?: return
        val monthStart = (calendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        val monthEnd = (calendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time

        val startParam = apiDateFormat.format(monthStart)
        val endParam = apiDateFormat.format(monthEnd)
        val url = "$API_BASE_URL/events/getByDateRange?startDate=$startParam&endDate=$endParam"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                activity?.runOnUiThread { renderCalendar() }
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!it.isSuccessful) {
                        activity?.runOnUiThread { renderCalendar() }
                        return
                    }
                    val body = it.body?.string() ?: "[]"
                    try {
                        val gson = Gson()
                        val listType = com.google.gson.reflect.TypeToken.getParameterized(List::class.java, java.util.Map::class.java).type
                        val events = gson.fromJson<List<Map<String, Any?>>>(body, listType)
                        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                        val now = System.currentTimeMillis()
                        val filtered = events.filter { evt -> (evt["departmentId"] as? String) == deptId }
                        for (evt in filtered) {
                            val title = evt["eventName"] as? String ?: continue
                            val dateStr = evt["date"] as? String ?: continue
                            val duration = (evt["duration"] as? String) ?: "01:00:00"
                            val statusFromDb = (evt["status"] as? String) ?: "upcoming"

                            val parsedDate = apiDateFormat.parse(dateStr) ?: continue
                            val durationParts = duration.split(":")
                            val durationMillis = when (durationParts.size) {
                                3 -> (durationParts[0].toLongOrNull() ?: 0)*3600000L + (durationParts[1].toLongOrNull() ?: 0)*60000L + (durationParts[2].toLongOrNull() ?: 0)*1000L
                                2 -> (durationParts[0].toLongOrNull() ?: 0)*3600000L + (durationParts[1].toLongOrNull() ?: 0)*60000L
                                1 -> (durationParts[0].toLongOrNull() ?: 0)*60000L
                                else -> 3600000L
                            }
                            val startMs = parsedDate.time
                            val endMs = startMs + durationMillis
                            val status = when {
                                statusFromDb.equals("cancelled", true) -> "cancelled"
                                statusFromDb.equals("ongoing", true) -> "ongoing"
                                statusFromDb.equals("ended", true) -> "ended"
                                now < startMs -> "upcoming"
                                now in startMs..endMs -> "ongoing"
                                else -> "ended"
                            }
                            allEvents.add(
                                CalendarEvent(
                                    title = title,
                                    date = parsedDate,
                                    status = status,
                                    timeLabel = timeFormat.format(parsedDate)
                                )
                            )
                        }
                    } catch (_: Exception) { }
                    activity?.runOnUiThread {
                        renderCalendar()
                        if (selectedDayKey != null) filterForSelected()
                    }
                }
            }
        })
    }

    private fun changeMonth(offset: Int) {
        calendar.add(Calendar.MONTH, offset)
        selectedDayKey = null
        fetchEventsForCurrentMonth()
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

            // Color status dot based on real event status (maps to existing styles)
            val dayEvents = allEvents.filter { keyFormat.format(it.date) == dayKey }
            val statusForDot = when {
                dayEvents.any { it.status.equals("ongoing", true) } -> "late" // orange
                dayEvents.any { it.status.equals("upcoming", true) } -> "present" // green
                dayEvents.any { it.status.equals("cancelled", true) || it.status.equals("ended", true) } -> "absent" // red
                else -> null
            }
            statusForDot?.let {
                when (it) {
                    "present" -> statusDot.background = ContextCompat.getDrawable(requireContext(), R.drawable.dot_attendance_present)
                    "late" -> statusDot.background = ContextCompat.getDrawable(requireContext(), R.drawable.dot_attendance_late)
                    "absent" -> statusDot.background = ContextCompat.getDrawable(requireContext(), R.drawable.dot_attendance_absent)
                }
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