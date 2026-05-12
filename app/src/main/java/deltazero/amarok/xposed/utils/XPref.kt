package deltazero.amarok.xposed.utils

import com.github.kyuubiran.ezxhelper.Log
import de.robv.android.xposed.XSharedPreferences
import deltazero.amarok.BuildConfig
import java.util.Collections

object XPref {
  private var xPref: XSharedPreferences? = null

  const val XPREF_PATH: String = "deltazero.amarok.xposed.prefs"
  const val HIDE_PKG_NAMES: String = "hidePkgNames"
  const val IS_ACTIVE: String = "isActive"

  private var isActiveCache = false
  private var hidePkgNamesCache: Set<String> = Collections.emptySet()

  @JvmStatic
  fun init() {
    Log.d("Initializing XPref...", null)

    xPref = XSharedPreferences(BuildConfig.APPLICATION_ID, XPREF_PATH)
    Log.d("xPref path: " + xPref!!.file.absolutePath, null)

    if (xPref!!.file.canRead()) {
      xPref!!.reload()
      Log.d("xPref content: " + xPref!!.all, null)
    } else {
      Log.wx("No XPref found. Launch Amarok once to activate XHide.", null)
    }

    Log.ix("XPref initialized.", null)
  }

  @JvmStatic
  fun refreshCache() {
    xPref!!.reload()
    if (!xPref!!.file.canRead()) return
    isActiveCache = xPref!!.getBoolean(IS_ACTIVE, false)
    hidePkgNamesCache =
      xPref!!.getStringSet(HIDE_PKG_NAMES, Collections.emptySet()) ?: Collections.emptySet()
  }

  @JvmStatic fun isXHideActive(): Boolean = isActiveCache

  @JvmStatic fun shouldHide(pkgName: String?): Boolean = hidePkgNamesCache.contains(pkgName)

  @JvmStatic
  fun getXPrefDir(): String =
    // This function can be called without initializing XPref
    XSharedPreferences(BuildConfig.APPLICATION_ID, XPREF_PATH).file.parentFile!!.absolutePath
}
