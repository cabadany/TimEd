package com.example.timed_mobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.timed_mobile.model.EventLogModel
import com.example.timed_mobile.R
import com.google.firebase.firestore.FirebaseFirestore

class EventLogAdapter(
    private val onTimeOutClick: (EventLogModel) -> Unit,
    private val onTimeOutConfirmed: () -> Unit
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
    private val methodChip: TextView = itemView.findViewById(R.id.text_method_chip)
        private val timeOutButton: Button = itemView.findViewById(R.id.button_time_out)

        fun bind(log: EventLogModel) {
            eventNameView.text = log.eventName
            timeInView.text = "Time-In: ${log.timeInTimestamp}"
            statusView.text = log.status

            statusView.setTypeface(null, android.graphics.Typeface.BOLD)
            when (log.status) {
                "Timed-In" -> statusView.setTextColor(ContextCompat.getColor(itemView.context, R.color.success_green))
                "Timed-Out" -> statusView.setTextColor(ContextCompat.getColor(itemView.context, R.color.error_red))
                else -> statusView.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.black))
            }

            // Bind check-in method chip
            val isManual = log.checkinMethod
            methodChip.text = if (isManual) "Manual Code" else "QR Scan"
            methodChip.background = ContextCompat.getDrawable(
                itemView.context,
                if (isManual) R.drawable.bg_chip_manual else R.drawable.bg_chip_qr
            )

            if (log.showTimeOutButton) {
                timeOutButton.visibility = View.VISIBLE
                timeOutButton.setOnClickListener {
                    val context = itemView.context

                    AlertDialog.Builder(context)
                        .setTitle("Confirm Time-Out")
                        .setMessage("Are you sure you want to time out from \"${log.eventName}\"?")
                        .setPositiveButton("Yes") { _, _ ->
                            val loadingDialog = AlertDialog.Builder(context)
                                .setView(R.layout.dialog_loading)
                                .setCancelable(false)
                                .create()

                            loadingDialog.show()

                            // ✅ Firestore update logic
                            FirebaseFirestore.getInstance()
                                .collection("events")
                                .document(log.eventId)
                                .collection("attendees")
                                .whereEqualTo("userId", log.userId) // Ensure your EventLogModel includes userId
                                .get()
                                .addOnSuccessListener { snapshot ->
                                    val batch = FirebaseFirestore.getInstance().batch()
                                    // Create timeout timestamp with Philippines timezone
                                    val philippinesTimeZone = java.util.TimeZone.getTimeZone("Asia/Manila")
                                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                    sdf.timeZone = philippinesTimeZone
                                    val timeOutTimestamp = sdf.format(java.util.Date())
                                    
                                    for (doc in snapshot.documents) {
                                        batch.update(doc.reference, mapOf(
                                            "hasTimedOut" to true,
                                            "timeOutTimestamp" to timeOutTimestamp
                                        ))
                                    }
                                    batch.commit().addOnSuccessListener {
                                        loadingDialog.dismiss()
                                        onTimeOutConfirmed()
                                        Toast.makeText(context, "You’ve successfully timed out.", Toast.LENGTH_SHORT).show()
                                    }.addOnFailureListener { e ->
                                        loadingDialog.dismiss()
                                        Toast.makeText(context, "Error updating timeout: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    loadingDialog.dismiss()
                                    Toast.makeText(context, "Error finding attendee: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            } else {
                timeOutButton.visibility = View.GONE
            }
        }
    }
}