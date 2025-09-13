package com.example.timed_mobile

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

class UiDialogs {
    companion object {
        fun showErrorPopup(
            ctx: Context,
            title: String,
            message: String,
            onClose: (() -> Unit)? = null
        ) {
            val dialog = Dialog(ctx)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(true)
            dialog.setContentView(R.layout.error_popup_generic)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val titleView = dialog.findViewById<TextView>(R.id.popup_title)
            val messageView = dialog.findViewById<TextView>(R.id.popup_message)
            val closeBtn = dialog.findViewById<Button>(R.id.popup_close_button)
            val iconView = dialog.findViewById<ImageView>(R.id.error_icon)

            titleView?.text = title
            messageView?.text = message

            closeBtn?.setOnClickListener {
                dialog.dismiss()
                onClose?.invoke()
            }
            dialog.setOnCancelListener { onClose?.invoke() }
            dialog.show()
        }
    }
}
