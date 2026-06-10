package deltazero.amarok.xhide

object ParceledListSliceUtil {
  private val sliceClass: Class<*> by lazy { Class.forName("android.content.pm.ParceledListSlice") }

  @Suppress("UNCHECKED_CAST")
  fun <T> sliceToList(slice: Any): List<T> =
    slice.javaClass.getMethod("getList").invoke(slice) as List<T>

  fun listToSlice(list: List<*>): Any =
    sliceClass.getConstructor(List::class.java).newInstance(list)
}
