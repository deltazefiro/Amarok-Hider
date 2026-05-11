package deltazero.amarok.ui

import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import deltazero.amarok.R

class SetPasswordFragment : BottomSheetDialogFragment() {
  fun interface OnSetPasswordCallback {
    fun onSetPassword(password: String?)
  }

  private lateinit var etlPassword: TextInputLayout
  private lateinit var etlConfirmPassword: TextInputLayout
  private lateinit var etPassword: TextInputEditText
  private lateinit var etConfirmPassword: TextInputEditText
  private lateinit var btOk: MaterialButton
  private lateinit var btCancel: MaterialButton
  private var callback: OnSetPasswordCallback? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View = inflater.inflate(R.layout.dialog_set_password, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    etlPassword = view.findViewById(R.id.set_password_dialog_til_password)
    etlConfirmPassword = view.findViewById(R.id.set_password_dialog_til_confirm_password)
    etPassword = view.findViewById(R.id.set_password_dialog_et_password)
    etConfirmPassword = view.findViewById(R.id.set_password_dialog_et_confirm_password)
    btCancel = view.findViewById(R.id.set_password_dialog_bt_cancel)
    btOk = view.findViewById(R.id.set_password_dialog_bt_ok)

    btOk.setOnClickListener {
      val password = etPassword.text!!.toString()
      val confirmPassword = etConfirmPassword.text!!.toString()

      if (password.length < 3 || password.length > 15) {
        etlPassword.error = getString(R.string.password_length_error)
        return@setOnClickListener
      }

      if (password != confirmPassword) {
        etlConfirmPassword.error = getString(R.string.password_mismatch_error)
        return@setOnClickListener
      }

      callback!!.onSetPassword(password)
      dismiss()
    }

    btCancel.setOnClickListener {
      callback?.onSetPassword(null)
      dismiss()
    }

    etPassword.addTextChangedListener(
      object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable) {
          etlPassword.error = null
          etlConfirmPassword.error = null
          etlPassword.isErrorEnabled = false
          etlConfirmPassword.isErrorEnabled = false
        }
      }
    )
  }

  fun setCallback(callback: OnSetPasswordCallback?): SetPasswordFragment {
    this.callback = callback
    return this
  }

  override fun onCancel(dialog: DialogInterface) {
    callback?.onSetPassword(null)
    super.onCancel(dialog)
  }
}
