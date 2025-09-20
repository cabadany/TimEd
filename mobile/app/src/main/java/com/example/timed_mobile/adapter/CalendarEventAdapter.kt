package com.example.timed_mobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
        holder.title.text = item.title
        holder.time.text = item.timeLabel
        holder.status.text = item.status.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    override fun getItemCount(): Int = items.size

    fun submit(newItems: List<CalendarEvent>) {
        items = newItems
        notifyDataSetChanged()
    }
}