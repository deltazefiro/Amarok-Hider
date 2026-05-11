package deltazero.amarok.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import deltazero.amarok.AmarokApplication
import deltazero.amarok.R
import deltazero.amarok.utils.HashUtil

class PasswordAuthFragment : BottomSheetDialogFragment() {
  private var onVerifiedCallback: OnVerifiedCallback? = null
  private lateinit var etPassword: TextInputEditText
  private lateinit var tilPassword: TextInputLayout

  fun interface OnVerifiedCallback {
    fun onVerified(succeed: Boolean)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    isCancelable = false
    val fragmentView = inflater.inflate(R.layout.dialog_security, container, false)

    fragmentView.findViewById<View>(R.id.security_dialog_bt_unlock).setOnClickListener { verify() }
    fragmentView.findViewById<View>(R.id.security_dialog_bt_cancel).setOnClickListener {
      onVerifiedCallback?.onVerified(false)
    }
    etPassword = fragmentView.findViewById(R.id.security_dialog_et_password_input)
    tilPassword = fragmentView.findViewById(R.id.security_dialog_til_password_input)

    // Clear error on text changed
    etPassword.addTextChangedListener(
      object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable) {
          tilPassword.error = null
          tilPassword.isErrorEnabled = false
        }
      }
    )

    return fragmentView
  }

  fun setOnVerifiedCallback(onVerifiedCallback: OnVerifiedCallback?): PasswordAuthFragment {
    this.onVerifiedCallback = onVerifiedCallback
    return this
  }

  private fun verify() {
    val password =
      (requireActivity().application as AmarokApplication).settingsRepo.settings.value.password

    if (password == null || HashUtil.calculateHash(etPassword.text!!.toString()) == password) {
      onVerifiedCallback?.onVerified(true)
      dismiss()
    } else {
      tilPassword.error = getText(R.string.password_incorrect)
    }
  }
}
