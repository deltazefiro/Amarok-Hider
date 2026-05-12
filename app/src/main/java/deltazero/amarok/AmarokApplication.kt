package deltazero.amarok

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.google.android.material.color.DynamicColors
import com.rosan.dhizuku.api.Dhizuku
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import deltazero.amarok.core.Hider
import deltazero.amarok.core.HiderStateRepository
import deltazero.amarok.core.SettingsRepository
import deltazero.amarok.receivers.ScreenStatusReceiver
import deltazero.amarok.utils.AppCenterUtil
import deltazero.amarok.utils.XHidePrefBridge
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

    // WARNING: Do not change the order of those initializations.
    XHidePrefBridge.migratePrefsIfNeeded(this)

    val repositoryEntryPoint =
      EntryPointAccessors.fromApplication(this, RepositoryEntryPoint::class.java)
    settingsRepo = repositoryEntryPoint.settingsRepository()
    hiderStateRepo = repositoryEntryPoint.hiderStateRepository()

    // Block until DataStore has loaded from disk, so that all synchronous .value reads below
    // return actual persisted data rather than defaults.
    runBlocking(Dispatchers.IO) {
      settingsRepo.awaitLoaded()
      hiderStateRepo.awaitLoaded()
    }

    Hider.init(this, settingsRepo, hiderStateRepo)
    QuickHideService.init(this)
    QSTileService.init(applicationContext)
    ToggleWidget.init(applicationContext)

    val settings = settingsRepo.settings.value
    AppCompatDelegate.setDefaultNightMode(settings.darkTheme)
    if (settings.dynamicColor) DynamicColors.applyToActivitiesIfAvailable(this)

    registerReceiver(
      ScreenStatusReceiver(),
      IntentFilter().apply {
        addAction(Intent.ACTION_SCREEN_ON)
        addAction(Intent.ACTION_SCREEN_OFF)
      },
    )

    XHidePrefBridge.init(this)
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
  }
}
