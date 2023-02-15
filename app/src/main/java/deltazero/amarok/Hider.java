package deltazero.amarok;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.MutableLiveData;

import java.nio.file.Paths;
import java.util.Set;

import deltazero.amarok.AppHider.AppHiderBase;
import deltazero.amarok.AppHider.NoneAppHider;
import rikka.shizuku.ShizukuProvider;


public class Hider {

    private static final String TAG = "Hider";
    private static final HandlerThread backgroundThread = new HandlerThread("HIDER_THREAD");
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
        if (backgroundThread.getState() == Thread.State.NEW)
            backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());

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

    public void syncHide() {

        appHider = prefMgr.getAppHider();

        // Hide apps
        Set<String> hideApps = prefMgr.getHideApps();
        if (hideApps.size() > 0) {
            if (!(appHider instanceof NoneAppHider)) {
                appHider.hide(hideApps);
            } else {
                Log.w(TAG, "No AppHider selected, skipped App hiding.");
            }
        } else {
            Log.i(TAG, "No hide App, skipped App hiding.");
        }

        // Hide files
        Set<String> hideFilePath = prefMgr.getHideFilePath();
        if (hideFilePath.size() > 0) {
            Log.i(TAG, "Hiding files ...");
            for (String p : hideFilePath) {
                FileHider.process(Paths.get(p), new FileHider.ProcessConfig(prefMgr));
            }
        } else {
            Log.i(TAG, "No hide path, skipped file hiding.");
        }

        prefMgr.setIsHidden(true);

        Log.i(TAG, "Hid. Dusk to Dawn! Goodmorning ~");
        Toast.makeText(context, R.string.hidden_toast, Toast.LENGTH_SHORT).show();
    }

    public void syncUnhide() {

        appHider = prefMgr.getAppHider();

        // Unhide apps
        Set<String> hideApps = prefMgr.getHideApps();
        if (hideApps.size() > 0) {
            if (!(appHider instanceof NoneAppHider)) {
                appHider.unhide(hideApps);
            } else {
                Log.w(TAG, "No AppHider selected, skipped App unhiding.");
            }
        } else {
            Log.i(TAG, "No hide App, skipped App unhiding.");
        }

        // Unhide files
        Set<String> hideFilePath = prefMgr.getHideFilePath();
        if (hideFilePath.size() > 0) {
            Log.i(TAG, "Unhiding files ...");
            for (String p : hideFilePath) {
                FileHider.process(Paths.get(p), new FileHider.ProcessConfig(prefMgr));
            }
        } else {
            Log.i(TAG, "No hide path, skipped file unhiding.");
        }

        prefMgr.setIsHidden(false);

        Log.i(TAG, "Dusk to Dusk! Night has come!");
        Toast.makeText(context, R.string.unhidden_toast, Toast.LENGTH_SHORT).show();
    }


}
