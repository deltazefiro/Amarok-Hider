package deltazero.amarok;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.MutableLiveData;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.hjq.xtoast.XToast;
import com.hjq.xtoast.draggable.SpringDraggable;

import java.util.List;

import deltazero.amarok.ui.MainActivity;

public class QuickHideService extends LifecycleService {

    private MutableLiveData<Boolean> isProcessing;
    private XToast<?> panicButton;
    private Hider hider;
    private ImageView ivPanicButton;

    private PendingIntent pendingIntent;
    private static final String CHANNEL_ID = "QUICK_HIDE_CHANNEL";
    private static final int NOTIFICATION_ID = 1;

    private static boolean isServiceRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();

        hider = new Hider(this);

        // Create notification channel
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);

        pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelPanicButton();
        isServiceRunning = false;
        Log.i("QuickHideService", "Service stopped.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        // Start foreground
        Notification notification =
                new Notification.Builder(this, CHANNEL_ID)
                        .setContentTitle("Quick Hide Service")
                        .setContentText("Your privacy is visible. Be aware.")
                        .setSmallIcon(R.drawable.ic_app)
                        .setContentIntent(pendingIntent)
                        .build();

        startForeground(NOTIFICATION_ID, notification);
        isServiceRunning = true;

        // Init panic button
        panicButton = new XToast<>(getApplication())
                .setContentView(R.layout.dialog_panic_button)
                .setGravity(Gravity.END | Gravity.BOTTOM)
                .setYOffset(300)
                .setDraggable(new SpringDraggable())
                .setOnClickListener(R.id.dialog_iv_panic_button,
                        (XToast.OnClickListener<ImageView>) (xToast, view) -> hider.hide());

        ivPanicButton = panicButton.findViewById(R.id.dialog_iv_panic_button);
        ivPanicButton.setColorFilter(getColor(R.color.light_grey),
                PorterDuff.Mode.SRC_IN);

        isProcessing = hider.getIsProcessingLiveData();
        isProcessing.observe(this, aBoolean -> updatePanicButton());
        updatePanicButton();

        Log.i("QuickHideService", "Service start.");

        return START_STICKY;
    }

    public static void startService(Context context) {

        var prefMgr = new PrefMgr(context);
        if (isServiceRunning) {
            Log.w("QuickHideService", "Restarting QuickHideService ...");
            stopService(context);
            context.startForegroundService(new Intent(context, QuickHideService.class));
        } else if (!prefMgr.getEnableQuickHideService()) {
            Log.i("QuickHideService", "QuickHideService is disabled. Skip starting service.");
        } else if (prefMgr.getIsHidden()) {
            Log.i("QuickHideService", "Current state is hidden. Skip starting service.");
        } else if (!XXPermissions.isGranted(context, Permission.NOTIFICATION_SERVICE)) {
            Log.w("QuickHideService", "Permission denied: NOTIFICATION_SERVICE. Skip starting service.");
            prefMgr.setEnableQuickHideService(false);
        } else {
            // Start the service
            context.startForegroundService(new Intent(context, QuickHideService.class));
        }
    }

    public static void stopService(Context context) {
        context.stopService(new Intent(context, QuickHideService.class));
    }

    private void updatePanicButton() {
        if (!hider.prefMgr.getEnablePanicButton())
            return;

        if (!XXPermissions.isGranted(getApplication(), Permission.SYSTEM_ALERT_WINDOW)) {
            Log.w("QuickHideService", "Failed to show PanicButton: Permission denied: SYSTEM_ALERT_WINDOW");
            hider.prefMgr.setEnablePanicButton(false);
            return;
        }

        assert isProcessing.getValue() != null;
        if (isProcessing.getValue()) {
            ivPanicButton.setColorFilter(getApplication().getColor(R.color.design_default_color_error),
                    android.graphics.PorterDuff.Mode.SRC_IN);
            ivPanicButton.setEnabled(false);
        } else {
            if (hider.prefMgr.getIsHidden()) {
                cancelPanicButton();
            } else {
                showPanicButton();
            }
            ivPanicButton.setColorFilter(getApplication().getColor(R.color.light_grey),
                    android.graphics.PorterDuff.Mode.SRC_IN);
            ivPanicButton.setEnabled(true);
        }
    }

    private void showPanicButton() {
        if (!panicButton.isShowing()) {
            panicButton.show();
        }
    }

    private void cancelPanicButton() {
        if (panicButton != null && panicButton.isShowing()) {
            panicButton.cancel();
        }
    }
}
