package deltazero.amarok.AppHider;

import android.content.Context;
import android.widget.Toast;

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
        for (String p: pkgNames)
            cmd.append(String.format("&pm disable-user %s", p));
        ShellUtil.exec(new String[]{"su", "-c", cmd.toString()});
    }

    @Override
    public void unhide(Set<String> pkgNames) {
        StringBuilder cmd = new StringBuilder("echo amarok");
        for (String p: pkgNames)
            cmd.append(String.format("&pm enable %s", p));
        ShellUtil.exec(new String[]{"su", "-c", cmd.toString()});
    }

    @Override
    public boolean checkAvailability() {
        String[] output = ShellUtil.exec("su -c echo \"Amarok-root-test\"");
        if (output == null || output[1].length() != 0) {
            Toast.makeText(context, R.string.root_not_ava, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    @Override
    public String getName() {
        return "Root";
    }

}