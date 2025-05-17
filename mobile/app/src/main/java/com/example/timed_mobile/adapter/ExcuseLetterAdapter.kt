package com.example.timed_mobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.timed_mobile.R
import com.example.timed_mobile.model.ExcuseLetterModel

class ExcuseLetterAdapter(private val excuses: List<ExcuseLetterModel>) :
    RecyclerView.Adapter<ExcuseLetterAdapter.ExcuseViewHolder>() {

    class ExcuseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.text_date)
        val reason: TextView = view.findViewById(R.id.text_reason)
        val status: TextView = view.findViewById(R.id.text_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExcuseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_excuse_letter, parent, false)
        return ExcuseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExcuseViewHolder, position: Int) {
        val excuse = excuses[position]
        holder.date.text = excuse.date
        holder.reason.text = excuse.reason
        holder.status.text = "Status: ${excuse.status}"
    }

    override fun getItemCount() = excuses.size
}