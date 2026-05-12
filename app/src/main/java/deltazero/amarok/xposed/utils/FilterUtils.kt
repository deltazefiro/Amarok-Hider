@file:JvmName("FilterUtils")

package deltazero.amarok.xposed.utils

import android.annotation.SuppressLint
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.ObjectUtils
import java.lang.reflect.Method
import java.util.Arrays
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors

object FilterUtils {
  // NOTE: This method will not refresh XPref cache. Call refreshCache() before calling this method
  @JvmStatic
  @SuppressLint("DefaultLocale")
  fun filterAppsOrPkgs(appsOrPkgs: List<Any>, m: Method): List<Any> {
    if (!XPref.isXHideActive()) return appsOrPkgs

    val filteredCount = AtomicInteger()
    val filteredPackages =
      appsOrPkgs
        .stream()
        .filter { appOrPkg ->
          val shouldHide: Boolean =
            try {
              XPref.shouldHide(
                ObjectUtils.getObjectOrNullUntilSuperclassAs(appOrPkg, "packageName", null)
              )
            } catch (e: NoSuchFieldException) {
              throw RuntimeException(e)
            }
          if (shouldHide) filteredCount.getAndIncrement()
          !shouldHide
        }
        .collect(Collectors.toList())

    Log.d(
      String.format(
        "%s$%s(%s): Filtered %d packages",
        m.declaringClass.simpleName,
        m.name,
        Arrays.toString(m.parameterTypes),
        filteredCount.get(),
      ),
      null,
    )
    return filteredPackages
  }

  // NOTE: This method will not refresh XPref cache. Call refreshCache() before calling this method
  @JvmStatic
  fun filterAppsOrPkgsInSlices(appsOrPkgs: Any, m: Method): Any {
    val slice: List<Any> = ParceledListSliceUtil.sliceToList(appsOrPkgs)
    return ParceledListSliceUtil.listToSlice(filterAppsOrPkgs(slice, m))
  }
}
