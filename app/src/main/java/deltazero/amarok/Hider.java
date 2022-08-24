package deltazero.amarok;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public class Hider {

    public static final String TAG = "Hider";
    //    private final Handler mBackgroundHandler;
    private final SharedPreferences mPrefs;
    private final SharedPreferences.Editor mPrefEditor;
    public AppHider appHider;
    public Context context;
    public Path encodePath;
    public String[] hidePkgNames;


    public Hider(Context context) {
        this.context = context;
//        HandlerThread backgroundThread = new HandlerThread("BACKGROUND_THREAD");
//        backgroundThread.start();
//        mBackgroundHandler = new Handler(backgroundThread.getLooper());

        mPrefs = context.getSharedPreferences("deltazero.amarok.prefs", MODE_PRIVATE);
        mPrefEditor = mPrefs.edit();
        appHider = new AppHider(new AppHider.RootMode());
    }

    public void hide() {

        // Encode files
        String encodePathStr = mPrefs.getString("encodePath", null);
        if (encodePathStr != null) {
            encodePath = Paths.get(encodePathStr);
            FileHider.process(encodePath, FileHider.ProcessMethod.ENCODE);
        } else {
            Log.d(TAG, "No encode path, skipped file encoding.");
        }

        // Hide apps
        Set<String> pkgNames = getHidePkgNames();
        if (pkgNames != null) {
            if (appHider.isAvailable) {
                appHider.hide(pkgNames);
            } else {
                Toast.makeText(context, "App hide not available", Toast.LENGTH_SHORT).show();
            }
        }

        setIsHidden(false);

        Log.i(TAG, "Dusk to Dawn! Goodmorning ~");
        Toast.makeText(context, "Cock-a-doodle-do~ Morning!", Toast.LENGTH_SHORT).show();
    }

    public void unhide() {

        // Decode files
        String encodePathStr = mPrefs.getString("encodePath", null);
        if (encodePathStr != null) {
            encodePath = Paths.get(encodePathStr);
            FileHider.process(encodePath, FileHider.ProcessMethod.DECODE);
        } else {
            Log.d(TAG, "No encode path, skipped file decoding.");
        }

        // Enable apps
        Set<String> pkgNames = getHidePkgNames();
        if (pkgNames != null) {
            if (appHider.isAvailable) {
                appHider.unhide(pkgNames);
            } else {
                Toast.makeText(context, "App hide not available", Toast.LENGTH_SHORT).show();
            }
        }

        setIsHidden(true);

        Log.i(TAG, "Dusk to Dusk! Night has come!");
        Toast.makeText(context, "Hoooooooooo! Night falls!", Toast.LENGTH_SHORT).show();
    }

    public void setEncodePath(String path) {
        mPrefEditor.putString("encodePath", path);
        mPrefEditor.commit();
    }

    public boolean getIsHidden() {
        return mPrefs.getBoolean("isHidden", true);
    }

    public void setIsHidden(boolean isHidden) {
        mPrefEditor.putBoolean("isHidden", isHidden);
        mPrefEditor.commit();
    }

    public Set<String> getHidePkgNames() {
        return mPrefs.getStringSet("hidePkgNames", null);
    }

    public void setHidePkgNames(Set<String> pkgNames) {
        if (pkgNames == null || pkgNames.isEmpty()) {
            mPrefEditor.remove("hidePkgNames");
            mPrefEditor.commit();
        } else {
            mPrefEditor.putStringSet("hidePkgNames", pkgNames);
            mPrefEditor.commit();
            Log.i(TAG, "Set Hide App: " + pkgNames.toString());
        }
    }


}
