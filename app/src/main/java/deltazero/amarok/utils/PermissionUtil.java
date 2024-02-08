package deltazero.amarok.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.util.List;

import deltazero.amarok.R;


public class PermissionUtil {

    public static void requestStoragePermission(Context context) {
        if (XXPermissions.isGranted(context, Permission.MANAGE_EXTERNAL_STORAGE))
            return;

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.storage_permission_request_title)
                .setMessage(R.string.storage_permission_request_message)
                .setPositiveButton(R.string.ok, (dialog, which) -> {

                    // Request permissions
                    XXPermissions.with(context)
                            .permission(com.hjq.permissions.Permission.MANAGE_EXTERNAL_STORAGE)
                            .request(new OnPermissionCallback() {
                                @Override
                                public void onGranted(@NonNull List<String> permissions, boolean all) {
                                    Log.d("Permission", "Granted: MANAGE_EXTERNAL_STORAGE");
                                }

                                @Override
                                public void onDenied(@NonNull List<String> permissions, boolean never) {
                                    Log.w("Permission", "User denied: MANAGE_EXTERNAL_STORAGE");
                                    Toast.makeText(context, R.string.storage_permission_denied, Toast.LENGTH_LONG).show();
                                }
                            });

                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
                .show();


    }

    public static void requestNotificationPermission(Context context, OnPermissionCallback callback) {
        if (XXPermissions.isGranted(context, Permission.NOTIFICATION_SERVICE)) {
            callback.onGranted(List.of(), true);
            return;
        }

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.notification_permission_request_title)
                .setMessage(R.string.notification_permission_request_message)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    // Request permissions
                    XXPermissions.with(context)
                            .permission(Permission.NOTIFICATION_SERVICE)
                            .request(callback);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> callback.onDenied(List.of(), false))
                .setOnCancelListener(dialog -> callback.onDenied(List.of(), false))
                .show();
    }

    public static void requestSystemAlertPermission(Context context, OnPermissionCallback callback) {
        if (XXPermissions.isGranted(context, Permission.SYSTEM_ALERT_WINDOW)) {
            callback.onGranted(List.of(), true);
            return;
        }

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.alert_permission_request_title)
                .setMessage(R.string.alert_permission_request_message)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    // Request permissions
                    XXPermissions.with(context)
                            .permission(Permission.SYSTEM_ALERT_WINDOW)
                            .request(callback);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> callback.onDenied(List.of(), false))
                .setOnCancelListener(dialog -> callback.onDenied(List.of(), false))
                .show();
    }

}