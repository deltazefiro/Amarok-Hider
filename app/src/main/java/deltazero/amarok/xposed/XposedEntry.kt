package deltazero.amarok.xposed

import android.os.Build
import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import deltazero.amarok.BuildConfig
import deltazero.amarok.xposed.hooks.FilterHooks
import deltazero.amarok.xposed.hooks.IHook
import deltazero.amarok.xposed.utils.ParceledListSliceUtil
import deltazero.amarok.xposed.utils.XPref

class XposedEntry : IXposedHookLoadPackage, IXposedHookZygoteInit {

  override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
    EzXHelper.initHandleLoadPackage(lpparam)
    when (lpparam.packageName) {
      BuildConfig.APPLICATION_ID -> loadSelfHooks(lpparam)
      "android" -> {
        ParceledListSliceUtil.init()
        XPref.init()
        loadSystemHooks()
      }
    }
  }

  override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
    EzXHelper.initZygote(startupParam)
    EzXHelper.setLogTag(TAG)
    EzXHelper.setToastTag(TAG)
  }

  private fun loadSystemHooks() {
    Log.d("Initializing system hooks...", null)
    val hooks: MutableList<IHook> = ArrayList()

    if (Build.VERSION.SDK_INT > 36) {
      Log.ex("Unsupported Android version. Skip loading hooks.", null)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      hooks.addAll(FilterHooks.target33())
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      hooks.addAll(FilterHooks.target31())
    } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
      hooks.addAll(FilterHooks.target29())
    } else {
      hooks.addAll(FilterHooks.legacy())
    }

    Log.d("Loading system hooks...", null)
    hooks.forEach { it.load() }
  }

  fun loadSelfHooks(lpparam: XC_LoadPackage.LoadPackageParam) {
    Log.d("Loading self hooks...", null)

    val c = XposedHelpers.findClass("deltazero.amarok.utils.XHidePrefBridge", lpparam.classLoader)
    XposedHelpers.setStaticBooleanField(c, "isModuleActive", true)
    XposedHelpers.setStaticIntField(c, "xposedVersion", XposedBridge.getXposedVersion())
    XposedHelpers.setStaticObjectField(c, "xPrefDir", XPref.getXPrefDir())

    Log.ix("Self hooks loaded.", null)
  }

  companion object {
    private const val TAG = "Amarok-XHide"
  }
}
