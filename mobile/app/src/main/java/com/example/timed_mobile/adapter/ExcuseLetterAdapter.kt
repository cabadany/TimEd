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
        holder.dateText.text = item.date
        holder.reasonText.text = item.reason
        holder.statusText.text = item.status
    }
}