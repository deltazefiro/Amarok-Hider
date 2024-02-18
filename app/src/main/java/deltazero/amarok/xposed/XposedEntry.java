package deltazero.amarok.xposed;

import android.os.Build;

import com.github.kyuubiran.ezxhelper.EzXHelper;
import com.github.kyuubiran.ezxhelper.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
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
        if (!lpparam.packageName.equals("android")) return;
        initUtils(lpparam);
        loadHooks();
    }

    @Override
    public void initZygote(StartupParam startupParam) {
        EzXHelper.initZygote(startupParam);
    }

    private void initUtils(XC_LoadPackage.LoadPackageParam lpparam) {
        EzXHelper.initHandleLoadPackage(lpparam);
        EzXHelper.setLogTag(TAG);
        EzXHelper.setToastTag(TAG);
        ParceledListSliceUtil.init();
        XPref.init();
    }

    private void loadHooks() {
        Log.i("Loading hooks...", null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            new HookTarget33().init();
        }
        Log.i("Xposed hooks loaded.", null);
    }
}
