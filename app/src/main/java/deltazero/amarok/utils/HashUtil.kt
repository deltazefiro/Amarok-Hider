package deltazero.amarok.utils

import android.util.Log
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object HashUtil {
  @JvmStatic
  fun calculateHash(s: String): String {
    try {
      val digest = MessageDigest.getInstance("MD5")
      digest.update(s.toByteArray(StandardCharsets.US_ASCII), 0, s.length)
      val magnitude = digest.digest()
      val bi = BigInteger(1, magnitude)
      return String.format("%0${magnitude.size shl 1}x", bi)
    } catch (e: NoSuchAlgorithmException) {
      Log.w("HashUtil", "Failed to calculate MD5, fallback to raw input: ", e)
    }
    return s
  }
}
