package deltazero.amarok;

import java.util.Set;

import deltazero.amarok.utils.ShellUtil;


public class AppHider {

    public boolean isAvailable;

    public AppHiderMode activeMode;

    public AppHider(AppHiderMode mode) {
        activeMode = mode;
        isAvailable = checkAvailability();
    }

    public void hide(Set<String> pkgNames) {
        activeMode.hide(pkgNames);
    }

    public void unhide(Set<String> pkgNames) {
        activeMode.unhide(pkgNames);
    }

    public boolean checkAvailability() {
        return activeMode.checkAvailability();
    }

    interface AppHiderMode {
        void hide(Set<String> pkgNames);
        void unhide(Set<String> pkgNames);
        boolean checkAvailability();
    }

    public static class IceboxMode implements AppHiderMode {

        // Not available on Android 11+

        @Override
        public void hide(Set<String> pkgNames) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void unhide(Set<String> pkgNames) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean checkAvailability() {
            return false;
        }
    }

    public static class RootMode implements AppHiderMode {


        @Override
        public void hide(Set<String> pkgNames) {
            for (String n: pkgNames) {
                ShellUtil.exec("su -c pm disable-user " + n);
            }
        }

        @Override
        public void unhide(Set<String> pkgNames) {
            for (String n: pkgNames) {
                ShellUtil.exec("su -c pm enable " + n);
            }
        }

        @Override
        public boolean checkAvailability() {
            String[] output = ShellUtil.exec("su -h");
            return output != null && output[1].length() == 0;
        }
    }
}
