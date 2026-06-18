package deltazero.amarok.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.activityDataStore: DataStore<Preferences> by preferencesDataStore("activity")

/**
 * Persists hide/unhide activity surfaced by the dashboard: timestamps of the last hide and last
 * reveal, plus the set of calendar days on which a reveal (unhide) action occurred. Kept separate
 * from [HiderStateRepository] (what is hidden right now) since this is historical/analytics data.
 *
 * Reveal days are pruned to a rolling [RETENTION_DAYS] window, enough to cover the dashboard's
 * "this month" calendar and the trailing-30-day frequency stat.
 */
@Singleton
class ActivityRepository @Inject constructor(@ApplicationContext context: Context) {

  private val dataStore = context.applicationContext.activityDataStore
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  val lastHiddenAt: StateFlow<Long?> =
    dataStore.data.map { it[Keys.LAST_HIDDEN_AT] }.stateIn(scope, SharingStarted.Eagerly, null)

  val lastRevealedAt: StateFlow<Long?> =
    dataStore.data.map { it[Keys.LAST_REVEALED_AT] }.stateIn(scope, SharingStarted.Eagerly, null)

  val revealDays: StateFlow<Set<LocalDate>> =
    dataStore.data
      .map { prefs ->
        (prefs[Keys.REVEAL_DAYS] ?: emptySet()).mapNotNullTo(mutableSetOf(), ::parseDate)
      }
      .stateIn(scope, SharingStarted.Eagerly, emptySet())

  suspend fun recordHide(at: Long = System.currentTimeMillis()) =
    dataStore.edit { it[Keys.LAST_HIDDEN_AT] = at }

  suspend fun recordReveal(
    at: Long = System.currentTimeMillis(),
    today: LocalDate = LocalDate.now(),
  ) =
    dataStore.edit { prefs ->
      prefs[Keys.LAST_REVEALED_AT] = at
      val cutoff = today.minusDays(RETENTION_DAYS)
      val kept =
        (prefs[Keys.REVEAL_DAYS] ?: emptySet()).mapNotNull(::parseDate).filterTo(mutableSetOf()) {
          it.isAfter(cutoff)
        }
      kept += today
      prefs[Keys.REVEAL_DAYS] = kept.mapTo(mutableSetOf()) { it.toString() }
    }

  private fun parseDate(value: String): LocalDate? =
    try {
      LocalDate.parse(value)
    } catch (e: Exception) {
      null
    }

  private object Keys {
    val LAST_HIDDEN_AT = longPreferencesKey("lastHiddenAt")
    val LAST_REVEALED_AT = longPreferencesKey("lastRevealedAt")
    val REVEAL_DAYS = stringSetPreferencesKey("revealDays")
  }

  private companion object {
    const val RETENTION_DAYS = 92L
  }
}
