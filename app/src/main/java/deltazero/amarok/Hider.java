package deltazero.amarok;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.MutableLiveData;

import java.util.Collections;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import deltazero.amarok.ui.settings.SwitchAppHiderActivity;
import deltazero.amarok.utils.SecurityUtil;


public final class Hider {

    private static final String TAG = "Hider";
    private static final HandlerThread hiderThread = new HandlerThread("HIDER_THREAD");
    private static final Handler threadHandler;

    public static boolean initialized = false;
    public static MutableLiveData<State> state;
    /** 单应用取消隐藏完成事件，value 为被取消隐藏的包名。供 ShortcutLaunchActivity 监听。 */
    public static final MutableLiveData<String> singleUnhideEvent = new MutableLiveData<>();

    public enum State {
        HIDDEN,
        VISIBLE,
        PROCESSING
    }

    static {
        hiderThread.start();
        threadHandler = new Handler(hiderThread.getLooper());
    }

    /**
     * This method should be invoked in {@link AmarokApplication#onCreate()}, after {@link PrefMgr#init(Context)}.
     * Do not invoke this method in static part or before {@link PrefMgr#init(Context)}.
     */
    public static void init() {
        assert PrefMgr.initialized;
        state = new MutableLiveData<>(PrefMgr.getIsHidden() ? State.HIDDEN : State.VISIBLE);
        state.observeForever(state -> {
            if (state != State.PROCESSING)
                PrefMgr.setIsHidden(state == State.HIDDEN);
        });
        initialized = true;
    }

    /**
     * NOTE: Calling this method on a background thread
     * does not guarantee that the latest value set will be received.
     */
    public static State getState() {
        return state.getValue();
    }

    public static void hide(Context context) {
        PrefMgr.getAppHider(context).tryToActivate((appHiderClass, succeed, msg) -> {
            if (succeed) {
                processHide(context);
                return;
            }
            if (context instanceof Activity)
                showNoHiderDialog(context, msg);
            else
                showNoHiderToast(context, msg);
        });

        // Avoid password or disguise right after hide
        if (PrefMgr.getDisableSecurityWhenUnhidden()) {
            SecurityUtil.unlock();
            SecurityUtil.dismissDisguise();
        }
    }

    private static void processHide(Context context) {

        threadHandler.post(() -> {

            Log.i(TAG, "Process 'hide' start.");
            state.postValue(State.PROCESSING);

            try {
                // Determine if we should only disable apps (skip hide step) when XHide is enabled
                boolean disableOnly = PrefMgr.isXHideEnabled() && PrefMgr.getDisableOnlyWithXHide();

                PrefMgr.getAppHider(context).hide(PrefMgr.getHideApps(), disableOnly);
                PrefMgr.getFileHider(context).hide(PrefMgr.getHideFilePath());
            } catch (InterruptedException e) {
                Log.w(TAG, "Process 'hide' interrupted.");
                return;
            }

            Log.i(TAG, "Process 'hide' finish.");
            state.postValue(State.HIDDEN);

            if (!PrefMgr.getDisableToasts())
                Toast.makeText(context, R.string.hidden_toast, Toast.LENGTH_SHORT).show();

            QuickHideService.stopService(context);

        });
    }

    public static void unhide(Context context) {
        PrefMgr.getAppHider(context).tryToActivate((appHiderClass, succeed, msg) -> {
            if (succeed) {
                processUnhide(context);
                return;
            }
            if (context instanceof Activity)
                showNoHiderDialog(context, msg);
            else
                showNoHiderToast(context, msg);
        });
    }

    private static void processUnhide(Context context) {

        threadHandler.post(() -> {

            Log.i(TAG, "Process 'unhide' start.");
            state.postValue(State.PROCESSING);

            try {
                PrefMgr.getAppHider(context).unhide(PrefMgr.getHideApps());
                PrefMgr.getFileHider(context).unhide(PrefMgr.getHideFilePath());
            } catch (InterruptedException e) {
                Log.w(TAG, "Process 'unhide' interrupted.");
                return;
            }

            Log.i(TAG, "Process 'unhide' finish.");
            state.postValue(State.VISIBLE);

            if (!PrefMgr.getDisableToasts())
                Toast.makeText(context, R.string.unhidden_toast, Toast.LENGTH_SHORT).show();

            // Important: The startService() method of QuickHideService must be invoked on the main thread.
            // If it's called from a background thread, the service might not get the most recent value from Hider.getState() in time.
            // As a result, if the state changes into VISIBLE from HIDDEN just before, the service won't start.
            new Handler(Looper.getMainLooper()).post(
                    () -> QuickHideService.startService(context));
        });
    }

    /**
     * 仅取消隐藏指定的单个应用，不影响其他隐藏应用，也不改变全局 Hider.State。
     * 完成后通过 {@link #singleUnhideEvent} 发出通知，并将包名记入 tempUnhiddenApps。
     * 用于快捷方式启动、应用更新等单应用维度的场景。
     */
    public static void unhideOne(Context context, String packageName) {
        PrefMgr.getAppHider(context).tryToActivate((appHiderClass, succeed, msg) -> {
            if (succeed) {
                processUnhideOne(context, packageName);
                return;
            }
            showNoHiderToast(context, msg);
        });
    }

    private static void processUnhideOne(Context context, String packageName) {
        threadHandler.post(() -> {
            Log.i(TAG, "Process 'unhideOne' start: " + packageName);
            try {
                PrefMgr.getAppHider(context).unhide(Collections.singleton(packageName));
            } catch (InterruptedException e) {
                Log.w(TAG, "Process 'unhideOne' interrupted.");
                return;
            }
            Log.i(TAG, "Process 'unhideOne' finish: " + packageName);

            // 只在全局隐藏中时才记录临时状态
            // 全局 UNHIDDEN 时，锁屏后的全量 AutoHide 会覆盖，不需要单独记录
            if (state.getValue() == State.HIDDEN) {
                PrefMgr.addTempUnhiddenApp(packageName);
            }

            singleUnhideEvent.postValue(packageName);

            if (!PrefMgr.getDisableToasts())
                Toast.makeText(context, R.string.unhidden_toast, Toast.LENGTH_SHORT).show();
        });
    }

    public static void forceUnhide(Context context) {
        if (state.getValue() == State.PROCESSING)
            hiderThread.interrupt();
        PrefMgr.setIsHidden(true);
        unhide(context);
    }

    public static void showNoHiderDialog(Context context, int message) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.apphider_not_ava_title)
                .setMessage(message)
                .setPositiveButton(R.string.switch_app_hider, (dialog, which)
                        -> context.startActivity(new Intent(context, SwitchAppHiderActivity.class)))
                .setNegativeButton(context.getString(R.string.ok), null)
                .show();
    }

    private static void showNoHiderToast(Context context, int message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

}
