package com.example.timed_mobile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ExcuseLetterAdapter(private val items: List<ExcuseLetterModel>) :
    RecyclerView.Adapter<ExcuseLetterAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.text_date)
        val reason: TextView = view.findViewById(R.id.text_reason)
        val status: TextView = view.findViewById(R.id.text_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_excuse_letter, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.date.text = item.date
        holder.reason.text = item.reason
        holder.status.text = item.status
    }

    override fun getItemCount() = items.size
}