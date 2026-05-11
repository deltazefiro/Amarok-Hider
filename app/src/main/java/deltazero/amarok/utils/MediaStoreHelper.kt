package deltazero.amarok.utils

import android.content.Context
import android.media.MediaScannerConnection
import android.util.Log

object MediaStoreHelper {
  private const val TAG = "MediaStoreHelper"

  @JvmStatic
  fun scan(context: Context, dirs: Set<String>) {
    try {
      MediaScannerConnection.scanFile(context, dirs.toTypedArray(), null) { _, _ ->
        Log.d(TAG, "MediaStore cache refreshed")
      }
    } catch (e: Exception) {
      // MediaScannerConnection.scanFile may throw ArrayIndexOutOfBoundsException on OxygenOS 11
      // See https://github.com/deltazefiro/Amarok-Hider/issues/171#issuecomment-2225104851
      Log.w(TAG, "Error while rescanning media store", e)
    }
  }
}
