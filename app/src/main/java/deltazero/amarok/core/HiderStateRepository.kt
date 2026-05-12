package deltazero.amarok.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.hiderStateDataStore: DataStore<Preferences> by
  preferencesDataStore("hider_state")

/**
 * Persists managed item lists and hidden item lists. Kept in a separate DataStore from settings so
 * that frequent hide/unhide operations don't trigger spurious recompositions of the settings UI.
 */
@Singleton
class HiderStateRepository @Inject constructor(@ApplicationContext context: Context) {

  private val dataStore = context.applicationContext.hiderStateDataStore
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  val managedApps: StateFlow<Set<String>> =
    dataStore.data
      .map { prefs -> prefs[Keys.MANAGED_APPS] ?: emptySet() }
      .stateIn(scope, SharingStarted.Eagerly, emptySet())

  val managedFolders: StateFlow<Set<String>> =
    dataStore.data
      .map { prefs -> prefs[Keys.MANAGED_FOLDERS] ?: emptySet() }
      .stateIn(scope, SharingStarted.Eagerly, emptySet())

  val hiddenApps: StateFlow<Set<String>> =
    dataStore.data
      .map { prefs ->
        val managed = prefs[Keys.MANAGED_APPS] ?: emptySet()
        (prefs[Keys.HIDDEN_APPS] ?: emptySet()).intersect(managed)
      }
      .stateIn(scope, SharingStarted.Eagerly, emptySet())

  val hiddenFolders: StateFlow<Set<String>> =
    dataStore.data
      .map { prefs ->
        val managed = prefs[Keys.MANAGED_FOLDERS] ?: emptySet()
        (prefs[Keys.HIDDEN_FOLDERS] ?: emptySet()).intersect(managed)
      }
      .stateIn(scope, SharingStarted.Eagerly, emptySet())

  // Called once at startup to ensure disk data is loaded before synchronous callers use .value.
  suspend fun awaitLoaded() {
    dataStore.data.first()
  }

  // Setters

  suspend fun setManagedApps(apps: Set<String>) =
    dataStore.edit {
      it[Keys.MANAGED_APPS] = apps
      it[Keys.HIDDEN_APPS] = (it[Keys.HIDDEN_APPS] ?: emptySet()).intersect(apps)
    }

  suspend fun addManagedApp(pkgName: String) =
    dataStore.edit { prefs ->
      prefs[Keys.MANAGED_APPS] = (prefs[Keys.MANAGED_APPS] ?: emptySet()) + pkgName
    }

  suspend fun removeManagedApp(pkgName: String) =
    dataStore.edit { prefs ->
      val managed = (prefs[Keys.MANAGED_APPS] ?: emptySet()) - pkgName
      prefs[Keys.MANAGED_APPS] = managed
      prefs[Keys.HIDDEN_APPS] = (prefs[Keys.HIDDEN_APPS] ?: emptySet()).intersect(managed)
    }

  suspend fun setManagedFolders(folders: Set<String>) =
    dataStore.edit {
      it[Keys.MANAGED_FOLDERS] = folders
      it[Keys.HIDDEN_FOLDERS] = (it[Keys.HIDDEN_FOLDERS] ?: emptySet()).intersect(folders)
    }

  suspend fun addManagedFolder(path: String) =
    dataStore.edit { prefs ->
      prefs[Keys.MANAGED_FOLDERS] = (prefs[Keys.MANAGED_FOLDERS] ?: emptySet()) + path
    }

  suspend fun removeManagedFolder(path: String) =
    dataStore.edit { prefs ->
      val managed = (prefs[Keys.MANAGED_FOLDERS] ?: emptySet()) - path
      prefs[Keys.MANAGED_FOLDERS] = managed
      prefs[Keys.HIDDEN_FOLDERS] = (prefs[Keys.HIDDEN_FOLDERS] ?: emptySet()).intersect(managed)
    }

  // Hidden-state writes must only be performed by Hider after the corresponding hide/unhide
  // side effect succeeds. UI and other callers should change managed items, not hidden state.
  suspend fun setHiddenApps(apps: Set<String>) =
    dataStore.edit { it[Keys.HIDDEN_APPS] = apps.intersect(it[Keys.MANAGED_APPS] ?: emptySet()) }

  suspend fun setHiddenFolders(folders: Set<String>) =
    dataStore.edit {
      it[Keys.HIDDEN_FOLDERS] = folders.intersect(it[Keys.MANAGED_FOLDERS] ?: emptySet())
    }

  suspend fun addManagedApps(apps: Set<String>) {
    if (apps.isEmpty()) return
    dataStore.edit { it[Keys.MANAGED_APPS] = (it[Keys.MANAGED_APPS] ?: emptySet()) + apps }
  }

  suspend fun removeManagedApps(apps: Set<String>) {
    if (apps.isEmpty()) return
    dataStore.edit {
      val managed = (it[Keys.MANAGED_APPS] ?: emptySet()) - apps
      it[Keys.MANAGED_APPS] = managed
      it[Keys.HIDDEN_APPS] = (it[Keys.HIDDEN_APPS] ?: emptySet()).intersect(managed)
    }
  }

  suspend fun addManagedFolders(folders: Set<String>) {
    if (folders.isEmpty()) return
    dataStore.edit { it[Keys.MANAGED_FOLDERS] = (it[Keys.MANAGED_FOLDERS] ?: emptySet()) + folders }
  }

  suspend fun removeManagedFolders(folders: Set<String>) {
    if (folders.isEmpty()) return
    dataStore.edit {
      val managed = (it[Keys.MANAGED_FOLDERS] ?: emptySet()) - folders
      it[Keys.MANAGED_FOLDERS] = managed
      it[Keys.HIDDEN_FOLDERS] = (it[Keys.HIDDEN_FOLDERS] ?: emptySet()).intersect(managed)
    }
  }

  // See setHiddenApps: these mutate the persisted result of Hider operations, not configuration.
  suspend fun addHiddenApps(apps: Set<String>) {
    if (apps.isEmpty()) return
    dataStore.edit {
      val managed = it[Keys.MANAGED_APPS] ?: emptySet()
      it[Keys.HIDDEN_APPS] = ((it[Keys.HIDDEN_APPS] ?: emptySet()) + apps).intersect(managed)
    }
  }

  suspend fun removeHiddenApps(apps: Set<String>) {
    if (apps.isEmpty()) return
    dataStore.edit { it[Keys.HIDDEN_APPS] = (it[Keys.HIDDEN_APPS] ?: emptySet()) - apps }
  }

  suspend fun addHiddenFolders(folders: Set<String>) {
    if (folders.isEmpty()) return
    dataStore.edit {
      val managed = it[Keys.MANAGED_FOLDERS] ?: emptySet()
      it[Keys.HIDDEN_FOLDERS] =
        ((it[Keys.HIDDEN_FOLDERS] ?: emptySet()) + folders).intersect(managed)
    }
  }

  suspend fun removeHiddenFolders(folders: Set<String>) {
    if (folders.isEmpty()) return
    dataStore.edit { it[Keys.HIDDEN_FOLDERS] = (it[Keys.HIDDEN_FOLDERS] ?: emptySet()) - folders }
  }

  // Preference keys

  private object Keys {
    val MANAGED_APPS = stringSetPreferencesKey("hidePkgNames")
    val MANAGED_FOLDERS = stringSetPreferencesKey("hideFilePath")
    val HIDDEN_APPS = stringSetPreferencesKey("hiddenApps")
    val HIDDEN_FOLDERS = stringSetPreferencesKey("hiddenFolders")
  }
}
