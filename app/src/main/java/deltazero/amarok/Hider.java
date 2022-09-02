package deltazero.amarok;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.Toast;

import java.nio.file.Paths;
import java.util.Set;

import deltazero.amarok.AppHider.AppHiderBase;
import deltazero.amarok.AppHider.RootAppHider;


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
        appHider = new RootAppHider();

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
            appHider.hide(hideApps);
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
            appHider.unhide(hideApps);
        } else {
            Log.i(TAG, "No hide App, skipped App unhiding.");
        }

        prefMgr.setIsHidden(false);

        Log.i(TAG, "Dusk to Dusk! Night has come!");
        Toast.makeText(context, "Hoooooooooo! Night falls!", Toast.LENGTH_SHORT).show();
    }


}
