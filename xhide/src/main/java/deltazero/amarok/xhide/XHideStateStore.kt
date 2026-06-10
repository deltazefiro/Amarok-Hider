package deltazero.amarok.xhide

import android.content.SharedPreferences
import java.util.concurrent.atomic.AtomicReference

object XHideStateStore {
  private val snapshotRef = AtomicReference(XHideSnapshot())
  private var remotePrefs: SharedPreferences? = null
  @Volatile private var cachedMainAppPackage = ""
  @Volatile private var cachedMainAppUid = -1
  private val listener =
    SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, _ ->
      refreshFrom(sharedPreferences)
    }

  val snapshot: XHideSnapshot
    get() = snapshotRef.get()

  fun snapshotForCaller(uid: Int): XHideSnapshot? {
    val snapshot = snapshotRef.get()
    if (!snapshot.active || isMainAppCaller(snapshot.mainAppPackage, uid)) return null
    return snapshot
  }

  fun attach(prefs: SharedPreferences) {
    remotePrefs = prefs
    refreshFrom(prefs)
    prefs.registerOnSharedPreferenceChangeListener(listener)
  }

  fun write(snapshot: XHideSnapshot): Boolean {
    val prefs = remotePrefs ?: return false
    val ok =
      prefs
        .edit()
        .putInt(XHideContract.KEY_PROTOCOL_VERSION, snapshot.protocolVersion)
        .putBoolean(XHideContract.KEY_ENABLED, snapshot.enabled)
        .putStringSet(XHideContract.KEY_HIDDEN_PACKAGES, HashSet(snapshot.hiddenPackages))
        .putString(XHideContract.KEY_MAIN_APP_PACKAGE, snapshot.mainAppPackage)
        .putLong(XHideContract.KEY_MAIN_APP_VERSION_CODE, snapshot.mainAppVersionCode)
        .putLong(XHideContract.KEY_UPDATED_AT, snapshot.updatedAt)
        .commit()
    if (ok) snapshotRef.set(snapshot)
    return ok
  }

  private fun refreshFrom(prefs: SharedPreferences) {
    val snapshot = XHideSnapshot.fromPreferences(prefs)
    if (snapshot.protocolVersion == XHideContract.PROTOCOL_VERSION) snapshotRef.set(snapshot)
  }

  private fun isMainAppCaller(mainAppPackage: String, uid: Int): Boolean {
    if (mainAppPackage.isEmpty()) return false
    if (cachedMainAppUid == uid && cachedMainAppPackage == mainAppPackage) return true
    val matches = packagesForUid(uid)?.contains(mainAppPackage) == true
    if (matches) {
      cachedMainAppUid = uid
      cachedMainAppPackage = mainAppPackage
    }
    return matches
  }

  fun packagesForUid(uid: Int): Array<String>? =
    runCatching {
        val packageManager =
          Class.forName("android.app.AppGlobals").getMethod("getPackageManager").invoke(null)
        @Suppress("UNCHECKED_CAST")
        packageManager.javaClass
          .getMethod("getPackagesForUid", Int::class.javaPrimitiveType)
          .invoke(packageManager, uid) as? Array<String>
      }
      .getOrNull()
}
