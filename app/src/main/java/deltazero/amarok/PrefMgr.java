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
    public static final String MAIN_PREF_FILENAME = "deltazero.amarok.prefs";
    public static boolean initialized = false;

    /**
     * This method should be invoked in {@link AmarokApplication#onCreate()}.
     *
     * @param context Application context
     */
    public static void init(Context context) {
        mPrefs = context.getSharedPreferences(MAIN_PREF_FILENAME, MODE_PRIVATE);
        mPrefEditor = mPrefs.edit();
        initialized = true;
    }

    public static SharedPreferences getPrefs() {
        return mPrefs;
    }

    public static final String HIDE_FILE_PATH = "hideFilePath";
    public static final String IS_HIDDEN = "isHidden";
    public static final String HIDE_PKG_NAMES = "hidePkgNames";
    public static final String APP_HIDER_MODE = "appHiderMode";
    public static final String FILE_HIDER_MODE = "fileHiderMode";
    public static final String IS_ENABLE_AUTO_UPDATE = "isEnableAutoUpdate";
    public static final String ENABLE_OBFUSCATE_FILE_HEADER = "enableObfuscateFileHeader";
    public static final String ENABLE_OBFUSCATE_TEXT_FILE = "enableObfuscateTextFile";
    public static final String ENABLE_OBFUSCATE_TEXT_FILE_ENHANCED = "enableObfuscateTextFileEnhanced";
    public static final String ENABLE_QUICK_HIDE_SERVICE = "enableQuickHideService";
    public static final String ENABLE_PANIC_BUTTON = "enablePanicButton";
    public static final String AMAROK_PASSWORD = "amarokPassword";
    public static final String ENABLE_AMAROK_BIOMETRIC_AUTH = "enableAmarokBiometricAuth";
    public static final String ENABLE_DYNAMIC_COLOR = "enableDynamicColor";
    public static final String ENABLE_DISGUISE = "enableDisguise";
    public static final String HIDE_AMAROK_ICON = "hideAmarokIcon";
    public static final String DO_SHOW_QUIT_DISGUISE_INSTRUCT = "doShowQuitDisguiseInstuct";
    public static final String SHOW_WELCOME = "showWelcome";
    public static final String ENABLE_AUTO_HIDE = "enableAutoHide";
    public static final String AUTO_HIDE_DELAY = "autoHideDelay";
    public static final String BLOCK_SCREENSHOTS = "blockScreenshots";
    public static final String ENABLE_X_HIDE = "enableXHide";
    public static final String DISABLE_ONLY_WITH_XHIDE = "disableOnlyWithXHide";
    public static final String DISABLE_TOASTS = "disableToasts";
    public static final String INVERT_TILE_COLOR = "invertTileColor";
    public static final String DISABLE_SECURITY_WHEN_UNHIDDEN = "disableSecurityWhenUnhidden";
    public static final String HIDE_FROM_RECENTS = "hideFromRecents";

    public static Set<String> getHideFilePath() {
        return mPrefs.getStringSet(HIDE_FILE_PATH, new HashSet<>());
    }

    public static void setHideFilePath(Set<String> path) {
        mPrefEditor.putStringSet(HIDE_FILE_PATH, path);
        mPrefEditor.apply();
    }

    /**
     * Avoid using this method except for initializing {@link Hider}.
     * Use {@link Hider#getState()} instead.
     */
    public static boolean getIsHidden() {
        return mPrefs.getBoolean(IS_HIDDEN, false);
    }

    public static void setIsHidden(boolean isHidden) {
        mPrefEditor.putBoolean(IS_HIDDEN, isHidden);
        mPrefEditor.apply();
    }

    public static Set<String> getHideApps() {
        return mPrefs.getStringSet(HIDE_PKG_NAMES, new HashSet<>());
    }

    public static void setHideApps(Set<String> pkgNames) {
        mPrefEditor.putStringSet(HIDE_PKG_NAMES, pkgNames);
        mPrefEditor.apply();
    }

    public static BaseAppHider getAppHider(Context context) {
        return switch (mPrefs.getInt(APP_HIDER_MODE, 0)) {
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
        mPrefEditor.putInt(APP_HIDER_MODE, modeCode);
        mPrefEditor.apply();
    }

    public static BaseFileHider getFileHider(Context context) {
        return switch (mPrefs.getInt(FILE_HIDER_MODE, 1)) {
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
        mPrefEditor.putInt(FILE_HIDER_MODE, modeCode);
        mPrefEditor.apply();
    }

    public static boolean getEnableAutoUpdate() {
        return mPrefs.getBoolean(IS_ENABLE_AUTO_UPDATE, true);
    }

    public static void setEnableAutoUpdate(boolean isEnable) {
        mPrefEditor.putBoolean(IS_ENABLE_AUTO_UPDATE, isEnable);
        mPrefEditor.apply();
    }

    public static boolean getEnableObfuscateFileHeader() {
        return mPrefs.getBoolean(ENABLE_OBFUSCATE_FILE_HEADER, false);
    }

    public static void setEnableObfuscateFileHeader(boolean ifObfuscateFileHeader) {
        mPrefEditor.putBoolean(ENABLE_OBFUSCATE_FILE_HEADER, ifObfuscateFileHeader);
        mPrefEditor.apply();
    }

    public static boolean getEnableObfuscateTextFile() {
        return mPrefs.getBoolean(ENABLE_OBFUSCATE_TEXT_FILE, false);
    }

    public static void setEnableObfuscateTextFile(boolean ifObfuscateTextFile) {
        mPrefEditor.putBoolean(ENABLE_OBFUSCATE_TEXT_FILE, ifObfuscateTextFile);
        mPrefEditor.apply();
    }

    public static boolean getEnableObfuscateTextFileEnhanced() {
        return mPrefs.getBoolean(ENABLE_OBFUSCATE_TEXT_FILE_ENHANCED, false);
    }

    public static void setEnableObfuscateTextFileEnhanced(boolean ifObfuscateTextFileEnhanced) {
        mPrefEditor.putBoolean(ENABLE_OBFUSCATE_TEXT_FILE_ENHANCED, ifObfuscateTextFileEnhanced);
        mPrefEditor.apply();
    }

    public static boolean getEnableQuickHideService() {
        return mPrefs.getBoolean(ENABLE_QUICK_HIDE_SERVICE, false);
    }

    public static void setEnableQuickHideService(boolean isEnableQuickHideService) {
        mPrefEditor.putBoolean(ENABLE_QUICK_HIDE_SERVICE, isEnableQuickHideService);
        mPrefEditor.apply();
    }

    public static boolean getEnablePanicButton() {
        return mPrefs.getBoolean(ENABLE_PANIC_BUTTON, false);
    }

    public static void setEnablePanicButton(boolean isEnablePanicButton) {
        mPrefEditor.putBoolean(ENABLE_PANIC_BUTTON, isEnablePanicButton);
        mPrefEditor.apply();
    }

    @Nullable
    public static String getAmarokPassword() {
        return mPrefs.getString(AMAROK_PASSWORD, null);
    }

    public static void setAmarokPassword(String password) {
        mPrefEditor.putString(AMAROK_PASSWORD, password);
        mPrefEditor.apply();
    }

    public static boolean getEnableAmarokBiometricAuth() {
        return mPrefs.getBoolean(ENABLE_AMAROK_BIOMETRIC_AUTH, false);
    }

    public static void setEnableAmarokBiometricAuth(boolean enableAmarokBiometricAuth) {
        mPrefEditor.putBoolean(ENABLE_AMAROK_BIOMETRIC_AUTH, enableAmarokBiometricAuth);
        mPrefEditor.apply();
    }

    public static boolean getEnableDynamicColor() {
        return mPrefs.getBoolean(ENABLE_DYNAMIC_COLOR,
                (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU));
    }

    public static void setEnableDynamicColor(boolean enableDynamicColor) {
        mPrefEditor.putBoolean(ENABLE_DYNAMIC_COLOR, enableDynamicColor);
        mPrefEditor.apply();
    }

    public static boolean getEnableDisguise() {
        return mPrefs.getBoolean(ENABLE_DISGUISE, false);
    }

    public static void setEnableDisguise(boolean enableDisguise) {
        mPrefEditor.putBoolean(ENABLE_DISGUISE, enableDisguise);
        mPrefEditor.apply();
    }

    public static boolean getHideAmarokIcon() {
        return mPrefs.getBoolean(HIDE_AMAROK_ICON, false);
    }

    public static void setHideAmarokIcon(boolean hideAmarokIcon) {
        mPrefEditor.putBoolean(HIDE_AMAROK_ICON, hideAmarokIcon);
        mPrefEditor.apply();
    }

    public static boolean getDoShowQuitDisguiseInstuct() {
        return mPrefs.getBoolean(DO_SHOW_QUIT_DISGUISE_INSTRUCT, true);
    }

    public static void setDoShowQuitDisguiseInstuct(boolean doShowQuitDisguiseInstuct) {
        mPrefEditor.putBoolean(DO_SHOW_QUIT_DISGUISE_INSTRUCT, doShowQuitDisguiseInstuct);
        mPrefEditor.apply();
    }

    public static boolean getShowWelcome() {
        return mPrefs.getBoolean(SHOW_WELCOME, true);
    }

    public static void setShowWelcome(boolean showWelcome) {
        mPrefEditor.putBoolean(SHOW_WELCOME, showWelcome);
        mPrefEditor.apply();
    }

    public static boolean getEnableAutoHide() {
        return mPrefs.getBoolean(ENABLE_AUTO_HIDE, false);
    }

    public static void setEnableAutoHide(boolean enableAutoHide) {
        mPrefEditor.putBoolean(ENABLE_AUTO_HIDE, enableAutoHide);
        mPrefEditor.apply();
    }

    public static int getAutoHideDelay() {
        return mPrefs.getInt(AUTO_HIDE_DELAY, 0);
    }

    public static void setAutoHideDelay(int autoHideDelay) {
        mPrefEditor.putInt(AUTO_HIDE_DELAY, autoHideDelay);
        mPrefEditor.apply();
    }

    public static boolean getBlockScreenshots() {
        return mPrefs.getBoolean(BLOCK_SCREENSHOTS, false);
    }

    public static void setBlockScreenshots(boolean blockScreenshots) {
        mPrefEditor.putBoolean(BLOCK_SCREENSHOTS, blockScreenshots);
        mPrefEditor.apply();
    }

    public static boolean isXHideEnabled() {
        return mPrefs.getBoolean(ENABLE_X_HIDE, false);
    }

    public static void setXHideEnabled(boolean enableXHide) {
        mPrefEditor.putBoolean(ENABLE_X_HIDE, enableXHide);
        mPrefEditor.apply();
    }

    public static boolean getDisableOnlyWithXHide() {
        return mPrefs.getBoolean(DISABLE_ONLY_WITH_XHIDE, false);
    }

    public static void setDisableOnlyWithXHide(boolean disableOnlyWithXHide) {
        mPrefEditor.putBoolean(DISABLE_ONLY_WITH_XHIDE, disableOnlyWithXHide);
        mPrefEditor.apply();
    }

    public static boolean getDisableToasts() {
        return mPrefs.getBoolean(DISABLE_TOASTS, false);
    }

    public static void setDisableToasts(boolean disableToasts) {
        mPrefEditor.putBoolean(DISABLE_TOASTS, disableToasts);
        mPrefEditor.apply();
    }

    public static boolean getInvertTileColor() {
        return mPrefs.getBoolean(INVERT_TILE_COLOR, false);
    }

    public static void setInvertTileColor(boolean invertTileColor) {
        mPrefEditor.putBoolean(INVERT_TILE_COLOR, invertTileColor);
        mPrefEditor.apply();
    }

    public static boolean getDisableSecurityWhenUnhidden() {
        return mPrefs.getBoolean(DISABLE_SECURITY_WHEN_UNHIDDEN, false);
    }

    public static void setDisableSecurityWhenUnhidden(boolean disableSecurityWhenUnhidden) {
        mPrefEditor.putBoolean(DISABLE_SECURITY_WHEN_UNHIDDEN, disableSecurityWhenUnhidden);
        mPrefEditor.apply();
    }

    public static boolean getHideFromRecents() {
        return mPrefs.getBoolean(HIDE_FROM_RECENTS, false);
    }

    public static void setHideFromRecents(boolean hideFromRecents) {
        mPrefEditor.putBoolean(HIDE_FROM_RECENTS, hideFromRecents);
        mPrefEditor.apply();
    }
}
