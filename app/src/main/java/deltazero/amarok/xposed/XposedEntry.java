package deltazero.amarok.xposed;

import android.os.Build;

import com.github.kyuubiran.ezxhelper.EzXHelper;
import com.github.kyuubiran.ezxhelper.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import deltazero.amarok.BuildConfig;
import deltazero.amarok.xposed.hooks.HookTarget33;
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
        Log.i("Loading system hooks...", null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            new HookTarget33().init();
        } else {
            Log.e("Unsupported Android version. Skip loading hooks.", null);
        }
        Log.i("System hooks loaded.", null);
    }

    public void loadSelfHooks(XC_LoadPackage.LoadPackageParam lpparam) {
        Log.i("Loading self hooks...", null);
        var c = XposedHelpers.findClass("deltazero.amarok.utils.XHideUtil", lpparam.classLoader);
        XposedHelpers.setStaticBooleanField(c, "isModuleActive", true);
        XposedHelpers.setStaticIntField(c, "xposedVersion", XposedBridge.getXposedVersion());
        Log.i("Self hooks loaded.", null);
    }
}
