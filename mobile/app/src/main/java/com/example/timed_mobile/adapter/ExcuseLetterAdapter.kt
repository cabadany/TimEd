package com.example.timed_mobile.adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.timed_mobile.R
import com.example.timed_mobile.model.ExcuseLetterModel
import java.text.SimpleDateFormat
import java.util.Locale

class ExcuseLetterAdapter(private val list: List<ExcuseLetterModel>) :
    RecyclerView.Adapter<ExcuseLetterAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.item_date)
        val reasonText: TextView = itemView.findViewById(R.id.item_reason)
        val statusText: TextView = itemView.findViewById(R.id.item_status)
        val attachmentDivider: View = itemView.findViewById(R.id.attachment_divider)
        val attachmentSection: LinearLayout = itemView.findViewById(R.id.attachment_section)
        val btnViewAttachment: TextView = itemView.findViewById(R.id.btn_view_attachment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_excuse_letter, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        
        // Format date from "19/12/2025" to "Absence: Dec 19, 2025"
        holder.dateText.text = formatAbsenceDate(item.date)

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
        
        // Attachment Preview Logic
        if (!item.attachmentUrl.isNullOrEmpty()) {
            holder.attachmentDivider.visibility = View.VISIBLE
            holder.attachmentSection.visibility = View.VISIBLE
            
            holder.btnViewAttachment.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse(item.attachmentUrl)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Unable to open attachment", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            holder.attachmentDivider.visibility = View.GONE
            holder.attachmentSection.visibility = View.GONE
        }
    }
    
    /**
     * Formats date from "19/12/2025" to "Absence: Dec 19, 2025"
     * This makes the displayed date more readable and clear
     */
    private fun formatAbsenceDate(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return "Absence: Unknown"
        
        return try {
            // Parse the input format (day/month/year)
            val inputFormat = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            
            val date = inputFormat.parse(dateStr)
            if (date != null) {
                "Absence: ${outputFormat.format(date)}"
            } else {
                "Absence: $dateStr"
            }
        } catch (e: Exception) {
            // If parsing fails, just show the original with label
            "Absence: $dateStr"
        }
    }
}