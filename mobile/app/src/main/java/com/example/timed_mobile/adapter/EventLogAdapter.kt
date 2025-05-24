package com.example.timed_mobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.timed_mobile.model.EventLogModel
import com.example.timed_mobile.R

class EventLogAdapter(
    private val onTimeOutClick: (EventLogModel) -> Unit
) : ListAdapter<EventLogModel, EventLogAdapter.EventLogViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<EventLogModel>() {
        override fun areItemsTheSame(oldItem: EventLogModel, newItem: EventLogModel): Boolean {
            return oldItem.eventId == newItem.eventId && oldItem.timeInTimestamp == newItem.timeInTimestamp
        }

        override fun areContentsTheSame(oldItem: EventLogModel, newItem: EventLogModel): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventLogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_event_log, parent, false)
        return EventLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventLogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EventLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val eventNameView: TextView = itemView.findViewById(R.id.text_event_name)
        private val timeInView: TextView = itemView.findViewById(R.id.text_time_in)
        private val statusView: TextView = itemView.findViewById(R.id.text_status)
        private val timeOutButton: Button = itemView.findViewById(R.id.button_time_out)

        fun bind(log: EventLogModel) {
            eventNameView.text = log.eventName
            timeInView.text = "Time-In: ${log.timeInTimestamp}"
            statusView.text = log.status

            if (log.status == "Timed-In: Haven't Timed-Out" && log.showTimeOutButton) {
                timeOutButton.visibility = View.VISIBLE
                timeOutButton.setOnClickListener {
                    onTimeOutClick(log)
                }
            } else {
                timeOutButton.visibility = View.GONE
            }
        }
    }
}