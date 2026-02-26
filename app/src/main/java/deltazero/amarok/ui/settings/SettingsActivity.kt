package deltazero.amarok.ui.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import com.hjq.permissions.OnPermissionCallback
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import deltazero.amarok.AmarokActivity
import deltazero.amarok.PrefMgr
import deltazero.amarok.QuickHideService
import deltazero.amarok.R
import deltazero.amarok.ui.CountdownConfirmDialog
import deltazero.amarok.ui.SetPasswordFragment
import deltazero.amarok.ui.theme.AmarokTheme
import deltazero.amarok.utils.PermissionUtil

class SettingsActivity : AmarokActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AmarokTheme {
                SettingsScreen(
                    onBack = { finish() },
                    onSwitchAppHider = {
                        startActivity(android.content.Intent(this, SwitchAppHiderActivity::class.java))
                    },
                    onSwitchFileHider = {
                        startActivity(android.content.Intent(this, SwitchFileHiderActivity::class.java))
                    },
                    onSetPassword = { callback ->
                        SetPasswordFragment()
                            .setCallback { password -> callback(password) }
                            .show(supportFragmentManager, null)
                    },
                    onShowCountdownConfirm = { onConfirm, onCancel ->
                        CountdownConfirmDialog.Builder(this)
                            .setTitle(R.string.hide_amarok_icon_dialog_title)
                            .setMessage(R.string.hide_amarok_icon_dialog_message)
                            .setCountdownSeconds(10)
                            .setOnConfirmAction(onConfirm)
                            .setOnCancelAction(onCancel)
                            .show()
                    },
                    onRequestNotificationPermission = { onGranted, onDenied ->
                        PermissionUtil.requestNotificationPermission(this, object : OnPermissionCallback {
                            override fun onGranted(permissions: MutableList<String>, all: Boolean) { onGranted() }
                            override fun onDenied(permissions: MutableList<String>, never: Boolean) { onDenied() }
                        })
                    },
                    onRequestSystemAlertPermission = { onGranted, onDenied ->
                        PermissionUtil.requestSystemAlertPermission(this, object : OnPermissionCallback {
                            override fun onGranted(permissions: MutableList<String>, all: Boolean) { onGranted() }
                            override fun onDenied(permissions: MutableList<String>, never: Boolean) { onDenied() }
                        })
                    },
                    onShowColorPicker = {
                        val builder = ColorPickerDialog.Builder(this)
                            .setTitle(R.string.panic_button_color)
                            .setPreferenceName("PanicButtonColorPicker")
                            .setPositiveButton(
                                getString(android.R.string.ok),
                                ColorEnvelopeListener { envelope, _ ->
                                    PrefMgr.setPanicButtonColor(envelope.color)
                                    QuickHideService.startService(this)
                                }
                            )
                            .setNegativeButton(getString(android.R.string.cancel)) { dialog, _ -> dialog.dismiss() }
                            .attachAlphaSlideBar(true)
                            .attachBrightnessSlideBar(true)
                            .setBottomSpace(12)
                        builder.colorPickerView.setInitialColor(PrefMgr.getPanicButtonColor())
                        builder.show()
                    },
                    onSwitchLocale = {
                        deltazero.amarok.utils.SwitchLocaleUtil.switchLocale(this)
                    }
                )
            }
        }
    }
}
