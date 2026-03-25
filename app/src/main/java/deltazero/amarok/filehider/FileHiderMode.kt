package deltazero.amarok.filehider

enum class FileHiderMode(val key: String) {
  NONE("none"),
  OBFUSCATE("obfuscate"),
  NOMEDIA("nomedia"),
  CHMOD("chmod");

  companion object {
    fun fromKey(key: String) = entries.find { it.key == key } ?: NONE
  }
}
