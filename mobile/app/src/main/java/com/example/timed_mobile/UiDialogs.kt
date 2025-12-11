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

        fun showSuccessPopup(
            ctx: Context,
            title: String,
            message: String,
            onClose: (() -> Unit)? = null
        ) {
            val dialog = Dialog(ctx)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(false)
            dialog.setCanceledOnTouchOutside(false)
            // Reuse an existing success layout with generic title/message/close button
            dialog.setContentView(R.layout.success_popup_time_in)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val titleView = dialog.findViewById<TextView>(R.id.popup_title)
            val messageView = dialog.findViewById<TextView>(R.id.popup_message)
            val closeBtn = dialog.findViewById<Button>(R.id.popup_close_button)

            titleView?.text = title
            messageView?.text = message

            closeBtn?.setOnClickListener { dialog.dismiss() }
            dialog.setOnDismissListener { onClose?.invoke() }
            dialog.show()
        }

        fun showForgotPasswordSuccess(
            ctx: Context,
            email: String,
            onClose: (() -> Unit)? = null
        ) {
            val dialog = Dialog(ctx)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(true)
            dialog.setContentView(R.layout.success_popup_forgot_password)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val titleView = dialog.findViewById<TextView>(R.id.popup_title)
            val messageView = dialog.findViewById<TextView>(R.id.popup_message)
            val closeBtn = dialog.findViewById<Button>(R.id.popup_close_button)

            titleView?.text = ctx.getString(R.string.popup_title_reset_sent)
            // Provide a helpful message that includes the target email
            messageView?.text = ctx.getString(R.string.popup_message_forgot_password_sent, email)

            closeBtn?.setOnClickListener {
                dialog.dismiss()
                onClose?.invoke()
            }
            dialog.setOnCancelListener { onClose?.invoke() }
            dialog.show()
        }

        fun showConfirmationDialog(
            ctx: Context,
            title: String,
            message: String,
            positiveText: String = "Confirm",
            negativeText: String = "Cancel",
            onPositive: () -> Unit,
            onNegative: (() -> Unit)? = null
        ) {
            val dialog = Dialog(ctx)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(true)
            dialog.setContentView(R.layout.dialog_confirmation)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val titleView = dialog.findViewById<TextView>(R.id.dialog_title)
            val messageView = dialog.findViewById<TextView>(R.id.dialog_message)
            val positiveBtn = dialog.findViewById<Button>(R.id.btn_positive)
            val negativeBtn = dialog.findViewById<Button>(R.id.btn_negative)

            titleView?.text = title
            messageView?.text = message
            positiveBtn?.text = positiveText
            negativeBtn?.text = negativeText

            positiveBtn?.setOnClickListener {
                dialog.dismiss()
                onPositive()
            }

            negativeBtn?.setOnClickListener {
                dialog.dismiss()
                onNegative?.invoke()
            }

            dialog.show()
        }

        fun showInfoDialog(
            ctx: Context,
            title: String,
            message: String,
            buttonText: String = "OK",
            onClose: (() -> Unit)? = null
        ) {
            val dialog = Dialog(ctx)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(true)
            dialog.setContentView(R.layout.dialog_info)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val titleView = dialog.findViewById<TextView>(R.id.info_dialog_title)
            val messageView = dialog.findViewById<TextView>(R.id.info_dialog_message)
            val okBtn = dialog.findViewById<Button>(R.id.btn_ok)

            titleView?.text = title
            messageView?.text = message
            okBtn?.text = buttonText

            okBtn?.setOnClickListener {
                dialog.dismiss()
                onClose?.invoke()
            }
            
            dialog.setOnDismissListener { onClose?.invoke() }
            dialog.show()
        }
    }
}
