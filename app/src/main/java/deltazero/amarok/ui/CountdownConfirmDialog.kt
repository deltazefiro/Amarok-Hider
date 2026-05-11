package deltazero.amarok.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import deltazero.amarok.R

class CountdownConfirmDialog private constructor(builder: Builder) {
  private val context: Context = builder.context
  private val title: String = builder.title
  private val message: String = builder.message
  private val countdownSeconds: Int = builder.countdownSeconds
  private val onConfirmAction: Runnable? = builder.onConfirmAction
  private val onCancelAction: Runnable? = builder.onCancelAction

  fun show() {
    val dialogBuilder = MaterialAlertDialogBuilder(context)
    dialogBuilder.setTitle(title)
    dialogBuilder.setMessage(message)

    // Positive button (initially disabled)
    dialogBuilder.setPositiveButton(android.R.string.ok) { _, _ -> onConfirmAction?.run() }

    // Negative button
    dialogBuilder.setNegativeButton(android.R.string.cancel) { _, _ -> onCancelAction?.run() }

    val dialog = dialogBuilder.create()
    dialog.show()

    // Disable the positive button initially and set countdown text
    val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
    if (positiveButton != null) {
      positiveButton.isEnabled = false
      positiveButton.text = context.getString(R.string.confirm_with_countdown, countdownSeconds)
    }

    // Start countdown
    val finalDialog = dialog
    val handler = Handler(Looper.getMainLooper())
    val countdown = intArrayOf(countdownSeconds)

    val countdownRunnable =
      object : Runnable {
        override fun run() {
          if (countdown[0] > 0 && finalDialog.isShowing) {
            countdown[0]--
            val button = finalDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            if (button != null) {
              if (countdown[0] > 0) {
                button.text = context.getString(R.string.confirm_with_countdown, countdown[0])
              } else {
                button.text = context.getString(R.string.confirm)
                button.isEnabled = true
              }
            }
            if (countdown[0] > 0) {
              handler.postDelayed(this, 1000)
            }
          }
        }
      }

    handler.postDelayed(countdownRunnable, 1000)
  }

  class Builder internal constructor(internal val context: Context) {
    internal var title: String = ""
    internal var message: String = ""
    internal var countdownSeconds: Int = 5
    internal var onConfirmAction: Runnable? = null
    internal var onCancelAction: Runnable? = null

    fun setTitle(title: String): Builder {
      this.title = title
      return this
    }

    fun setTitle(titleResId: Int): Builder {
      title = context.getString(titleResId)
      return this
    }

    fun setMessage(message: String): Builder {
      this.message = message
      return this
    }

    fun setMessage(messageResId: Int): Builder {
      message = context.getString(messageResId)
      return this
    }

    fun setCountdownSeconds(seconds: Int): Builder {
      countdownSeconds = seconds
      return this
    }

    fun setOnConfirmAction(action: Runnable?): Builder {
      onConfirmAction = action
      return this
    }

    fun setOnCancelAction(action: Runnable?): Builder {
      onCancelAction = action
      return this
    }

    fun build(): CountdownConfirmDialog = CountdownConfirmDialog(this)

    fun show() {
      build().show()
    }
  }
}
