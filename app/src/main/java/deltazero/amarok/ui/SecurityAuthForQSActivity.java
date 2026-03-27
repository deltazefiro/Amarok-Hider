package deltazero.amarok.ui;

import deltazero.amarok.core.Hider;

public class SecurityAuthForQSActivity extends SecurityAuthActivity {
  @Override
  protected void onSuccess() {
    Hider.processAll(this, Hider.Action.UNHIDE);
    super.onSuccess();
  }
}
