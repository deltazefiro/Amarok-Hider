package deltazero.amarok;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;

import androidx.annotation.MainThread;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;

import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.hjq.window.EasyWindow;
import com.hjq.window.draggable.SpringBackDraggable;

import deltazero.amarok.receivers.ActionReceiver;

public class QuickHideService extends LifecycleService {

    private EasyWindow<?> panicButton;
    private ImageView ivPanicButton;

    private PendingIntent activityPendingIntent;
    private static final String CHANNEL_ID = "QUICK_HIDE_CHANNEL";
    private static final int NOTIFICATION_ID = 1;

    private static boolean isServiceRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();

        // Create notification channel
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);

        Intent actionIntent = new Intent(this, ActionReceiver.class);
        actionIntent.setAction(ActionReceiver.ACTION_HIDE);
        activityPendingIntent = PendingIntent.getBroadcast(this, 1, actionIntent, PendingIntent.FLAG_IMMUTABLE);
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
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle(getText(R.string.quick_hide_notification_title))
                        .setContentText(getText(R.string.quick_hide_notification_content))
                        .setSmallIcon(R.drawable.ic_paw)
                        .setContentIntent(activityPendingIntent)
                        .setOngoing(true)
                        .build();

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        isServiceRunning = true;

        // Init panic button
        panicButton = new EasyWindow<>(getApplication())
                .setContentView(R.layout.dialog_panic_button)
                .setGravity(Gravity.END | Gravity.BOTTOM)
                .setYOffset(300)
                .setDraggable(new SpringBackDraggable())
                .setOnClickListener(R.id.dialog_iv_panic_button,
                        (EasyWindow.OnClickListener<ImageView>) (xToast, view) -> Hider.hide(this));

        ivPanicButton = panicButton.findViewById(R.id.dialog_iv_panic_button);
        ivPanicButton.setColorFilter(getColor(R.color.light_grey),
                PorterDuff.Mode.SRC_IN);

        Hider.state.observe(this, state -> updatePanicButton());
        updatePanicButton();

        Log.i("QuickHideService", "Service start.");

        return START_STICKY;
    }

    @MainThread
    public static void startService(Context context) {

        assert PrefMgr.initialized;

        if (isServiceRunning) {
            Log.w("QuickHideService", "Restarting QuickHideService ...");
            stopService(context);
            context.startForegroundService(new Intent(context, QuickHideService.class));
        } else if (!PrefMgr.getEnableQuickHideService()) {
            Log.i("QuickHideService", "QuickHideService is disabled. Skip starting service.");
        } else if (Hider.getState() == Hider.State.HIDDEN) {
            Log.i("QuickHideService", "Current state is hidden. Skip starting service.");
        } else if (!XXPermissions.isGranted(context, Permission.NOTIFICATION_SERVICE)) {
            Log.w("QuickHideService", "Permission denied: NOTIFICATION_SERVICE. Skip starting service.");
            PrefMgr.setEnableQuickHideService(false);
        } else {
            // Start the service
            context.startForegroundService(new Intent(context, QuickHideService.class));
        }
    }

    public static void stopService(Context context) {
        context.stopService(new Intent(context, QuickHideService.class));
    }

    private void updatePanicButton() {
        if (!PrefMgr.getEnablePanicButton())
            return;

        if (!XXPermissions.isGranted(getApplication(), Permission.SYSTEM_ALERT_WINDOW)) {
            Log.w("QuickHideService", "Failed to show PanicButton: Permission denied: SYSTEM_ALERT_WINDOW");
            PrefMgr.setEnablePanicButton(false);
            return;
        }

        if (Hider.getState() == Hider.State.PROCESSING) {
            ivPanicButton.setColorFilter(getApplication().getColor(com.google.android.material.R.color.design_default_color_error),
                    android.graphics.PorterDuff.Mode.SRC_IN);
            ivPanicButton.setEnabled(false);
        } else {
            if (Hider.getState() == Hider.State.HIDDEN) {
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
