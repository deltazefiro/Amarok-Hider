package deltazero.amarok.AppHider;

import android.content.Context;

import java.util.Set;

import deltazero.amarok.R;
import deltazero.amarok.utils.ShellUtil;

public class RootAppHider extends BaseAppHider {

    public RootAppHider(Context context) {
        super(context);
    }

    @Override
    public void hide(Set<String> pkgNames) {
        StringBuilder cmd = new StringBuilder("echo amarok");
        for (String p : pkgNames)
            cmd.append(String.format("&pm disable-user %s & pm hide %s", p, p));
        ShellUtil.exec(new String[]{"su", "-c", cmd.toString()});
    }

    @Override
    public void unhide(Set<String> pkgNames) {
        StringBuilder cmd = new StringBuilder("echo amarok");
        for (String p : pkgNames)
            cmd.append(String.format("&pm enable %s & pm unhide %s", p, p));
        ShellUtil.exec(new String[]{"su", "-c", cmd.toString()});
    }

    @Override
    public void tryToActivate(ActivationCallbackListener activationCallbackListener) {
        String[] output = ShellUtil.exec("su -c echo \"Amarok-root-test\"");
        if (output == null || output[1].length() != 0) {
            activationCallbackListener.onActivateCallback(RootAppHider.class, false, R.string.root_not_ava);
        } else {
            activationCallbackListener.onActivateCallback(getClass(), true, 0);
        }
    }

    @Override
    public String getName() {
        return "Root";
    }

}