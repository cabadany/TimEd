package com.example.timed_mobile.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.timed_mobile.R

class StatusAdapter(context: Context, private val statuses: List<String>) :
    ArrayAdapter<String>(context, R.layout.status_spinner, statuses) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = LayoutInflater.from(context).inflate(R.layout.status_spinner, parent, false)
        val label = view.findViewById<TextView>(R.id.spinner_text)
        val status = getItem(position)

        label.text = status

        when (status) {
            "On Duty" -> {
                label.setBackgroundResource(R.drawable.bg_spinner_on_duty)
                label.setTextColor(ContextCompat.getColor(context, R.color.status_on_duty_text))
            }
            "On Break" -> {
                label.setBackgroundResource(R.drawable.bg_spinner_on_break)
                label.setTextColor(ContextCompat.getColor(context, R.color.status_on_break_text))
            }
            "Off Duty" -> {
                label.setBackgroundResource(R.drawable.bg_spinner_off_duty)
                label.setTextColor(ContextCompat.getColor(context, R.color.status_off_duty_text))
            }
        }

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = LayoutInflater.from(context).inflate(R.layout.item_status_spinner, parent, false)
        val textView = row.findViewById<TextView>(R.id.spinner_item_text)
        val status = getItem(position)

        textView.text = status

        when (status) {
            "On Duty" -> {
                textView.setBackgroundResource(R.drawable.bg_spinner_on_duty)
                textView.setTextColor(ContextCompat.getColor(context, R.color.status_on_duty_text))
            }
            "On Break" -> {
                textView.setBackgroundResource(R.drawable.bg_spinner_on_break)
                textView.setTextColor(ContextCompat.getColor(context, R.color.status_on_break_text))
            }
            "Off Duty" -> {
                textView.setBackgroundResource(R.drawable.bg_spinner_off_duty)
                textView.setTextColor(ContextCompat.getColor(context, R.color.status_off_duty_text))
            }
        }
        return row
    }
}