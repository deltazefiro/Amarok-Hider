package deltazero.amarok.ui

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
import dagger.hilt.android.AndroidEntryPoint
import deltazero.amarok.R
import deltazero.amarok.core.SettingsRepository
import deltazero.amarok.ui.theme.AmarokTheme
import deltazero.amarok.utils.HashUtil
import javax.inject.Inject

@AndroidEntryPoint
class PasswordAuthFragment : BottomSheetDialogFragment() {
  @Inject lateinit var settingsRepo: SettingsRepository

  private var onVerifiedCallback: OnVerifiedCallback? = null

  fun interface OnVerifiedCallback {
    fun onVerified(succeed: Boolean)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    isCancelable = false
    return ComposeView(requireContext()).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        AmarokTheme {
          PasswordAuthSheet(
            isCorrect = { input ->
              val password = settingsRepo.settings.value.password
              password == null || HashUtil.calculateHash(input) == password
            },
            onCancel = { onVerifiedCallback?.onVerified(false) },
            onVerified = {
              onVerifiedCallback?.onVerified(true)
              dismiss()
            },
          )
        }
      }
    }
  }

  fun setOnVerifiedCallback(onVerifiedCallback: OnVerifiedCallback?): PasswordAuthFragment {
    this.onVerifiedCallback = onVerifiedCallback
    return this
  }
}

@Composable
private fun PasswordAuthSheet(
  isCorrect: (String) -> Boolean,
  onCancel: () -> Unit,
  onVerified: () -> Unit,
) {
  var password by remember { mutableStateOf("") }
  var showError by remember { mutableStateOf(false) }

  Column(
    modifier =
      Modifier.fillMaxWidth().padding(start = 40.dp, end = 40.dp, top = 40.dp, bottom = 50.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
      Spacer(Modifier.width(5.dp))
      Text(stringResource(R.string.unlock_required), style = MaterialTheme.typography.titleMedium)
    }
    OutlinedTextField(
      value = password,
      onValueChange = {
        password = it
        showError = false
      },
      modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
      label = { Text(stringResource(R.string.password)) },
      singleLine = true,
      isError = showError,
      supportingText =
        if (showError) {
          { Text(stringResource(R.string.password_incorrect)) }
        } else null,
      visualTransformation = PasswordVisualTransformation(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
    )
    Row(
      modifier = Modifier.fillMaxWidth().padding(top = 15.dp),
      horizontalArrangement = Arrangement.End,
    ) {
      TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
      Button(onClick = { if (isCorrect(password)) onVerified() else showError = true }) {
        Text(stringResource(R.string.unlock))
      }
    }
  }
}
