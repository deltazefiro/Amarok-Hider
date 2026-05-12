package deltazero.amarok.xposed.hooks

import com.github.kyuubiran.ezxhelper.ClassUtils
import com.github.kyuubiran.ezxhelper.HookFactory
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.finders.MethodFinder
import com.github.kyuubiran.ezxhelper.interfaces.IMethodHookCallback
import de.robv.android.xposed.XC_MethodHook
import deltazero.amarok.xposed.utils.FilterUtils
import deltazero.amarok.xposed.utils.XPref
import java.util.Collections
import java.util.function.Consumer

object FilterHookFactory {
  @JvmStatic
  fun build(className: String, methodName: String, isSlice: Boolean): List<IHook> {
    val c: Class<*>
    try {
      c = ClassUtils.loadClass(className, null)
    } catch (e: ClassNotFoundException) {
      Log.ex(String.format("Error when initializing: %s$%s", className, methodName), e)
      return Collections.emptyList()
    }

    val methods = MethodFinder.fromClass(c).filterByName(methodName).toSet()
    if (methods.isEmpty()) {
      Log.ex(String.format("Method not found: %s$%s", methodName, c), null)
      return Collections.emptyList()
    }

    val hooks: MutableList<IHook> = ArrayList()
    for (m in methods) {
      hooks.add(
        object : IHook {

          private var unhook: XC_MethodHook.Unhook? = null

          override fun getName(): String = m.name

          @Suppress("UNCHECKED_CAST")
          override fun load() {
            Log.d(String.format("Loading hook: %s", m), null)
            try {
              unhook =
                HookFactory.createHook(
                  method = m,
                  block =
                    Consumer { hookFactory ->
                      hookFactory.after(
                        IMethodHookCallback { param ->
                          try {
                            XPref.refreshCache()
                            param.result =
                              if (isSlice) {
                                FilterUtils.filterAppsOrPkgsInSlices(param.result!!, m)
                              } else {
                                FilterUtils.filterAppsOrPkgs(param.result as List<Any>, m)
                              }
                          } catch (e: Exception) {
                            Log.ex(String.format("Error while hooking %s", m), e)
                            unhook!!.unhook()
                          }
                        }
                      )
                    },
                )
            } catch (e: Exception) {
              Log.ex(String.format("Hooked failed: %s", m), e)
            } finally {
              Log.ix(String.format("Method hooked: %s", m), null)
            }
          }
        }
      )
    }
    return hooks
  }
}
