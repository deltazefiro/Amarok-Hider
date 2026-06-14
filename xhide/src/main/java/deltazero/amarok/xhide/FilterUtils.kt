package deltazero.amarok.xhide

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Binder
import java.lang.reflect.Method

object FilterUtils {
  fun filterAppsOrPkgs(appsOrPkgs: List<Any>, method: Method, log: (String) -> Unit): List<Any> {
    val callerUid = Binder.getCallingUid()
    if (isModuleInstalledPackagesQuery(callerUid, method)) return appsOrPkgs + buildSentinel()

    val snapshot = XHideStateStore.snapshotForCaller(callerUid) ?: return appsOrPkgs
    var filteredCount = 0
    val filtered =
      appsOrPkgs.filterNot {
        val shouldHide = snapshot.shouldHide(it.packageNameOrNull())
        if (shouldHide) filteredCount++
        shouldHide
      }
    logFilter(method, filteredCount, callerUid, log)
    return filtered
  }

  fun filterAppsOrPkgsInPlace(appsOrPkgs: MutableList<Any>, method: Method, log: (String) -> Unit) {
    val callerUid = Binder.getCallingUid()
    if (isModuleInstalledPackagesQuery(callerUid, method)) {
      appsOrPkgs.add(buildSentinel())
      return
    }

    val snapshot = XHideStateStore.snapshotForCaller(callerUid) ?: return
    var filteredCount = 0
    appsOrPkgs.removeAll {
      val shouldHide = snapshot.shouldHide(it.packageNameOrNull())
      if (shouldHide) filteredCount++
      shouldHide
    }
    logFilter(method, filteredCount, callerUid, log)
  }

  fun filterAppsOrPkgsInSlices(appsOrPkgs: Any, method: Method, log: (String) -> Unit): Any {
    val list = ParceledListSliceUtil.sliceToList<Any>(appsOrPkgs)
    val callerUid = Binder.getCallingUid()
    if (isModuleInstalledPackagesQuery(callerUid, method)) {
      if (list is MutableList<*>) {
        @Suppress("UNCHECKED_CAST") (list as MutableList<Any>).add(buildSentinel())
        return appsOrPkgs
      }
      return ParceledListSliceUtil.listToSlice(list + buildSentinel())
    }

    if (list is MutableList<*>) {
      @Suppress("UNCHECKED_CAST") filterAppsOrPkgsInPlace(list as MutableList<Any>, method, log)
      return appsOrPkgs
    }
    return ParceledListSliceUtil.listToSlice(filterAppsOrPkgs(list, method, log))
  }

  private fun isModuleInstalledPackagesQuery(callerUid: Int, method: Method): Boolean =
    method.name == "getInstalledPackages" &&
      XHideStateStore.packagesForUid(callerUid)?.contains(XHideContract.MODULE_PACKAGE) == true

  private fun buildSentinel(): PackageInfo =
    PackageInfo().apply {
      packageName = XHideContract.SENTINEL_PACKAGE
      versionName = XHideHookStatus.errorMessage
      @Suppress("DEPRECATION")
      versionCode = XHideHookStatus.hookCount
      longVersionCode = XHideHookStatus.hookCount.toLong()
    }

  private fun logFilter(method: Method, filteredCount: Int, callerUid: Int, log: (String) -> Unit) {
    val callerPackages = XHideStateStore.packagesForUid(callerUid)?.contentToString() ?: "[]"
    log(
      "${method.declaringClass.simpleName}\$${method.name}" +
        "(${method.parameterTypes.contentToString()}): " +
        "Filtered $filteredCount packages for callerUid=$callerUid callerPackages=$callerPackages"
    )
  }

  private fun Any.packageNameOrNull(): String? =
    when (this) {
      is ApplicationInfo -> packageName
      is PackageInfo -> packageName
      else -> runCatching { javaClass.getField("packageName").get(this) as? String }.getOrNull()
    }
}
