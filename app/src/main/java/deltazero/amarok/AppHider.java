package deltazero.amarok;

import java.util.Set;

import deltazero.amarok.utils.ShellUtil;


public class AppHider {

    public AppHiderMode activeMode;

    public AppHider(AppHiderMode mode) {
        activeMode = mode;
    }

    public void hide(String pkgName) {
        activeMode.hide(pkgName);
    }

    public void unhide(String pkgName) {
        activeMode.unhide(pkgName);
    }

    public boolean checkAvailability() {
        return activeMode.checkAvailability();
    }

    interface AppHiderMode {
        void hide(String pkgName);
        void unhide(String pkgName);
        boolean checkAvailability();
    }

    public static class IceboxMode implements AppHiderMode {

        // Not available on Android 11+

        @Override
        public void hide(String pkgName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void unhide(String pkgName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean checkAvailability() {
            return false;
        }
    }

    public static class RootMode implements AppHiderMode {


        @Override
        public void hide(String pkgName) {
            ShellUtil.exec("su -c pm disable-user " + pkgName);
        }

        @Override
        public void unhide(String pkgName) {
            ShellUtil.exec("su -c pm enable " + pkgName);
        }

        @Override
        public boolean checkAvailability() {
            String[] output = ShellUtil.exec("su -c echo \"Amarok-root-test\"");
            return output != null && output[1].length() == 0;
        }
    }
}
