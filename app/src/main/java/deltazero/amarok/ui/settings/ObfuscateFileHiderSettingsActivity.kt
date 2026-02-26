package deltazero.amarok.ui.settings

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
import deltazero.amarok.AmarokActivity
import deltazero.amarok.PrefMgr
import deltazero.amarok.R
import deltazero.amarok.ui.theme.AmarokTheme

class ObfuscateFileHiderSettingsActivity : AmarokActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AmarokTheme {
                ObfuscateSettingsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ObfuscateSettingsScreen(onBack: () -> Unit) {
    var obfuscateHeader by remember { mutableStateOf(PrefMgr.getEnableObfuscateFileHeader()) }
    var obfuscateText by remember { mutableStateOf(PrefMgr.getEnableObfuscateTextFile()) }
    var obfuscateTextEnhanced by remember { mutableStateOf(PrefMgr.getEnableObfuscateTextFileEnhanced()) }

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
            SwitchSettingItem(
                title = stringResource(R.string.obfuscate_file_header),
                description = stringResource(R.string.obfuscate_file_header_description),
                checked = obfuscateHeader,
                onCheckedChange = { checked ->
                    obfuscateHeader = checked
                    PrefMgr.setEnableObfuscateFileHeader(checked)
                    if (!checked) {
                        obfuscateText = false
                        PrefMgr.setEnableObfuscateTextFile(false)
                        obfuscateTextEnhanced = false
                        PrefMgr.setEnableObfuscateTextFileEnhanced(false)
                    }
                }
            )
            SwitchSettingItem(
                title = stringResource(R.string.obfuscate_text_file),
                description = stringResource(R.string.obfuscate_text_file_description),
                checked = obfuscateText,
                enabled = obfuscateHeader,
                onCheckedChange = { checked ->
                    obfuscateText = checked
                    PrefMgr.setEnableObfuscateTextFile(checked)
                    if (!checked) {
                        obfuscateTextEnhanced = false
                        PrefMgr.setEnableObfuscateTextFileEnhanced(false)
                    }
                }
            )
            SwitchSettingItem(
                title = stringResource(R.string.obfuscate_text_file_enhanced),
                description = stringResource(R.string.obfuscate_text_file_description_enhanced),
                checked = obfuscateTextEnhanced,
                enabled = obfuscateHeader && obfuscateText,
                onCheckedChange = { checked ->
                    obfuscateTextEnhanced = checked
                    PrefMgr.setEnableObfuscateTextFileEnhanced(checked)
                }
            )
        }
    }
}
