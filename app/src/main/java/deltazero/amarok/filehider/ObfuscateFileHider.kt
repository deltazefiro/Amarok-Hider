package deltazero.amarok.filehider

import android.content.Context
import android.util.Base64
import android.util.Log
import deltazero.amarok.core.ActivationResult
import deltazero.amarok.core.HideAction
import deltazero.amarok.core.PrefMgr
import deltazero.amarok.utils.FileHiderUtil
import deltazero.amarok.utils.MediaStoreHelper
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive

class ObfuscateFileHider(context: Context) : FileHider {
  override val mode = FileHiderMode.OBFUSCATE
  override val name = "Obfuscate"

  private val processHeader: Boolean = PrefMgr.getEnableObfuscateFileHeader()
  private val processTextFile: Boolean = PrefMgr.getEnableObfuscateTextFile()
  private val processTextFileEnhanced: Boolean = PrefMgr.getEnableObfuscateTextFileEnhanced()
  private val context: Context = context

  override suspend fun activate() = ActivationResult(success = true, msgResId = 0)

  override suspend fun process(targetDirs: Set<String>, action: HideAction) {
    val hide = action is HideAction.Hide
    for (dir in targetDirs) {
      try {
        processTree(Paths.get(dir), hide)
      } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "Failed to process $dir: ", e)
      }
    }
    MediaStoreHelper.scan(context, targetDirs)
  }

  private suspend fun processTree(targetDir: Path, hide: Boolean) {
    Log.i(TAG, "Start to process file tree: $targetDir")

    Files.walkFileTree(
      targetDir,
      object : SimpleFileVisitor<Path>() {
        override fun visitFile(path: Path, attrs: BasicFileAttributes): FileVisitResult {
          // Check cancellation via thread interrupt (walkFileTree is blocking)
          if (Thread.currentThread().isInterrupted) {
            Log.w(TAG, "File process interrupted.")
            return FileVisitResult.TERMINATE
          }

          // Skip .nomedia
          if (path.fileName.toString() == ".nomedia") return FileVisitResult.CONTINUE

          val shouldProcessHeader = checkShouldProcessHeader(path, hide)
          val shouldProcessWhole = checkShouldProcessWhole(path, hide)

          val endingMark =
            when {
              shouldProcessWhole -> FILENAME_FULL_PROCESS_MARK
              shouldProcessHeader -> FILENAME_HEADER_PROCESS_MARK
              else -> FILENAME_NO_PROCESS_MARK
            }

          val newPath = processFilename(path, hide, endingMark)

          if (newPath != null) {
            when {
              shouldProcessWhole -> processWholeFile(newPath)
              shouldProcessHeader -> processFileHeader(newPath)
            }
          }

          return FileVisitResult.CONTINUE
        }

        override fun postVisitDirectory(dir: Path, e: IOException?): FileVisitResult {
          if (dir != targetDir) processFilename(dir, hide, FILENAME_NO_PROCESS_MARK)
          return FileVisitResult.CONTINUE
        }
      },
    )

    // Rethrow as CancellationException if interrupted
    coroutineContext.ensureActive()
  }

  private fun processFilename(path: Path, hide: Boolean, extraEndingMark: String): Path? {
    val filename = path.fileName.toString()
    val hasEncoded = FileHiderUtil.checkIsMarkInFilename(filename)

    val newFilename: String =
      if (hide) {
        if (hasEncoded) {
          Log.d(TAG, "Found encoded name: $filename, skip...")
          return null
        }
        "." +
          Base64.encodeToString(filename.toByteArray(Charsets.UTF_8), BASE64_TAG) +
          extraEndingMark
      } else {
        if (!hasEncoded) {
          Log.w(TAG, "Found not coded name: $filename, skip...")
          return null
        }
        try {
          String(
            Base64.decode(FileHiderUtil.stripFilenameExtras(filename), BASE64_TAG),
            Charsets.UTF_8,
          )
        } catch (e: IllegalArgumentException) {
          Log.w(TAG, "Unable to decode: $filename")
          return null
        }
      }

    if (hide) Log.d(TAG, "Encode: $path -> $newFilename")
    else Log.d(TAG, "Decode: $path -> $newFilename")

    val newPath = Paths.get(path.parent.toString(), newFilename)
    val succeeded = path.toFile().renameTo(newPath.toFile())
    return if (succeeded) {
      newPath
    } else {
      Log.w(TAG, "Error when renaming file: $path -> $newPath")
      null
    }
  }

  private fun processFileHeader(path: Path) {
    Log.d(TAG, "Processing file header: $path")
    try {
      val file = path.toFile()
      val lastModified = file.lastModified()
      try {
        RandomAccessFile(file, "rw").use { raf ->
          val bytes = ByteArray(8)
          val numBytesRead = raf.read(bytes)
          val numBytesToReplace = maxOf(minOf(numBytesRead, 8), 0)
          for (i in 0 until numBytesToReplace) {
            bytes[i] = bytes[i].toInt().inv().toByte()
          }
          raf.seek(0)
          raf.write(bytes, 0, numBytesToReplace)
        }
      } catch (e: IOException) {
        Log.w(TAG, "processFileHeader failed: ", e)
      }
      file.setLastModified(lastModified)
    } catch (e: SecurityException) {
      Log.w(TAG, "processFileHeader failed: ", e)
    }
  }

  private fun processWholeFile(path: Path) {
    Log.d(TAG, "Processing whole file: $path")
    val buffer = ByteArray(1024)
    var numReadLoops = 0L
    try {
      val file = path.toFile()
      val lastModified = file.lastModified()
      try {
        RandomAccessFile(file, "rw").use { raf ->
          var numBytesRead: Int
          while (raf.read(buffer).also { numBytesRead = it } != -1) {
            val numBytesToReplace = maxOf(minOf(numBytesRead, buffer.size), 0)
            for (i in 0 until numBytesToReplace) {
              buffer[i] = buffer[i].toInt().inv().toByte()
            }
            raf.seek(numReadLoops * buffer.size)
            raf.write(buffer, 0, numBytesToReplace)
            numReadLoops++
          }
        }
      } catch (e: IOException) {
        Log.w(TAG, "processWholeFile failed: ", e)
      }
      file.setLastModified(lastModified)
    } catch (e: SecurityException) {
      Log.w(TAG, "processWholeFile failed: ", e)
    }
  }

  private fun checkShouldProcessWhole(path: Path, hide: Boolean): Boolean {
    if (!processHeader || !processTextFile) return false
    val filename = path.fileName.toString()
    return if (hide) {
      if (processTextFileEnhanced) {
        FileHiderUtil.checkIsTextFileEnhanced(path) &&
          FileHiderUtil.getFileSizeKB(path) <= MAX_PROCESS_WHOLE_FILE_SIZE_KB
      } else {
        FileHiderUtil.checkIsTextFile(filename) &&
          FileHiderUtil.getFileSizeKB(path) <= MAX_PROCESS_ENHANCED_WHOLE_FILE_SIZE_KB
      }
    } else {
      filename.endsWith(FILENAME_FULL_PROCESS_MARK)
    }
  }

  private fun checkShouldProcessHeader(path: Path, hide: Boolean): Boolean {
    val filename = path.fileName.toString()
    return if (hide) processHeader else filename.endsWith(FILENAME_HEADER_PROCESS_MARK)
  }

  companion object {
    private const val TAG = "FileHider"
    private const val MAX_PROCESS_WHOLE_FILE_SIZE_KB = 10 * 1024
    private const val MAX_PROCESS_ENHANCED_WHOLE_FILE_SIZE_KB = 30 * 1024
    private const val BASE64_TAG = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING

    const val FILENAME_NO_PROCESS_MARK = "!amk"
    const val FILENAME_FULL_PROCESS_MARK = "!amk1"
    const val FILENAME_HEADER_PROCESS_MARK = "!amk2"
  }
}
