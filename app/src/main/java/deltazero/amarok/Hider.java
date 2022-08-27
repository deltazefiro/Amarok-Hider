package deltazero.amarok;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.Toast;

import java.nio.file.Paths;
import java.util.Set;



public class Hider {

    private static final String TAG = "Hider";
    private final Context context;
    private final Handler backgroundHandler;
    public AppHider appHider;
    public PrefMgr prefMgr;

    public interface HiderCallback {
        void onComplete();
    }

    public Hider(Context context) {
        this.context = context;
        prefMgr = new PrefMgr(context);
        appHider = new AppHider(new AppHider.RootMode());

        // Init Background Handler
        HandlerThread backgroundThread = new HandlerThread("HIDER_THREAD");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());

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

        // Hide files
        Set<String> hideFilePath = prefMgr.getHideFilePath();
        if (hideFilePath.size() > 0) {
            Log.i(TAG, "Hiding files ...");
            for (String p : hideFilePath) {
                FileHider.process(Paths.get(p), FileHider.ProcessMethod.ENCODE);
            }
        } else {
            Log.i(TAG, "No hide path, skipped file hiding.");
        }


        // Hide apps
        Set<String> hideApps = prefMgr.getHideApps();
        if (hideApps.size() > 0) {
            if (appHider.checkAvailability()) {
                for (String a : hideApps) {
                    appHider.hide(a);
                }
            } else {
                Log.w(TAG, "Unable to hide App: Hider not available");
                Toast.makeText(context, "App hide not available", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.i(TAG, "No hide App, skipped App hiding.");
        }

        prefMgr.setIsHidden(true);

        Log.i(TAG, "Dusk to Dawn! Goodmorning ~");
        Toast.makeText(context, "Cock-a-doodle-do~ Morning!", Toast.LENGTH_SHORT).show();
    }

    public void syncUnhide() {

        // Unhide files
        Set<String> hideFilePath = prefMgr.getHideFilePath();
        if (hideFilePath.size() > 0) {
            Log.i(TAG, "Unhiding files ...");
            for (String p : hideFilePath) {
                FileHider.process(Paths.get(p), FileHider.ProcessMethod.DECODE);
            }
        } else {
            Log.i(TAG, "No hide path, skipped file unhiding.");
        }


        // Unhide apps
        Set<String> hideApps = prefMgr.getHideApps();
        if (hideApps.size() > 0) {
            if (appHider.checkAvailability()) {
                for (String a : hideApps) {
                    appHider.unhide(a);
                }
            } else {
                Log.w(TAG, "Unable to unhide App: Hider not available");
                Toast.makeText(context, "App hide not available", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.i(TAG, "No hide App, skipped App unhiding.");
        }

        prefMgr.setIsHidden(false);

        Log.i(TAG, "Dusk to Dusk! Night has come!");
        Toast.makeText(context, "Hoooooooooo! Night falls!", Toast.LENGTH_SHORT).show();
    }


}
