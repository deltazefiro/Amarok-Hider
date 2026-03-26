package deltazero.amarok.ui;

import deltazero.amarok.core.HideAction;
import deltazero.amarok.core.Hider;

public class SecurityAuthForQSActivity extends SecurityAuthActivity {
  @Override
  protected void onSuccess() {
    Hider.processAll(this, HideAction.Unhide.INSTANCE);
    super.onSuccess();
  }
}
