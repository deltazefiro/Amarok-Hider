package deltazero.amarok;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.MutableLiveData;


public final class Hider {

    private static final String TAG = "Hider";
    private static final HandlerThread hiderThread = new HandlerThread("HIDER_THREAD");
    private static final Handler threadHandler;

    public static boolean initialized = false;
    public static MutableLiveData<State> state;

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
     * This method should be invoked in {@link AmarokApp#onCreate()}, after {@link PrefMgr#init(Context)}.
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

    public static State getState() {
        return state.getValue();
    }

    public static void hide(Context context) {

        threadHandler.post(() -> {

            Log.i(TAG, "Process 'hide' start.");
            state.postValue(State.PROCESSING);

            try {
                PrefMgr.getAppHider(context).hide(PrefMgr.getHideApps());
                PrefMgr.getFileHider(context).hide(PrefMgr.getHideFilePath());
            } catch (InterruptedException e) {
                Log.w(TAG, "Process 'hide' interrupted.");
                return;
            }

            Log.i(TAG, "Process 'hide' finish.");
            state.postValue(State.HIDDEN);

            Toast.makeText(context, R.string.hidden_toast, Toast.LENGTH_SHORT).show();

            QuickHideService.stopService(context);

        });
    }

    public static void unhide(Context context) {

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

            Toast.makeText(context, R.string.unhidden_toast, Toast.LENGTH_SHORT).show();

            QuickHideService.startService(context);

        });
    }

    public static void forceUnhide(Context context) {
        if (state.getValue() == State.PROCESSING)
            hiderThread.interrupt();
        PrefMgr.setIsHidden(true);
        unhide(context);
    }

}
