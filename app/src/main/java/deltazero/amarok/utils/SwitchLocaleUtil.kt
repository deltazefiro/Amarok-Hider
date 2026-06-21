package deltazero.amarok.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import deltazero.amarok.R
import java.util.Locale

object SwitchLocaleUtil {
  /** `(tag, nativeDisplayName)` pairs for the language picker, in [LangList] order. */
  @JvmStatic
  fun localeOptions(context: Context): List<Pair<String, String>> =
    LangList.LOCALES.map { tag ->
      val name =
        if (tag == "SYSTEM") context.getString(R.string.follow_system)
        else Locale.forLanguageTag(tag).let { it.getDisplayName(it) }
      tag to name
    }

  /** Language tag currently applied, or `"SYSTEM"` when following the system locale. */
  @JvmStatic
  fun currentLocaleTag(): String {
    val active = AppCompatDelegate.getApplicationLocales()
    return if (active.isEmpty) "SYSTEM" else active.toLanguageTags()
  }

  @JvmStatic
  fun applyLocale(tag: String) {
    val locales =
      if (tag == "SYSTEM") LocaleListCompat.getEmptyLocaleList()
      else LocaleListCompat.forLanguageTags(tag)
    AppCompatDelegate.setApplicationLocales(locales)
  }

  @JvmStatic
  fun getActiveLocale(context: Context): Locale {
    val activeLocale = AppCompatDelegate.getApplicationLocales()
    return if (activeLocale == LocaleListCompat.getEmptyLocaleList()) {
      context.resources.configuration.locales.get(0)
    } else {
      Locale.forLanguageTag(activeLocale.toLanguageTags())
    }
  }
}
