package deltazero.amarok.ui;

import deltazero.amarok.core.Hider;

public class SecurityAuthForQSActivity extends SecurityAuthActivity {
  @Override
  protected void onSuccess() {
    Hider.unhide(this);
    super.onSuccess();
  }
}
