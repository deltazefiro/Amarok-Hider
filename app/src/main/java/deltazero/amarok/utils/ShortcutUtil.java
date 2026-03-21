package deltazero.amarok.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.util.Collections;
import java.util.Set;

import deltazero.amarok.PrefMgr;
import deltazero.amarok.R;
import deltazero.amarok.ui.ShortcutLaunchActivity;
import deltazero.amarok.utils.AppInfoUtil.AppInfo;

public class ShortcutUtil {

    private static final String TAG = "ShortcutUtil";

    /**
     * 为指定应用创建桌面快捷方式。
     * 点击快捷方式会启动 {@link ShortcutLaunchActivity}（透明、无动画），
     * 后者立即发广播给 ShortcutReceiver，自身 finish()，
     * 由 ShortcutReceiver 完成 unhide + 启动目标应用。
     */
    public static void createShortcut(Context context, AppInfo app) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Toast.makeText(context, R.string.shortcut_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }

        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        if (!shortcutManager.isRequestPinShortcutSupported()) {
            Toast.makeText(context, R.string.shortcut_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent launchIntent = new Intent(context, ShortcutLaunchActivity.class);
        launchIntent.setAction(Intent.ACTION_VIEW);
        launchIntent.putExtra(ShortcutLaunchActivity.EXTRA_TARGET_PACKAGE, app.packageName());
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        ShortcutInfo shortcut = new ShortcutInfo.Builder(context, shortcutId(app.packageName()))
                .setShortLabel(app.label())
                .setLongLabel(app.label())
                .setIcon(Icon.createWithBitmap(drawableToBitmap(app.icon())))
                .setIntent(launchIntent)
                .build();

        shortcutManager.requestPinShortcut(shortcut, null);
        Log.i(TAG, "Shortcut created for: " + app.packageName());

        // 记录到 Preference
        Set<String> shortcuts = PrefMgr.getShortcutApps();
        shortcuts.add(app.packageName());
        PrefMgr.setShortcutApps(shortcuts);
    }

    /**
     * 移除指定应用的快捷方式记录，并禁用桌面快捷方式。
     */
    public static void removeShortcut(Context context, String packageName) {
        Set<String> shortcuts = PrefMgr.getShortcutApps();
        shortcuts.remove(packageName);
        PrefMgr.setShortcutApps(shortcuts);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            shortcutManager.disableShortcuts(
                    Collections.singletonList(shortcutId(packageName)),
                    context.getString(R.string.shortcut_disabled_hint)
            );
        }
        Log.i(TAG, "Shortcut removed for: " + packageName);
    }

    public static String shortcutId(String packageName) {
        return "shortcut_" + packageName;
    }

    private static Bitmap drawableToBitmap(Drawable drawable) {
        int width = drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : 96;
        int height = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : 96;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
