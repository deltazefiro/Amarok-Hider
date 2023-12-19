package deltazero.amarok;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

import deltazero.amarok.apphider.BaseAppHider;
import deltazero.amarok.apphider.DhizukuAppHider;
import deltazero.amarok.apphider.DsmAppHider;
import deltazero.amarok.apphider.NoneAppHider;
import deltazero.amarok.apphider.RootAppHider;
import deltazero.amarok.apphider.ShizukuAppHider;
import deltazero.amarok.filehider.BaseFileHider;
import deltazero.amarok.filehider.ChmodFileHider;
import deltazero.amarok.filehider.NoMediaFileHider;
import deltazero.amarok.filehider.NoneFileHider;
import deltazero.amarok.filehider.ObfuscateFileHider;

public final class PrefMgr {

    private static SharedPreferences mPrefs;
    private static SharedPreferences.Editor mPrefEditor;
    public static boolean initialized = false;

    /**
     * This method should be invoked in {@link AmarokApplication#onCreate()}.
     *
     * @param context Application context
     */
    public static void init(Context context) {
        mPrefs = context.getSharedPreferences("deltazero.amarok.prefs", MODE_PRIVATE);
        mPrefEditor = mPrefs.edit();
        initialized = true;
    }

    public static Set<String> getHideFilePath() {
        return mPrefs.getStringSet("hideFilePath", new HashSet<>());
    }

    public static void setHideFilePath(Set<String> path) {
        mPrefEditor.putStringSet("hideFilePath", path);
        mPrefEditor.apply();
    }

    /**
     * Avoid using this method except for initializing {@link Hider}.
     * Use {@link Hider#getState()} instead.
     */
    public static boolean getIsHidden() {
        return mPrefs.getBoolean("isHidden", false);
    }

    public static void setIsHidden(boolean isHidden) {
        mPrefEditor.putBoolean("isHidden", isHidden);
        mPrefEditor.apply();
    }

    public static Set<String> getHideApps() {
        return mPrefs.getStringSet("hidePkgNames", new HashSet<>());
    }

    public static void setHideApps(Set<String> pkgNames) {
        mPrefEditor.putStringSet("hidePkgNames", pkgNames);
        mPrefEditor.apply();
    }

    public static BaseAppHider getAppHider(Context context) {
        return switch (mPrefs.getInt("appHiderMode", 0)) {
            case 0 -> new NoneAppHider(context);
            case 1 -> new RootAppHider(context);
            case 2 -> new DsmAppHider(context);
            case 3 -> new ShizukuAppHider(context);
            case 4 -> new DhizukuAppHider(context);
            default -> throw new IndexOutOfBoundsException("Should not reach here");
        };
    }

    public static void setAppHiderMode(Class<? extends BaseAppHider> mode) {
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

    public static BaseFileHider getFileHider(Context context) {
        return switch (mPrefs.getInt("fileHiderMode", 1)) {
            case 0 -> new NoneFileHider(context);
            case 1 -> new ObfuscateFileHider(context);
            case 2 -> new NoMediaFileHider(context);
            case 3 -> new ChmodFileHider(context);
            default -> throw new IndexOutOfBoundsException("Should not reach here");
        };
    }

    public static void setFileHiderMode(Class<? extends BaseFileHider> mode) {
        int modeCode;
        if (mode == NoneFileHider.class)
            modeCode = 0;
        else if (mode == ObfuscateFileHider.class)
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

    public static int getAppHiderCode() {
        return mPrefs.getInt("appHiderMode", 0);
    }

    public static boolean getEnableAutoUpdate() {
        return mPrefs.getBoolean("isEnableAutoUpdate", true);
    }

    public static void setEnableAutoUpdate(boolean isEnable) {
        mPrefEditor.putBoolean("isEnableAutoUpdate", isEnable);
        mPrefEditor.apply();
    }

    public static boolean getEnableObfuscateFileHeader() {
        return mPrefs.getBoolean("enableObfuscateFileHeader", false);
    }

    public static void setEnableObfuscateFileHeader(boolean ifObfuscateFileHeader) {
        mPrefEditor.putBoolean("enableObfuscateFileHeader", ifObfuscateFileHeader);
        mPrefEditor.apply();
    }

    public static boolean getEnableObfuscateTextFile() {
        return mPrefs.getBoolean("enableObfuscateTextFile", false);
    }

    public static void setEnableObfuscateTextFile(boolean ifObfuscateTextFile) {
        mPrefEditor.putBoolean("enableObfuscateTextFile", ifObfuscateTextFile);
        mPrefEditor.apply();
    }

    public static boolean getEnableObfuscateTextFileEnhanced() {
        return mPrefs.getBoolean("enableObfuscateTextFileEnhanced", false);
    }

    public static void setEnableObfuscateTextFileEnhanced(boolean ifObfuscateTextFileEnhanced) {
        mPrefEditor.putBoolean("enableObfuscateTextFileEnhanced", ifObfuscateTextFileEnhanced);
        mPrefEditor.apply();
    }

    public static boolean getEnableQuickHideService() {
        return mPrefs.getBoolean("enableQuickHideService", false);
    }

    public static void setEnableQuickHideService(boolean isEnableQuickHideService) {
        mPrefEditor.putBoolean("enableQuickHideService", isEnableQuickHideService);
        mPrefEditor.apply();
    }

    public static boolean getEnablePanicButton() {
        return mPrefs.getBoolean("enablePanicButton", false);
    }

    public static void setEnablePanicButton(boolean isEnablePanicButton) {
        mPrefEditor.putBoolean("enablePanicButton", isEnablePanicButton);
        mPrefEditor.apply();
    }

    @Nullable
    public static String getAmarokPassword() {
        return mPrefs.getString("amarokPassword", null);
    }

    public static void setAmarokPassword(String password) {
        mPrefEditor.putString("amarokPassword", password);
        mPrefEditor.apply();
    }

    public static boolean getEnableAmarokBiometricAuth() {
        return mPrefs.getBoolean("enableAmarokBiometricAuth", false);
    }

    public static void setEnableAmarokBiometricAuth(boolean enableAmarokBiometricAuth) {
        mPrefEditor.putBoolean("enableAmarokBiometricAuth", enableAmarokBiometricAuth);
        mPrefEditor.apply();
    }

    public static boolean getEnableDynamicColor() {
        return mPrefs.getBoolean("enableDynamicColor",
                (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU));
    }

    public static void setEnableDynamicColor(boolean enableDynamicColor) {
        mPrefEditor.putBoolean("enableDynamicColor", enableDynamicColor);
        mPrefEditor.apply();
    }

    public static boolean getEnableDisguise() {
        return mPrefs.getBoolean("enableDisguise", false);
    }

    public static void setEnableDisguise(boolean enableDisguise) {
        mPrefEditor.putBoolean("enableDisguise", enableDisguise);
        mPrefEditor.apply();
    }

    public static boolean getDoShowQuitDisguiseInstuct() {
        return mPrefs.getBoolean("doShowQuitDisguiseInstuct", true);
    }

    public static void setDoShowQuitDisguiseInstuct(boolean doShowQuitDisguiseInstuct) {
        mPrefEditor.putBoolean("doShowQuitDisguiseInstuct", doShowQuitDisguiseInstuct);
        mPrefEditor.apply();
    }

    public static boolean getShowWelcome() {
        return mPrefs.getBoolean("showWelcome", true);
    }

    public static void setShowWelcome(boolean showWelcome) {
        mPrefEditor.putBoolean("showWelcome", showWelcome);
        mPrefEditor.apply();
    }
}
