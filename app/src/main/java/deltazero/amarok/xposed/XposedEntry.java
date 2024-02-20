package deltazero.amarok.xposed;

import android.os.Build;

import com.github.kyuubiran.ezxhelper.EzXHelper;
import com.github.kyuubiran.ezxhelper.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import deltazero.amarok.BuildConfig;
import deltazero.amarok.xposed.hooks.FilterHooks;
import deltazero.amarok.xposed.hooks.IHook;
import deltazero.amarok.xposed.utils.ParceledListSliceUtil;
import deltazero.amarok.xposed.utils.XPref;

/**
 * @noinspection unused
 */
public class XposedEntry implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static final String TAG = "Amarok-XHide";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        EzXHelper.initHandleLoadPackage(lpparam);
        switch (lpparam.packageName) {
            case BuildConfig.APPLICATION_ID -> loadSelfHooks(lpparam);
            case "android" -> {
                ParceledListSliceUtil.init();
                XPref.init();
                loadSystemHooks();
            }
        }
    }

    @Override
    public void initZygote(StartupParam startupParam) {
        EzXHelper.initZygote(startupParam);
        EzXHelper.setLogTag(TAG);
        EzXHelper.setToastTag(TAG);
    }

    private void loadSystemHooks() {
        Log.d("Initializing system hooks...", null);
        List<IHook> hooks = new ArrayList<>();

        if (Build.VERSION.SDK_INT > 35) {
            Log.e("Unsupported Android version. Skip loading hooks.", null);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hooks.addAll(FilterHooks.target33());
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hooks.addAll(FilterHooks.target31());
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
            hooks.addAll(FilterHooks.target29());
        } else {
            hooks.addAll(FilterHooks.legacy());
        }

        Log.d("Loading system hooks...", null);
        hooks.forEach(IHook::load);
    }

    public void loadSelfHooks(XC_LoadPackage.LoadPackageParam lpparam) {
        Log.d("Loading self hooks...", null);
        var c = XposedHelpers.findClass("deltazero.amarok.utils.XHideUtil", lpparam.classLoader);
        XposedHelpers.setStaticBooleanField(c, "isModuleActive", true);
        XposedHelpers.setStaticIntField(c, "xposedVersion", XposedBridge.getXposedVersion());
        Log.i("Self hooks loaded.", null);
    }
}
