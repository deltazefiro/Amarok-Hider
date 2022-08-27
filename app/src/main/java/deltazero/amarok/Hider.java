package deltazero.amarok;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.nio.file.Paths;
import java.util.Set;

public class Hider {

    public static final String TAG = "Hider";
    public AppHider appHider;
    public Context context;
    public PrefMgr prefMgr;


    public Hider(Context context) {
        this.context = context;
        prefMgr = new PrefMgr(context);
        appHider = new AppHider(new AppHider.RootMode());
    }

    public void hide() {

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

    public void unhide() {

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
