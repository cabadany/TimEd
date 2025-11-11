package com.example.timed_mobile.calendar

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.NumberPicker
import androidx.core.content.ContextCompat
import androidx.annotation.StringRes
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.timed_mobile.R
import com.example.timed_mobile.adapter.CalendarEventAdapter
import com.example.timed_mobile.model.CalendarEvent
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.gson.Gson
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.Legend.LegendOrientation
import com.github.mikephil.charting.components.Legend.LegendVerticalAlignment
import com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment
import com.github.mikephil.charting.components.Legend.LegendForm
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EventCalendarBottomSheet : BottomSheetDialogFragment() {

    var onDismissed: (() -> Unit)? = null

    private lateinit var monthYearText: TextView
    private lateinit var yearButton: TextView
    private lateinit var dayGrid: GridLayout
    private lateinit var headersGrid: GridLayout
    private lateinit var recycler: RecyclerView
    private lateinit var selectedDateText: TextView
    private lateinit var adapter: CalendarEventAdapter
    private lateinit var calendarTitle: TextView
    private lateinit var toggleGroup: MaterialButtonToggleGroup
    private lateinit var eventLayout: LinearLayout
    private lateinit var attendanceLayout: LinearLayout
    private lateinit var attendanceHeaders: GridLayout
    private lateinit var attendanceGrid: GridLayout
    private lateinit var monthlyPieChart: PieChart
    private lateinit var monthlyPresentCount: TextView
    private lateinit var monthlyLateCount: TextView
    private lateinit var monthlyAbsentCount: TextView

    private val calendar: Calendar = Calendar.getInstance()
    private val dayFormat = SimpleDateFormat("d", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
    private val keyFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
        timeZone = java.util.TimeZone.getTimeZone("Asia/Manila")
    }
    private val switchInterpolator = FastOutSlowInInterpolator()
    private val switchDuration = 220L

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
        val view = inflater.inflate(R.layout.event_calendar_bottom_sheet, container, false)
        calendarTitle = view.findViewById(R.id.text_calendar_title)
        monthYearText = view.findViewById(R.id.text_month_year)
        yearButton = view.findViewById<TextView>(R.id.btn_select_year)
        dayGrid = view.findViewById(R.id.calendar_day_grid)
        headersGrid = view.findViewById(R.id.calendar_day_headers)
        recycler = view.findViewById(R.id.recycler_calendar_events)
        selectedDateText = view.findViewById(R.id.text_selected_date)
        toggleGroup = view.findViewById(R.id.calendar_toggle_group)
        eventLayout = view.findViewById(R.id.layout_event_calendar)
        attendanceLayout = view.findViewById(R.id.layout_attendance_calendar)
        attendanceHeaders = view.findViewById(R.id.attendance_day_headers)
        attendanceGrid = view.findViewById(R.id.attendance_day_grid)
        monthlyPieChart = view.findViewById(R.id.chart_attendance_monthly)
        monthlyPresentCount = view.findViewById(R.id.text_attendance_present_count)
        monthlyLateCount = view.findViewById(R.id.text_attendance_late_count)
        monthlyAbsentCount = view.findViewById(R.id.text_attendance_absent_count)

        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = CalendarEventAdapter(emptyList())
        recycler.adapter = adapter

        view.findViewById<ImageButton>(R.id.btn_prev_month).setOnClickListener { changeMonth(-1) }
        view.findViewById<ImageButton>(R.id.btn_next_month).setOnClickListener { changeMonth(1) }
        yearButton.setOnClickListener { showYearPicker() }

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btn_toggle_events -> {
                    if (eventLayout.visibility != View.VISIBLE) {
                        animateCalendarSwitch(eventLayout, attendanceLayout, R.string.event_calendar_title)
                    }
                }
                R.id.btn_toggle_attendance -> {
                    renderAttendanceCalendarPlaceholder()
                    if (attendanceLayout.visibility != View.VISIBLE) {
                        animateCalendarSwitch(attendanceLayout, eventLayout, R.string.attendance_calendar_title)
                    }
                }
            }
        }
        toggleGroup.check(R.id.btn_toggle_events)

        buildHeaders()
        buildAttendanceHeaders()
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
            tv.textSize = 13f
            tv.setTypeface(null, android.graphics.Typeface.BOLD)
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
        monthYearText.text = monthFormat.format(calendar.time)
        yearButton.text = "${calendar.get(Calendar.YEAR)} ▼"
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
            numberView.textSize = 16f
            numberView.setTypeface(null, android.graphics.Typeface.NORMAL)
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
                dayEvents.any { it.status.equals("ongoing", true) } -> "present" // green
                dayEvents.any { it.status.equals("upcoming", true) } -> "late" // yellow
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
            selectedDateText.text = getString(R.string.calendar_select_date_prompt)
        } else {
            filterForSelected()
        }
        renderAttendanceCalendarPlaceholder()
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
            selectedDateText.text = getString(R.string.calendar_select_date_prompt)
            return
        }
        val filtered = allEvents.filter { keyFormat.format(it.date) == selectedDayKey }
        adapter.submit(filtered.sortedBy { it.date })
        val parsed = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).parse(selectedDayKey!!)
        selectedDateText.text = if (filtered.isEmpty()) {
            getString(R.string.calendar_no_events_for_selected)
        } else {
            SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(parsed!!)
        }
    }

    private fun buildAttendanceHeaders() {
        attendanceHeaders.removeAllViews()
        val days = listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")
        val size = resources.getDimensionPixelSize(R.dimen.calendar_cell_size)
        val colGap = resources.getDimensionPixelSize(R.dimen.calendar_cell_col_spacing)
        val rowGap = resources.getDimensionPixelSize(R.dimen.calendar_cell_row_spacing)
        days.forEachIndexed { index, d ->
            val tv = TextView(requireContext())
            tv.text = d
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
            tv.textSize = 13f
            tv.setTypeface(null, android.graphics.Typeface.BOLD)
            tv.gravity = android.view.Gravity.CENTER
            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = size
            val left = if (index == 0) 0 else colGap
            params.setMargins(left, 0, 0, rowGap/2)
            params.columnSpec = GridLayout.spec(index,1f)
            tv.layoutParams = params
            attendanceHeaders.addView(tv)
        }
    }

    private fun renderAttendanceCalendarPlaceholder() {
        attendanceGrid.removeAllViews()
        val tempCal = calendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeekIndex = tempCal.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val totalCells = firstDayOfWeekIndex + daysInMonth
        val rows = ((totalCells + 6) / 7)
        attendanceGrid.rowCount = rows

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
            attendanceGrid.addView(placeholder)
        }

        // Get current user ID from SharedPreferences
        val prefs = requireContext().getSharedPreferences("TimedAppPrefs", android.content.Context.MODE_PRIVATE)
        val userId = prefs.getString("userId", null)
        
        if (userId == null) {
            // If no user ID, render empty calendar
            renderEmptyAttendanceCalendar(daysInMonth, firstDayOfWeekIndex, size, colGap, rowGap)
            renderEmptyAttendanceStatistics()
            return
        }

        // Fetch attendance data from backend
        fetchAttendanceDataForMonth(userId, daysInMonth, firstDayOfWeekIndex, size, colGap, rowGap)
    }

    private fun fetchAttendanceDataForMonth(userId: String, daysInMonth: Int, firstDayOfWeekIndex: Int, size: Int, colGap: Int, rowGap: Int) {
        val url = "$API_BASE_URL/attendance/user/$userId/attended-events"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                activity?.runOnUiThread {
                    renderEmptyAttendanceCalendar(daysInMonth, firstDayOfWeekIndex, size, colGap, rowGap)
                    renderEmptyAttendanceStatistics()
                }
            }
            
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use {
                    if (!it.isSuccessful) {
                        activity?.runOnUiThread {
                            renderEmptyAttendanceCalendar(daysInMonth, firstDayOfWeekIndex, size, colGap, rowGap)
                            renderEmptyAttendanceStatistics()
                        }
                        return
                    }
                    
                    val body = it.body?.string() ?: "[]"
                    try {
                        val gson = Gson()
                        val listType = com.google.gson.reflect.TypeToken.getParameterized(List::class.java, java.util.Map::class.java).type
                        val attendedEvents = gson.fromJson<List<Map<String, Any?>>>(body, listType)
                        
                        // Filter events for current month
                        val monthStart = (calendar.clone() as Calendar).apply {
                            set(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        
                        val monthEnd = (calendar.clone() as Calendar).apply {
                            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }.timeInMillis
                        
                        // Process attended events and create day-to-status mapping
                        val dayStatusMap = mutableMapOf<Int, String>() // day -> status (present/late/absent)
                        var presentCount = 0
                        var lateCount = 0
                        var absentCount = 0
                        
                        // Get all events in the month from allEvents (already fetched)
                        val monthEvents = allEvents.filter { event ->
                            val eventTime = event.date.time
                            eventTime >= monthStart && eventTime <= monthEnd
                        }
                        
                        // Create a set of event dates that user attended
                        val attendedEventDates = mutableSetOf<String>()
                        for (attendedEvent in attendedEvents) {
                            val eventDate = attendedEvent["eventDate"] as? String
                            if (eventDate != null) {
                                try {
                                    val parsedDate = apiDateFormat.parse(eventDate)
                                    if (parsedDate != null) {
                                        val dayKey = keyFormat.format(parsedDate)
                                        attendedEventDates.add(dayKey)
                                    }
                                } catch (e: Exception) { }
                            }
                        }
                        
                        // Determine status for each day
                        for (event in monthEvents) {
                            val dayKey = keyFormat.format(event.date)
                            val dayOfMonth = SimpleDateFormat("d", Locale.getDefault()).format(event.date).toIntOrNull() ?: continue
                            
                            if (attendedEventDates.contains(dayKey)) {
                                // User attended - check if on time or late
                                val eventStartTime = event.date.time
                                val eventDateOnly = (calendar.clone() as Calendar).apply {
                                    time = event.date
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                                
                                // Find the attended event to get time-in timestamp
                                val attendedEvent = attendedEvents.find { evt ->
                                    val evtDate = evt["eventDate"] as? String
                                    evtDate != null && keyFormat.format(apiDateFormat.parse(evtDate) ?: Date()) == dayKey
                                }
                                
                                if (attendedEvent != null) {
                                    // Check if user was late (arrived after event start)
                                    // For simplicity, mark as present if attended, late if event status suggests it
                                    val status = if (event.status == "ongoing" || event.status == "ended") "present" else "late"
                                    dayStatusMap[dayOfMonth] = status
                                    if (status == "present") presentCount++ else lateCount++
                                } else {
                                    dayStatusMap[dayOfMonth] = "present"
                                    presentCount++
                                }
                            } else {
                                // Event exists but user didn't attend
                                if (dayStatusMap[dayOfMonth] == null) {
                                    dayStatusMap[dayOfMonth] = "absent"
                                    absentCount++
                                }
                            }
                        }
                        
                        // Count days with events but no attendance as absent
                        for (event in monthEvents) {
                            val dayOfMonth = SimpleDateFormat("d", Locale.getDefault()).format(event.date).toIntOrNull() ?: continue
                            val dayKey = keyFormat.format(event.date)
                            if (!attendedEventDates.contains(dayKey) && dayStatusMap[dayOfMonth] == null) {
                                dayStatusMap[dayOfMonth] = "absent"
                                absentCount++
                            }
                        }
                        
                        activity?.runOnUiThread {
                            renderAttendanceCalendarWithData(daysInMonth, firstDayOfWeekIndex, size, colGap, rowGap, dayStatusMap)
                            renderAttendanceStatistics(presentCount, lateCount, absentCount)
                        }
                    } catch (e: Exception) {
                        activity?.runOnUiThread {
                            renderEmptyAttendanceCalendar(daysInMonth, firstDayOfWeekIndex, size, colGap, rowGap)
                            renderEmptyAttendanceStatistics()
                        }
                    }
                }
            }
        })
    }

    private fun renderEmptyAttendanceCalendar(daysInMonth: Int, firstDayOfWeekIndex: Int, size: Int, colGap: Int, rowGap: Int) {
        val dayFormatLocal = SimpleDateFormat("d", Locale.getDefault())
        val tempCal = calendar.clone() as Calendar
        
        for (day in 1..daysInMonth) {
            val container = LinearLayout(requireContext())
            container.orientation = LinearLayout.VERTICAL
            container.gravity = android.view.Gravity.CENTER

            val numberView = TextView(requireContext())
            numberView.text = dayFormatLocal.format(tempCal.apply { set(Calendar.DAY_OF_MONTH, day) }.time)
            numberView.textSize = 16f
            numberView.setTypeface(null, android.graphics.Typeface.NORMAL)
            numberView.gravity = android.view.Gravity.CENTER
            numberView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_deep_blue))

            val statusDot = View(requireContext())
            val dotParams = LinearLayout.LayoutParams(12,12)
            dotParams.topMargin = 2
            statusDot.layoutParams = dotParams
            statusDot.visibility = View.INVISIBLE

            container.addView(numberView)
            container.addView(statusDot)

            val absoluteIndex = firstDayOfWeekIndex + (day - 1)
            val column = absoluteIndex % 7
            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = size
            val left = if (column == 0) 0 else colGap
            params.setMargins(left, rowGap/2, 0, rowGap/2)
            params.columnSpec = GridLayout.spec(column,1f)
            container.layoutParams = params
            container.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_calendar_day_unselected)
            attendanceGrid.addView(container)
        }
    }

    private fun renderAttendanceCalendarWithData(daysInMonth: Int, firstDayOfWeekIndex: Int, size: Int, colGap: Int, rowGap: Int, dayStatusMap: Map<Int, String>) {
        val dayFormatLocal = SimpleDateFormat("d", Locale.getDefault())
        val tempCal = calendar.clone() as Calendar
        
        for (day in 1..daysInMonth) {
            val container = LinearLayout(requireContext())
            container.orientation = LinearLayout.VERTICAL
            container.gravity = android.view.Gravity.CENTER

            val numberView = TextView(requireContext())
            numberView.text = dayFormatLocal.format(tempCal.apply { set(Calendar.DAY_OF_MONTH, day) }.time)
            numberView.textSize = 16f
            numberView.setTypeface(null, android.graphics.Typeface.NORMAL)
            numberView.gravity = android.view.Gravity.CENTER
            numberView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_deep_blue))

            val statusDot = View(requireContext())
            val dotParams = LinearLayout.LayoutParams(12,12)
            dotParams.topMargin = 2
            statusDot.layoutParams = dotParams

            val status = dayStatusMap[day]
            val statusDrawable = when (status) {
                "present" -> R.drawable.dot_attendance_present
                "late" -> R.drawable.dot_attendance_late
                "absent" -> R.drawable.dot_attendance_absent
                else -> null
            }
            
            statusDrawable?.let {
                statusDot.background = ContextCompat.getDrawable(requireContext(), it)
            } ?: run {
                statusDot.visibility = View.INVISIBLE
            }

            container.addView(numberView)
            container.addView(statusDot)

            val absoluteIndex = firstDayOfWeekIndex + (day - 1)
            val column = absoluteIndex % 7
            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = size
            val left = if (column == 0) 0 else colGap
            params.setMargins(left, rowGap/2, 0, rowGap/2)
            params.columnSpec = GridLayout.spec(column,1f)
            container.layoutParams = params
            container.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_calendar_day_unselected)
            attendanceGrid.addView(container)
        }
    }

    private fun renderEmptyAttendanceStatistics() {
        renderAttendanceStatistics(0, 0, 0)
    }

    private fun renderAttendanceStatistics(presentCount: Int, lateCount: Int, absentCount: Int) {
        val total = presentCount + lateCount + absentCount

        monthlyPresentCount.text = getString(R.string.attendance_present_count_format, presentCount)
        monthlyLateCount.text = getString(R.string.attendance_late_count_format, lateCount)
        monthlyAbsentCount.text = getString(R.string.attendance_absent_count_format, absentCount)

        val entries = listOf(
            PieEntry(presentCount.toFloat(), getString(R.string.attendance_status_on_time)),
            PieEntry(lateCount.toFloat(), getString(R.string.attendance_status_late)),
            PieEntry(absentCount.toFloat(), getString(R.string.attendance_status_absent))
        ).filter { it.value > 0f }

        if (entries.isEmpty()) {
            monthlyPieChart.data = null
            monthlyPieChart.invalidate()
            return
        }

        val colors = listOf(
            ContextCompat.getColor(requireContext(), R.color.attendance_green),
            ContextCompat.getColor(requireContext(), R.color.attendance_yellow),
            ContextCompat.getColor(requireContext(), R.color.attendance_red)
        )

        val dataSet = PieDataSet(entries, "").apply {
            setColors(colors)
            sliceSpace = 3f
            valueTextSize = 13f
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.primary_deep_blue)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String = value.toInt().toString()
            }
        }

        val pieData = PieData(dataSet)

        monthlyPieChart.apply {
            data = pieData
            setUsePercentValues(false)
            setDrawEntryLabels(false)
            description.isEnabled = false
            setHoleColor(ContextCompat.getColor(requireContext(), R.color.white))
            setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.primary_deep_blue))
            centerText = getString(R.string.attendance_stats_center_label, total)
            setCenterTextSize(14f)
            legend.apply {
                verticalAlignment = LegendVerticalAlignment.BOTTOM
                horizontalAlignment = LegendHorizontalAlignment.CENTER
                orientation = LegendOrientation.HORIZONTAL
                form = LegendForm.CIRCLE
                textColor = ContextCompat.getColor(requireContext(), R.color.neutral_text_gray)
                textSize = 12f
                isWordWrapEnabled = true
            }
            setTouchEnabled(false)
            alpha = 0f
            animateY(900, Easing.EaseInOutQuad)
            animate().alpha(1f).setDuration(400).start()
            invalidate()
        }
    }

    private fun animateCalendarSwitch(show: View, hide: View, @StringRes titleRes: Int) {
        calendarTitle.text = getString(titleRes)

        val translation = resources.displayMetrics.density * 20f

        show.animate().cancel()
        hide.animate().cancel()

        if (hide.visibility == View.VISIBLE) {
            hide.animate()
                .alpha(0f)
                .translationY(-translation)
                .setDuration(switchDuration)
                .setInterpolator(switchInterpolator)
                .withEndAction {
                    hide.visibility = View.GONE
                    hide.alpha = 1f
                    hide.translationY = 0f
                }
                .start()
        } else {
            hide.visibility = View.GONE
            hide.alpha = 1f
            hide.translationY = 0f
        }

        show.alpha = 0f
        show.translationY = translation
        show.visibility = View.VISIBLE
        show.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(switchDuration)
            .setInterpolator(switchInterpolator)
            .start()
    }

    private fun showYearPicker() {
        val dialogView = layoutInflater.inflate(R.layout.event_calendar_year_dialog, null)
        val numberPicker = dialogView.findViewById<NumberPicker>(R.id.year_number_picker)
        val currentYear = calendar.get(Calendar.YEAR)
        numberPicker.minValue = currentYear - 5
        numberPicker.maxValue = currentYear + 5
        numberPicker.value = currentYear
        numberPicker.wrapSelectorWheel = false

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Confirm") { dialog, _ ->
                val selectedYear = numberPicker.value
                if (selectedYear != calendar.get(Calendar.YEAR)) {
                    calendar.set(Calendar.YEAR, selectedYear)
                    selectedDayKey = null
                    fetchEventsForCurrentMonth()
                }
                renderCalendar()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        
        // Style buttons after dialog is shown
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_indigo))
            isAllCaps = false
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
            setTextColor(ContextCompat.getColor(requireContext(), R.color.medium_gray))
            isAllCaps = false
            textSize = 16f
        }
    }
}