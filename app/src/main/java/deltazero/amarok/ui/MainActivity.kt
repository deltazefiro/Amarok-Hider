package deltazero.amarok.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hjq.permissions.XXPermissions
import deltazero.amarok.AmarokActivity
import deltazero.amarok.Hider
import deltazero.amarok.PrefMgr
import deltazero.amarok.R
import deltazero.amarok.apphider.NoneAppHider
import deltazero.amarok.filehider.NoneFileHider
import deltazero.amarok.ui.settings.SettingsActivity
import deltazero.amarok.ui.settings.SwitchAppHiderActivity
import deltazero.amarok.ui.settings.SwitchFileHiderActivity
import deltazero.amarok.ui.theme.AmarokTheme
import deltazero.amarok.utils.PermissionUtil
import deltazero.amarok.utils.UpdateUtil

class MainActivity : AmarokActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AmarokTheme {
                MainScreen(
                    onSetHideFiles = { setHideFile() },
                    onSetHideApps = { setHideApps() },
                    onSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                    onChangeStatus = { changeStatus() }
                )
            }
        }

        // Show welcome dialog
        if (PrefMgr.getShowWelcome()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.welcome_title)
                .setMessage(R.string.welcome_msg)
                .setPositiveButton(R.string.ok) { _, _ -> PermissionUtil.requestStoragePermission(this) }
                .setNegativeButton(R.string.view_github_repo) { _, _ ->
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/deltazefiro/Amarok-Hider")))
                    PermissionUtil.requestStoragePermission(this)
                }
                .setOnCancelListener { PermissionUtil.requestStoragePermission(this) }
                .show()
            PrefMgr.setShowWelcome(false)
        } else {
            PermissionUtil.requestStoragePermission(this)
        }

        // Check Hiders availability
        PrefMgr.getAppHider(this).tryToActivate { _, succeed, msg ->
            if (succeed) return@tryToActivate
            Hider.showNoHiderDialog(this, msg)
        }

        PrefMgr.getFileHider(this).tryToActive { _, succeed, msg ->
            if (succeed) return@tryToActive
            PrefMgr.setFileHiderMode(NoneFileHider::class.java)
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.filehider_not_ava_title)
                .setMessage(msg)
                .setPositiveButton(R.string.switch_file_hider) { _, _ ->
                    startActivity(Intent(this, SwitchFileHiderActivity::class.java))
                }
                .setNegativeButton(getString(R.string.ok), null)
                .show()
        }

        if (PrefMgr.getEnableAutoUpdate()) {
            UpdateUtil.checkAndNotify(this, true)
        }
    }

    private fun changeStatus() {
        if (Hider.getState() == Hider.State.HIDDEN) Hider.unhide(this)
        else Hider.hide(this)
    }

    private fun setHideApps() {
        if (Hider.getState() == Hider.State.HIDDEN) {
            Toast.makeText(this, R.string.setting_not_ava_when_hidden, Toast.LENGTH_SHORT).show()
            return
        }
        if (PrefMgr.getAppHider(this) is NoneAppHider) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.apphider_not_activated_title)
                .setMessage(R.string.apphider_not_activated_msg)
                .setPositiveButton(R.string.switch_app_hider) { _, _ ->
                    startActivity(Intent(this, SwitchAppHiderActivity::class.java))
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
            return
        }
        startActivity(Intent(this, SetHideAppActivity::class.java))
    }

    private fun setHideFile() {
        if (!XXPermissions.isGranted(this, com.hjq.permissions.Permission.MANAGE_EXTERNAL_STORAGE)) {
            Toast.makeText(this, R.string.storage_permission_denied, Toast.LENGTH_LONG).show()
            return
        }
        if (Hider.getState() == Hider.State.HIDDEN) {
            Toast.makeText(this, R.string.setting_not_ava_when_hidden, Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(this, SetHideFilesActivity::class.java))
    }
}
