package deltazero.amarok.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import deltazero.amarok.R
import deltazero.amarok.ui.settings.sections.AboutSection
import deltazero.amarok.ui.settings.sections.AppearanceSection
import deltazero.amarok.ui.settings.sections.PrivacySection
import deltazero.amarok.ui.settings.sections.QuickHideSection
import deltazero.amarok.ui.settings.sections.UpdateSection
import deltazero.amarok.ui.settings.sections.WorkmodeSection
import deltazero.amarok.ui.settings.sections.XHideSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreenContent(
  state: SettingsUiState,
  isHidden: Boolean,
  workmodeActions: WorkmodeActions,
  xHideActions: XHideActions,
  privacyActions: PrivacyActions,
  quickHideActions: QuickHideActions,
  appearanceActions: AppearanceActions,
  updateActions: UpdateActions,
  aboutActions: AboutActions,
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      MediumTopAppBar(
        title = { Text(stringResource(R.string.more_settings)) },
        scrollBehavior = scrollBehavior,
      )
    },
  ) { padding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
    ) {
      WorkmodeSection(state = state.workmode, isHidden = isHidden, actions = workmodeActions)
      XHideSection(state = state.xHide, actions = xHideActions)
      PrivacySection(state = state.privacy, actions = privacyActions)
      QuickHideSection(state = state.quickHide, actions = quickHideActions)
      AppearanceSection(state = state.appearance, actions = appearanceActions)
      UpdateSection(state = state.updates, actions = updateActions)
      AboutSection(state = state.about, actions = aboutActions)
      Spacer(Modifier.height(32.dp))
    }
  }
}
