package deltazero.amarok.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager.GET_META_DATA
import android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS
import android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES
import android.graphics.drawable.Drawable
import deltazero.amarok.AmarokApplication
import deltazero.amarok.R
import deltazero.amarok.core.HiderStateRepository
import java.util.Locale

class AppInfoUtil(context: Context) {
  private val pkgMgr = context.packageManager
  @Volatile private var appInfoList: List<AppInfo> = listOf()
  private val predefinedRootApps =
    HashSet(context.resources.getStringArray(R.array.root_app_packages).asList())
  private val hiderStateRepo: HiderStateRepository =
    (context.applicationContext as AmarokApplication).hiderStateRepo

  private fun isRootApp(appInfo: ApplicationInfo): Boolean {
    val isXposedModule = appInfo.metaData != null && appInfo.metaData.containsKey("xposedmodule")
    return isXposedModule || predefinedRootApps.contains(appInfo.packageName)
  }

  private fun isSystemApp(appInfo: ApplicationInfo): Boolean {
    return appInfo.flags and ApplicationInfo.FLAG_SYSTEM == ApplicationInfo.FLAG_SYSTEM
  }

  fun refresh() {
    val hiddenApps = hiderStateRepo.managedApps.value

    // Get applications info
    val installedApplications =
      pkgMgr.getInstalledApplications(
        GET_META_DATA or MATCH_DISABLED_COMPONENTS or MATCH_UNINSTALLED_PACKAGES
      )

    val newList = ArrayList<AppInfo>()
    for (applicationInfo in installedApplications) {
      // Filter out Amarok itself
      if (applicationInfo.packageName.contains("deltazero.amarok")) continue

      val appInfo =
        AppInfo(
          applicationInfo.packageName,
          pkgMgr.getApplicationLabel(applicationInfo).toString(),
          isSystemApp(applicationInfo),
          isRootApp(applicationInfo),
          pkgMgr.getApplicationIcon(applicationInfo),
        )

      newList.add(appInfo)
    }

    // Sort with app name, with the hidden apps always on the top
    newList.sortWith { o1, o2 ->
      if (hiddenApps.contains(o1.packageName()) && !hiddenApps.contains(o2.packageName())) {
        return@sortWith -1
      }
      if (hiddenApps.contains(o2.packageName()) && !hiddenApps.contains(o1.packageName())) {
        return@sortWith 1
      }
      o1.label().compareTo(o2.label())
    }

    // Atomically swap to the new immutable list
    appInfoList = newList.toList()
  }

  fun getFilteredApps(
    query: String?,
    includeSystemApps: Boolean,
    includeRootApps: Boolean,
  ): List<AppInfo> {
    val filtered = ArrayList<AppInfo>()
    val hiddenApps = hiderStateRepo.managedApps.value

    for (appInfo in appInfoList) {
      val queryFilterResult =
        query == null ||
          containsIgnoreCase(appInfo.label(), query) ||
          containsIgnoreCase(appInfo.packageName(), query)
      val systemFilterResult =
        includeSystemApps || !appInfo.isSystemApp || hiddenApps.contains(appInfo.packageName())
      val rootFilterResult =
        includeRootApps || !appInfo.isRootApp || hiddenApps.contains(appInfo.packageName())
      if (queryFilterResult && systemFilterResult && rootFilterResult) filtered.add(appInfo)
    }
    return filtered
  }

  class AppInfo(
    private val packageName: String,
    private val label: String,
    val isSystemApp: Boolean,
    val isRootApp: Boolean,
    private val icon: Drawable?,
  ) {
    fun packageName(): String = packageName

    fun label(): String = label

    fun icon(): Drawable? = icon

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is AppInfo) return false

      return packageName == other.packageName &&
        label == other.label &&
        isSystemApp == other.isSystemApp &&
        isRootApp == other.isRootApp &&
        icon == other.icon
    }

    override fun hashCode(): Int {
      var result = packageName.hashCode()
      result = 31 * result + label.hashCode()
      result = 31 * result + isSystemApp.hashCode()
      result = 31 * result + isRootApp.hashCode()
      result = 31 * result + (icon?.hashCode() ?: 0)
      return result
    }

    override fun toString(): String {
      return "AppInfo[packageName=$packageName, label=$label, isSystemApp=$isSystemApp, isRootApp=$isRootApp, icon=$icon]"
    }
  }

  companion object {
    @JvmStatic
    private fun containsIgnoreCase(str: String?, searchStr: String?): Boolean {
      if (str == null || searchStr == null) return false
      if (searchStr.isEmpty()) return true
      return str.lowercase(Locale.getDefault()).contains(searchStr.lowercase(Locale.getDefault()))
    }
  }
}
