package deltazero.amarok.utils

import deltazero.amarok.filehider.ObfuscateFileHider.Companion.FILENAME_FULL_PROCESS_MARK
import deltazero.amarok.filehider.ObfuscateFileHider.Companion.FILENAME_HEADER_PROCESS_MARK
import deltazero.amarok.filehider.ObfuscateFileHider.Companion.FILENAME_NO_PROCESS_MARK
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileHiderUtilTest {
  @Test
  fun stripEndingRemovesFromLastOccurrenceNotOnlyActualEnding() {
    assertEquals(
      "photo",
      FileHiderUtil.stripEnding("photo${FILENAME_NO_PROCESS_MARK}", FILENAME_NO_PROCESS_MARK),
    )
    assertEquals(
      "photo",
      FileHiderUtil.stripEnding("photo${FILENAME_NO_PROCESS_MARK}.jpg", FILENAME_NO_PROCESS_MARK),
    )
    assertEquals("photo.old", FileHiderUtil.stripEnding("photo.old.old", ".old"))
    assertEquals("photo.jpg", FileHiderUtil.stripEnding("photo.jpg", ""))
  }

  @Test
  fun stripStartRemovesOnlyLeadingPrefix() {
    assertEquals("photo.jpg", FileHiderUtil.stripStart(".photo.jpg", "."))
    assertEquals("photo.jpg", FileHiderUtil.stripStart("photo.jpg", "."))
    assertEquals("", FileHiderUtil.stripStart(".", "."))
  }

  @Test
  fun checkIsMarkInFilenameIgnoresOneLeadingDotAndChecksKnownSuffixes() {
    assertTrue(FileHiderUtil.checkIsMarkInFilename(".photo$FILENAME_NO_PROCESS_MARK"))
    assertTrue(FileHiderUtil.checkIsMarkInFilename("photo$FILENAME_FULL_PROCESS_MARK"))
    assertTrue(FileHiderUtil.checkIsMarkInFilename("photo$FILENAME_HEADER_PROCESS_MARK"))
    assertFalse(FileHiderUtil.checkIsMarkInFilename("photo${FILENAME_NO_PROCESS_MARK}.jpg"))
    assertFalse(FileHiderUtil.checkIsMarkInFilename("photo.jpg"))
  }

  @Test
  fun stripFilenameExtrasRemovesLeadingDotAndKnownMarks() {
    assertEquals("photo", FileHiderUtil.stripFilenameExtras(".photo$FILENAME_NO_PROCESS_MARK"))
    assertEquals("photo", FileHiderUtil.stripFilenameExtras(".photo$FILENAME_FULL_PROCESS_MARK"))
    assertEquals("photo", FileHiderUtil.stripFilenameExtras(".photo$FILENAME_HEADER_PROCESS_MARK"))
    assertEquals("photo", FileHiderUtil.stripFilenameExtras("photo${FILENAME_NO_PROCESS_MARK}.jpg"))
  }

  @Test
  fun checkIsTextFileMatchesOnlyLowercaseCommonExtensions() {
    assertTrue(FileHiderUtil.checkIsTextFile("notes.txt"))
    assertTrue(FileHiderUtil.checkIsTextFile("lyrics.lrc"))
    assertFalse(FileHiderUtil.checkIsTextFile("notes.TXT"))
    assertFalse(FileHiderUtil.checkIsTextFile("README"))
  }

  @Test
  fun getFileSizeKBRoundsDownToWholeKilobytes() {
    val file = Files.createTempFile("amarok-file-hider-util", ".bin")
    try {
      Files.write(file, ByteArray(1536))

      assertEquals(1, FileHiderUtil.getFileSizeKB(file))
    } finally {
      Files.deleteIfExists(file)
    }
  }
}
