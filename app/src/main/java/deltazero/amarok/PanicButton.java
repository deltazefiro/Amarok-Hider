package deltazero.amarok;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.lifecycle.MutableLiveData;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.hjq.xtoast.XToast;
import com.hjq.xtoast.draggable.SpringDraggable;

import java.util.List;

public class PanicButton {

    private final Context context;
    private final MutableLiveData<Boolean> isProcessing;
    private final XToast<?> xToast;
    private final Hider hider;
    private final ImageView ivPanicButton;

    public PanicButton(Context context) {
        this.context = context;
        hider = new Hider(context);

        xToast = new XToast<>((Application) context.getApplicationContext())
                .setContentView(R.layout.dialog_panic_button)
                .setGravity(Gravity.END | Gravity.BOTTOM)
                .setYOffset(300)
                .setDraggable(new SpringDraggable())
                .setOnClickListener(R.id.dialog_iv_panic_button,
                        (XToast.OnClickListener<ImageView>) (xToast, view) -> hider.hide());

        ivPanicButton = xToast.findViewById(R.id.dialog_iv_panic_button);
        ivPanicButton.setColorFilter(context.getColor(R.color.light_grey),
                android.graphics.PorterDuff.Mode.SRC_IN);

        isProcessing = hider.getIsProcessingLiveData();
        isProcessing.observeForever(aBoolean -> updateToastState());
    }

    private void requestPermission() {
        if (XXPermissions.isGranted(context, Permission.SYSTEM_ALERT_WINDOW))
            return;

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.alert_permission_request_title)
                .setMessage(R.string.alert_permission_request_message)
                .setPositiveButton("OK", (dialog, which) -> {
                    // Request permissions
                    XXPermissions.with(context)
                            .permission(Permission.SYSTEM_ALERT_WINDOW)
                            .request(new OnPermissionCallback() {
                                @Override
                                public void onGranted(List<String> permissions, boolean all) {
                                    Log.d("Permission", "Granted: SYSTEM_ALERT_WINDOW");
                                }

                                @Override
                                public void onDenied(List<String> permissions, boolean never) {
                                    Log.w("Permission", "User denied: SYSTEM_ALERT_WINDOW");
                                    Toast.makeText(context, R.string.alert_permission_denied, Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel())
                .show();
    }

    public void updateToastState() {
        if (!hider.prefMgr.getEnablePanicButton()) {
            xToast.cancel();
            return;
        }

        requestPermission();

        assert isProcessing.getValue() != null;
        if (isProcessing.getValue()) {
            ivPanicButton.setColorFilter(context.getColor(R.color.design_default_color_error),
                    android.graphics.PorterDuff.Mode.SRC_IN);
            ivPanicButton.setEnabled(false);
        } else {
            if (hider.prefMgr.getIsHidden()) {
                xToast.cancel();
            } else {
                xToast.show();
            }
            ivPanicButton.setColorFilter(context.getColor(R.color.light_grey),
                    android.graphics.PorterDuff.Mode.SRC_IN);
            ivPanicButton.setEnabled(true);
        }
    }
}
