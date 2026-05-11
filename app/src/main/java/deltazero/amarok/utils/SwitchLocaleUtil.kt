package deltazero.amarok.utils

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import deltazero.amarok.R
import java.util.Locale

object SwitchLocaleUtil {
  @JvmStatic
  fun switchLocale(context: Context) {
    val radioGroupView =
      LayoutInflater.from(context).inflate(R.layout.dialog_scrollable_button_group, null)
    val rgRadioGroup = radioGroupView.findViewById<RadioGroup>(R.id.dialog_rg_radio_group)

    // Setup buttons
    for (i in LangList.LOCALES.indices) {
      val radioButton = RadioButton(context)

      val displayName =
        if (LangList.LOCALES[i] == "SYSTEM") {
          context.getString(R.string.follow_system)
        } else {
          val locale = Locale.forLanguageTag(LangList.LOCALES[i])
          locale.getDisplayName(locale)
          // displayName = String.format("%s: %s", locale.getDisplayName(locale),
          // locale.getDisplayName());
        }

      radioButton.text = displayName
      radioButton.id = i
      rgRadioGroup.addView(radioButton)
    }

    // Apply current active locale
    val activeLocale = AppCompatDelegate.getApplicationLocales()
    if (activeLocale == LocaleListCompat.getEmptyLocaleList()) {
      rgRadioGroup.check(0)
    } else {
      rgRadioGroup.check(LangList.LOCALES.asList().indexOf(activeLocale.toLanguageTags()))
    }

    // Listener and switch locale
    rgRadioGroup.setOnCheckedChangeListener { _, checkedId ->
      Log.d("Locales", String.format("Active locale: %s", LangList.LOCALES[checkedId]))
      if (checkedId == 0) {
        /* Follow system */
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
      } else {
        val appLocale = LocaleListCompat.forLanguageTags(LangList.LOCALES[checkedId])
        AppCompatDelegate.setApplicationLocales(appLocale)
      }
    }

    // Show dialog
    MaterialAlertDialogBuilder(context).setView(radioGroupView).show()
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
