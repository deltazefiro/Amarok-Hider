package deltazero.amarok;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

import deltazero.amarok.AppHider.BaseAppHider;
import deltazero.amarok.AppHider.DhizukuAppHider;
import deltazero.amarok.AppHider.DsmAppHider;
import deltazero.amarok.AppHider.NoneAppHider;
import deltazero.amarok.AppHider.RootAppHider;
import deltazero.amarok.AppHider.ShizukuAppHider;
import deltazero.amarok.FileHider.ChmodFileHider;
import deltazero.amarok.FileHider.BaseFileHider;
import deltazero.amarok.FileHider.NoMediaFileHider;
import deltazero.amarok.FileHider.ObfuscateFileHider;

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

    public BaseAppHider getAppHider() {
        return switch (mPrefs.getInt("appHiderMode", 0)) {
            case 0 -> new NoneAppHider(context);
            case 1 -> new RootAppHider(context);
            case 2 -> new DsmAppHider(context);
            case 3 -> new ShizukuAppHider(context);
            case 4 -> new DhizukuAppHider(context);
            default -> throw new IndexOutOfBoundsException("Should not reach here");
        };
    }

    public void setAppHiderMode(Class<? extends BaseAppHider> mode) {
        int modeCode;
        if (mode == NoneAppHider.class)
            modeCode = 0;
        else if (mode == RootAppHider.class)
            modeCode = 1;
        else if (mode == DsmAppHider.class)
            modeCode = 2;
        else if (mode == ShizukuAppHider.class)
            modeCode = 3;
        else if (mode == DhizukuAppHider.class)
            modeCode = 4;
        else
            throw new IndexOutOfBoundsException("Should not reach here");
        mPrefEditor.putInt("appHiderMode", modeCode);
        mPrefEditor.apply();
    }

    public BaseFileHider getFileHider() {
        return switch (mPrefs.getInt("fileHiderMode", 1)) {
            case 1 -> new ObfuscateFileHider(context);
            case 2 -> new NoMediaFileHider(context);
            case 3 -> new ChmodFileHider(context);
            default -> throw new IndexOutOfBoundsException("Should not reach here");
        };
    }

    public void setFileHiderMode(Class<? extends BaseFileHider> mode) {
        int modeCode;
        if (mode == ObfuscateFileHider.class)
            modeCode = 1;
        else if (mode == NoMediaFileHider.class)
            modeCode = 2;
        else if (mode == ChmodFileHider.class)
            modeCode = 3;
        else
            throw new IndexOutOfBoundsException("Should not reach here");
        mPrefEditor.putInt("fileHiderMode", modeCode);
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

    @Nullable
    public String getAmarokPassword() {
        return mPrefs.getString("amarokPassword", null);
    }

    public void setAmarokPassword(String password) {
        mPrefEditor.putString("amarokPassword", password);
        mPrefEditor.apply();
    }

    public boolean getEnableAmarokBiometricAuth() {
        return mPrefs.getBoolean("enableAmarokBiometricAuth", false);
    }

    public void setEnableAmarokBiometricAuth(boolean enableAmarokBiometricAuth) {
        mPrefEditor.putBoolean("enableAmarokBiometricAuth", enableAmarokBiometricAuth);
        mPrefEditor.apply();
    }

    public boolean getEnableDynamicColor() {
        return mPrefs.getBoolean("enableDynamicColor", false);
    }

    public void setEnableDynamicColor(boolean enableDynamicColor) {
        mPrefEditor.putBoolean("enableDynamicColor", enableDynamicColor);
        mPrefEditor.apply();
    }

    public boolean getEnableDisguise() {
        return mPrefs.getBoolean("enableDisguise", false);
    }

    public void setEnableDisguise(boolean enableDisguise) {
        mPrefEditor.putBoolean("enableDisguise", enableDisguise);
        mPrefEditor.apply();
    }

    public boolean getDoShowQuitDisguiseInstuct() {
        return mPrefs.getBoolean("doShowQuitDisguiseInstuct", true);
    }

    public void setDoShowQuitDisguiseInstuct(boolean doShowQuitDisguiseInstuct) {
        mPrefEditor.putBoolean("doShowQuitDisguiseInstuct", doShowQuitDisguiseInstuct);
        mPrefEditor.apply();
    }
}
