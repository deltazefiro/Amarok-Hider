package deltazero.amarok

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import deltazero.amarok.core.SettingsRepository
import deltazero.amarok.receivers.ActionReceiver
import deltazero.amarok.receivers.DialerReceiver
import deltazero.amarok.receivers.ScreenStatusReceiver
import deltazero.amarok.ui.MainActivity
import deltazero.amarok.utils.AutoHideUtil
import deltazero.amarok.widget.ToggleWidget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class EntrypointContractTest {
  private val context: Context = ApplicationProvider.getApplicationContext()

  @Test
  fun actionReceiverConstantsMatchManifestActions() {
    assertEquals("deltazero.amarok.HIDE", ActionReceiver.ACTION_HIDE)
    assertEquals("deltazero.amarok.UNHIDE", ActionReceiver.ACTION_UNHIDE)
    assertEquals("deltazero.amarok.TOGGLE", ActionReceiver.ACTION_TOGGLE)

    assertBroadcastResolvesTo(ActionReceiver.ACTION_HIDE, ActionReceiver::class.java)
    assertBroadcastResolvesTo(ActionReceiver.ACTION_UNHIDE, ActionReceiver::class.java)
    assertBroadcastResolvesTo(ActionReceiver.ACTION_TOGGLE, ActionReceiver::class.java)
  }

  @Test
  fun actionReceiverShowsInvalidActionToastForUnknownActions() {
    ActionReceiver().onReceive(context, Intent("deltazero.amarok.UNKNOWN"))

    assertEquals(
      context.getString(R.string.invalid_action, "deltazero.amarok.UNKNOWN"),
      ShadowToast.getTextOfLatestToast(),
    )
  }

  @Test
  fun dialerReceiverLaunchesMainActivityInNewTask() {
    DialerReceiver().onReceive(context, Intent("android.provider.Telephony.SECRET_CODE"))

    val startedIntent = shadowOf(context as Application).nextStartedActivity

    assertEquals(MainActivity::class.java.name, startedIntent.component?.className)
    assertTrue(startedIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
  }

  @Test
  fun screenStatusReceiverIgnoresUnrelatedActionsWithoutSideEffects() {
    ScreenStatusReceiver(SettingsRepository(context))
      .onReceive(context, Intent("deltazero.amarok.UNRELATED_SCREEN_ACTION"))
  }

  @Test
  fun toggleWidgetActionAndProviderMetadataMatchManifest() {
    assertEquals(
      "deltazero.amarok.widget.ACTION_TOGGLE",
      privateStaticString(ToggleWidget::class.java, "ACTION_TOGGLE"),
    )
    assertFalse(ToggleWidget.initialized)

    val receiverInfo = receiverInfo(ToggleWidget::class.java)

    assertTrue(receiverInfo.exported)
    assertEquals(
      R.xml.toggle_widget_info,
      receiverInfo.metaData.getInt("android.appwidget.provider"),
    )
    assertBroadcastResolvesTo("android.appwidget.action.APPWIDGET_UPDATE", ToggleWidget::class.java)
    assertBroadcastResolvesTo("deltazero.amarok.widget.ACTION_TOGGLE", ToggleWidget::class.java)
  }

  @Test
  fun quickSettingsTileManifestContractIsStable() {
    assertFalse(QSTileService.initialized)

    val serviceInfo =
      context.packageManager.getServiceInfo(
        ComponentName(context, QSTileService::class.java),
        PackageManager.GET_META_DATA,
      )

    assertTrue(serviceInfo.exported)
    assertEquals("android.permission.BIND_QUICK_SETTINGS_TILE", serviceInfo.permission)
    assertEquals(R.drawable.ic_paw, serviceInfo.iconResource)
    assertEquals(
      context.getString(R.string.app_name),
      serviceInfo.loadLabel(context.packageManager).toString(),
    )
    assertServiceResolvesTo(
      "android.service.quicksettings.action.QS_TILE",
      QSTileService::class.java,
    )
    assertEquals(true, serviceInfo.metaData.getBoolean("android.service.quicksettings.ACTIVE_TILE"))
  }

  @Test
  fun autoHideUsesSingleStableUniqueWorkName() {
    assertEquals(
      "deltazero.amarok.AUTO_HIDE_WORK",
      privateStaticString(AutoHideUtil::class.java, "AUTO_HIDE_WORK_NAME"),
    )
  }

  private fun assertBroadcastResolvesTo(action: String, receiverClass: Class<*>) {
    val intent = Intent(action).setPackage(context.packageName)
    val resolves = context.packageManager.queryBroadcastReceivers(intent, 0)

    assertTrue(resolves.any { it.activityInfo.name == receiverClass.name })
  }

  private fun assertServiceResolvesTo(action: String, serviceClass: Class<*>) {
    val intent = Intent(action).setPackage(context.packageName)
    val resolves = context.packageManager.queryIntentServices(intent, 0)

    assertTrue(resolves.any { it.serviceInfo.name == serviceClass.name })
  }

  private fun receiverInfo(receiverClass: Class<*>) =
    context.packageManager.getReceiverInfo(
      ComponentName(context, receiverClass),
      PackageManager.GET_META_DATA,
    )

  private fun privateStaticString(targetClass: Class<*>, fieldName: String): String {
    val field = targetClass.getDeclaredField(fieldName)
    field.isAccessible = true
    val value = field.get(null)
    assertNotNull(value)
    return value as String
  }
}
