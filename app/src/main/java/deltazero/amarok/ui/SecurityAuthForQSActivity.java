package deltazero.amarok.ui;

import deltazero.amarok.Hider;

public class SecurityAuthForQSActivity extends SecurityAuthActivity {
    @Override
    protected void onSuccess() {
        Hider.unhide(this);
        super.onSuccess();
    }
}