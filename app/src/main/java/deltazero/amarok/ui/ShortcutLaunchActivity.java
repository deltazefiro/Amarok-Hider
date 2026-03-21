package deltazero.amarok.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import deltazero.amarok.receivers.ShortcutReceiver;

/**
 * 快捷方式点击后的轻量中转 Activity（透明、无动画）。
 *
 * <p>Android ShortcutInfo.setIntent() 只支持 Activity，无法直接触发广播，
 * 因此保留此 Activity 作为入口，但其唯一职责是：
 * <ol>
 *   <li>立即发广播给 {@link ShortcutReceiver}</li>
 *   <li>立即 finish()，用户感知不到此页面</li>
 * </ol>
 * 实际的 unhide + 启动目标应用逻辑全部在 ShortcutReceiver 中执行。
 */
public class ShortcutLaunchActivity extends AppCompatActivity {

    public static final String EXTRA_TARGET_PACKAGE = "target_package";
    private static final String TAG = "ShortcutLaunchActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 无进入动画
        overridePendingTransition(0, 0);

        String targetPackage = getIntent().getStringExtra(EXTRA_TARGET_PACKAGE);
        if (targetPackage == null) {
            Log.w(TAG, "No target package specified.");
            finish();
            overridePendingTransition(0, 0);
            return;
        }

        Log.i(TAG, "Relaying shortcut to ShortcutReceiver for: " + targetPackage);

        // 发广播，由 ShortcutReceiver 执行 unhide + 启动
        Intent broadcast = new Intent(ShortcutReceiver.ACTION);
        broadcast.setPackage(getPackageName());
        broadcast.putExtra(ShortcutReceiver.EXTRA_TARGET_PACKAGE, targetPackage);
        sendBroadcast(broadcast);

        // 立即退出，用户感知不到此页面
        finish();
        overridePendingTransition(0, 0);
    }
}
