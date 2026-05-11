package deltazero.amarok.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class HashUtilTest {
  @Test
  fun calculateHashReturnsLowercaseMd5WithLeadingZeroPadding() {
    assertEquals("d41d8cd98f00b204e9800998ecf8427e", HashUtil.calculateHash(""))
    assertEquals("b17f5fea97bbb3e07df097fbd6151627", HashUtil.calculateHash("Amarok"))
  }

  @Test
  fun calculateHashUsesAsciiBytesForNonAsciiInput() {
    assertEquals(HashUtil.calculateHash("?"), HashUtil.calculateHash("é"))
  }
}
