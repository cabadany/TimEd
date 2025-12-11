package com.example.timed_mobile.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.timed_mobile.R
import com.example.timed_mobile.model.CalendarEvent

class CalendarEventAdapter(private var items: List<CalendarEvent>) : RecyclerView.Adapter<CalendarEventAdapter.EventVH>() {

    inner class EventVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.text_event_title)
        val time: TextView = itemView.findViewById(R.id.text_event_time)
        val status: TextView = itemView.findViewById(R.id.text_event_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_event, parent, false)
        return EventVH(v)
    }

    override fun onBindViewHolder(holder: EventVH, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        
        holder.title.text = item.title
        holder.time.text = item.timeLabel
        
        val statusText = item.status.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        holder.status.text = statusText

        val (bgColorRes, textColorRes) = when (item.status.lowercase()) {
            "ongoing", "present" -> Pair(R.color.attendance_green, R.color.white)
            "upcoming", "late" -> Pair(R.color.attendance_yellow, R.color.black)
            "ended", "cancelled", "absent" -> Pair(R.color.attendance_red, R.color.white)
            else -> Pair(R.color.medium_gray, R.color.white)
        }

        holder.status.background = ContextCompat.getDrawable(context, R.drawable.bg_status_pill)
        holder.status.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, bgColorRes))
        holder.status.setTextColor(ContextCompat.getColor(context, textColorRes))
    }

    override fun getItemCount(): Int = items.size

    fun submit(newItems: List<CalendarEvent>) {
        items = newItems
        notifyDataSetChanged()
    }
}