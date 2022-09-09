package deltazero.amarok.AppHider;

import android.content.Context;

import java.util.Set;

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
        return output != null && output[1].length() == 0;
    }

    @Override
    public String getName() {
        return "Root";
    }

}