package deltazero.amarok.utils;

import deltazero.amarok.PrefMgr;

public class SecurityUtil {
    private static boolean locked = true;
    private static boolean disguised = true;

    public static void lockAndDisguise() {
        locked = true;
        disguised = true;
    }

    public static void unlock() {
        locked = false;
    }

    public static void dismissDisguise() {
        disguised = false;
    }

    public static boolean isDisguiseNeeded() {
        return PrefMgr.getEnableDisguise() && disguised;
    }

    public static boolean isUnlockRequired() {
        return PrefMgr.getAmarokPassword() != null && locked;
    }
}
