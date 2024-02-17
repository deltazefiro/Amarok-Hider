package deltazero.amarok.xposed;

import com.github.kyuubiran.ezxhelper.EzXHelper;
import com.github.kyuubiran.ezxhelper.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** @noinspection unused*/
public class XposedEntry implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static final String TAG = "AmarokXposed";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("android")) return;

        EzXHelper.initHandleLoadPackage(lpparam);
        EzXHelper.setLogTag(TAG);
        EzXHelper.setToastTag(TAG);

        Log.i("Xposed hooks loaded.", null);
    }

    @Override
    public void initZygote(StartupParam startupParam) {
        EzXHelper.initZygote(startupParam);
    }
}
