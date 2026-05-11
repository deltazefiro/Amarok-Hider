package deltazero.amarok.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageVolume
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.os.EnvironmentCompat
import java.io.File
import java.nio.file.Paths

object SDCardUtil {
  /**
   * Get path from uri which points to a path in SdCard.
   *
   * @param context Context.
   * @param splitUri Uri split by `:`
   * @return If the uri is in the sdcard, return the path. Otherwise return null.
   */
  @JvmStatic
  fun getSdCardPathFromUri(context: Context, splitUri: Array<String>): String? {
    assert(splitUri.size == 2)

    var sdCardPaths: List<String>? = ArrayList()
    try {
      sdCardPaths = getSdCardPaths(context, false)
    } catch (e: Exception) {
      Log.e("SDCardUtil", "Get sdcard path failed", e)
    }

    if (sdCardPaths == null || sdCardPaths.isEmpty()) {
      return null
    }

    for (sdCardPath in sdCardPaths) {
      if (splitUri[0].contains(Paths.get(sdCardPath).fileName.toString())) {
        return sdCardPath + File.separator + splitUri[1]
      }
    }

    return null
  }

  /**
   * Get absolute SdCard Path. FIXME: 2022/12/31 THIS FUNCTION HAS NOT BEEN TESTED. IT MAY WORK
   * DIFFERENTLY AND UNEXPECTEDLY ON DIFFERENT ANDROID VERSION!
   *
   * @param includePrimaryExternalStorage set to true if you wish to also include the path of the
   *   primary external storage
   * @return a list of all available sd cards paths, or null if not found. Modified form @android
   *   developer's answer on stackoverflow.
   * @see
   *   [StackOverflow answer](https://stackoverflow.com/questions/11281010/how-can-i-get-the-external-sd-card-path-for-android-4-0/27197248#27197248)
   */
  @JvmStatic
  fun getSdCardPaths(context: Context, includePrimaryExternalStorage: Boolean): List<String>? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val storageManager =
        context.getSystemService(Context.STORAGE_SERVICE) as android.os.storage.StorageManager
      val storageVolumes = storageManager.storageVolumes
      if (storageVolumes.isNotEmpty()) {
        val primaryVolume = storageManager.primaryStorageVolume
        val result = ArrayList<String>(storageVolumes.size)
        for (storageVolume in storageVolumes) {
          val volumePath = getVolumePath(storageVolume) ?: continue
          if (
            storageVolume.mediaStoreVolumeName == primaryVolume.mediaStoreVolumeName ||
              storageVolume.isPrimary
          ) {
            if (includePrimaryExternalStorage) result.add(volumePath)
            continue
          }
          result.add(volumePath)
        }
        return if (result.isEmpty()) null else result
      }
    }

    val externalCacheDirs = ContextCompat.getExternalCacheDirs(context)
    if (externalCacheDirs.isEmpty()) return null
    if (externalCacheDirs.size == 1) {
      if (externalCacheDirs[0] == null) return null
      val storageState = EnvironmentCompat.getStorageState(externalCacheDirs[0]!!)
      if (Environment.MEDIA_MOUNTED != storageState) return null
      if (!includePrimaryExternalStorage && Environment.isExternalStorageEmulated()) return null
    }
    val result = ArrayList<String>()
    if (
      externalCacheDirs[0] != null && (includePrimaryExternalStorage || externalCacheDirs.size == 1)
    ) {
      result.add(getRootOfInnerSdCardFolder(context, externalCacheDirs[0]!!))
    }
    for (i in 1 until externalCacheDirs.size) {
      val file = externalCacheDirs[i] ?: continue
      val storageState = EnvironmentCompat.getStorageState(file)
      if (Environment.MEDIA_MOUNTED == storageState) {
        result.add(getRootOfInnerSdCardFolder(context, externalCacheDirs[i]!!))
      }
    }
    return if (result.isEmpty()) null else result
  }

  private fun getRootOfInnerSdCardFolder(context: Context, inputFile: File): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val storageManager =
        context.getSystemService(Context.STORAGE_SERVICE) as android.os.storage.StorageManager
      val storageVolume = storageManager.getStorageVolume(inputFile)
      if (storageVolume != null) {
        val result = getVolumePath(storageVolume)
        if (result != null) return result
      }
    }

    var file = inputFile
    val totalSpace = file.totalSpace
    while (true) {
      val parentFile = file.parentFile
      if (parentFile == null || parentFile.totalSpace != totalSpace || !parentFile.canRead()) {
        return file.absolutePath
      }
      file = parentFile
    }
  }

  private fun getVolumePath(storageVolume: StorageVolume): String? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      return storageVolume.directory!!.absolutePath
    }
    try {
      val getPath = StorageVolume::class.java.getMethod("getPath")
      return getPath.invoke(storageVolume) as String
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return null
  }
}
