package deltazero.amarok.apphider

enum class AppHiderMode(val key: String) {
  NONE("none"),
  ROOT("root"),
  SHIZUKU("shizuku"),
  DHIZUKU("dhizuku");

  companion object {
    fun fromKey(key: String) = entries.find { it.key == key } ?: NONE
  }
}
