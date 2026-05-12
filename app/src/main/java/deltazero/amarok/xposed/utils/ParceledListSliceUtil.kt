package deltazero.amarok.xposed.utils

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.ClassUtils.newInstanceBestMatch
import com.github.kyuubiran.ezxhelper.Log
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch

object ParceledListSliceUtil {
  private var parceledListSliceClass: Class<*>? = null

  @JvmStatic
  fun init() {
    Log.d("Initializing ParceledListSliceUtil...", null)
    try {
      parceledListSliceClass = loadClass("android.content.pm.ParceledListSlice", null)
    } catch (e: ClassNotFoundException) {
      throw RuntimeException(e)
    }
    Log.d("ParceledListSliceUtil initialized.", null)
  }

  @JvmStatic
  @Suppress("UNCHECKED_CAST")
  fun <T> sliceToList(slice: Any): List<T> =
    try {
      invokeMethodBestMatch(slice, "getList", null) as List<T>
    } catch (e: NoSuchMethodException) {
      throw RuntimeException(e)
    }

  @JvmStatic
  fun listToSlice(list: List<*>): Any =
    try {
      newInstanceBestMatch(parceledListSliceClass!!, list)
    } catch (e: NoSuchMethodException) {
      throw RuntimeException(e)
    }
}
