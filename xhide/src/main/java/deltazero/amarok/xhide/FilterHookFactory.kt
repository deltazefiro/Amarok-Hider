package deltazero.amarok.xhide

import android.util.Log
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Method

object FilterHookFactory {
  fun load(
    xposed: XposedInterface,
    classLoader: ClassLoader,
    className: String,
    methodName: String,
  ) {
    val clazz =
      runCatching { Class.forName(className, false, classLoader) }
        .getOrElse {
          xposed.log(Log.ERROR, TAG, "Error when initializing: ${className}\$${methodName}", it)
          return
        }
    val methods = clazz.declaredMethods.filter { it.name == methodName }
    if (methods.isEmpty()) {
      xposed.log(Log.ERROR, TAG, "Method not found: ${className}\$${methodName}", null)
      return
    }

    methods.forEach { method ->
      xposed.log(Log.DEBUG, TAG, "Loading hook: $method", null)
      runCatching {
          xposed.hook(method).intercept { chain ->
            val result = chain.proceed() ?: return@intercept null
            filterResult(result, method) { message -> xposed.log(Log.DEBUG, TAG, message, null) }
          }
        }
        .onFailure { xposed.log(Log.ERROR, TAG, "Hook failed: $method", it) }
        .onSuccess { xposed.log(Log.DEBUG, TAG, "Method hooked: $method", null) }
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun filterResult(result: Any, method: Method, log: (String) -> Unit): Any =
    when (result) {
      is MutableList<*> -> {
        FilterUtils.filterAppsOrPkgsInPlace(result as MutableList<Any>, method, log)
        result
      }
      is List<*> -> FilterUtils.filterAppsOrPkgs(result as List<Any>, method, log)
      else ->
        runCatching { FilterUtils.filterAppsOrPkgsInSlices(result, method, log) }
          .getOrDefault(result)
    }

  private const val TAG = "Amarok-XHide"
}
