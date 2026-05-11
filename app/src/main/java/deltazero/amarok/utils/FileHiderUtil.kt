package deltazero.amarok.utils

import android.util.Log
import deltazero.amarok.filehider.ObfuscateFileHider.Companion.FILENAME_FULL_PROCESS_MARK
import deltazero.amarok.filehider.ObfuscateFileHider.Companion.FILENAME_HEADER_PROCESS_MARK
import deltazero.amarok.filehider.ObfuscateFileHider.Companion.FILENAME_NO_PROCESS_MARK
import java.nio.file.Files
import java.nio.file.Path

object FileHiderUtil {
  private val COMMON_TEXT_EXTENSION = listOf(".txt", ".md", ".lrc")
  private const val ASSUME_IS_TEXT_FILE_SIZE_THRESHOLD_KB = 5 * 1024

  @JvmStatic
  fun stripEnding(str: String, suffix: String): String {
    if (str.isEmpty() || suffix.isEmpty()) {
      return str
    }
    val indexOfLast = str.lastIndexOf(suffix)
    if (indexOfLast >= 0) {
      return str.substring(0, indexOfLast)
    }
    return str
  }

  @JvmStatic
  fun stripStart(str: String, prefix: String): String {
    if (str.isEmpty() || prefix.isEmpty()) {
      return str
    }
    if (str.startsWith(prefix)) {
      return str.substring(prefix.length)
    }
    return str
  }

  @JvmStatic
  fun checkIsMarkInFilename(filename: String): Boolean {
    var strippedFilename = filename

    // Strip leading dot
    if (strippedFilename.startsWith(".")) strippedFilename = strippedFilename.substring(1)

    return strippedFilename.endsWith(FILENAME_NO_PROCESS_MARK) ||
      strippedFilename.endsWith(FILENAME_HEADER_PROCESS_MARK) ||
      strippedFilename.endsWith(FILENAME_FULL_PROCESS_MARK)
  }

  /**
   * Remove leading-dot & full obfuscation mark form the filename.
   *
   * @param filename Original filename.
   * @return Striped filename.
   */
  @JvmStatic
  fun stripFilenameExtras(filename: String): String {
    var strippedFilename = filename

    // Strip leading dot
    if (strippedFilename.startsWith(".")) strippedFilename = strippedFilename.substring(1)

    // Strip ending mark
    strippedFilename = stripEnding(strippedFilename, FILENAME_NO_PROCESS_MARK)
    strippedFilename = stripEnding(strippedFilename, FILENAME_HEADER_PROCESS_MARK)
    strippedFilename = stripEnding(strippedFilename, FILENAME_FULL_PROCESS_MARK)

    return strippedFilename
  }

  /**
   * Get file size.
   *
   * @param p Path to the file.
   * @return File size, in KB.
   */
  @JvmStatic fun getFileSizeKB(p: Path): Int = (p.toFile().length() / 1024).toString().toInt()

  @JvmStatic
  fun checkIsTextFile(filename: String): Boolean {
    val idx = filename.lastIndexOf(".")
    if (idx == -1) return false
    return COMMON_TEXT_EXTENSION.contains(filename.substring(idx))
  }

  @JvmStatic
  fun checkIsTextFileEnhanced(path: Path): Boolean {
    val type =
      try {
        Files.probeContentType(path)
      } catch (e: Exception) {
        Log.w(
          "FileHider",
          String.format("%s: Failed to check is text: ", path.fileName.toString()),
          e,
        )
        return false
      }

    if (type == null) {
      // `Files` failed to probe context type. If the files is smaller than
      // ASSUME_IS_TEXT_FILE_SIZE_THRESHOLD_KB, assume it is a text file.
      Log.d("FileHider", String.format("%s: Failed to probe MIME: ", path.fileName.toString()))
      return getFileSizeKB(path) <= ASSUME_IS_TEXT_FILE_SIZE_THRESHOLD_KB
    }

    Log.d("FileHider", String.format("%s: MIME type: %s", path.fileName.toString(), type))
    return type.startsWith("text/") ||
      type.contains("lrc") ||
      type.contains("json") ||
      type.contains("xml") ||
      type.contains("html")
  }
}
