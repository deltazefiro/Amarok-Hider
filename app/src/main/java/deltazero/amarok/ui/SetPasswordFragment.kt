package deltazero.amarok.ui

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import deltazero.amarok.R
import deltazero.amarok.ui.theme.AmarokTheme

class SetPasswordFragment : BottomSheetDialogFragment() {
  fun interface OnSetPasswordCallback {
    fun onSetPassword(password: String?)
  }

  private var callback: OnSetPasswordCallback? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View =
    ComposeView(requireContext()).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        AmarokTheme {
          SetPasswordSheet(
            onConfirm = { password ->
              callback?.onSetPassword(password)
              dismiss()
            },
            onCancel = {
              callback?.onSetPassword(null)
              dismiss()
            },
          )
        }
      }
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

@Composable
private fun SetPasswordSheet(onConfirm: (String) -> Unit, onCancel: () -> Unit) {
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var passwordError by remember { mutableStateOf<String?>(null) }
  var confirmError by remember { mutableStateOf<String?>(null) }
  val lengthError = stringResource(R.string.password_length_error)
  val mismatchError = stringResource(R.string.password_mismatch_error)

  Column(
    modifier =
      Modifier.fillMaxWidth().padding(start = 40.dp, end = 40.dp, top = 40.dp, bottom = 50.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
      Spacer(Modifier.width(5.dp))
      Text(stringResource(R.string.set_password), style = MaterialTheme.typography.titleMedium)
    }
    OutlinedTextField(
      value = password,
      onValueChange = {
        password = it
        passwordError = null
        confirmError = null
      },
      modifier = Modifier.fillMaxWidth().padding(top = 30.dp),
      label = { Text(stringResource(R.string.password)) },
      singleLine = true,
      isError = passwordError != null,
      supportingText = passwordError?.let { { Text(it) } },
      visualTransformation = PasswordVisualTransformation(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
    )
    OutlinedTextField(
      value = confirmPassword,
      onValueChange = {
        confirmPassword = it
        passwordError = null
        confirmError = null
      },
      modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
      label = { Text(stringResource(R.string.confirm_password)) },
      singleLine = true,
      isError = confirmError != null,
      supportingText = confirmError?.let { { Text(it) } },
      visualTransformation = PasswordVisualTransformation(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
    )
    Row(
      modifier = Modifier.fillMaxWidth().padding(top = 35.dp),
      horizontalArrangement = Arrangement.End,
    ) {
      TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
      Button(
        onClick = {
          when {
            password.length < 3 || password.length > 15 -> passwordError = lengthError
            password != confirmPassword -> confirmError = mismatchError
            else -> onConfirm(password)
          }
        }
      ) {
        Text(stringResource(R.string.ok))
      }
    }
  }
}
