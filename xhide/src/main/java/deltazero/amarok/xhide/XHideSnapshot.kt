package deltazero.amarok.xhide

import android.content.SharedPreferences
import android.os.Bundle

data class XHideSnapshot(
  val protocolVersion: Int = XHideContract.PROTOCOL_VERSION,
  val enabled: Boolean = false,
  val hiddenPackages: Set<String> = emptySet(),
  val mainAppPackage: String = "",
  val mainAppVersionCode: Long = 0,
  val updatedAt: Long = 0,
) {
  val active: Boolean
    get() = enabled && hiddenPackages.isNotEmpty()

  fun shouldHide(pkgName: String?): Boolean = active && pkgName in hiddenPackages

  fun toBundle(): Bundle =
    Bundle().apply {
      putInt(XHideContract.KEY_PROTOCOL_VERSION, protocolVersion)
      putBoolean(XHideContract.KEY_ENABLED, enabled)
      putStringArrayList(XHideContract.KEY_HIDDEN_PACKAGES, ArrayList(hiddenPackages))
      putString(XHideContract.KEY_MAIN_APP_PACKAGE, mainAppPackage)
      putLong(XHideContract.KEY_MAIN_APP_VERSION_CODE, mainAppVersionCode)
      putLong(XHideContract.KEY_UPDATED_AT, updatedAt)
    }

  companion object {
    fun fromBundle(bundle: Bundle): XHideSnapshot? {
      val protocolVersion = bundle.getInt(XHideContract.KEY_PROTOCOL_VERSION, -1)
      if (protocolVersion != XHideContract.PROTOCOL_VERSION) return null
      val mainAppPackage = bundle.getString(XHideContract.KEY_MAIN_APP_PACKAGE, "")
      val hiddenPackages =
        bundle.getStringArrayList(XHideContract.KEY_HIDDEN_PACKAGES)?.toSet() ?: emptySet()
      return XHideSnapshot(
        protocolVersion = protocolVersion,
        enabled = bundle.getBoolean(XHideContract.KEY_ENABLED, false),
        hiddenPackages = hiddenPackages,
        mainAppPackage = mainAppPackage,
        mainAppVersionCode = bundle.getLong(XHideContract.KEY_MAIN_APP_VERSION_CODE, 0),
        updatedAt = bundle.getLong(XHideContract.KEY_UPDATED_AT, 0),
      )
    }

    fun fromPreferences(prefs: SharedPreferences): XHideSnapshot =
      XHideSnapshot(
        protocolVersion =
          prefs.getInt(XHideContract.KEY_PROTOCOL_VERSION, XHideContract.PROTOCOL_VERSION),
        enabled = prefs.getBoolean(XHideContract.KEY_ENABLED, false),
        hiddenPackages =
          prefs.getStringSet(XHideContract.KEY_HIDDEN_PACKAGES, emptySet()) ?: emptySet(),
        mainAppPackage = prefs.getString(XHideContract.KEY_MAIN_APP_PACKAGE, "") ?: "",
        mainAppVersionCode = prefs.getLong(XHideContract.KEY_MAIN_APP_VERSION_CODE, 0),
        updatedAt = prefs.getLong(XHideContract.KEY_UPDATED_AT, 0),
      )
  }
}
