package deltazero.amarok.xhide

object XHideContract {
  const val PROTOCOL_VERSION = 1
  const val MODULE_PACKAGE = "deltazero.amarok.xhide"
  const val SERVICE_CLASS = "deltazero.amarok.xhide.XHideSyncService"
  const val REMOTE_PREF_GROUP = "xhide"
  const val SENTINEL_PACKAGE = "deltazero.amarok.xhide.status"

  const val KEY_PROTOCOL_VERSION = "protocolVersion"
  const val KEY_ENABLED = "enabled"
  const val KEY_HIDDEN_PACKAGES = "hiddenPackages"
  const val KEY_MAIN_APP_PACKAGE = "mainAppPackage"
  const val KEY_MAIN_APP_VERSION_CODE = "mainAppVersionCode"
  const val KEY_UPDATED_AT = "updatedAt"

  const val KEY_MODULE_ACTIVE = "moduleActive"
  const val KEY_FRAMEWORK_NAME = "frameworkName"
  const val KEY_FRAMEWORK_VERSION = "frameworkVersion"
  const val KEY_FRAMEWORK_VERSION_CODE = "frameworkVersionCode"
  const val KEY_API_VERSION = "apiVersion"
  const val KEY_LAST_SYNC_TIME = "lastSyncTime"
  const val KEY_HOOKS_LIVE = "hooksLive"
  const val KEY_HOOK_COUNT = "hookCount"
  const val KEY_HOOK_ERROR = "hookError"
}
