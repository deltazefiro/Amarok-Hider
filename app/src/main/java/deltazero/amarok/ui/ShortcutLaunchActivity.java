package deltazero.amarok.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import deltazero.amarok.Hider;

/**
 * 快捷方式点击后的中转 Activity。
 * 先通过 {@link Hider#unhideOne} 取消隐藏目标应用，完成后启动目标应用。
 */
public class ShortcutLaunchActivity extends Activity {

    public static final String EXTRA_TARGET_PACKAGE = "target_package";
    private static final String TAG = "ShortcutLaunchActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String targetPackage = getIntent().getStringExtra(EXTRA_TARGET_PACKAGE);
        if (targetPackage == null) {
            Log.w(TAG, "No target package specified.");
            finish();
            return;
        }

        Log.i(TAG, "Shortcut launched for: " + targetPackage);

        // 监听单应用取消隐藏完成事件
        Hider.singleUnhideEvent.observe(this, pkg -> {
            if (targetPackage.equals(pkg)) {
                Hider.singleUnhideEvent.removeObservers(this);
                launchApp(targetPackage);
                finish();
            }
        });

        // 触发单应用取消隐藏
        Hider.unhideOne(this, targetPackage);
    }

    private void launchApp(String packageName) {
        PackageManager pm = getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launchIntent);
            Log.i(TAG, "Launched: " + packageName);
        } else {
            Log.w(TAG, "No launch intent for: " + packageName);
        }
    }
}
