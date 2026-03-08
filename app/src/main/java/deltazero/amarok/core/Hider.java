package deltazero.amarok.core;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import deltazero.amarok.QuickHideService;
import deltazero.amarok.R;
import deltazero.amarok.apphider.BaseAppHider;
import deltazero.amarok.filehider.BaseFileHider;
import deltazero.amarok.utils.SecurityUtil;


public final class Hider {

    private static final String TAG = "Hider";
    private static final HandlerThread hiderThread = new HandlerThread("HIDER_THREAD");
    private static final Handler threadHandler;

    public static boolean initialized = false;
    private static MutableLiveData<State> _state;
    public static LiveData<State> state;

    public enum State {
        HIDDEN,
        VISIBLE,
        PROCESSING
    }

    public interface OnActivationFailedListener {
        void onActivationFailed(int msgResID);
    }

    static {
        hiderThread.start();
        threadHandler = new Handler(hiderThread.getLooper());
    }

    /**
     * This method should be invoked in {@link deltazero.amarok.AmarokApplication#onCreate()}, after {@link PrefMgr#init(Context)}.
     * Do not invoke this method in static part or before {@link PrefMgr#init(Context)}.
     */
    public static void init() {
        assert PrefMgr.initialized;
        _state = new MutableLiveData<>(PrefMgr.getIsHidden() ? State.HIDDEN : State.VISIBLE);
        state = _state;
        _state.observeForever(s -> {
            if (s != State.PROCESSING)
                PrefMgr.setIsHidden(s == State.HIDDEN);
        });
        initialized = true;
    }

    /**
     * NOTE: Calling this method on a background thread
     * does not guarantee that the latest value set will be received.
     */
    public static State getState() {
        return _state.getValue();
    }

    public static void hide(Context context) {
        hide(context, null);
    }

    public static void hide(Context context, OnActivationFailedListener listener) {
        BaseAppHider.fromMode(context, PrefMgr.getAppHiderMode()).tryToActivate((appHiderClass, succeed, msg) -> {
            if (succeed) {
                processHide(context);
                return;
            }
            if (listener != null) {
                listener.onActivationFailed(msg);
            } else {
                showNoHiderToast(context, msg);
            }
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
            _state.postValue(State.PROCESSING);

            try {
                // Determine if we should only disable apps (skip hide step) when XHide is enabled
                boolean disableOnly = PrefMgr.isXHideEnabled() && PrefMgr.getDisableOnlyWithXHide();

                BaseAppHider.fromMode(context, PrefMgr.getAppHiderMode()).hide(PrefMgr.getHideApps(), disableOnly);
                BaseFileHider.fromMode(context, PrefMgr.getFileHiderMode()).hide(PrefMgr.getHideFilePath());
            } catch (InterruptedException e) {
                Log.w(TAG, "Process 'hide' interrupted.");
                return;
            }

            Log.i(TAG, "Process 'hide' finish.");
            _state.postValue(State.HIDDEN);

            if (!PrefMgr.getDisableToasts())
                Toast.makeText(context, R.string.hidden_toast, Toast.LENGTH_SHORT).show();

            QuickHideService.stopService(context);

        });
    }

    public static void unhide(Context context) {
        unhide(context, null);
    }

    public static void unhide(Context context, OnActivationFailedListener listener) {
        BaseAppHider.fromMode(context, PrefMgr.getAppHiderMode()).tryToActivate((appHiderClass, succeed, msg) -> {
            if (succeed) {
                processUnhide(context);
                return;
            }
            if (listener != null) {
                listener.onActivationFailed(msg);
            } else {
                showNoHiderToast(context, msg);
            }
        });
    }

    private static void processUnhide(Context context) {

        threadHandler.post(() -> {

            Log.i(TAG, "Process 'unhide' start.");
            _state.postValue(State.PROCESSING);

            try {
                BaseAppHider.fromMode(context, PrefMgr.getAppHiderMode()).unhide(PrefMgr.getHideApps());
                BaseFileHider.fromMode(context, PrefMgr.getFileHiderMode()).unhide(PrefMgr.getHideFilePath());
            } catch (InterruptedException e) {
                Log.w(TAG, "Process 'unhide' interrupted.");
                return;
            }

            Log.i(TAG, "Process 'unhide' finish.");
            _state.postValue(State.VISIBLE);

            if (!PrefMgr.getDisableToasts())
                Toast.makeText(context, R.string.unhidden_toast, Toast.LENGTH_SHORT).show();

            // Important: The startService() method of QuickHideService must be invoked on the main thread.
            // If it's called from a background thread, the service might not get the most recent value from Hider.getState() in time.
            // As a result, if the state changes into VISIBLE from HIDDEN just before, the service won't start.
            new Handler(Looper.getMainLooper()).post(
                    () -> QuickHideService.startService(context));
        });
    }

    public static void forceUnhide(Context context) {
        if (_state.getValue() == State.PROCESSING)
            hiderThread.interrupt();
        PrefMgr.setIsHidden(true);
        unhide(context);
    }

    private static void showNoHiderToast(Context context, int message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

}
