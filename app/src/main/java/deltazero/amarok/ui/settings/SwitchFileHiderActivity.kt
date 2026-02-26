package deltazero.amarok.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import deltazero.amarok.AmarokActivity
import deltazero.amarok.PrefMgr
import deltazero.amarok.R
import deltazero.amarok.filehider.BaseFileHider
import deltazero.amarok.filehider.ChmodFileHider
import deltazero.amarok.filehider.NoMediaFileHider
import deltazero.amarok.filehider.NoneFileHider
import deltazero.amarok.filehider.ObfuscateFileHider
import deltazero.amarok.ui.theme.AmarokTheme

class SwitchFileHiderActivity : AmarokActivity() {

    private var selectedHider by mutableStateOf<Class<out BaseFileHider>>(NoneFileHider::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedHider = PrefMgr.getFileHider(this).javaClass
        setContent {
            AmarokTheme {
                SwitchFileHiderScreen(
                    selectedHider = selectedHider,
                    onSelectDisabled = { activate(NoneFileHider(this)) },
                    onSelectObfuscate = { activate(ObfuscateFileHider(this)) },
                    onSelectChmod = { showChmodWarning() },
                    onSelectNoMedia = { activate(NoMediaFileHider(this)) },
                    onObfuscateSettings = {
                        startActivity(Intent(this, ObfuscateFileHiderSettingsActivity::class.java))
                    },
                    onBack = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        selectedHider = PrefMgr.getFileHider(this).javaClass
    }

    private fun activate(hider: BaseFileHider) {
        hider.tryToActive { cls, success, msgResID ->
            if (success) {
                PrefMgr.setFileHiderMode(cls)
                selectedHider = cls
            } else {
                PrefMgr.setFileHiderMode(NoneFileHider::class.java)
                selectedHider = NoneFileHider::class.java
                runOnUiThread {
                    val builder = MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.filehider_not_ava_title)
                        .setPositiveButton(R.string.ok, null)
                        .setNegativeButton(R.string.help) { _, _ ->
                            startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.common_error_doc_url)))
                            )
                        }
                    if (msgResID != 0) builder.setMessage(msgResID)
                    builder.show()
                }
            }
        }
    }

    private fun showChmodWarning() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.warning)
            .setMessage(R.string.chmod_samsung_warning)
            .setPositiveButton(R.string.ok) { _, _ -> activate(ChmodFileHider(this)) }
            .setNegativeButton(R.string.cancel) { _, _ -> activate(NoneFileHider(this)) }
            .show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwitchFileHiderScreen(
    selectedHider: Class<*>,
    onSelectDisabled: () -> Unit,
    onSelectObfuscate: () -> Unit,
    onSelectChmod: () -> Unit,
    onSelectNoMedia: () -> Unit,
    onObfuscateSettings: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.switch_file_hider)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            RadioOptionItem(
                title = stringResource(R.string.filehider_none),
                description = stringResource(R.string.filehider_none_description),
                selected = selectedHider == NoneFileHider::class.java,
                onClick = onSelectDisabled
            )
            RadioOptionItem(
                title = stringResource(R.string.filehider_obfuscate),
                description = stringResource(R.string.filehider_obfuscate_description),
                selected = selectedHider == ObfuscateFileHider::class.java,
                onClick = onSelectObfuscate,
                trailingContent = {
                    IconButton(onClick = onObfuscateSettings) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                    }
                }
            )
            RadioOptionItem(
                title = stringResource(R.string.filehider_chmod),
                description = stringResource(R.string.filehider_chmod_description),
                selected = selectedHider == ChmodFileHider::class.java,
                onClick = onSelectChmod
            )
            RadioOptionItem(
                title = stringResource(R.string.filehider_nomedia),
                description = stringResource(R.string.filehider_nomedia_description),
                selected = selectedHider == NoMediaFileHider::class.java,
                onClick = onSelectNoMedia
            )

            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = onBack) { Text(stringResource(R.string.ok)) }
            }
        }
    }
}
