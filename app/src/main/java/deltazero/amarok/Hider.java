package deltazero.amarok;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.Toast;

import com.catchingnow.icebox.sdk_client.IceBox;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;
import static com.catchingnow.icebox.sdk_client.IceBox.SilentInstallSupport.SUPPORTED;

public class Hider {

    public static final String TAG = "Hider";

    private final Context context;
    private final Handler mBackgroundHandler;
    private final SharedPreferences mPrefs;
    private final SharedPreferences.Editor mPrefEditor;

    public Path encodePath;
    public String[] hidePkgNames;

    @SuppressLint("CommitPrefEdits")
    public Hider(Context context) {
        this.context = context;
        HandlerThread backgroundThread = new HandlerThread("BACKGROUND_THREAD");
        backgroundThread.start();
        mBackgroundHandler = new Handler(backgroundThread.getLooper());

        mPrefs = context.getSharedPreferences("deltazero.amarok.prefs", MODE_PRIVATE);
        mPrefEditor = mPrefs.edit();
    }

    public void dawn() {

        String encodePathStr = mPrefs.getString("encodePath", null);
        if (encodePathStr != null) {
            encodePath = Paths.get(encodePathStr);
            FileProcessor.process(encodePath, FileProcessor.ProcessMethod.ENCODE);
        }

        Set<String> hidePkgNamesArr = getHidePkgNames();
        if (IceBox.querySupportSilentInstall(context) == SUPPORTED && hidePkgNamesArr != null) {
            hidePkgNames = hidePkgNamesArr.toArray(new String[0]);
            mBackgroundHandler.post(() -> {
                IceBox.setAppEnabledSettings(context, false, hidePkgNames);
            });
        }

        setIsNight(false);

        Log.i(TAG, "Dusk to Dawn! Goodmorning ~");
        Toast.makeText(context, "Cock-a-doodle-do~ Morning!", Toast.LENGTH_SHORT).show();
    }

    public void dusk() {
        String encodePathStr = mPrefs.getString("encodePath", null);
        if (encodePathStr != null) {
            encodePath = Paths.get(encodePathStr);
            FileProcessor.process(encodePath, FileProcessor.ProcessMethod.DECODE);
        }

        Set<String> hidePkgNamesArr = getHidePkgNames();
        if (IceBox.querySupportSilentInstall(context) == SUPPORTED && hidePkgNamesArr != null){
            hidePkgNames = hidePkgNamesArr.toArray(new String[0]);
            mBackgroundHandler.post(() -> {
                IceBox.setAppEnabledSettings(context, true, hidePkgNames);
            });
        }

        setIsNight(true);

        Log.i(TAG, "Dusk to Dusk! Night has come!");
        Toast.makeText(context, "Hoooooooooo! Night falls!", Toast.LENGTH_SHORT).show();
    }

    public void setEncodePath(String path) {
        mPrefEditor.putString("encodePath", path);
        mPrefEditor.commit();
    }

    public boolean getIsNight() {
        return mPrefs.getBoolean("isNight", true);
    }

    public void setIsNight(boolean isNight) {
        mPrefEditor.putBoolean("isNight", isNight);
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
        }
    }


}
