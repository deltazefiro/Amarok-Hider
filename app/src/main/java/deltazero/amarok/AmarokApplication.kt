package deltazero.amarok

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.asLiveData
import com.google.android.material.color.DynamicColors
import com.rosan.dhizuku.api.Dhizuku
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import deltazero.amarok.core.Hider
import deltazero.amarok.core.HiderController
import deltazero.amarok.core.HiderStateRepository
import deltazero.amarok.core.LockTrigger
import deltazero.amarok.core.QuickHideController
import deltazero.amarok.core.SettingsRepository
import deltazero.amarok.receivers.ScreenStatusReceiver
import deltazero.amarok.utils.AppCenterUtil
import deltazero.amarok.utils.SecurityUtil
import deltazero.amarok.utils.XHideModuleBridge
import deltazero.amarok.widget.ToggleWidget
import jonathanfinerty.once.Once
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class AmarokApplication : Application() {

  lateinit var settingsRepo: SettingsRepository
    private set

  lateinit var hiderStateRepo: HiderStateRepository
    private set

  val invertTileColorLiveData: LiveData<Boolean> by lazy {
    settingsRepo.settings.map { it.invertTileColor }.distinctUntilChanged().asLiveData()
  }

  override fun onCreate() {
    super.onCreate()

    val repositoryEntryPoint =
      EntryPointAccessors.fromApplication(this, RepositoryEntryPoint::class.java)
    settingsRepo = repositoryEntryPoint.settingsRepository()
    hiderStateRepo = repositoryEntryPoint.hiderStateRepository()
    Hider.install(repositoryEntryPoint.hiderController())

    // Block until DataStore has loaded from disk, so that all synchronous .value reads below
    // return actual persisted data rather than defaults.
    runBlocking(Dispatchers.IO) {
      settingsRepo.awaitLoaded()
      hiderStateRepo.awaitLoaded()
    }

    Hider.init(this)
    repositoryEntryPoint.quickHideController().init()
    QSTileService.init(applicationContext)
    ToggleWidget.init(applicationContext)

    val settings = settingsRepo.settings.value
    AppCompatDelegate.setDefaultNightMode(settings.darkTheme)
    if (settings.dynamicColor) DynamicColors.applyToActivitiesIfAvailable(this)

    registerReceiver(
      ScreenStatusReceiver(settingsRepo),
      IntentFilter().apply {
        addAction(Intent.ACTION_SCREEN_ON)
        addAction(Intent.ACTION_SCREEN_OFF)
      },
    )

    // Re-arm lock/disguise when the whole app goes to background, if the user opted in.
    ProcessLifecycleOwner.get()
      .lifecycle
      .addObserver(
        object : DefaultLifecycleObserver {
          override fun onStop(owner: LifecycleOwner) {
            SecurityUtil.onTrigger(LockTrigger.APP_BACKGROUND, settingsRepo)
          }
        }
      )

    XHideModuleBridge.init(this)
    XHideModuleBridge.startSync(this, settingsRepo, hiderStateRepo)
    val appScope = CoroutineScope(Dispatchers.IO)
    AppCenterUtil.startAppCenter(this, settings.autoUpdate) {
      appScope.launch { settingsRepo.setAutoUpdate(false) }
    }
    Dhizuku.init()
    Once.initialise(this)
  }

  @EntryPoint
  @InstallIn(SingletonComponent::class)
  interface RepositoryEntryPoint {
    fun settingsRepository(): SettingsRepository

    fun hiderStateRepository(): HiderStateRepository

    fun hiderController(): HiderController

    fun quickHideController(): QuickHideController
  }
}
