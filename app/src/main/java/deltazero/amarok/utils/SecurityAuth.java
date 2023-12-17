package deltazero.amarok.utils;

import androidx.annotation.CallSuper;

import deltazero.amarok.PrefMgr;

public class SecurityAuth {
    private static boolean locked = true;

    public static void lock() {
        locked = true;
    }

    public static void unlock() {
        locked = false;
    }

    public static boolean isUnlockRequired() {
        return PrefMgr.getAmarokPassword() != null && locked;
    }

    public static class SecurityAuthCallback {
        @CallSuper
        public void onSecurityAuthCallback(boolean succeed) {
            if (succeed) unlock();
        }
    }
}
