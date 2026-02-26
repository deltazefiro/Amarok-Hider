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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import deltazero.amarok.AmarokActivity
import deltazero.amarok.PrefMgr
import deltazero.amarok.R
import deltazero.amarok.apphider.BaseAppHider
import deltazero.amarok.apphider.DhizukuAppHider
import deltazero.amarok.apphider.DsmAppHider
import deltazero.amarok.apphider.NoneAppHider
import deltazero.amarok.apphider.RootAppHider
import deltazero.amarok.apphider.ShizukuAppHider
import deltazero.amarok.ui.theme.AmarokTheme

class SwitchAppHiderActivity : AmarokActivity() {

    private var selectedHider by mutableStateOf<Class<out BaseAppHider>>(NoneAppHider::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedHider = PrefMgr.getAppHider(this).javaClass
        setContent {
            AmarokTheme {
                SwitchAppHiderScreen(
                    selectedHider = selectedHider,
                    onSelectDisabled = { activate(NoneAppHider(this)) },
                    onSelectRoot = { activate(RootAppHider(this)) },
                    onSelectShizuku = { activate(ShizukuAppHider(this)) },
                    onSelectDhizuku = { activate(DhizukuAppHider(this)) },
                    onSelectDsm = { activate(DsmAppHider(this)) },
                    onLearnMore = {
                        startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.hideapp_doc_url)))
                        )
                    },
                    onBack = { finish() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        selectedHider = PrefMgr.getAppHider(this).javaClass
    }

    private fun activate(hider: BaseAppHider) {
        hider.tryToActivate { cls, success, msgResID ->
            if (success) {
                PrefMgr.setAppHiderMode(cls)
                selectedHider = cls
            } else {
                PrefMgr.setAppHiderMode(NoneAppHider::class.java)
                selectedHider = NoneAppHider::class.java
                runOnUiThread {
                    val builder = MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.apphider_not_ava_title)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwitchAppHiderScreen(
    selectedHider: Class<*>,
    onSelectDisabled: () -> Unit,
    onSelectRoot: () -> Unit,
    onSelectShizuku: () -> Unit,
    onSelectDhizuku: () -> Unit,
    onSelectDsm: () -> Unit,
    onLearnMore: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.switch_app_hider)) },
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
                title = stringResource(R.string.apphider_none),
                description = stringResource(R.string.apphider_none_description),
                selected = selectedHider == NoneAppHider::class.java,
                onClick = onSelectDisabled
            )
            RadioOptionItem(
                title = stringResource(R.string.apphider_root),
                description = stringResource(R.string.apphider_root_description),
                selected = selectedHider == RootAppHider::class.java,
                onClick = onSelectRoot
            )
            RadioOptionItem(
                title = stringResource(R.string.apphider_shizuku),
                description = stringResource(R.string.apphider_shizuku_description),
                selected = selectedHider == ShizukuAppHider::class.java,
                onClick = onSelectShizuku
            )
            RadioOptionItem(
                title = stringResource(R.string.apphider_dhizuku),
                description = stringResource(R.string.apphider_dhizuku_description),
                selected = selectedHider == DhizukuAppHider::class.java,
                onClick = onSelectDhizuku
            )
            RadioOptionItem(
                title = stringResource(R.string.apphider_dsm),
                description = stringResource(R.string.apphider_dsm_description),
                selected = selectedHider == DsmAppHider::class.java,
                onClick = onSelectDsm
            )

            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(onClick = onLearnMore) { Text(stringResource(R.string.learn_more)) }
                Button(onClick = onBack) { Text(stringResource(R.string.ok)) }
            }
        }
    }
}
