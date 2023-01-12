package deltazero.amarok;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.Toast;

import java.nio.file.Paths;
import java.util.Set;

import deltazero.amarok.AppHider.AppHiderBase;
import deltazero.amarok.AppHider.NoneAppHider;
import rikka.shizuku.ShizukuProvider;


public class Hider {

    private static final String TAG = "Hider";
    private final Context context;
    private final Handler backgroundHandler;
    public AppHiderBase appHider;
    public PrefMgr prefMgr;

    public interface HiderCallback {
        void onComplete();
    }

    public Hider(Context context) {
        this.context = context;
        prefMgr = new PrefMgr(context);

        // Init Background Handler
        HandlerThread backgroundThread = new HandlerThread("HIDER_THREAD");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());

        // Enable shizukuProvider
        backgroundHandler.post(() -> ShizukuProvider.enableMultiProcessSupport(false));
    }

    public void Hide(HiderCallback callback) {
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                syncHide();
                callback.onComplete();
            }
        });
    }

    public void Unhide(HiderCallback callback) {
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                syncUnhide();
                callback.onComplete();
            }
        });
    }

    public void syncHide() {

        appHider = prefMgr.getAppHider();

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

        prefMgr.setIsHidden(true);

        Log.i(TAG, "Hid. Dusk to Dawn! Goodmorning ~");
        Toast.makeText(context, R.string.hidden_toast, Toast.LENGTH_SHORT).show();
    }

    public void syncUnhide() {

        appHider = prefMgr.getAppHider();

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

        prefMgr.setIsHidden(false);

        Log.i(TAG, "Dusk to Dusk! Night has come!");
        Toast.makeText(context, R.string.unhidden_toast, Toast.LENGTH_SHORT).show();
    }


}
