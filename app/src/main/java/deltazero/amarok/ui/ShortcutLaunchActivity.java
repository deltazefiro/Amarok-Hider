package deltazero.amarok.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import deltazero.amarok.Hider;

/**
 * 快捷方式点击后的中转 Activity。
 * 先通过 {@link Hider#unhideOneWithCallback} 取消隐藏目标应用，完成后无动画启动目标应用。
 *
 * <p>设计要点：
 * <ul>
 *   <li>进入/退出均无动画，避免空白页闪现</li>
 *   <li>使用回调而非 LiveData observer，避免 LiveData 回放旧值导致
 *       应用尚未取消隐藏就被启动的问题</li>
 *   <li>回调在主线程执行（由 Hider 保证），startActivity 安全</li>
 * </ul>
 */
public class ShortcutLaunchActivity extends AppCompatActivity {

    public static final String EXTRA_TARGET_PACKAGE = "target_package";
    private static final String TAG = "ShortcutLaunchActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 消除本 Activity 的进入动画
        overridePendingTransition(0, 0);

        String targetPackage = getIntent().getStringExtra(EXTRA_TARGET_PACKAGE);
        if (targetPackage == null) {
            Log.w(TAG, "No target package specified.");
            finish();
            overridePendingTransition(0, 0);
            return;
        }

        Log.i(TAG, "Shortcut launched for: " + targetPackage);

        // 取消隐藏完成后立即启动目标 App（回调在主线程执行）
        Hider.unhideOneWithCallback(this, targetPackage, () -> {
            launchApp(targetPackage);
            finish();
            overridePendingTransition(0, 0);
        });
    }

    private void launchApp(String packageName) {
        PackageManager pm = getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launchIntent);
            overridePendingTransition(0, 0);
            Log.i(TAG, "Launched: " + packageName);
        } else {
            Log.w(TAG, "No launch intent for: " + packageName);
        }
    }
}
