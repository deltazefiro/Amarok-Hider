package deltazero.amarok.AppHider;

import android.content.Context;

import java.util.Set;

import deltazero.amarok.R;
import deltazero.amarok.utils.ShellUtil;

public class RootAppHider extends AppHiderBase {

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
    public CheckAvailabilityResult checkAvailability() {
        String[] output = ShellUtil.exec("su -c echo \"Amarok-root-test\"");
        if (output == null || output[1].length() != 0) {
            return new CheckAvailabilityResult(CheckAvailabilityResult.Result.UNAVAILABLE, R.string.root_not_ava);
        }
        return new CheckAvailabilityResult(CheckAvailabilityResult.Result.AVAILABLE);
    }

    @Override
    public void active(OnActivateCallbackListener onActivateCallbackListener) {
        // Will active when calling `checkAvailability`.
        CheckAvailabilityResult r = checkAvailability();
        if (r.result == CheckAvailabilityResult.Result.AVAILABLE) {
            onActivateCallbackListener.onActivateCallback(getClass(), true, 0);
        } else {
            onActivateCallbackListener.onActivateCallback(getClass(), false, r.msgResID);
        }
    }

    @Override
    public String getName() {
        return "Root";
    }

}