package com.example.timed_mobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.timed_mobile.R
import com.example.timed_mobile.model.ExcuseLetterModel

class ExcuseLetterAdapter(private val list: List<ExcuseLetterModel>) :
    RecyclerView.Adapter<ExcuseLetterAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.item_date)
        val reasonText: TextView = itemView.findViewById(R.id.item_reason)
        val statusText: TextView = itemView.findViewById(R.id.item_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_excuse_letter, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        
        // Date formatting could be improved here if 'item.date' is raw timestamp. 
        // Assuming item.date is already formatted string based on existing code, or we just display it.
        // If it needs formatting, we would need to know the input format. 
        // For now, prepending label if needed or just showing it. 
        // Layout has "Date:" prefix hardcoded? No, layout has tools:text.
        holder.dateText.text = "Submitted: ${item.date}"

        holder.reasonText.text = item.reason

        holder.statusText.text = item.status
        
        // Status Color Logic
        val context = holder.itemView.context
        val background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_rounded_status)?.mutate()
        
        val color = when (item.status?.lowercase()) {
            "approved" -> androidx.core.content.ContextCompat.getColor(context, R.color.status_green)
            "rejected", "declined" -> androidx.core.content.ContextCompat.getColor(context, R.color.status_red)
            "pending" -> androidx.core.content.ContextCompat.getColor(context, R.color.status_yellow)
            else -> androidx.core.content.ContextCompat.getColor(context, R.color.neutral_text_gray)
        }
        
        background?.setTint(color)
        holder.statusText.background = background
    }
}