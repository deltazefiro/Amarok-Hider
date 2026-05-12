package deltazero.amarok.xposed.hooks

import com.github.kyuubiran.ezxhelper.Log

object FilterHooks {
  /*
  Target 33 hooks: com.android.server.pm.IPackageManagerBase
  - public final ParceledListSlice<ApplicationInfo> getInstalledApplications(
                @PackageManager.ApplicationInfoFlagsBits long flags, int userId)
  - public final ParceledListSlice<PackageInfo> getInstalledPackages(
                @PackageManager.PackageInfoFlagsBits long flags, int userId)
  */
  @JvmStatic
  fun target33(): List<IHook> {
    Log.ix("Loading target33 FilterHooks", null)
    val hooks: MutableList<IHook> = ArrayList()
    hooks.addAll(
      FilterHookFactory.build(
        "com.android.server.pm.IPackageManagerBase",
        "getInstalledApplications",
        true,
      )
    )
    hooks.addAll(
      FilterHookFactory.build(
        "com.android.server.pm.IPackageManagerBase",
        "getInstalledPackages",
        true,
      )
    )
    return hooks
  }

  /*
  Target 31 hooks: com.android.server.pm.PackageManagerService
  - private List<ApplicationInfo> getInstalledApplicationsListInternal(int flags, int userId, int callingUid)
  - public ParceledListSlice<PackageInfo> getInstalledPackages(int flags, int userId)
  - public final ParceledListSlice<PackageInfo> getInstalledPackages(int flags, int userId)
  - public final ParceledListSlice<PackageInfo> getInstalledPackages(int flags, int userId)
  */
  @JvmStatic
  fun target31(): List<IHook> {
    Log.ix("Loading target31 FilterHooks", null)
    val hooks: MutableList<IHook> = ArrayList()
    hooks.addAll(
      FilterHookFactory.build(
        "com.android.server.pm.PackageManagerService",
        "getInstalledApplicationsListInternal",
        false,
      )
    )
    hooks.addAll(
      FilterHookFactory.build(
        "com.android.server.pm.PackageManagerService",
        "getInstalledPackages",
        true,
      )
    )
    return hooks
  }

  /*
  Target 29 hooks: com.android.server.pm.PackageManagerService
  - private List<ApplicationInfo> getInstalledApplicationsListInternal(int flags, int userId, int callingUid)
  - public ParceledListSlice<PackageInfo> getInstalledPackages(int flags, int userId)
   */
  @JvmStatic
  fun target29(): List<IHook> {
    Log.ix("Loading target29 FilterHooks", null)
    val hooks: MutableList<IHook> = ArrayList()
    hooks.addAll(
      FilterHookFactory.build(
        "com.android.server.pm.PackageManagerService",
        "getInstalledApplicationsListInternal",
        false,
      )
    )
    hooks.addAll(
      FilterHookFactory.build(
        "com.android.server.pm.PackageManagerService",
        "getInstalledPackages",
        true,
      )
    )
    return hooks
  }

  /*
  LegacyHooks: com.android.server.pm.PackageManagerService
  - public ParceledListSlice<ApplicationInfo> getInstalledApplications(int flags, int userId)
  - public ParceledListSlice<PackageInfo> getInstalledPackages(int flags, int userId)
   */
  @JvmStatic
  fun legacy(): List<IHook> {
    Log.ix("Loading legacy FilterHooks", null)
    val hooks: MutableList<IHook> = ArrayList()
    hooks.addAll(
      FilterHookFactory.build(
        "com.android.server.pm.PackageManagerService",
        "getInstalledApplications",
        true,
      )
    )
    hooks.addAll(
      FilterHookFactory.build(
        "com.android.server.pm.PackageManagerService",
        "getInstalledPackages",
        true,
      )
    )
    return hooks
  }
}
