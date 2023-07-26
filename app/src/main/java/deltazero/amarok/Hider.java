package deltazero.amarok;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.MutableLiveData;

import java.nio.file.Paths;

import deltazero.amarok.AppHider.AppHiderBase;
import rikka.shizuku.ShizukuProvider;


public class Hider {

    private static final String TAG = "Hider";
    private static final HandlerThread hiderThread = new HandlerThread("HIDER_THREAD");
    private static final MutableLiveData<Boolean> isProcessing = new MutableLiveData<>(false);
    private final Context context;
    private final Handler backgroundHandler;
    public AppHiderBase appHider;
    public PrefMgr prefMgr;

    public MutableLiveData<Boolean> getIsProcessingLiveData() {
        return isProcessing;
    }

    public Hider(Context context) {
        this.context = context;
        prefMgr = new PrefMgr(context);

        // Init Background Handler
        if (hiderThread.getState() == Thread.State.NEW)
            hiderThread.start();
        backgroundHandler = new Handler(hiderThread.getLooper());

        // Enable shizukuProvider
        backgroundHandler.post(() -> ShizukuProvider.enableMultiProcessSupport(false));
    }

    public void hide() {
        backgroundHandler.post(() -> {
            isProcessing.postValue(true);
            syncHide();
            isProcessing.postValue(false);
            QuickHideService.stopService(context);
        });
    }

    public void unhide() {
        backgroundHandler.post(() -> {
            isProcessing.postValue(true);
            syncUnhide();
            isProcessing.postValue(false);
            QuickHideService.startService(context);
        });
    }

    private void syncHide() {

        appHider = prefMgr.getAppHider();

        try {

            // Hide apps
            try {
                appHider.hide(prefMgr.getHideApps());
            } catch (UnsupportedOperationException e) {
                Log.w(TAG, "Unable to hide app(s): ", e);
            }

            // Hide files
            for (String p : prefMgr.getHideFilePath()) {
                FileHider.process(Paths.get(p),
                        new FileHider.ProcessConfig(prefMgr, FileHider.ProcessConfig.ProcessMethod.HIDE));
            }

        } catch (InterruptedException e) {
            Log.w(TAG, "Process 'hide' interrupted.");
            return;
        }

        prefMgr.setIsHidden(true);

        Log.i(TAG, "Process 'hide' finished.");
        Toast.makeText(context, R.string.hidden_toast, Toast.LENGTH_SHORT).show();
    }

    private void syncUnhide() {

        appHider = prefMgr.getAppHider();

        try {

            // Unhide apps
            try {
                appHider.unhide(prefMgr.getHideApps());
            } catch (UnsupportedOperationException e) {
                Log.w(TAG, "Unable to hide app(s): ", e);
            }

            // Unhide files
            for (String p : prefMgr.getHideFilePath()) {
                FileHider.process(Paths.get(p),
                        new FileHider.ProcessConfig(prefMgr, FileHider.ProcessConfig.ProcessMethod.UNHIDE));
            }

        } catch (InterruptedException e) {
            Log.w(TAG, "Process 'unhide' interrupted.");
            return;
        }

        prefMgr.setIsHidden(false);

        Log.i(TAG, "Process 'unhide' finished.");
        Toast.makeText(context, R.string.unhidden_toast, Toast.LENGTH_SHORT).show();
    }

    public void forceUnhide() {
        if (Boolean.TRUE.equals(isProcessing.getValue())) {
            hiderThread.interrupt();
        }
        prefMgr.setIsHidden(true);
        unhide();
    }
}
