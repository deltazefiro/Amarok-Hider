package deltazero.amarok.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import deltazero.amarok.Hider;

/**
 * 接收快捷方式点击广播，执行 unhide + 启动目标应用。
 *
 * <p>相比中转 Activity 方案，广播方案无任何 Activity 切换动画，用户体验更流畅。
 *
 * <p>广播 action: {@link #ACTION}，携带 extra {@link #EXTRA_TARGET_PACKAGE}。
 */
public class ShortcutReceiver extends BroadcastReceiver {

    public static final String ACTION = "deltazero.amarok.SHORTCUT_LAUNCH";
    public static final String EXTRA_TARGET_PACKAGE = "target_package";
    private static final String TAG = "ShortcutReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION.equals(intent.getAction())) return;

        String targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE);
        if (targetPackage == null) {
            Log.w(TAG, "No target package specified.");
            return;
        }

        Log.i(TAG, "Shortcut broadcast received for: " + targetPackage);

        // unhideOne 完成后回调主线程启动 App
        Hider.unhideOneWithCallback(context, targetPackage, () -> {
            PackageManager pm = context.getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(targetPackage);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
                Log.i(TAG, "Launched: " + targetPackage);
            } else {
                Log.w(TAG, "No launch intent for: " + targetPackage);
            }
        });
    }
}
