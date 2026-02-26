package deltazero.amarok.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import deltazero.amarok.AmarokActivity
import deltazero.amarok.PrefMgr
import deltazero.amarok.R
import deltazero.amarok.ui.theme.AmarokTheme
import deltazero.amarok.utils.SDCardUtil
import java.io.File
import java.nio.file.Paths

class SetHideFilesActivity : AmarokActivity() {

    private val TAG = "SetHideFiles"
    private var paths by mutableStateOf(PrefMgr.getHideFilePath().toList())

    private val dirRequest = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri ?: return@registerForActivityResult
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        val newPath = getPathFromUri(uri) ?: run {
            Log.w(TAG, "Not supported Directory: $uri")
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.not_local_storage)
                .setMessage(R.string.not_local_storage_description)
                .setPositiveButton(R.string.ok, null)
                .show()
            return@registerForActivityResult
        }
        addPath(newPath)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AmarokTheme {
                SetHideFilesScreen(
                    paths = paths,
                    onRemovePath = ::removePath,
                    onAddFolder = {
                        try {
                            dirRequest.launch(null)
                        } catch (e: Exception) {
                            MaterialAlertDialogBuilder(this)
                                .setTitle(R.string.failed_to_open_doc_tree)
                                .setMessage(R.string.failed_to_open_doc_tree_description)
                                .show()
                        }
                    },
                    onBack = { finish() }
                )
            }
        }
    }

    private fun addPath(newPath: String) {
        val current = PrefMgr.getHideFilePath()
        val p2 = Paths.get(newPath).toAbsolutePath()
        for (p in current) {
            val p1 = Paths.get(p).toAbsolutePath()
            val msg = when {
                p1.startsWith(p2) -> getString(R.string.path_duplicated_description, newPath, p)
                p2.startsWith(p1) -> getString(R.string.path_duplicated_description, p, newPath)
                else -> null
            }
            if (msg != null) {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.path_duplicated)
                    .setMessage(msg)
                    .setPositiveButton(R.string.ok, null)
                    .show()
                return
            }
        }
        current.add(newPath)
        PrefMgr.setHideFilePath(current)
        paths = current.toList()
        Log.i(TAG, "Added file hide path: $newPath")
    }

    private fun removePath(path: String) {
        val current = PrefMgr.getHideFilePath()
        current.remove(path)
        PrefMgr.setHideFilePath(current)
        paths = current.toList()
    }

    private fun getPathFromUri(uri: Uri): String? {
        val splitUri = uri.path?.split(":") ?: return null
        if (splitUri.size != 2) return null
        SDCardUtil.getSdCardPathFromUri(this, splitUri.toTypedArray())?.let { return it }
        if ("com.android.externalstorage.documents" == uri.authority) {
            return Environment.getExternalStorageDirectory().toString() + File.separator + splitUri[1]
        }
        return null
    }
}
