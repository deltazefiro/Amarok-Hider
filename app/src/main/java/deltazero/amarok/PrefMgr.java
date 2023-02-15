package deltazero.amarok;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

import deltazero.amarok.AppHider.AppHiderBase;
import deltazero.amarok.AppHider.DsmAppHider;
import deltazero.amarok.AppHider.NoneAppHider;
import deltazero.amarok.AppHider.RootAppHider;
import deltazero.amarok.AppHider.ShizukuHider;

public class PrefMgr {

    private final SharedPreferences mPrefs;
    private final SharedPreferences.Editor mPrefEditor;
    public Context context;

    public PrefMgr(Context context) {
        this.context = context;

        mPrefs = context.getSharedPreferences("deltazero.amarok.prefs", MODE_PRIVATE);
        mPrefEditor = mPrefs.edit();
    }


    public Set<String> getHideFilePath() {
        return mPrefs.getStringSet("hideFilePath", new HashSet<>());
    }

    public void setHideFilePath(Set<String> path) {
        mPrefEditor.putStringSet("hideFilePath", path);
        mPrefEditor.apply();
    }

    public boolean getIsHidden() {
        return mPrefs.getBoolean("isHidden", false);
    }

    public void setIsHidden(boolean isHidden) {
        mPrefEditor.putBoolean("isHidden", isHidden);
        mPrefEditor.apply();
    }

    public Set<String> getHideApps() {
        return mPrefs.getStringSet("hidePkgNames", new HashSet<>());
    }

    public void setHideApps(Set<String> pkgNames) {
        mPrefEditor.putStringSet("hidePkgNames", pkgNames);
        mPrefEditor.apply();
    }

    public AppHiderBase getAppHider() {
        switch (mPrefs.getInt("appHiderMode", 0)) {
            case 1:
                return new RootAppHider(context);
            case 2:
                return new DsmAppHider(context);
            case 3:
                return new ShizukuHider(context);
            default:
                return new NoneAppHider(context);
        }
    }

    public void setAppHiderMode(Class<? extends AppHiderBase> mode) {
        int modeCode = 0;
        if (mode == RootAppHider.class)
            modeCode = 1;
        if (mode == DsmAppHider.class)
            modeCode = 2;
        if (mode == ShizukuHider.class)
            modeCode = 3;
        mPrefEditor.putInt("appHiderMode", modeCode);
        mPrefEditor.apply();
    }

    public void setAppHiderMode(int modeCode) {
        mPrefEditor.putInt("appHiderMode", modeCode);
        mPrefEditor.apply();
    }

    public int getAppHiderCode() {
        return mPrefs.getInt("appHiderMode", 0);
    }

    public boolean getEnableAutoUpdate() {
        return mPrefs.getBoolean("isEnableAutoUpdate", true);
    }

    public void setEnableAutoUpdate(boolean isEnable) {
        mPrefEditor.putBoolean("isEnableAutoUpdate", isEnable);
        mPrefEditor.apply();
    }

    @Deprecated
    public boolean getLegacyEnableObfuscateFileHeader() {
        return mPrefs.getBoolean("enableCorruptFileHeader", false);
    }

    public boolean getEnableObfuscateFileHeader() {
        return mPrefs.getBoolean("enableObfuscateFileHeader", false);
    }

    public void setEnableObfuscateFileHeader(boolean ifObfuscateFileHeader) {
        mPrefEditor.putBoolean("enableObfuscateFileHeader", ifObfuscateFileHeader);
        mPrefEditor.apply();
    }

    public boolean getEnableObfuscateTextFile() {
        return mPrefs.getBoolean("enableObfuscateTextFile", false);
    }

    public void setEnableObfuscateTextFile(boolean ifObfuscateTextFile) {
        mPrefEditor.putBoolean("enableObfuscateTextFile", ifObfuscateTextFile);
        mPrefEditor.apply();
    }

    public boolean getEnableObfuscateTextFileEnhanced() {
        return mPrefs.getBoolean("enableObfuscateTextFileEnhanced", false);
    }

    public void setEnableObfuscateTextFileEnhanced(boolean ifObfuscateTextFileEnhanced) {
        mPrefEditor.putBoolean("enableObfuscateTextFileEnhanced", ifObfuscateTextFileEnhanced);
        mPrefEditor.apply();
    }

    public boolean getEnableQuickHideService() {
        return mPrefs.getBoolean("enableQuickHideService", false);
    }

    public void setEnableQuickHideService(boolean isEnableQuickHideService) {
        mPrefEditor.putBoolean("enableQuickHideService", isEnableQuickHideService);
        mPrefEditor.apply();
    }

    public boolean getEnablePanicButton() {
        return mPrefs.getBoolean("enablePanicButton", false);
    }

    public void setEnablePanicButton(boolean isEnablePanicButton) {
        mPrefEditor.putBoolean("enablePanicButton", isEnablePanicButton);
        mPrefEditor.apply();
    }
}
