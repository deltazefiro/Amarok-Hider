package deltazero.amarok;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

import deltazero.amarok.AppHider.AppHiderBase;
import deltazero.amarok.AppHider.NoneAppHider;
import deltazero.amarok.AppHider.RootAppHider;

public class PrefMgr {

    private final SharedPreferences mPrefs;
    private final SharedPreferences.Editor mPrefEditor;
    public Context context;

    public PrefMgr(Context context) {
        this.context = context;

        mPrefs = context.getSharedPreferences("deltazero.amarok.prefs", MODE_PRIVATE);
        mPrefEditor = mPrefs.edit();
    }


    public void setHideFilePath(Set<String> path) {
        mPrefEditor.putStringSet("hideFilePath", path);
        mPrefEditor.commit();
    }

    public Set<String> getHideFilePath() {
        return mPrefs.getStringSet("hideFilePath", new HashSet<>());
    }

    public boolean getIsHidden() {
        return mPrefs.getBoolean("isHidden", false);
    }

    public void setIsHidden(boolean isHidden) {
        mPrefEditor.putBoolean("isHidden", isHidden);
        mPrefEditor.commit();
    }

    public Set<String> getHideApps() {
        return mPrefs.getStringSet("hidePkgNames", new HashSet<>());
    }

    public void setHideApps(Set<String> pkgNames) {
        mPrefEditor.putStringSet("hidePkgNames", pkgNames);
        mPrefEditor.commit();
    }

    public boolean getEnableAnalytics() {
        return mPrefs.getBoolean("isEnableAnalytics", true);
    }

    public void setEnableAnalytics(boolean isEnable) {
        mPrefEditor.putBoolean("isEnableAnalytics", isEnable);
        mPrefEditor.commit();
    }

    public AppHiderBase getAppHiderMode() {
        switch (mPrefs.getInt("appHiderMode", 0)) {
            case 1:
                return new RootAppHider();
            default:
                return new NoneAppHider();
        }
    }

    public void setAppHiderMode(AppHiderBase mode) {
        int modeCode = 0;
        if (mode instanceof RootAppHider) {
            modeCode = 1;
        }
        mPrefEditor.putInt("appHiderMode", modeCode);
        mPrefEditor.commit();
    }
}
