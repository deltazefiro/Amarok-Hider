package deltazero.amarok.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import deltazero.amarok.R

object PermissionUtil {
  @JvmStatic
  fun requestStoragePermission(context: Context) {
    if (XXPermissions.isGranted(context, Permission.MANAGE_EXTERNAL_STORAGE)) return

    MaterialAlertDialogBuilder(context)
      .setTitle(R.string.storage_permission_request_title)
      .setMessage(R.string.storage_permission_request_message)
      .setPositiveButton(R.string.ok) { _, _ ->
        // Request permissions
        XXPermissions.with(context)
          .permission(Permission.MANAGE_EXTERNAL_STORAGE)
          .request(
            object : OnPermissionCallback {
              override fun onGranted(permissions: MutableList<String>, all: Boolean) {
                Log.d("Permission", "Granted: MANAGE_EXTERNAL_STORAGE")
              }

              override fun onDenied(permissions: MutableList<String>, never: Boolean) {
                Log.w("Permission", "User denied: MANAGE_EXTERNAL_STORAGE")
                Toast.makeText(context, R.string.storage_permission_denied, Toast.LENGTH_LONG)
                  .show()
              }
            }
          )
      }
      .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
      .show()
  }

  @JvmStatic
  fun requestNotificationPermission(context: Context, callback: OnPermissionCallback) {
    if (XXPermissions.isGranted(context, Permission.NOTIFICATION_SERVICE)) {
      callback.onGranted(listOf(), true)
      return
    }

    MaterialAlertDialogBuilder(context)
      .setTitle(R.string.notification_permission_request_title)
      .setMessage(R.string.notification_permission_request_message)
      .setPositiveButton(R.string.ok) { _, _ ->
        // Request permissions
        XXPermissions.with(context).permission(Permission.NOTIFICATION_SERVICE).request(callback)
      }
      .setNegativeButton(R.string.cancel) { _, _ -> callback.onDenied(listOf(), false) }
      .setOnCancelListener { callback.onDenied(listOf(), false) }
      .show()
  }

  @JvmStatic
  fun requestSystemAlertPermission(context: Context, callback: OnPermissionCallback) {
    if (XXPermissions.isGranted(context, Permission.SYSTEM_ALERT_WINDOW)) {
      callback.onGranted(listOf(), true)
      return
    }

    MaterialAlertDialogBuilder(context)
      .setTitle(R.string.alert_permission_request_title)
      .setMessage(R.string.alert_permission_request_message)
      .setPositiveButton(R.string.ok) { _, _ ->
        // Request permissions
        XXPermissions.with(context).permission(Permission.SYSTEM_ALERT_WINDOW).request(callback)
      }
      .setNegativeButton(R.string.cancel) { _, _ -> callback.onDenied(listOf(), false) }
      .setOnCancelListener { callback.onDenied(listOf(), false) }
      .show()
  }
}
