package com.example.timed_mobile.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.timed_mobile.EventDetailActivity
import com.example.timed_mobile.R
import com.example.timed_mobile.model.EventModel

class EventAdapter(private val eventList: List<EventModel>) :
    RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    inner class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val eventTitle: TextView = view.findViewById(R.id.eventTitle)
        val eventStatus: TextView = view.findViewById(R.id.eventStatus)
        val eventDate: TextView = view.findViewById(R.id.eventDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.event_card, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = eventList[position]
        holder.eventTitle.text = event.title
        holder.eventStatus.text = "🗂 Status: ${event.status}"
        holder.eventDate.text = "📅 ${event.dateFormatted}"

        holder.itemView.setOnClickListener {
            val context = it.context
            val intent = Intent(context, EventDetailActivity::class.java).apply {
                putExtra("eventTitle", event.title)
                putExtra("eventDate", event.dateFormatted)
                putExtra("eventStatus", event.status)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = eventList.size
}