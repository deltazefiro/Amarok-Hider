package deltazero.amarok.xhide

import android.os.Build
import io.github.libxposed.api.XposedInterface

object FilterHooks {
  fun load(xposed: XposedInterface, classLoader: ClassLoader) {
    when {
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> loadTarget33(xposed, classLoader)
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> loadTarget31(xposed, classLoader)
      Build.VERSION.SDK_INT == Build.VERSION_CODES.P -> loadTarget29(xposed, classLoader)
      else -> loadLegacy(xposed, classLoader)
    }
  }

  private fun loadTarget33(xposed: XposedInterface, classLoader: ClassLoader) {
    FilterHookFactory.load(
      xposed,
      classLoader,
      "com.android.server.pm.IPackageManagerBase",
      "getInstalledApplications",
    )
    FilterHookFactory.load(
      xposed,
      classLoader,
      "com.android.server.pm.IPackageManagerBase",
      "getInstalledPackages",
    )
  }

  private fun loadTarget31(xposed: XposedInterface, classLoader: ClassLoader) {
    FilterHookFactory.load(
      xposed,
      classLoader,
      "com.android.server.pm.PackageManagerService",
      "getInstalledApplicationsListInternal",
    )
    FilterHookFactory.load(
      xposed,
      classLoader,
      "com.android.server.pm.PackageManagerService",
      "getInstalledPackages",
    )
  }

  private fun loadTarget29(xposed: XposedInterface, classLoader: ClassLoader) {
    FilterHookFactory.load(
      xposed,
      classLoader,
      "com.android.server.pm.PackageManagerService",
      "getInstalledApplicationsListInternal",
    )
    FilterHookFactory.load(
      xposed,
      classLoader,
      "com.android.server.pm.PackageManagerService",
      "getInstalledPackages",
    )
  }

  private fun loadLegacy(xposed: XposedInterface, classLoader: ClassLoader) {
    FilterHookFactory.load(
      xposed,
      classLoader,
      "com.android.server.pm.PackageManagerService",
      "getInstalledApplications",
    )
    FilterHookFactory.load(
      xposed,
      classLoader,
      "com.android.server.pm.PackageManagerService",
      "getInstalledPackages",
    )
  }
}
