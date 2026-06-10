package deltazero.amarok.xhide

import android.app.Application
import android.content.SharedPreferences
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

class XHideApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    XposedServiceHelper.registerListener(
      object : XposedServiceHelper.OnServiceListener {
        override fun onServiceBind(service: XposedService) {
          XHideFrameworkStatus.onServiceBind(service)
          runCatching {
            val prefs: SharedPreferences =
              service.getRemotePreferences(XHideContract.REMOTE_PREF_GROUP)
            XHideStateStore.attach(prefs)
          }
        }

        override fun onServiceDied(service: XposedService) {
          XHideFrameworkStatus.onServiceDied(service)
        }
      }
    )
  }
}
