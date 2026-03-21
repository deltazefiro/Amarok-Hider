package deltazero.amarok.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import deltazero.amarok.Hider;
import deltazero.amarok.PrefMgr;

/**
 * 监听应用更新广播（{@link Intent#ACTION_PACKAGE_REPLACED}）。
 * 若被更新的应用在隐藏列表中，则触发单应用维度的取消隐藏，
 * 并由 AutoHide 在锁屏时重新将其隐藏。
 */
public class PackageUpdateReceiver extends BroadcastReceiver {

    private static final String TAG = "PackageUpdateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction())) return;

        String packageName = intent.getData() != null
                ? intent.getData().getSchemeSpecificPart() : null;
        if (packageName == null) return;

        Log.i(TAG, "Package updated: " + packageName);

        // 只处理在隐藏列表中的应用
        if (!PrefMgr.getHideApps().contains(packageName)) return;

        Log.i(TAG, "Hidden app updated: " + packageName);

        if (Hider.getState() == Hider.State.HIDDEN) {
            // 全局隐藏中，单个 App 被更新后系统自动恢复可见
            // 记录到临时列表，锁屏时 AutoHide 会重新隐藏这个 App
            PrefMgr.addTempUnhiddenApp(packageName);
        }
        // 全局 UNHIDDEN 时不处理：锁屏时原有的全量 AutoHide 逻辑会覆盖
    }
}
