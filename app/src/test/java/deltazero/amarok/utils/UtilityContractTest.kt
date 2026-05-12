package deltazero.amarok.utils

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityOptionsCompat
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ApplicationProvider
import deltazero.amarok.BuildConfig
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class UtilityContractTest {
  @After
  fun resetLocales() {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
  }

  @Test
  fun betterActivityLauncherUsesDefaultCallbackUntilLaunchOverridesIt() {
    val caller = CapturingActivityResultCaller<String, String>()
    val received = mutableListOf<String>()
    val launcher =
      BetterActivityLauncher.registerForActivityResult(
        caller,
        EchoContract(),
        BetterActivityLauncher.OnActivityResult { received.add("default:$it") },
      )

    launcher.launch("first")
    caller.dispatchResult("one")
    launcher.launch(
      "second",
      BetterActivityLauncher.OnActivityResult { received.add("override:$it") },
    )
    caller.dispatchResult("two")
    launcher.launch("third")
    caller.dispatchResult("three")

    assertEquals(listOf("default:one", "override:two", "override:three"), received)
    assertEquals(listOf("first", "second", "third"), caller.launcher.launchedInputs)
  }

  @Test
  fun betterActivityLauncherAllowsReplacingCallbackWithoutLaunching() {
    val caller = CapturingActivityResultCaller<String, String>()
    val received = mutableListOf<String>()
    val launcher = BetterActivityLauncher.registerForActivityResult(caller, EchoContract())

    caller.dispatchResult("ignored")
    launcher.setOnActivityResult { received.add(it) }
    caller.dispatchResult("handled")

    assertEquals(listOf("handled"), received)
  }

  @Test
  fun registerActivityForResultUsesStartActivityForResultContract() {
    val caller = CapturingActivityResultCaller<Intent, androidx.activity.result.ActivityResult>()

    BetterActivityLauncher.registerActivityForResult(caller)

    assertTrue(caller.contract is ActivityResultContracts.StartActivityForResult)
  }

  @Test
  fun switchLocaleFallsBackToConfigurationLocaleWhenApplicationLocaleIsEmpty() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())

    assertSame(
      context.resources.configuration.locales.get(0),
      SwitchLocaleUtil.getActiveLocale(context),
    )
  }

  @Test
  fun launcherIconControllerTogglesDefaultAndCalendarAliases() {
    val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    val packageManager = activity.packageManager
    val defaultAlias =
      ComponentName(BuildConfig.APPLICATION_ID, "deltazero.amarok.launcher.default")
    val calendarAlias =
      ComponentName(BuildConfig.APPLICATION_ID, "deltazero.amarok.launcher.calendar")

    LauncherIconController.setIconState(activity, LauncherIconController.IconState.VISIBLE)
    assertEquals(
      PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
      packageManager.getComponentEnabledSetting(defaultAlias),
    )
    assertEquals(
      PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
      packageManager.getComponentEnabledSetting(calendarAlias),
    )

    LauncherIconController.setIconState(activity, LauncherIconController.IconState.DISGUISED)
    assertEquals(
      PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
      packageManager.getComponentEnabledSetting(defaultAlias),
    )
    assertEquals(
      PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
      packageManager.getComponentEnabledSetting(calendarAlias),
    )

    LauncherIconController.setIconState(activity, LauncherIconController.IconState.HIDDEN)
    assertEquals(
      PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
      packageManager.getComponentEnabledSetting(defaultAlias),
    )
    assertEquals(
      PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
      packageManager.getComponentEnabledSetting(calendarAlias),
    )
  }

  private class CapturingActivityResultCaller<Input, Result> : ActivityResultCaller {
    lateinit var callback: ActivityResultCallback<Result>
    lateinit var contract: ActivityResultContract<Input, Result>
    lateinit var launcher: CapturingActivityResultLauncher<Input>

    override fun <I : Any?, O : Any?> registerForActivityResult(
      contract: ActivityResultContract<I, O>,
      callback: ActivityResultCallback<O>,
    ): ActivityResultLauncher<I> {
      @Suppress("UNCHECKED_CAST")
      this.contract = contract as ActivityResultContract<Input, Result>
      @Suppress("UNCHECKED_CAST")
      this.callback = callback as ActivityResultCallback<Result>
      val launcher = CapturingActivityResultLauncher(contract)
      @Suppress("UNCHECKED_CAST")
      this.launcher = launcher as CapturingActivityResultLauncher<Input>
      @Suppress("UNCHECKED_CAST")
      return launcher as ActivityResultLauncher<I>
    }

    override fun <I : Any?, O : Any?> registerForActivityResult(
      contract: ActivityResultContract<I, O>,
      registry: ActivityResultRegistry,
      callback: ActivityResultCallback<O>,
    ): ActivityResultLauncher<I> = registerForActivityResult(contract, callback)

    fun dispatchResult(result: Result) {
      callback.onActivityResult(result)
    }
  }

  private class CapturingActivityResultLauncher<Input>(
    override val contract: ActivityResultContract<Input, *>
  ) : ActivityResultLauncher<Input>() {
    val launchedInputs = mutableListOf<Input>()

    override fun launch(input: Input, options: ActivityOptionsCompat?) {
      launchedInputs.add(input)
    }

    override fun unregister() = Unit
  }

  private class EchoContract : ActivityResultContract<String, String>() {
    override fun createIntent(context: Context, input: String): Intent = Intent(input)

    override fun parseResult(resultCode: Int, intent: Intent?): String = intent?.action.orEmpty()
  }
}
