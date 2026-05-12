package deltazero.amarok.utils

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import dagger.hilt.android.EntryPointAccessors
import deltazero.amarok.AmarokApplication
import deltazero.amarok.BuildConfig
import deltazero.amarok.R
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.maven.artifact.versioning.ComparableVersion
import org.json.JSONArray

object UpdateUtil {
  private const val TAG = "UpdateUtil"
  private val executor = Executors.newSingleThreadExecutor()
  private val mainHandler = Handler(Looper.getMainLooper())
  private val httpClient =
    OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.SECONDS)
      .readTimeout(10, TimeUnit.SECONDS)
      .build()

  enum class UpdateChannel {
    RELEASE,
    BETA;

    companion object {
      @JvmStatic
      fun fromString(value: String): UpdateChannel {
        return try {
          valueOf(value.uppercase(Locale.getDefault()))
        } catch (e: Exception) {
          RELEASE
        }
      }
    }
  }

  @JvmRecord data class Release(val version: String, val url: String)

  /**
   * Check for updates and show dialog if available.
   *
   * @param context Context
   * @param silent If true, only show dialog when update is available
   */
  @JvmStatic
  fun checkAndNotify(context: Context, silent: Boolean) {
    if (!silent) {
      mainHandler.post {
        Toast.makeText(context, R.string.checking_update, Toast.LENGTH_SHORT).show()
      }
    }

    executor.execute {
      try {
        val currentVersion = getCurrentVersion(context)
        val channel =
          EntryPointAccessors.fromApplication(
              context.applicationContext,
              AmarokApplication.RepositoryEntryPoint::class.java,
            )
            .settingsRepository()
            .settings
            .value
            .updateChannel
        val latestRelease = fetchLatestRelease(channel)

        Log.d(
          TAG,
          "Latest release version: ${latestRelease.version} Current version: v$currentVersion",
        )

        if (isNewerVersion(currentVersion, latestRelease.version)) {
          mainHandler.post { showUpdateDialog(context, latestRelease) }
        } else if (!silent) {
          mainHandler.post {
            Toast.makeText(context, R.string.no_update_ava, Toast.LENGTH_SHORT).show()
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to check for updates", e)
        if (!silent) {
          mainHandler.post {
            Toast.makeText(context, R.string.update_check_failed, Toast.LENGTH_SHORT).show()
          }
        }
      }
    }
  }

  private fun showUpdateDialog(context: Context, release: Release) {
    AlertDialog.Builder(context)
      .setTitle(R.string.update_available_title)
      .setMessage(context.getString(R.string.update_available_message, release.version))
      .setPositiveButton(R.string.view_on_github) { _, _ ->
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.url)))
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun fetchLatestRelease(channel: UpdateChannel): Release {
    val request =
      Request.Builder()
        .url(BuildConfig.GITHUB_CHECK_UPDATE_URL)
        .header("Accept", "application/vnd.github.v3+json")
        .get()
        .build()

    httpClient.newCall(request).execute().use { response ->
      if (!response.isSuccessful) {
        throw Exception("Network error: ${response.code}")
      }

      val releases = JSONArray(response.body.string())

      for (i in 0 until releases.length()) {
        val release = releases.getJSONObject(i)
        if (release.getBoolean("draft")) continue
        if (channel == UpdateChannel.RELEASE && release.getBoolean("prerelease")) continue
        return Release(release.getString("tag_name"), release.getString("html_url"))
      }

      throw Exception("No release found")
    }
  }

  @Suppress("DEPRECATION")
  private fun getCurrentVersion(context: Context): String {
    return try {
      context.packageManager
        .getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)
        .versionName ?: "0.0.0"
    } catch (e: PackageManager.NameNotFoundException) {
      "0.0.0"
    }
  }

  @JvmStatic
  private fun isNewerVersion(current: String, newVersion: String): Boolean {
    val normalizedCurrent = current.replaceFirst("^v".toRegex(), "")
    val normalizedNewVersion = newVersion.replaceFirst("^v".toRegex(), "")
    return ComparableVersion(normalizedNewVersion).compareTo(ComparableVersion(normalizedCurrent)) >
      0
  }
}
