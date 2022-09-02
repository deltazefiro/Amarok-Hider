package deltazero.amarok.AppHider;

import java.util.Set;

import deltazero.amarok.utils.ShellUtil;

public class RootAppHider implements AppHiderBase {

    @Override
    public void hide(Set<String> pkgNames) {
        StringBuilder cmd = new StringBuilder("su\n");
        for (String p: pkgNames)
            cmd.append(String.format("pm disable-user %s\n", p));
        ShellUtil.exec(cmd.toString());
    }

    @Override
    public void unhide(Set<String> pkgNames) {
        StringBuilder cmd = new StringBuilder("su\n");
        for (String p: pkgNames)
            cmd.append(String.format("pm enable %s\n", p));
        ShellUtil.exec(cmd.toString());
    }

    @Override
    public boolean checkAvailability() {
        String[] output = ShellUtil.exec("su -c echo \"Amarok-root-test\"");
        return output != null && output[1].length() == 0;
    }
    
}