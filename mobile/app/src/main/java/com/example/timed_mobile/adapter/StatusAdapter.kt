package com.example.timed_mobile.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.timed_mobile.R

class StatusAdapter(context: Context, private val statuses: List<String>) :
    ArrayAdapter<String>(context, R.layout.spinner_status_item, statuses) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, convertView, parent)
    }

    private fun getCustomView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.spinner_status_item, parent, false)
        val textView = view.findViewById<TextView>(R.id.status_item)
        val status = statuses[position]

        textView.text = status

        // Set background drawable depending on status
        when (status) {
            "On Duty" -> textView.setBackgroundResource(R.drawable.status_on_duty)
            "On Break" -> textView.setBackgroundResource(R.drawable.status_on_break)
            "Off Duty" -> textView.setBackgroundResource(R.drawable.status_off_duty)
        }

        return view
    }
}